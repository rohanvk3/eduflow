import { useAuth } from "../auth/AuthContext";
import { lazy, Suspense } from "react";
import { Link } from "react-router-dom";
import { Card, Loading } from "./UI";
const AssignmentActivity = lazy(() => import("./AssignmentActivity"));
import {
  AssignmentStats,
  AssignmentError,
  useAssignmentStats,
  staffRole,
  assignmentTime,
  pct,
} from "./AssignmentUI";
export default function AssignmentDashboard({ full = false }) {
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const state = useAssignmentStats(null, allowed);
  if (!allowed) return null;
  const d = state.data;
  return (
    <Card className="mb-6 p-5">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold">Assignments & homework</h2>
          <p className="mt-1 text-xs text-slate-500">
            Refreshes every 30 seconds, on focus, and after changes.
          </p>
        </div>
        <Link
          className="button-secondary"
          to={staff ? "/assignments" : "/my-assignments"}
        >
          {staff ? "Manage assignments" : "My homework"}
        </Link>
      </div>
      <AssignmentError state={state} />
      {!state.error && (
        <>
          <AssignmentStats state={state} student={!staff} />
          {d && (
            <>
              <p className="mb-5 text-xs text-slate-500">
                {staff ? `Created by you: ${d.createdByMe} · ` : ""}Grading
                progress: {pct(d.gradingPercentage)} · Average marks:{" "}
                {d.averageMarks ?? "No data"} · Average percentage:{" "}
                {pct(d.averagePercentage)}
              </p>
              <div className="grid gap-6 lg:grid-cols-2">
                <div>
                  <h3 className="mb-3 text-xs font-semibold">
                    Upcoming deadlines
                  </h3>
                  {!d.upcoming.length ? (
                    <p className="text-xs text-slate-500">
                      No upcoming assignments.
                    </p>
                  ) : (
                    <ul className="space-y-3">
                      {d.upcoming.map((a) => (
                        <li
                          key={a.id}
                          className="flex flex-wrap justify-between gap-3 text-xs"
                        >
                          <Link
                            className="font-medium text-indigo-600"
                            to={`/assignments/${a.id}`}
                          >
                            {a.title}
                          </Link>
                          <span className="text-slate-500">
                            {assignmentTime(a.dueDateTime)}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                <div>
                  <h3 className="mb-3 text-xs font-semibold">
                    {staff
                      ? "Recent submissions / grades"
                      : "Recent homework & feedback"}
                  </h3>
                  {!d.recentSubmissions.length ? (
                    <p className="text-xs text-slate-500">
                      No submitted work yet.
                    </p>
                  ) : (
                    <ul className="space-y-3">
                      {d.recentSubmissions.map((s) => (
                        <li
                          key={s.id}
                          className="rounded-lg bg-slate-50 p-3 text-xs"
                        >
                          <Link
                            className="font-medium text-indigo-600"
                            to={
                              staff
                                ? `/assignments/submissions/${s.id}`
                                : `/assignments/${s.assignmentId}/submit`
                            }
                          >
                            {s.assignmentTitle}
                            {staff ? " · " + s.studentName : ""}
                          </Link>
                          <p className="mt-1 text-slate-500">
                            {s.status} ·{" "}
                            {s.obtainedMarks == null
                              ? "Not graded"
                              : s.obtainedMarks + " / " + s.maxMarks}
                          </p>
                          {!staff && s.feedback && (
                            <p className="mt-2 line-clamp-2 text-slate-600">
                              {s.feedback}
                            </p>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
              {(d.activity || []).length > 0 && (
                <Suspense
                  fallback={<Loading label="Loading submission trends…" />}
                >
                  <AssignmentActivity rows={d.activity} />
                </Suspense>
              )}
              {staff && d.batches.length > 0 && (
                <div className="mt-6 border-t border-slate-100 pt-5">
                  <h3 className="mb-4 text-xs font-semibold">
                    Batch submission completion
                  </h3>
                  <div className="grid gap-4 sm:grid-cols-2">
                    {d.batches.slice(0, full ? 100 : 4).map((b) => (
                      <div key={b.batchId}>
                        <div className="mb-2 flex justify-between gap-3 text-xs">
                          <span>{b.batchName}</span>
                          <span>
                            {pct(b.completionPercentage)} · {b.submitted}/
                            {b.eligible}
                          </span>
                        </div>
                        <progress
                          className="w-full accent-indigo-600"
                          aria-label={b.batchName + " submission completion"}
                          max={100}
                          value={b.completionPercentage ?? 0}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}
    </Card>
  );
}
