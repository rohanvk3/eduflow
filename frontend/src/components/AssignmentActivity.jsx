import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
} from "recharts";
import { Card, EmptyState } from "./UI";
export default function AssignmentActivity({ rows = [] }) {
  const batches = Array.from(
      new Map(rows.map((r) => [r.batchId, r.batchName])).entries(),
    ).slice(0, 10),
    ids = new Set(batches.map(([id]) => id)),
    days = new Map();
  for (const row of rows) {
    if (!ids.has(row.batchId)) continue;
    const day = days.get(row.day) || { day: row.day };
    day["batch" + row.batchId] = row.submissions;
    days.set(row.day, day);
  }
  const data = Array.from(days.values()).sort((a, b) =>
    a.day.localeCompare(b.day),
  );
  return (
    <Card className="mt-6 min-w-0 p-5">
      <h3 className="text-sm font-semibold">Submission activity by batch</h3>
      <p className="mb-5 mt-2 text-xs leading-6 text-slate-500">
        First submissions in the last 30 institute-local days. Revisions do not
        increase the count. Up to 10 batches are shown.
      </p>
      {!data.length ? (
        <EmptyState compact title="No recent submission activity" />
      ) : (
        <div
          className="h-64"
          role="img"
          aria-label="Daily first submissions by batch"
        >
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data}>
              <CartesianGrid stroke="var(--color-slate-100)" vertical={false} />
              <XAxis
                dataKey="day"
                tick={{ fill: "var(--color-slate-500)", fontSize: 10 }}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fill: "var(--color-slate-500)", fontSize: 10 }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface)",
                  color: "var(--color-slate-800)",
                  border: "1px solid var(--color-slate-200)",
                  borderRadius: 10,
                }}
              />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              {batches.map(([id, name], i) => (
                <Bar
                  key={id}
                  dataKey={"batch" + id}
                  name={name}
                  stackId="submissions"
                  fill={
                    ["#818cf8", "#34d399", "#fbbf24", "#38bdf8", "#c084fc"][
                      i % 5
                    ]
                  }
                />
              ))}
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}
