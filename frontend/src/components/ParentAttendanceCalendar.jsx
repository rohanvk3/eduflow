import { useState } from "react";
import { Card, Input } from "./UI";
import { pct } from "./ParentUI";
export default function ParentAttendanceCalendar({ trend, fromDate, toDate }) {
  const [month, setMonth] = useState(toDate.slice(0, 7)),
    [selected, setSelected] = useState(null);
  const first = new Date(month + "-01T12:00:00"),
    offset = (first.getDay() + 6) % 7,
    days = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate(),
    map = new Map(trend.map((d) => [d.date, d.totals]));
  return (
    <Card className="mb-5 p-5">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-sm font-semibold">Attendance calendar</h2>
        <Input
          type="month"
          aria-label="Calendar month"
          value={month}
          min={fromDate.slice(0, 7)}
          max={toDate.slice(0, 7)}
          className="!w-auto"
          onChange={(e) => {
            if (e.target.value) {
              setMonth(e.target.value);
              setSelected(null);
            }
          }}
        />
      </div>
      <div className="grid grid-cols-7 gap-1.5 text-center text-xs">
        {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) => (
          <span className="py-2 text-[10px] text-slate-500" key={day}>
            {day}
          </span>
        ))}
        {Array.from({ length: offset }, (_, i) => (
          <span key={"blank" + i} />
        ))}
        {Array.from({ length: days }, (_, i) => {
          const day = month + "-" + String(i + 1).padStart(2, "0"),
            data = map.get(day),
            outside = day < fromDate || day > toDate;
          return (
            <button
              key={day}
              disabled={outside}
              aria-label={
                day +
                (data
                  ? ", " +
                    data.totalEligibleFinalizedSessions +
                    " finalized sessions"
                  : ", no finalized records")
              }
              onClick={() => setSelected(day)}
              className={
                "min-h-12 rounded-lg border p-1 transition-colors " +
                (data
                  ? "border-indigo-100 bg-indigo-50 text-indigo-700 hover:bg-indigo-100"
                  : "border-slate-100 text-slate-500 hover:bg-slate-50") +
                (outside ? " opacity-30" : "")
              }
            >
              <span>{i + 1}</span>
              {data && (
                <span className="mt-1 block text-[8px]">
                  {data.totalEligibleFinalizedSessions} class
                  {data.totalEligibleFinalizedSessions === 1 ? "" : "es"}
                </span>
              )}
            </button>
          );
        })}
      </div>
      <p className="mt-4 text-xs leading-5 text-slate-500">
        Dates without finalized records do not imply absence. Counts include
        multiple classes on the same date.
      </p>
      {selected && (
        <div className="mt-4 rounded-lg bg-slate-50 p-4 text-xs">
          <strong>{selected}</strong>
          {map.has(selected) ? (
            <p className="mt-2 leading-6">
              Present {map.get(selected).presentCount} · Late{" "}
              {map.get(selected).lateCount} · Absent{" "}
              {map.get(selected).absentCount} · Excused{" "}
              {map.get(selected).excusedCount} ·{" "}
              {pct(map.get(selected).attendancePercentage)}
            </p>
          ) : (
            <p className="mt-2">No finalized attendance records.</p>
          )}
        </div>
      )}
    </Card>
  );
}
