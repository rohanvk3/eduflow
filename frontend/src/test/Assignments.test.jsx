import { beforeEach, afterEach, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import AssignmentsPage from "../pages/AssignmentsPage";
import AssignmentFormPage from "../pages/AssignmentFormPage";
import AssignmentSubmitPage from "../pages/AssignmentSubmitPage";
import AssignmentReviewPage from "../pages/AssignmentReviewPage";
import SubmissionDetailsPage from "../pages/SubmissionDetailsPage";
import AssignmentDashboard from "../components/AssignmentDashboard";
import { AttachmentList, FilePicker } from "../components/AssignmentUI";
const { assignment, submission, auth, download } = vi.hoisted(() => ({
  assignment: {
    getAssignments: vi.fn(),
    getAssignment: vi.fn(),
    saveAssignment: vi.fn(),
    getAssignmentStats: vi.fn(),
    getUploadPolicy: vi.fn(),
    uploadAssignmentFiles: vi.fn(),
    getAssignmentFiles: vi.fn(),
    downloadAssignmentFile: vi.fn(),
  },
  submission: {
    getSubmission: vi.fn(),
    saveSubmission: vi.fn(),
    getMySubmissions: vi.fn(),
    getSubmissions: vi.fn(),
    getPendingRecipients: vi.fn(),
    getSubmissionHistory: vi.fn(),
    gradeSubmission: vi.fn(),
    downloadSubmissionFile: vi.fn(),
  },
  auth: { user: { role: "ADMIN", userId: 1, email: "admin@test" } },
  download: vi.fn(),
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/assignmentService", () => assignment);
vi.mock("../services/submissionService", () => submission);
vi.mock("../services/financeService", async () => ({
  ...(await vi.importActual("../services/financeService")),
  saveDownload: download,
}));
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange }) => (
    <label>
      {label}
      <input
        aria-label={label}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
const a = {
  id: 1,
  title: "Algebra Homework",
  batchId: 2,
  batchName: "Morning",
  courseName: "Math",
  createdBy: "Teacher",
  maxMarks: 100,
  dueDateTime: "2026-11-11T10:00:00Z",
  serverNow: "2026-11-11T09:00:00Z",
  allowLateSubmission: false,
  status: "PUBLISHED",
  version: 1,
  counts: {
    eligible: 2,
    submitted: 0,
    pending: 2,
    late: 0,
    graded: 0,
    completionPercentage: 0,
  },
};
const work = {
  id: 5,
  assignmentId: 1,
  assignmentTitle: a.title,
  batchName: "Morning",
  courseName: "Math",
  studentName: "Asha",
  admissionNumber: "EDU-3",
  studentId: 3,
  submissionText: "My saved work",
  submittedAt: a.serverNow,
  status: "SUBMITTED",
  late: false,
  maxMarks: 100,
  version: 0,
  attachments: [],
};
const page = {
  content: [a],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};
const stats = {
  totalAssignments: 1,
  publishedAssignments: 1,
  createdByMe: 1,
  eligibleSubmissions: 2,
  pendingSubmissions: 2,
  submittedAssignments: 0,
  lateSubmissions: 0,
  gradedSubmissions: 0,
  overdueAssignments: 0,
  completionPercentage: 0,
  gradingPercentage: null,
  averageMarks: null,
  averagePercentage: null,
  batches: [],
  upcoming: [a],
  recentSubmissions: [],
  serverNow: a.serverNow,
};
beforeEach(() => {
  vi.resetAllMocks();
  sessionStorage.clear();
  auth.user = { role: "ADMIN", userId: 1, email: "admin@test" };
  assignment.getAssignments.mockResolvedValue({ data: page });
  assignment.getAssignment.mockResolvedValue({ data: a });
  assignment.getAssignmentStats.mockResolvedValue({ data: stats });
  assignment.getAssignmentFiles.mockResolvedValue({ data: [] });
  assignment.getUploadPolicy.mockResolvedValue({
    data: { maxFileBytes: 10485760, maxFiles: 10 },
  });
  submission.getSubmission.mockResolvedValue({ data: work });
  submission.getSubmissionHistory.mockResolvedValue({
    data: { revisions: [], grades: [] },
  });
  submission.getSubmissions.mockResolvedValue({
    data: { ...page, content: [work] },
  });
  submission.getPendingRecipients.mockResolvedValue({
    data: {
      ...page,
      content: [
        {
          studentId: 4,
          studentName: "Pending Student",
          admissionNumber: "EDU-4",
        },
      ],
    },
  });
});
afterEach(() => vi.useRealTimers());
const mount = (element) => render(<MemoryRouter>{element}</MemoryRouter>);
const at = (path, route, element) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={route} element={element} />
        <Route path="/assignments/1" element={<p>Saved assignment</p>} />
      </Routes>
    </MemoryRouter>,
  );
it("directory loads live statistics and sends filters to the shared API service", async () => {
  mount(<AssignmentsPage />);
  expect(await screen.findByText(a.title)).toBeInTheDocument();
  await userEvent.type(screen.getByPlaceholderText("Title…"), "Algebra");
  await waitFor(() =>
    expect(assignment.getAssignments).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "Algebra", page: 0 }),
      expect.any(AbortSignal),
    ),
  );
  expect(screen.getByText("Pending submissions")).toBeInTheDocument();
});
it("parents do not load assignment data and students cannot create assignments", async () => {
  auth.user = { role: "PARENT" };
  const view = mount(<AssignmentsPage />);
  expect(screen.getByText("Assignment access restricted")).toBeInTheDocument();
  expect(assignment.getAssignments).not.toHaveBeenCalled();
  view.unmount();
  auth.user = { role: "STUDENT" };
  mount(<AssignmentsPage />);
  await screen.findByText(a.title);
  expect(screen.queryByText("Create Assignment")).not.toBeInTheDocument();
});
it("pending-homework shortcut sends the server-side student filter", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  at("/assignments?pendingOnly=true", "/assignments", <AssignmentsPage />);
  expect(
    await screen.findByLabelText("Show only pending homework"),
  ).toBeChecked();
  await waitFor(() =>
    expect(assignment.getAssignments).toHaveBeenCalledWith(
      expect.objectContaining({ pendingOnly: true }),
      expect.any(AbortSignal),
    ),
  );
});
it("create form converts IST to UTC and uploads selected files after the draft", async () => {
  assignment.saveAssignment.mockResolvedValue({
    data: { ...a, status: "DRAFT", version: 0 },
  });
  assignment.uploadAssignmentFiles.mockResolvedValue({ data: [] });
  at("/assignments/new", "/assignments/new", <AssignmentFormPage />);
  for (const [label, value] of [
    ["Assignment title *", "Algebra Homework"],
    ["Batch", "2"],
    ["Maximum marks *", "100"],
    ["Due date & time (IST) *", "2026-11-11T15:30"],
  ])
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  const input = screen.getByLabelText(/Attach files/);
  await waitFor(() => expect(input).not.toBeDisabled());
  const file = new File(["pdf"], "worksheet.pdf", { type: "application/pdf" });
  await userEvent.upload(input, file);
  await userEvent.click(screen.getByText("Save Draft"));
  expect(await screen.findByText("Saved assignment")).toBeInTheDocument();
  expect(assignment.saveAssignment).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({
      dueDateTime: "2026-11-11T10:00:00.000Z",
      batchId: 2,
    }),
  );
  expect(assignment.uploadAssignmentFiles).toHaveBeenCalledWith(
    1,
    0,
    [file],
    expect.any(Function),
  );
});
it("file picker uses server size policy and reports oversized files", async () => {
  assignment.getUploadPolicy.mockResolvedValue({
    data: { maxFileBytes: 2, maxFiles: 10 },
  });
  mount(<FilePicker files={[]} onChange={() => {}} />);
  const input = screen.getByLabelText(/Attach files/);
  await waitFor(() => expect(input).not.toBeDisabled());
  await userEvent.upload(
    input,
    new File(["too large"], "answer.pdf", { type: "application/pdf" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent("size limit");
});
it("student cannot submit empty homework", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  at(
    "/assignments/1/submit",
    "/assignments/:id/submit",
    <AssignmentSubmitPage />,
  );
  await userEvent.click(await screen.findByText("Submit Homework"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Add text");
  expect(submission.saveSubmission).not.toHaveBeenCalled();
});
it("student file-only submission is sent atomically with files", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  submission.saveSubmission.mockResolvedValue({ data: work });
  at(
    "/assignments/1/submit",
    "/assignments/:id/submit",
    <AssignmentSubmitPage />,
  );
  const input = await screen.findByLabelText(/Attach files/);
  await waitFor(() => expect(input).not.toBeDisabled());
  const file = new File(["pdf"], "answer.pdf", { type: "application/pdf" });
  await userEvent.upload(input, file);
  await userEvent.click(screen.getByText("Submit Homework"));
  await waitFor(() =>
    expect(submission.saveSubmission).toHaveBeenCalledWith(
      1,
      undefined,
      expect.objectContaining({ submissionText: "" }),
      [file],
      expect.any(Function),
    ),
  );
  expect(
    await screen.findByText("Homework submitted successfully."),
  ).toBeInTheDocument();
});
it("network errors preserve student work for retry", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  submission.saveSubmission.mockRejectedValue({
    response: {
      status: 500,
      data: { message: "Storage unavailable", errors: {} },
    },
  });
  at(
    "/assignments/1/submit",
    "/assignments/:id/submit",
    <AssignmentSubmitPage />,
  );
  const text = await screen.findByLabelText("Your response");
  await userEvent.type(text, "My detailed working");
  await userEvent.click(screen.getByText("Submit Homework"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Storage unavailable",
  );
  expect(text).toHaveValue("My detailed working");
  expect(sessionStorage.getItem("eduflow.homework.3.1")).toBe(
    "My detailed working",
  );
});
it("restored retry text remains clearly labeled as unsaved", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  sessionStorage.setItem("eduflow.homework.3.1", "Restored draft working");
  at(
    "/assignments/1/submit",
    "/assignments/:id/submit",
    <AssignmentSubmitPage />,
  );
  expect(await screen.findByLabelText("Your response")).toHaveValue(
    "Restored draft working",
  );
  expect(screen.getByText(/Unsaved changes/)).toBeInTheDocument();
});
it("graded student work is read-only and feedback is displayed", async () => {
  auth.user = { role: "STUDENT", userId: 3 };
  assignment.getAssignment.mockResolvedValue({
    data: { ...a, ownSubmissionId: 5 },
  });
  submission.getSubmission.mockResolvedValue({
    data: {
      ...work,
      status: "GRADED",
      obtainedMarks: 85,
      feedback: "Improve question three",
      gradedBy: "Teacher",
      gradedAt: a.serverNow,
    },
  });
  at(
    "/assignments/1/submit",
    "/assignments/:id/submit",
    <AssignmentSubmitPage />,
  );
  expect(await screen.findByText("Improve question three")).toBeInTheDocument();
  expect(screen.getByLabelText("Your response")).toBeDisabled();
  expect(screen.queryByText("Save Resubmission")).not.toBeInTheDocument();
});
it("teacher review includes a paginated pending-recipient view", async () => {
  at(
    "/assignments/1/submissions",
    "/assignments/:id/submissions",
    <AssignmentReviewPage />,
  );
  await screen.findByText("Asha");
  await userEvent.selectOptions(
    screen.getByLabelText("Submission status"),
    "PENDING",
  );
  expect(await screen.findByText("Pending Student")).toBeInTheDocument();
  expect(submission.getPendingRecipients).toHaveBeenCalledWith(
    "1",
    expect.objectContaining({ page: 0 }),
    expect.any(AbortSignal),
  );
});
it("teacher grades publish feedback using the current submission version", async () => {
  submission.gradeSubmission.mockResolvedValue({
    data: { ...work, status: "GRADED" },
  });
  at(
    "/assignments/submissions/5",
    "/assignments/submissions/:submissionId",
    <SubmissionDetailsPage />,
  );
  fireEvent.change(await screen.findByLabelText("Marks (maximum 100) *"), {
    target: { value: "85" },
  });
  await userEvent.type(screen.getByLabelText("Feedback"), "Good explanation");
  await userEvent.click(screen.getByText("Publish grade & feedback"));
  await waitFor(() =>
    expect(submission.gradeSubmission).toHaveBeenCalledWith(5, {
      version: 0,
      obtainedMarks: "85",
      feedback: "Good explanation",
      reason: "",
    }),
  );
});
it("grade corrections expose a mandatory reason", async () => {
  submission.getSubmission.mockResolvedValue({
    data: {
      ...work,
      status: "GRADED",
      obtainedMarks: 75,
      feedback: "First feedback",
    },
  });
  at(
    "/assignments/submissions/5",
    "/assignments/submissions/:submissionId",
    <SubmissionDetailsPage />,
  );
  const reason = await screen.findByLabelText("Correction reason *");
  expect(reason).toBeRequired();
  expect(screen.getByText("Save audited correction")).toBeInTheDocument();
});
it("attachment downloads use authenticated blob services", async () => {
  const blob = new Blob(["pdf"]);
  submission.downloadSubmissionFile.mockResolvedValue({ data: blob });
  mount(
    <AttachmentList
      files={[{ id: 7, originalFileName: "answer.pdf", fileSize: 100 }]}
      submission
    />,
  );
  await userEvent.click(screen.getByLabelText("Download answer.pdf"));
  await waitFor(() =>
    expect(download).toHaveBeenCalledWith(blob, "answer.pdf"),
  );
});
it("dashboard renders the real first-submission batch trend", async () => {
  assignment.getAssignmentStats.mockResolvedValue({
    data: {
      ...stats,
      activity: [
        { day: "2026-11-11", batchId: 2, batchName: "Morning", submissions: 1 },
      ],
    },
  });
  mount(<AssignmentDashboard />);
  expect(
    await screen.findByRole(
      "img",
      {
        name: "Daily first submissions by batch",
      },
      { timeout: 10000 },
    ),
  ).toBeInTheDocument();
}, 15000);
it("dashboard refreshes statistics after focus and mutation events", async () => {
  mount(<AssignmentDashboard />);
  await screen.findByText("Upcoming deadlines");
  const before = assignment.getAssignmentStats.mock.calls.length;
  fireEvent(window, new Event("eduflow:assignments-changed"));
  await waitFor(() =>
    expect(assignment.getAssignmentStats.mock.calls.length).toBeGreaterThan(
      before,
    ),
  );
});
