import {Routes, Route, Link} from "react-router-dom";
import {useAuth} from "./context/AuthContext.tsx";
import LoginPage from "./pages/LoginPage";
import {ProtectedRoute, RoleRoute} from "./components/ProtectedRoute";
import AboutMePage from "./pages/AboutMePage.tsx";
import Hello from "./pages/Hello";
import RegisterPage from "./pages/RegisterPage.tsx";
import LogoutPage from "./pages/LogoutPage.tsx";
import PetsPage from "./pages/PetsPage.tsx";
import LocationsPage from "./pages/LocationsPage.tsx";
import AppointmentBookingPage from "./pages/AppointmentBookingPage.tsx";
import AppointmentsPage from "./pages/AppointmentsPage.tsx";
import VetAppointmentsPage from "./pages/VetAppointmentsPage.tsx";

function App() {
    const {user} = useAuth();
    return (
        <>
            <nav style={{marginBottom: "1rem"}}>
                <Link to="/">Home</Link>
                {user && (
                    <>
                        |{" "} <Link to="/aboutme">About Me</Link>
                         {user.hasRole("VET") && (
                             <>
                                 |{" "} <Link to="/locations">Locations</Link>
                                 |{" "} <Link to="/appointments/vet">Appointments</Link>
                             </>
                         )}
                         {user.hasRole("OWNER") && (
                             <>
                                 |{" "} <Link to="/pets">My Pets</Link>
                                 |{" "} <Link to="/appointments">Appointments</Link>
                             </>
                         )}
                        |{" "} <Link to="/logout">Logout</Link>
                    </>
                )}
                {!user && (
                    <>
                        |{" "} <Link to="/login">Login</Link>
                        |{" "} <Link to="/register">Register</Link>
                    </>
                )}
            </nav>

            <Routes>
                <Route path="/" element={<Hello/>}/>
                <Route path="/login" element={<LoginPage/>}/>
                <Route path="/logout" element={<LogoutPage/>}/>
                <Route path="/register" element={<RegisterPage/>}/>

                <Route element={<ProtectedRoute/>}>
                    <Route path="/aboutme" element={<AboutMePage/>}/>

                    <Route element={<RoleRoute roles={["VET"]}/>}>
                        <Route path="/locations" element={<LocationsPage/>}/>
                        <Route path="/appointments/vet" element={<VetAppointmentsPage/>}/>
                    </Route>

                    <Route element={<RoleRoute roles={["OWNER"]}/>}>
                        <Route path="/appointments" element={<AppointmentsPage/>}/>
                        <Route path="/appointments/book" element={<AppointmentBookingPage/>}/>
                        <Route path="/pets" element={<PetsPage/>}/>
                    </Route>
                </Route>
            </Routes>
        </>
    );
}

export default App;
