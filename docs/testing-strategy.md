# Testing Strategy

PetcliniX uses three tiers of tests. Each tier has a fixed scope, a fixed set of
collaborators it is allowed to use, and a fixed cost. Mixing scopes produces tests that
are slow to run, hard to diagnose when they fail, and easy to break when unrelated code
changes. The rule is: **test at the lowest tier that can meaningfully answer the question**.

---

## The three tiers

```
┌─────────────────────────────────────────────────────┐
│  E2E  — does the deployed system behave correctly?  │  few, slow, fragile
├─────────────────────────────────────────────────────┤
│  Integration — do the parts work together?          │  some, medium
├─────────────────────────────────────────────────────┤
│  Unit — does this piece of logic do the right thing?│  many, fast, stable
└─────────────────────────────────────────────────────┘
```

### Unit tests

**Question answered:** does this isolated piece of logic produce the correct output for a
given input?

**What is real:** the class under test, its pure dependencies (mappers, value objects).
**What is mocked:** every external collaborator — repositories, other services, the
network, the clock.

Unit tests are the majority of the test suite. They are fast (milliseconds), deterministic,
and require no infrastructure. A failing unit test points directly at the defect.

### Integration tests

**Question answered:** do two or more components interact correctly across a real
boundary?

**What is real:** the components being integrated and the shared infrastructure between
them (Spring context slice, H2 in-memory database, HTTP layer).
**What is mocked:** everything outside the scope of the integration being tested.

Integration tests are more expensive than unit tests but cheaper than E2E. They verify
contracts that unit tests cannot — serialisation formats, SQL query correctness, security
rules, transaction boundaries.

### E2E tests

**Question answered:** does the full system — frontend, backend, and database — behave
correctly from a user's perspective?

**What is real:** the entire running stack (Playwright drives a real browser against the
full Docker Compose deployment).
**What is mocked:** nothing.

E2E tests are the most expensive tier. They are slow, can be affected by timing, and
require all services to be running. They are used sparingly to verify critical user
journeys end-to-end, not to re-check logic already covered below.

---

## Backend test types

The backend has five distinct test types. Each maps to a specific tier.

| Type | Tier | Annotation | What it tests |
|------|------|-----------|---------------|
| Service unit test | Unit | `@ExtendWith(MockitoExtension)` | Business rules in isolation; repositories mocked |
| Entity relation test | Unit | Plain JUnit 5 | `@OneToMany` back-pointer consistency; no Spring, no DB |
| Controller unit test | Unit | `@ExtendWith(MockitoExtension)` | Branching logic inside a controller; no HTTP machinery |
| Controller slice test | Integration | `@WebMvcTest` | HTTP contract: serialisation, status codes, `@PreAuthorize` |
| Repository integration test | Integration | `@DataJpaTest` | Criteria API queries against H2; no service or web layer |

There are no `@SpringBootTest` tests for individual layers. Full-context tests are
reserved for scenarios where the interaction between all layers is what is being
verified — which is covered by the E2E tier instead.

### Why no `@SpringBootTest` for controller tests

`@SpringBootTest` starts the full application context. A test that only needs to verify
a JSON field name loads JPA, H2, security, all services, and all repositories — none of
which affect the assertion. Beyond the startup cost, full-context tests produce
misleading failures: a bug in the service layer causes the controller test to fail,
pointing at the wrong layer.

`@WebMvcTest` loads only the web slice. The service is replaced by a Mockito mock.
Failures in a controller slice test are always caused by the controller or its
serialisation — not by anything below it.

See `backend/docs/architecture-internals.md` → Section 8 for the required scenarios per
endpoint and code examples.

---

## Frontend test types

| Type | Tier | Framework | What it tests |
|------|------|----------|---------------|
| Page component test | Integration | Vitest + React Testing Library | Page renders correctly; `useApiClient` hook mocked |
| Hook / utility test | Unit | Vitest | Pure logic in hooks or utility functions |

Each page test renders the component inside a `MemoryRouter` with a controlled
`AuthContext`. The `useApiClient` hook is mocked to return a controlled `ApiClient`
instance. This keeps the test within the frontend boundary — the HTTP client is never
called.

```ts
// arrange — mock the api client
vi.mock("../hooks/useApiClient", () => ({
    useApiClient: () => ({ listPets: vi.fn().mockResolvedValue([]) }),
}));

// act — render with controlled auth state
render(
    <MemoryRouter>
        <AuthContext.Provider value={mockAuthValue}>
            <PetsPage />
        </AuthContext.Provider>
    </MemoryRouter>
);

// assert
expect(await screen.findByText("No pets yet.")).toBeInTheDocument();
```

Every page test covers at minimum: successful data load, API error display, and the
primary user action (form submit, button click).

---

## E2E tests

E2E tests use **Playwright** and run against the full Docker Compose stack. They drive
a real browser and make real HTTP requests through Nginx to the Spring Boot backend and
MariaDB.

Each spec file covers one user journey end-to-end:

```
e2e/
  specs/
    1-auth.spec.ts           Register, login, logout
    2-owner-pets.spec.ts     Add a pet, view pet list
    3-owner-appointments.spec.ts  Book and cancel an appointment
    4-vet-locations.spec.ts  Create and edit a location
    5-admin-users.spec.ts    Activate and deactivate a user
```

E2E tests do not duplicate assertions already covered by unit or integration tests.
They verify that the pieces connect — the JWT produced by the backend is accepted by
the frontend, the appointment created via the API appears in the UI, the role restriction
blocks navigation to the correct page.

---

## Decision guide — which tier to use

| Question | Tier |
|----------|------|
| Does this service method throw `NotFoundException` when the record is absent? | Unit |
| Does this mapper produce the correct domain object from the entity? | Unit (via service test — mappers are not mocked) |
| Does this Criteria API query return the right rows? | Integration (`@DataJpaTest`) |
| Does the endpoint return 403 for the wrong role? | Integration (`@WebMvcTest`) |
| Does the endpoint return 400 for a malformed request body? | Integration (`@WebMvcTest`) |
| Does this page render the error message when the API fails? | Integration (Vitest + RTL) |
| Can a new user register, log in, add a pet, and book an appointment? | E2E |
| Does the admin user list update immediately after deactivating a user? | E2E |

If the question involves a real browser, a real network call, or verifying that two
independently deployable layers work together, use E2E. If the question is about a
contract between two components in the same deployable unit, use Integration. If the
question is about logic with no external dependencies, use Unit.

---

## Coverage enforcement

**Backend:** JaCoCo enforces **80% complexity coverage** as a build gate (`mvn verify`
fails below this threshold). Coverage is measured across the unit and integration tiers
combined.

**Frontend:** no automated coverage gate. Coverage is a development tool, not a gate.

**E2E:** coverage is implicit — each critical user journey must have a spec.
