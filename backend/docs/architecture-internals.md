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

### The problem with `instanceof`

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

This works, but it has a maintenance problem: if a fourth subtype is added (`StaffEntity`,
for example), the compiler does not tell you that this block needs to be updated. Every
`instanceof` chain in the codebase silently becomes incomplete.

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

Now, code that needs to behave differently per subtype implements `UserVisitor<T>` as a
lambda or anonymous class. `UserMapper` uses it to determine the `UserType`:

```java
entity.accept(new UserVisitor<UserType>() {
    public UserType visitOwner(OwnerEntity o) { return UserType.OWNER; }
    public UserType visitVet(VetEntity v)     { return UserType.VET;   }
    public UserType visitAdmin(AdminEntity a) { return UserType.ADMIN; }
});
```

The compiler enforces that all three branches are implemented. If `StaffEntity` is added
and `UserVisitor` gains a `visitStaff` method, every existing implementation of
`UserVisitor` fails to compile until it handles the new case. The `instanceof` chain
would not catch this.

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

### The petclinix exception hierarchy

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
