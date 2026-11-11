import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ErrorNotice,
  Input,
  Loading,
  Modal,
  Toast,
} from "../components/UI";
import {
  ExamHeading,
  ExamBadge,
  ExamDenied,
  ExamError,
  Field,
  examTime,
  staffRole,
  useExamLoad,
  examError,
} from "../components/ExamUI";
import QuestionBuilder from "../components/QuestionBuilder";
import { getExam, publishExam, cancelExam } from "../services/examService";
import { publishResults } from "../services/resultService";
export default function ExamDetailsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const state = useExamLoad((signal) => getExam(id, signal), [id], allowed);
  const [action, setAction] = useState(""),
    [reason, setReason] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState("");
  if (!allowed) return <ExamDenied />;
  if (state.loading) return <Loading label="Loading exam…" />;
  if (state.error) return <ExamError state={state} />;
  if (!state.data) return null;
  const e = state.data;
  return (
    <>
      <ExamHeading
        title={e.title}
        subtitle={`${e.examCode} · ${e.courseName} · ${e.batchName}`}
      >
        <Link className="button-secondary" to="/exams">
          All exams
        </Link>
        {staff && e.status === "DRAFT" && (
          <>
            <Link className="button-secondary" to={`/exams/${id}/edit`}>
              Edit exam
            </Link>
            <Button onClick={() => setAction("publish")}>Publish exam</Button>
          </>
        )}
        {staff && e.status === "PUBLISHED" && (
          <Button onClick={() => setAction("results")}>Publish results</Button>
        )}
        {staff && ["DRAFT", "PUBLISHED"].includes(e.status) && (
          <Button variant="secondary" onClick={() => setAction("cancel")}>
            Cancel exam
          </Button>
        )}
      </ExamHeading>
      <Card className="mb-6 p-6">
        <div className="mb-5 flex flex-wrap gap-3">
          <ExamBadge status={e.status} />
          <span className="text-xs text-slate-500">
            {e.examType.replaceAll("_", " ")} · Results{" "}
            {e.resultsPublished ? "published" : "not published"}
          </span>
        </div>
        <p className="mb-5 whitespace-pre-wrap text-sm leading-7 text-slate-600">
          {e.description}
        </p>
        <dl className="grid gap-5 text-sm sm:grid-cols-2 lg:grid-cols-4">
          {[
            ["Starts (IST)", examTime(e.startDateTime)],
            ["Ends (IST)", examTime(e.endDateTime)],
            [
              "Duration",
              e.durationMinutes ? e.durationMinutes + " minutes" : "Offline",
            ],
            ["Marks", e.totalMarks + " total · " + e.passingMarks + " passing"],
          ].map(([label, value]) => (
            <div key={label}>
              <dt className="text-xs text-slate-500">{label}</dt>
              <dd className="mt-2 font-medium">{value}</dd>
            </div>
          ))}
        </dl>
        <div className="mt-6 flex flex-wrap gap-3">
          {staff && e.examType === "OFFLINE" && (
            <Link className="button-primary" to={`/exams/${id}/gradebook`}>
              Open gradebook
            </Link>
          )}
          {staff && e.resultsPublished && (
            <Link className="button-primary" to={`/exams/${id}/results`}>
              Results & rankings
            </Link>
          )}
          {!staff &&
            e.examType === "ONLINE_MCQ" &&
            e.status === "PUBLISHED" && (
              <Link className="button-primary" to={`/exams/${id}/take`}>
                Open online test
              </Link>
            )}
          {!staff && (
            <Link className="button-secondary" to={`/exams/${id}/result`}>
              My result
            </Link>
          )}
        </div>
      </Card>
      {staff && e.examType === "ONLINE_MCQ" && (
        <QuestionBuilder exam={e} onChange={state.reload} />
      )}
      {action && (
        <Modal
          title={
            action === "publish"
              ? "Publish exam?"
              : action === "results"
                ? "Publish results?"
                : "Cancel exam?"
          }
          busy={busy}
          onClose={() => {
            setAction("");
            setError("");
          }}
        >
          <form
            className="space-y-5 p-6"
            onSubmit={async (event) => {
              event.preventDefault();
              if (busy) return;
              setBusy(true);
              setError("");
              try {
                if (action === "publish") await publishExam(id, e.version);
                else if (action === "results")
                  await publishResults(id, e.version);
                else await cancelExam(id, { version: e.version, reason });
                setToast(
                  action === "results"
                    ? "Results published."
                    : action === "publish"
                      ? "Exam published."
                      : "Exam cancelled.",
                );
                setAction("");
                state.reload();
              } catch (err) {
                setError(await examError(err));
              } finally {
                setBusy(false);
              }
            }}
          >
            <ErrorNotice message={error} />
            <p className="text-sm leading-7 text-slate-600">
              {action === "publish"
                ? "Published rules and questions cannot be changed. Ensure marks and timings are correct."
                : action === "results"
                  ? "Result publication is separate from exam publication. The exam must have ended and grading must be complete. Online non-attempts are excluded; offline absence requires an explicit zero."
                  : "An exam with attempts or results cannot be cancelled."}
            </p>
            {action === "cancel" && (
              <Field label="Cancellation reason *">
                <Input
                  required
                  maxLength={1000}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                />
              </Field>
            )}
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                disabled={busy}
                onClick={() => setAction("")}
              >
                Back
              </Button>
              <Button type="submit" disabled={busy}>
                {busy ? "Working…" : "Confirm"}
              </Button>
            </div>
          </form>
        </Modal>
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
