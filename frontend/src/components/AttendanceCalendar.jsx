import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import { getBatchRegister } from "../services/attendanceService";
import { errorMessage } from "../api/client";
import { Button, Card, EmptyState, ErrorNotice, Input, Loading } from "./UI";
import { localDate, AttendanceBadge } from "./AttendanceUI";

export default function AttendanceCalendar({ batchId }) {
  const [month, setMonth] = useState(localDate().slice(0, 7));
  const [data, setData] = useState(null);
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    setPage(0);
    setRows([]);
  }, [batchId, month]);
  useEffect(() => {
    if (!batchId) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    const end = new Date(
      Number(month.slice(0, 4)),
      Number(month.slice(5)),
      0,
    ).getDate();
    getBatchRegister(
      batchId,
      {
        fromDate: `${month}-01`,
        toDate: `${month}-${end}`,
        page,
        size: 100,
        sort: "sessionDate,asc",
      },
      c.signal,
    )
      .then(({ data }) => {
        if (!c.signal.aborted) {
          setData(data);
          setRows((old) =>
            page === 0 ? data.content : [...old, ...data.content],
          );
        }
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [batchId, month, page, reload]);
  if (!batchId)
    return (
      <Card>
        <EmptyState
          title="Select a batch to view its calendar"
          description="Dates without sessions do not imply absence."
        />
      </Card>
    );
  const first = new Date(`${month}-01T12:00:00`);
  const offset = (first.getDay() + 6) % 7;
  const days = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  return (
    <Card className="p-5">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-sm font-semibold">Batch attendance calendar</h2>
        <label className="field-label">
          Calendar month
          <Input
            className="mt-2"
            type="month"
            value={month}
            onChange={(e) => {
              if (e.target.value) setMonth(e.target.value);
            }}
          />
        </label>
      </div>
      <p className="mb-4 text-xs text-slate-500">
        Each class is shown separately. Empty days have no loaded session, not
        an absence.
      </p>
      <ErrorNotice message={error} />
      {error && (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Retry calendar
        </Button>
      )}
      {loading && page === 0 ? (
        <Loading label="Loading calendar…" />
      ) : (
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-7">
          {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((d) => (
            <div
              key={d}
              className="hidden pb-2 text-center text-xs text-slate-400 sm:block"
            >
              {d}
            </div>
          ))}
          {Array.from({ length: offset }, (_, i) => (
            <div key={`blank-${i}`} className="hidden sm:block" />
          ))}
          {Array.from({ length: days }, (_, i) => {
            const date = `${month}-${String(i + 1).padStart(2, "0")}`;
            return (
              <div
                key={date}
                className="min-h-24 min-w-0 rounded-lg border border-slate-100 p-2"
              >
                <p className="mb-2 text-xs font-medium text-slate-600">
                  {i + 1}
                </p>
                {rows
                  .filter((s) => s.sessionDate === date)
                  .map((s) => (
                    <Link
                      className="mb-2 block break-words rounded border border-indigo-100 bg-indigo-50/50 p-2 text-[10px] hover:border-indigo-300"
                      key={s.id}
                      to={`/attendance/sessions/${s.id}`}
                    >
                      <p className="mb-1">
                        {s.startTime?.slice(0, 5) || "Untimed"} ·{" "}
                        {s.topic || "Class"}
                      </p>
                      <AttendanceBadge status={s.status} />
                    </Link>
                  ))}
              </div>
            );
          })}
        </div>
      )}
      {data && data.totalElements > rows.length && (
        <div className="mt-5 flex flex-wrap items-center gap-3 text-xs text-slate-500">
          <span>
            {rows.length} of {data.totalElements} sessions loaded
          </span>
          <Button
            variant="secondary"
            disabled={loading}
            onClick={() => setPage((p) => p + 1)}
          >
            Load more calendar sessions
          </Button>
        </div>
      )}
    </Card>
  );
}
