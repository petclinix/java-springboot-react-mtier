# PetcliniX — React Frontend

React 19 + TypeScript SPA, built with Vite and styled with TailwindCSS. Communicates with the Spring Boot backend via a JSON REST API protected by JWT.

---

## Architecture Overview

```
Browser
  │
  ├── main.tsx              Entry point — mounts <App> inside <AuthProvider>
  ├── App.tsx               Router + nav bar (role-aware links)
  │
  ├── context/
  │   └── AuthContext.tsx   Global auth state: token, decoded User, signin/signout
  │
  ├── components/
  │   └── ProtectedRoute.tsx  Route guards: ProtectedRoute (any auth) + RoleRoute (by role)
  │
  ├── pages/                One component per route; calls useApiClient()
  │
  ├── hooks/
  │   └── useApiClient.ts   Returns a stable ApiClient instance via useMemo
  │
  └── client/
      ├── ApiClient.tsx     HTTP client class — all fetch() calls live here
      └── dto/              TypeScript interfaces mirroring backend response shapes
```

### Dependency direction

```
pages → useApiClient → ApiClient → REST API
pages → useAuth      → AuthContext
App   → ProtectedRoute / RoleRoute → useAuth
```

Pages depend on the hook, never on `ApiClient` directly. This keeps the HTTP layer swappable and makes pages easy to test by mocking the hook.

---

## Layers in detail

### `client/ApiClient.tsx` — HTTP client

A plain TypeScript class. One method per API operation; no React inside.

- Constructor accepts an optional `baseUrl` (defaults to `/api`).
- Private `buildHeaders()` reads the JWT from `localStorage` and injects `Authorization: Bearer <token>`.
- Every method calls `fetch`, checks `res.ok`, and either returns parsed JSON or throws an `Error` with the server's message body.

```
ApiClient
  ├── loginUser(payload)           POST /auth/login
  ├── registerUser(payload)        POST /users/register
  ├── fetchAboutMe()               GET  /users/aboutme
  ├── listPets() / createPet()     GET|POST /pets
  ├── listAppointments()           GET  /owner/appointments
  ├── createAppointment()          POST /owner/appointments
  ├── cancelAppointment(id)        DELETE /owner/appointments/:id
  ├── listVetAppointments()        GET  /vet/appointments
  ├── cancelVetAppointment(id)     DELETE /vet/appointments/:id
  ├── listVets()                   GET  /vets
  ├── saveLocation() / list / retrieve / delete    /locations/*
  ├── getVetVisit() / saveVetVisit()               /vet/visits/:id
  ├── listPetVisits(petId)         GET  /owner/pets/:id/visits
  ├── listAllUsers()               GET  /admin/users
  ├── activateUser() / deactivateUser()            /admin/users/:id/*
  └── getStats()                   GET  /admin/stats
```

A singleton `export const apiClient = new ApiClient()` is exported for production use. Tests can construct a fresh instance with a mock `baseUrl`.

### `client/dto/` — API shapes

Plain TypeScript interfaces (no classes) that match the JSON returned by the backend. No logic here.

```
LoginRequest / LoginResponse
RegisterRequest
UserResponse
Pet / PetRequest
Appointment / AppointmentRequest
VetAppointment / VetVisit / OwnerVisit
Location
Vet
AdminUser / Stats
```

### `hooks/useApiClient.ts` — stable client reference

```ts
export function useApiClient(): ApiClient {
    return useMemo(() => apiClient, []);
}
```

Wraps the singleton in `useMemo` so components receive a stable reference across renders. Pages call `const client = useApiClient()` and never import `ApiClient` directly.

### `context/AuthContext.tsx` — authentication state

`AuthProvider` wraps the entire app. It stores the raw JWT in `localStorage` and in React state, and decodes it with `jwt-decode` on every token change.

Exports:
- `useAuth()` — hook returning `{ user, token, signin, signout, hasRole }`
- `User` class — `id`, `username`, `roles: Role[]`, `hasRole(role)`
- `AuthProvider` — context provider to mount at the root

**Flow:**
1. `signin(jwt)` → writes to `localStorage` + updates state.
2. `useEffect` on `[token]` decodes the JWT and populates `User`.
3. `signout()` clears both `localStorage` and state.
4. `hasRole(role | role[])` checks the decoded roles array.

### `components/ProtectedRoute.tsx` — route guards

Two thin wrappers over React Router's `<Outlet>`:

| Component | Condition | Redirect |
|-----------|-----------|----------|
| `<ProtectedRoute>` | `user != null` | `/login` |
| `<RoleRoute roles={[...]}>` | `hasRole(roles)` | `/unauthorized` |

Used as layout routes in `App.tsx`:

```tsx
<Route element={<ProtectedRoute/>}>
    <Route path="/aboutme" element={<AboutMePage/>}/>

    <Route element={<RoleRoute roles={["OWNER"]}/>}>
        <Route path="/pets" element={<PetsPage/>}/>
    </Route>

    <Route element={<RoleRoute roles={["VET"]}/>}>
        <Route path="/locations" element={<LocationsPage/>}/>
    </Route>
</Route>
```

### `pages/` — page components

Each page component is responsible for:
1. Calling `useApiClient()` to get the HTTP client.
2. Managing local state: `loading`, `error`, `data`.
3. Triggering fetches in `useEffect` (on mount) or in event handlers (on user action).
4. Rendering the result.

Typical pattern (see `PetsPage.tsx`):

```tsx
export default function PetsPage() {
    const client = useApiClient();
    const [pets, setPets]     = useState<Pet[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError]   = useState<string | null>(null);

    useEffect(() => { fetchPets(); }, []);

    async function fetchPets() {
        setLoading(true);
        try {
            setPets(await client.listPets());
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }
    // render ...
}
```

Pages never import `ApiClient` or `AuthContext` directly — they go through the hooks.

---

## Auth flow

```
1. POST /auth/login  →  { token: "eyJ..." }
2. signin(token)     →  localStorage.setItem("jwt", token)
                    →  AuthContext decodes roles from `scope` claim
3. ApiClient.buildHeaders() reads localStorage on every request
4. ProtectedRoute / RoleRoute check AuthContext before rendering pages
5. signout()         →  localStorage.removeItem("jwt")  →  redirect to /login
```

---

## Testing

Tests use **Vitest** + **React Testing Library** + **jsdom**.

- Pages are tested via `render()` with a mocked `useApiClient` hook.
- `AuthContext` is provided through a helper wrapper that exposes the desired auth state.
- Each test follows the **arrange / act / assert** structure.

Run:

```bash
npm test           # watch mode
npm run test:run   # single pass (CI)
```

---

## Build & run

```bash
npm install
npm run dev        # Vite dev server (proxies /api → localhost:8080)
npm run build      # Production build → dist/
npm run preview    # Serve the production build locally
```

Docker:

```bash
docker build --target production -t petclinix/react-frontend .
```

In the full stack, Nginx on port 8080 serves `dist/` for `/*` and proxies `/api/*` to the Spring Boot backend.