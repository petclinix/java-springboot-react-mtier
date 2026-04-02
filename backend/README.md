# PetcliniX Backend

## Getting Started

```bash
docker build --target production -t petclinix/spring-backend .
```

---

## Concept

The PetcliniX backend is a Spring Boot application using a layered architecture with a
pragmatic approach: clear dependency direction, minimal boilerplate, no over-engineering.

A logic layer earns its existence for two reasons:

1. **Transformation** — the data model differs from the presentation model and business
   logic is required to bridge the gap.
2. **Testability** — business rules must be verifiable without a running database.

Where neither condition applies, the layer adds ceremony without value.

---

## Package Structure

```
tech.petclinix
├── BackendApplication.java
│
├── web                          # HTTP boundary
│   ├── controller               # REST controllers
│   │   └── mapper               # Entity/DomainObject → DTO mappers
│   ├── dto                      # Request and response records
│   └── advice                   # GlobalExceptionHandler
│
├── logic                        # Business logic — framework-agnostic
│   ├── domain                   # Domain objects, interfaces, enums, value objects
│   ├── service                  # Service classes (use-cases)
│   │   └── mapper               # Entity → DomainObject mappers
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

**`web`** (Presentation Layer) — owns the HTTP contract. Controllers receive requests, delegate to services,
and map results to DTOs. No business logic lives here.

**`logic`** (Logic Layer) — owns the domain model and use-cases. Services orchestrate repositories and
enforce business rules. No knowledge of HTTP, JSON, or JPA annotations. The `logic/domain`
package is the only package all other layers may depend on for shared types.

**`persistence`** (Model Layer) — owns the database schema. JPA entities and Spring Data repositories
live here.

**`security`** (Security Layer) — owns authentication. JWT validation and Spring Security configuration
live here. Depends on `logic` for user lookup; exposes no security types back to `logic`.

---

## Design Constraints

These rules govern the structure of the codebase — what may depend on what, how layers
interact, and how services are composed. Violations break the architecture.

### 1 — Dependency direction

Dependencies flow inward only.

```
web          →  logic/domain          ✓
web          →  logic/service         ✓
web          →  persistence           ✗  FORBIDDEN
web/mapper   →  persistence/entity    ✓  (mappers only — see constraint 6)

logic/service  →  logic/domain        ✓
logic/service  →  persistence/entity  ✓
logic/service  →  persistence/jpa     ✓
logic/service  →  web                 ✗  FORBIDDEN

persistence    →  any layer           ✗  FORBIDDEN

security       →  logic/domain        ✓
security       →  logic/service       ✓
security       →  web                 ✗  FORBIDDEN
```

### 2 — `logic/domain` has no framework dependencies

Every class, record, interface, and enum in `logic/domain` must have zero imports from
`persistence`, `web`, `security`, or any Spring framework class. It may only import from
`java.*`.

This makes `logic/domain` the stable centre — safe for all layers to depend on without
risk of circular dependencies.

### 3 — Entities are domain objects for simple aggregates

For aggregates where the database model directly represents the domain — no computation,
no aggregation across multiple sources — the entity *is* the domain object. Introducing
a parallel domain record would be duplication without value.

A separate domain object in `logic/domain` is introduced **only** when a service method
computes, aggregates, or transforms data such that the result no longer maps to a single
entity.

```
// Entity returned directly — data model is the domain model
VetService.findAll()         → List<VetEntity>
PetService.findAllByOwner()  → List<PetEntity>

// Domain object required — result aggregates multiple sources
StatsService.getStats()      → StatsData       (aggregates four repositories)
LocationService.persist()    → uses LocationData  (input spans periods + overrides)
```

The presence of a class in `logic/domain` is itself a signal that something non-trivial
happens here. Keep it meaningful.

### 4 — The public service API must not expose persistence entities unless constraint 3 permits it

Service **method signatures** must use types from `logic/domain`, or entity types only
when constraint 3 applies. Inside service implementations, entities are expected and
correct — the restriction is on the public boundary only.

```java
// CORRECT — entity qualifies as domain object under constraint 3
public List<VetEntity> findAll() { ... }

