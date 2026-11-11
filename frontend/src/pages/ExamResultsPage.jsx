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
  Pagination,
  Table,
} from "../components/UI";
import {
  ExamHeading,
  ExamDenied,
  ExamError,
  Field,
  ResultTable,
  staffRole,
  useExamLoad,
  examError,
  pct,
} from "../components/ExamUI";
import {
  getExamResults,
  getLeaderboard,
  correctResult,
  getCorrections,
} from "../services/resultService";
export default function ExamResultsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role);
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [target, setTarget] = useState(null),
    [auditTarget, setAuditTarget] = useState(null);
  const state = useExamLoad(
    (signal) => getExamResults(id, { page, size }, signal),
    [id, page, size],
    allowed,
  );
  const ranks = useExamLoad(
    (signal) => getLeaderboard(id, { page, size }, signal),
    [id, page, size],
    allowed,
  );
  if (!allowed) return <ExamDenied />;
  if (state.loading) return <Loading label="Loading results…" />;
  if (state.error) return <ExamError state={state} />;
  if (!state.data) return null;
  const data = state.data;
  return (
    <>
      <ExamHeading
        title={data.exam.title + " · Results"}
        subtitle="Published results and competition ranks. Equal marks share a rank."
      >
        <Link className="button-secondary" to={`/exams/${id}`}>
          Back to exam
        </Link>
      </ExamHeading>
      <Card className="mb-6 grid grid-cols-2 gap-5 p-5 sm:grid-cols-4">
        {[
          ["Evaluated", data.summary.count],
          ["Batch average marks", data.summary.average ?? "No data"],
          [
            "Highest / Lowest",
            (data.summary.highest ?? "—") +
              " / " +
              (data.summary.lowest ?? "—"),
          ],
          ["Pass rate", pct(data.summary.passRate)],
        ].map(([label, v]) => (
          <div key={label}>
            <p className="text-xs text-slate-500">{label}</p>
            <p className="mt-2 text-xl font-semibold">{v}</p>
          </div>
        ))}
      </Card>
      <Card className="mb-6 overflow-hidden">
        <ResultTable rows={data.results.content} />
        {data.exam.examType === "OFFLINE" && (
          <div className="flex flex-wrap gap-3 border-t border-slate-100 p-5">
            {data.results.content.map((r) => (
              <div
                key={r.id}
                className="rounded-lg border border-slate-200 p-3"
              >
                <p className="mb-2 text-xs font-medium">{r.studentName}</p>
                <Button variant="ghost" onClick={() => setTarget(r)}>
                  Correct marks
                </Button>
                <Button variant="ghost" onClick={() => setAuditTarget(r)}>
                  Audit history
                </Button>
              </div>
            ))}
          </div>
        )}
        <Pagination
          noun="results"
          page={page}
          size={size}
          total={data.results.totalElements}
          totalPages={data.results.totalPages}
          onPageChange={setPage}
          onSizeChange={(v) => {
            setSize(v);
            setPage(0);
          }}
        />
      </Card>
      <Card className="overflow-hidden">
        <h2 className="p-5 text-sm font-semibold">Leaderboard · staff only</h2>
        <ExamError state={ranks} />
        <Table label="Exam leaderboard">
          <thead>
            <tr>
              <th>Rank</th>
              <th>Student</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {ranks.data?.content.map((r) => (
              <tr key={r.result.id}>
                <td>#{r.rank}</td>
                <td>{r.result.studentName}</td>
                <td>{r.result.obtainedMarks}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Card>
      {target && (
        <CorrectionForm
          exam={data.exam}
          result={target}
          onClose={() => setTarget(null)}
          onSuccess={() => {
            setTarget(null);
            state.reload();
            ranks.reload();
          }}
        />
      )}
      {auditTarget && (
        <AuditHistory
          examId={id}
          result={auditTarget}
          onClose={() => setAuditTarget(null)}
        />
      )}
    </>
  );
}
function CorrectionForm({ exam, result, onClose, onSuccess }) {
  const [marks, setMarks] = useState(result.obtainedMarks),
    [reason, setReason] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Modal
      title="Correct published marks"
      description={result.studentName}
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            await correctResult(exam.id, result.id, {
              version: exam.version,
              obtainedMarks: marks,
              reason,
            });
            onSuccess();
          } catch (err) {
            setError(await examError(err));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <Field label="Corrected marks *">
          <Input
            required
            type="number"
            min="0"
            max={exam.totalMarks}
            step="0.01"
            value={marks}
            onChange={(e) => setMarks(e.target.value)}
          />
        </Field>
        <Field label="Correction reason *">
          <textarea
            className="input min-h-24"
            required
            maxLength={1000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
        <p className="text-xs text-slate-500">
          Previous marks, new marks, reason, actor, and time are permanently
          recorded.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            Save audited correction
          </Button>
        </div>
      </form>
    </Modal>
  );
}
function AuditHistory({ examId, result, onClose }) {
  const state = useExamLoad(
    (signal) => getCorrections(examId, result.id, signal),
    [examId, result.id],
  );
  return (
    <Modal
      title="Correction audit history"
      description={result.studentName}
      onClose={onClose}
    >
      <div className="space-y-4 p-6">
        <ExamError state={state} />
        {state.loading ? (
          <Loading label="Loading audit…" />
        ) : !state.data?.length ? (
          <p className="text-sm text-slate-500">No corrections recorded.</p>
        ) : (
          state.data.map((a) => (
            <div
              key={a.id}
              className="rounded-lg border border-slate-200 p-4 text-sm"
            >
              <p className="font-medium">
                {a.previousMarks} → {a.newMarks}
              </p>
              <p className="mt-2 whitespace-pre-wrap text-slate-600">
                {a.reason}
              </p>
              <p className="mt-2 text-xs text-slate-500">
                {a.correctedBy} · {a.correctedAt}
              </p>
            </div>
          ))
        )}
      </div>
    </Modal>
  );
}
