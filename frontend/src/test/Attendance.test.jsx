import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AttendancePage from "../pages/AttendancePage";
import AttendanceSessionPage from "../pages/AttendanceSessionPage";
import StudentAttendancePage from "../pages/StudentAttendancePage";
import AttendanceReportsPage from "../pages/AttendanceReportsPage";
import AttendanceCalendar from "../components/AttendanceCalendar";
import CreateAttendanceSession from "../components/CreateAttendanceSession";
import { localDate } from "../components/AttendanceUI";

const { service, academic, auth } = vi.hoisted(() => ({
  service: {
    createSession: vi.fn(),
    getSessions: vi.fn(),
    getBatchRegister: vi.fn(),
    getAttendanceOverview: vi.fn(),
    getSessionById: vi.fn(),
    saveAttendanceRecords: vi.fn(),
    finalizeSession: vi.fn(),
    getMyAttendance: vi.fn(),
    getAttendanceReports: vi.fn(),
    exportAttendanceReport: vi.fn(),
  },
  academic: { getSchedules: vi.fn(), getBatches: vi.fn() },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../services/attendanceService", () => service);
vi.mock("../services/academicService", () => academic);
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
vi.mock("../components/AttendanceTrend", () => ({
  default: () => <p>Trend chart</p>,
}));
const totals = {
  totalEligibleFinalizedSessions: 1,
  presentCount: 1,
  lateCount: 0,
  absentCount: 0,
  excusedCount: 0,
  attendancePercentage: 100,
  finalizedSessions: 1,
};
const empty = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 10,
};
const session = {
  id: 1,
  batchId: 2,
  batchName: "Math batch",
  courseName: "Mathematics",
  sessionDate: localDate(),
  startTime: "09:00:00",
  endTime: "10:00:00",
  topic: "Algebra",
  status: "DRAFT",
  version: 0,
  eligibleCount: 1,
  markedCount: 0,
  markedBy: "Admin",
};
const details = {
  session,
  students: [
    {
      studentId: 3,
      fullName: "Asha Student",
      admissionNumber: "EDU-1",
      status: null,
      remarks: null,
    },
  ],
  staleRecords: 0,
};
const groups = {
  ...empty,
  content: [{ id: 2, name: "Math batch", code: "MATH", totals }],
  totalElements: 1,
  totalPages: 1,
};
beforeEach(() => {
  vi.restoreAllMocks();
  vi.resetAllMocks();
  auth.user = { role: "ADMIN" };
  service.getSessions.mockResolvedValue({
    data: { ...empty, content: [session], totalElements: 21, totalPages: 3 },
  });
  service.getBatchRegister.mockResolvedValue({
    data: { ...empty, content: [session], totalElements: 1, totalPages: 1 },
  });
  service.getAttendanceOverview.mockResolvedValue({
    data: { totals, pendingDrafts: 1, batches: groups, recentFinalized: empty },
  });
  service.getSessionById.mockResolvedValue({ data: details });
  academic.getSchedules.mockResolvedValue({ data: [] });
  academic.getBatches.mockResolvedValue({ data: { ...empty, content: [] } });
});
const register = () =>
  render(
    <MemoryRouter initialEntries={["/attendance/sessions/1"]}>
      <Routes>
        <Route
          path="/attendance/sessions/:id"
          element={<AttendanceSessionPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
it("loads session lists from APIs and applies batch/date filters", async () => {
  render(
    <MemoryRouter>
      <AttendancePage />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("link", { name: "Mark attendance" }),
  ).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Batch"), { target: { value: "2" } });
  await waitFor(() =>
    expect(service.getBatchRegister).toHaveBeenCalledWith(
      "2",
      expect.objectContaining({ batchId: "2" }),
      expect.anything(),
    ),
  );
  fireEvent.change(screen.getByLabelText("From date"), {
    target: { value: "2026-01-01" },
  });
  await waitFor(() =>
    expect(service.getAttendanceOverview).toHaveBeenLastCalledWith(
      expect.objectContaining({ fromDate: "2026-01-01" }),
      expect.anything(),
    ),
  );
});
it("creates an actual session request without trusting markedBy", async () => {
  service.createSession.mockResolvedValue({ data: details });
  const created = vi.fn();
  render(
    <CreateAttendanceSession
      onCreated={created}
      onClose={vi.fn()}
      onBusyChange={vi.fn()}
    />,
  );
  fireEvent.change(screen.getByLabelText("Batch"), { target: { value: "2" } });
  await userEvent.click(screen.getByRole("button", { name: "Create session" }));
  await waitFor(() =>
    expect(service.createSession).toHaveBeenCalledWith(
      expect.objectContaining({
        batchId: 2,
        sessionDate: localDate(),
        scheduleId: null,
      }),
    ),
  );
  expect(service.createSession.mock.calls[0][0]).not.toHaveProperty("markedBy");
});
it("starts NOT_MARKED and saves explicit draft choices with the current version", async () => {
  service.saveAttendanceRecords.mockResolvedValue({
    data: {
      ...details,
      session: { ...session, version: 1, markedCount: 1 },
      students: [{ ...details.students[0], status: "LATE", remarks: "" }],
    },
  });
  register();
  const status = await screen.findByLabelText("Attendance for Asha Student");
  expect(status).toHaveValue("");
  expect(
    screen.getByRole("button", { name: "Finalize Attendance" }),
  ).toBeDisabled();
  await userEvent.selectOptions(status, "LATE");
  await userEvent.click(screen.getByRole("button", { name: "Save Draft" }));
  await waitFor(() =>
    expect(service.saveAttendanceRecords).toHaveBeenCalledWith("1", {
      version: 0,
      records: [{ studentId: 3, status: "LATE", remarks: "" }],
    }),
  );
  expect(
    await screen.findByText("Draft saved. Unmarked students remain unmarked."),
  ).toBeInTheDocument();
});
it("bulk marking requires explicit action and finalization follows a saved draft", async () => {
  const marked = {
    ...details,
    session: { ...session, version: 1, markedCount: 1 },
    students: [{ ...details.students[0], status: "PRESENT", remarks: "" }],
  };
  service.saveAttendanceRecords.mockResolvedValue({ data: marked });
  service.finalizeSession.mockResolvedValue({
    data: {
      ...marked,
      session: { ...marked.session, status: "FINALIZED", version: 2 },
    },
  });
  register();
  await screen.findByText("Asha Student");
  expect(service.saveAttendanceRecords).not.toHaveBeenCalled();
  await userEvent.click(
    screen.getByRole("button", { name: "Apply to visible students" }),
  );
  await userEvent.click(screen.getByRole("button", { name: "Save Draft" }));
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Finalize Attendance" }),
    ).toBeEnabled(),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Finalize Attendance" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Finalize attendance" }),
  );
  await waitFor(() =>
    expect(service.finalizeSession).toHaveBeenCalledWith("1", 1),
  );
  expect(
    screen.queryByRole("button", { name: "Save Draft" }),
  ).not.toBeInTheDocument();
});
it("finalized registers have no edit controls", async () => {
  service.getSessionById.mockResolvedValue({
    data: {
      ...details,
      session: { ...session, status: "FINALIZED" },
      students: [{ ...details.students[0], status: "ABSENT" }],
    },
  });
  register();
  expect(await screen.findByText("ABSENT")).toBeInTheDocument();
  expect(
    screen.queryByLabelText("Attendance for Asha Student"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Save Draft" }),
  ).not.toBeInTheDocument();
});
it("preserves unsaved draft choices when the API rejects a stale version", async () => {
  service.saveAttendanceRecords.mockRejectedValue({
    response: {
      status: 409,
      data: { message: "Attendance changed; reload before saving" },
    },
  });
  register();
  await userEvent.selectOptions(
    await screen.findByLabelText("Attendance for Asha Student"),
    "EXCUSED",
  );
  await userEvent.click(screen.getByRole("button", { name: "Save Draft" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Attendance changed",
  );
  expect(screen.getByLabelText("Attendance for Asha Student")).toHaveValue(
    "EXCUSED",
  );
});
it("warns and blocks link navigation when changes are unsaved", async () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  register();
  await userEvent.selectOptions(
    await screen.findByLabelText("Attendance for Asha Student"),
    "PRESENT",
  );
  await userEvent.click(
    screen.getByRole("link", { name: "← Back to attendance" }),
  );
  expect(confirm).toHaveBeenCalled();
  const navigation = new Event("eduflow:before-navigation", {
    cancelable: true,
  });
  expect(window.dispatchEvent(navigation)).toBe(false);
  expect(
    screen.getByLabelText("Attendance for Asha Student"),
  ).toBeInTheDocument();
});
it("students only request their own history, never the staff roster or list", async () => {
  auth.user = { role: "STUDENT" };
  service.getMyAttendance.mockResolvedValue({
    data: {
      linked: true,
      totals,
      batches: groups,
      trend: [],
      history: {
        ...empty,
        content: [{ ...session, sessionId: 1, status: "PRESENT" }],
        totalElements: 1,
        totalPages: 1,
      },
    },
  });
  render(
    <MemoryRouter>
      <AttendancePage />
    </MemoryRouter>,
  );
  expect(
    await screen.findByRole("heading", { name: "My attendance" }),
  ).toBeInTheDocument();
  expect(await screen.findByText("PRESENT")).toBeInTheDocument();
  expect(service.getSessions).not.toHaveBeenCalled();
  expect(service.getSessionById).not.toHaveBeenCalled();
});
it("unlinked students receive an informative empty state", async () => {
  service.getMyAttendance.mockResolvedValue({ data: { linked: false } });
  render(<StudentAttendancePage />);
  expect(
    await screen.findByText("Your student profile is not linked yet"),
  ).toBeInTheDocument();
});
it("calendar renders distinct same-day sessions", async () => {
  service.getBatchRegister.mockResolvedValue({
    data: {
      ...empty,
      content: [
        session,
        {
          ...session,
          id: 2,
          startTime: "11:00:00",
          topic: "Geometry",
          status: "FINALIZED",
        },
      ],
      totalElements: 2,
      totalPages: 1,
    },
  });
  render(
    <MemoryRouter>
      <AttendanceCalendar batchId={2} />
    </MemoryRouter>,
  );
  expect(await screen.findByRole("link", { name: /Algebra/ })).toHaveAttribute(
    "href",
    "/attendance/sessions/1",
  );
  expect(screen.getByRole("link", { name: /Geometry/ })).toHaveAttribute(
    "href",
    "/attendance/sessions/2",
  );
});
it("admin reports and CSV export share the same filters", async () => {
  service.getAttendanceReports.mockResolvedValue({
    data: {
      totals,
      students: empty,
      batches: groups,
      trend: [],
      fromDate: localDate(),
      toDate: localDate(),
    },
  });
  service.exportAttendanceReport.mockResolvedValue({
    data: new Blob(["CSV"], { type: "text/csv" }),
  });
  URL.createObjectURL = vi.fn(() => "blob:test");
  URL.revokeObjectURL = vi.fn();
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  render(
    <MemoryRouter>
      <AttendanceReportsPage />
    </MemoryRouter>,
  );
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Export CSV" })).toBeEnabled(),
  );
  fireEvent.change(screen.getByLabelText("Batch"), { target: { value: "2" } });
  await waitFor(() =>
    expect(service.getAttendanceReports).toHaveBeenLastCalledWith(
      expect.objectContaining({ batchId: "2" }),
      expect.anything(),
    ),
  );
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Export CSV" })).toBeEnabled(),
  );
  await userEvent.click(screen.getByRole("button", { name: "Export CSV" }));
  await waitFor(() =>
    expect(service.exportAttendanceReport).toHaveBeenCalledWith(
      expect.objectContaining({
        batchId: "2",
        fromDate: expect.any(String),
        toDate: expect.any(String),
      }),
    ),
  );
});
it("parent users and teachers never load admin reports", () => {
  auth.user = { role: "TEACHER" };
  render(
    <MemoryRouter>
      <AttendanceReportsPage />
    </MemoryRouter>,
  );
  expect(screen.getByRole("alert")).toHaveTextContent("administrators only");
  expect(service.getAttendanceReports).not.toHaveBeenCalled();
});
