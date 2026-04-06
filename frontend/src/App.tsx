import React from "react";
import {Routes, Route, Link, NavLink} from "react-router-dom";
import {useAuth} from "./context/AuthContext.tsx";
import LoginPage from "./pages/LoginPage";
import {ProtectedRoute, RoleRoute} from "./components/ProtectedRoute";
import AboutMePage from "./pages/AboutMePage.tsx";
import HomePage from "./pages/HomePage";
import RegisterPage from "./pages/RegisterPage.tsx";
import LogoutPage from "./pages/LogoutPage.tsx";
import PetsPage from "./pages/PetsPage.tsx";
import LocationsPage from "./pages/LocationsPage.tsx";
import AppointmentBookingPage from "./pages/AppointmentBookingPage.tsx";
import AppointmentsPage from "./pages/AppointmentsPage.tsx";
import VetAppointmentsPage from "./pages/VetAppointmentsPage.tsx";
import VetVisitPage from "./pages/VetVisitPage.tsx";
import PetVisitsPage from "./pages/PetVisitsPage.tsx";
import AdminUsersPage from "./pages/AdminUsersPage.tsx";
import AdminDashboardPage from "./pages/AdminDashboardPage.tsx";

const navStyle: React.CSSProperties = {
    background: "var(--color-surface)",
    borderBottom: "1px solid var(--color-border)",
    padding: "0 24px",
    display: "flex",
    alignItems: "center",
    height: 56,
    gap: 0,
};

const brandStyle: React.CSSProperties = {
    fontWeight: 800,
    fontSize: 18,
    color: "var(--color-primary)",
    letterSpacing: "-0.02em",
    marginRight: "auto",
    textDecoration: "none",
};

const navLinkStyle: React.CSSProperties = {
    padding: "8px 14px",
    fontSize: 14,
    fontWeight: 500,
    color: "var(--color-text-muted)",
    borderRadius: "var(--radius-md)",
    textDecoration: "none",
    transition: "background 0.15s, color 0.15s",
};

function App() {
    const {user} = useAuth();
    return (
        <>
            <nav style={navStyle}>
                <Link to="/" style={brandStyle}>PetcliniX</Link>
                <NavLink to="/" end style={({ isActive }) => ({
                    ...navLinkStyle,
                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                    background: isActive ? "var(--color-primary-light)" : "transparent",
                })}>Home</NavLink>
                {user && (
                    <>
                        <NavLink to="/aboutme" style={({ isActive }) => ({
                            ...navLinkStyle,
                            color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                            background: isActive ? "var(--color-primary-light)" : "transparent",
                        })}>About Me</NavLink>
                        {user.hasRole("VET") && (
                            <>
                                <NavLink to="/locations" style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>Locations</NavLink>
                                <NavLink to="/appointments/vet" style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>Appointments</NavLink>
                            </>
                        )}
                        {user.hasRole("OWNER") && (
                            <>
                                <NavLink to="/pets" style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>My Pets</NavLink>
                                <NavLink to="/appointments" end style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>Appointments</NavLink>
                            </>
                        )}
                        {user.hasRole("ADMIN") && (
                            <>
                                <NavLink to="/admin/dashboard" style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>Dashboard</NavLink>
                                <NavLink to="/admin/users" style={({ isActive }) => ({
                                    ...navLinkStyle,
                                    color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                                    background: isActive ? "var(--color-primary-light)" : "transparent",
                                })}>Users</NavLink>
                            </>
                        )}
                        <NavLink to="/logout" style={({ isActive }) => ({
                            ...navLinkStyle,
                            color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                            background: isActive ? "var(--color-primary-light)" : "transparent",
                        })}>Logout</NavLink>
                    </>
                )}
                {!user && (
                    <>
                        <NavLink to="/login" style={({ isActive }) => ({
                            ...navLinkStyle,
                            color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                            background: isActive ? "var(--color-primary-light)" : "transparent",
                        })}>Login</NavLink>
                        <NavLink to="/register" style={({ isActive }) => ({
                            ...navLinkStyle,
                            color: isActive ? "var(--color-primary)" : "var(--color-text-muted)",
                            background: isActive ? "var(--color-primary-light)" : "transparent",
                        })}>Register</NavLink>
                    </>
                )}
            </nav>

            <main style={{ minHeight: "calc(100vh - 56px)" }}>
                <Routes>
                    <Route path="/" element={<HomePage/>}/>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/logout" element={<LogoutPage/>}/>
                    <Route path="/register" element={<RegisterPage/>}/>

                    <Route element={<ProtectedRoute/>}>
                        <Route path="/aboutme" element={<AboutMePage/>}/>

                        <Route element={<RoleRoute roles={["VET"]}/>}>
                            <Route path="/locations" element={<LocationsPage/>}/>
                            <Route path="/appointments/vet" element={<VetAppointmentsPage/>}/>
                            <Route path="/appointments/vet/visit/:appointmentId" element={<VetVisitPage/>}/>
                        </Route>

                        <Route element={<RoleRoute roles={["OWNER"]}/>}>
                            <Route path="/appointments" element={<AppointmentsPage/>}/>
                            <Route path="/appointments/book" element={<AppointmentBookingPage/>}/>
                            <Route path="/pets" element={<PetsPage/>}/>
                            <Route path="/pets/:petId/visits" element={<PetVisitsPage/>}/>
                        </Route>

                        <Route element={<RoleRoute roles={["ADMIN"]}/>}>
                            <Route path="/admin/dashboard" element={<AdminDashboardPage/>}/>
                            <Route path="/admin/users" element={<AdminUsersPage/>}/>
                        </Route>
                    </Route>

                    <Route path="/unauthorized" element={
                        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", minHeight: "calc(100vh - 56px)", gap: 12 }}>
                            <h1 style={{ fontSize: 24, fontWeight: 700, color: "var(--color-text)" }}>Access Denied</h1>
                            <p style={{ color: "var(--color-text-muted)" }}>You don't have permission to view this page.</p>
                        </div>
                    }/>
                </Routes>
            </main>
        </>
    );
}

export default App;
