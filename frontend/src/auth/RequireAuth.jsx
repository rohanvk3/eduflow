import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { Loading } from "../components/UI";
import { Suspense } from "react";

export function allowedRoute(role, path) {
  // Students can open account details; the API enforces account ownership.
  if (/^\/finance\/accounts\/\d+$/.test(path)) return ["ADMIN", "STUDENT"].includes(role);
  if (path.startsWith("/admin/") || path.startsWith("/finance") || path === "/students" || path === "/teachers" || /^\/teachers\/new$|^\/teachers\/\d+\/edit$/.test(path))
    return role === "ADMIN";
  if (path.startsWith("/parent/")) return role === "PARENT";
  if (path.startsWith("/analytics") || path === "/reports") return ["ADMIN", "TEACHER"].includes(role);
  if (["/my-fees", "/my-assignments", "/my-results", "/my-resources", "/my-progress", "/students/me", "/batches/my-enrollments"].includes(path)) return role === "STUDENT";
  return true;
}

export default function RequireAuth() {
  const { user, loading } = useAuth();
  const { pathname } = useLocation();
  if (loading) return <Loading label="Opening your workspace…" />;
  if (!user) return <Navigate to="/login" replace />;
  if (!allowedRoute(user.role, pathname)) return <Navigate to="/dashboard" replace />;
  return <Suspense fallback={<Loading />}><Outlet /></Suspense>;
}
