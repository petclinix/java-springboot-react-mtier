import React from "react";
import {Navigate, Outlet} from "react-router-dom";
import {useAuth, type Role} from "../context/AuthContext.tsx";

export const ProtectedRoute = () => {
    const {user} = useAuth();
    return user ? <Outlet/> : <Navigate to="/login" replace/>;
};

export const RoleRoute: React.FC<{ roles: Role[] }> = ({roles}) => {
    const {hasRole} = useAuth();
    return hasRole(roles) ? <Outlet/> : <Navigate to="/unauthorized" replace/>;
};
