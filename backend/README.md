# PetcliniX

## Getting Started with PetcliniX SpringBoot App

## Build Docker Image

To build a Docker image for the Springboot app, you can use the following command:

```bash
docker build --target production -t petclinix/spring-backend .
```

# Architecture & Design Conventions

## Concept

The PetcliniX backend is a Spring Boot application using a multi-tiered layered architecture
with a pragmatic approach. It uses a clear dependency direction, minimal boilerplate, and no
over-engineering. The goal is to show that good architecture emerges from consistent rules
applied to real problems — not from maximising abstraction.

A logic layer earns its existence for two reasons:

1. **Transformation** — the data model differs from the presentation model and business logic
   is required to bridge the gap.
2. **Testability** — business rules must be verifiable without a running database.

Where neither condition applies, the layer adds ceremony without value.

---

## Package Structure

```
tech.petclinix
├── BackendApplication.java
│
├── web                          # HTTP boundary
│   ├── controller               # REST controllers + inline mapping helpers
│   │   └── mapper               # Entity → DTO mappers used by controllers
│   ├── dto                      # Request and response records
│   └── advice                   # GlobalExceptionHandler
│
├── logic                        # Business logic — framework-agnostic
│   ├── domain                   # Domain objects, interfaces, enums, value objects
│   ├── service                  # Service classes (use-cases)
│   │   └── mapper               # Entity → DomainObject mappers used by services
│   └── AdminInitializer.java    # Data seeding for admin user
│
├── persistence                  # Database boundary
│   ├── entity                   # JPA entities
│   └── jpa                      # Spring Data repositories + Specifications
│
└── security                     # Authentication boundary
    ├── config                   # SecurityConfig, SecurityBeans
    └── jwt                      # JwtUtil, JwtFilter
```

---

## Layer Responsibilities

### `web` — presentation layer
Owns the HTTP contract. Controllers receive requests, delegate to services, and map results
to DTOs. No business logic lives here. No persistence types cross this boundary outward.

### `logic` — logic layer
Owns the domain model and use-cases. Services orchestrate repositories and enforce business
rules. This layer has no knowledge of HTTP, JSON, or JPA annotations. It is the only layer
that all other layers may depend on for domain types. Data objects may implement domain
interfaces from `logic/domain` to avoid redundant mapping where the structures are identical.

### `persistence` — model layer
Owns the database schema. JPA entities and Spring Data repositories live here.

### `security` — security layer
Owns authentication. JWT validation and Spring Security configuration live here. The security
layer depends on `logic` for user lookup but exposes no security types to `logic` itself.

---

## Dependency Rules

Dependencies flow in one direction only: **inward**.

```
web  →  logic/domain
web  →  logic/service
web  →  persistence                     ✗  FORBIDDEN
web  →  persistence/entity              (allowed — pragmatic option)
web/dto  →  logic/domain                implements interfaces (allowed — pragmatic option)

logic/service  →  logic/domain
logic/service  →  persistence/entity    (allowed — pragmatic option)
logic/service  →  persistence/jpa       
logic/service  →  web                   ✗  FORBIDDEN

persistence  →  any layer               ✗  FORBIDDEN

security  →  logic/domain
security  →  logic/service
security  →  web                        ✗  FORBIDDEN
```

The `logic/domain` package is the only package that multiple layers may depend on
simultaneously, because it contains pure data types with no dependencies of its own.

---

## Design Rules

### 1 — Entities are domain objects for simple aggregates

JPA entities are not second-class citizens. For aggregates where the database model
directly represents the domain — no computation, no aggregation across multiple sources —
the entity *is* the domain object. Introducing a parallel domain record would be pure
duplication without value.

A separate domain object in `logic/domain` is introduced **only** when a service method
computes, aggregates, or transforms data such that the result no longer maps directly to
a single entity.

```
// No logic/domain object needed — entity is returned directly
VetService.findAll()         → List<VetEntity>    the data model is the domain model
PetService.findAllByOwner()  → List<PetEntity>    one-to-one, no transformation

// logic/domain object required — result cannot be represented by a single entity
StatsService.getStats()      → StatsData          aggregates four repositories
LocationService              → uses LocationData  input spans periods + overrides
```

