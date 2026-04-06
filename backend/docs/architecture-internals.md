# Architecture Internals

This document explains the non-obvious design decisions in the backend. Each section
describes what a pattern is, why it was chosen, and what breaks if it is not followed.

---

## 1. JPA Metamodel and the Specifications Pattern

### The problem with string-based queries

Spring Data JPA allows repositories to derive queries from method names:

```java
List<PetEntity> findByOwnerUsername(String username);
```

This looks convenient but has a hidden cost: the string `"owner"` and `"username"` are
not checked by the compiler. If `PetEntity.owner` is renamed to `PetEntity.user`, the
derived query method silently breaks at runtime — no compile error, no IDE warning.

The same applies to JPQL strings in `@Query` annotations:

```java
@Query("SELECT p FROM PetEntity p WHERE p.owner.username = :u")
```

Again, `"owner"` and `"username"` are opaque strings. A field rename breaks this at
startup, not at compile time.

### The Criteria API

The JPA Criteria API solves this by building queries from Java objects instead of
strings. A query that filters pets by owner username looks like this:

```java
(root, query, cb) -> {
    Path<OwnerEntity> ownerPath = root.get(PetEntity_.owner);
    return cb.equal(ownerPath.get(OwnerEntity_.username), ownerUsername.value());
}
```

`root.get(PetEntity_.owner)` is a method call with a compile-time constant
`PetEntity_.owner`. If the field is renamed, the compiler tells you immediately.

### The metamodel — what are the `*_` classes?

`PetEntity_`, `OwnerEntity_`, `AppointmentEntity_` — these are **metamodel classes**,
generated automatically by the JPA annotation processor at build time. They contain
one static attribute per mapped field of the corresponding entity:

```java
// generated — do not edit
public abstract class PetEntity_ {
    public static volatile SingularAttribute<PetEntity, Long>        id;
    public static volatile SingularAttribute<PetEntity, String>      name;
    public static volatile SingularAttribute<PetEntity, OwnerEntity> owner;
    public static volatile SingularAttribute<PetEntity, Species>     species;
    // ...
}
```

These attributes are typed. `PetEntity_.owner` knows it is a `SingularAttribute` of
type `OwnerEntity`. The Criteria API uses this type information to prevent cross-type
comparisons at compile time.

**The metamodel is generated during `mvn compile`.** After any change to an entity field
(adding, removing, or renaming), run `mvn compile` (or the full `mvn clean install`) to
regenerate the `*_` classes. If you skip this step, the old attribute constant may still
compile but point to a non-existent field, causing a runtime error.

The annotation processor is configured in `pom.xml` under the `hibernate-jpamodelgen`
dependency.

### The `Specifications` inner class

Each repository in `persistence/jpa/` contains a static inner class named
`Specifications`:

```java
public interface PetJpaRepository extends JpaRepository<PetEntity, Long>,
        JpaSpecificationExecutor<PetEntity> {

    class Specifications {
        public static Specification<PetEntity> byOwner(OwnerEntity owner) {
            return (root, query, cb) ->
                    cb.equal(root.get(PetEntity_.owner), owner);
        }

        public static Specification<PetEntity> byOwnerUsername(Username ownerUsername) {
            return (root, query, cb) -> {
                Path<OwnerEntity> ownerPath = root.get(PetEntity_.owner);
                return cb.equal(ownerPath.get(OwnerEntity_.username), ownerUsername.value());
            };
        }
    }
}
```

Keeping specifications as static methods inside the repository that owns them means all
queries for a given entity are defined in one place. They are composed at the call site:

```java
// Single condition
repository.findAll(Specifications.byOwner(owner));

// Composed — AND two conditions together
repository.findOne(
    Specifications.byOwnerUsername(username).and(Specifications.byId(petId))
);
```

`JpaSpecificationExecutor<T>` is the second interface the repository extends. It
provides `findAll(Specification<T>)`, `findOne(Specification<T>)`, and
`count(Specification<T>)`. Without it, the repository cannot accept `Specification`
arguments.

---

## 2. Transaction Boundaries and Lazy Loading

### What a transaction and a session are

A **database transaction** is a unit of work that is either fully committed or fully
rolled back. In Spring, `@Transactional` wraps a method so that everything inside it
runs within one transaction.

