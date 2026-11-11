import { Link } from "react-router-dom";
import { Badge, Card, EmptyState, Table } from "./UI";
export {
  Field,
  useFinanceLoad as useExamLoad,
  FinanceError as ExamError,
} from "./FinanceUI";
export { financeError as examError } from "../services/financeService";
export const staffRole = (role) => ["ADMIN", "TEACHER"].includes(role);
export const examTime = (value) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Kolkata",
      }).format(new Date(value))
    : "—";
export const pct = (value) =>
  value == null ? "No data" : `${Number(value).toFixed(2)}%`;
export function ExamHeading({ title, subtitle, children }) {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Assessments</p>
        <h1 className="page-title">{title}</h1>
        <p className="mt-2 text-sm text-slate-500">{subtitle}</p>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function ExamBadge({ status }) {
  return (
    <Badge
      tone={
        ["COMPLETED", "SUBMITTED", "AUTO_SUBMITTED"].includes(status)
          ? "success"
          : status === "PUBLISHED"
            ? "indigo"
            : "neutral"
      }
    >
      {status.replaceAll("_", " ")}
    </Badge>
  );
}
export function ExamDenied() {
  return (
    <Card>
      <EmptyState
        title="Exam access restricted"
        description="This feature is not available for your role."
      />
    </Card>
  );
}
export function ResultTable({ rows = [], own = false }) {
  return rows.length ? (
    <Table label="Exam results" className="min-w-[700px]">
      <thead>
        <tr>
          <th>{own ? "Exam" : "Student"}</th>
          <th>Batch</th>
          <th>Marks</th>
          <th>Percentage</th>
          <th>Grade</th>
          <th>Result</th>
          {own && <th />}
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              <p className="font-medium">{own ? r.examTitle : r.studentName}</p>
              <p className="mt-1 text-xs text-slate-500">
                {own ? examTime(r.examDate) : r.admissionNumber}
              </p>
            </td>
            <td>{r.batchName}</td>
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
            {own && (
              <td>
                <Link
                  className="button-secondary"
                  to={`/exams/${r.examId}/result`}
                >
                  View result
                </Link>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </Table>
  ) : (
    <EmptyState
      title="No published results"
      description="Results appear after your institute publishes them."
    />
  );
}
