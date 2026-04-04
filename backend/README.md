# PetcliniX Backend

## Getting Started

```bash
docker build --target production -t petclinix/spring-backend .
```

---
## Documentation & Diagrams

| Document | Description | Diagram |
|----------|-------------|---------|
| [Architecture Internals](docs/architecture-internals.md) | Design rationale, patterns, and conventions explained in depth | — |
| [C4 Layer Architecture](docs/c4-layer-architecture.puml) | Stacked C4 component diagram of all backend layers | [SVG](docs/c4-layer-architecture.svg) |
| [Layer Dependencies](docs/layer-dependencies.puml) | Package-level dependency diagram — allowed and forbidden dependencies | [SVG](docs/layer-dependencies.svg) |
| [Service Composition](docs/service-composition.puml) | Controller → Service wiring; data services vs orchestrating services | [SVG](docs/service-composition.svg) |
| [Entity Model](docs/entity-model.puml) | JPA entity class diagram with relationships and cascade rules | [SVG](docs/entity-model.svg) |
| [Sequence: Book Appointment](docs/sequence-book-appointment.puml) | End-to-end request flow through JWT filter, controller, and services | [SVG](docs/sequence-book-appointment.svg) |

> SVG files are generated from the PlantUML sources via `generate-diagrams.ps1` in the repo root.
                                                                                                                                                                                                                                                                                                                    
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
│── bootstrap 
│   └── AdminInitializer.java    # Data seeding for admin user
│
├── web                          # HTTP boundary
│   ├── controller               # REST controllers
│   │   └── mapper               # Entity/DomainObject → DTO mappers
│   ├── dto                      # Request and response records
│   └── advice                   # GlobalExceptionHandler
│
├── logic                        # Business logic — framework-agnostic
│   ├── domain                   # Domain objects, interfaces, enums, value objects
│   └── service                  # Service classes (use-cases)
│       └── mapper               # Entity → DomainObject mappers
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
| `retrieve` | single object | throws `NotFoundException` |
| `findBy` | `Optional<T>` | returns `Optional.empty()` |
| `findAll` | `List<T>` | returns empty list, never `null` |

```java
public VetEntity retrieveById(Long id) {
    return repository.findById(id)
        .map(VetMapper::toDomain)
        .orElseThrow(() -> new NotFoundException("Vet not found: " + id));
}

public Optional<VetEntity> findByUsername(Username username) {
    return repository.findOne(Specifications.byUsername(username))
        .map(VetMapper::toDomain);
}

public List<VetEntity> findAll() {
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

`LocationData` / `Location` is the concrete example in this codebase. `Location` is
both the domain record and the request/response body. `LocationService` accepts
`LocationData` — it never imports `Location` directly. If the API shape and domain
shape ever diverge, a dedicated `LocationRequest` implementing `LocationData` is
introduced in `web/dto`. The service signature does not change.

```java
// Current: structures are identical — one record serves both roles
public record Location(...) implements LocationData { ... }

// Future: structures diverge — introduce a request type, service unchanged
public record LocationRequest(...) implements LocationData { ... }
public record Location(...) { ... }  // domain record, no longer the DTO
```

### 6 — Exceptions are domain types, not framework types

Services must not throw JPA or Spring exceptions. `jakarta.persistence.EntityNotFoundException`
is a persistence-layer class — throwing it from `logic/service` couples the logic layer
to the persistence framework and leaks an infrastructure concept into the domain.

All exceptional conditions originate from `logic/domain/exception/`:

```
logic/domain/exception/
  PetclinixException.java      (abstract base)
  NotFoundException.java    (extends PetclinixException — resource does not exist)
