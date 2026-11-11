import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import {
  getAttendanceOverview,
  getMyAttendance,
} from "../services/attendanceService";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Skeleton,
  dateLabel,
} from "./UI";
import { AttendanceBadge, defaultRange, percentage } from "./AttendanceUI";

export default function AttendanceDashboard() {
  const { user } = useAuth();
  const student = user.role === "STUDENT";
  const allowed = ["ADMIN", "TEACHER", "STUDENT"].includes(user.role);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [reload, setReload] = useState(0);
  const [range] = useState(defaultRange);
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    (student
      ? getMyAttendance({ ...range, size: 5 }, c.signal)
      : getAttendanceOverview(range, c.signal)
    )
      .then(({ data }) => {
        if (!c.signal.aborted) setData(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [student, allowed, range, reload]);
  if (!allowed) return null;
  return (
    <Card className="mb-6 overflow-hidden">
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 p-5">
        <div>
          <h2 className="text-sm font-semibold">
            {student
              ? "My attendance"
              : user.role === "TEACHER"
                ? "Assigned batch attendance"
                : "Attendance overview"}
          </h2>
          <p className="mt-1 text-[11px] text-slate-500">
            {range.fromDate} to {range.toDate} · Asia/Kolkata
          </p>
        </div>
        <Link className="text-xs font-medium text-indigo-600" to="/attendance">
          View attendance
        </Link>
      </div>
      <div className="p-5">
        <ErrorNotice message={error} />
        {error && (
          <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
            Retry attendance overview
          </Button>
        )}
        {loading ? (
          <Skeleton className="h-20 w-full" />
        ) : (
          !error &&
          data &&
          (student && !data.linked ? (
            <EmptyState
              compact
              title="Student profile not linked"
              description="Contact your administrator to link your student record."
            />
          ) : (
            <>
              <div className="mb-4 flex flex-wrap items-center gap-4">
                <p className="text-2xl font-semibold">
                  {percentage(data.totals.attendancePercentage)}
                </p>
                <Badge tone="indigo">
                  {data.totals.finalizedSessions} finalized classes
                </Badge>
                {!student && (
                  <Badge tone="warning">{data.pendingDrafts} drafts</Badge>
                )}
              </div>
              <p className="mb-4 text-xs text-slate-500">
                Present {data.totals.presentCount} · Late{" "}
                {data.totals.lateCount} · Absent {data.totals.absentCount} ·
                Excused {data.totals.excusedCount}
              </p>
              {!student && data.batches.content.length > 0 && (
                <div className="mb-4 flex flex-wrap gap-2">
                  {data.batches.content.map((b) => (
                    <div
                      className="max-w-full truncate rounded-md bg-slate-100 px-2 py-1 text-[11px] text-slate-600"
                      key={b.id}
                    >
                      {b.name}: {percentage(b.totals.attendancePercentage)}
                    </div>
                  ))}
                </div>
              )}
              <div className="space-y-3">
                {(student
                  ? data.history.content
                  : data.recentFinalized.content
                ).map((s) => (
                  <div
                    key={student ? s.sessionId : s.id}
                    className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3 text-xs"
                  >
                    <span>
                      {dateLabel(s.sessionDate)} · {s.batchName}
                    </span>
                    {student ? (
                      <AttendanceBadge status={s.status} />
                    ) : (
                      <Link
                        className="text-indigo-600"
                        to={`/attendance/sessions/${s.id}`}
                      >
                        View register
                      </Link>
                    )}
                  </div>
                ))}
              </div>
              {!data.totals.finalizedSessions && (
                <p className="text-xs text-slate-500">
                  No finalized attendance in this range. Drafts and days without
                  classes are not counted as absent.
                </p>
              )}
            </>
          ))
        )}
      </div>
    </Card>
  );
}
