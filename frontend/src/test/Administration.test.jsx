import { beforeEach, it, expect, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import AdministrationPage, {
  AccountSettingsPage,
} from "../pages/AdministrationPage";
import AccountRecoveryPage from "../pages/AccountRecoveryPage";
const { service, auth } = vi.hoisted(() => ({
  service: Object.fromEntries(
    [
      "getAdmin",
      "saveInstitute",
      "uploadLogo",
      "changeUserStatus",
      "changeUserRole",
      "issueRecovery",
      "getProfile",
      "saveProfile",
      "changePassword",
      "requestRecovery",
      "confirmRecovery",
    ].map((k) => [k, vi.fn()]),
  ),
  auth: { user: { role: "ADMIN" }, logout: vi.fn(), refreshUser: vi.fn() },
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/adminService", () => service);
const account = {
  id: 2,
  fullName: "Account User",
  email: "account@test.invalid",
  role: "STUDENT",
  status: "ACTIVE",
  createdAt: "2026-11-10T00:00:00Z",
  resetRequestedAt: null,
};
const settings = {
  instituteName: "Live Institute",
  instituteCode: "LIVE",
  email: "office@test.invalid",
  timezone: "Asia/Kolkata",
  currencyCode: "INR",
  version: 0,
  hasLogo: false,
};
const page = (content) => ({
  data: { content, totalPages: 2, totalElements: 21 },
});
beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  auth.user = { role: "ADMIN" };
  service.getAdmin.mockImplementation(async (path) =>
    path === "institute-settings"
      ? { data: settings }
      : path === "control-center"
        ? {
            data: {
              institute: settings,
              counts: { activeUsers: 7 },
              security: ["Bearer JWT checks"],
              integrityWarnings: [],
              recentAuditEvents: [],
            },
          }
        : page([account]),
  );
  service.getProfile.mockResolvedValue({ data: account });
  service.saveInstitute.mockResolvedValue({ data: settings });
  service.saveProfile.mockResolvedValue({ data: account });
  service.changePassword.mockResolvedValue({});
  service.changeUserStatus.mockResolvedValue({});
  service.changeUserRole.mockResolvedValue({});
  service.issueRecovery.mockResolvedValue({
    data: {
      resetToken: "single-use-example",
      expiresAt: "2026-11-10T01:00:00Z",
      instruction: "No email was sent.",
    },
  });
  service.requestRecovery.mockResolvedValue({
    data: { message: "Contact institute administration. No email was sent." },
  });
});
const mount = (e) => render(<MemoryRouter>{e}</MemoryRouter>);
it("control center renders real supplied counts and safe observations", async () => {
  mount(<AdministrationPage />);
  expect(await screen.findByText("Live Institute")).toBeInTheDocument();
  expect(screen.getByText("7")).toBeInTheDocument();
  expect(
    screen.getByText(/not a full database consistency scan/),
  ).toBeInTheDocument();
});
it("non-admin never requests administration data", () => {
  auth.user.role = "STUDENT";
  mount(<AdministrationPage kind="users" />);
  expect(screen.getByText("Administrator access required")).toBeInTheDocument();
  expect(service.getAdmin).not.toHaveBeenCalled();
});
it("institute settings save through the real service contract", async () => {
  mount(<AdministrationPage kind="institute-settings" />);
  const input = await screen.findByLabelText("Institute Name *");
  await userEvent.clear(input);
  await userEvent.type(input, "New Institute");
  await userEvent.click(screen.getByText("Save settings"));
  await waitFor(() =>
    expect(service.saveInstitute).toHaveBeenCalledWith(
      expect.objectContaining({ instituteName: "New Institute", version: 0 }),
    ),
  );
  expect(
    await screen.findByText("Institute settings saved"),
  ).toBeInTheDocument();
});
it("user directory filters and pages on the server", async () => {
  mount(<AdministrationPage kind="users" />);
  await screen.findByText("Account User");
  await userEvent.selectOptions(screen.getByLabelText("Role"), "TEACHER");
  await waitFor(() =>
    expect(service.getAdmin).toHaveBeenLastCalledWith(
      "users",
      expect.objectContaining({ role: "TEACHER" }),
      expect.any(AbortSignal),
    ),
  );
  await userEvent.click(screen.getByText("Next"));
  await waitFor(() =>
    expect(service.getAdmin).toHaveBeenLastCalledWith(
      "users",
      expect.objectContaining({ page: 1 }),
      expect.any(AbortSignal),
    ),
  );
});
it("status changes require confirmation and render backend conflicts", async () => {
  service.changeUserStatus.mockRejectedValue({
    response: {
      data: { message: "Last active administrator must be preserved" },
    },
  });
  mount(<AdministrationPage kind="users" />);
  await screen.findByText("Account User");
  await userEvent.click(screen.getByText("View"));
  await userEvent.selectOptions(
    within(screen.getByRole("dialog")).getByLabelText("Account status"),
    "INACTIVE",
  );
  await userEvent.click(screen.getByText("Apply status"));
  expect(window.confirm).toHaveBeenCalled();
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Last active administrator",
  );
});
it("authorized recovery shows only an ephemeral fragment link", async () => {
  mount(<AdministrationPage kind="users" />);
  await screen.findByText("Account User");
  await userEvent.click(screen.getByText("View"));
  await userEvent.click(screen.getByText("Issue recovery link"));
  expect(await screen.findByLabelText("Single-use recovery link")).toHaveValue(
    location.origin + "/recover-account#single-use-example",
  );
  expect(screen.getByText("No email was sent.")).toBeInTheDocument();
});
it("audit viewer opens safe event details", async () => {
  service.getAdmin.mockResolvedValue(
    page([
      {
        id: 9,
        actorUserId: 1,
        action: "USER_STATUS_CHANGED",
        entityType: "USER",
        description: "Account status changed",
        outcome: "SUCCESS",
        timestamp: "2026-11-10T00:00:00Z",
        requestId: "request-test",
      },
    ]),
  );
  mount(<AdministrationPage kind="audit-logs" />);
  await screen.findByText("USER_STATUS_CHANGED");
  await userEvent.click(screen.getByText("View"));
  expect(await screen.findByText("Account status changed")).toBeInTheDocument();
  expect(screen.getByText("request-test")).toBeInTheDocument();
});
it("profile changes refresh the existing auth context", async () => {
  mount(<AccountSettingsPage />);
  const input = await screen.findByLabelText("Full name");
  await userEvent.clear(input);
  await userEvent.type(input, "Updated Name");
  await userEvent.click(screen.getByText("Save profile"));
  expect(service.saveProfile).toHaveBeenCalledWith("Updated Name");
  await waitFor(() => expect(auth.refreshUser).toHaveBeenCalled());
});
it("password change signs out after backend confirmation", async () => {
  mount(<AccountSettingsPage security />);
  await userEvent.type(
    await screen.findByLabelText("Current password"),
    "CurrentPass123!",
  );
  await userEvent.type(
    screen.getByLabelText("New password"),
    "ChangedPass123!",
  );
  await userEvent.click(screen.getByText("Change password and sign out"));
  await waitFor(() =>
    expect(auth.logout).toHaveBeenCalledWith(
      expect.stringContaining("Password changed"),
    ),
  );
});
it("password errors preserve the form and authenticated session", async () => {
  service.changePassword.mockRejectedValue({
    response: { data: { message: "Current password is incorrect" } },
  });
  mount(<AccountSettingsPage security />);
  await userEvent.type(
    await screen.findByLabelText("Current password"),
    "IncorrectPass123!",
  );
  await userEvent.type(
    screen.getByLabelText("New password"),
    "ChangedPass123!",
  );
  await userEvent.click(screen.getByText("Change password and sign out"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Current password is incorrect",
  );
  expect(auth.logout).not.toHaveBeenCalled();
});
it("recovery requests never claim email delivery", async () => {
  mount(<AccountRecoveryPage />);
  await userEvent.type(
    screen.getByLabelText("Account email"),
    "account@test.invalid",
  );
  await userEvent.click(screen.getByText("Request verified assistance"));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "No email was sent",
  );
});
it("reset fragment is removed from the browser URL", async () => {
  history.replaceState(null, "", "/recover-account#one-use");
  service.confirmRecovery.mockResolvedValue({});
  mount(<AccountRecoveryPage />);
  expect(location.hash).toBe("");
  await userEvent.type(
    screen.getByLabelText("New password"),
    "RecoveredPass123!",
  );
  await userEvent.click(screen.getByText("Reset password"));
  expect(service.confirmRecovery).toHaveBeenCalledWith({
    token: "one-use",
    newPassword: "RecoveredPass123!",
  });
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Password reset completed",
  );
  history.replaceState(null, "", "/");
});
