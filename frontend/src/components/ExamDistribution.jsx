import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { Card, EmptyState } from "./UI";
export default function ExamDistribution({ rows }) {
  return (
    <Card className="mb-6 min-w-0 p-5">
      <h2 className="mb-5 text-sm font-semibold">
        Exam-wise score distribution
      </h2>
      {!rows.length ? (
        <EmptyState compact title="No published score distribution" />
      ) : (
        <div className="h-72" role="img" aria-label="Exam score distribution">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={rows}>
              <CartesianGrid stroke="var(--color-slate-100)" />
              <XAxis
                dataKey="label"
                tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fontSize: 10, fill: "var(--color-slate-500)" }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface)",
                  color: "var(--color-slate-800)",
                  border: "1px solid var(--color-slate-200)",
                }}
              />
              <Bar
                dataKey="count"
                name="Students"
                fill="var(--chart-line)"
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}
