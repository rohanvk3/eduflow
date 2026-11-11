import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";
import { Card, EmptyState } from "./UI";
function Chart({ title, rows, label }) {
  return (
    <Card className="min-w-0 p-5">
      <h2 className="mb-4 text-sm font-semibold">{title}</h2>
      {rows.length ? (
        <div className="h-64" role="img" aria-label={label}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={rows}>
              <CartesianGrid vertical={false} stroke="var(--color-slate-100)" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
              />
              <YAxis
                domain={[0, 100]}
                tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface)",
                  color: "var(--color-slate-800)",
                  border: "1px solid var(--color-slate-200)",
                  borderRadius: 10,
                }}
              />
              <Line
                type="monotone"
                dataKey="percentage"
                name="Percentage"
                stroke="#818cf8"
                strokeWidth={2}
                connectNulls={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <EmptyState compact title="No published data in this range" />
      )}
    </Card>
  );
}
export default function ParentCharts({
  progress,
  assignments = false,
  resultsOnly = false,
}) {
  const attendance = progress.attendanceTrend.map((r) => ({
      date: r.date,
      percentage:
        r.totals.attendancePercentage == null
          ? null
          : Number(r.totals.attendancePercentage),
    })),
    exams = progress.examTrend
      .slice()
      .sort((a, b) => a.examDate.localeCompare(b.examDate))
      .map((r) => ({
        date: new Date(r.examDate).toLocaleDateString("en-IN", {
          timeZone: "Asia/Kolkata",
        }),
        percentage: r.percentage == null ? null : Number(r.percentage),
      }));
  let done = 0;
  const completion = progress.assignmentTrend.map((r, i) => {
    if (r.submissionStatus !== "PENDING") done++;
    return {
      date: new Date(r.dueDateTime).toLocaleDateString("en-IN", {
        timeZone: "Asia/Kolkata",
      }),
      percentage: Math.round((done / (i + 1)) * 10000) / 100,
    };
  });
  if (resultsOnly)
    return (
      <div className="mb-5">
        <Chart
          title="Published exam scores on this page"
          rows={exams}
          label="Child published exam performance"
        />
        <p className="mt-2 text-xs text-slate-500">
          Chart reflects the current result page and filters.
        </p>
      </div>
    );
  return (
    <>
      <div className="mb-5 grid gap-5 lg:grid-cols-2">
        <Chart
          title="Attendance trend"
          rows={attendance}
          label="Child attendance trend"
        />
        <Chart
          title="Published exam performance"
          rows={exams}
          label="Child published exam performance"
        />
      </div>
      {assignments && (
        <>
          <Chart
            title="Assignment completion by due date"
            rows={completion}
            label="Child assignment completion trend"
          />
          <p className="my-3 text-xs text-slate-500">
            Trends show up to 100 exams/assignments in the selected range.
            Overall and course totals include all matching records. Excused-only
            attendance days have no percentage.
          </p>
          <Card className="mt-5 p-5">
            <h2 className="mb-4 text-sm font-semibold">Grading trend</h2>
            {progress.assignmentTrend
              .filter((a) => a.obtainedMarks != null)
              .map((a) => (
                <p
                  key={a.id}
                  className="mb-3 flex justify-between gap-3 text-xs"
                >
                  <span>{a.title}</span>
                  <strong>
                    {a.obtainedMarks} / {a.maxMarks}
                  </strong>
                </p>
              ))}
            {!progress.assignmentTrend.some((a) => a.obtainedMarks != null) && (
              <p className="text-xs text-slate-500">
                No published assignment grades in this range.
              </p>
            )}
          </Card>
        </>
      )}
    </>
  );
}
