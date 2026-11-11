import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { Plus, CalendarDays, List, ClipboardCheck } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getSessions,
  getBatchRegister,
  getAttendanceOverview,
} from "../services/attendanceService";
import { getBatches } from "../services/academicService";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Modal,
  Pagination,
  Select,
  TableSkeleton,
} from "../components/UI";
import {
  AttendanceStats,
  DateRange,
  GroupAttendanceTable,
  SessionTable,
  defaultRange,
} from "../components/AttendanceUI";
import RecordSelect from "../components/RecordSelect";
import AttendanceCalendar from "../components/AttendanceCalendar";
import CreateAttendanceSession from "../components/CreateAttendanceSession";
import StudentAttendancePage from "./StudentAttendancePage";

export default function AttendancePage() {
  const { user } = useAuth();
  if (user.role === "STUDENT") return <StudentAttendancePage />;
  if (!["ADMIN", "TEACHER"].includes(user.role))
    return (
      <ErrorNotice message="Attendance access is not available for your role." />
    );
  return <StaffAttendance />;
}
function StaffAttendance() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [range, setRange] = useState(defaultRange);
  const [batchId, setBatchId] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [mode, setMode] = useState("list");
  const [data, setData] = useState(null);
  const [overview, setOverview] = useState(null);
  const [assigned, setAssigned] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [creating, setCreating] = useState(false);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    setError("");
    const params = {
      ...range,
      batchId: batchId || undefined,
      status: status || undefined,
      page,
      size,
    };
    Promise.all([
      batchId
        ? getBatchRegister(batchId, params, c.signal)
        : getSessions(params, c.signal),
      getAttendanceOverview(
        { ...range, batchId: batchId || undefined },
        c.signal,
      ),
      user.role === "TEACHER"
        ? getBatches({ page: 0, size: 5 }, c.signal)
        : Promise.resolve({ data: null }),
    ])
      .then(([s, o, b]) => {
        if (!c.signal.aborted) {
          setData(s.data);
          setOverview(o.data);
          setAssigned(b.data);
        }
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [range, batchId, status, page, size, reload, user.role]);
  return (
    <>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Attendance</h1>
          <p className="mt-2 text-sm text-slate-500">
            Track class attendance and student participation.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {user.role === "ADMIN" && (
            <Link className="button-secondary" to="/attendance/reports">
              <ClipboardCheck size={15} />
              Reports
            </Link>
          )}
          <Button onClick={() => setCreating(true)}>
            <Plus size={16} />
            Create session
          </Button>
        </div>
      </div>
      <Card className="mb-6 p-5">
        <div className="grid items-start gap-5 lg:grid-cols-2">
          <RecordSelect
            endpoint="/batches"
            status=""
            required={false}
            label={user.role === "TEACHER" ? "Assigned batch" : "Batch"}
            value={batchId}
            onChange={(v) => {
              setBatchId(v);
              setPage(0);
            }}
          />
          <DateRange
            value={range}
            onChange={(v) => {
              setRange(v);
              setPage(0);
            }}
          />
        </div>
        <p className="mt-3 text-[11px] text-slate-500">
          Asia/Kolkata · Only finalized records count toward attendance
          percentages. Excused entries are excluded.
        </p>
      </Card>
      <ErrorNotice message={error} />
      {error && (
        <Button
          variant="secondary"
          className="mb-5"
          onClick={() => setReload((r) => r + 1)}
        >
          Retry attendance
        </Button>
      )}
      <AttendanceStats
        totals={error ? null : overview?.totals}
        loading={loading}
      />
      {overview && !error && (
        <div className="mb-6 flex flex-wrap gap-3 text-xs text-slate-500">
          <Badge tone="indigo">
            {overview.totals.finalizedSessions} finalized classes
          </Badge>
          <Badge tone="warning">
            {overview.pendingDrafts} draft classes in selected range
          </Badge>
          <Button
            variant="ghost"
            onClick={() => {
              setStatus("DRAFT");
              setPage(0);
            }}
          >
            Show sessions needing attendance
          </Button>
        </div>
      )}
      {user.role === "TEACHER" && assigned && (
        <Card className="mb-6 p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Assigned batches</h2>
            <Link to="/batches" className="text-xs text-indigo-600">
              View all ({assigned.totalElements})
            </Link>
          </div>
          {assigned.content.length ? (
            <div className="flex flex-wrap gap-2">
              {assigned.content.map((b) => (
                <Button
                  key={b.id}
                  variant="secondary"
                  onClick={() => {
                    setBatchId(String(b.id));
                    setPage(0);
                  }}
                >
                  {b.name}
                </Button>
              ))}
            </div>
          ) : (
            <EmptyState
              compact
              title="No active teacher assignments"
              description="An administrator must link your teacher profile and assign a batch."
            />
          )}
        </Card>
      )}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2">
          <Button
            variant={mode === "list" ? "primary" : "secondary"}
            aria-pressed={mode === "list"}
            onClick={() => setMode("list")}
          >
            <List size={15} />
            Sessions
          </Button>
          <Button
            variant={mode === "calendar" ? "primary" : "secondary"}
            aria-pressed={mode === "calendar"}
            onClick={() => setMode("calendar")}
          >
            <CalendarDays size={15} />
            Calendar
          </Button>
        </div>
        {mode === "list" && (
          <Select
            aria-label="Session status"
            className="!w-auto"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="FINALIZED">Finalized</option>
          </Select>
        )}
      </div>
      {mode === "calendar" ? (
        <AttendanceCalendar key={`${batchId}-${reload}`} batchId={batchId} />
      ) : (
        <Card className="mb-6 overflow-hidden">
          {loading ? (
            <TableSkeleton />
          ) : (
            !error &&
            (!data?.content.length ? (
              <EmptyState
                title="No attendance sessions in this range"
                description="Create a class session or adjust the batch and date filters. No sessions does not mean students were absent."
              />
            ) : (
              <SessionTable sessions={data.content} />
            ))
          )}
          <Pagination
            noun="sessions"
            page={page}
            size={size}
            total={error ? 0 : data?.totalElements}
            totalPages={data?.totalPages}
            loading={loading || !!error}
            onPageChange={setPage}
            onSizeChange={(v) => {
              setSize(v);
              setPage(0);
            }}
          />
        </Card>
      )}
      {overview && !error && (
        <>
          <Card className="mb-6 overflow-hidden">
            <div className="p-5">
              <h2 className="text-sm font-semibold">
                Batch insights · first 5 batches with finalized data
              </h2>
            </div>
            <GroupAttendanceTable
              rows={overview.batches.content}
              kind="batches"
            />
          </Card>
          <Card className="overflow-hidden">
            <div className="p-5">
              <h2 className="text-sm font-semibold">
                Recent finalized sessions
              </h2>
            </div>
            {overview.recentFinalized.content.length ? (
              <SessionTable sessions={overview.recentFinalized.content} />
            ) : (
              <EmptyState compact title="No finalized sessions yet" />
            )}
          </Card>
        </>
      )}
      {creating && (
        <Modal
          title="Create attendance session"
          busy={busy}
          onClose={() => setCreating(false)}
        >
          <CreateAttendanceSession
            batchId={batchId}
            onBusyChange={setBusy}
            onClose={() => setCreating(false)}
            onCreated={(result) => {
              setCreating(false);
              navigate(`/attendance/sessions/${result.session.id}`);
            }}
          />
        </Modal>
      )}
    </>
  );
}
