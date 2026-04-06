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

const navLinkClass = "px-[14px] py-[8px] text-[14px] font-medium rounded-card no-underline transition-[background,color] duration-150";

function App() {
    const {user} = useAuth();
    return (
        <>
            <nav className="bg-surface border-b border-border px-[24px] flex items-center h-[56px] gap-0">
                <Link
                    to="/"
                    className="font-extrabold text-[18px] text-primary tracking-[-0.02em] mr-auto no-underline"
                >
                    PetcliniX
                </Link>
                <NavLink
                    to="/"
                    end
                    className={({isActive}) =>
                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                    }
                >
                    Home
                </NavLink>
                {user && (
                    <>
                        <NavLink
                            to="/aboutme"
                            className={({isActive}) =>
                                `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                            }
                        >
                            About Me
                        </NavLink>
                        {user.hasRole("VET") && (
                            <>
                                <NavLink
                                    to="/locations"
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    Locations
                                </NavLink>
                                <NavLink
                                    to="/appointments/vet"
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    Appointments
                                </NavLink>
                            </>
                        )}
                        {user.hasRole("OWNER") && (
                            <>
                                <NavLink
                                    to="/pets"
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    My Pets
                                </NavLink>
                                <NavLink
                                    to="/appointments"
                                    end
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    Appointments
                                </NavLink>
                            </>
                        )}
                        {user.hasRole("ADMIN") && (
                            <>
                                <NavLink
                                    to="/admin/dashboard"
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    Dashboard
                                </NavLink>
                                <NavLink
                                    to="/admin/users"
                                    className={({isActive}) =>
                                        `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                                    }
                                >
                                    Users
                                </NavLink>
                            </>
                        )}
                        <NavLink
                            to="/logout"
                            className={({isActive}) =>
                                `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                            }
                        >
                            Logout
                        </NavLink>
                    </>
                )}
                {!user && (
                    <>
                        <NavLink
                            to="/login"
                            className={({isActive}) =>
                                `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                            }
                        >
                            Login
                        </NavLink>
                        <NavLink
                            to="/register"
                            className={({isActive}) =>
                                `${navLinkClass} ${isActive ? "text-primary bg-primary-light" : "text-muted"}`
                            }
                        >
                            Register
                        </NavLink>
                    </>
                )}
            </nav>

            <main className="min-h-[calc(100vh-56px)]">
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
                        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-56px)] gap-[12px]">
                            <h1 className="text-[24px] font-bold">Access Denied</h1>
                            <p className="text-muted">You don't have permission to view this page.</p>
                        </div>
                    }/>
                </Routes>
            </main>
        </>
    );
}

export default App;
