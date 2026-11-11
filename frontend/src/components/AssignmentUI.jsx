import { useEffect, useEffectEvent, useState } from "react";
import { Link } from "react-router-dom";
import { Download, Paperclip } from "lucide-react";
import {
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Input,
  Table,
  Skeleton,
} from "./UI";
import {
  Field,
  useExamLoad,
  ExamError,
  examError,
  examTime,
  pct,
  staffRole,
} from "./ExamUI";
import {
  getUploadPolicy,
  getAssignmentStats,
  downloadAssignmentFile,
} from "../services/assignmentService";
import { downloadSubmissionFile } from "../services/submissionService";
import { saveDownload } from "../services/financeService";
export {
  Field,
  useExamLoad as useAssignmentLoad,
  ExamError as AssignmentError,
  examError as assignmentError,
  examTime as assignmentTime,
  pct,
  staffRole,
};
export function AssignmentHeading({ title, subtitle, children }) {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Learning & homework</p>
        <h1 className="page-title">{title}</h1>
        <p className="mt-2 text-sm text-slate-500">{subtitle}</p>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function AssignmentDenied() {
  return (
    <Card>
      <EmptyState
        title="Assignment access restricted"
        description="This feature is unavailable for your role."
      />
    </Card>
  );
}
export function useAssignmentStats(batchId, allowed = true) {
  const state = useExamLoad(
    (signal) => getAssignmentStats({ batchId: batchId || undefined }, signal),
    [batchId],
    allowed,
  );
  const refreshStats = useEffectEvent(() => state.reload());
  useEffect(() => {
    if (!allowed) return;
    const refresh = () => {
      if (document.visibilityState !== "hidden") refreshStats();
    };
    const timer = setInterval(refresh, 30000);
    window.addEventListener("focus", refresh);
    window.addEventListener("eduflow:assignments-changed", refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener("focus", refresh);
      window.removeEventListener("eduflow:assignments-changed", refresh);
    };
  }, [allowed, batchId]);
  return state;
}
export function AssignmentStats({ state, student = false }) {
  const d = state.data;
  const items = student
    ? [
        ["Pending homework", d?.pendingSubmissions],
        ["Submitted", d?.submittedAssignments],
        ["Overdue", d?.overdueAssignments],
        ["Graded", d?.gradedSubmissions],
      ]
    : [
        ["Assignments", d?.totalAssignments],
        ["Pending submissions", d?.pendingSubmissions],
        [
          "Awaiting grading",
          d ? d.submittedAssignments - d.gradedSubmissions : null,
        ],
        ["Completion", d ? pct(d.completionPercentage) : null],
      ];
  return (
    <div className="mb-6 grid grid-cols-2 gap-4 xl:grid-cols-4">
      {items.map(([label, v]) => (
        <Card className="p-5" key={label}>
          <p className="text-xs text-slate-500">{label}</p>
          {state.loading ? (
            <Skeleton className="mt-3 h-8 w-20" />
          ) : (
            <p className="mt-3 text-2xl font-semibold">{v ?? "—"}</p>
          )}
        </Card>
      ))}
    </div>
  );
}
export function FilePicker({
  files,
  onChange,
  disabled = false,
  existingCount = 0,
}) {
  const policy = useExamLoad((signal) => getUploadPolicy(signal), []);
  const [error, setError] = useState("");
  return (
    <div>
      <label className="field-label block">
        <Paperclip size={15} className="mb-2 inline" /> Attach files (PDF, DOCX,
        PNG, JPG)
        <Input
          className="mt-2"
          type="file"
          multiple
          accept=".pdf,.docx,.png,.jpg,.jpeg"
          disabled={disabled || policy.loading || !!policy.error}
          onChange={(e) => {
            const next = Array.from(e.target.files || []);
            if (next.length + existingCount > policy.data.maxFiles) {
              setError("Maximum ten files in one assignment or submission.");
              return;
            }
            if (next.some((f) => f.size > policy.data.maxFileBytes)) {
              setError("A selected file exceeds the configured size limit.");
              return;
            }
            setError("");
            onChange(next);
          }}
        />
      </label>
      <ExamError state={policy} />
      <ErrorNotice message={error} />
      {policy.data && (
        <p className="mt-2 text-xs leading-6 text-slate-500">
          Up to {policy.data.maxFiles} files ·{" "}
          {Math.round((policy.data.maxFileBytes / 1024 / 1024) * 10) / 10} MB
          each. Actual content is checked by the server.
        </p>
      )}
      {files.length > 0 && (
        <ul className="mt-3 space-y-2 text-xs">
          {files.map((f, i) => (
            <li
              key={i}
              className="flex items-center justify-between gap-3 rounded-lg bg-slate-50 p-3"
            >
              <span className="min-w-0 break-all">{f.name}</span>
              <Button
                variant="ghost"
                disabled={disabled}
                onClick={() => onChange(files.filter((_, n) => n !== i))}
              >
                Remove
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
export function UploadProgress({ value, busy }) {
  return busy && value != null ? (
    <div role="status" className="my-4 text-xs text-slate-500">
      Upload: {value}%
      {value === 100 ? " · Server is validating and saving…" : ""}
      <progress
        className="mt-2 block w-full accent-indigo-600"
        aria-label="File upload progress"
        max={100}
        value={value}
      />
    </div>
  ) : null;
}
export const progressHandler = (set) => (event) => {
  if (event.total)
    set(Math.min(100, Math.round((event.loaded / event.total) * 100)));
};
export function AttachmentList({
  files = [],
  submission = false,
  onRemove,
  busy = false,
}) {
  const [error, setError] = useState(""),
    [downloading, setDownloading] = useState(null);
  if (!files.length)
    return <p className="text-xs text-slate-500">No attachments.</p>;
  return (
    <>
      <ErrorNotice message={error} />
      <ul className="space-y-2">
        {files.map((file) => (
          <li
            key={file.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 p-3"
          >
            <div className="min-w-0">
              <p className="break-all text-sm font-medium">
                {file.originalFileName}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {Math.ceil(file.fileSize / 1024)} KB
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="ghost"
                disabled={downloading != null || busy}
                aria-label={`Download ${file.originalFileName}`}
                onClick={async () => {
                  setDownloading(file.id);
                  setError("");
                  try {
                    const { data } = await (
                      submission
                        ? downloadSubmissionFile
                        : downloadAssignmentFile
                    )(file.id);
                    saveDownload(data, file.originalFileName);
                  } catch (e) {
                    setError(await examError(e));
                  } finally {
                    setDownloading(null);
                  }
                }}
              >
                <Download size={15} />
                {downloading === file.id ? "Downloading…" : "Download"}
              </Button>
              {onRemove && (
                <Button
                  variant="ghost"
                  disabled={busy}
                  onClick={() => onRemove(file)}
                >
                  Remove
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </>
  );
}
export function SubmissionTable({ rows = [], staff = false }) {
  if (!rows.length)
    return (
      <EmptyState
        title="No submissions found"
        description="Submitted homework and feedback will appear here."
      />
    );
  return (
    <Table label="Assignment submissions" className="min-w-[760px]">
      <thead>
        <tr>
          <th>{staff ? "Student" : "Assignment"}</th>
          <th>Batch</th>
          <th>Submitted (IST)</th>
          <th>Status</th>
          <th>Marks</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {rows.map((s) => (
          <tr key={s.id}>
            <td>
              <p className="font-medium">
                {staff ? s.studentName : s.assignmentTitle}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {staff ? s.admissionNumber : s.courseName}
              </p>
              {!staff && s.feedback && (
                <p className="mt-2 max-w-64 truncate text-xs text-slate-500">
                  {s.feedback}
                </p>
              )}
            </td>
            <td>{s.batchName}</td>
            <td>{examTime(s.submittedAt)}</td>
            <td>
              <span className="text-xs font-medium">
                {s.status}
                {s.late && s.status === "GRADED" ? " · LATE" : ""}
              </span>
            </td>
            <td>
              {s.obtainedMarks == null
                ? "Not graded"
                : s.obtainedMarks + " / " + s.maxMarks}
            </td>
            <td>
              <Link
                className="button-secondary"
                to={
                  staff
                    ? `/assignments/submissions/${s.id}`
                    : `/assignments/${s.assignmentId}/submit`
                }
              >
                {staff ? "View / Grade" : "View work"}
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
