import { lazy, Suspense, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { Card, Loading, Table } from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ExamHeading,
  ExamDenied,
  ExamError,
  staffRole,
  useExamLoad,
  pct,
} from "../components/ExamUI";
import { getExamAnalytics } from "../services/examService";
const ExamTrend = lazy(() => import("../components/ExamTrend"));
const Distribution = lazy(() => import("../components/ExamDistribution"));
export default function ExamAnalyticsPage() {
  const { user } = useAuth(),
    allowed = staffRole(user.role);
  const [batch, setBatch] = useState(""),
    [student, setStudent] = useState("");
  const state = useExamLoad(
    (signal) =>
      getExamAnalytics(
        { batchId: batch || undefined, studentId: student || undefined },
        signal,
      ),
    [batch, student],
    allowed,
  );
  if (!allowed) return <ExamDenied />;
  return (
    <>
      <ExamHeading
        title="Exam Analytics"
        subtitle="Performance from published results in your authorized batches."
      />
      <Card className="mb-6 grid gap-5 p-5 sm:grid-cols-2">
        <RecordSelect
          label="Batch"
          endpoint="/batches"
          status=""
          required={false}
          value={batch}
          onChange={(v) => {
            setBatch(v);
            setStudent("");
          }}
        />
        {batch && (
          <RecordSelect
            key={batch}
            label="Student progress"
            valueKey="studentId"
            endpoint={`/batches/${batch}/enrollments`}
            status=""
            required={false}
            value={student}
            onChange={setStudent}
          />
        )}
      </Card>
      <ExamError state={state} />
      {state.loading ? (
        <Loading label="Calculating analytics…" />
      ) : (
        !state.error && state.data && <Analytics data={state.data} />
      )}
    </>
  );
}
function Analytics({ data: d }) {
  return (
    <>
      <div className="mb-6 grid grid-cols-2 gap-4 xl:grid-cols-4">
        {[
          ["Total exams", d.totalExams],
          ["Published exams", d.publishedExams],
          ["Completed exams", d.completedExams],
          ["Upcoming / available", d.upcomingExams],
          ["Awaiting results", d.awaitingResults],
          ["Average percentage", pct(d.scores.average)],
          ["Pass rate", pct(d.scores.passRate)],
          ["Published results", d.scores.count],
        ].map(([label, v]) => (
          <Card key={label} className="p-5">
            <p className="text-xs text-slate-500">{label}</p>
            <p className="mt-3 text-2xl font-semibold">{v}</p>
          </Card>
        ))}
      </div>
      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        {[
          ["Batch performance", d.batches],
          ["Course performance", d.courses],
        ].map(([label, rows]) => (
          <Card key={label} className="overflow-hidden">
            <h2 className="p-5 text-sm font-semibold">{label}</h2>
            <Table label={label}>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Results</th>
                  <th>Average %</th>
                  <th>Pass rate</th>
                </tr>
              </thead>
              <tbody>
                {rows.length ? (
                  rows.map((r, i) => (
                    <tr key={i}>
                      <td>{r.label}</td>
                      <td>{r.count}</td>
                      <td>{pct(r.average)}</td>
                      <td>{pct(r.passRate)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4}>No published result data.</td>
                  </tr>
                )}
              </tbody>
            </Table>
          </Card>
        ))}
      </div>
      <Suspense fallback={<Loading />}>
        <Distribution rows={d.distribution} />
        <ExamTrend rows={d.trend} />
      </Suspense>
      <p className="text-xs leading-6 text-slate-500">
        Scores are normalized to percentages across exams. Only published
        results enter performance averages. Non-attempts are excluded from
        online scores. Charts show up to 100 groups / recent results. For
        individual progress, filter a student in the selected batch.
      </p>
    </>
  );
}
