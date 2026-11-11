import { beforeEach, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import AnalyticsPage from "../pages/AnalyticsPage";
const { api, auth, reports } = vi.hoisted(() => ({
  api: { getAnalytics: vi.fn(), getOwnProgress: vi.fn() },
  auth: { user: { role: "ADMIN" } },
  reports: { getReport: vi.fn(), downloadReport: vi.fn() },
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/analyticsService", () => api);
vi.mock("../services/reportService", () => reports);
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, onChange }) => (
    <label>
      {label}
      <input aria-label={label} onChange={(e) => onChange(e.target.value)} />
    </label>
  ),
}));
vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  BarChart: () => null,
  LineChart: () => null,
  Bar: () => null,
  Line: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  CartesianGrid: () => null,
}));
const data = {
  title: "Real scoped analytics",
  metrics: { collections: "40.00", outstanding: "960.00", examAverage: null },
  rows: [{ id: 1, label: "Physics", value: 80 }],
  page: 0,
  size: 10,
  hasMore: false,
};
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { role: "ADMIN" };
  api.getAnalytics.mockResolvedValue({ data });
  reports.getReport.mockResolvedValue({ data });
  reports.downloadReport.mockResolvedValue({});
});
const mount = (kind) =>
  render(
    <MemoryRouter>
      <AnalyticsPage kind={kind} />
    </MemoryRouter>,
  );
it("loads backend KPIs and explicit no-data values", async () => {
  mount("finance");
  expect(await screen.findByText("₹40.00")).toBeInTheDocument();
  expect(screen.getByText("No data")).toBeInTheDocument();
});
it("updates date filters on the API", async () => {
  mount("attendance");
  await screen.findByText("Real scoped analytics");
  await userEvent.type(screen.getByLabelText("Start date"), "2026-11-01");
  await waitFor(() =>
    expect(api.getAnalytics).toHaveBeenLastCalledWith(
      "attendance/trend",
      expect.objectContaining({ startDate: "2026-11-01" }),
      expect.any(AbortSignal),
    ),
  );
});
it("teacher has no financial UI or API access", async () => {
  auth.user.role = "TEACHER";
  mount("finance");
  expect(screen.getByText(/not available for your role/)).toBeInTheDocument();
  expect(api.getAnalytics).not.toHaveBeenCalled();
});
it("reports use authenticated download services", async () => {
  mount("reports");
  await screen.findByText("Real scoped analytics");
  await userEvent.click(screen.getByText("Export CSV"));
  expect(reports.downloadReport).toHaveBeenCalledWith(
    "csv",
    expect.objectContaining({ reportType: "REVENUE" }),
  );
  await userEvent.click(screen.getByText("Export PDF"));
  expect(reports.downloadReport).toHaveBeenCalledWith(
    "pdf",
    expect.any(Object),
  );
});
it("shows export errors", async () => {
  reports.downloadReport.mockRejectedValue({
    response: { data: { message: "Export exceeds safe limit" } },
  });
  mount("reports");
  await screen.findByText("Real scoped analytics");
  await userEvent.click(screen.getByText("Export CSV"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Export exceeds safe limit",
  );
});
it.each(["attendance", "academics", "batches", "teachers"])(
  "loads %s from the shared API",
  async (kind) => {
    mount(kind);
    expect(await screen.findByText("Physics")).toBeInTheDocument();
    expect(api.getAnalytics).toHaveBeenCalled();
  },
);
it("student progress resolves the signed-in profile", async () => {
  auth.user.role = "STUDENT";
  api.getOwnProgress.mockResolvedValue({ data: { academics: data } });
  mount("progress");
  expect(await screen.findByText("Physics")).toBeInTheDocument();
  expect(api.getOwnProgress).toHaveBeenCalled();
});
