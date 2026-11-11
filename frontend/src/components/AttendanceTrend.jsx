import {
  LineChart,
  Line,
  ResponsiveContainer,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { Card, EmptyState } from "./UI";
import { percentage } from "./AttendanceUI";
export default function AttendanceTrend({ data }) {
  return (
    <Card className="mb-6 p-5">
      <h2 className="mb-4 text-sm font-semibold">Attendance trend</h2>
      {!data?.length ? (
        <EmptyState
          compact
          title="No finalized attendance in this range"
          description="Draft and unmarked entries do not contribute to the percentage."
        />
      ) : (
        <>
          <div
            className="h-56 min-w-0"
            role="img"
            aria-label="Finalized attendance percentage by date"
          >
            <ResponsiveContainer
              width="100%"
              height="100%"
              minWidth={0}
              initialDimension={{ width: 600, height: 224 }}
            >
              <LineChart
                data={data.map((d) => ({
                  date: d.date,
                  percentage: d.totals.attendancePercentage,
                }))}
                margin={{ left: 0, right: 16, top: 10, bottom: 5 }}
                accessibilityLayer
              >
                <CartesianGrid
                  vertical={false}
                  stroke="var(--color-slate-100)"
                />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  domain={[0, 100]}
                  tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
                  tickLine={false}
                  axisLine={false}
                  width={35}
                />
                <Tooltip
                  formatter={(v) => percentage(v)}
                  contentStyle={{
                    backgroundColor: "var(--color-surface)",
                    borderColor: "var(--color-slate-200)",
                    color: "var(--color-slate-800)",
                    borderRadius: 10,
                  }}
                  itemStyle={{ color: "var(--color-slate-800)" }}
                  labelStyle={{ color: "var(--color-slate-500)" }}
                />
                <Line
                  dataKey="percentage"
                  name="Attendance"
                  type="linear"
                  connectNulls={false}
                  stroke="var(--chart-line)"
                  strokeWidth={2}
                  isAnimationActive={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <p className="mt-3 text-[10px] text-slate-500">
            Only recorded dates are plotted. Excused-only dates have no
            percentage; gaps do not imply absence.
          </p>
        </>
      )}
    </Card>
  );
}