A JPA **session** (Hibernate calls it a `Session`; JPA calls it a
`PersistenceContext`) is the object that tracks all entities loaded within a transaction.
It caches entity state, detects changes (dirty checking), and defers writes until flush
time. The session opens when the transaction starts and closes when it ends.

### Why lazy loading exists

Every entity association (`@ManyToOne`, `@OneToMany`, etc.) can be loaded eagerly or
lazily. `FetchType.EAGER` loads the associated objects immediately when the parent is
loaded. `FetchType.LAZY` loads them only when their getter is first called.

All associations in this codebase use `FetchType.LAZY`:

```java
// AppointmentEntity.java
@ManyToOne(optional = false, fetch = FetchType.LAZY)
private VetEntity vet;

@ManyToOne(optional = false, fetch = FetchType.LAZY)
private PetEntity pet;
```

Lazy loading is the correct default because it prevents loading the entire object graph
every time an entity is queried. Fetching an appointment should not automatically load
the full vet entity, the vet's locations, and every pet's owner — unless that data is
actually needed.

### LazyInitializationException

Loading a lazy association requires an active session. If the session has already
closed, Hibernate throws `LazyInitializationException`:

```
org.hibernate.LazyInitializationException:
  could not initialize proxy - no Session
```

This happens whenever code accesses a lazy association outside a `@Transactional`
boundary. The typical scenario in this codebase:

```java
// OwnerAppointmentService — no @Transactional
public List<Appointment> findAllByOwner(Username ownerUsername) {
    return appointmentService.findAllByOwner(ownerUsername)  // session closes here
            .stream()
            .map(EntityMapper::toAppointment)                // accesses .getVet(), .getPet()
            .toList();                                       // LazyInitializationException
}
```

`appointmentService.findAllByOwner()` executes the query and returns the list.
The session opened for that query closes when the repository method returns.
`EntityMapper.toAppointment()` then calls `a.getVet().getId()` and `a.getPet().getId()`
on the next line — but the session is gone. Hibernate cannot load the proxies.

**The fix** is `@Transactional(readOnly = true)` on the calling method. This keeps
one session open for the entire method, covering both the query and the mapper traversal.

### Why H2 tests do not reveal this

The integration tests run with `@SpringBootTest` and `@AutoConfigureMockMvc`. The test
itself runs inside a Spring-managed transaction, which keeps the session alive through
the entire test body — including the HTTP call into the controller and back. H2 also
defaults to certain fetch behaviours that differ from MariaDB. As a result, the
`LazyInitializationException` does not surface in the test environment. It only appears
in production with MariaDB under real request handling, where no outer transaction wraps
the controller call.

This is one of the most common classes of bugs in Spring/JPA applications: tests green,
production broken.

### `@Transactional(readOnly = true)` vs `@Transactional`

`readOnly = true` is not just a hint. When Hibernate knows a transaction is read-only,
it skips dirty checking (scanning all loaded entities for changes at flush time) and
disables snapshot copies. For read-heavy methods that load multiple entities, this is a
meaningful performance improvement. Use `readOnly = true` on every method that only
reads data, `@Transactional` (without flag) on every method that writes.

### The self-invocation trap

Spring implements `@Transactional` via a proxy. When a bean is injected, the caller
receives a proxy wrapping the actual instance. The proxy intercepts the method call,
opens a transaction, calls the real method, and commits or rolls back.

This means `@Transactional` only works when the method is called **from outside the
class**. If a method inside a class calls another `@Transactional` method in the same
class, the call bypasses the proxy and no transaction is opened:

```java
@Service
public class SomeService {

    public void outer() {
        inner(); // BYPASSES the proxy — @Transactional on inner() has no effect
    }

    @Transactional
    public void inner() { ... }
}
```

The fix is to either put `@Transactional` on `outer()`, or move `inner()` to a separate
bean so that the call goes through a proxy.

---

## 3. Single-Table Inheritance and the Visitor Pattern

### Why single-table inheritance

`UserEntity` is abstract. Three concrete subtypes extend it: `OwnerEntity`,
`VetEntity`, `AdminEntity`. JPA supports several strategies for mapping an inheritance
hierarchy to database tables:

- **`SINGLE_TABLE`** — one table for all subtypes, with a discriminator column
- **`JOINED`** — one table per class, joined on `id`
- **`TABLE_PER_CLASS`** — one full table per concrete class

