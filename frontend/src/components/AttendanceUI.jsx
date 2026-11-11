import { Link } from "react-router-dom";
import {
  CalendarCheck,
  Users,
  Clock3,
  UserX,
  CheckCircle2,
} from "lucide-react";
import {
  Badge,
  Card,
  EmptyState,
  Input,
  Table,
  Skeleton,
  dateLabel,
} from "./UI";

export function localDate(date = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}
export function defaultRange() {
  const end = localDate();
  const d = new Date(`${end}T12:00:00+05:30`);
  d.setDate(d.getDate() - 29);
  return { fromDate: localDate(d), toDate: end };
}
export const percentage = (value) =>
  value == null ? "No data" : `${Number(value).toFixed(2)}%`;
export function AttendanceBadge({ status }) {
  const tone = {
    PRESENT: "success",
    LATE: "warning",
    ABSENT: "neutral",
    EXCUSED: "indigo",
    DRAFT: "warning",
    FINALIZED: "success",
  };
  return (
    <Badge tone={tone[status] || "neutral"}>{status || "NOT_MARKED"}</Badge>
  );
}
export function DateRange({ value, onChange }) {
  return (
    <div className="flex flex-wrap gap-3">
      <label className="field-label">
        From date
        <Input
          className="mt-2"
          type="date"
          value={value.fromDate}
          onChange={(e) => onChange({ ...value, fromDate: e.target.value })}
        />
      </label>
      <label className="field-label">
        To date
        <Input
          className="mt-2"
          type="date"
          value={value.toDate}
          onChange={(e) => onChange({ ...value, toDate: e.target.value })}
        />
      </label>
    </div>
  );
}
export function AttendanceStats({ totals, loading }) {
  const items = [
    ["Attendance", percentage(totals?.attendancePercentage), CalendarCheck],
    [
      "Eligible finalized records",
      totals?.totalEligibleFinalizedSessions ?? "—",
      Users,
    ],
    [
      "Attended (present + late)",
      totals ? totals.presentCount + totals.lateCount : "—",
      CheckCircle2,
    ],
    ["Absent", totals?.absentCount ?? "—", UserX],
    ["Excused", totals?.excusedCount ?? "—", Clock3],
  ];
  return (
    <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-5">
      {items.map(([label, value, Icon]) => (
        <Card className="p-4" key={label}>
          <Icon size={17} className="mb-4 text-indigo-500" />
          <p className="text-[11px] text-slate-500">{label}</p>
          {loading ? (
            <Skeleton className="mt-2 h-7 w-20" />
          ) : (
            <p className="mt-2 text-xl font-semibold tabular-nums">{value}</p>
          )}
        </Card>
      ))}
    </div>
  );
}
export function GroupAttendanceTable({ rows, kind = "students" }) {
  return !rows?.length ? (
    <EmptyState
      compact
      title={`No ${kind} attendance data`}
      description="Finalized attendance will appear here."
    />
  ) : (
    <Table label={`${kind} attendance summary`} className="min-w-[700px]">
      <thead>
        <tr>
          <th>{kind === "students" ? "Student" : "Batch"}</th>
          <th>Eligible sessions</th>
          <th>Present</th>
          <th>Late</th>
          <th>Absent</th>
          <th>Excused</th>
          <th>Attendance</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              <p className="font-medium">{r.name}</p>
              <p
                className="mt-1 max-w-44 truncate font-mono text-[10px] text-slate-500"
                title={r.code}
              >
                {r.code}
              </p>
            </td>
            <td>{r.totals.totalEligibleFinalizedSessions}</td>
            <td>{r.totals.presentCount}</td>
            <td>{r.totals.lateCount}</td>
            <td>{r.totals.absentCount}</td>
            <td>{r.totals.excusedCount}</td>
            <td>{percentage(r.totals.attendancePercentage)}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
export function SessionTable({ sessions }) {
  return (
    <Table label="Attendance sessions" className="min-w-[800px]">
      <thead>
        <tr>
          <th>Date / time</th>
          <th>Batch</th>
          <th>Topic</th>
          <th>Status</th>
          <th>Register</th>
          <th>Marked by</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        {sessions.map((s) => (
          <tr key={s.id}>
            <td className="whitespace-nowrap text-xs">
              {dateLabel(s.sessionDate)}
              <p className="mt-1 text-slate-500">
                {s.startTime
                  ? `${s.startTime.slice(0, 5)}–${s.endTime.slice(0, 5)}`
                  : "Untimed class"}
              </p>
            </td>
            <td>
              <p className="font-medium">{s.batchName}</p>
              <p className="mt-1 text-xs text-slate-500">{s.courseName}</p>
            </td>
            <td className="max-w-52 break-words text-xs">{s.topic || "—"}</td>
            <td>
              <AttendanceBadge status={s.status} />
            </td>
            <td className="text-xs">
              {s.markedCount} / {s.eligibleCount} marked
            </td>
            <td className="text-xs">{s.markedBy}</td>
            <td>
              <Link
                className="button-secondary"
                to={`/attendance/sessions/${s.id}`}
              >
                {s.status === "DRAFT" ? "Mark attendance" : "View register"}
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
