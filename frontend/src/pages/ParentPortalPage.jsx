import { lazy, Suspense, useEffect, useState } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Avatar,
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  Select,
  Table,
  Pagination,
  Skeleton,
  ErrorNotice,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ParentHeading,
  ParentDenied,
  ParentMetrics,
  ParentDates,
  ParentScope,
  ParentError,
  useParentLoad,
  parentTime,
  pct,
  inr,
  childKey,
  parentTabs,
} from "../components/ParentUI";
import {
  getParentDashboard,
  getChildAttendance,
  getChildFees,
  getChildResults,
  getChildAssignments,
  getChildResources,
  getChildProgress,
  downloadChildReceipt,
} from "../services/parentDashboardService";
import { parentError, saveDownload } from "../services/parentService";
import ParentAttendanceCalendar from "../components/ParentAttendanceCalendar";
const ParentCharts = lazy(() => import("../components/ParentCharts"));
const NotificationDashboard = lazy(
  () => import("../components/NotificationDashboard"),
);
const titles = {
  dashboard: "Parent Dashboard",
  overview: "Child Overview",
  attendance: "Attendance",
  fees: "Fees",
  results: "Exam Results",
  assignments: "Assignments",
  progress: "Academic Progress",
  resources: "Learning Resources",
};
export default function ParentPortalPage({ mode = "dashboard" }) {
  const { user } = useAuth(),
    allowed = user.role === "PARENT",
    { studentId } = useParams(),
    [query] = useSearchParams(),
    nav = useNavigate();
  const [fallback, setFallback] = useState(() =>
      sessionStorage.getItem(childKey(user)),
    ),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [revision, setRevision] = useState(0);
  const selected = studentId || query.get("studentId") || fallback;
  const shell = useParentLoad(
    (signal) =>
      getParentDashboard(
        {
          studentId: selected || undefined,
          fromDate: from || undefined,
          toDate: to || undefined,
        },
        signal,
      ),
    [selected, from, to, revision],
    allowed,
  );
  useEffect(() => {
    if (!allowed) return;
    const refresh = () => setRevision((v) => v + 1);
    const timer = setInterval(refresh, 30000);
    window.addEventListener("focus", refresh);
    window.addEventListener("eduflow:parents-changed", refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener("focus", refresh);
      window.removeEventListener("eduflow:parents-changed", refresh);
    };
  }, [allowed]);
  useEffect(() => {
    if (shell.data?.linked)
      sessionStorage.setItem(childKey(user), String(shell.data.child.id));
  }, [shell.data, user]);
  if (!allowed) return <ParentDenied />;
  const child = shell.data?.child;
  const choose = (id) => {
    if (!id) return;
    sessionStorage.setItem(childKey(user), String(id));
    setFallback(id);
    nav(
      mode === "dashboard"
        ? "/parent/dashboard?studentId=" + id
        : "/parent/children/" + id + (mode === "overview" ? "" : "/" + mode),
    );
  };
  return (
    <>
      <ParentHeading
        title={titles[mode]}
        subtitle={
          mode === "dashboard"
            ? "Hello, " +
              user.fullName +
              ". Follow your child's learning journey."
            : "Read-only access to your verified child's institute records."
        }
      >
        <Link className="button-secondary" to="/parent/children">
          My Children
        </Link>
      </ParentHeading>
      {mode === "dashboard" && (
        <Suspense fallback={<Skeleton className="mb-5 h-24" />}>
          <NotificationDashboard />
        </Suspense>
      )}
      <Card className="mb-5 grid items-start gap-4 p-5 md:grid-cols-3">
        <RecordSelect
          endpoint="/parents/me/children"
          label="Select child"
          status=""
          value={child?.id || selected || ""}
          selectedLabel={
            child
              ? child.fullName + " · " + child.admissionNumber
              : "Selected child"
          }
          required={false}
          onChange={choose}
        />
        {!["fees", "resources"].includes(mode) && (
          <ParentDates from={from} to={to} onFrom={setFrom} onTo={setTo} />
        )}
      </Card>
      <ParentError state={shell} />
      {shell.error && (
        <Button
          variant="secondary"
          className="mb-5"
          onClick={() => {
            sessionStorage.removeItem(childKey(user));
            setFallback(null);
            nav("/parent/dashboard");
          }}
        >
          Reset child selection
        </Button>
      )}
      {shell.loading ? (
        <Skeleton className="h-64" />
      ) : !shell.error && !shell.data?.linked ? (
        <Card>
          <EmptyState
            title="No verified children linked"
            description="Contact institute administration to create and verify your parent–student relationship. Pending links do not grant access."
          />
        </Card>
      ) : (
        child && (
          <>
            <Card className="mb-5 p-5">
              <div className="flex items-center gap-4">
                <Avatar name={child.fullName} />
                <div>
                  <h2 className="font-semibold">{child.fullName}</h2>
                  <p className="mt-1 text-xs text-slate-500">
                    {child.admissionNumber} · {child.relationshipType} ·{" "}
                    {child.status}
                  </p>
                </div>
              </div>
              <nav
                aria-label="Child sections"
                className="mt-5 flex flex-wrap gap-2"
              >
                {parentTabs.map(([label, path]) => (
                  <Link
                    key={path}
                    className={
                      "button-" +
                      (mode === path || (mode === "overview" && path === "")
                        ? "primary"
                        : "secondary")
                    }
                    to={
                      "/parent/children/" + child.id + (path ? "/" + path : "")
                    }
                  >
                    {label}
                  </Link>
                ))}
              </nav>
            </Card>
            {["dashboard", "overview"].includes(mode) ? (
              <Dashboard data={shell.data} />
            ) : (
              <ChildSection
                key={child.id + "-" + mode}
                mode={mode}
                child={child}
                from={from}
                to={to}
                revision={revision}
                recentResults={shell.data.recentResults}
              />
            )}
          </>
        )
      )}
    </>
  );
}
function Dashboard({ data: d }) {
  const installments = d.feeAccounts
    .flatMap((a) =>
      a.installments
        .filter((i) => Number(i.outstandingAmount) > 0)
        .map((i) => ({ ...i, course: a.courseName, batch: a.batchName })),
    )
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
    .slice(0, 8);
  return (
    <>
      <ParentMetrics
        items={[
          [
            "Attendance · selected range",
            pct(d.attendance.attendancePercentage),
          ],
          ["Outstanding fees · lifetime", inr(d.fees.outstanding)],
          ["Upcoming / available exams", d.upcomingExamsCount],
          ["Pending assignments", d.pendingAssignments],
        ]}
      />
      <div className="mb-5 grid gap-5 lg:grid-cols-2">
        <Card className="p-5">
          <h2 className="mb-4 text-sm font-semibold">
            Enrolled courses & batches
          </h2>
          {d.child.enrollments.length ? (
            d.child.enrollments.map((e) => (
              <div
                key={e.id}
                className="mb-4 flex flex-wrap justify-between gap-2 text-xs"
              >
                <div>
                  <p className="font-medium">{e.courseName}</p>
                  <p className="mt-1 text-slate-500">
                    {e.batchName}
                    {e.batchStatus ? " · Batch " + e.batchStatus : ""} ·
                    Enrolled {e.enrollmentDate}
                  </p>
                </div>
                <Badge tone={e.active ? "success" : "neutral"}>
                  {e.status}
                </Badge>
              </div>
            ))
          ) : (
            <EmptyState compact title="No enrollments yet" />
          )}
        </Card>
        <Card className="p-5">
          <h2 className="mb-4 text-sm font-semibold">Upcoming exams</h2>
          {d.upcomingExams.length ? (
            d.upcomingExams.map((e) => (
              <div key={e.id} className="mb-4 text-xs">
                <p className="font-medium">{e.title}</p>
                <p className="mt-1 text-slate-500">
                  {e.batchName} · {parentTime(e.startDateTime)}
                </p>
              </div>
            ))
          ) : (
            <EmptyState compact title="No upcoming published exams" />
          )}
        </Card>
      </div>
      <Suspense fallback={<Skeleton className="mb-5 h-64" />}>
        <ParentCharts progress={d.progress} />
      </Suspense>
      <div className="mb-5 grid gap-5 lg:grid-cols-2">
        <Card className="p-5">
          <h2 className="mb-4 text-sm font-semibold">Homework deadlines</h2>
          {d.upcomingAssignments.length ? (
            d.upcomingAssignments.map((a) => (
              <div key={a.id} className="mb-4 text-xs">
                <p className="font-medium">{a.title}</p>
                <p className="mt-1 text-slate-500">
                  {a.batchName} · {parentTime(a.dueDateTime)}
                </p>
              </div>
            ))
          ) : (
            <EmptyState compact title="No pending homework" />
          )}
        </Card>
        <Card className="p-5">
          <h2 className="mb-4 text-sm font-semibold">
            Installments needing attention
          </h2>
          <p className="mb-4 text-xs text-slate-500">
            From the five most recent fee accounts. Open Fees for the full
            account directory.
          </p>
          {installments.length ? (
            installments.map((i) => (
              <div
                key={i.id}
                className="mb-4 flex justify-between gap-3 text-xs"
              >
                <div>
                  <p>
                    {i.course} · {i.batch}
                  </p>
                  <p className="mt-1 text-slate-500">
                    Due {i.dueDate} · {i.status}
                  </p>
                </div>
                <strong>{inr(i.outstandingAmount)}</strong>
              </div>
            ))
          ) : (
            <EmptyState
              compact
              title="No unpaid installments in recent accounts"
            />
          )}
        </Card>
      </div>
      <Card className="mb-5 p-5">
        <h2 className="mb-4 text-sm font-semibold">Recent published results</h2>
        <ResultTable rows={d.recentResults} />
      </Card>
      <Card className="p-5">
        <h2 className="mb-4 text-sm font-semibold">Recent academic activity</h2>
        {d.activity.length ? (
          d.activity.map((a, i) => (
            <div
              key={i}
              className="mb-4 flex flex-wrap justify-between gap-2 text-xs"
            >
              <span>{a.title}</span>
              <span className="text-slate-500">
                {a.type === "ATTENDANCE"
                  ? new Intl.DateTimeFormat("en-IN", {
                      dateStyle: "medium",
                      timeZone: "Asia/Kolkata",
                    }).format(new Date(a.occurredAt))
                  : parentTime(a.occurredAt)}
              </span>
            </div>
          ))
        ) : (
          <EmptyState compact title="No recent academic activity" />
        )}
      </Card>
      <p className="mt-5 text-xs text-slate-500">
        Academic charts use {d.progress.fromDate} to {d.progress.toDate}. Fees
        reflect current lifetime balances. Updates refresh every 30 seconds and
        on focus.
      </p>
    </>
  );
}
function ChildSection({ mode, child, from, to, revision, recentResults }) {
  const [page, setPage] = useState(0),
    [batchPage, setBatchPage] = useState(0),
    [size, setSize] = useState(10),
    [search, setSearch] = useState(""),
    [batch, setBatch] = useState(""),
    [course, setCourse] = useState(""),
    [status, setStatus] = useState("");
  const params = {
    page,
    batchPage,
    size,
    search,
    batchId: batch || undefined,
    courseId: course || undefined,
    status: status || undefined,
    fromDate: from || undefined,
    toDate: to || undefined,
  };
  const loaders = {
    attendance: getChildAttendance,
    fees: getChildFees,
    results: getChildResults,
    assignments: getChildAssignments,
    resources: getChildResources,
    progress: getChildProgress,
  };
  const state = useParentLoad(
    (signal) => loaders[mode](child.id, params, signal),
    [
      child.id,
      mode,
      page,
      batchPage,
      size,
      search,
      batch,
      course,
      status,
      from,
      to,
      revision,
    ],
  );
  useEffect(() => {
    setPage(0);
    setBatchPage(0);
  }, [from, to, search, batch, course, status, size]);
  const d = state.data,
    rows =
      mode === "attendance" ? d?.history : mode === "fees" ? d?.accounts : d;
  return (
    <>
      <Card className="mb-5 grid gap-4 p-5 md:grid-cols-3">
        {["attendance", "results", "assignments", "resources"].includes(
          mode,
        ) && (
          <ParentScope
            child={child}
            course={course}
            batch={batch}
            onCourse={
              ["results", "resources"].includes(mode)
                ? (v) => {
                    setCourse(v);
                    setBatch("");
                  }
                : null
            }
            onBatch={setBatch}
          />
        )}
        {["assignments", "resources"].includes(mode) && (
          <Input
            aria-label="Search records"
            placeholder="Search…"
            maxLength={100}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        )}
        {mode === "assignments" && (
          <Select
            aria-label="Submission status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">All submissions</option>
            {["PENDING", "SUBMITTED", "LATE", "GRADED"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        )}
        <p className="text-xs leading-5 text-slate-500">
          {mode === "fees"
            ? "Lifetime fee agreements and successful collections. Parents have read-only access."
            : mode === "resources"
              ? "Published metadata only. File previews, links and downloads remain available through the student's own resource portal."
              : mode === "assignments"
                ? "Published homework and preserved submitted history. Feedback appears when graded."
                : "Default academic range: the last 30 institute-local days. Choose dates above to view up to 366 days."}
        </p>
      </Card>
      <ParentError state={state} />
      {state.loading ? (
        <Skeleton className="h-64" />
      ) : (
        d &&
        !state.error && (
          <>
            {mode === "attendance" ? (
              <>
                <ParentMetrics
                  items={[
                    ["Attendance", pct(d.totals.attendancePercentage)],
                    [
                      "Present / late",
                      d.totals.presentCount + " / " + d.totals.lateCount,
                    ],
                    ["Absent", d.totals.absentCount],
                    ["Excused", d.totals.excusedCount],
                  ]}
                />
                <ParentAttendanceCalendar
                  key={d.fromDate + d.toDate}
                  trend={d.trend}
                  fromDate={d.fromDate}
                  toDate={d.toDate}
                />
                <Card className="mb-5 p-5">
                  <h2 className="mb-3 text-sm font-semibold">
                    Batch-wise attendance
                  </h2>
                  {d.batches.content.map((b) => (
                    <p key={b.id} className="mb-2 flex justify-between text-xs">
                      <span>{b.name}</span>
                      <strong>{pct(b.totals.attendancePercentage)}</strong>
                    </p>
                  ))}
                  <Pagination
                    noun="batch summaries"
                    page={batchPage}
                    size={size}
                    total={d.batches.totalElements}
                    totalPages={d.batches.totalPages}
                    onPageChange={setBatchPage}
                    onSizeChange={setSize}
                  />
                </Card>
                <Card>
                  <AttendanceTable rows={d.history.content} />
                </Card>
              </>
            ) : mode === "fees" ? (
              <Fees data={d} childId={child.id} />
            ) : mode === "progress" ? (
              <>
                <ParentMetrics
                  items={[
                    ["Attendance", pct(d.attendance.attendancePercentage)],
                    ["Average exam score", pct(d.averageExamPercentage)],
                    [
                      "Assignment completion",
                      pct(d.assignmentCompletionPercentage),
                    ],
                    [
                      "Submitted / eligible",
                      d.submittedAssignments + " / " + d.eligibleAssignments,
                    ],
                  ]}
                />
                <Suspense
                  fallback={<Loading label="Loading progress charts…" />}
                >
                  <ParentCharts progress={d} assignments />
                </Suspense>
                <Card className="mt-5 p-5">
                  <h2 className="mb-4 text-sm font-semibold">
                    Course-wise academic progress
                  </h2>
                  {d.courses.map((c) => (
                    <div
                      key={c.id}
                      className="mb-4 rounded-lg bg-slate-50 p-4 text-xs"
                    >
                      <p className="font-medium">{c.name}</p>
                      <p className="mt-2 text-slate-500">
                        Published exams: {c.publishedResults} · Average{" "}
                        {pct(c.averageExamPercentage)} · Assignment completion{" "}
                        {pct(c.assignmentCompletionPercentage)}
                      </p>
                    </div>
                  ))}
                </Card>
                <Card className="mt-5 p-5">
                  <h2 className="mb-4 text-sm font-semibold">
                    Recent published results
                  </h2>
                  <ResultTable rows={recentResults || []} />
                </Card>
              </>
            ) : mode === "results" ? (
              <>
                <Suspense fallback={<Skeleton className="h-64" />}>
                  <ParentCharts
                    progress={{
                      attendanceTrend: [],
                      assignmentTrend: [],
                      examTrend: d.content,
                    }}
                    resultsOnly
                  />
                </Suspense>
                <Card>
                  <ResultTable rows={d.content} />
                </Card>
              </>
            ) : mode === "assignments" ? (
              <Card>
                <AssignmentTable rows={d.content} />
              </Card>
            ) : (
              <div className="grid gap-4 md:grid-cols-2">
                {d.content.length ? (
                  d.content.map((m) => (
                    <Card key={m.id} className="p-5">
                      <Badge tone="indigo">{m.resourceType}</Badge>
                      <h2 className="mt-4 text-sm font-semibold">{m.title}</h2>
                      <p className="mt-2 whitespace-pre-wrap break-words text-xs leading-6 text-slate-500">
                        {m.description}
                      </p>
                      <p className="mt-3 text-xs">
                        {m.courseName} · {m.batchName || "Course-wide"}
                      </p>
                      <p className="mt-2 text-xs text-slate-500">
                        {m.categoryName} · {parentTime(m.publishedAt)}
                      </p>
                    </Card>
                  ))
                ) : (
                  <EmptyState title="No eligible published resources" />
                )}
              </div>
            )}
            {mode !== "progress" && (
              <Pagination
                noun={mode}
                page={page}
                size={size}
                total={rows?.totalElements}
                totalPages={rows?.totalPages}
                onPageChange={setPage}
                onSizeChange={setSize}
                loading={state.loading}
              />
            )}
          </>
        )
      )}
    </>
  );
}
function ResultTable({ rows }) {
  return rows.length ? (
    <Table label="Child published results">
      <thead>
        <tr>
          <th>Exam</th>
          <th>Course / batch</th>
          <th>Marks</th>
          <th>Percentage</th>
          <th>Grade</th>
          <th>Result</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              {r.examTitle}
              <p className="mt-1 text-xs text-slate-500">
                {parentTime(r.examDate)}
              </p>
            </td>
            <td>
              {r.courseName}
              <p className="text-xs">{r.batchName}</p>
            </td>
            <td>
              {r.obtainedMarks} / {r.totalMarks}
            </td>
            <td>{pct(r.percentage)}</td>
            <td>{r.grade}</td>
            <td>
              <Badge tone={r.passed ? "success" : "warning"}>
                {r.passed ? "Passed" : "Not passed"}
              </Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  ) : (
    <EmptyState title="No published results in this range" />
  );
}
function AssignmentTable({ rows }) {
  return rows.length ? (
    <Table label="Child assignments">
      <thead>
        <tr>
          <th>Assignment</th>
          <th>Due date</th>
          <th>Status</th>
          <th>Submitted</th>
          <th>Grade</th>
          <th>Feedback</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              {r.title}
              <p className="mt-1 text-xs text-slate-500">
                {r.batchName} · {r.assignmentStatus}
              </p>
            </td>
            <td>{parentTime(r.dueDateTime)}</td>
            <td>
              <Badge
                tone={r.submissionStatus === "GRADED" ? "success" : "neutral"}
              >
                {r.submissionStatus}
              </Badge>
              {r.late && (
                <p className="mt-1 text-xs text-amber-600">Submitted late</p>
              )}
            </td>
            <td>{parentTime(r.submittedAt)}</td>
            <td>
              {r.obtainedMarks == null
                ? "Not graded"
                : r.obtainedMarks + " / " + r.maxMarks}
            </td>
            <td className="max-w-xs whitespace-pre-wrap break-words">
              {r.feedback || "—"}
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  ) : (
    <EmptyState title="No assignments match these filters" />
  );
}
function AttendanceTable({ rows }) {
  return rows.length ? (
    <Table label="Child attendance history">
      <thead>
        <tr>
          <th>Date</th>
          <th>Batch / course</th>
          <th>Class</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.sessionId}>
            <td>{r.sessionDate}</td>
            <td>
              {r.batchName}
              <p className="text-xs text-slate-500">{r.courseName}</p>
            </td>
            <td>
              {r.startTime || "—"} – {r.endTime || "—"}
              <p className="mt-1 text-xs">{r.topic}</p>
            </td>
            <td>
              <Badge
                tone={
                  ["PRESENT", "LATE"].includes(r.status)
                    ? "success"
                    : r.status === "ABSENT"
                      ? "warning"
                      : "neutral"
                }
              >
                {r.status}
              </Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  ) : (
    <EmptyState title="No finalized attendance records" />
  );
}
function Fees({ data, childId }) {
  const [busy, setBusy] = useState(null),
    [error, setError] = useState("");
  const download = async (payment) => {
    if (busy) return;
    setBusy(payment.id);
    setError("");
    try {
      const r = await downloadChildReceipt(childId, payment.id);
      saveDownload(r.data, payment.receiptNumber + ".pdf");
    } catch (e) {
      setError(await parentError(e));
    } finally {
      setBusy(null);
    }
  };
  return (
    <>
      <ParentMetrics
        items={[
          ["Total payable", inr(data.totals.totalPayable)],
          ["Total successful payments", inr(data.totals.totalPaid)],
          ["Outstanding balance", inr(data.totals.outstanding)],
          ["Overdue amount", inr(data.totals.overdue)],
        ]}
      />
      <ErrorNotice message={error} />
      {!data.accounts.content.length ? (
        <Card>
          <EmptyState title="No fee accounts yet" />
        </Card>
      ) : (
        data.accounts.content.map((a) => (
          <Card key={a.id} className="mb-5 p-5">
            <h2 className="text-sm font-semibold">
              {a.courseName} · {a.batchName}
            </h2>
            <p className="my-4 text-xs text-slate-500">
              Account #{a.id} · Payable {inr(a.totalPayable)} · Paid{" "}
              {inr(a.paidAmount)} · Outstanding {inr(a.outstandingAmount)}
            </p>
            <h3 className="mb-3 text-xs font-semibold">Installment schedule</h3>
            {a.installments.length ? (
              <Table label="Installment schedule">
                <thead>
                  <tr>
                    <th>Installment</th>
                    <th>Due date</th>
                    <th>Amount</th>
                    <th>Paid</th>
                    <th>Outstanding</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {a.installments.map((i) => (
                    <tr key={i.id}>
                      <td>{i.installmentNumber}</td>
                      <td>{i.dueDate}</td>
                      <td>{inr(i.amount)}</td>
                      <td>{inr(i.paidAmount)}</td>
                      <td>{inr(i.outstandingAmount)}</td>
                      <td>{i.status}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            ) : (
              <p className="text-xs text-slate-500">
                No installment schedule recorded.
              </p>
            )}
            <h3 className="mb-3 mt-6 text-xs font-semibold">Payment history</h3>
            {a.payments.length ? (
              <Table label="Child payments">
                <thead>
                  <tr>
                    <th>Receipt</th>
                    <th>Date</th>
                    <th>Method</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {a.payments.map((p) => (
                    <tr key={p.id}>
                      <td>{p.receiptNumber}</td>
                      <td>{p.paymentDate}</td>
                      <td>{p.paymentMethod}</td>
                      <td>{inr(p.amount)}</td>
                      <td>{p.status}</td>
                      <td>
                        <Button
                          variant="secondary"
                          disabled={!!busy}
                          onClick={() => download(p)}
                        >
                          {busy === p.id
                            ? "Downloading…"
                            : p.status === "VOIDED"
                              ? "Download void notice"
                              : "Download receipt"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            ) : (
              <p className="text-xs text-slate-500">No payments recorded.</p>
            )}
          </Card>
        ))
      )}
    </>
  );
}