```

`retrieve*` methods throw `NotFoundException` when the requested resource is absent.
`GlobalExceptionHandler` in the web layer maps domain exceptions to HTTP responses:

```java
NotFoundException  →  404
PetclinixException    →  422
```

When a new business rule violation needs an exception, extend `PetclinixException`:

```java
// CORRECT — pure domain type, no framework imports
public class ConflictException extends PetclinixException {
    public ConflictException(String message) { super(message); }
}
```

`GlobalExceptionHandler` already handles all `PetclinixException` subtypes via the 422
handler. No change to the handler is needed unless the new exception requires a
different HTTP status.

---

## Testing

Two test types cover the backend. Each targets exactly one layer and mocks everything below it.

### Controller slice tests — `@WebMvcTest`

**Purpose:** verify the HTTP contract. A controller slice test answers three questions:

1. Does the endpoint deserialise the request body correctly?
2. Does the response body contain the expected JSON field names and types?
3. Are the security constraints enforced (correct role required, 403 on wrong role, 401 on no auth)?

Business logic is not tested here — the service is mocked.

**Class header:** every `*ControllerIntegrationTest` must open with a Javadoc that identifies it as a slice test and states its scope:

```java
/**
 * Slice test for {@link PetsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(PetsController.class)
@Import(SecurityConfig.class)   // required for @PreAuthorize to be active in the slice
class PetsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PetService petService;
}
```

`@Import(SecurityConfig.class)` is required because `@EnableMethodSecurity` lives there. Without it `@PreAuthorize` is not evaluated and role-enforcement tests always pass.

**Required test cases — every endpoint must cover both paths:**

| Scenario | How |
|---|---|
| Happy path — correct role | `@WithMockUser(roles = "OWNER")` + assert status 2xx + assert JSON fields |
| Wrong role | `@WithMockUser(roles = "VET")` + assert status 403 |
| No authentication | no `@WithMockUser` + assert status 401 |
| Invalid request body | send malformed / missing-field JSON + assert status 400 |
| Service throws `NotFoundException` | `when(...).thenThrow(new NotFoundException(...))` + assert status 404 |

Both the happy path and the primary error path must be covered. "The endpoint works" is not a complete test; "the endpoint returns 404 when the resource does not exist" is equally required.

**Example:**

```java
/** Returns 200 with a JSON array of pets belonging to the authenticated owner. */
@Test
@WithMockUser(username = "owner1", roles = {"OWNER"})
void retrieveAllReturnsOkWithPetList() throws Exception {
    //arrange
    when(petService.findAllByOwner(new Username("owner1")))
        .thenReturn(List.of(new Pet(1L, "kittycat", "CAT", "FEMALE", null)));

    //act + assert
    mockMvc.perform(get("/pets").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].username").value("kittycat"));
}

/** Returns 401 when no authentication header is present. */
@Test
void retrieveAllWithoutAuthReturns401() throws Exception {
    //act + assert
    mockMvc.perform(get("/pets"))
        .andExpect(status().isUnauthorized());
}

