import { lazy, Suspense, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Badge, Card, EmptyState, Loading, Pagination } from "../components/UI";
import {
  ExamHeading,
  ExamDenied,
  ExamError,
  ResultTable,
  useExamLoad,
  pct,
  examTime,
} from "../components/ExamUI";
import { getMyResults, getOwnResult } from "../services/resultService";
const ExamTrend = lazy(() => import("../components/ExamTrend"));
export default function MyResultsPage({ detail = false }) {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = user.role === "STUDENT";
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const state = useExamLoad(
    (signal) =>
      detail ? getOwnResult(id, signal) : getMyResults({ page, size }, signal),
    [detail, id, page, size],
    allowed,
  );
  if (!allowed) return <ExamDenied />;
  if (state.loading) return <Loading label="Loading your results…" />;
  if (state.error) return <ExamError state={state} />;
  if (!state.data) return null;
  const d = state.data;
  if (detail)
    return (
      <>
        <ExamHeading
          title={d.result?.examTitle || "Exam Result"}
          subtitle="Your result and authorized answer review."
        >
          <Link className="button-secondary" to="/my-results">
            All my results
          </Link>
        </ExamHeading>
        {!d.published ? (
          <Card>
            <EmptyState
              title="Results pending publication"
              description="Your score and answer key will be visible after institute publication."
            />
          </Card>
        ) : !d.result ? (
          <Card>
            <EmptyState
              title="No result for this exam"
              description="You do not have an evaluated attempt or recorded offline result."
            />
          </Card>
        ) : (
          <>
            <Card className="mb-6 p-6">
              <p className="text-sm text-slate-500">
                {d.result.courseName} · {d.result.batchName} ·{" "}
                {examTime(d.result.examDate)}
              </p>
              <p className="my-5 text-3xl font-semibold">
                {d.result.obtainedMarks}{" "}
                <span className="text-lg text-slate-400">
                  / {d.result.totalMarks}
                </span>
              </p>
              <div className="flex flex-wrap gap-4">
                <Badge tone={d.result.passed ? "success" : "warning"}>
                  {d.result.passed ? "Passed" : "Not passed"}
                </Badge>
                <span className="text-sm">
                  {pct(d.result.percentage)} · Grade {d.result.grade}
                </span>
              </div>
              <p className="mt-5 text-sm text-slate-500">{d.result.remarks}</p>
            </Card>
            {d.review.map((q, i) => (
              <Card key={q.questionId} className="mb-4 p-5">
                <h2 className="whitespace-pre-wrap text-sm font-semibold">
                  {i + 1}. {q.questionText}
                </h2>
                <p className="mt-2 text-xs text-slate-500">
                  {q.awardedMarks} / {q.marks} marks ·{" "}
                  {q.selectedOptionId == null ? "Unanswered" : "Answered"}
                </p>
                <ul className="mt-4 space-y-2">
                  {q.options.map((o) => (
                    <li
                      key={o.id}
                      className={`rounded-lg border p-3 text-sm ${o.id === q.correctOptionId ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-slate-200"}`}
                    >
                      {o.optionText}
                      {o.id === q.correctOptionId && " · Correct answer"}
                      {o.id === q.selectedOptionId && " · Your selection"}
                    </li>
                  ))}
                </ul>
              </Card>
            ))}
          </>
        )}
      </>
    );
  return (
    <>
      <ExamHeading
        title="My Results"
        subtitle="Your published scores, grades, and progress."
      />
      <Suspense fallback={<Loading />}>
        <ExamTrend rows={d.content} />
      </Suspense>
      <Card className="overflow-hidden">
        <ResultTable rows={d.content} own />
        <Pagination
          noun="results"
          page={page}
          size={size}
          total={d.totalElements}
          totalPages={d.totalPages}
          onPageChange={setPage}
          onSizeChange={(v) => {
            setSize(v);
            setPage(0);
          }}
        />
      </Card>
      <p className="mt-4 text-xs text-slate-500">
        The trend shows the results on this page. Pass/fail uses each exam's
        passing marks independently of its grade letter.
      </p>
    </>
  );
}
