import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Select,
  EmptyState,
  ErrorNotice,
} from "../components/UI";
import {
  useFinanceLoad,
  FinanceError,
  inr,
  Field,
} from "../components/FinanceUI";
import RecordSelect from "../components/RecordSelect";
import { getAnalytics, getOwnProgress } from "../services/analyticsService";
import { getReport, downloadReport } from "../services/reportService";
import { notificationError } from "../services/notificationService";
const sections = {
  finance: [
    "revenue-trend",
    "course-wise",
    "payment-methods",
    "batch-wise",
    "outstanding",
    "payments",
  ],
  attendance: ["trend", "batch-wise", "student-wise"],
  academics: [
    "exam-trend",
    "course-wise",
    "batch-wise",
    "assignment-performance",
  ],
  batches: ["batches"],
  teachers: ["teachers"],
};
const types = [
  "REVENUE",
  "OUTSTANDING_FEES",
  "PAYMENT_COLLECTIONS",
  "ATTENDANCE",
  "STUDENT_PERFORMANCE",
  "EXAM_RESULTS",
  "ASSIGNMENT_COMPLETION",
  "BATCH_PERFORMANCE",
  "TEACHER_WORKLOAD",
];
const label = (s) =>
  s.replaceAll(/[_-]/g, " ").replace(/([a-z])([A-Z])/g, "$1 $2");