// CORRECT — transformation exists, so a domain type is returned
public List<DomainUser> findAll() {
    return repository.findAll().stream()
        .map(UserMapper::toDomain)
        .toList();
}

// FORBIDDEN — web DTO crosses the service boundary
public List<AdminUserResponse> findAll() { ... }
```

### 5 — Services do not accept web DTOs as parameters

For write operations, define an interface in `logic/domain`. The web DTO implements this
interface. The service receives the interface — it never imports from `web`. This is a pragmaticly choice to reduce meaningless duplications.

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

// CORRECT
public LocationEntity persist(Username username, LocationData data) { ... }

// FORBIDDEN
public LocationEntity persist(Username username, LocationResponse dto) { ... }
```

When request and response structures diverge, introduce a dedicated `LocationRequest`
record that also implements `LocationData`. The service signature does not change.

### 6 — Services receive `Username`, not `Authentication`

Authentication is complete by the time a controller is reached. Services receive a
`Username` value object — a domain concept — not a Spring Security type.

```java
// CORRECT
public List<LocationEntity> findAllByVet(Username vetUsername) { ... }

// FORBIDDEN
public List<LocationEntity> findAllByVet(Authentication authentication) { ... }
```

Controllers extract the username and wrap it:

```java
locationService.findAllByVet(new Username(authentication.getName()));
```

### 7 — Only mappers in the web layer may import persistence types

Controllers must not reference persistence types directly. When a service returns an
entity (constraint 3), it is passed inline into a mapper — never stored in a named
local variable. This ensures controllers never acquire a compile-time dependency on
entity types. This is a pragmaticly choice to reduce meaningless duplications.

```java
// CORRECT — entity flows inline, controller has no persistence import
return ResponseEntity.ok(
    vetService.findAll().stream()
        .map(DtoMapper::toVetResponse)
        .toList()
);

// FORBIDDEN — storing the entity gives the controller a persistence import
VisitEntity visit = vetVisitService.retrieveByVetAndId(...);
return ResponseEntity.ok(DtoMapper.toVetVisitResponse(visit));
```

Mapper classes in `web/controller/mapper/` are permitted to navigate entity associations —
resolving the graph into flat DTO fields is precisely their job.

### 8 — Each controller depends on exactly one service

A controller represents a single resource or use-case boundary. If coordination across
multiple services is needed, that coordination belongs in a service, not in the
web layer.

```java
// CORRECT
@RestController
public class PetsController {
    private final PetService petService;
}

// FORBIDDEN — coordination logic in the controller
@RestController
public class OwnerAppointmentsController {
    private final AppointmentService appointmentService;
    private final PetService petService;     // ✗
    private final VetService vetService;     // ✗
}
```

Move the coordination into a dedicated service:

```java
// OwnerAppointmentService
public AppointmentEntity persist(Username ownerUsername, AppointmentData data) {
    PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, data.petId());
    VetEntity vet = vetService.retrieveById(data.vetId());
    return appointmentService.persist(pet, vet, data.startsAt());
}
```

### 9 — Services are either data services or orchestrating services

**Data services** own one aggregate and talk directly to repositories. They have no
dependencies on other services.

**Orchestrating services** coordinate multiple data services for a use-case that spans
aggregates. They have no direct repository dependencies.

```java
// Data service — owns its repository
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

**Permitted exception:** a data service may call one other data service to resolve an
entity needed by a Specification.

```java
@Service
public class LocationService {
    private final LocationJpaRepository repository;
    private final VetService vetService;   // resolves VetEntity for Specifications

