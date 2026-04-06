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

---

## Design System & UI/UX

### Styling approach

Components are styled with **inline style objects that reference CSS custom properties** (design tokens). Tailwind CSS is imported in `index.css` for its reset and base layer, but utility classes are intentionally not used in component JSX — all visual values come from the token system so there is a single place to change colours, radii, or shadows.

Rule: **never hardcode a colour, border-radius, or shadow value in a component.** Always use a `var(--…)` token.

```tsx
// correct
<div style={{ background: "var(--color-surface)", borderRadius: "var(--radius-md)" }}>

// wrong — bypasses the token system
<div style={{ background: "#ffffff", borderRadius: "8px" }}>
```

---

### Design tokens (`src/index.css`)

All tokens are CSS custom properties on `:root`. They are grouped by concern:

#### Brand / primary

| Token | Value | Tailwind equivalent |
|-------|-------|---------------------|
| `--color-primary` | `#0d9488` | teal-600 |
| `--color-primary-hover` | `#0f766e` | teal-700 |
| `--color-primary-light` | `#ccfbf1` | teal-100 |

Use `primary` for interactive elements (buttons, links, focus rings, active nav). Use `primary-light` for subtle backgrounds (active nav item, info banners).

#### Semantic colours

| Token | Value | Use |
|-------|-------|-----|
| `--color-success` / `--color-success-light` | green-600 / green-100 | Confirmations, active status |
| `--color-danger` / `--color-danger-hover` / `--color-danger-light` | red-600/700/100 | Destructive actions, errors |
| `--color-warning` / `--color-warning-light` | amber-600 / amber-100 | Warnings, cautions |

#### Neutrals

| Token | Value | Use |
|-------|-------|-----|
| `--color-text` | slate-800 | Body copy, headings |
| `--color-text-muted` | slate-500 | Labels, secondary text, table headers |
| `--color-border` | slate-200 | Default borders, dividers |
| `--color-border-strong` | slate-300 | Button outlines, focused inputs |
| `--color-bg` | slate-50 | Page background |
| `--color-surface` | white | Cards, nav bar, form fields |
| `--color-surface-hover` | slate-100 | Row hover, subtle hover states |

#### Role colours

Each user role has a dedicated badge palette so that roles are visually distinguishable at a glance:

| Token pair | Role | Colour |
|------------|------|--------|
| `--color-role-owner` / `--color-role-owner-text` | OWNER | blue-100 / blue-700 |
| `--color-role-vet` / `--color-role-vet-text` | VET | green-100 / green-800 |
| `--color-role-admin` / `--color-role-admin-text` | ADMIN | violet-100 / violet-800 |

Always use the `<Badge>` component for roles and status — never inline them.

#### Shape & depth

| Token | Value | Use |
|-------|-------|-----|
| `--radius-sm` | 4px | Badges, small chips |
| `--radius-md` | 8px | Buttons, inputs, cards |
| `--radius-lg` | 12px | Modal panels, large cards |
| `--shadow-card` | subtle 1px drop | Cards and surface containers |
| `--shadow-focus` | teal glow ring | Keyboard focus indicator |

---

### Component library (`src/components/ui/`)

These are the only building blocks pages should use. Do not reach for raw HTML elements where a component exists.

#### `<Button>`

```tsx
<Button variant="primary" size="md" loading={saving} onClick={handleSave}>
  Save
</Button>
```

| Prop | Options | Default |
|------|---------|---------|
| `variant` | `primary` `secondary` `danger` `ghost` | `primary` |
| `size` | `sm` `md` | `md` |
| `loading` | boolean | `false` |

- `primary` — teal fill; for the one main action on a surface.
- `secondary` — white with border; for secondary / cancel actions.
- `danger` — red fill; for destructive actions (cancel appointment, deactivate user).
- `ghost` — transparent with underline; for inline text actions.
- When `loading` is true the button is disabled and shows "Loading…". Only one primary action per form should ever be in a loading state at once.

#### `<Badge>`

```tsx
<Badge variant="owner">OWNER</Badge>
<Badge variant="active">Active</Badge>
```

Variants: `owner` `vet` `admin` `active` `inactive` `neutral`.

Use `active` / `inactive` for entity status. Use `owner` / `vet` / `admin` for role indicators. Use `neutral` for everything else.

#### `<Card>`

A white surface with `--shadow-card` and `--radius-lg`. Wrap any self-contained data block in a `<Card>`. Do not add extra borders or shadows on top of it.

#### `<FormField>`

```tsx
<FormField label="Username" error={errors.username} hint="At least 3 characters">
  <Input value={username} onChange={e => setUsername(e.target.value)} />
</FormField>
```

Always wrap an `<Input>` or `<Select>` in a `<FormField>`. This ensures consistent label sizing (13px/600), error display (12px danger), and hint text (12px muted). Never build label/error/hint markup manually.

#### `<Input>` and `<Select>`

Plain wrappers with consistent border (`--color-border`), radius (`--radius-md`), and focus ring. They forward refs. Always use these instead of raw `<input>` / `<select>`.