`SINGLE_TABLE` is used here:

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class UserEntity { ... }

@DiscriminatorValue("O")
public class OwnerEntity extends UserEntity { ... }

@DiscriminatorValue("V")
public class VetEntity   extends UserEntity { ... }

@DiscriminatorValue("A")
public class AdminEntity extends UserEntity { ... }
```

The `users` table has a `type` column. Each row holds `O`, `V`, or `A`. When JPA loads
a `UserEntity`, it reads the discriminator and instantiates the correct subtype.

`SINGLE_TABLE` was chosen because:
- All user types share most columns (`username`, `passwordHash`, `active`)
- Queries against all users (`findAll`, `findByUsername`) need no join
- The schema stays simple

The trade-off: nullable columns for subtype-specific fields (e.g. `OwnerEntity` has
`pets`, `VetEntity` has `locations` — but those are mapped as separate tables via
`@OneToMany`, not columns, so there is no null column issue here).

### Why `instanceof` does not work with JPA

When code receives a `UserEntity` reference and needs to do different things depending
on the subtype, the tempting approach is:

```java
if (entity instanceof OwnerEntity owner) {
    return UserType.OWNER;
} else if (entity instanceof VetEntity vet) {
    return UserType.VET;
} else if (entity instanceof AdminEntity admin) {
    return UserType.ADMIN;
} else {
    throw new IllegalStateException("Unknown user type");
}
```

This looks reasonable but breaks silently with JPA. Hibernate does not always return the
concrete subtype you declared — it returns a **proxy**: a runtime-generated subclass of
the declared type, used to support lazy loading. When a `UserEntity` is loaded as part
of a relationship (e.g. an `AppointmentEntity` loading its associated user), the object
in memory at runtime is not an `OwnerEntity` — it is something like
`OwnerEntity$HibernateProxyXyz`. The `instanceof OwnerEntity` check returns `false`
because the proxy class is not `OwnerEntity` itself, even though it represents one.

`getClass() == OwnerEntity.class` has the same problem for the same reason.

Hibernate provides `Hibernate.getClass(entity)` and
`HibernateProxyHelper.getClassWithoutInitializingProxy(entity)` as workarounds, but both
require importing `org.hibernate.*` directly into domain or service code — a hard binding
to the Hibernate implementation that this codebase explicitly forbids.

There is no correct `instanceof`-based solution that does not either break under proxies
or import a Hibernate class.

**The maintenance problem on top of the proxy problem:** even if proxies were not an
issue, the `instanceof` chain has a second flaw. If a fourth subtype is added
(`StaffEntity`, for example), the compiler does not tell you that this block needs
updating. Every `instanceof` chain in the codebase silently becomes incomplete.

### The Visitor pattern

`UserEntity` declares an abstract method:

```java
public abstract <T> T accept(UserVisitor<T> visitor);
```

Each subtype implements it by calling the corresponding method on the visitor:

```java
// OwnerEntity
public <T> T accept(UserVisitor<T> visitor) { return visitor.visitOwner(this); }

// VetEntity
public <T> T accept(UserVisitor<T> visitor) { return visitor.visitVet(this); }

// AdminEntity
public <T> T accept(UserVisitor<T> visitor) { return visitor.visitAdmin(this); }
```

`UserVisitor<T>` is an interface:

```java
public interface UserVisitor<T> {
    T visitOwner(OwnerEntity owner);
    T visitVet(VetEntity vet);
    T visitAdmin(AdminEntity admin);
}
```

Code that needs to behave differently per subtype implements `UserVisitor<T>` as a
lambda or anonymous class. `UserMapper` uses it to determine the `UserType`:

```java
entity.accept(new UserVisitor<UserType>() {
    public UserType visitOwner(OwnerEntity o) { return UserType.OWNER; }
    public UserType visitVet(VetEntity v)     { return UserType.VET;   }
    public UserType visitAdmin(AdminEntity a) { return UserType.ADMIN; }
});
```

**Why this solves the proxy problem.** The dispatch happens through a virtual method
call on the entity itself — `entity.accept(visitor)`. No type inspection is involved.
The entity knows what it is and calls the correct `visit` overload directly. Whether
`entity` is a real `OwnerEntity` or a Hibernate proxy wrapping one, the proxy forwards
the `accept()` call to the underlying object, which calls `visitor.visitOwner(this)`.
The correct branch is reached regardless of proxy wrapping — no `instanceof`, no
`getClass()`, no Hibernate imports anywhere outside the entity package.

**Why this also solves the compiler-exhaustiveness problem.** `UserVisitor<T>` is an
interface with one method per subtype. If `StaffEntity` is added and `visitStaff` is
added to the interface, every existing implementation of `UserVisitor` fails to compile
until it handles the new case. The `instanceof` chain would not catch this.

---

## 4. The `entityManager.flush()` Call in `LocationService`

### Cascade and orphan removal

`LocationEntity` owns two child collections:

```java
@OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OpeningPeriodEntity> weeklyPeriods = new ArrayList<>();

@OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OpeningOverrideEntity> overrides = new ArrayList<>();
```

`cascade = CascadeType.ALL` means any persist, merge, or remove operation on the parent
propagates to the children. `orphanRemoval = true` means if a child is removed from
the collection, it is automatically deleted from the database.

### The update problem

When updating a location's schedule, the service replaces the entire collections:

```java
locationEntity.getWeeklyPeriods().clear();  // marks children as orphans → DELETE
locationEntity.getOverrides().clear();

// ... then add new children
locationData.weeklyPeriods().stream()
    .map(p -> new OpeningPeriodEntity(...))
    .forEach(locationEntity.getWeeklyPeriods()::add);  // → INSERT
```

The problem is that the new periods may have the same `(location_id, dayOfWeek, sortOrder)`
combination as the old ones, and if a unique constraint covers those columns, the
database will reject the INSERT because the old row has not yet been DELETEd. JPA
accumulates changes and flushes them to the database in bulk — it is allowed to
reorder operations within a flush. The INSERT may be sent before the DELETE.

### Why `flush()` solves it

```java
locationEntity.getWeeklyPeriods().clear();
locationEntity.getOverrides().clear();
entityManager.flush(); // forces all pending DELETEs to execute NOW
                       // before any INSERTs are generated
```

`entityManager.flush()` writes all pending changes to the database immediately, within
the current transaction, without committing. After this call, the old rows are deleted
and the unique constraint is satisfied. The subsequent INSERTs for the new periods
succeed.

Without `flush()`, the database would see both the old and new rows temporarily
coexisting and reject the insert. This is not a Hibernate bug — it is correct SQL
behaviour, and `flush()` is the standard solution.

---

## 5. Package-Private Methods as an Intra-Layer Boundary

### The problem

`AppointmentService` and `PetService` need to expose some methods to orchestrating
services within the same package (`OwnerAppointmentService`, `VetVisitService`), but
these methods return or accept JPA entity types. Making them `public` would allow the
web layer to call them and receive entity references — violating the architectural
constraint that entities must not reach the web layer.

### The solution: `/* default */` package-private visibility

```java
// AppointmentService.java
/* default */ List<AppointmentEntity> findAllByOwner(Username ownerUsername) { ... }
/* default */ AppointmentEntity persist(PetEntity pet, VetEntity vet, LocalDateTime startAt) { ... }
```

Java's default (package-private) visibility means the method is accessible to any class
in the same package, but not from outside. Since all services live in `logic.service`,
orchestrating services can call these methods. Controllers live in `web.controller` —
a different package — and cannot see them. The ArchUnit rule
`web_layer_must_not_depend_on_persistence` cannot be violated because the methods are
not visible from `web`.

The comment `/* default */` is intentional. Java's package-private visibility has no
keyword — the absence of an access modifier signals it. The comment makes this explicit
so that a reader does not assume it is an oversight.

### The trade-off

Package-private methods are harder to test in isolation. A unit test in a different
package cannot call them directly without reflection. The existing unit test for
`UserService` is in `tech.petclinix.logic.service` (same package), which is why it
can construct the service directly. Tests for `AppointmentService` must be in the same
package to access these methods.

This is acceptable: the package boundary is doing the same job a module boundary would
do in a larger system, and the integration tests cover the behaviour end-to-end.

---

## 6. Petclinix Exception Hierarchy

### The problem with framework exceptions

The most natural way to signal "resource not found" in a JPA application is to throw
`jakarta.persistence.EntityNotFoundException`. Every JPA tutorial does this. The problem
is that `jakarta.persistence` is a persistence-layer package. Importing it in
`logic/service` couples the logic layer to the persistence framework — the same coupling
the architecture explicitly prevents for entities, repositories, and annotations.

If the persistence technology is ever changed (e.g. from JPA to jOOQ), every service
that throws `EntityNotFoundException` needs to be updated, even though the business
logic did not change.

There is a second, subtler problem: `GlobalExceptionHandler` in the web layer was
catching `jakarta.persistence.EntityNotFoundException`. The web layer was coupled to a
persistence class. A change in how the persistence layer signals errors would require
a change in the web layer — two layers affected by one infrastructure decision.

### The PetcliniX exception hierarchy

All exceptions originate from `logic/domain/exception/`, which contains only pure Java:

```
logic/domain/exception/
  PetclinixException.java     (abstract — base for all business-rule exceptions)
  NotFoundException.java   (extends PetclinixException — a named resource does not exist)
