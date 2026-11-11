import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import StudentsPage from "../pages/StudentsPage";
import StudentDetailsPage from "../pages/StudentDetailsPage";
import StudentForm from "../components/StudentForm";
import { student } from "./fixtures";

const { api, auth } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../api/client", () => ({
  api,
  errorMessage: (error) => error.response?.data?.message || "Network error",
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));

const page = (content = [student], extra = {}) => ({
  data: {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: 1,
    ...extra,
  },
});
const directory = () =>
  render(
    <MemoryRouter>
      <StudentsPage />
    </MemoryRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  auth.user = { role: "ADMIN" };
  api.get.mockResolvedValue(page());
});

describe("Student directory", () => {
  it("renders API records and searches with server pagination", async () => {
    directory();
    expect(
      await screen.findByRole("link", { name: "Asha Sharma" }),
    ).toBeInTheDocument();
    await userEvent.type(screen.getByRole("searchbox"), "asha");
    await waitFor(() =>
      expect(api.get).toHaveBeenLastCalledWith(
        "/students",
        expect.objectContaining({
          params: expect.objectContaining({ search: "asha", page: 0 }),
        }),
      ),
    );
  });

  it("requests the next page and applies the status filter", async () => {
    api.get.mockResolvedValue(
      page([student], { totalElements: 21, totalPages: 3 }),
    );
    directory();
    await screen.findByRole("link", { name: "Asha Sharma" });
    await userEvent.click(screen.getByRole("button", { name: "Next page" }));
    await waitFor(() =>
      expect(api.get).toHaveBeenLastCalledWith(
        "/students",
        expect.objectContaining({
          params: expect.objectContaining({ page: 1 }),
        }),
      ),
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Filter by status"),
      "INACTIVE",
    );
    await waitFor(() =>
      expect(api.get).toHaveBeenLastCalledWith(
        "/students",
        expect.objectContaining({
          params: expect.objectContaining({ page: 0, status: "INACTIVE" }),
        }),
      ),
    );
  });

  it("shows an empty state rather than sample student data", async () => {
    api.get.mockResolvedValue(page([]));
    directory();
    expect(
      await screen.findByText("Your student directory starts here"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Asha Sharma")).not.toBeInTheDocument();
  });

  it("shows request errors and offers retry", async () => {
    api.get.mockRejectedValueOnce({
      response: { data: { message: "Service unavailable" } },
    });
    directory();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Service unavailable",
    );
    api.get.mockResolvedValue(page());
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(
      await screen.findByRole("link", { name: "Asha Sharma" }),
    ).toBeInTheDocument();
  });

  it("does not expose admin controls to teachers", () => {
    auth.user = { role: "TEACHER" };
    directory();
    expect(
      screen.queryByRole("button", { name: "Add student" }),
    ).not.toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
  });

  it("routes students to their own profile without fetching the directory", async () => {
    auth.user = { role: "STUDENT" };
    render(
      <MemoryRouter initialEntries={["/students"]}>
        <Routes>
          <Route path="/students" element={<StudentsPage />} />
          <Route path="/students/me" element={<p>Own profile</p>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(await screen.findByText("Own profile")).toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
  });
});

describe("Student form", () => {
  it("creates a record using entered values and converts the optional user ID", async () => {
    api.post.mockResolvedValue({ data: student });
    const saved = vi.fn();
    render(<StudentForm onSaved={saved} onClose={vi.fn()} />);
    const values = {
      fullName: student.fullName,
      email: student.email,
      phone: student.phone,
      dateOfBirth: student.dateOfBirth,
      address: student.address,
      guardianName: student.guardianName,
      guardianPhone: student.guardianPhone,
      admissionDate: student.admissionDate,
      userId: "42",
    };
    for (const [name, value] of Object.entries(values))
      fireEvent.change(document.getElementById(name), { target: { value } });
    await userEvent.click(screen.getByRole("button", { name: "Add student" }));
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith(
        "/students",
        expect.objectContaining({ fullName: "Asha Sharma", userId: 42 }),
      ),
    );
    expect(saved).toHaveBeenCalledWith(student);
  });

  it("updates an existing record without sending admission number or status", async () => {
    api.put.mockResolvedValue({ data: student });
    render(
      <StudentForm student={student} onSaved={vi.fn()} onClose={vi.fn()} />,
    );
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));
    await waitFor(() => expect(api.put).toHaveBeenCalled());
    const payload = api.put.mock.calls[0][1];
    expect(payload.userId).toBeNull();
    expect(payload).not.toHaveProperty("admissionNumber");
    expect(payload).not.toHaveProperty("status");
  });

  it("displays server validation errors and keeps the form open", async () => {
    api.put.mockRejectedValue({
      response: {
        data: {
          message: "Validation failed",
          errors: { email: "Email is already used" },
        },
      },
    });
    const saved = vi.fn();
    render(<StudentForm student={student} onSaved={saved} onClose={vi.fn()} />);
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));
    expect(
      await screen.findByText("Email is already used"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/^Email address/)).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(saved).not.toHaveBeenCalled();
  });
});

describe("Student details", () => {
  const details = (own = false) =>
    render(
      <MemoryRouter initialEntries={["/students/1"]}>
        <Routes>
          <Route
            path="/students/:id"
            element={<StudentDetailsPage own={own} />}
          />
        </Routes>
      </MemoryRouter>,
    );

  it("requires confirmation before deactivation and displays the result", async () => {
    api.get.mockResolvedValue({ data: student });
    api.patch.mockResolvedValue({ data: { ...student, status: "INACTIVE" } });
    details();
    await userEvent.click(
      await screen.findByRole("button", { name: "Deactivate" }),
    );
    expect(api.patch).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Deactivate student" }),
    );
    await waitFor(() =>
      expect(api.patch).toHaveBeenCalledWith("/students/1/deactivate"),
    );
    expect(await screen.findByText(/Student deactivated/)).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Deactivate" }),
    ).not.toBeInTheDocument();
  });

  it("shows a linked student profile without edit or deactivate controls", async () => {
    auth.user = { role: "STUDENT" };
    api.get.mockResolvedValue({ data: student });
    details(true);
    expect(
      await screen.findByRole("heading", { name: "Asha Sharma" }),
    ).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith("/students/me", expect.anything());
    expect(
      screen.queryByRole("button", { name: "Edit details" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Deactivate" }),
    ).not.toBeInTheDocument();
  });
});
