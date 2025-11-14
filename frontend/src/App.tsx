import {BrowserRouter, Routes, Route, Link} from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import {ProtectedRoute} from "./components/ProtectedRoute";
import ProtectedHello from "./pages/ProtectedHello";
import Hello from "./pages/Hello";
import RegisterPage from "./pages/RegisterPage.tsx";
import {isLoggedIn} from "./utils/auth.ts";
import LogoutPage from "./pages/LogoutPage.tsx";

function App() {
    return (
        <BrowserRouter>
            <nav style={{marginBottom: "1rem"}}>
                <Link to="/">Home</Link> |{" "}
                <Link to="/dashboard">Dashboard (Protected)</Link>
                {isLoggedIn() && (
                    <>
                        |{" "}<Link to="/logout">Logout</Link>
                    </>
                )}
                {!isLoggedIn() && (
                    <>
                        |{" "}<Link to="/login">Login</Link>
                        <Link to="/register">Register</Link>
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
                            <ProtectedHello/>
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
