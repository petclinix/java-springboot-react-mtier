import React from "react";
import {BrowserRouter, Routes, Route, Link} from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import {ProtectedRoute} from "./components/ProtectedRoute";
import ProtectedHello from "./pages/ProtectedHello";
import Hello from "./pages/Hello";

function App() {
    return (
        <BrowserRouter>
            <nav style={{marginBottom: "1rem"}}>
                <Link to="/">Home</Link> |{" "}
                <Link to="/dashboard">Dashboard (Protected)</Link>
            </nav>

            <Routes>
                <Route path="/" element={<Hello/>}/>
                <Route path="/login" element={<LoginPage/>}/>

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
