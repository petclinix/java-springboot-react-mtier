# PetcliniX

## Getting Started with Petclinix SpringBoot App

## Build Docker Image

To build a Docker image for the Springboot app, you can use the following command:

```bash
docker build --target production -t petclinix/spring-backend .
```

# Architecture & Design Conventions
## Concept

The PetClinix bakend is a Spring Boot application using a multitierd layered architecture with a pragmatic approach.  
It uses a  clear dependency direction, minimal boilerplate, no over-engineering. The goal is to show that good architecture 
emerges from consistent rules applied to real problems — not from maximising abstraction.


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

### `web` presentation layer
Owns the HTTP contract. Controllers receive requests, delegate to services, and map results
to DTOs. No business logic lives here. No persistence types cross this boundary outward.

### `logic` logic layer
Owns the domain model and use-cases. Services orchestrate repositories and enforce business
rules. This layer has no knowledge of HTTP, JSON, or JPA annotations. It is the only layer
that all other layers may depend on for domain types. Data objects may
implement domain interfaces from `logic/domain` to avoid redundant mapping where the
structures are identical.

### `persistence` model layer
Owns the database schema. JPA entities and Spring Data repositories live here. 

### `security` security layer
Owns authentication. JWT validation and Spring Security configuration live here. The security
layer depends on `logic` for user lookup but exposes no security types to `logic` itself.

---

## Dependency Rules

Dependencies flow in one direction only: **inward**.

```
web  →  logic/domain
web  →  logic/service
web  →  persistence       ✗  FORBIDDEN
web/dto  →  logic/domain    implements interfaces (allowed — pragmatic option)

logic/service  →  logic/domain
logic/service  →  persistence/entity    (allowed — pragmatic option)
logic/service  →  persistence/jpa
logic/service  →  web                   ✗  FORBIDDEN

persistence  →  any layer                     ✗  FORBIDDEN

security  →  logic/domain
security  →  logic/service
security  →  web                        ✗  FORBIDDEN
```

The `logic/domain` package is the only package that multiple layers may depend on
simultaneously, because it contains pure data types with no dependencies of its own.

---

## Design Rules

### 1 — Services receive `Username`, not `Authentication`

Authentication is completed by `JwtFilter` before a request reaches a controller.
By the time a service is called, the identity is already verified. Services receive a
`Username` value object — a fachliches (domain) concept — not a Spring Security type.

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

### 2 — Services return domain types, not DTOs

A service must not know what the caller does with its result. Returning a DTO couples the
service to a single presentation format and prevents reuse across different endpoints or
output formats.

```java
// CORRECT — service returns domain type
public List<DomainUser> findAll() { ... }

// FORBIDDEN — service returns web DTO
public List<AdminUserResponse> findAll() { ... }
```

### 3 — Services do not accept web DTOs as parameters

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

### 4 — Controllers do not navigate entity graphs

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

### 5 — Controllers do not import persistence types

No `import tech.petclinix.persistence.*` in any controller or DTO. If a controller
currently receives an entity from a service, the service must be updated to return a
domain object instead.

### 6 — Mapping belongs to the layer that needs the result

| Mapping | Location |
|---|---|
| `Entity → DomainObject` | `logic/service/mapper/` |
| `Entity → DTO` | `web/controller/mapper/` |
| `DomainObject → DTO` | Controller method or `web/controller/mapper/` |

A mapper in `logic/service/mapper/` may import from `persistence/entity` and `logic/domain`.
A mapper in `web/controller/mapper/` may import from `persistence/entity`, `logic/domain`,
and `web/dto`. Neither mapper may import from the other layer's mapper.

### 7 — `logic/domain` has no dependencies

Every class, record, interface, and enum in `logic/domain` must have zero imports from
`persistence`, `web`, `security`, or any Spring framework class. It may import from
`java.*` only.

This makes `logic/domain` the stable centre of the architecture — safe for all layers
to depend on without risk of circular dependencies.

### 8 — Domain interfaces over copied records

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

### 9 — `StatsData` and similar aggregates live in `logic/domain`

A domain record with no HTTP-specific concerns (no Jackson annotations, no validation
annotations) belongs in `logic/domain` even if it is also used as a response body.
The controller serialises it directly without a separate DTO.

```java
// AdminStatsController — CORRECT
return ResponseEntity.ok(statsService.getStats());  // StatsData from logic/domain
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
// REDUNDANT — vetUsername is already a Username
vetService.retrieveByUsername(new Username(vetUsername.value()));

// CORRECT
vetService.retrieveByUsername(vetUsername);
```

---

## What is intentionally not here

**Ports & Adapters (Hexagonal Architecture)** — not applied. Services depend directly on
JPA repositories and entities. This is a conscious trade-off: less indirection, less
boilerplate, full testability via `@DataJpaTest` and integration tests. 

**MapStruct or similar mapping libraries** — not used. Mapping is explicit and readable. Explicit mappers communicate 
intent more clearly than generated code.

**Separate `Request` and `Response` DTOs for all endpoints** — not enforced. Where request
and response share the same structure, a single record implementing a `logic/domain`
interface is preferred. Separation is introduced only when the structures actually diverge.