```

```java
// PetclinixException.java
public abstract class PetclinixException extends RuntimeException {
    protected PetclinixException(String message) { super(message); }
}

// NotFoundException.java
public class NotFoundException extends PetclinixException {
    public NotFoundException(String message) { super(message); }
}
```

Services throw `NotFoundException` instead of `EntityNotFoundException`:

```java
// BEFORE — persistence framework class in logic layer
.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Vet not found: " + id));

// AFTER — pure domain class
.orElseThrow(() -> new NotFoundException("Vet not found: " + id));
```

### How exceptions reach the HTTP response

`GlobalExceptionHandler` in `web/advice/` maps domain exception types to HTTP status
codes. The web layer imports from `logic/domain` — which is explicitly allowed:

```java
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<String> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(404).body(ex.getMessage());
}

@ExceptionHandler(PetclinixException.class)
public ResponseEntity<String> handlePetclinixException(PetclinixException ex) {
    return ResponseEntity.status(422).body(ex.getMessage());
}
```

Spring matches the most specific handler first. A `NotFoundException` is caught by the
404 handler, not the 422 handler, even though `NotFoundException` extends `PetclinixException`.

### Adding a new exception

When a new business rule violation needs its own exception, extend `PetclinixException`:

```java
// logic/domain/exception/ConflictException.java
public class ConflictException extends PetclinixException {
    public ConflictException(String message) { super(message); }
}
```

The 422 handler in `GlobalExceptionHandler` already covers all `PetclinixException`
subtypes. No change to the web layer is needed unless the new exception requires a
different HTTP status — in which case add a specific `@ExceptionHandler` for it, exactly
as was done for `NotFoundException`.

---

### The JPA wrapping rule — persistence exceptions must not cross the service boundary

#### The problem

`DataIntegrityViolationException` is thrown by Spring Data when the database rejects an
operation — most often because a unique constraint is violated. The natural place to catch
it is wherever the `repository.save()` call is made — which is inside the service.

The temptation is to let it propagate up to the controller and catch it there:

```java
// UsersController — WRONG
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
        var user = userService.register(...);
        return ResponseEntity.ok(toUserResponse(user));
    } catch (DataIntegrityViolationException e) {       // persistence type in the web layer
        return ResponseEntity.status(409).body("Username already taken");
    }
}
```

This violates the dependency rule. `DataIntegrityViolationException` lives in
`org.springframework.dao` — a persistence/data-access package. The web layer now imports
a class that is conceptually part of the persistence stack. If the persistence technology
changes (JPA replaced by jOOQ, or the constraint is moved to application code), the
controller has to change — even though the controller's job is only to handle HTTP.

#### Why "check first" does not fix it

The reflex fix is to query for the username before saving:

```java
// UserService — STILL WRONG
public DomainUser register(Username username, ...) {
    if (repository.exists(Specifications.byUsername(username))) {
        throw new UsernameAlreadyTakenException(username.value());
    }
    return UserMapper.toDomain(repository.save(user));
}
```

This introduces a **TOCTOU race condition** (Time Of Check To Time Of Use). Two concurrent
registration requests for the same username both pass the existence check, both proceed
to `save()`, and one of them still hits the constraint violation. You end up needing the
same exception handling as before, plus an extra query on every registration.

The check-first pattern gives a false sense of safety while adding a database round-trip.

#### The correct approach — translate at the service boundary

The service is the translation boundary between the persistence world and the domain world.
It is already the only layer that knows about `repository.save()`. It is the right place
to catch the persistence exception and re-throw a domain exception:

```java
// UserService — CORRECT
@Transactional
public DomainUser register(Username username, String rawPassword, UserType userType) {
    var user = switch (userType) { ... };
    try {
        return UserMapper.toDomain(repository.save(user));
    } catch (DataIntegrityViolationException e) {
        throw new UsernameAlreadyTakenException(username.value());
    }
}
```

`UsernameAlreadyTakenException` extends `PetclinixException` and lives in
`logic/domain/exception/`. It is a domain fact — "this username is unavailable" — not
a persistence artifact. The controller never sees `DataIntegrityViolationException`.
`GlobalExceptionHandler` maps `UsernameAlreadyTakenException` to 409 Conflict.

The result: the web layer is clean, the exception hierarchy is self-contained, and the
mapping from infrastructure error to HTTP status is centralised in one place.

#### The rule, stated plainly

**Any exception thrown by a JPA repository or by the persistence framework must be caught
inside the service method that triggered it and re-thrown as a subtype of
`PetclinixException` before the method returns.** Persistence exception types must not
appear in `import` statements anywhere in `web/` or `logic/`.

---

## 7. Testing Strategy — Tests as Living Documentation

### Tests are the contract, not a check

A passing test suite is not just a signal that the code works today. It is the
machine-readable specification of what the system is supposed to do. When a new
developer joins, the tests tell them: which roles can call which endpoints, what JSON
the API produces, what happens when a resource does not exist, what the service does
when authentication fails. No other document stays as up-to-date as a test that must
pass before a commit is accepted.

This is why two distinct test types are required, each with a fixed scope. Mixing scopes
produces tests that are hard to diagnose when they fail and easy to break when unrelated
code changes.

---

### Controller slice tests — `@WebMvcTest`

A controller's job is mechanical: deserialise the request, delegate to a service, serialise
the response, enforce security. None of this requires a database. Loading a full Spring
context with H2, JPA, and all services for a test that only checks whether a JSON field
is spelled `"startsAt"` and not `"startAt"` is unnecessary cost.

`@WebMvcTest` loads only the web slice: controllers, `@ControllerAdvice`, filters, and
security configuration. Everything else is absent. The service is replaced by a Mockito
mock via `@MockBean`.

```java
/**
 * Slice test for {@link PetsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(PetsController.class)
@Import(SecurityConfig.class)
class PetsControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean  PetService petService;
}
```

**Why `@Import(SecurityConfig.class)` is not optional.**
`@EnableMethodSecurity` is declared on `SecurityConfig`. Without importing it, the Spring
test context does not activate method-level security processing. `@PreAuthorize` annotations
on controllers are silently ignored. A test that asserts `status().isForbidden()` will
pass with the correct role and will also pass with the wrong role — the protection appears
to work but does not. Importing `SecurityConfig` ensures `@PreAuthorize("hasRole('OWNER')")`
is evaluated exactly as it is in production.

**What a controller integration test must cover.**
Every endpoint requires at minimum:

| Scenario | What it proves |
|---|---|
| Happy path, correct role | The service result is serialised correctly; the HTTP status is right |
| Wrong role | `@PreAuthorize` rejects the request with 403 |
| No authentication | Spring Security rejects the request with 401 |
| Service throws `NotFoundException` | `GlobalExceptionHandler` maps it to 404 |
| Invalid request body | Bean Validation rejects it with 400 |

Covering only the happy path proves that serialisation works on a sunny day. Covering
the error paths proves that the contract holds when things go wrong — which is when it
matters most.

---

### Controller unit tests — when a controller contains logic

Controllers should contain no branching logic. The rule exists because a controller
that makes decisions is harder to test, harder to reuse, and blurs the separation between
the HTTP layer and the business layer.

`AuthController` is an intentional exception: it inspects `Optional.isPresent()` and
chooses between a 200 and a 401 response. That conditional is the controller's own
decision, not the service's. The slice test verifies that the request deserialises and
the response serialises. It does not — and cannot cleanly — verify both branches of the
conditional through `MockMvc` alone.

A plain `@ExtendWith(MockitoExtension.class)` unit test calls the controller method
directly, without any HTTP machinery:

```java
/**
 * Unit test for the branching logic inside {@link AuthController}.
 *
 * AuthControllerTest covers JSON and HTTP annotations.
 * This test covers the conditional paths that cannot be expressed through MockMvc alone.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock UserService userService;
    @Mock JwtUtil     jwtUtil;
    @InjectMocks AuthController authController;

    /** Returns 200 and a JWT token when credentials are valid. */
    @Test
    void loginWithValidCredentialsReturns200WithToken() { ... }

    /** Returns 401 when credentials do not match any active user. */
    @Test
    void loginWithInvalidCredentialsReturns401() { ... }
}
```

The naming convention `*ControllerTest` distinguishes it from the slice test.

The deeper lesson: if writing a controller unit test feels necessary, it is a signal
that the logic belongs in the service, not in the controller. Treat it as a design smell
and move the logic down. The `AuthController` case is retained because the conditional
is inherently about the HTTP response code, not about a business rule.

---

### Service unit tests — `@ExtendWith(MockitoExtension.class)`

Service tests verify business rules in isolation. The database is not involved.
Repositories are mocked. This keeps each test fast and focused on one decision.

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock PetJpaRepository repository;
    @Mock OwnerService     ownerService;
    @InjectMocks PetService petService;
}
```

