import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { expect, it, vi } from "vitest";
import RequireAuth, { allowedRoute } from "../auth/RequireAuth";

const auth = vi.hoisted(() => ({ user: null, loading: false }));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));

it.each(["TEACHER", "STUDENT", "PARENT"])("blocks %s from administration and finances", role => {
  expect(allowedRoute(role, "/admin/users")).toBe(false);
  expect(allowedRoute(role, "/finance/payments")).toBe(false);
});
it("separates parent, student and reporting routes", () => {
  expect(allowedRoute("STUDENT", "/parent/children")).toBe(false);
  expect(allowedRoute("PARENT", "/my-fees")).toBe(false);
  expect(allowedRoute("TEACHER", "/analytics")).toBe(true);
  expect(allowedRoute("ADMIN", "/admin/users")).toBe(true);
});
it("allows student fee details while keeping financial administration restricted", () => {
  expect(allowedRoute("STUDENT", "/finance/accounts/42")).toBe(true);
  expect(allowedRoute("ADMIN", "/finance/accounts/42")).toBe(true);
  expect(allowedRoute("STUDENT", "/finance/accounts")).toBe(false);
  expect(allowedRoute("STUDENT", "/finance/accounts/42/edit")).toBe(false);
  expect(allowedRoute("TEACHER", "/finance/accounts/42")).toBe(false);
  expect(allowedRoute("PARENT", "/finance/accounts/42")).toBe(false);
});
it.each([null, { role: "STUDENT" }])("redirects unauthenticated or unauthorized navigation", user => {
  auth.user = user;
  render(<MemoryRouter initialEntries={["/admin/users"]}><Routes>
    <Route element={<RequireAuth />}><Route path="/admin/users" element={<p>Secret admin page</p>} /></Route>
    <Route path="/login" element={<p>Sign in destination</p>} />
    <Route path="/dashboard" element={<p>Dashboard destination</p>} />
  </Routes></MemoryRouter>);
  expect(screen.queryByText("Secret admin page")).not.toBeInTheDocument();
  expect(screen.getByText(user ? "Dashboard destination" : "Sign in destination")).toBeInTheDocument();
});
