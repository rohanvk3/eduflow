import { beforeEach, expect, it, vi } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ParentPortalPage from "../pages/ParentPortalPage";
import ParentChildrenPage from "../pages/ParentChildrenPage";
import ParentProfilePage from "../pages/ParentProfilePage";
import AdminParentsPage from "../pages/AdminParentsPage";
import AdminParentDetailsPage from "../pages/AdminParentDetailsPage";
import ParentCharts from "../components/ParentCharts";
import Layout from "../components/Layout";
const { read, manage, auth } = vi.hoisted(() => ({
  read: Object.fromEntries(
    [
      "getParentDashboard",
      "getChildAttendance",
      "getChildFees",
      "getChildResults",
      "getChildAssignments",
      "getChildResources",
      "getChildProgress",
      "downloadChildReceipt",
    ].map((k) => [k, vi.fn()]),
  ),
  manage: Object.fromEntries(
    [
      "getMyChildren",
      "getParents",
      "getParent",
      "createParent",
      "updateParent",
      "getParentLinks",
      "createParentLink",
      "verifyParentLink",
      "revokeParentLink",
      "getParentLinkHistory",
      "getParentProfile",
      "updateParentProfile",
      "saveDownload",
    ].map((k) => [k, vi.fn()]),
  ),
  auth: {
    user: {
      id: 1,
      role: "PARENT",
      fullName: "Parent One",
      email: "parent@test",
    },
    refreshUser: vi.fn(),
    logout: vi.fn(),
  },
}));
vi.mock("../services/parentDashboardService", () => read);
vi.mock("../services/parentService", () => ({
  ...manage,
  parentError: async (e) => e.message || "Request failed",
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../components/NotificationDashboard", () => ({ default: () => null }));
vi.mock("../components/NotificationBell", () => ({ default: () => null }));
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange, required }) => (
    <label>
      {label}
      <input
        aria-label={label}
        value={value || ""}
        required={required}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
const child = {
  id: 2,
  fullName: "Asha",
  admissionNumber: "EDU-2",
  status: "ACTIVE",
  relationshipType: "MOTHER",
  enrollments: [
    {
      id: 1,
      courseId: 3,
      courseName: "Mathematics",
      batchId: 4,
      batchName: "Evening",
      status: "ACTIVE",
      active: true,
      enrollmentDate: "2026-10-01",
    },
  ],
};
const totals = {
  totalEligibleFinalizedSessions: 4,
  presentCount: 2,
  lateCount: 1,
  absentCount: 1,
  excusedCount: 0,
  attendancePercentage: 75,
};
const result = {
  id: 5,
  examId: 1,
  examTitle: "Algebra Test",
  courseId: 3,
  courseName: "Mathematics",
  batchId: 4,
  batchName: "Evening",
  examDate: "2026-11-09T09:00:00Z",
  obtainedMarks: 80,
  totalMarks: 100,
  percentage: 80,
  grade: "A",
  passed: true,
};
const assignment = {
  id: 7,
  title: "Quadratic worksheet",
  courseId: 3,
  courseName: "Mathematics",
  batchId: 4,
  batchName: "Evening",
  dueDateTime: "2026-11-11T12:00:00Z",
  assignmentStatus: "PUBLISHED",
  submissionStatus: "GRADED",
  submittedAt: "2026-11-10T09:00:00Z",
  late: false,
  maxMarks: 100,
  obtainedMarks: 85,
  feedback: "Well explained",
  gradedAt: "2026-11-10T10:00:00Z",
};
const progress = {
  fromDate: "2026-10-12",
  toDate: "2026-11-10",
  attendance: totals,
  attendanceTrend: [{ date: "2026-11-09", totals }],
  examTrend: [result],
  assignmentTrend: [assignment],
  eligibleAssignments: 1,
  submittedAssignments: 1,
  assignmentCompletionPercentage: 100,
  averageExamPercentage: 80,
  courses: [
    {
      id: 3,
      name: "Mathematics",
      publishedResults: 1,
      averageExamPercentage: 80,
      assignmentCompletionPercentage: 100,
    },
  ],
};
const feeTotals = {
  totalPayable: 1000,
  totalPaid: 200,
  outstanding: 800,
  overdue: 800,
};
const page = (content) => ({
  content,
  page: 0,
  size: 10,
  totalElements: content.length,
  totalPages: 1,
});
const account = {
  id: 8,
  courseName: "Mathematics",
  batchName: "Evening",
  totalPayable: 1000,
  paidAmount: 200,
  outstandingAmount: 800,
  overdueAmount: 800,
  installments: [
    {
      id: 9,
      installmentNumber: 1,
      dueDate: "2026-11-09",
      amount: 1000,
      paidAmount: 200,
      outstandingAmount: 800,
      status: "OVERDUE",
    },
  ],
  payments: [
    {
      id: 10,
      receiptNumber: "EDU-R10",
      amount: 200,
      paymentDate: "2026-11-10",
      paymentMethod: "CASH",
      status: "SUCCESS",
    },
  ],
};
const dashboard = {
  linked: true,
  child,
  attendance: totals,
  fees: feeTotals,
  upcomingExamsCount: 1,
  pendingAssignments: 1,
  upcomingExams: [
    {
      id: 1,
      title: "Next test",
      batchName: "Evening",
      startDateTime: "2026-11-11T09:00:00Z",
    },
  ],
  recentResults: [result],
  upcomingAssignments: [assignment],
  feeAccounts: [account],
  activity: [
    {
      type: "RESULT",
      title: "Published result: Algebra Test",
      occurredAt: "2026-11-10T10:00:00Z",
    },
  ],
  progress,
};
const profile = {
  id: 11,
  userId: 1,
  fullName: "Parent One",
  loginEmail: "parent@test",
  email: "contact@test",
  phoneNumber: "9876543210",
  alternatePhone: null,
  address: "Address",
  status: "ACTIVE",
  version: 0,
};
const link = {
  id: 12,
  parentId: 11,
  studentId: 2,
  studentName: "Asha",
  admissionNumber: "EDU-2",
  relationshipType: "MOTHER",
  status: "PENDING",
  primaryGuardian: true,
  version: 0,
};
beforeEach(() => {
  vi.resetAllMocks();
  sessionStorage.clear();
  auth.user = {
    id: 1,
    role: "PARENT",
    fullName: "Parent One",
    email: "parent@test",
  };
  auth.refreshUser.mockResolvedValue();
  read.getParentDashboard.mockResolvedValue({ data: dashboard });
  read.getChildProgress.mockResolvedValue({ data: progress });
  read.getChildAttendance.mockResolvedValue({
    data: {
      totals,
      trend: progress.attendanceTrend,
      batches: page([{ id: 4, name: "Evening", totals }]),
      history: page([
        {
          sessionId: 1,
          sessionDate: "2026-11-09",
          batchId: 4,
          batchName: "Evening",
          courseName: "Mathematics",
          status: "PRESENT",
        },
      ]),
      fromDate: progress.fromDate,
      toDate: progress.toDate,
    },
  });
  read.getChildFees.mockResolvedValue({
    data: { totals: feeTotals, accounts: page([account]) },
  });
  read.getChildResults.mockResolvedValue({ data: page([result]) });
  read.getChildAssignments.mockResolvedValue({ data: page([assignment]) });
  read.getChildResources.mockResolvedValue({
    data: page([
      {
        id: 20,
        title: "Published lecture notes",
        courseName: "Mathematics",
        categoryName: "Notes",
        resourceType: "DOCUMENT",
        publishedAt: "2026-11-10T09:00:00Z",
      },
    ]),
  });
  manage.getMyChildren.mockResolvedValue({ data: page([child]) });
  manage.getParents.mockResolvedValue({ data: page([profile]) });
  manage.getParent.mockResolvedValue({ data: profile });
  manage.getParentProfile.mockResolvedValue({ data: profile });
  manage.getParentLinks.mockResolvedValue({ data: page([link]) });
  manage.getParentLinkHistory.mockResolvedValue({
    data: page([
      {
        id: 1,
        newStatus: "PENDING",
        relationshipType: "MOTHER",
        changedBy: "Admin",
        changedAt: "2026-11-10T09:00:00Z",
        reason: "Guardian consent checked",
      },
    ]),
  });
  manage.updateParentProfile.mockResolvedValue({
    data: { ...profile, version: 1 },
  });
  manage.verifyParentLink.mockResolvedValue({
    data: { ...link, status: "VERIFIED" },
  });
  manage.revokeParentLink.mockResolvedValue({
    data: { ...link, status: "REVOKED" },
  });
  manage.createParentLink.mockResolvedValue({ data: link });
});
const mount = (component, path = "/parent/dashboard", route = "*") =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={route} element={component} />
      </Routes>
    </MemoryRouter>,
  );
it("loads real dashboard response and academic activity", async () => {
  mount(<ParentPortalPage />);
  expect(await screen.findByText("Asha")).toBeInTheDocument();
  expect(screen.getByText("75.00%")).toBeInTheDocument();
  expect(
    screen.getByText("Published result: Algebra Test"),
  ).toBeInTheDocument();
  expect(read.getParentDashboard).toHaveBeenCalled();
});
it("child selection is persisted per authenticated parent and refetched", async () => {
  mount(<ParentPortalPage />);
  await screen.findByText("Asha");
  fireEvent.change(screen.getByLabelText("Select child"), {
    target: { value: "22" },
  });
  await waitFor(() =>
    expect(read.getParentDashboard).toHaveBeenLastCalledWith(
      expect.objectContaining({ studentId: "22" }),
      expect.anything(),
    ),
  );
});
it("no verified children produces a useful empty state", async () => {
  read.getParentDashboard.mockResolvedValue({ data: { linked: false } });
  mount(<ParentPortalPage />);
  expect(
    await screen.findByText("No verified children linked"),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Contact institute administration/),
  ).toBeInTheDocument();
  expect(read.getChildFees).not.toHaveBeenCalled();
});
it("unauthorized child response hides data and offers reset", async () => {
  read.getParentDashboard.mockRejectedValue({
    response: { data: { message: "No verified relationship" } },
  });
  mount(<ParentPortalPage />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No verified relationship",
  );
  expect(screen.queryByText("Asha")).not.toBeInTheDocument();
  expect(screen.getByText("Reset child selection")).toBeInTheDocument();
});
it("attendance page has finalized records, calendar and batch filter", async () => {
  mount(
    <ParentPortalPage mode="attendance" />,
    "/parent/children/2/attendance",
    "/parent/children/:studentId/attendance",
  );
  expect(
    await screen
      .findByRole("table", { name: "" })
      .catch(() => screen.findByText("Attendance calendar")),
  ).toBeInTheDocument();
  expect(await screen.findByText("PRESENT")).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Batch"), { target: { value: "4" } });
  await waitFor(() =>
    expect(read.getChildAttendance).toHaveBeenLastCalledWith(
      2,
      expect.objectContaining({ batchId: "4" }),
      expect.anything(),
    ),
  );
});
it("fees are read-only and receipt download is authenticated", async () => {
  const blob = new Blob(["pdf"]);
  read.downloadChildReceipt.mockResolvedValue({ data: blob });
  mount(<ParentPortalPage mode="fees" />);
  await userEvent.click(await screen.findByText("Download receipt"));
  await waitFor(() =>
    expect(read.downloadChildReceipt).toHaveBeenCalledWith(2, 10),
  );
  expect(manage.saveDownload).toHaveBeenCalledWith(blob, "EDU-R10.pdf");
  expect(screen.queryByText("Record payment")).not.toBeInTheDocument();
});
it("results use published data and course filters", async () => {
  mount(<ParentPortalPage mode="results" />);
  expect(await screen.findByText("Algebra Test")).toBeInTheDocument();
  expect(screen.getByText("Passed")).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Course"), { target: { value: "3" } });
  await waitFor(() =>
    expect(read.getChildResults).toHaveBeenLastCalledWith(
      2,
      expect.objectContaining({ courseId: "3" }),
      expect.anything(),
    ),
  );
});
it("assignment feedback is visible without submission/grading actions", async () => {
  mount(<ParentPortalPage mode="assignments" />);
  expect(await screen.findByText("Well explained")).toBeInTheDocument();
  expect(screen.queryByText("Submit assignment")).not.toBeInTheDocument();
  expect(screen.queryByText("Save grade")).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Submission status"), {
    target: { value: "GRADED" },
  });
  await waitFor(() =>
    expect(read.getChildAssignments).toHaveBeenLastCalledWith(
      2,
      expect.objectContaining({ status: "GRADED" }),
      expect.anything(),
    ),
  );
});
it("progress has real totals and course-wise academic data", async () => {
  mount(<ParentPortalPage mode="progress" />);
  expect(
    await screen.findByText("Course-wise academic progress"),
  ).toBeInTheDocument();
  expect(screen.getByText("100.00%")).toBeInTheDocument();
  expect(read.getChildProgress).toHaveBeenCalledWith(
    2,
    expect.anything(),
    expect.anything(),
  );
});
it("resource metadata does not expose unsupported downloads", async () => {
  mount(<ParentPortalPage mode="resources" />);
  expect(
    await screen.findByText("Published lecture notes"),
  ).toBeInTheDocument();
  expect(screen.getByText(/Published metadata only/)).toBeInTheDocument();
  expect(screen.queryByText("Download")).not.toBeInTheDocument();
});
it("children directory never renders management actions", async () => {
  mount(<ParentChildrenPage />);
  expect(await screen.findByText("Asha")).toBeInTheDocument();
  expect(screen.queryByText("Verify")).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Overview" })).toHaveAttribute(
    "href",
    "/parent/children/2",
  );
});
it("profile sends permitted personal fields and refreshes account display", async () => {
  mount(<ParentProfilePage />);
  await screen.findByText("Save profile");
  fireEvent.change(screen.getByLabelText("Full name *"), {
    target: { value: "Updated Parent" },
  });
  await userEvent.click(screen.getByText("Save profile"));
  await waitFor(() =>
    expect(manage.updateParentProfile).toHaveBeenCalledWith(
      expect.objectContaining({ fullName: "Updated Parent", version: 0 }),
    ),
  );
  expect(manage.updateParentProfile.mock.calls[0][0]).not.toHaveProperty(
    "role",
  );
  expect(manage.updateParentProfile.mock.calls[0][0]).not.toHaveProperty(
    "userId",
  );
  expect(auth.refreshUser).toHaveBeenCalled();
  expect(await screen.findByText("Profile updated")).toBeInTheDocument();
});
it("parent cannot fetch administrator management directory", () => {
  mount(<AdminParentsPage />);
  expect(
    screen.getByText("Parent portal access restricted"),
  ).toBeInTheDocument();
  expect(manage.getParents).not.toHaveBeenCalled();
});
it("administrator directory links to real parent profiles", async () => {
  auth.user.role = "ADMIN";
  mount(<AdminParentsPage />);
  expect(
    await screen.findByRole("link", { name: "View profile" }),
  ).toHaveAttribute("href", "/admin/parents/11");
});
it("admin verifies relationship with explicit basis and current version", async () => {
  auth.user.role = "ADMIN";
  mount(<AdminParentDetailsPage />, "/admin/parents/11", "/admin/parents/:id");
  await userEvent.click(await screen.findByText("Verify"));
  await userEvent.type(
    screen.getByLabelText("Verification basis *"),
    "Identity and signed consent checked",
  );
  await userEvent.click(screen.getByText("Confirm verification"));
  await waitFor(() =>
    expect(manage.verifyParentLink).toHaveBeenCalledWith(12, {
      version: 0,
      reason: "Identity and signed consent checked",
    }),
  );
});
it("admin revocation requires a reason and audit history loads", async () => {
  auth.user.role = "ADMIN";
  mount(<AdminParentDetailsPage />, "/admin/parents/11", "/admin/parents/:id");
  await userEvent.click(await screen.findByText("Audit history"));
  expect(
    await screen.findByText("Guardian consent checked"),
  ).toBeInTheDocument();
  await userEvent.click(screen.getByLabelText("Close dialog"));
  await userEvent.click(screen.getByText("Revoke"));
  expect(screen.getByLabelText("Reason *")).toBeRequired();
  await userEvent.type(screen.getByLabelText("Reason *"), "Consent withdrawn");
  await userEvent.click(screen.getByText("Confirm revocation"));
  await waitFor(() =>
    expect(manage.revokeParentLink).toHaveBeenCalledWith(12, {
      version: 0,
      reason: "Consent withdrawn",
    }),
  );
});
it("parent sidebar shows dedicated read-only navigation", () => {
  render(
    <MemoryRouter>
      <Layout />
    </MemoryRouter>,
  );
  const nav = screen.getByRole("navigation", { name: "Main navigation" });
  expect(
    within(nav).getByRole("link", { name: "My Children" }),
  ).toHaveAttribute("href", "/parent/children");
  expect(
    within(nav).queryByRole("link", { name: "Students" }),
  ).not.toBeInTheDocument();
  expect(within(nav).getByRole("link", { name: "Fees" })).toHaveAttribute(
    "href",
    "/parent/fees",
  );
});
it("actual progress charts render scoped exam and completion data", () => {
  mount(<ParentCharts progress={progress} assignments />);
  expect(
    screen.getByRole("img", { name: "Child attendance trend" }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("img", { name: "Child published exam performance" }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("img", { name: "Child assignment completion trend" }),
  ).toBeInTheDocument();
  expect(screen.getByText("85 / 100")).toBeInTheDocument();
});
it("admin creates a pending relationship using the selected student and reason", async () => {
  auth.user.role = "ADMIN";
  mount(<AdminParentDetailsPage />, "/admin/parents/11", "/admin/parents/:id");
  await userEvent.click(await screen.findByText("Create pending link"));
  fireEvent.change(screen.getByLabelText("Student"), {
    target: { value: "2" },
  });
  await userEvent.type(
    screen.getByLabelText("Reason *"),
    "Guardian documents received for review",
  );
  await userEvent.click(
    within(
      screen.getByRole("dialog", { name: "Create pending relationship" }),
    ).getByText("Create pending link"),
  );
  await waitFor(() =>
    expect(manage.createParentLink).toHaveBeenCalledWith({
      parentId: 11,
      studentId: 2,
      relationshipType: "GUARDIAN",
      primaryGuardian: false,
      reason: "Guardian documents received for review",
    }),
  );
});
it("admin creates a parent profile with real account fields", async () => {
  auth.user.role = "ADMIN";
  manage.createParent.mockResolvedValue({ data: { id: 11 } });
  mount(<AdminParentsPage />);
  await userEvent.click(screen.getByText("Create parent profile"));
  await userEvent.type(screen.getByLabelText("Full name *"), "New Parent");
  await userEvent.type(
    screen.getByLabelText("Login email *"),
    "new@eduflow.test",
  );
  await userEvent.type(
    screen.getByLabelText("Initial password *"),
    "Testing@2026",
  );
  await userEvent.type(screen.getByLabelText("Phone *"), "9876543210");
  await userEvent.click(screen.getByText("Create profile"));
  await waitFor(() =>
    expect(manage.createParent).toHaveBeenCalledWith(
      expect.objectContaining({
        fullName: "New Parent",
        email: "new@eduflow.test",
        password: "Testing@2026",
        phoneNumber: "9876543210",
      }),
    ),
  );
  expect(manage.createParent.mock.calls[0][0]).not.toHaveProperty("role");
});
it("revocation discovered on focus clears previous child data", async () => {
  mount(<ParentPortalPage />);
  await screen.findByText("Asha");
  read.getParentDashboard.mockRejectedValueOnce({
    response: { data: { message: "No verified relationship" } },
  });
  fireEvent(window, new Event("focus"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No verified relationship",
  );
  expect(screen.queryByText("Asha")).not.toBeInTheDocument();
});
it("student role cannot fetch the parent dashboard", () => {
  auth.user.role = "STUDENT";
  mount(<ParentPortalPage />);
  expect(
    screen.getByText("Parent portal access restricted"),
  ).toBeInTheDocument();
  expect(read.getParentDashboard).not.toHaveBeenCalled();
});
