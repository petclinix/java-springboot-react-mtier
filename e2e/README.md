# PetcliniX — E2E Tests

Playwright end-to-end tests for the full PetcliniX stack. Tests run against the running Docker Compose environment (Nginx on port 8080).

---

## Prerequisites

- Node.js 18+
- The full stack running: `docker compose up --build -d` from the repo root

---

## Run

```bash
cd e2e
npm install
npm test                                        # all tests, headless Chromium
npx playwright test tests/1-auth.spec.ts        # single file
npx playwright test --headed                    # watch the browser
npx playwright test --ui                        # interactive Playwright UI
npx playwright show-report                      # open last HTML report
```

Tests run **sequentially** (`workers: 1`) because all tests share a single MariaDB instance. Parallel execution would cause test data collisions.

---

## Structure

```
e2e/
├── playwright.config.ts       # base URL, browser, reporter config
├── helpers/
│   └── auth.ts                # shared registerUser() and loginAs() helpers
└── tests/
    ├── 1-auth.spec.ts         # Registration, login, about me
    ├── 2-owner-pets.spec.ts   # Pet management (OWNER)
    ├── 3-owner-appointments.spec.ts  # Booking and cancelling appointments (OWNER)
    ├── 4-vet-locations.spec.ts       # Location management (VET)
    ├── 5-vet-appointments.spec.ts    # Vet appointment list and visit documentation (VET)
    └── 6-admin.spec.ts               # Admin dashboard and user management (ADMIN)
```

---

## Test Data Strategy

Each test file creates its own timestamped users in `beforeAll` so test runs are isolated from each other:

```typescript
const ts = Date.now();
const ownerUser = `appt_owner_${ts}`;
const vetUser   = `appt_vet_${ts}`;
```

Tests within a file share users. Assertions are scoped to avoid conflicts when shared users accumulate state across tests (e.g., filter list items by name rather than asserting an empty list).

---

## Shared Helpers (`helpers/auth.ts`)

```typescript
// Register a new user via the UI registration form
await registerUser(page, username, password, 'OWNER' | 'VET');

// Log in and wait for the nav bar to confirm successful auth
await loginAs(page, username, password);
```

---

## Configuration (`playwright.config.ts`)

| Setting | Value |
|---------|-------|
| Base URL | `http://localhost:8080` |
| Browser | Chromium (Desktop Chrome) |
| Workers | 1 (sequential) |
| Retries | 0 |
| Reporting | HTML + list |
| Screenshots | On failure only |
| Traces | On first retry |