Service tests are the right place to verify: does `retrieveByOwnerAndId` throw
`NotFoundException` when the pet does not belong to the owner? Does `persist` call
`repository.save` with the correct entity? Does the mapper produce the correct domain
record from the entity?

The controller test would need to set up a full request/response cycle to reach the same
assertions. The service test does it in three lines.

---

### Test method naming and documentation

Test method names use camelCase. A one-sentence Javadoc above `@Test` states what the
test asserts — not how:

```java
/** Returns 404 when no pet with the given id belongs to the authenticated owner. */
@Test
void retrieveByIdWithUnknownIdReturns404() throws Exception { ... }
```

The Javadoc is the specification. The method name is a compact identifier.
The body is the proof.

This convention matters because test names appear in build output, CI reports, and IDE
test runners. `retrieveByIdWithUnknownIdReturns404` tells the reader immediately what
broke without opening the file. The Javadoc tells the reader why the behaviour matters.

---

### Why `@SpringBootTest` is not used for controller tests

`@SpringBootTest` starts the full application context: all beans, JPA, the datasource,
security, and everything in between. For a test that only needs to verify that a JSON
field is named correctly, this is many times more than necessary. It is also slower,
noisier (H2 startup logs), and requires database cleanup between tests.

More importantly, full-context tests mask layer violations. When a controller test
runs with the real service and the real repository, a bug in the service or mapper can
cause the controller test to fail — and the failure message points at the endpoint, not
at the actual source. Slice tests fail closer to the defect because they have fewer
moving parts.