The presence of a class in `logic/domain` is itself a signal: something non-trivial
happens here. Keep it meaningful by only adding to it when a real transformation warrants it.

### 2 — The public API of the logic layer must not expose persistence entities

Service **method signatures** — return types and parameters — must use types from
`logic/domain`, or entity types only when Rule 1 explicitly permits it (the entity is the
domain object for that aggregate and no transformation is needed).

Inside service implementations, entities are expected and correct. The restriction applies
only at the public boundary — what callers in `web` or `security` see.

```java
// CORRECT — entity qualifies as the domain object under Rule 1
public List<VetEntity> findAll() { ... }

// CORRECT — transformation exists, so a domain type is introduced and returned
public List<DomainUser> findAll() {
    return repository.findAll()          // List<UserEntity> internally — fine
        .stream()
        .map(UserMapper::toDomain)
        .toList();
}

// FORBIDDEN — web DTO crosses the public boundary
public List<AdminUserResponse> findAll() { ... }
```

### 3 — Services receive `Username`, not `Authentication`

Authentication is completed by `JwtFilter` before a request reaches a controller.
By the time a service is called, the identity is already verified. Services receive a
`Username` value object — a domain concept — not a Spring Security type.

```java
// CORRECT
public List<LocationEntity> findAllByVet(Username vetUsername) { ... }

// FORBIDDEN
public List<LocationEntity> findAllByVet(Authentication authentication) { ... }
```

Controllers extract the username from `Authentication` and wrap it:

```java
locationService.findAllByVet(new Username(authentication.getName()));
```

### 4 — Services do not accept web DTOs as parameters

For write operations, define an interface in `logic/domain`. The web DTO implements this
interface. The service receives the interface type — it never imports from `web`.

```java
// logic/domain/LocationData.java
public interface LocationData {
    String name();
    String zoneId();
    List<? extends PeriodData> weeklyPeriods();
    List<? extends OverrideData> overrides();
}

// web/dto/LocationResponse.java
public record LocationResponse(...) implements LocationData { ... }

// logic/service/LocationService.java — CORRECT
public LocationEntity persist(Username username, LocationData data) { ... }

// logic/service/LocationService.java — FORBIDDEN
public LocationEntity persist(Username username, LocationResponse dto) { ... }
```

When request and response structures diverge, introduce a dedicated `LocationRequest` record
that also implements `LocationData`. The service signature does not change.

### 5 — Controllers do not navigate entity graphs

Controllers must not traverse entity associations to build DTOs. Navigation like
`visit.getAppointment().getVet().getUsername()` couples the web layer to the database
schema and triggers hidden lazy-loading queries.

The service is responsible for loading and returning everything the controller needs,
either as a domain object or directly via a method on the service.

```java
// FORBIDDEN in a controller
v.getAppointment().getVet().getUsername()

// CORRECT — service returns a domain object with the field already resolved
domainVisit.vetUsername()
```

### 6 — Only mappers in the web layer may import persistence types

Controllers must not reference persistence types directly. When a service returns an entity
that requires no transformation in the logic layer (Rule 1), the entity is passed inline
directly into a mapper method — never stored in a named local variable.

This is enforced by the convention that within `web`, only classes ending in `Mapper` may
import from `persistence`. Controllers that follow the inline pattern never acquire a
compile-time dependency on the entity type, so the rule holds automatically.

```java
// CORRECT — entity flows inline into the mapper, controller has no persistence import
return ResponseEntity.ok(
    DtoMapper.toVetVisitResponse(
        vetVisitService.retrieveByVetAndId(new Username(authentication.getName()), appointmentId)
    )
);

// CORRECT — same pattern in a stream, no persistence type named in the controller
return ResponseEntity.ok(
    vetService.findAll().stream()
        .map(DtoMapper::toVetResponse)
        .toList()
);

// FORBIDDEN — controller stores the entity in a named variable, acquiring a persistence import
VisitEntity visit = vetVisitService.retrieveByVetAndId(...);
return ResponseEntity.ok(DtoMapper.toVetVisitResponse(visit));
```

