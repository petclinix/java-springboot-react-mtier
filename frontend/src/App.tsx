import { Routes, Route, Link} from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import {ProtectedRoute} from "./components/ProtectedRoute";
import AboutMePage from "./pages/AboutMePage.tsx";
import Hello from "./pages/Hello";
import RegisterPage from "./pages/RegisterPage.tsx";
import LogoutPage from "./pages/LogoutPage.tsx";
import {useAuth} from "./context/AuthContext.tsx";
import PetsPage from "./pages/PetsPage.tsx";

function App() {
    const { isLoggedIn } = useAuth();
    return (
        <>
            <nav style={{marginBottom: "1rem"}}>
                <Link to="/">Home</Link> |{" "}
                <Link to="/dashboard">Dashboard (Protected)</Link> |{" "}
                <Link to="/pets">My Pets</Link>
                {isLoggedIn && (
                    <>
                        |{" "}<Link to="/logout">Logout</Link>
                    </>
                )}
                {!isLoggedIn && (
                    <>
                        |{" "}<Link to="/login">Login</Link>
                        |{" "}<Link to="/register">Register</Link>
                    </>
                )}
            </nav>

            <Routes>
                <Route path="/" element={<Hello/>}/>
                <Route path="/login" element={<LoginPage/>}/>
                <Route path="/logout" element={<LogoutPage/>}/>
                <Route path="/register" element={<RegisterPage/>}/>

                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <AboutMePage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/pets"
                    element={
                        <ProtectedRoute>
                            <PetsPage/>
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    );
}

export default App;
