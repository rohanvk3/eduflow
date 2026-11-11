import { beforeEach, expect, it, vi } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import NotificationBell from "../components/NotificationBell";
import NotificationDashboard from "../components/NotificationDashboard";
import { NotificationItem } from "../components/NotificationUI";
import NotificationsPage from "../pages/NotificationsPage";
import NotificationPreferencesPage from "../pages/NotificationPreferencesPage";
import AnnouncementsPage from "../pages/AnnouncementsPage";
import AnnouncementFormPage from "../pages/AnnouncementFormPage";
import AnnouncementDetailsPage from "../pages/AnnouncementDetailsPage";
const { notices, announcements, auth } = vi.hoisted(() => ({
  notices: Object.fromEntries(
    [
      "getNotifications",
      "getUnreadCount",
      "markNotificationRead",
      "markAllNotificationsRead",
      "dismissNotification",
      "getNotificationPreferences",
      "saveNotificationPreferences",
    ].map((k) => [k, vi.fn()]),
  ),
  announcements: Object.fromEntries(
    [
      "getAnnouncements",
      "getMyAnnouncements",
      "getAnnouncement",
      "saveAnnouncement",
      "publishAnnouncement",
      "archiveAnnouncement",
      "getDeliveryOverview",
    ].map((k) => [k, vi.fn()]),
  ),
  auth: {
    user: { id: 1, fullName: "QA Admin", email: "qa@test", role: "ADMIN" },
  },
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/notificationService", async () => ({
  ...(await vi.importActual("../services/notificationService")),
  ...notices,
  notificationError: async (e) =>
    e.response?.data?.message || e.message || "Request failed",
}));
vi.mock("../services/announcementService", () => announcements);
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange, required = true }) => (
    <label>
      {label}
      <input
        aria-label={label}
        required={required}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
const notification = {
  id: 1,
  type: "EXAM_SCHEDULED",
  title: "Exam scheduled",
  message: "A published exam is available.",
  actionUrl: "/exams/9",
  isRead: false,
  createdAt: new Date().toISOString(),
};
const announcement = {
  id: 9,
  title: "Class notice",
  content: "Read the class instructions.",
  audienceType: "BATCH",
  batchId: 3,
  batchName: "Evening",
  createdBy: "Teacher",
  status: "DRAFT",
  version: 0,
  canManage: true,
  createdAt: "2026-11-10T09:00:00Z",
  recipients: 3,
  delivered: 2,
  skipped: 0,
  deliveryComplete: false,
};
const preferences = {
  announcementsEnabled: true,
  examNotificationsEnabled: true,
  assignmentNotificationsEnabled: true,
  feeNotificationsEnabled: true,
  materialNotificationsEnabled: true,
  attendanceNotificationsEnabled: false,
};
const page = (content) => ({
  content,
  page: 0,
  size: 10,
  totalElements: content.length,
  totalPages: 1,
});
const changed = () =>
  window.dispatchEvent(new Event("eduflow:notifications-changed"));
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { id: 1, fullName: "QA Admin", email: "qa@test", role: "ADMIN" };
  notices.getNotifications.mockResolvedValue({ data: page([notification]) });
  notices.getUnreadCount.mockResolvedValue({ data: { count: 2 } });
  notices.markNotificationRead.mockImplementation(async () => {
    changed();
    return { data: { ...notification, isRead: true } };
  });
  notices.markAllNotificationsRead.mockImplementation(async () => {
    changed();
    return { data: { updated: 1 } };
  });
  notices.dismissNotification.mockImplementation(async () => {
    changed();
    return {};
  });
  notices.getNotificationPreferences.mockResolvedValue({ data: preferences });
  notices.saveNotificationPreferences.mockResolvedValue({ data: preferences });
  announcements.getAnnouncements.mockResolvedValue({
    data: page([announcement]),
  });
  announcements.getMyAnnouncements.mockResolvedValue({
    data: page([
      {
        ...announcement,
        status: "PUBLISHED",
        publishedAt: announcement.createdAt,
      },
    ]),
  });
  announcements.getAnnouncement.mockResolvedValue({ data: announcement });
  announcements.saveAnnouncement.mockResolvedValue({ data: announcement });
  announcements.publishAnnouncement.mockResolvedValue({
    data: { ...announcement, status: "PUBLISHED" },
  });
  announcements.archiveAnnouncement.mockResolvedValue({
    data: { ...announcement, status: "ARCHIVED" },
  });
  announcements.getDeliveryOverview.mockResolvedValue({
    data: {
      jobs: 4,
      pendingJobs: 2,
      recipients: 12,
      delivered: 10,
      skipped: 1,
      scheduledAnnouncements: 1,
    },
  });
  vi.spyOn(window, "confirm").mockReturnValue(true);
});
const mount = (component, path = "/notifications", route = "*") =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={route} element={component} />
        <Route path="/announcements/9" element={<p>Saved announcement</p>} />
      </Routes>
    </MemoryRouter>,
  );