    public List<LocationEntity> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        return repository.findAll(Specifications.byVet(vet));
    }
}
```

`StatsService` is an intentional exception: its sole purpose is a cross-aggregate
summary, so it holds four repositories directly.

If a service is accumulating both repository dependencies and service calls beyond a
single entity lookup, extract a new orchestrating service.

---

## Code Conventions

These rules govern how code is written within the architecture — naming, idioms, and
patterns that keep the codebase consistent.

### 1 — Service method naming

Three prefixes communicate intent and return type consistently:

| Prefix | Return type | Behaviour when result is missing |
|--------|------------|----------------------------------|
| `retrieve` | single object | throws `EntityNotFoundException` |
| `findBy` | `Optional<T>` | returns `Optional.empty()` |
| `findAll` | `List<T>` | returns empty list, never `null` |

```java
public VetEntity retrieveById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + id));
}

public Optional<VetEntity> findByUsername(Username username) {
    return repository.findOne(Specifications.byUsername(username));
}

public List<VetEntity> findAll() {
    return repository.findAll();
}
```

Mutation operations use descriptive names without a fixed prefix:
`persist`, `activate`, `deactivate`, `cancel`.

### 2 — `Username` is a value object, not a `String`

Raw strings are opaque. A `Username` record makes method signatures self-documenting
and prevents accidental parameter swapping.

```java
// Ambiguous
public void cancel(String ownerUsername, Long appointmentId) { ... }

// Self-documenting
public void cancel(Username ownerUsername, Long appointmentId) { ... }
```

Do not re-wrap an already-typed `Username`:

```java
// REDUNDANT
vetService.retrieveByUsername(new Username(vetUsername.value()));

// CORRECT
vetService.retrieveByUsername(vetUsername);
```

### 3 — All queries use the Criteria API via a static `Specifications` inner class

Spring Data method name derivation is not used — those methods are stringly typed,
break silently on field renames, and cannot be composed.

Every repository declares a static inner class named `Specifications` with type-safe
factory methods built on the JPA Criteria API and the generated metamodel (`*_` classes).

```java
public interface VetJpaRepository
        extends JpaRepository<VetEntity, Long>, JpaSpecificationExecutor<VetEntity> {

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

Specifications are composable at the call site:

```java
// CORRECT — type-safe, composable, refactor-safe
repository.findOne(Specifications.byId(id).and(Specifications.byUsername(username)));

// FORBIDDEN — stringly typed, breaks on rename
repository.findByIdAndUsername(id, username.value());
```

The metamodel classes are generated by the JPA annotation processor in `pom.xml`.
Regenerate them after any entity field change.

### 4 — Mapping belongs to the layer that owns the result

| Mapping | Location |
|---------|----------|
| `Entity → DomainObject` | `logic/service/mapper/` |
| `Entity → DTO` | `web/controller/mapper/` |
| `DomainObject → DTO` | `web/controller/mapper/` or inline in controller |

A mapper in `logic/service/mapper/` may import from `persistence/entity` and
`logic/domain`. A mapper in `web/controller/mapper/` may import from
`persistence/entity`, `logic/domain`, and `web/dto`. Neither may import the other's
mapper.

### 5 — Prefer a domain interface over duplicating a record

When a domain type and a DTO would be structurally identical, define an interface in
`logic/domain` and have the DTO implement it. This avoids a redundant mapping step
while preserving the correct dependency direction.

```java
// Preferred when structures match
public record LocationResponse(...) implements LocationData { ... }
```

If the structures later diverge, replace the interface with a concrete type — the
service signature remains unchanged.

---

## What Is Intentionally Not Here

**Ports & Adapters (Hexagonal Architecture)** — not applied. Services depend directly on
JPA repositories and entities. Less indirection, less boilerplate, full testability via
`@DataJpaTest` and integration tests.

**MapStruct or similar mapping libraries** — not used. Explicit mappers communicate
intent more clearly than generated code.

**Separate `Request` and `Response` DTOs for all endpoints** — not enforced. Where
structures are identical, a single record implementing a `logic/domain` interface is
preferred. Separation is introduced only when the structures actually diverge.

**A dedicated domain object for every entity** — not enforced. Entities serve as domain
objects for simple aggregates. A parallel domain record is introduced only when a
service method performs a transformation the entity cannot represent. See constraint 3.
