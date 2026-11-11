import { useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  ErrorNotice,
  Loading,
  Toast,
} from "../components/UI";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  AttachmentList,
  FilePicker,
  UploadProgress,
  progressHandler,
  staffRole,
  useAssignmentLoad,
  assignmentError,
  assignmentTime,
  pct,
} from "../components/AssignmentUI";
import {
  getAssignment,
  getAssignmentFiles,
  publishAssignment,
  closeAssignment,
  uploadAssignmentFiles,
  removeAssignmentFile,
} from "../services/assignmentService";
export default function AssignmentDetailsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT",
    { state: routeState } = useLocation();
  const state = useAssignmentLoad(
      (signal) => getAssignment(id, signal),
      [id],
      allowed,
    ),
    attached = useAssignmentLoad(
      (signal) => getAssignmentFiles(id, signal),
      [id],
      allowed,
    );
  const [files, setFiles] = useState([]),
    [action, setAction] = useState(""),
    [target, setTarget] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [progress, setProgress] = useState(null),
    [toast, setToast] = useState(routeState?.message || "");
  if (!allowed) return <AssignmentDenied />;
  if (state.loading) return <Loading label="Loading assignment…" />;
  if (state.error) return <AssignmentError state={state} />;
  if (!state.data) return null;
  const a = state.data;
  const refresh = () => {
    state.reload();
    attached.reload();
  };
  return (
    <>
      <AssignmentHeading
        title={a.title}
        subtitle={`${a.courseName} · ${a.batchName} · Created by ${a.createdBy}`}
      >
        <Link className="button-secondary" to="/assignments">
          All assignments
        </Link>
        {staff && a.status === "DRAFT" && (
          <>
            <Link className="button-secondary" to={`/assignments/${id}/edit`}>
              Edit
            </Link>
            <Button
              onClick={() => {
                setAction("publish");
                setError("");
              }}
            >
              Publish Assignment
            </Button>
          </>
        )}
        {staff && a.status === "PUBLISHED" && (
          <Button
            variant="secondary"
            onClick={() => {
              setAction("close");
              setError("");
            }}
          >
            Close assignment
          </Button>
        )}
        {staff && a.status !== "DRAFT" && (
          <Link
            className="button-primary"
            to={`/assignments/${id}/submissions`}
          >
            Review submissions
          </Link>
        )}
        {!staff && (
          <Link className="button-primary" to={`/assignments/${id}/submit`}>
            {a.ownSubmissionId ? "View / Revise work" : "Submit homework"}
          </Link>
        )}
      </AssignmentHeading>
      <Card className="mb-6 p-6">
        <div className="flex flex-wrap gap-3">
          <Badge tone={a.status === "PUBLISHED" ? "indigo" : "neutral"}>
            {a.status}
          </Badge>
          <span className="text-xs text-slate-500">
            {a.maxMarks} marks · Due {assignmentTime(a.dueDateTime)}
          </span>
        </div>
        <p className="mt-5 whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
          {a.description || "No additional instructions."}
        </p>
        <p className="mt-5 text-xs leading-6 text-slate-500">
          {a.allowLateSubmission
            ? "Late first submissions are accepted and labeled LATE."
            : "Late submissions are not accepted."}{" "}
          Revisions are allowed only before the deadline and before grading.
          Published instructions, marks, deadlines, and teaching attachments are
          immutable.
        </p>
      </Card>
      {staff && a.status !== "DRAFT" && (
        <Card className="mb-6 grid grid-cols-2 gap-5 p-5 sm:grid-cols-4">
          {[
            ["Eligible students", a.counts.eligible],
            ["Submitted", a.counts.submitted],
            ["Pending", a.counts.pending],
            ["Completion", pct(a.counts.completionPercentage)],
          ].map(([label, value]) => (
            <div key={label}>
              <p className="text-xs text-slate-500">{label}</p>
              <p className="mt-2 text-2xl font-semibold">{value}</p>
            </div>
          ))}
        </Card>
      )}
      <Card className="p-6">
        <h2 className="mb-5 text-sm font-semibold">Assignment attachments</h2>
        <AssignmentError state={attached} />
        {attached.loading ? (
          <Loading label="Loading attachments…" />
        ) : (
          <AttachmentList
            files={attached.data || []}
            busy={busy}
            onRemove={
              staff && a.status === "DRAFT"
                ? (file) => {
                    setTarget(file);
                    setAction("remove");
                    setError("");
                  }
                : null
            }
          />
        )}
        {staff && a.status === "DRAFT" && (
          <div className="mt-6 border-t border-slate-100 pt-5">
            <ErrorNotice message={error} />
            <FilePicker
              files={files}
              onChange={setFiles}
              disabled={busy}
              existingCount={attached.data?.length || 0}
            />
            <UploadProgress value={progress} busy={busy} />
            <Button
              className="mt-4"
              disabled={busy || !files.length}
              onClick={async () => {
                setBusy(true);
                setError("");
                setProgress(null);
                try {
                  await uploadAssignmentFiles(
                    id,
                    a.version,
                    files,
                    progressHandler(setProgress),
                  );
                  setFiles([]);
                  setToast("Attachments uploaded.");
                  refresh();
                } catch (err) {
                  setError(await assignmentError(err));
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? "Uploading…" : "Upload attachments"}
            </Button>
          </div>
        )}
      </Card>
      {action && (
        <ConfirmDialog
          title={
            action === "publish"
              ? "Publish assignment?"
              : action === "close"
                ? "Close assignment?"
                : "Remove draft attachment?"
          }
          confirmLabel="Confirm"
          busy={busy}
          error={error}
          onClose={() => setAction("")}
          onConfirm={async () => {
            setBusy(true);
            setError("");
            try {
              if (action === "publish") await publishAssignment(id, a.version);
              else if (action === "close") await closeAssignment(id, a.version);
              else await removeAssignmentFile(target.id, a.version);
              setToast(
                action === "publish"
                  ? "Assignment published to eligible students."
                  : action === "close"
                    ? "Assignment closed. History is preserved."
                    : "Attachment removed.",
              );
              setAction("");
              refresh();
            } catch (err) {
              setError(await assignmentError(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          {action === "publish"
            ? "The current eligible roster will be assigned this homework. Published instructions, files, marks and deadlines cannot be changed."
            : action === "close"
              ? "New submissions and revisions will stop. Existing work, downloads and grading remain available."
              : "Only draft teaching attachments can be removed."}
        </ConfirmDialog>
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