it("bell displays the real count and recent notification dropdown", async () => {
  mount(<NotificationBell />);
  const button = await screen.findByRole("button", {
    name: "Open notifications, 2 unread",
  });
  await userEvent.click(button);
  expect(
    await screen.findByRole("dialog", { name: "Recent notifications" }),
  ).toBeInTheDocument();
  expect(screen.getByText("Exam scheduled")).toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: "View all notifications" }),
  ).toHaveAttribute("href", "/notifications");
});
it("bell hides a zero badge and shows an empty state", async () => {
  notices.getUnreadCount.mockResolvedValue({ data: { count: 0 } });
  notices.getNotifications.mockResolvedValue({ data: page([]) });
  mount(<NotificationBell />);
  await userEvent.click(
    await screen.findByRole("button", { name: "Open notifications, 0 unread" }),
  );
  expect(await screen.findByText("You're all caught up")).toBeInTheDocument();
  expect(screen.queryByText("0")).not.toBeInTheDocument();
});
it("bell refreshes unread count after marking a notification", async () => {
  mount(<NotificationBell />);
  await userEvent.click(
    await screen.findByRole("button", { name: "Open notifications, 2 unread" }),
  );
  notices.getUnreadCount.mockResolvedValue({ data: { count: 1 } });
  await userEvent.click(
    await screen.findByLabelText("Mark Exam scheduled as read"),
  );
  await screen.findByRole("button", { name: "Open notifications, 1 unread" });
  expect(notices.markNotificationRead).toHaveBeenCalledWith(1);
});
it("dropdown supports Escape and API failure retry", async () => {
  notices.getUnreadCount.mockRejectedValueOnce(
    new Error("Updates unavailable"),
  );
  mount(<NotificationBell />);
  await userEvent.click(
    screen.getByRole("button", { name: "Open notifications" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Updates unavailable",
  );
  await userEvent.click(screen.getByText("Retry"));
  expect(await screen.findByText("Exam scheduled")).toBeInTheDocument();
  fireEvent.keyDown(window, { key: "Escape" });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});
it("notification center sends type, read and search filters", async () => {
  mount(<NotificationsPage />);
  await screen.findByText("Exam scheduled");
  fireEvent.change(screen.getByLabelText("Read status"), {
    target: { value: "UNREAD" },
  });
  fireEvent.change(screen.getByLabelText("Notification type"), {
    target: { value: "EXAM_SCHEDULED" },
  });
  fireEvent.change(screen.getByLabelText("Search notifications"), {
    target: { value: "exam" },
  });
  await waitFor(() =>
    expect(notices.getNotifications).toHaveBeenLastCalledWith(
      expect.objectContaining({
        readStatus: "UNREAD",
        type: "EXAM_SCHEDULED",
        search: "exam",
      }),
      expect.anything(),
    ),
  );
});
it("notification center marks all and dismisses the user's copy", async () => {
  mount(<NotificationsPage />);
  await screen.findByText("Exam scheduled");
  await userEvent.click(screen.getByText("Mark all as read"));
  await waitFor(() =>
    expect(notices.markAllNotificationsRead).toHaveBeenCalled(),
  );
  await userEvent.click(await screen.findByLabelText("Dismiss Exam scheduled"));
  expect(notices.dismissNotification).toHaveBeenCalledWith(1);
});
it("unsafe external action URLs never become links", () => {
  mount(
    <NotificationItem
      notification={{ ...notification, actionUrl: "//evil.test" }}
    />,
  );
  expect(screen.queryByRole("link", { name: /Open/ })).not.toBeInTheDocument();
});
it("notification center paginates real API records", async () => {
  notices.getNotifications.mockResolvedValue({
    data: { ...page([notification]), totalElements: 21, totalPages: 3 },
  });
  mount(<NotificationsPage />);
  await screen.findByText("Exam scheduled");
  await userEvent.click(screen.getByLabelText("Next page"));
  await waitFor(() =>
    expect(notices.getNotifications).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1 }),
      expect.anything(),
    ),
  );
});
it("preference toggles save all categories through the own-user API", async () => {
  mount(<NotificationPreferencesPage />);
  const exams = await screen.findByRole("switch", { name: "Exams" });
  await userEvent.click(exams);
  await userEvent.click(screen.getByText("Save preferences"));
  await waitFor(() =>
    expect(notices.saveNotificationPreferences).toHaveBeenCalledWith({
      ...preferences,
      examNotificationsEnabled: false,
    }),
  );
  expect(
    await screen.findByText("Notification preferences saved"),
  ).toBeInTheDocument();
});
it("preference API errors preserve edits and display the problem", async () => {
  notices.saveNotificationPreferences.mockRejectedValue(
    new Error("Cannot save preferences"),
  );
  mount(<NotificationPreferencesPage />);
  await userEvent.click(
    await screen.findByRole("switch", { name: "Attendance" }),
  );
  await userEvent.click(screen.getByText("Save preferences"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Cannot save");
  expect(screen.getByRole("switch", { name: "Attendance" })).toBeChecked();
});
it("announcement directory has real search and audience filters", async () => {
  mount(<AnnouncementsPage />);
  await screen.findByRole("link", { name: "Class notice" });
  fireEvent.change(screen.getByLabelText("Search announcements"), {
    target: { value: "class" },
  });
  fireEvent.change(screen.getByLabelText("Audience filter"), {
    target: { value: "BATCH" },
  });
  await waitFor(() =>
    expect(announcements.getAnnouncements).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "class", audienceType: "BATCH" }),
      expect.anything(),
    ),
  );
});
it("students see announcement content without management controls", async () => {
  auth.user.role = "STUDENT";
  announcements.getAnnouncements.mockResolvedValue({
    data: page([{ ...announcement, canManage: false, status: "PUBLISHED" }]),
  });
  mount(<AnnouncementsPage />);
  await screen.findByText("Class notice");
  expect(screen.queryByText("Create announcement")).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Edit" })).not.toBeInTheDocument();
});
it("teacher announcement editor is restricted to batch audience", async () => {
  auth.user.role = "TEACHER";
  mount(<AnnouncementFormPage />, "/announcements/new");
  expect(await screen.findByLabelText("Audience *")).toBeDisabled();
  expect(screen.getByLabelText("Audience *")).toHaveValue("BATCH");
  expect(screen.getByLabelText("Target batch")).toBeInTheDocument();
});
it("parent cannot open announcement management editor", () => {
  auth.user.role = "PARENT";
  mount(<AnnouncementFormPage />);
  expect(
    screen.getByText(/Announcement management is restricted/),
  ).toBeInTheDocument();
  expect(announcements.getAnnouncement).not.toHaveBeenCalled();
});
it("draft preview renders scripts as text without HTML execution", async () => {
  mount(<AnnouncementFormPage />, "/announcements/new");
  fireEvent.change(await screen.findByLabelText("Title *"), {
    target: { value: "Preview" },
  });
  fireEvent.change(screen.getByLabelText("Content *"), {
    target: { value: "<script>alert(1)</script>" },
  });
  await userEvent.click(screen.getByText("Preview"));
  expect(
    within(screen.getByRole("dialog")).getByText("<script>alert(1)</script>"),
  ).toBeInTheDocument();
  expect(document.querySelector("script")).toBeNull();
});
it("editor saves and schedules using institute-local time", async () => {
  announcements.saveAnnouncement.mockResolvedValue({
    data: {
      ...announcement,
      audienceType: "ALL",
      publishAt: "2026-12-11T03:30:00Z",
    },
  });
  announcements.publishAnnouncement.mockResolvedValue({
    data: { ...announcement, status: "SCHEDULED" },
  });
  mount(<AnnouncementFormPage />, "/announcements/new");
  fireEvent.change(await screen.findByLabelText("Title *"), {
    target: { value: "Scheduled notice" },
  });
  fireEvent.change(screen.getByLabelText("Content *"), {
    target: { value: "Notice content" },
  });
  fireEvent.change(screen.getByLabelText("Publication mode"), {
    target: { value: "SCHEDULE" },
  });
  fireEvent.change(
    screen.getByLabelText("Scheduled date & time (Asia/Kolkata) *"),
    { target: { value: "2026-12-11T09:00" } },
  );
  await userEvent.click(screen.getByText("Save and schedule"));
  expect(await screen.findByText("Saved announcement")).toBeInTheDocument();
  expect(announcements.saveAnnouncement).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({
      audienceType: "ALL",
      publishAt: "2026-12-11T03:30:00.000Z",
    }),
  );
  expect(announcements.publishAnnouncement).toHaveBeenCalledWith(9, 0);
});
it("editor publishes only after explicit confirmation", async () => {
  window.confirm.mockReturnValue(false);
  mount(<AnnouncementFormPage />, "/announcements/new");
  fireEvent.change(await screen.findByLabelText("Title *"), {
    target: { value: "Notice" },
  });
  fireEvent.change(screen.getByLabelText("Content *"), {
    target: { value: "Body" },
  });
  await userEvent.click(screen.getByText("Save and publish"));
  expect(announcements.saveAnnouncement).not.toHaveBeenCalled();
});
it("published announcement details expose delivery status and archive action", async () => {
  announcements.getAnnouncement.mockResolvedValue({
    data: { ...announcement, status: "PUBLISHED" },
  });
  mount(<AnnouncementDetailsPage />, "/announcements/8", "/announcements/:id");
  expect(await screen.findByText("Delivery overview")).toBeInTheDocument();
  expect(screen.queryByText("Edit draft")).not.toBeInTheDocument();
  await userEvent.click(screen.getByText("Archive"));
  await waitFor(() =>
    expect(announcements.archiveAnnouncement).toHaveBeenCalledWith("8", 0),
  );
});
it("dashboard uses persisted notification and scheduled delivery data", async () => {
  mount(<NotificationDashboard />);
  expect(await screen.findByText(/4 jobs/)).toHaveTextContent("2 pending");
  expect(screen.getByText(/4 jobs/)).toHaveTextContent("10 delivered");
  expect(await screen.findByText("Exam scheduled")).toBeInTheDocument();
});
it("loads the teacher's own announcements within current batch scope", async () => {
  auth.user.role = "TEACHER";
  announcements.getAnnouncements.mockImplementation(async (params) => ({
    data: params.createdByMe
      ? page([
          { ...announcement, title: "My batch update", status: "PUBLISHED" },
        ])
      : page([]),
  }));
  mount(<NotificationDashboard />);
  expect(
    await screen.findByText("My batch update · published"),
  ).toBeInTheDocument();
  expect(announcements.getAnnouncements).toHaveBeenCalledWith(
    expect.objectContaining({ createdByMe: true }),
    expect.any(AbortSignal),
  );
});
it("announcement API errors provide retry without displaying unrelated content", async () => {
  announcements.getAnnouncement.mockRejectedValue({
    response: { data: { message: "Announcement access denied" } },
  });
  mount(<AnnouncementDetailsPage />, "/announcements/8", "/announcements/:id");
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Announcement access denied",
  );
  expect(
    screen.queryByText("Read the class instructions."),
  ).not.toBeInTheDocument();
});
