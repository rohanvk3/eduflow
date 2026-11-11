import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AcademicCatalogPage from "../pages/AcademicCatalogPage";
import AcademicForm from "../components/AcademicForm";
import BatchDetailsPage from "../pages/BatchDetailsPage";
import BatchOperationForm from "../components/BatchOperationForm";
const { service, auth } = vi.hoisted(() => ({
  service: {
    getCourses: vi.fn(),
    getBatches: vi.fn(),
    getBatch: vi.fn(),
    getAssignments: vi.fn(),
    getSchedules: vi.fn(),
    getEnrollments: vi.fn(),
    saveCourse: vi.fn(),
    saveBatch: vi.fn(),
    assignTeacher: vi.fn(),
    enrollStudent: vi.fn(),
    saveSchedule: vi.fn(),
    withdrawStudent: vi.fn(),
    unassignTeacher: vi.fn(),
  },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../services/academicService", () => service);
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange }) => (
    <label>
      {label}
      <input
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
const course = {
  id: 1,
  code: "MATH",
  name: "Mathematics",
  status: "ACTIVE",
  durationWeeks: 12,
};
const batch = {
  id: 2,
  code: "B1",
  name: "Morning batch",
  courseId: 1,
  courseName: "Mathematics",
  status: "ACTIVE",
  capacity: 20,
  activeEnrollments: 1,
  startDate: "2026-01-01",
  endDate: "2026-12-31",
};
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { role: "ADMIN" };
  service.getCourses.mockResolvedValue({
    data: { content: [course], totalElements: 21, totalPages: 3 },
  });
  service.getBatches.mockResolvedValue({
    data: { content: [batch], totalElements: 1, totalPages: 1 },
  });
  service.getBatch.mockResolvedValue({ data: batch });
  service.getAssignments.mockResolvedValue({ data: [] });
  service.getSchedules.mockResolvedValue({ data: [] });
  service.getEnrollments.mockResolvedValue({
    data: { content: [], totalElements: 0, totalPages: 0 },
  });
});
it("loads courses, searches and paginates using the API", async () => {
  render(
    <MemoryRouter>
      <AcademicCatalogPage kind="courses" />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("link", { name: "Mathematics" }),
  ).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Next page" }));
  await waitFor(() =>
    expect(service.getCourses).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1 }),
      expect.anything(),
    ),
  );
  await userEvent.type(screen.getByRole("searchbox"), "math");
  await waitFor(() =>
    expect(service.getCourses).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "math", page: 0 }),
      expect.anything(),
    ),
  );
});
it("creates a course with validated form values", async () => {
  service.saveCourse.mockResolvedValue({ data: course });
  const saved = vi.fn();
  render(<AcademicForm kind="courses" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.change(document.getElementById("code"), {
    target: { value: "MATH" },
  });
  fireEvent.change(document.getElementById("name"), {
    target: { value: "Mathematics" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
  await waitFor(() => expect(saved).toHaveBeenCalledWith(course));
  expect(service.saveCourse).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({ code: "MATH", durationWeeks: 12 }),
  );
});
it("creates a course-linked batch with numeric capacity", async () => {
  service.saveBatch.mockResolvedValue({ data: batch });
  render(<AcademicForm kind="batches" onClose={vi.fn()} onSaved={vi.fn()} />);
  fireEvent.change(document.getElementById("code"), {
    target: { value: "B1" },
  });
  fireEvent.change(document.getElementById("name"), {
    target: { value: "Morning" },
  });
  fireEvent.change(screen.getByLabelText("Course"), { target: { value: "1" } });
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(service.saveBatch).toHaveBeenCalledWith(
      undefined,
      expect.objectContaining({ courseId: 1, capacity: 30 }),
    ),
  );
});
it("student batch view never requests the roster and hides management controls", async () => {
  auth.user = { role: "STUDENT" };
  render(
    <MemoryRouter initialEntries={["/batches/2"]}>
      <Routes>
        <Route path="/batches/:id" element={<BatchDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("heading", { name: "Morning batch" }),
  ).toBeInTheDocument();
  expect(service.getEnrollments).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("button", { name: "Assign teacher" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Enroll student" }),
  ).not.toBeInTheDocument();
});
it("enrolls an existing student and retains errors on failure", async () => {
  service.enrollStudent.mockRejectedValueOnce({
    response: { data: { message: "Batch capacity is full" } },
  });
  render(
    <BatchOperationForm
      action="enroll"
      batchId={2}
      assignments={[]}
      onClose={vi.fn()}
      onSaved={vi.fn()}
      onBusyChange={vi.fn()}
    />,
  );
  fireEvent.change(screen.getByLabelText("Student"), {
    target: { value: "5" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Batch capacity is full",
  );
  expect(service.enrollStudent).toHaveBeenCalledWith(
    2,
    expect.objectContaining({ studentId: 5 }),
  );
});
it("edits an actual timetable entry and persists an inactive status", async () => {
  service.saveSchedule.mockResolvedValue({ data: {} });
  const saved = vi.fn();
  render(
    <BatchOperationForm
      action="schedule"
      batchId={2}
      schedule={{
        id: 4,
        teacherId: 3,
        dayOfWeek: "MONDAY",
        startTime: "09:00:00",
        endTime: "10:00:00",
        active: true,
      }}
      assignments={[{ teacherId: 3, fullName: "Faculty", active: true }]}
      onClose={vi.fn()}
      onSaved={saved}
      onBusyChange={vi.fn()}
    />,
  );
  await userEvent.selectOptions(
    screen.getByLabelText("Schedule status"),
    "false",
  );
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(service.saveSchedule).toHaveBeenCalledWith(
      2,
      4,
      expect.objectContaining({ active: false, startTime: "09:00" }),
    ),
  );
});
it("confirms withdrawal instead of silently changing enrollment history", async () => {
  service.getEnrollments.mockResolvedValue({
    data: {
      content: [
        {
          id: 5,
          fullName: "Student",
          admissionNumber: "EDU-1",
          enrollmentDate: "2026-01-01",
          status: "ACTIVE",
        },
      ],
      totalElements: 1,
      totalPages: 1,
    },
  });
  service.withdrawStudent.mockResolvedValue({ data: {} });
  render(
    <MemoryRouter initialEntries={["/batches/2"]}>
      <Routes>
        <Route path="/batches/:id" element={<BatchDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  );
  await userEvent.click(
    await screen.findByRole("button", { name: "Withdraw" }),
  );
  expect(service.withdrawStudent).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(service.withdrawStudent).toHaveBeenCalledWith(
      "2",
      5,
      expect.any(String),
    ),
  );
});
it("renders request failures with retry and honest empty states", async () => {
  service.getCourses.mockRejectedValueOnce({
    response: { data: { message: "Unavailable" } },
  });
  render(
    <MemoryRouter>
      <AcademicCatalogPage kind="courses" />
    </MemoryRouter>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent("Unavailable");
  service.getCourses.mockResolvedValue({
    data: { content: [], totalElements: 0, totalPages: 0 },
  });
  await userEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("No courses found")).toBeInTheDocument();
});
