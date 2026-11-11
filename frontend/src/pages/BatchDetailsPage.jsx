import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axios from "axios";
import { Plus, Pencil, UserMinus, ArrowLeft, CalendarDays } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import * as service from "../services/academicService";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorNotice,
  Loading,
  Modal,
  Pagination,
  Table,
  Toast,
  dateLabel,
} from "../components/UI";
import AcademicForm from "../components/AcademicForm";
import BatchOperationForm, { weekdays } from "../components/BatchOperationForm";

export default function BatchDetailsPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const admin = user.role === "ADMIN";
  const rosterAllowed = admin || user.role === "TEACHER";
  const allowed = ["ADMIN", "TEACHER", "STUDENT"].includes(user.role);
  const [batch, setBatch] = useState(null);
  const [teachers, setTeachers] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [roster, setRoster] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [operation, setOperation] = useState(null);
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState(null);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    service
      .getBatch(id, c.signal)
      .then(async ({ data }) => {
        if (c.signal.aborted) return;
        const [a, s, r] = await Promise.all([
          service.getAssignments(id, c.signal),
          service.getSchedules(id, c.signal),
          rosterAllowed
            ? service.getEnrollments(id, { page, size }, c.signal)
            : Promise.resolve({ data: null }),
        ]);
        if (!c.signal.aborted) {
          setBatch(data);
          setTeachers(a.data);
          setSchedules(s.data);
          setRoster(r.data);
        }
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [id, allowed, rosterAllowed, page, size, reload]);
  if (!allowed)
    return <ErrorNotice message="Your role does not have batch access." />;
  const editable =
    admin && batch && !["COMPLETED", "CANCELLED"].includes(batch.status);
  const saved = () => {
    setOperation(null);
    setEditing(false);
    setNotice("Batch workspace updated.");
    setReload((r) => r + 1);
  };
  const confirm = async () => {
    if (busy) return;
    setBusy(true);
    setActionError("");
    try {
      if (target.action === "unassign")
        await service.unassignTeacher(id, target.teacherId);
      else
        await service.withdrawStudent(
          id,
          target.id,
          new Date().toLocaleDateString("en-CA"),
        );
      setTarget(null);
      saved();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <Link
        className="mb-5 inline-flex items-center gap-2 text-xs text-slate-500"
        to="/batches"
      >
        <ArrowLeft size={14} />
        Back to batches
      </Link>
      <ErrorNotice message={error} />
      {error && (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Try again
        </Button>
      )}
      {loading ? (
        <Loading label="Loading batch workspace…" />
      ) : (
        !error &&
        batch && (
          <>
            <div className="mb-6 flex flex-wrap justify-between gap-4">
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="page-title">{batch.name}</h1>
                  <Badge
                    tone={batch.status === "ACTIVE" ? "success" : "neutral"}
                  >
                    {batch.status}
                  </Badge>
                </div>
                <p className="mt-2 text-sm text-slate-500">
                  {batch.code} · {batch.courseName}
                </p>
              </div>
              {editable && (
                <Button variant="secondary" onClick={() => setEditing(true)}>
                  <Pencil size={15} />
                  Edit batch
                </Button>
              )}
            </div>
            <Card className="mb-6 flex flex-wrap gap-x-10 gap-y-4 p-5">
              <div>
                <p className="eyebrow mb-2">Dates</p>
                <p className="text-sm">
                  {dateLabel(batch.startDate)} – {dateLabel(batch.endDate)}
                </p>
              </div>
              <div>
                <p className="eyebrow mb-2">Capacity</p>
                <p className="text-sm">
                  {batch.activeEnrollments} active / {batch.capacity} seats
                </p>
              </div>
              <div>
                <p className="eyebrow mb-2">Institute timezone</p>
                <p className="text-sm">Asia/Kolkata</p>
              </div>
            </Card>
            <Card className="mb-6 overflow-hidden">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5">
                <h2 className="text-sm font-semibold">Assigned teachers</h2>
                {editable && (
                  <Button
                    variant="secondary"
                    onClick={() => setOperation({ action: "assign" })}
                  >
                    <Plus size={14} />
                    Assign teacher
                  </Button>
                )}
              </div>
              {!teachers.length ? (
                <EmptyState
                  title="No teachers assigned"
                  description="An administrator can assign existing faculty to this batch."
                />
              ) : (
                <Table label="Batch teachers">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Employee ID</th>
                      <th>Status</th>
                      {editable && <th>Actions</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {teachers.map((t) => (
                      <tr key={t.id}>
                        <td>{t.fullName}</td>
                        <td className="font-mono text-xs">{t.employeeId}</td>
                        <td>
                          <Badge tone={t.active ? "success" : "neutral"}>
                            {t.active ? "Assigned" : "Unassigned"}
                          </Badge>
                        </td>
                        {editable && (
                          <td>
                            {t.active && (
                              <Button
                                variant="ghost"
                                aria-label={`Unassign ${t.fullName}`}
                                onClick={() => {
                                  setTarget({ ...t, action: "unassign" });
                                  setActionError("");
                                }}
                              >
                                <UserMinus size={14} />
                                Unassign
                              </Button>
                            )}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Card>
            <Card className="mb-6">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5">
                <div>
                  <h2 className="flex items-center gap-2 text-sm font-semibold">
                    <CalendarDays size={16} className="text-indigo-500" />
                    Weekly timetable
                  </h2>
                  <p className="mt-1 text-xs text-slate-500">
                    Recurring classes within the batch dates · 24-hour local
                    time
                  </p>
                </div>
                {editable && (
                  <Button
                    variant="secondary"
                    disabled={!teachers.some((t) => t.active)}
                    onClick={() => setOperation({ action: "schedule" })}
                  >
                    <Plus size={14} />
                    Add class
                  </Button>
                )}
              </div>
              <div className="grid gap-3 p-5 sm:grid-cols-2 xl:grid-cols-4">
                {weekdays.map((day) => (
                  <section
                    key={day}
                    className="min-w-0 rounded-lg border border-slate-100 p-3"
                  >
                    <h3 className="mb-3 text-xs font-semibold capitalize text-slate-500">
                      {day.toLowerCase()}
                    </h3>
                    {schedules
                      .filter((s) => s.dayOfWeek === day)
                      .map((s) => (
                        <div
                          key={s.id}
                          className={`mb-2 rounded-lg border p-3 ${s.active ? "border-indigo-100 bg-indigo-50/50" : "border-slate-100 bg-slate-50 opacity-70"}`}
                        >
                          <p className="text-xs font-semibold">
                            {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                          </p>
                          <p className="mt-2 break-words text-xs">
                            {s.teacherName}
                          </p>
                          {s.room && (
                            <p className="mt-1 text-[11px] text-slate-500">
                              {s.room}
                            </p>
                          )}
                          {!s.active && (
                            <Badge className="mt-2">Inactive</Badge>
                          )}
                          {editable && (
                            <Button
                              variant="ghost"
                              className="mt-2 !px-0"
                              aria-label={`Edit ${day} ${s.startTime}`}
                              onClick={() =>
                                setOperation({
                                  action: "schedule",
                                  schedule: s,
                                })
                              }
                            >
                              <Pencil size={12} />
                              Edit class
                            </Button>
                          )}
                        </div>
                      ))}
                    {!schedules.some((s) => s.dayOfWeek === day) && (
                      <p className="text-[11px] text-slate-400">
                        No class scheduled
                      </p>
                    )}
                  </section>
                ))}
              </div>
            </Card>
            {rosterAllowed && (
              <Card className="overflow-hidden">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5">
                  <div>
                    <h2 className="text-sm font-semibold">
                      Student enrollment history
                    </h2>
                    <p className="mt-1 text-xs text-slate-500">
                      Withdrawal is effective today. Historical eligibility is
                      preserved.
                    </p>
                  </div>
                  {editable && (
                    <Button
                      variant="secondary"
                      disabled={batch.activeEnrollments >= batch.capacity}
                      title={
                        batch.activeEnrollments >= batch.capacity
                          ? "Batch is full"
                          : undefined
                      }
                      onClick={() => setOperation({ action: "enroll" })}
                    >
                      <Plus size={14} />
                      Enroll student
                    </Button>
                  )}
                </div>
                {!roster?.content.length ? (
                  <EmptyState
                    title="No enrollments yet"
                    description="Enroll existing active students to reserve their seats."
                  />
                ) : (
                  <Table
                    label="Batch enrollment history"
                    className="min-w-[650px]"
                  >
                    <thead>
                      <tr>
                        <th>Student</th>
                        <th>Admission no.</th>
                        <th>Enrolled</th>
                        <th>Withdrawn / completed</th>
                        <th>Status</th>
                        {admin && <th>Actions</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {roster.content.map((e) => (
                        <tr key={e.id}>
                          <td>{e.fullName}</td>
                          <td
                            className="max-w-40 truncate font-mono text-[10px]"
                            title={e.admissionNumber}
                          >
                            {e.admissionNumber}
                          </td>
                          <td className="whitespace-nowrap text-xs">
                            {dateLabel(e.enrollmentDate)}
                          </td>
                          <td className="text-xs">
                            {dateLabel(e.withdrawalDate || e.completionDate)}
                          </td>
                          <td>
                            <Badge
                              tone={
                                e.status === "ACTIVE" ? "success" : "neutral"
                              }
                            >
                              {e.status}
                            </Badge>
                          </td>
                          {admin && (
                            <td>
                              {e.status === "ACTIVE" && (
                                <Button
                                  variant="ghost"
                                  onClick={() => {
                                    setTarget({ ...e, action: "withdraw" });
                                    setActionError("");
                                  }}
                                >
                                  Withdraw
                                </Button>
                              )}
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                )}
                <Pagination
                  noun="enrollment periods"
                  page={page}
                  size={size}
                  total={roster?.totalElements}
                  totalPages={roster?.totalPages}
                  onPageChange={setPage}
                  onSizeChange={(v) => {
                    setSize(v);
                    setPage(0);
                  }}
                />
              </Card>
            )}
          </>
        )
      )}
      {editing && (
        <Modal title="Edit batch" busy={busy} onClose={() => setEditing(false)}>
          <AcademicForm
            kind="batches"
            record={batch}
            onBusyChange={setBusy}
            onClose={() => setEditing(false)}
            onSaved={saved}
          />
        </Modal>
      )}
      {operation && (
        <Modal
          title={
            operation.action === "assign"
              ? "Assign teacher"
              : operation.action === "enroll"
                ? "Enroll student"
                : operation.schedule
                  ? "Edit weekly class"
                  : "Add weekly class"
          }
          busy={busy}
          onClose={() => setOperation(null)}
        >
          <BatchOperationForm
            {...operation}
            batchId={id}
            assignments={teachers}
            onBusyChange={setBusy}
            onClose={() => setOperation(null)}
            onSaved={saved}
          />
        </Modal>
      )}
      {target && (
        <ConfirmDialog
          title={
            target.action === "unassign"
              ? "Unassign teacher?"
              : "Withdraw student?"
          }
          error={actionError}
          busy={busy}
          onClose={() => setTarget(null)}
          onConfirm={confirm}
        >
          Update {target.fullName}'s membership?{" "}
          {target.action === "unassign"
            ? "Their active timetable entries in this batch will also be deactivated."
            : "Their past enrollment dates remain available; the seat becomes available immediately."}
        </ConfirmDialog>
      )}
      <Toast message={notice} onDismiss={() => setNotice("")} />
    </>
  );
}
