import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import DashboardPage from "../pages/DashboardPage";
import Layout from "../components/Layout";
import { student } from "./fixtures";

const { api, auth } = vi.hoisted(() => ({
  api: { get: vi.fn() },
  auth: {
    user: { role: "ADMIN", fullName: "QA Admin", email: "admin@example.com" },
    logout: vi.fn(),
  },
}));
vi.mock("../api/client", () => ({
  api,
  errorMessage: () => "Could not load the overview",
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../components/AttendanceDashboard", () => ({ default: () => null }));
vi.mock("../components/ExamDashboard", () => ({ default: () => null }));
vi.mock("../components/AssignmentDashboard", () => ({ default: () => null }));
vi.mock("../components/ResourceDashboard", () => ({ default: () => null }));
vi.mock("../components/NotificationDashboard", () => ({ default: () => null }));
vi.mock("../components/NotificationBell", () => ({ default: () => null }));
vi.mock("../services/financeService", () => ({
  getFinanceDashboard: () =>
    Promise.resolve({
      data: {
        totalCollections: 25000,
        outstandingBalance: 10000,
        overdueAmount: 1000,
        monthCollections: 5000,
      },
    }),
}));
vi.mock("../services/academicService", () => ({
  getBatchStats: () => Promise.resolve({ data: { total: 3, active: 2 } }),
}));
vi.mock("../services/teacherService", () => ({
  getTeacherStats: () =>
    Promise.resolve({ data: { total: 5, active: 4, inactive: 1 } }),
}));

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  auth.user = {
    role: "ADMIN",
    fullName: "QA Admin",
    email: "admin@example.com",
  };
  api.get.mockResolvedValue({
    data: { content: [student], totalElements: 123 },
  });
});

it("shows live student totals and admissions without inventing unavailable metrics", async () => {
  render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
  expect(await screen.findByText("123")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Asha Sharma" })).toHaveAttribute(
    "href",
    "/students/1",
  );
  expect(await screen.findByText("Total collections")).toBeInTheDocument();
  expect(await screen.findByText("5 total teachers")).toBeInTheDocument();
  expect(screen.getByText("Outstanding dues")).toBeInTheDocument();
  expect(api.get).toHaveBeenCalledWith(
    "/students",
    expect.objectContaining({ params: { page: 0, size: 5 } }),
  );
});

it("distinguishes unavailable student data from zero and allows retry", async () => {
  api.get.mockRejectedValueOnce(new Error("Offline"));
  render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Could not load the overview",
  );
  expect(screen.getByText("Could not load student total")).toBeInTheDocument();
  await userEvent.click(
    screen.getByRole("button", { name: "Retry student overview" }),
  );
  expect(await screen.findByText("123")).toBeInTheDocument();
});

it("does not request institute-wide metrics for students", () => {
  auth.user = { ...auth.user, role: "STUDENT" };
  render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
  expect(screen.getByRole("link", { name: "View my profile" })).toHaveAttribute(
    "href",
    "/students/me",
  );
  expect(api.get).not.toHaveBeenCalled();
});

it("shows a truthful empty admissions state for an empty institute", async () => {
  api.get.mockResolvedValue({ data: { content: [], totalElements: 0 } });
  render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
  expect(
    await screen.findByText("Your next chapter starts here"),
  ).toBeInTheDocument();
  expect(screen.queryByText("Asha Sharma")).not.toBeInTheDocument();
});

const shell = () =>
  render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<p>Dashboard content</p>} />
          <Route path="/students" element={<p>Students content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

it("collapses the sidebar, links to notifications, and supports profile logout", async () => {
  shell();
  expect(
    screen.getByRole("link", { name: "Finance Overview" }),
  ).toHaveAttribute("href", "/finance");
  expect(screen.getByRole("link", { name: "Notifications" })).toHaveAttribute(
    "href",
    "/notifications",
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Collapse sidebar" }),
  );
  expect(
    screen.getByRole("button", { name: "Expand sidebar" }),
  ).toHaveAttribute("aria-expanded", "false");
  expect(localStorage.getItem("eduflow.sidebar")).toBe("collapsed");
  await userEvent.click(screen.getByLabelText("Open profile menu"));
  await userEvent.click(screen.getByRole("button", { name: "Sign out" }));
  expect(auth.logout).toHaveBeenCalledOnce();
});

it("opens mobile navigation and closes it when a route is selected", async () => {
  shell();
  await userEvent.click(
    screen.getByRole("button", { name: "Open navigation" }),
  );
  const drawer = screen.getByRole("dialog", { name: "Navigation" });
  await userEvent.click(within(drawer).getByRole("link", { name: "Students" }));
  expect(await screen.findByText("Students content")).toBeInTheDocument();
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

it("submits global search into the existing students route and updates breadcrumbs", async () => {
  shell();
  await userEvent.type(
    screen.getByRole("textbox", { name: "Search workspace" }),
    "asha{Enter}",
  );
  await waitFor(() =>
    expect(screen.getByText("Students content")).toBeInTheDocument(),
  );
  expect(
    within(screen.getByRole("navigation", { name: "Breadcrumb" })).getByText(
      "Students",
    ),
  ).toHaveAttribute("aria-current", "page");
});