### 7 — Mapping belongs to the layer that needs the result

| Mapping | Location |
|---|---|
| `Entity → DomainObject` | `logic/service/mapper/` |
| `Entity → DTO` | `web/controller/mapper/` |
| `DomainObject → DTO` | Controller method or `web/controller/mapper/` |

A mapper in `logic/service/mapper/` may import from `persistence/entity` and `logic/domain`.
A mapper in `web/controller/mapper/` may import from `persistence/entity`, `logic/domain`,
and `web/dto`. Neither mapper may import from the other layer's mapper.

### 8 — `logic/domain` has no dependencies

Every class, record, interface, and enum in `logic/domain` must have zero imports from
`persistence`, `web`, `security`, or any Spring framework class. It may import from
`java.*` only.

This makes `logic/domain` the stable centre of the architecture — safe for all layers
to depend on without risk of circular dependencies.

### 9 — Domain interfaces over copied records

When a domain type and a DTO would be structurally identical, prefer a `logic/domain`
interface implemented by the DTO over duplicating the record. This avoids boilerplate
mapping while preserving the correct dependency direction.

If the structures later diverge, replace the interface with a concrete class — the service
signature remains unchanged.

```java
// Preferred when structures are identical
public record LocationResponse(...) implements LocationData { ... }

// Only when structures differ
public record LocationRequest(...) implements LocationData { ... }
public record LocationResponse(...) { ... }  // separate, no shared interface needed
```

### 10 — `Username` is a value object, not a `String`

Raw strings are opaque. A `Username` record makes method signatures self-documenting and
prevents accidental parameter swapping.

```java
// Ambiguous
public void cancel(String ownerUsername, Long appointmentId) { ... }

// Self-documenting
public void cancel(Username ownerUsername, Long appointmentId) { ... }
```

Do not wrap an already-typed `Username` in a new one:

```java
// REDUNDANT
vetService.retrieveByUsername(new Username(vetUsername.value()));

// CORRECT
vetService.retrieveByUsername(vetUsername);
```

### 11 — Each controller depends on exactly one service

A controller represents a single resource or use-case boundary. If a controller needs to
coordinate multiple services, that coordination is business logic and belongs in a service,
not in the web layer.

```java
// CORRECT — one primary service per controller
@RestController
public class PetsController {
    private final PetService petService;
}

// FORBIDDEN — coordination logic leaking into the controller
@RestController
public class OwnerAppointmentsController {
    private final AppointmentService appointmentService;
    private final PetService petService;      // ✗
    private final VetService vetService;      // ✗
}
```

The fix is to move the coordination into the primary service:

```java
// AppointmentService — CORRECT
public AppointmentEntity persist(Username ownerUsername, Long petId, Long vetId, LocalDateTime startAt) {
    PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, petId);
    VetEntity vet = vetService.retrieveById(vetId);
    ...
}
```

The rule as written is too strict for the real codebase and you know it — `LocationService`, `PetService`, and `StatsService` all violate it for legitimate reasons. A rule that the code immediately contradicts teaches nothing.

The honest question is whether the two-tier distinction (data services vs orchestrating services) is worth documenting at all. My opinion: yes, but only as a guideline, not as an absolute rule. The value is that it gives a developer a mental model for where to put new code. Without it, everything ends up in one god service.

Here is the adapted Rule 12:

---

### 12 — Services are either data services or orchestrating services

Two kinds of services exist in the logic layer. Understanding the distinction helps decide
where new code belongs.

**Data services** own a single aggregate and talk directly to one or more repositories.
They expose `retrieve`, `findBy`, and `findAll` methods and contain no calls to other
services. Most services in the codebase are data services.

**Orchestrating services** coordinate multiple data services to fulfil a use-case that
spans aggregates. They contain no direct repository dependencies — all data access goes
through the data services they call.

```java
// Data service — owns its repository, no other service dependencies
@Service
public class VisitService {
    private final VisitJpaRepository repository;
}

// Orchestrating service — coordinates data services, no repository
@Service
public class VetVisitService {
    private final AppointmentService appointmentService;
    private final VisitService visitService;
}
```

