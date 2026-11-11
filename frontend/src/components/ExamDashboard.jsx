import { Link } from "react-router-dom";
import { lazy, Suspense } from "react";
import { useAuth } from "../auth/AuthContext";
import { Card, Loading } from "./UI";
import { ExamError, useExamLoad, pct, staffRole, examTime } from "./ExamUI";
import { getExamAnalytics } from "../services/examService";
const ExamTrend = lazy(() => import("./ExamTrend"));
export default function ExamDashboard() {
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const state = useExamLoad(
    (signal) => getExamAnalytics({}, signal),
    [user.role],
    allowed,
  );
  if (!allowed) return null;
  const d = state.data;
  return (
    <Card className="mb-6 p-5">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold">Exams & performance</h2>
          <p className="mt-1 text-xs text-slate-500">
            Your authorized batches and published results
          </p>
        </div>
        <Link
          className="button-secondary"
          to={staff ? "/exams/analytics" : "/my-results"}
        >
          {staff ? "View analytics" : "My results"}
        </Link>
      </div>
      <ExamError state={state} />
      {!state.error && (
        <>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {[
              ["Total exams", d?.totalExams],
              ["Available / upcoming", d?.upcomingExams],
              ["Completed", d?.completedExams],
              [
                staff ? "Awaiting results" : "Average percentage",
                staff ? d?.awaitingResults : pct(d?.scores?.average),
              ],
            ].map(([label, v]) => (
              <div key={label}>
                <p className="text-xs text-slate-500">{label}</p>
                <p className="mt-2 text-xl font-semibold">{v ?? "—"}</p>
              </div>
            ))}
          </div>
          {d?.scores && (
            <p className="mt-5 text-xs text-slate-500">
              Published average:{" "}
              <strong className="text-slate-700">
                {pct(d.scores.average)}
              </strong>{" "}
              · Pass rate:{" "}
              <strong className="text-slate-700">
                {pct(d.scores.passRate)}
              </strong>
            </p>
          )}
          {d?.upcoming?.length > 0 && (
            <ul className="mt-5 space-y-3 border-t border-slate-100 pt-4">
              {d.upcoming.map((e) => (
                <li
                  key={e.id}
                  className="flex flex-wrap justify-between gap-3 text-xs"
                >
                  <Link
                    className="font-medium text-indigo-600"
                    to={`/exams/${e.id}`}
                  >
                    {e.title}
                  </Link>
                  <span className="text-slate-500">
                    {examTime(e.startDateTime)}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {d?.trend?.length > 0 && (
            <div className="mt-5 border-t border-slate-100 pt-4">
              <h3 className="mb-3 text-xs font-medium">
                Recent published results
              </h3>
              <ul className="space-y-3">
                {d.trend.slice(0, 3).map((r) => (
                  <li
                    key={r.id}
                    className="flex flex-wrap justify-between gap-3 text-xs"
                  >
                    <Link
                      className="font-medium text-indigo-600"
                      to={`/exams/${r.examId}/${staff ? "results" : "result"}`}
                    >
                      {r.examTitle}
                      {staff ? " · " + r.studentName : ""}
                    </Link>
                    <span>
                      {pct(r.percentage)} · {r.grade}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {!staff && d?.trend?.length > 0 && (
            <div className="mt-5">
              <Suspense
                fallback={<Loading label="Loading performance trend…" />}
              >
                <ExamTrend rows={d.trend.slice(0, 10)} />
              </Suspense>
            </div>
          )}
          {staff && d?.needsGrading?.length > 0 && (
            <div className="mt-4 border-t border-slate-100 pt-4">
              <p className="mb-2 text-xs font-medium">
                Awaiting grading / result publication
              </p>
              {d.needsGrading.map((e) => (
                <Link
                  key={e.id}
                  className="mr-4 inline-block text-xs text-indigo-600"
                  to={`/exams/${e.id}`}
                >
                  {e.title}
                </Link>
              ))}
            </div>
          )}
        </>
      )}
    </Card>
  );
}
