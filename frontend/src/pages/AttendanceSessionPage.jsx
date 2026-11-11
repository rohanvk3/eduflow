import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axios from "axios";
import { Save, CheckCircle2, RefreshCw } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getSessionById,
  saveAttendanceRecords,
  finalizeSession,
} from "../services/attendanceService";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorNotice,
  Input,
  Loading,
  Select,
  Table,
  Toast,
  dateLabel,
} from "../components/UI";
import { AttendanceBadge, localDate } from "../components/AttendanceUI";
import useUnsavedAttendance from "../components/useUnsavedAttendance";

const statuses = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
export default function AttendanceSessionPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const allowed = ["ADMIN", "TEACHER"].includes(user.role);
  const [data, setData] = useState(null);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const locked = useRef(false);
  const [search, setSearch] = useState("");
  const [bulk, setBulk] = useState("PRESENT");
  const [confirming, setConfirming] = useState(false);
  const [notice, setNotice] = useState("");
  useUnsavedAttendance(dirty);
  const install = (result) => {
    setData(result);
    setRows(
      result.students.map((r) => ({
        ...r,
        status: r.status || "",
        remarks: r.remarks || "",
      })),
    );
    setDirty(false);
  };
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    getSessionById(id, c.signal)
      .then(({ data }) => {
        if (!c.signal.aborted) install(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [id, reload, allowed]);
  if (!allowed)
    return (
      <ErrorNotice message="Only administrators and assigned teachers may view a class register." />
    );
  const finalized = data?.session.status === "FINALIZED";
  const filtered = rows.filter((r) =>
    `${r.fullName} ${r.admissionNumber}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  );
  const unmarked = rows.filter((r) => !r.status).length;
  const update = (studentId, key, value) => {
    setRows((old) =>
      old.map((r) => (r.studentId === studentId ? { ...r, [key]: value } : r)),
    );
    setDirty(true);
  };
  const save = async () => {
    if (locked.current) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      const { data: result } = await saveAttendanceRecords(id, {
        version: data.session.version,
        records: rows
          .filter((r) => r.status)
          .map((r) => ({
            studentId: r.studentId,
            status: r.status,
            remarks: r.remarks,
          })),
      });
      install(result);
      setNotice("Draft saved. Unmarked students remain unmarked.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  const finalize = async () => {
    if (locked.current) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      const { data: result } = await finalizeSession(id, data.session.version);
      install(result);
      setConfirming(false);
      setNotice("Attendance finalized. This register is now read-only.");
    } catch (e) {
      setError(errorMessage(e));
      setConfirming(false);
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  const refresh = () => {
    if (
      !dirty ||
      window.confirm("Discard unsaved changes and reload the register?")
    ) {
      setDirty(false);
      setReload((r) => r + 1);
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
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Class attendance</h1>
          <p className="mt-2 text-sm text-slate-500">
            Mark the register, save a draft, then finalize when complete.
          </p>
        </div>
        <Button variant="secondary" disabled={busy} onClick={refresh}>
          <RefreshCw size={15} />
          Reload
        </Button>
      </div>
      <ErrorNotice message={error} />
      {loading ? (
        <Loading label="Loading eligible students…" />
      ) : (
        data && (
          <>
            <Card className="mb-6 p-5">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <h2 className="break-words text-lg font-semibold">
                    {data.session.batchName}
                  </h2>
                  <p className="mt-2 text-xs text-slate-500">
                    {data.session.courseName} ·{" "}
                    {dateLabel(data.session.sessionDate)} ·{" "}
                    {data.session.startTime
                      ? `${data.session.startTime.slice(0, 5)}–${data.session.endTime.slice(0, 5)}`
                      : "Untimed class"}{" "}
                    · Asia/Kolkata
                  </p>
                  <p className="mt-2 break-words text-sm">
                    {data.session.topic || "No topic provided"}
                  </p>
                </div>
                <AttendanceBadge status={data.session.status} />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Badge>{rows.length} eligible students</Badge>
                <Badge tone={unmarked ? "warning" : "success"}>
                  {unmarked} not marked
                </Badge>
                {dirty && <Badge tone="warning">Unsaved changes</Badge>}
                {finalized && (
                  <Badge tone="success">Finalized roster preserved</Badge>
                )}
              </div>
            </Card>
            {data.staleRecords > 0 && (
              <ErrorNotice
                message={`${data.staleRecords} old draft records no longer match current enrollment. Reload, review and save the displayed register before finalizing.`}
              />
            )}
            {finalized && (
              <p className="mb-5 rounded-lg border border-indigo-100 bg-indigo-50 p-4 text-xs leading-6 text-indigo-700">
                Finalized attendance is read-only. Historical entries are
                retained after enrollment changes. No ordinary correction action
                is available.
              </p>
            )}
            <Card className="overflow-hidden">
              <div className="flex flex-wrap items-center gap-3 p-5">
                <Input
                  type="search"
                  aria-label="Search attendance students"
                  placeholder="Search name or admission number…"
                  className="max-w-lg"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                {!finalized && (
                  <>
                    <Select
                      aria-label="Bulk attendance status"
                      className="!w-auto"
                      disabled={busy}
                      value={bulk}
                      onChange={(e) => setBulk(e.target.value)}
                    >
                      {statuses.map((s) => (
                        <option key={s}>{s}</option>
                      ))}
                    </Select>
                    <Button
                      variant="secondary"
                      disabled={busy || !filtered.length}
                      onClick={() => {
                        const ids = new Set(filtered.map((r) => r.studentId));
                        setRows((old) =>
                          old.map((r) =>
                            ids.has(r.studentId) ? { ...r, status: bulk } : r,
                          ),
                        );
                        setDirty(true);
                      }}
                    >
                      Apply to visible students
                    </Button>
                  </>
                )}
              </div>
              {!rows.length ? (
                <EmptyState
                  title="No eligible students for this session date"
                  description="Eligibility uses enrollment dates and withdrawal/completion history, not the current student status."
                />
              ) : (
                <Table
                  label="Attendance marking register"
                  className="min-w-[750px]"
                >
                  <thead>
                    <tr>
                      <th>Student</th>
                      <th>Admission number</th>
                      <th>Status</th>
                      <th>Remarks</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((r) => (
                      <tr key={r.studentId}>
                        <td className="font-medium">{r.fullName}</td>
                        <td
                          className="max-w-44 truncate font-mono text-[10px]"
                          title={r.admissionNumber}
                        >
                          {r.admissionNumber}
                        </td>
                        <td>
                          {finalized ? (
                            <AttendanceBadge status={r.status} />
                          ) : (
                            <Select
                              aria-label={`Attendance for ${r.fullName}`}
                              disabled={busy}
                              value={r.status}
                              onChange={(e) =>
                                update(r.studentId, "status", e.target.value)
                              }
                            >
                              <option value="">Not marked</option>
                              {statuses.map((s) => (
                                <option key={s}>{s}</option>
                              ))}
                            </Select>
                          )}
                        </td>
                        <td>
                          {finalized ? (
                            <p className="max-w-xs whitespace-pre-wrap break-words text-xs">
                              {r.remarks || "—"}
                            </p>
                          ) : (
                            <Input
                              aria-label={`Remarks for ${r.fullName}`}
                              maxLength={500}
                              disabled={busy}
                              value={r.remarks}
                              onChange={(e) =>
                                update(r.studentId, "remarks", e.target.value)
                              }
                            />
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
              {!finalized && (
                <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 p-5">
                  <Button variant="secondary" disabled={busy} onClick={save}>
                    <Save size={15} />
                    {busy ? "Working…" : "Save Draft"}
                  </Button>
                  <Button
                    disabled={
                      busy ||
                      dirty ||
                      unmarked > 0 ||
                      data.staleRecords > 0 ||
                      data.session.sessionDate > localDate()
                    }
                    title={
                      dirty
                        ? "Save the draft before finalizing"
                        : unmarked
                          ? "Mark every eligible student first"
                          : undefined
                    }
                    onClick={() => setConfirming(true)}
                  >
                    <CheckCircle2 size={15} />
                    Finalize Attendance
                  </Button>
                </div>
              )}
            </Card>
          </>
        )
      )}
      {confirming && (
        <ConfirmDialog
          title="Finalize attendance?"
          busy={busy}
          onClose={() => setConfirming(false)}
          onConfirm={finalize}
          confirmLabel="Finalize attendance"
        >
          All eligible students are marked. Finalization locks this class
          register against ordinary changes.
        </ConfirmDialog>
      )}
      <Toast message={notice} onDismiss={() => setNotice("")} />
    </>
  );
}