**Pragmatic exceptions** are permitted when injecting an entity from another service is
necessary to satisfy a repository method — for example, passing a resolved `VetEntity`
into a `Specification`. In these cases a data service may call one other data service
to resolve the entity it needs.

```java
// Permitted exception — one service call to resolve an entity for a query
@Service
public class LocationService {
    private final LocationJpaRepository repository;
    private final VetService vetService;        // resolves VetEntity for Specifications

    public List<LocationEntity> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        return repository.findAll(Specifications.byVet(vet));
    }
}
```

`StatsService` is an intentional exception: it aggregates counts from four repositories
because its sole purpose is to compute a cross-aggregate summary. Splitting it into four
data services with single-method count wrappers would produce boilerplate without value.

The guideline to follow when adding new code: if a service is growing dependencies on both
repositories and other services beyond what a single entity lookup requires, it is a signal
that the use-case should be extracted into a dedicated orchestrating service.

---

### 13 — Service method naming convention

Three prefixes communicate intent and return type consistently across all services:

| Prefix | Return type | Behaviour on missing result |
|---|---|---|
| `retrieve` | single object | throws `EntityNotFoundException` |
| `findBy` | `Optional<T>` | returns `Optional.empty()` |
| `findAll` | `List<T>` | returns empty list, never `null` |

```java
// retrieve — single result, throws if not found
public VetEntity retrieveById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + id));
}

// findBy — single result, caller decides what missing means
public Optional<VetEntity> findByUsername(Username username) {
    return repository.findOne(Specifications.byUsername(username));
}

// findAll — collection, never null
public List<VetEntity> findAll() {
    return repository.findAll();
}
```

Mutation operations use descriptive names without a fixed prefix:
`persist`, `activate`, `deactivate`, `cancel`.

### 14 — All queries use the Criteria API via a static `Specifications` inner class

Spring Data method name derivation (e.g. `findByUsernameAndActive`) is not used. These
methods are stringly typed, break silently on field renames, and cannot be composed.

Every repository declares a static inner class named `Specifications` that exposes type-safe
`Specification<T>` factory methods built with the JPA Criteria API and the generated
metamodel (`*_` classes). Specifications are composable with `.and()` and `.or()`.

```java
// persistence/jpa/VetJpaRepository.java
public interface VetJpaRepository extends JpaRepository<VetEntity, Long>,
        JpaSpecificationExecutor<VetEntity> {

    class Specifications {

        public static Specification<VetEntity> byUsername(Username username) {
            return (root, query, cb) ->
                cb.equal(root.get(VetEntity_.username), username.value());
        }

        public static Specification<VetEntity> byId(Long id) {
            return (root, query, cb) ->
                cb.equal(root.get(VetEntity_.id), id);
        }
    }
}
```

Usage at the call site is explicit and composable:

```java
// CORRECT — type-safe, composable, refactor-safe
repository.findOne(Specifications.byId(id).and(Specifications.byUsername(username)));

// FORBIDDEN — stringly typed, not composable, breaks on rename
repository.findByIdAndUsername(id, username.value());
```

The metamodel classes (`VetEntity_`, `PetEntity_`, etc.) are generated by the JPA
annotation processor configured in `pom.xml`. They must be regenerated after any entity
field change.

---

## What is intentionally not here

**Ports & Adapters (Hexagonal Architecture)** — not applied. Services depend directly on
JPA repositories and entities. This is a conscious trade-off: less indirection, less
boilerplate, full testability via `@DataJpaTest` and integration tests.

**MapStruct or similar mapping libraries** — not used. Mapping is explicit and readable.
Explicit mappers communicate intent more clearly than generated code.

**Separate `Request` and `Response` DTOs for all endpoints** — not enforced. Where request
and response share the same structure, a single record implementing a `logic/domain`
interface is preferred. Separation is introduced only when the structures actually diverge.

**A dedicated domain object for every entity** — not enforced. Entities serve as domain
objects for simple aggregates. A parallel domain record is introduced only when a service
method performs a transformation that the entity cannot represent directly. See Rule 1.