`@SpringBootTest` remains appropriate for true end-to-end scenarios where the interaction
between layers is what is being tested. Controller slice tests are not that — they test
one layer with everything else mocked.

---

### Repository integration tests — `@DataJpaTest`

#### What must be tested

Every `Specifications` factory method in every `*JpaRepository` interface, and every
method in a custom repository implementation (currently
`AppointmentRepositoryCustomImpl.countPerVet()`), must have a `@DataJpaTest` integration
test. These tests confirm that:

1. The Criteria API predicate addresses the correct column and join path.
2. The JPA metamodel class (`*_`) references the right field.
3. The query returns the expected rows for a known data set.

#### Why `@DataJpaTest` and not `@SpringBootTest`

`@SpringBootTest` starts the full application context — all services, security,
controllers, and the datasource. A query test needs none of that. `@DataJpaTest` loads
only the JPA slice: the entity classes, the repositories, and an H2 in-memory database
that is reset between tests. This makes the tests fast (single-digit seconds) and
precise — a failure points directly at the query, not at a service or controller above it.

`@DataJpaTest` also wraps each test in a transaction that is rolled back at the end,
so no data cleanup is needed.

#### Test structure

Each test class arranges data by persisting entities directly through `@Autowired`
repositories, then invokes the specification or custom method, then asserts on the
results.

