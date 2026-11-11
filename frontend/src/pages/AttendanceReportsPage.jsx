import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import { Download } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getAttendanceReports,
  exportAttendanceReport,
} from "../services/attendanceService";
import RecordSelect from "../components/RecordSelect";
import {
  AttendanceStats,
  DateRange,
  GroupAttendanceTable,
  defaultRange,
} from "../components/AttendanceUI";
import AttendanceTrend from "../components/AttendanceTrend";
import {
  Button,
  Card,
  ErrorNotice,
  Pagination,
  TableSkeleton,
} from "../components/UI";

export default function AttendanceReportsPage() {
  const { user } = useAuth();
  const allowed = user.role === "ADMIN";
  const [range, setRange] = useState(defaultRange);
  const [courseId, setCourseId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [studentId, setStudentId] = useState("");
  const [page, setPage] = useState(0);
  const [batchPage, setBatchPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [exporting, setExporting] = useState(false);
  const exportLock = useRef(false);
  const [exportError, setExportError] = useState("");
  const filter = {
    ...range,
    courseId: courseId || undefined,
    batchId: batchId || undefined,
    studentId: studentId || undefined,
  };
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    getAttendanceReports(
      {
        ...range,
        courseId: courseId || undefined,
        batchId: batchId || undefined,
        studentId: studentId || undefined,
        page,
        batchPage,
        size,
      },
      c.signal,
    )
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
  }, [
    allowed,
    range,
    courseId,
    batchId,
    studentId,
    page,
    batchPage,
    size,
    reload,
  ]);
  if (!allowed)
    return (
      <ErrorNotice message="Attendance reports and CSV export are available to administrators only." />
    );
  const reset = () => {
    setPage(0);
    setBatchPage(0);
  };
  const resize = (v) => {
    setSize(v);
    reset();
  };
  const download = async () => {
    if (exportLock.current) return;
    exportLock.current = true;
    setExporting(true);
    setExportError("");
    try {
      const { data } = await exportAttendanceReport(filter);
      const url = URL.createObjectURL(data);
      const a = document.createElement("a");
      a.href = url;
      a.download = `eduflow-attendance-${range.fromDate}-${range.toDate}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      if (e.response?.data instanceof Blob) {
        try {
          const body = JSON.parse(await e.response.data.text());
          setExportError(body.message || "Export failed");
        } catch {
          setExportError("Export failed. Please try again.");
        }
      } else setExportError(errorMessage(e));
    } finally {
      exportLock.current = false;
      setExporting(false);
    }
  };
  return (
    <>
      <Link
        className="mb-5 inline-block text-xs text-slate-500"
        to="/attendance"
      >
        ← Back to attendance
      </Link>
      <div className="mb-6 flex flex-wrap justify-between gap-4">
        <div>
          <h1 className="page-title">Attendance reports</h1>
          <p className="mt-2 text-sm text-slate-500">
            Finalized participation by student, batch and date range.
          </p>
        </div>
        <Button
          variant="secondary"
          disabled={loading || exporting || !!error}
          onClick={download}
        >
          <Download size={15} />
          {exporting ? "Exporting…" : "Export CSV"}
        </Button>
      </div>
      <Card className="mb-6 p-5">
        <div className="grid gap-5 lg:grid-cols-3">
          <RecordSelect
            endpoint="/courses"
            status=""
            required={false}
            label="Course"
            value={courseId}
            onChange={(v) => {
              setCourseId(v);
              reset();
            }}
          />
          <RecordSelect
            endpoint="/batches"
            status=""
            required={false}
            label="Batch"
            value={batchId}
            onChange={(v) => {
              setBatchId(v);
              reset();
            }}
          />
          <RecordSelect
            endpoint="/students"
            status=""
            required={false}
            label="Student"
            value={studentId}
            onChange={(v) => {
              setStudentId(v);
              reset();
            }}
          />
        </div>
        <div className="mt-5">
          <DateRange
            value={range}
            onChange={(v) => {
              setRange(v);
              reset();
            }}
          />
        </div>
        <p className="mt-4 text-xs leading-6 text-slate-500">
          Filters are combined. Percentages use (present + late) / (present +
          late + absent). Excused is excluded; no-data percentages remain empty.
          Export uses the same filters and is limited to 10,000 students.
        </p>
      </Card>
      <ErrorNotice message={error} />
      <ErrorNotice message={exportError} />
      {error && (
        <Button
          variant="secondary"
          className="mb-5"
          onClick={() => setReload((r) => r + 1)}
        >
          Retry report
        </Button>
      )}
      {loading ? (
        <TableSkeleton />
      ) : (
        !error &&
        data && (
          <>
            <AttendanceStats totals={data.totals} />
            <p className="mb-4 text-xs text-slate-500">
              {data.totals.finalizedSessions} finalized classes ·{" "}
              {data.fromDate} to {data.toDate} (Asia/Kolkata)
            </p>
            <AttendanceTrend data={data.trend} />
            <Card className="mb-6 overflow-hidden">
              <h2 className="p-5 text-sm font-semibold">
                Student-wise summary
              </h2>
              <GroupAttendanceTable rows={data.students.content} />
              <Pagination
                noun="students"
                page={page}
                size={size}
                total={data.students.totalElements}
                totalPages={data.students.totalPages}
                onPageChange={setPage}
                onSizeChange={resize}
              />
            </Card>
            <Card className="overflow-hidden">
              <h2 className="p-5 text-sm font-semibold">Batch-wise summary</h2>
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
          </>
        )
      )}
    </>
  );
}
