import React from "react";
import {Navigate, useLocation} from "react-router-dom";
import {useAuth} from "../context/AuthContext.tsx";

interface ProtectedRouteProps {
    children: React.ReactElement;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({children}) => {
    const {isLoggedIn} = useAuth();
    const location = useLocation();

    if (!isLoggedIn) {
        return <Navigate to="/login" state={{from: location}} replace/>;
    }

    return children;
};
