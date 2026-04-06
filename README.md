# PetcliniX — Java Spring Boot + React

A veterinary clinic management system implemented as a classic layered monolith: Spring Boot REST backend and React SPA frontend, connected through an Nginx reverse proxy.

---

## Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.5, Spring Security, JPA / Hibernate |
| Frontend | React 19, TypeScript, Vite, TailwindCSS |
| Database | MariaDB 11.1 (prod) · H2 (test) |
| Auth | JWT — HS256, 1 h expiry, roles in `scope` claim |
| Infra | Docker Compose, Nginx ingress on port 8080 |

---

## Architecture

```
Nginx :8080
  ├── /*      → React SPA (dist/)
  └── /api/*  → Spring Boot backend
```

```
/
├── backend/    Java Spring Boot — controllers, services, repositories
├── frontend/   React SPA — pages, components, API client
├── e2e/        Playwright end-to-end tests
├── ingress/    Nginx configuration
└── docs/       Architecture diagrams, testing strategy
```

![C4 System Context](docs/system_context.svg "C4 System Context Diagram")

---

## Documentation

| Document | Description |
|----------|-------------|
| [backend/README.md](backend/README.md) | Backend architecture, design constraints, testing strategy |
| [frontend/README.md](frontend/README.md) | Component structure, auth flow, design system, page pattern |
| [e2e/README.md](e2e/README.md) | How to run E2E tests, test structure, data strategy |
| [docs/testing-strategy.md](docs/testing-strategy.md) | Cross-cutting testing philosophy — unit, integration, and E2E tiers |

---

## Quick Start

### Full stack (Docker Compose)

```bash
docker compose up --build
```

Open `http://localhost:8080`.

### Backend only

```bash
cd backend
mvn spring-boot:run                  # H2 in-memory (dev)
mvn spring-boot:run -Pmariadb        # MariaDB (requires DB running)
mvn test                             # unit + integration tests
```

### Frontend only

```bash
cd frontend
npm install
npm run dev      # Vite dev server on http://localhost:5173
npm test         # Vitest unit tests
```

### E2E tests

```bash
docker compose up --build -d         # start the full stack first
cd e2e
npm install
npm test                             # Playwright, headless Chromium
```

---

## User Roles

| Role | Registration | Capabilities |
|------|-------------|--------------|
| **OWNER** | Self-register | Manage pets, book and cancel appointments, view visit history |
| **VET** | Self-register | Manage clinic locations, view and document appointments |
| **ADMIN** | Seeded via env vars | View stats dashboard, activate/deactivate users |
