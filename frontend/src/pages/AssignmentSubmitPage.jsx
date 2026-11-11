import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Badge,
  Button,
  Card,
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
  useAssignmentLoad,
  assignmentError,
  assignmentTime,
  Field,
} from "../components/AssignmentUI";
import {
  getAssignment,
  getAssignmentFiles,
} from "../services/assignmentService";
import { getSubmission, saveSubmission } from "../services/submissionService";
export default function AssignmentSubmitPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = user.role === "STUDENT";
  const [toast, setToast] = useState("");
  const state = useAssignmentLoad(
    async (signal) => {
      const { data: assignment } = await getAssignment(id, signal);
      const [files, work] = await Promise.all([
        getAssignmentFiles(id, signal),
        assignment.ownSubmissionId
          ? getSubmission(assignment.ownSubmissionId, signal)
          : Promise.resolve({ data: null }),
      ]);
      return { data: { assignment, files: files.data, work: work.data } };
    },
    [id],
    allowed,
  );
  if (!allowed) return <AssignmentDenied />;
  if (state.loading) return <Loading label="Loading your homework…" />;
  if (state.error) return <AssignmentError state={state} />;
  if (!state.data) return null;
  return (
    <>
      <SubmissionForm
        key={state.data.work?.version ?? "new"}
        initial={state.data}
        user={user}
        onSaved={(message) => {
          setToast(message);
          state.reload();
        }}
      />
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
function SubmissionForm({ initial, user, onSaved }) {
  const { assignment: a, files: teachingFiles, work } = initial;
  const storageKey = `eduflow.homework.${user.userId || user.id || user.email}.${a.id}`;
  const [text, setText] = useState(() => {
      try {
        return sessionStorage.getItem(storageKey) ?? work?.submissionText ?? "";
      } catch {
        return work?.submissionText || "";
      }
    }),
    [files, setFiles] = useState([]),
    [keep, setKeep] = useState(work?.attachments.map((f) => f.id) || []),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [progress, setProgress] = useState(null),
    [remaining, setRemaining] = useState(1),
    [dirty, setDirty] = useState(() => {
      if (work?.status === "GRADED" || a.status === "CLOSED") return false;
      try {
        const cached = sessionStorage.getItem(storageKey);
        return cached !== null && cached !== (work?.submissionText || "");
      } catch {
        return false;
      }
    });
  const anchor = useRef({
      at: performance.now(),
      ms: new Date(a.dueDateTime) - new Date(a.serverNow),
    }),
    lock = useRef(false);
  useEffect(() => {
    const tick = () =>
      setRemaining(
        Math.ceil(
          (anchor.current.ms - (performance.now() - anchor.current.at)) / 1000,
        ),
      );
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, []);
  useEffect(() => {
    if (!dirty) return;
    try {
      sessionStorage.setItem(storageKey, text);
    } catch { /* Storage may be unavailable; server submission remains available. */ }
  }, [text, dirty, storageKey]);
  useEffect(() => {
    if (!dirty) return;
    const warn = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  const graded = work?.status === "GRADED",
    late = remaining <= 0,
    editable =
      a.status === "PUBLISHED" &&
      !graded &&
      (!work ? !late || a.allowLateSubmission : !late);
  return (
    <>
      <AssignmentHeading
        title={a.title}
        subtitle={`${a.courseName} · ${a.batchName} · ${a.createdBy}`}
      >
        <Link className="button-secondary" to="/my-assignments">
          My homework
        </Link>
      </AssignmentHeading>
      <Card className="mb-6 p-6">
        <div className="flex flex-wrap items-center gap-4">
          <Badge tone={graded ? "success" : work ? "indigo" : "neutral"}>
            {work?.status || "NOT SUBMITTED"}
          </Badge>
          <p className="text-sm text-slate-500">
            Maximum {a.maxMarks} marks · Due {assignmentTime(a.dueDateTime)}
          </p>
        </div>
        <p
          className="mt-4 text-sm font-medium text-indigo-600"
          role="timer"
          aria-label="Homework deadline"
        >
          {late
            ? "Deadline has passed"
            : `${Math.floor(remaining / 86400)}d ${Math.floor((remaining % 86400) / 3600)}h ${Math.floor((remaining % 3600) / 60)}m remaining`}
        </p>
        <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
          {a.description}
        </p>
        <p className="mt-4 text-xs leading-6 text-slate-500">
          {a.allowLateSubmission
            ? "Late first submissions are allowed and labeled LATE."
            : "Late submissions are rejected."}{" "}
          Existing work can be revised only before the deadline and before
          grading. Server time controls acceptance.
        </p>
        <div className="mt-6">
          <h2 className="mb-3 text-sm font-semibold">Assignment attachments</h2>
          <AttachmentList files={teachingFiles} />
        </div>
      </Card>
      {graded && (
        <Card className="mb-6 border-emerald-200 p-6">
          <h2 className="text-sm font-semibold">Your grade & feedback</h2>
          <p className="my-4 text-3xl font-semibold">
            {work.obtainedMarks}{" "}
            <span className="text-lg text-slate-400">/ {work.maxMarks}</span>
          </p>
          <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">
            {work.feedback || "No written feedback."}
          </p>
          <p className="mt-4 text-xs text-slate-500">
            Graded by {work.gradedBy} · {assignmentTime(work.gradedAt)}
          </p>
        </Card>
      )}
      <Card className="p-6">
        <h2 className="mb-5 text-sm font-semibold">
          {work ? "Your submitted work" : "Submit your work"}
        </h2>
        <ErrorNotice message={error} />
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (lock.current) return;
            if (!text.trim() && !files.length && !keep.length) {
              setError("Add text or at least one file before submitting.");
              return;
            }
            lock.current = true;
            setBusy(true);
            setError("");
            setProgress(null);
            try {
              const { data } = await saveSubmission(
                a.id,
                work?.id,
                {
                  submissionText: text,
                  version: work?.version,
                  keepAttachmentIds: work ? keep : undefined,
                },
                files,
                progressHandler(setProgress),
              );
              try {
                sessionStorage.removeItem(storageKey);
              } catch { /* Storage may be unavailable; server submission remains available. */ }
              setDirty(false);
              onSaved(
                data.status === "LATE"
                  ? "Homework submitted and marked late."
                  : work
                    ? "Revision saved. Previous work remains in the history."
                    : "Homework submitted successfully.",
              );
            } catch (err) {
              setError(await assignmentError(err));
            } finally {
              lock.current = false;
              setBusy(false);
            }
          }}
        >
          <Field label="Your response">
            <textarea
              className="input min-h-52"
              disabled={!editable || busy}
              maxLength={20000}
              value={editable ? text : work?.submissionText || text}
              onChange={(e) => {
                setText(e.target.value);
                setDirty(true);
              }}
              placeholder="Write your homework response…"
            />
          </Field>
          {work?.attachments.length > 0 && (
            <div className="mt-5">
              <h3 className="mb-3 text-sm font-medium">Submitted files</h3>
              <AttachmentList files={work.attachments} submission />
              {editable && (
                <div className="mt-4 space-y-3">
                  {work.attachments.map((f) => (
                    <label
                      key={f.id}
                      className="flex items-center gap-3 text-xs text-slate-600"
                    >
                      <input
                        type="checkbox"
                        disabled={busy}
                        checked={keep.includes(f.id)}
                        onChange={(e) => {
                          setKeep(
                            e.target.checked
                              ? [...keep, f.id]
                              : keep.filter((id) => id !== f.id),
                          );
                          setDirty(true);
                        }}
                      />
                      Keep {f.originalFileName}
                    </label>
                  ))}
                  <p className="text-xs leading-6 text-slate-500">
                    Removed files remain in the revision history; original work
                    is never deleted.
                  </p>
                </div>
              )}
            </div>
          )}
          {editable ? (
            <>
              <div className="mt-6">
                <FilePicker
                  files={files}
                  onChange={(next) => {
                    setFiles(next);
                    setDirty(true);
                  }}
                  disabled={busy}
                  existingCount={keep.length}
                />
              </div>
              <UploadProgress value={progress} busy={busy} />
              <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
                <p className="text-xs text-slate-500">
                  {dirty
                    ? "Unsaved changes. Text is retained in this tab for retries."
                    : work
                      ? "Your saved work is shown above."
                      : "No submission has been saved yet."}
                </p>
                <Button type="submit" disabled={busy}>
                  {busy
                    ? "Submitting…"
                    : work
                      ? "Save Resubmission"
                      : "Submit Homework"}
                </Button>
              </div>
            </>
          ) : (
            <p className="mt-5 text-sm text-slate-500">
              {graded
                ? "Graded work is read-only."
                : a.status === "CLOSED"
                  ? "This assignment is closed. Your submitted work is preserved."
                  : "Revisions are not accepted after the deadline."}
            </p>
          )}
        </form>
        {work && (
          <Link
            className="button-ghost mt-5"
            to={`/assignments/submissions/${work.id}`}
          >
            View work & grade history
          </Link>
        )}
      </Card>
    </>
  );
}