/** Returns 403 when the authenticated user has the VET role instead of OWNER. */
@Test
@WithMockUser(roles = {"VET"})
void retrieveAllWithVetRoleReturns403() throws Exception {
    //act + assert
    mockMvc.perform(get("/pets"))
        .andExpect(status().isForbidden());
}
```

**File naming:** `XxxControllerIntegrationTest.java` in `src/test/java/.../web/controller/`.

Every controller class must have a corresponding `*ControllerIntegrationTest`.

### Controller unit tests — when a controller contains logic

Controllers should contain no business logic. When a controller unavoidably does contain branching logic — as `AuthController` does, deciding between a 200 and a 401 based on the `Optional` returned by `UserService.authenticate()` — that logic must be covered by a plain unit test in addition to the slice test.

```java
/**
 * Unit test for the branching logic inside {@link AuthController}.
 *
 * The slice test ({@link AuthControllerIntegrationTest}) covers JSON and HTTP annotations.
 * This test covers the conditional paths that cannot be expressed through MockMvc alone.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    UserService userService;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    AuthController authController;

    /** Returns 200 and a JWT token when credentials are valid. */
    @Test
    void loginWithValidCredentialsReturns200WithToken() {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        when(userService.authenticate(new Username("alice"), "secret"))
            .thenReturn(Optional.of(domainUser));
        when(jwtUtil.generateToken(domainUser)).thenReturn("jwt-token");

        //act
        var response = authController.login(new LoginRequest("alice", "secret"));

        //assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((LoginResponse) response.getBody()).token()).isEqualTo("jwt-token");
    }

    /** Returns 401 when credentials do not match any active user. */
    @Test
    void loginWithInvalidCredentialsReturns401() {
        //arrange
        when(userService.authenticate(new Username("alice"), "wrong"))
            .thenReturn(Optional.empty());

        //act
        var response = authController.login(new LoginRequest("alice", "wrong"));

        //assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
```

---

### Service unit tests — `@ExtendWith(MockitoExtension.class)`

**Purpose:** verify business logic in complete isolation from HTTP and the database.

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    PetJpaRepository repository;

    @InjectMocks
    PetService petService;

    @Test
    void persist_savesEntityAndReturnsDomainRecord() {
        //arrange
        ...
        //act
        ...
        //assert
        ...
    }
}
```

**File naming:** `XxxServiceTest.java` in `src/test/java/.../logic/service/`.

---

### Repository integration tests — `@DataJpaTest`

**Purpose:** verify that each `Specifications` factory method and every custom repository
query (`AppointmentRepositoryCustomImpl.countPerVet`) produces the correct results against
a real database.

`@DataJpaTest` loads only the JPA slice — entities, repositories, and the H2 in-memory
database. No services, no controllers, no mocking. Data is arranged by persisting entities
directly via `@Autowired` repositories.

```java
/**
 * Integration test for {@link PetJpaRepository}.
 *
 * Verifies each Specification executes correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via @DataJpaTest.
 */
@DataJpaTest
class PetJpaRepositoryIntegrationTest {

    @Autowired PetJpaRepository petRepository;
    @Autowired OwnerJpaRepository ownerRepository;

    /** Returns all pets belonging to the given owner entity. */
    @Test
    void byOwnerFindsAllPetsForThatOwner() {
        //arrange
        var owner = ownerRepository.save(new OwnerEntity("grace", "hash"));
        petRepository.save(new PetEntity("Fluffy", owner));

        //act
        var results = petRepository.findAll(PetJpaRepository.Specifications.byOwner(owner));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Fluffy");
    }
}
```

**What must be tested:** every `Specifications` method and every custom query method.
Happy path is sufficient — these are structural queries, not business logic; error paths
(empty results) are covered by at least one additional test per class to confirm
the query does not produce false matches.

**What is NOT needed:** error-path edge cases beyond a basic "no match returns empty"
check, transaction management (handled by `@DataJpaTest`), or mocking.

**File naming:** `XxxJpaRepositoryIntegrationTest.java` in
`src/test/java/.../persistence/jpa/`.

---

### Service unit tests — `@ExtendWith(MockitoExtension.class)`

**Purpose:** verify business logic in complete isolation from HTTP and the database.
Every public method must have at least one test; each significant branch (happy path and
NotFoundException path) must be covered separately.

**What is mocked:** direct collaborators only — repositories for data services, other
services for orchestrating services. Never both in the same test class.

**Mappers:** inject the real mapper instance (e.g. `EntityMapper`, `LocationMapper`).
These are pure static utility classes with no side effects; mocking them adds noise
without value.

**Package location:** same package as the service under test (`tech.petclinix.logic.service`).
This is required so that package-private methods (marked `/* default */`) are accessible
without reflection.

**File naming:** `XxxServiceTest.java` in `src/test/java/.../logic/service/`.

---

### Entity relation tests — `@DataJpaTest`

**Purpose:** verify that `@OneToMany` cascade and `orphanRemoval` declarations behave
as declared at the database level. These tests confirm that the JPA annotations are
correct and that the ORM propagates operations as expected.

**One test per ToMany relationship:** one test for cascade save (does saving the parent
persist the children?) and one test for orphan removal behaviour (does clearing the
collection delete the rows when `orphanRemoval = true`? Does the row survive when
`orphanRemoval` is absent?).

**What is NOT needed:** service or controller logic — this is purely a JPA mapping
verification. Happy path only; no mocking.

**Use `entityManager.flush()` followed by `entityManager.clear()`** before asserting
on reloaded state. Without `clear()`, the first-level cache returns the in-memory entity
rather than reloading from the database, masking whether the operation actually reached
the DB.

**File naming:** `XxxEntityIntegrationTest.java` in
`src/test/java/.../persistence/entity/`.

---

### Test method structure — Arrange / Act / Assert

Every test method uses inline comments to mark the three phases:

```java
@Test
void someTest() throws Exception {
    //arrange
    // set up mocks, build request data

    //act
    // call the method or perform the request

    //assert
    // verify the outcome
}
```

When act and assert are a single fluent MockMvc chain, combine them:

```java
    //act + assert
    mockMvc.perform(get("/pets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
```

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
