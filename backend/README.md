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

### 3 — Services are the transaction boundary — no entity crosses into the web layer

Services in `logic/service` are the transaction boundary of the application. A JPA entity
must never appear in a service's public method signature. All association traversal and
lazy loading must be completed inside the service, within the active transaction, before
the result is returned to the caller.

`@Transactional` is permitted — and expected — on service methods that require it.

The return type of every public service method must be a plain Java record or interface
from `logic/domain`, a primitive, or a standard JDK type. This guarantees that the web
layer can never trigger lazy loading after the transaction has closed.

```java
// CORRECT — entity resolved and mapped inside the transaction
@Transactional(readOnly = true)
public List<Pet> findAllByOwner(Username ownerUsername) {
    return repository.findAll(Specifications.byOwnerUsername(ownerUsername))
        .stream()
        .map(PetMapper::toDomain)
        .toList();
}

// FORBIDDEN — entity escapes the transaction boundary
public List<PetEntity> findAllByOwner(Username ownerUsername) { ... }
```

### 4 — The public service API uses domain records, never persistence entities

Service **method signatures** must use types from `logic/domain`, primitives, or standard
JDK types. Inside service implementations, entities are expected and correct — the
restriction is on the public boundary only.

When a service returns data that maps directly to a single entity with no transformation,
define a plain record in `logic/domain` that mirrors the entity fields and map to it.
This is not duplication — it is the explicit, annotation-free representation of the data
that crosses the transaction boundary.

```java
// logic/domain/Pet.java — plain record, no annotations, no framework imports
public record Pet(Long id, String name, String breed, PetType type) {}

// CORRECT — domain record returned, entity stays inside the transaction
public List<Pet> findAllByOwner(Username ownerUsername) {
    return repository.findAll(Specifications.byOwnerUsername(ownerUsername))
        .stream()
        .map(PetMapper::toDomain)
        .toList();
}

// CORRECT — transformation exists, aggregated domain type returned
public StatsData getStats() { ... }

// FORBIDDEN — entity crosses the transaction boundary
public List<PetEntity> findAllByOwner(Username ownerUsername) { ... }

// FORBIDDEN — web DTO crosses the service boundary
public List<PetResponse> findAllByOwner(Username ownerUsername) { ... }
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

// web/dto/LocationRequest.java
public record LocationRequest(...) implements LocationData { ... }

// CORRECT
public Location persist(Username username, LocationData data) { ... }

// FORBIDDEN
public Location persist(Username username, LocationRequest dto) { ... }
```

When request and response structures diverge, introduce a dedicated `LocationRequest`
record that also implements `LocationData`. The service signature does not change.

### 6 — Services receive `Username`, not `Authentication`

Authentication is complete by the time a controller is reached. Services receive a
`Username` value object — a domain concept — not a Spring Security type.

```java
// CORRECT
public List<Location> findAllByVet(Username vetUsername) { ... }

// FORBIDDEN
public List<Location> findAllByVet(Authentication authentication) { ... }
```

Controllers extract the username and wrap it:

```java
locationService.findAllByVet(new Username(authentication.getName()));
```

### 7 — Web mappers translate domain records to DTOs

Controllers receive domain records from services and pass them to mappers in
`web/controller/mapper/` to produce response DTOs. Controllers must not reference
persistence types directly.

When the domain record and the DTO are structurally identical, the domain record itself
can be returned directly from the controller — no mapping step is needed.

```java
// CORRECT — domain record returned directly when structure matches DTO
return ResponseEntity.ok(petService.findAllByOwner(username));

// CORRECT — mapper produces a richer DTO from the domain record
return ResponseEntity.ok(
    vetService.findAll().stream()
        .map(DtoMapper::toVetResponse)
        .toList()
);

// FORBIDDEN — importing or naming a persistence entity in the controller
VisitEntity visit = vetVisitService.retrieveByVetAndId(...);
```

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
public Appointment persist(Username ownerUsername, AppointmentData data) {
    Pet pet = petService.retrieveByOwnerAndId(ownerUsername, data.petId());
    Vet vet = vetService.retrieveById(data.vetId());
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

    @Transactional(readOnly = true)
    public List<Location> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveEntityByUsername(vetUsername);
        return repository.findAll(Specifications.byVet(vet))
            .stream()
            .map(LocationMapper::toDomain)
            .toList();
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
public Vet retrieveById(Long id) {
    return repository.findById(id)
        .map(VetMapper::toDomain)
        .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + id));
}

public Optional<Vet> findByUsername(Username username) {
    return repository.findOne(Specifications.byUsername(username))
        .map(VetMapper::toDomain);
}

public List<Vet> findAll() {
    return repository.findAll().stream()
        .map(VetMapper::toDomain)
        .toList();
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
| `Entity → DomainRecord` | `logic/service/mapper/` |
| `DomainRecord → DTO` | `web/controller/mapper/` or inline in controller |

A mapper in `logic/service/mapper/` may import from `persistence/entity` and
`logic/domain`. A mapper in `web/controller/mapper/` may import from `logic/domain`
and `web/dto`. Neither may import the other's mapper.

Web mappers never see persistence entities. By the time data reaches the web layer it is
already a domain record — the transaction is closed and all associations are resolved.

### 5 — Prefer a domain interface over duplicating a record

When a domain type and a DTO would be structurally identical, define an interface in
`logic/domain` and have the DTO implement it. This avoids a redundant mapping step
while preserving the correct dependency direction.

```java
// Preferred for write operations where request and domain structures match
public record LocationRequest(...) implements LocationData { ... }
```

If the structures later diverge, replace the interface with a concrete domain record —
the service signature remains unchanged.

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

**A dedicated domain object for every entity** — not enforced as a separate class hierarchy.
Domain records in `logic/domain` are plain Java records that mirror the data needed by the
use case, not necessarily every field of the entity. A domain record is introduced
whenever a service must expose data to the web layer — which is always. The entity itself
never crosses the service boundary.
