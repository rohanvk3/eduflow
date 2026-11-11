import { useEffect, useState } from "react";
import axios from "axios";
import { getMyAttendance } from "../services/attendanceService";
import { errorMessage } from "../api/client";
import {
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Pagination,
  Table,
  TableSkeleton,
  dateLabel,
} from "../components/UI";
import {
  AttendanceBadge,
  AttendanceStats,
  DateRange,
  GroupAttendanceTable,
  defaultRange,
} from "../components/AttendanceUI";
import AttendanceTrend from "../components/AttendanceTrend";

export default function StudentAttendancePage() {
  const [range, setRange] = useState(defaultRange);
  const [page, setPage] = useState(0);
  const [batchPage, setBatchPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    setError("");
    getMyAttendance({ ...range, page, batchPage, size }, c.signal)
      .then(({ data }) => {
        if (!c.signal.aborted) setData(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [range, page, batchPage, size, reload]);
  const resize = (value) => {
    setSize(value);
    setPage(0);
    setBatchPage(0);
  };
  return (
    <>
      <h1 className="page-title">My attendance</h1>
      <p className="mb-6 mt-2 text-sm text-slate-500">
        Your finalized class attendance and learning history.
      </p>
      <Card className="mb-6 p-5">
        <DateRange
          value={range}
          onChange={(v) => {
            setRange(v);
            setPage(0);
            setBatchPage(0);
          }}
        />
        <p className="mt-4 text-xs text-slate-500">
          Present and late count as attended. Excused entries do not reduce your
          percentage.
        </p>
      </Card>
      <ErrorNotice message={error} />
      {error && (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Retry my attendance
        </Button>
      )}
      {loading ? (
        <TableSkeleton />
      ) : (
        !error &&
        data &&
        (!data.linked ? (
          <Card>
            <EmptyState
              title="Your student profile is not linked yet"
              description="Contact your administrator to connect your login account to your student record."
            />
          </Card>
        ) : (
          <>
            <AttendanceStats totals={data.totals} />
            <AttendanceTrend data={data.trend} />
            <Card className="mb-6 overflow-hidden">
              <h2 className="p-5 text-sm font-semibold">
                Batch-wise attendance
              </h2>
              <GroupAttendanceTable
                rows={data.batches.content}
                kind="batches"
              />
              <Pagination
                noun="batches"
                page={batchPage}
                size={size}
                total={data.batches.totalElements}
                totalPages={data.batches.totalPages}
                onPageChange={setBatchPage}
                onSizeChange={resize}
              />
            </Card>
            <Card className="overflow-hidden">
              <h2 className="p-5 text-sm font-semibold">Attendance history</h2>
              {!data.history.content.length ? (
                <EmptyState
                  title="No finalized attendance in this date range"
                  description="Unmarked students, draft sessions and days without a class are never counted as absent."
                />
              ) : (
                <Table label="My attendance history" className="min-w-[700px]">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Batch / course</th>
                      <th>Class time</th>
                      <th>Topic</th>
                      <th>Status</th>
                      <th>Remarks</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.history.content.map((r) => (
                      <tr key={r.sessionId}>
                        <td className="whitespace-nowrap text-xs">
                          {dateLabel(r.sessionDate)}
                        </td>
                        <td>
                          <p>{r.batchName}</p>
                          <p className="mt-1 text-xs text-slate-500">
                            {r.courseName}
                          </p>
                        </td>
                        <td className="text-xs">
                          {r.startTime
                            ? `${r.startTime.slice(0, 5)}–${r.endTime.slice(0, 5)}`
                            : "Untimed"}
                        </td>
                        <td className="max-w-48 break-words text-xs">
                          {r.topic || "—"}
                        </td>
                        <td>
                          <AttendanceBadge status={r.status} />
                        </td>
                        <td className="max-w-64 break-words text-xs">
                          {r.remarks || "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
              <Pagination
                noun="class records"
                page={page}
                size={size}
                total={data.history.totalElements}
                totalPages={data.history.totalPages}
                onPageChange={setPage}
                onSizeChange={resize}
              />
            </Card>
          </>
        ))
      )}
    </>
  );
}