#### `<StatusMessage>`

```tsx
<StatusMessage variant="error">{error}</StatusMessage>
<StatusMessage variant="success">Saved successfully.</StatusMessage>
```

Variants: `error` `success` `warning` `info`.

- Renders `role="alert"` for `error`, `role="status"` for everything else — screen readers announce these automatically.
- Place at the top of the affected section, not at the bottom.
- Clear the message when the user starts a new action (reset error state before each async call).

#### `<PageLayout>`

```tsx
<PageLayout>…</PageLayout>          {/* 960px max-width — data/list pages */}
<PageLayout narrow>…</PageLayout>   {/* 640px max-width — forms, single-entity pages */}
```

All pages must be wrapped in `<PageLayout>`. Use `narrow` for any page whose primary content is a single form or a single entity detail view.

#### `<PageHeader>`

```tsx
<PageHeader title="My Pets" actions={<Button onClick={openForm}>Add Pet</Button>} />
```

Renders the page title (h1) aligned left and an optional actions slot aligned right. Every page should start with a `<PageHeader>` — it keeps page title and primary action consistently positioned.

#### `<EmptyState>`

```tsx
<EmptyState message="No pets yet." action={<Button onClick={openForm}>Add your first pet</Button>} />
```

Render this when a list is empty. Never render a blank space or a bare "No results" string.

#### `<DataTable>`

```tsx
<DataTable
  columns={[
    { header: "Name", render: row => row.name },
    { header: "Role",  render: row => <Badge variant={row.role.toLowerCase() as any}>{row.role}</Badge> },
  ]}
  rows={users}
  keyFn={row => row.id}
  emptyMessage="No users found."
/>
```

Use for any tabular data with more than two columns. Handles empty state, row hover, and column width control internally.

---

### Layout rules

- **Page background**: `--color-bg` (slate-50). Never white.
- **Content width**: always via `<PageLayout>` — 960px for data pages, 640px for forms.
- **Page padding**: handled by `<PageLayout>` (32px vertical, 20px horizontal). Do not add extra padding to the top-level element inside a page.
- **Nav bar height**: 56px fixed. Page content starts at `calc(100vh - 56px)`.
- **Cards**: use `--radius-lg` and `--shadow-card`. Stack vertically with a `gap` of 16–24px.

---

### UX patterns

#### Loading states

Every async operation must reflect in-progress state so the user knows something is happening:

- **Page-level fetch**: set `loading = true` before the call, `false` in `finally`. Render "Loading…" text or a spinner in place of the list/form.
- **Per-action loading**: when a list has row-level actions (e.g. cancel one appointment), track the in-flight id, not just a boolean. This disables only the affected button, not the whole page.

```tsx
// per-action pattern
const [cancellingId, setCancellingId] = useState<number | null>(null);

async function cancel(id: number) {
    setCancellingId(id);
    try { await client.cancelAppointment(id); }
    finally { setCancellingId(null); }
}

<Button loading={cancellingId === appt.id} variant="danger" onClick={() => cancel(appt.id)}>
    Cancel
</Button>
```

#### Error handling

- Always clear `error` before each async call: `setError(null)`.
- Catch errors and set `setError(err.message || "Unexpected error")`.
- Display errors with `<StatusMessage variant="error">`.
- Never swallow errors silently.

#### Empty states

Never leave the page blank when a list has no data. Always render `<EmptyState>` with a helpful message and, where appropriate, a call-to-action button.

#### Form validation

- Validate before submitting, not only after a server error.
- Use `<FormField error={…}>` to show per-field errors inline below the field.
- Disable the submit button while the request is in flight (use `loading` prop).

#### Destructive actions

- Always use `variant="danger"` on the button.
- Do not add confirmation dialogs for low-impact actions (cancelling an appointment the user owns). Add one only when the action cannot be undone and affects other users.

---

### Typography rules

- Base font: **Inter**, fallback to `system-ui, -apple-system, sans-serif`.
- Base size: **16px**, line-height **1.6**.
- Headings (`h1`–`h3`): `line-height: 1.3`, `margin: 0 0 0.5em`.
- Body copy: `--color-text` (slate-800).
- Secondary / helper text: `--color-text-muted` (slate-500), typically 12–13px.
- Table column headers: 12px, 700 weight, uppercase, `--color-text-muted`.
- Do not use font sizes below 12px.

---

### Accessibility rules

- **Focus ring**: `:focus-visible` renders a 2px `--color-primary` outline with a 2px offset. Do not suppress this with `outline: none`.
- **ARIA roles**: `StatusMessage` sets `role="alert"` for errors and `role="status"` for other feedback — do not override this.
- **Semantic HTML**: use `<button>` for actions, `<a>` for navigation, `<table>` for tabular data via `<DataTable>`. Do not use `<div onClick>` for interactive elements.
- **Labels**: every form input must have a visible `<label>` via `<FormField>`. Do not rely on placeholder text as a label substitute.
- **Keyboard navigation**: all interactive elements must be reachable and operable by keyboard alone. The focus ring must remain visible.