export function AnalyticsChart({ rows, title }) {
  const points = rows
    .map((r) => ({ ...r, value: r.value ?? r.weeklyScheduleSlots }))
    .filter((r) => r.value != null);
  const temporal = /trend/i.test(title),
    Chart = temporal ? LineChart : BarChart;
  return (
    <Card className="p-5">
      <h2 className="mb-5 font-semibold">{title}</h2>
      {!points.length ? (
        <EmptyState compact title="No chart data in this period" />
      ) : (
        <div className="h-72" role="img" aria-label={title}>
          <ResponsiveContainer width="100%" height="100%">
            <Chart data={points}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} />
              <YAxis />
              <Tooltip />
              {temporal ? (
                <Line dataKey="value" stroke="#4F46E5" connectNulls={false} />
              ) : (
                <Bar dataKey="value" fill="#4F46E5" radius={[4, 4, 0, 0]} />
              )}
            </Chart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}
function Metrics({ metrics }) {
  return (
    <div className="mb-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Object.entries(metrics || {}).map(([k, v]) => (
        <Card className="p-5" key={k}>
          <p className="text-xs capitalize text-slate-500">{label(k)}</p>
          <p className="mt-2 text-2xl font-semibold">
            {v == null
              ? "No data"
              : /collections|outstanding|overdue|agreedFees/i.test(k)
                ? inr(v)
                : /percentage|average|completion|rate/i.test(k)
                  ? Number(v).toFixed(2) + "%"
                  : String(v)}
          </p>
        </Card>
      ))}
    </div>
  );
}
export function AnalyticsTable({ rows, kind }) {
  if (!rows?.length)
    return <EmptyState title="No records match these filters" />;
  const keys = Object.keys(rows[0]);
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr>
            {keys.map((k) => (
              <th
                className="whitespace-nowrap border-b p-3 text-xs capitalize text-slate-500"
                key={k}
              >
                {label(k)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr className="border-b border-slate-100" key={r.id || i}>
              {keys.map((k) => (
                <td className="whitespace-nowrap p-3" key={k}>
                  {k === "label" &&
                  r.id &&
                  ["batches", "teachers"].includes(kind) ? (
                    <Link
                      className="text-indigo-600"
                      to={"/analytics/" + kind + "/" + r.id}
                    >
                      {r[k]}
                    </Link>
                  ) : r[k] == null ? (
                    "—"
                  ) : typeof r[k] === "boolean" ? (
                    r[k] ? (
                      "Yes"
                    ) : (
                      "No"
                    )
                  ) : (
                    String(r[k])
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export default function AnalyticsPage({ kind = "overview" }) {
  const { user } = useAuth(),
    { id } = useParams(),
    admin = user.role === "ADMIN",
    staff = ["ADMIN", "TEACHER"].includes(user.role),
    [filters, setFilters] = useState({}),
    [revision, setRevision] = useState(0),
    [section, setSection] = useState((sections[kind] || [])[0] || ""),
    [page, setPage] = useState(0),
    [reportType, setReportType] = useState(admin ? "REVENUE" : "ATTENDANCE"),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const allowed =
    kind === "progress"
      ? user.role === "STUDENT"
      : staff && (kind !== "finance" || admin);
  const params = {
    ...filters,
    page,
    size: 10,
    ...(id ? { [kind === "batches" ? "batchId" : "teacherId"]: id } : {}),
  };
  const state = useFinanceLoad(
    async (signal) => {
      if (kind === "progress") return getOwnProgress(filters, signal);
      if (kind === "reports")
        return getReport({ ...params, reportType }, signal);
      if (kind === "overview") {
        const paths = admin
          ? [
              "overview",
              "finance/revenue-trend",
              "attendance/trend",
              "academics/exam-trend",
              "finance/payment-methods",
              "batches",
            ]
          : [
              "attendance/summary",
              "attendance/trend",
              "academics/exam-trend",
              "teachers",
            ];
        const results = await Promise.all(
          paths.map((p) => getAnalytics(p, params, signal)),
        );
        return {
          data: {
            ...results[0].data,
            panels: results
              .slice(1)
              .map((r, i) => ({ ...r.data, title: paths[i + 1] })),
          },
        };
      }
      return getAnalytics(
        kind + (section === kind ? "" : "/" + section),
        params,
        signal,
      );
    },
    [JSON.stringify(filters), revision, section, page, reportType, id, kind],
    allowed,
  );
  const edit = (k, v) => {
    setFilters((f) => ({
      ...f,
      [k]: v || undefined,
      ...(k === "courseId" ? { batchId: undefined } : {}),
    }));
    setPage(0);
  };
  const exportFile = async (format) => {
    setBusy(true);
    setError("");
    try {
      await downloadReport(format, { ...filters, reportType });
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  if (!allowed)
    return (
      <EmptyState title="This analytics view is not available for your role" />
    );
  const title =
    kind === "overview"
      ? "Advanced Analytics"
      : kind === "reports"
        ? "Reports Center"
        : kind === "progress"
          ? "My Progress"
          : label(kind) + " Analytics";
  const financeReport =
    kind === "reports" &&
    ["REVENUE", "OUTSTANDING_FEES", "PAYMENT_COLLECTIONS"].includes(reportType);
  return (
    <>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="eyebrow">Institute insights</p>
          <h1 className="page-title capitalize">{title}</h1>
          <p className="mt-2 text-sm text-slate-500">
            Monitor institute performance and make data-driven decisions.
          </p>
        </div>
        <Button onClick={() => setRevision((x) => x + 1)}>
          {kind === "reports" ? "Generate report" : "Refresh"}
        </Button>
      </div>
      {staff && (
        <nav className="mb-5 flex flex-wrap gap-3 text-sm">
          {[
            "overview",
            ...(admin ? ["finance"] : []),
            "attendance",
            "academics",
            "batches",
            "teachers",
            "reports",
          ].map((k) => (
            <Link
              className="rounded-lg border border-slate-200 px-3 py-2 capitalize hover:bg-indigo-50"
              key={k}
              to={
                k === "reports"
                  ? "/reports"
                  : "/analytics" + (k === "overview" ? "" : "/" + k)
              }
            >
              {label(k)}
            </Link>
          ))}
        </nav>
      )}
      <Card className="mb-5 grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-4">
        <Field label="Start date">
          <Input
            type="date"
            value={filters.startDate || ""}
            onChange={(e) => edit("startDate", e.target.value)}
          />
        </Field>
        <Field label="End date">
          <Input
            type="date"
            value={filters.endDate || ""}
            onChange={(e) => edit("endDate", e.target.value)}
          />
        </Field>
        {staff && (
          <>
            <RecordSelect
              endpoint="/courses"
              label="Course"
              required={false}
              value={filters.courseId}
              onChange={(v) => edit("courseId", v)}
            />
            <RecordSelect
              endpoint="/batches"
              label="Batch"
              required={false}
              value={filters.batchId}
              onChange={(v) => edit("batchId", v)}
            />
          </>
        )}
        {kind === "reports" && (
          <Field label="Report type">
            <Select
              value={reportType}
              onChange={(e) => {
                setReportType(e.target.value);
                setFilters((f) => ({
                  startDate: f.startDate,
                  endDate: f.endDate,
                  courseId: f.courseId,
                  batchId: f.batchId,
                }));
                setPage(0);
              }}
            >
              {types
                .filter(
                  (t) =>
                    admin ||
                    ![
                      "REVENUE",
                      "OUTSTANDING_FEES",
                      "PAYMENT_COLLECTIONS",
                    ].includes(t),
                )
                .map((t) => (
                  <option key={t}>{t}</option>
                ))}
            </Select>
          </Field>
        )}
        {kind === "reports" && reportType === "OUTSTANDING_FEES" && (
          <Field label="Payment status">
            <Select
              value={filters.paymentStatus || ""}
              onChange={(e) => edit("paymentStatus", e.target.value)}
            >
              <option value="">All accounts</option>
              {["PAID", "OUTSTANDING", "OVERDUE"].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
          </Field>
        )}
        {(kind === "academics" ||
          (kind === "reports" &&
            ["EXAM_RESULTS", "STUDENT_PERFORMANCE"].includes(reportType))) && (
          <Field label="Exam type">
            <Select
              value={filters.examType || ""}
              onChange={(e) => edit("examType", e.target.value)}
            >
              <option value="">All exams</option>
              <option>ONLINE_MCQ</option>
              <option>OFFLINE</option>
            </Select>
          </Field>
        )}
      </Card>
      {admin &&
        kind === "reports" &&
        reportType !== "TEACHER_WORKLOAD" &&
        reportType !== "BATCH_PERFORMANCE" && (
          <Card className="mb-5 p-5">
            <RecordSelect
              endpoint="/students"
              label="Student"
              required={false}
              value={filters.studentId}
              onChange={(v) => edit("studentId", v)}
            />
          </Card>
        )}
      {admin && kind === "reports" && reportType === "TEACHER_WORKLOAD" && (
        <Card className="mb-5 p-5">
          <RecordSelect
            endpoint="/teachers"
            label="Teacher"
            required={false}
            value={filters.teacherId}
            onChange={(v) => edit("teacherId", v)}
          />
        </Card>
      )}
      {kind === "attendance" && section === "student-wise" && (
        <Field label="Low-attendance threshold (%)">
          <Input
            type="number"
            min="0"
            max="100"
            value={filters.threshold ?? 75}
            onChange={(e) => edit("threshold", e.target.value)}
          />
        </Field>
      )}
      {kind === "batches" && !id && (
        <Field label="Batch order">
          <Select
            value={filters.sort || "name,asc"}
            onChange={(e) => edit("sort", e.target.value)}
          >
            <option value="name,asc">Name A–Z</option>
            <option value="name,desc">Name Z–A</option>
          </Select>
        </Field>
      )}
      <p className="mb-5 text-xs leading-6 text-slate-500">
        Dates are inclusive in Asia/Kolkata; default: last 30 days, maximum: 366
        days. Collections use payment dates. Outstanding and workload assignment
        counts are current/lifetime values. No data means no eligible
        observations.
      </p>
      {sections[kind]?.length > 1 && (
        <Field label="Analysis">
          <Select
            value={section}
            onChange={(e) => {
              setSection(e.target.value);
              setPage(0);
            }}
          >
            {sections[kind].map((s) => (
              <option value={s} key={s}>
                {label(s)}
              </option>
            ))}
          </Select>
        </Field>
      )}
      {kind === "reports" && (
        <div className="my-5 flex gap-3">
          <Button
            disabled={busy || state.loading || !!state.error}
            onClick={() => exportFile("csv")}
          >
            Export CSV
          </Button>
          <Button
            disabled={busy || state.loading || !!state.error}
            variant="secondary"
            onClick={() => exportFile("pdf")}
          >
            Export PDF
          </Button>
        </div>
      )}
      {error && <ErrorNotice message={error} />}
      <FinanceError state={state} />
      {state.loading && (
        <Card className="animate-pulse p-12">Loading analytics…</Card>
      )}
      {state.data && kind !== "progress" && (
        <>
          <Metrics
            metrics={
              id && state.data.rows[0]
                ? Object.fromEntries(
                    Object.entries(state.data.rows[0]).filter(
                      ([k, v]) =>
                        !["id", "label", "course", "teachers"].includes(k) &&
                        typeof v !== "object",
                    ),
                  )
                : state.data.metrics
            }
          />
          {state.data.panels ? (
            <div className="grid gap-5 xl:grid-cols-2">
              {state.data.panels.map((p) => (
                <AnalyticsChart
                  key={p.title}
                  title={label(p.title)}
                  rows={p.rows.map((r) => ({
                    ...r,
                    value: r.value ?? r.attendance ?? r.examAverage,
                  }))}
                />
              ))}
            </div>
          ) : (
            <>
              <AnalyticsChart
                title={state.data.title}
                rows={state.data.rows.map((r) => ({
                  ...r,
                  value: r.value ?? r.attendance ?? r.weeklyScheduleSlots,
                }))}
              />
              {kind === "batches" && (
                <div className="mt-5 grid gap-5 lg:grid-cols-2">
                  <AnalyticsChart
                    title="Published exam performance (%)"
                    rows={state.data.rows.map((r) => ({
                      ...r,
                      value: r.examAverage,
                    }))}
                  />
                  <AnalyticsChart
                    title="Assignment completion (%)"
                    rows={state.data.rows.map((r) => ({
                      ...r,
                      value: r.assignmentCompletion,
                    }))}
                  />
                </div>
              )}
              <Card className="mt-5 p-4">
                <AnalyticsTable rows={state.data.rows} kind={kind} />
              </Card>
            </>
          )}
          <div className="my-5 flex items-center gap-4">
            <Button
              variant="secondary"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </Button>
            <span className="text-sm">Page {page + 1}</span>
            <Button
              variant="secondary"
              disabled={!state.data.hasMore}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </>
      )}
      {state.data &&
        kind === "progress" &&
        Object.entries(state.data).map(([k, d]) => (
          <div className="mb-5" key={k}>
            <h2 className="mb-4 font-semibold capitalize">{k}</h2>
            <Metrics metrics={d.metrics} />
            <AnalyticsChart title={label(k) + " trend"} rows={d.rows} />
            <Card className="mt-4 p-4">
              <AnalyticsTable rows={d.rows} />
            </Card>
          </div>
        ))}
    </>
  );
}
