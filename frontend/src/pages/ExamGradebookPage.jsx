import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ErrorNotice,
  Input,
  Loading,
  Table,
  Toast,
  ConfirmDialog,
} from "../components/UI";
import {
  ExamHeading,
  ExamDenied,
  ExamError,
  staffRole,
  useExamLoad,
  examError,
} from "../components/ExamUI";
import {
  getGradebook,
  saveGradebook,
  publishResults,
} from "../services/resultService";
export default function ExamGradebookPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role);
  const state = useExamLoad(
    (signal) => getGradebook(id, signal),
    [id],
    allowed,
  );
  if (!allowed) return <ExamDenied />;
  if (state.loading) return <Loading label="Loading gradebook…" />;
  if (state.error) return <ExamError state={state} />;
  if (!state.data) return null;
  return (
    <Gradebook
      key={state.data.exam.version}
      initial={state.data}
      reload={state.reload}
    />
  );
}
function Gradebook({ initial, reload }) {
  const { exam, students } = initial;
  const [marks, setMarks] = useState(
      Object.fromEntries(
        students.map((s) => [
          s.studentId,
          { obtainedMarks: s.obtainedMarks ?? "", remarks: s.remarks || "" },
        ]),
      ),
    ),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [confirm, setConfirm] = useState(false);
  const readonly = exam.resultsPublished || exam.status !== "PUBLISHED",
    set = (id, key, value) =>
      setMarks((old) => ({ ...old, [id]: { ...old[id], [key]: value } }));
  return (
    <>
      <ExamHeading
        title={exam.title + " · Gradebook"}
        subtitle="Enter marks for every eligible student. Absence must be recorded explicitly as zero."
      >
        <Link className="button-secondary" to={`/exams/${exam.id}`}>
          Back to exam
        </Link>
        {exam.resultsPublished && (
          <Link className="button-primary" to={`/exams/${exam.id}/results`}>
            Results & corrections
          </Link>
        )}
      </ExamHeading>
      <ErrorNotice message={error} />
      <Card className="overflow-hidden">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (busy) return;
            setBusy(true);
            setError("");
            try {
              const entries = Object.entries(marks)
                .filter(([, v]) => v.obtainedMarks !== "")
                .map(([id, v]) => ({ studentId: Number(id), ...v }));
              if (!entries.length) {
                setError("Enter at least one mark.");
                return;
              }
              await saveGradebook(exam.id, {
                version: exam.version,
                results: entries,
              });
              setToast("Draft marks saved.");
              reload();
            } catch (err) {
              setError(await examError(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Table label="Offline exam gradebook" className="min-w-[720px]">
            <thead>
              <tr>
                <th>Student</th>
                <th>Admission number</th>
                <th>Marks (max {exam.totalMarks})</th>
                <th>Remarks</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.studentId}>
                  <td>{s.studentName}</td>
                  <td>{s.admissionNumber}</td>
                  <td>
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      max={exam.totalMarks}
                      disabled={readonly || busy}
                      aria-label={`Marks for ${s.studentName}`}
                      value={marks[s.studentId].obtainedMarks}
                      onChange={(e) =>
                        set(s.studentId, "obtainedMarks", e.target.value)
                      }
                    />
                  </td>
                  <td>
                    <Input
                      disabled={readonly || busy}
                      maxLength={1000}
                      aria-label={`Remarks for ${s.studentName}`}
                      value={marks[s.studentId].remarks}
                      onChange={(e) =>
                        set(s.studentId, "remarks", e.target.value)
                      }
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
          {!students.length && (
            <p className="p-6 text-sm text-slate-500">
              No eligible enrollments for this exam date.
            </p>
          )}
          <div className="flex flex-wrap items-center justify-end gap-3 border-t border-slate-100 p-5">
            {!readonly && (
              <>
                <Button
                  variant="secondary"
                  type="submit"
                  disabled={busy || !students.length}
                >
                  {busy ? "Saving…" : "Bulk Save Draft"}
                </Button>
                <Button disabled={busy} onClick={() => setConfirm(true)}>
                  Publish Results
                </Button>
              </>
            )}
            {readonly && (
              <p className="text-xs text-slate-500">
                {exam.resultsPublished
                  ? "Published marks are read-only. Use Results & corrections for audited changes."
                  : "Publish the exam first. Marks entry opens after the exam ends."}
              </p>
            )}
          </div>
        </form>
      </Card>
      {confirm && (
        <ConfirmDialog
          title="Publish offline results?"
          confirmLabel="Publish results"
          error={error}
          busy={busy}
          onClose={() => setConfirm(false)}
          onConfirm={async () => {
            setBusy(true);
            setError("");
            try {
              await publishResults(exam.id, exam.version);
              setConfirm(false);
              reload();
            } catch (err) {
              setError(await examError(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          Save your draft marks first. Every eligible student must have a
          persisted result before publication.
        </ConfirmDialog>
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
