import { lazy, Suspense, useState } from "react";
import { Link } from "react-router-dom";
import { Download, Plus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ErrorNotice,
  Pagination,
  Table,
  TableSkeleton,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import { DateRange, localDate } from "../components/AttendanceUI";
import {
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  FinanceStats,
  PaymentsTable,
  inr,
  useFinanceLoad,
} from "../components/FinanceUI";
import {
  getFinanceDashboard,
  getFinanceReport,
  exportFinanceReport,
  saveDownload,
  financeError,
} from "../services/financeService";
const FinanceCharts = lazy(() => import("../components/FinanceCharts"));
export function financeRange() {
  const toDate = localDate();
  const date = new Date(toDate + "T12:00:00+05:30");
  date.setDate(1);
  date.setMonth(date.getMonth() - 11);
  return { fromDate: localDate(date), toDate };
}
export default function FinancePage({ reports = false }) {
  const { user } = useAuth(),
    allowed = user.role === "ADMIN";
  const [range, setRange] = useState(financeRange),
    [course, setCourse] = useState(""),
    [batch, setBatch] = useState(""),
    [student, setStudent] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const [exporting, setExporting] = useState(false),
    [error, setError] = useState("");
  const filters = {
    ...range,
    courseId: course || undefined,
    batchId: batch || undefined,
    studentId: student || undefined,
  };
  const state = useFinanceLoad(
    (signal) =>
      (reports ? getFinanceReport : getFinanceDashboard)(
        { ...filters, page, size },
        signal,
      ),
    [reports, range.fromDate, range.toDate, course, batch, student, page, size],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  return (
    <>
      <FinanceHeading
        title={reports ? "Finance Reports" : "Finance Overview"}
        subtitle="Track collections, outstanding dues, and student fee accounts."
      >
        {reports ? (
          <Button
            variant="secondary"
            disabled={exporting || state.loading || !!state.error}
            onClick={async () => {
              setExporting(true);
              setError("");
              try {
                const { data } = await exportFinanceReport(filters);
                saveDownload(data, "eduflow-finance.csv");
              } catch (e) {
                setError(await financeError(e));
              } finally {
                setExporting(false);
              }
            }}
          >
            <Download size={16} />
            {exporting ? "Exporting…" : "Export CSV"}
          </Button>
        ) : (
          <>
            <Link className="button-secondary" to="/finance/accounts">
              View fee accounts
            </Link>
            <Link
              className="button-primary"
              to="/finance/accounts?recordPayment=1"
            >
              <Plus size={16} />
              Record payment
            </Link>
          </>
        )}
      </FinanceHeading>
      <Card className="mb-6 p-5">
        <div className="mb-4">
          <DateRange
            value={range}
            onChange={(v) => {
              setRange(v);
              setPage(0);
            }}
          />
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <RecordSelect
            endpoint="/courses"
            status=""
            label="Course"
            required={false}
            value={course}
            onChange={(v) => {
              setCourse(v);
              setPage(0);
            }}
          />
          <RecordSelect
            endpoint="/batches"
            status=""
            label="Batch"
            required={false}
            value={batch}
            onChange={(v) => {
              setBatch(v);
              setPage(0);
            }}
          />
          <RecordSelect
            endpoint="/students"
            status=""
            label="Student"
            required={false}
            value={student}
            onChange={(v) => {
              setStudent(v);
              setPage(0);
            }}
          />
        </div>
        <p className="mt-4 text-xs leading-6 text-slate-500">
          Collections and charts use the selected payment dates. Agreed fees and
          outstanding dues use lifetime balances for the selected students,
          courses, and batches. Overdue is assessed today in Asia/Kolkata.
          Today's and this month's collections use the current institute date.
          Voided payments are excluded from all collection totals.
        </p>
      </Card>
      <FinanceError state={state} />
      <ErrorNotice message={error} />
      {state.loading ? (
        <>
          <FinanceStats loading />
          <TableSkeleton />
        </>
      ) : (
        !state.error &&
        state.data && (
          <>
            <FinanceStats data={state.data} />
            <div className="mb-5 flex flex-wrap gap-5 text-xs text-slate-500">
              <span>
                Total agreed fees:{" "}
                <strong className="text-slate-700">
                  {inr(state.data.totalAgreedFees)}
                </strong>
              </span>
              <span>
                Today's collections:{" "}
                <strong className="text-slate-700">
                  {inr(state.data.todayCollections)}
                </strong>
              </span>
              <span>
                Lifetime collections:{" "}
                <strong className="text-slate-700">
                  {inr(state.data.lifetimeCollections)}
                </strong>
              </span>
              <span>
                {state.data.fromDate} to {state.data.toDate} · INR
              </span>
            </div>
            <Suspense fallback={<TableSkeleton />}>
              <FinanceCharts data={state.data} />
            </Suspense>
            {reports && (
              <div className="mb-6 grid gap-6 lg:grid-cols-2">
                <MetricTable
                  title="Course-wise collections"
                  rows={state.data.courseCollections}
                />
                <MetricTable
                  title="Batch-wise outstanding dues"
                  rows={state.data.batchOutstanding}
                />
              </div>
            )}
            <Card className="overflow-hidden">
              <div className="flex justify-between p-5">
                <h2 className="text-sm font-semibold">
                  {reports ? "Payment ledger" : "Recent payments"}
                </h2>
                <Link
                  className="text-xs font-medium text-indigo-600"
                  to="/finance/payments"
                >
                  All payments
                </Link>
              </div>
              <PaymentsTable rows={state.data.payments.content} admin />
              <Pagination
                noun="payments"
                page={page}
                size={size}
                total={state.data.payments.totalElements}
                totalPages={state.data.payments.totalPages}
                onPageChange={setPage}
                onSizeChange={(v) => {
                  setSize(v);
                  setPage(0);
                }}
              />
            </Card>
            {reports && (
              <p className="mt-5 text-xs text-slate-500">
                CSV includes successful and voided ledger rows with status,
                using the same filters. Limit: 10,000 payments per export.
                Course and batch summaries show up to 100 groups.
              </p>
            )}
          </>
        )
      )}
    </>
  );
}
function MetricTable({ title, rows }) {
  return (
    <Card className="overflow-hidden">
      <h2 className="p-5 text-sm font-semibold">{title}</h2>
      <Table label={title}>
        <thead>
          <tr>
            <th>Name</th>
            <th>Amount (INR)</th>
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((r, i) => (
              <tr key={i}>
                <td>{r.label}</td>
                <td>{inr(r.amount)}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={2} className="text-slate-500">
                No matching financial records.
              </td>
            </tr>
          )}
        </tbody>
      </Table>
    </Card>
  );
}
