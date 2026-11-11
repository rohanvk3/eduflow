import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { Card, EmptyState } from "./UI";
import { inr } from "./FinanceUI";
const tooltip = {
  background: "var(--color-surface)",
  border: "1px solid var(--color-slate-200)",
  borderRadius: 10,
  color: "var(--color-slate-800)",
};
export default function FinanceCharts({ data }) {
  return (
    <div className="mb-6 grid gap-6 lg:grid-cols-3">
      <Card className="min-w-0 p-5 lg:col-span-2">
        <h2 className="mb-1 text-sm font-semibold">Monthly collections</h2>
        <p className="mb-5 text-xs text-slate-500">
          Successful payments in the selected payment-date range
        </p>
        {!data.monthlyTrend.length ? (
          <EmptyState title="No collections in this period" compact />
        ) : (
          <div
            role="img"
            aria-label="Monthly successful collections"
            className="h-64"
          >
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={data.monthlyTrend}
                margin={{ left: 5, right: 10 }}
              >
                <CartesianGrid
                  stroke="var(--color-slate-100)"
                  vertical={false}
                />
                <XAxis
                  dataKey="label"
                  tick={{ fill: "var(--color-slate-500)", fontSize: 11 }}
                />
                <YAxis
                  width={80}
                  tickFormatter={(v) => inr(v)}
                  tick={{ fill: "var(--color-slate-500)", fontSize: 10 }}
                />
                <Tooltip
                  contentStyle={tooltip}
                  formatter={(v) => [inr(v), "Collected"]}
                />
                <Area
                  dataKey="amount"
                  stroke="var(--chart-line)"
                  fill="var(--color-indigo-50)"
                  strokeWidth={2}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
      <Card className="min-w-0 p-5">
        <h2 className="text-sm font-semibold">Payment methods</h2>
        {!data.paymentMethods.length ? (
          <EmptyState title="No payment methods yet" compact />
        ) : (
          <>
            <div
              role="img"
              aria-label="Collections by payment method"
              className="h-48"
            >
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={data.paymentMethods}
                    dataKey="amount"
                    nameKey="label"
                    innerRadius={48}
                    outerRadius={76}
                  >
                    {data.paymentMethods.map((m, i) => (
                      <Cell
                        key={m.label}
                        fill={
                          ["#818cf8", "#34d399", "#fbbf24", "#38bdf8"][i % 4]
                        }
                      />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={tooltip} formatter={(v) => inr(v)} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <ul className="space-y-3 text-xs">
              {data.paymentMethods.map((m) => (
                <li key={m.label} className="flex justify-between gap-3">
                  <span className="text-slate-500">
                    {m.label.replaceAll("_", " ")}
                  </span>
                  <span className="font-medium">{inr(m.amount)}</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </Card>
    </div>
  );
}
