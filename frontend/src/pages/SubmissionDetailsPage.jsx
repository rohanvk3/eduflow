import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Badge,
  Button,
  Card,
  ErrorNotice,
  Input,
  Loading,
  Toast,
} from "../components/UI";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  AttachmentList,
  Field,
  staffRole,
  useAssignmentLoad,
  assignmentError,
  assignmentTime,
} from "../components/AssignmentUI";
import {
  getSubmission,
  getSubmissionHistory,
  gradeSubmission,
} from "../services/submissionService";
export default function SubmissionDetailsPage() {
  const { submissionId } = useParams(),
    { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const [toast, setToast] = useState("");
  const state = useAssignmentLoad(
      (signal) => getSubmission(submissionId, signal),
      [submissionId],
      allowed,
    ),
    history = useAssignmentLoad(
      (signal) => getSubmissionHistory(submissionId, signal),
      [submissionId],
      allowed,
    );
  if (!allowed) return <AssignmentDenied />;
  if (state.loading) return <Loading label="Loading submitted work…" />;
  if (state.error) return <AssignmentError state={state} />;
  if (!state.data) return null;
  const s = state.data;
  return (
    <>
      <AssignmentHeading
        title={s.assignmentTitle}
        subtitle={`${s.studentName} · ${s.admissionNumber} · ${s.batchName}`}
      >
        <Link
          className="button-secondary"
          to={
            staff
              ? `/assignments/${s.assignmentId}/submissions`
              : `/assignments/${s.assignmentId}/submit`
          }
        >
          Back
        </Link>
      </AssignmentHeading>
      <Card className="mb-6 p-6">
        <div className="flex flex-wrap gap-3">
          <Badge tone={s.status === "GRADED" ? "success" : "indigo"}>
            {s.status}
            {s.late && s.status === "GRADED" ? " · LATE" : ""}
          </Badge>
          <p className="text-xs text-slate-500">
            Submitted {assignmentTime(s.submittedAt)}
          </p>
        </div>
        <h2 className="mb-4 mt-6 text-sm font-semibold">Submitted response</h2>
        <p className="whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
          {s.submissionText || "File submission"}
        </p>
        <div className="mt-6">
          <AttachmentList files={s.attachments} submission />
        </div>
      </Card>
      {staff ? (
        <GradeForm
          key={s.version}
          submission={s}
          onSaved={() => {
            setToast("Grade and feedback published to the student.");
            state.reload();
            history.reload();
          }}
        />
      ) : (
        s.status === "GRADED" && (
          <Card className="mb-6 p-6">
            <h2 className="text-sm font-semibold">Your grade & feedback</h2>
            <p className="my-4 text-2xl font-semibold">
              {s.obtainedMarks} / {s.maxMarks}
            </p>
            <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">
              {s.feedback}
            </p>
            <p className="mt-4 text-xs text-slate-500">
              {s.gradedBy} · {assignmentTime(s.gradedAt)}
            </p>
          </Card>
        )
      )}
      <Card className="p-6">
        <h2 className="mb-5 text-sm font-semibold">
          Revision & grading history
        </h2>
        <AssignmentError state={history} />
        {history.loading ? (
          <Loading label="Loading history…" />
        ) : (
          history.data && (
            <>
              <h3 className="mb-3 text-xs font-medium text-slate-500">
                Previous submitted versions
              </h3>
              {!history.data.revisions.length && (
                <p className="mb-5 text-sm text-slate-500">No resubmissions.</p>
              )}
              {history.data.revisions.map((r) => (
                <details
                  key={r.id}
                  className="mb-4 rounded-lg border border-slate-200 p-4"
                >
                  <summary className="cursor-pointer text-sm">
                    Version {r.revisionNumber} · {assignmentTime(r.submittedAt)}
                  </summary>
                  <p className="my-4 whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
                    {r.submissionText || "File submission"}
                  </p>
                  <AttachmentList files={r.attachments} submission />
                </details>
              ))}
              <h3 className="mb-3 mt-6 text-xs font-medium text-slate-500">
                Published grades & corrections
              </h3>
              {!history.data.grades.length && (
                <p className="text-sm text-slate-500">Not graded yet.</p>
              )}
              {history.data.grades.map((g) => (
                <div
                  key={g.id}
                  className="mb-3 rounded-lg border border-slate-200 p-4 text-sm"
                >
                  <p className="font-medium">
                    {g.previousMarks == null
                      ? "Initial grade"
                      : g.previousMarks + " →"}{" "}
                    {g.newMarks} / {s.maxMarks}
                  </p>
                  <p className="mt-2 whitespace-pre-wrap text-slate-600">
                    {g.newFeedback}
                  </p>
                  <p className="mt-3 text-xs text-slate-500">
                    {g.reason} · {g.gradedBy} · {assignmentTime(g.gradedAt)}
                  </p>
                </div>
              ))}
            </>
          )
        )}
      </Card>
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
function GradeForm({ submission: s, onSaved }) {
  const [marks, setMarks] = useState(s.obtainedMarks ?? ""),
    [feedback, setFeedback] = useState(s.feedback || ""),
    [reason, setReason] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Card className="mb-6 p-6">
      <h2 className="mb-5 text-sm font-semibold">
        {s.status === "GRADED" ? "Correct published grade" : "Grade submission"}
      </h2>
      <form
        className="space-y-5"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            await gradeSubmission(s.id, {
              version: s.version,
              obtainedMarks: marks,
              feedback,
              reason,
            });
            onSaved();
          } catch (err) {
            setError(await assignmentError(err));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <fieldset disabled={busy} className="grid gap-5 md:grid-cols-2">
          <Field label={`Marks (maximum ${s.maxMarks}) *`}>
            <Input
              type="number"
              required
              min="0"
              max={s.maxMarks}
              step="0.01"
              value={marks}
              onChange={(e) => setMarks(e.target.value)}
            />
          </Field>
          {s.status === "GRADED" && (
            <Field label="Correction reason *">
              <Input
                required
                maxLength={1000}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </Field>
          )}
          <div className="md:col-span-2">
            <Field label="Feedback">
              <textarea
                className="input min-h-32"
                maxLength={5000}
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
              />
            </Field>
          </div>
        </fieldset>
        <p className="text-xs leading-6 text-slate-500">
          Grades and feedback are immediately visible to the submission owner.
          Every grade is audited; corrections require a reason.
        </p>
        <div className="flex justify-end">
          <Button type="submit" disabled={busy}>
            {busy
              ? "Saving…"
              : s.status === "GRADED"
                ? "Save audited correction"
                : "Publish grade & feedback"}
          </Button>
        </div>
      </form>
    </Card>
  );
}
