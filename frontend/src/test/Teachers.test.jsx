import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import TeachersPage from "../pages/TeachersPage";
import TeacherFormPage from "../pages/TeacherFormPage";
import TeacherDetailsPage from "../pages/TeacherDetailsPage";

const { service, auth } = vi.hoisted(() => ({
  service: {
    getTeachers: vi.fn(),
    getTeacherById: vi.fn(),
    createTeacher: vi.fn(),
    updateTeacher: vi.fn(),
    activateTeacher: vi.fn(),
    deactivateTeacher: vi.fn(),
    getMyTeacherProfile: vi.fn(),
  },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../services/teacherService", () => service);
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
const faculty = {
  id: 1,
  employeeId: "EMP-1",
  fullName: "Test Faculty",
  email: "faculty@example.com",
  status: "ACTIVE",
  specialization: "Math",
  subjects: ["Algebra"],
  salary: 50000,
  experienceYears: 5,
};
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { role: "ADMIN" };
  service.getTeachers.mockResolvedValue({
    data: { content: [faculty], totalElements: 21, totalPages: 3 },
  });
  service.getTeacherById.mockResolvedValue({ data: faculty });
});
it("loads real-service records and requests search, filtering and pagination", async () => {
  render(
    <MemoryRouter>
      <TeachersPage />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("link", { name: "Test Faculty" }),
  ).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Next page" }));
  await waitFor(() =>
    expect(service.getTeachers).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1 }),
      expect.anything(),
    ),
  );
  await userEvent.selectOptions(
    screen.getByLabelText("Teacher status"),
    "INACTIVE",
  );
  await userEvent.type(screen.getByRole("searchbox"), "math");
  await waitFor(() =>
    expect(service.getTeachers).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "math", status: "INACTIVE", page: 0 }),
      expect.anything(),
    ),
  );
});
it("shows empty and API error states", async () => {
  service.getTeachers.mockRejectedValueOnce({
    response: { data: { message: "Unavailable" } },
  });
  render(
    <MemoryRouter>
      <TeachersPage />
    </MemoryRouter>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent("Unavailable");
  service.getTeachers.mockResolvedValue({
    data: { content: [], totalElements: 0, totalPages: 0 },
  });
  await userEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("No teachers found")).toBeInTheDocument();
});
it("creates a teacher and redirects to the directory", async () => {
  service.createTeacher.mockResolvedValue({ data: faculty });
  render(
    <MemoryRouter initialEntries={["/teachers/new"]}>
      <Routes>
        <Route path="/teachers/new" element={<TeacherFormPage />} />
        <Route path="/teachers" element={<p>Directory reached</p>} />
      </Routes>
    </MemoryRouter>,
  );
  for (const [id, value] of Object.entries({
    fullName: "Test Faculty",
    employeeId: "EMP-1",
    email: "faculty@example.com",
    salary: "50000",
    subjects: "Math, Algebra",
  }))
    fireEvent.change(document.getElementById(id), { target: { value } });
  await userEvent.click(screen.getByRole("button", { name: "Save teacher" }));
  expect(await screen.findByText("Directory reached")).toBeInTheDocument();
  expect(service.createTeacher).toHaveBeenCalledWith(
    expect.objectContaining({ subjects: ["Math", "Algebra"], salary: "50000" }),
  );
});
it("prefills edit data, preserves salary and shows server validation errors", async () => {
  service.updateTeacher.mockRejectedValue({
    response: {
      data: {
        message: "Validation failed",
        errors: { email: "Email already exists" },
      },
    },
  });
  render(
    <MemoryRouter initialEntries={["/teachers/1/edit"]}>
      <Routes>
        <Route path="/teachers/:id/edit" element={<TeacherFormPage />} />
      </Routes>
    </MemoryRouter>,
  );
  expect(await screen.findByDisplayValue("EMP-1")).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Save teacher" }));
  expect(await screen.findByText("Email already exists")).toBeInTheDocument();
  expect(service.updateTeacher).toHaveBeenCalledWith(
    "1",
    expect.objectContaining({ salary: 50000 }),
  );
});
it("confirms deactivation and reactivation through actual service functions", async () => {
  service.deactivateTeacher.mockResolvedValue({
    data: { ...faculty, status: "INACTIVE" },
  });
  service.activateTeacher.mockResolvedValue({ data: faculty });
  render(
    <MemoryRouter initialEntries={["/teachers/1"]}>
      <Routes>
        <Route path="/teachers/:id" element={<TeacherDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  );
  await userEvent.click(
    await screen.findByRole("button", { name: "Deactivate" }),
  );
  expect(service.deactivateTeacher).not.toHaveBeenCalled();
  await userEvent.click(
    screen.getByRole("button", { name: "Deactivate teacher" }),
  );
  await userEvent.click(
    await screen.findByRole("button", { name: "Activate" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Activate teacher" }),
  );
  await waitFor(() => expect(service.activateTeacher).toHaveBeenCalledWith(1));
});
it("teacher self-profile never renders salary even if an unexpected salary field arrives", async () => {
  auth.user = { role: "TEACHER" };
  service.getMyTeacherProfile.mockResolvedValue({ data: faculty });
  render(
    <MemoryRouter>
      <TeacherDetailsPage own />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("heading", { name: "Test Faculty" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Salary (INR)")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "Edit teacher" }),
  ).not.toBeInTheDocument();
});
it("unlinked teachers receive a helpful empty state", async () => {
  auth.user = { role: "TEACHER" };
  service.getMyTeacherProfile.mockRejectedValue({ response: { status: 404 } });
  render(
    <MemoryRouter>
      <TeacherDetailsPage own />
    </MemoryRouter>,
  );
  expect(
    await screen.findByText("Your teaching profile isn't linked yet"),
  ).toBeInTheDocument();
});
