import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartNoAxesCombined } from "lucide-react";
import { EmptyState } from "./UI";

// Supply real monthly { month, amount } records when the fee reporting API exists.
// No points, zero-value series, or sample revenue are fabricated for an empty chart.
export default function RevenueChart({ data }) {
  const available = Array.isArray(data) && data.length > 0;
  return (
    <div
      className="relative h-[246px] min-w-0"
      role="img"
      aria-label={
        available
          ? "Monthly fee collection chart"
          : "Revenue chart unavailable: fee reporting is not connected"
      }
    >
      <ResponsiveContainer
        width="100%"
        height="100%"
        minWidth={0}
        initialDimension={{ width: 600, height: 246 }}
      >
        <AreaChart
          data={available ? data : []}
          margin={{ top: 20, right: 16, bottom: 10, left: 0 }}
          accessibilityLayer
        >
          <CartesianGrid stroke="var(--color-slate-100)" vertical={false} />
          <XAxis
            dataKey="month"
            hide={!available}
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--color-slate-500)", fontSize: 11 }}
          />
          <YAxis
            hide={!available}
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--color-slate-500)", fontSize: 11 }}
          />
          {available && (
            <Tooltip
              formatter={(value) =>
                new Intl.NumberFormat("en-IN", {
                  style: "currency",
                  currency: "INR",
                  maximumFractionDigits: 0,
                }).format(value)
              }
              contentStyle={{
                borderRadius: 10,
                borderColor: "var(--color-slate-200)",
                backgroundColor: "var(--color-surface)",
                color: "var(--color-slate-800)",
                fontSize: 12,
              }}
              itemStyle={{ color: "var(--color-slate-800)" }}
              labelStyle={{ color: "var(--color-slate-500)" }}
            />
          )}
          <Area
            dataKey="amount"
            type="monotone"
            stroke="var(--chart-line)"
            fill="var(--color-indigo-50)"
            strokeWidth={2}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
      {!available && (
        <div className="absolute inset-0 flex items-center justify-center">
          <EmptyState
            compact
            icon={ChartNoAxesCombined}
            title="A clearer view of your revenue"
            description="Monthly collections will appear here when fee reporting is available."
          />
        </div>
      )}
    </div>
  );
}
