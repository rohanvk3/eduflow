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
import { pct } from "./ExamUI";
export default function ExamTrend({ rows = [] }) {
  const data = [...rows].sort(
    (a, b) => new Date(a.examDate) - new Date(b.examDate) || a.id - b.id,
  );
  return (
    <Card className="mb-6 min-w-0 p-5">
      <h2 className="mb-4 text-sm font-semibold">
        Published performance trend
      </h2>
      {!data.length ? (
        <EmptyState compact title="No published scores yet" />
      ) : (
        <div className="h-64" role="img" aria-label="Exam percentage trend">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
              <CartesianGrid stroke="var(--color-slate-100)" />
              <XAxis
                dataKey="examTitle"
                tick={{ fill: "var(--color-slate-500)", fontSize: 10 }}
              />
              <YAxis
                domain={[0, 100]}
                tick={{ fill: "var(--color-slate-500)", fontSize: 10 }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface)",
                  border: "1px solid var(--color-slate-200)",
                  color: "var(--color-slate-800)",
                  borderRadius: 10,
                }}
                formatter={(v) => [pct(v), "Score"]}
              />
              <Line
                dataKey="percentage"
                stroke="var(--chart-line)"
                strokeWidth={2}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}