```java
@DataJpaTest
class LocationJpaRepositoryIntegrationTest {

    @Autowired LocationJpaRepository locationRepository;
    @Autowired VetJpaRepository      vetRepository;

    /** Returns all locations belonging to the given vet entity. */
    @Test
    void byVetFindsAllLocationsForThatVet() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        locationRepository.save(new LocationEntity(vet, "Clinic North", "Europe/Vienna"));

        //act
        var results = locationRepository.findAll(
                LocationJpaRepository.Specifications.byVet(vet));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Clinic North");
    }
}
```

#### What is NOT tested

- Error paths that depend on business rules — those belong in service unit tests.
- Transaction boundaries and lazy loading — those are covered by the `@SpringBootTest`
  controller integration tests and documented in section 2 of this file.
- Spring Data `findById` or `save` — these are framework methods, not application code.

#### File naming

`XxxJpaRepositoryIntegrationTest.java` in `src/test/java/tech/petclinix/persistence/jpa/`.

---

### Service unit tests — `@ExtendWith(MockitoExtension.class)`

#### Why this type, not another

A controller slice test exercises only the HTTP layer. A `@DataJpaTest` exercises only the
database layer. Neither is the right place for "does the service throw `NotFoundException`
when the pet does not belong to the owner?" — that question is about business logic, and
the answer must not depend on the database or on HTTP plumbing.

A Mockito unit test answers this directly: mock the repository to return `Optional.empty()`,
call the service method, assert `NotFoundException` is thrown. It is faster, more
targeted, and produces a failure message that points at the service — not at a controller
or a query above it.

#### What is covered

- Every public method of the service — at minimum one test per method.
- Each significant branch: happy path and the `NotFoundException` path.
- Correct delegation: orchestrating services verify that they call the right data service
  with the right arguments (using `verify()`).
- Correct mapping: mappers (`EntityMapper`, `LocationMapper`) are **not** mocked. They
  are pure static functions; injecting the real class verifies that the domain record is
  populated correctly from the entity.

#### What is NOT needed

- Database behaviour — that belongs in `@DataJpaTest` tests.
- HTTP status codes — those belong in `@WebMvcTest` slice tests.
- Testing the mapper in isolation — mappers are covered through the service tests that
  call them.

#### Package location and naming convention

Test classes live in `tech.petclinix.logic.service` — the **same package** as the service
under test. Java's package-private access (`/* default */`) makes certain intra-layer
methods invisible to classes in other packages. Placing tests in the same package gives
them access to these methods without reflection.

File naming: `XxxServiceTest.java` in `src/test/java/tech/petclinix/logic/service/`.

---

### Entity relation tests — plain JUnit5

#### What is being tested

Each `@OneToMany` relationship in this codebase requires a bidirectional back-pointer: the
child must hold a reference back to the parent via its `@ManyToOne` field. These tests
verify that the entity constructors and collection-manipulation methods keep both sides of
the relationship consistent **in Java**, with no database involved.

The question answered is: when I add a child to the parent's collection (or construct a
child with a parent reference), does the child's back-pointer point to the correct parent?

This is a structural contract of the entity code, not a database or ORM behaviour. No
Spring context, no H2, no mocking.

#### What is NOT covered here

- ORM cascade and orphan-removal behaviour — those are JPA annotations; verifying them
  requires a real database and belongs in `@DataJpaTest` repository integration tests if
  that coverage is ever needed.
- Business logic — belongs in service unit tests.

#### Test structure

One test class per entity that owns a `@OneToMany` relationship. Each test:
1. Constructs the parent entity.
2. Constructs the child entity with the parent reference, or adds the child to the parent's
   collection.
3. Asserts that the child's `@ManyToOne` field points to the same parent instance (`isSameAs`).

```java
/**
 * Unit test for {@link LocationEntity}.
 *
 * Verifies that the {@code @OneToMany weeklyPeriods} and {@code @OneToMany overrides}
 * collections maintain the back-pointer to the owning location correctly in Java.
 * No database involved.
 */
class LocationEntityTest {

    /** Adding a period to the collection is consistent with the period's back-pointer. */
    @Test
    void addingPeriodToCollectionMatchesItsBackPointer() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var period = new OpeningPeriodEntity(location, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);

        //act
        location.getWeeklyPeriods().add(period);

        //assert
        assertThat(location.getWeeklyPeriods()).contains(period);
        assertThat(period.getLocation()).isSameAs(location);
    }
}
```

#### File naming

`XxxEntityTest.java` in `src/test/java/tech/petclinix/persistence/entity/`.
