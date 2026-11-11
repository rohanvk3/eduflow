import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import axios from "axios";
import {
  Users,
  UserRoundCheck,
  Layers,
  Wallet,
  CircleDollarSign,
  ArrowUpRight,
  CalendarDays,
  CalendarCheck,
  ArrowRight,
  Plus,
  Sparkles,
} from "lucide-react";
import { api, errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import {
  Avatar,
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Skeleton,
  StatusBadge,
  Table,
  TableSkeleton,
  dateLabel,
} from "../components/UI";
import FinanceDashboard from "../components/FinanceDashboard";
import { getTeacherStats } from "../services/teacherService";
import { getBatchStats } from "../services/academicService";
import AttendanceDashboard from "../components/AttendanceDashboard";
import ExamDashboard from "../components/ExamDashboard";
import AssignmentDashboard from "../components/AssignmentDashboard";
import ResourceDashboard from "../components/ResourceDashboard";
import NotificationDashboard from "../components/NotificationDashboard";

export default function DashboardPage() {
  const { user } = useAuth();
  const admin = user.role === "ADMIN";
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(admin);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [teacherStats, setTeacherStats] = useState(null);
  const [teacherError, setTeacherError] = useState("");
  const [batchStats, setBatchStats] = useState(null);
  const [batchError, setBatchError] = useState("");
  useEffect(() => {
    if (!admin) return;
    const c = new AbortController();
    setBatchStats(null);
    setBatchError("");
    getBatchStats(c.signal)
      .then(({ data }) => {
        if (!c.signal.aborted) setBatchStats(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setBatchError(errorMessage(e));
      });
    return () => c.abort();
  }, [admin, reload]);
  useEffect(() => {
    if (!admin) return;
    const controller = new AbortController();
    setTeacherStats(null);
    setTeacherError("");
    getTeacherStats(controller.signal)
      .then(({ data }) => {
        if (!controller.signal.aborted) setTeacherStats(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setTeacherError(errorMessage(e));
      });
    return () => controller.abort();
  }, [admin, reload]);
  useEffect(() => {
    if (!admin) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    api
      .get("/students", {
        params: { page: 0, size: 5 },
        signal: controller.signal,
      })
      .then(({ data }) => {
        if (!controller.signal.aborted) setResult(data);
      })
      .catch((failure) => {
        if (!axios.isCancel(failure)) setError(errorMessage(failure));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [admin, reload]);
  if (user.role === "PARENT")
    return <Navigate to="/parent/dashboard" replace />;
  if (!admin)
    return (
      <>
        <div className="mb-7">
          <p className="eyebrow mb-2">Your workspace</p>
          <h1 className="page-title">
            Welcome back, {user.fullName.split(" ")[0]}.
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            Everything you need for your next step.
          </p>
        </div>
        <NotificationDashboard />
        {["ADMIN", "TEACHER"].includes(user.role) ? (
          <div className="mb-5 flex gap-4 text-sm">
            <Link className="text-indigo-600" to="/analytics">
              Advanced Analytics
            </Link>
            <Link className="text-indigo-600" to="/reports">
              Reports Center
            </Link>
          </div>
        ) : user.role === "STUDENT" ? (
          <Link
            className="mb-5 inline-block text-sm text-indigo-600"
            to="/my-progress"
          >
            View My Progress
          </Link>
        ) : null}
        <AssignmentDashboard />
        <ResourceDashboard />
        <ExamDashboard />
        <AttendanceDashboard />
        <Card>
          <EmptyState
            icon={
              user.role === "STUDENT"
                ? Users
                : user.role === "TEACHER"
                  ? UserRoundCheck
                  : Layers
            }
            title={
              user.role === "STUDENT"
                ? "Your student profile"
                : user.role === "TEACHER"
                  ? "Your teaching profile"
                  : "Your workspace is taking shape"
            }
            description={
              user.role === "STUDENT"
                ? "View your admission details, contact information, and guardian information."
                : user.role === "TEACHER"
                  ? "View your faculty details, subjects and contact information."
                  : "Institute-wide reports are available to administrators. Your role-specific tools are not available yet."
            }
            action={
              (user.role === "STUDENT" || user.role === "TEACHER") && (
                <Link
                  to={user.role === "TEACHER" ? "/teachers/me" : "/students/me"}
                  className="button-primary"
                >
                  View my profile
                  <ArrowRight size={15} />
                </Link>
              )
            }
          />
        </Card>
      </>
    );
  return (
    <>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <p className="eyebrow">Workspace overview</p>
            <span className="h-1 w-1 rounded-full bg-slate-300" />
            <span className="text-[10px] text-slate-400">
              {new Intl.DateTimeFormat("en-IN", {
                day: "numeric",
                month: "long",
                year: "numeric",
              }).format(new Date())}
            </span>
          </div>
          <h1 className="page-title">
            Welcome back, {user.fullName.split(" ")[0]}{" "}
            <span className="text-indigo-500">.</span>
          </h1>
          <p className="mt-2 text-[13px] text-slate-500">
            A clear view of your institute. More room to focus on what matters.
          </p>
        </div>
        <Link to="/students" className="button-primary">
          <Users size={16} />
          Manage students
          <ArrowUpRight size={15} />
        </Link>
      </div>
      {error && (
        <>
          <ErrorNotice message={error} />
          <Button
            variant="secondary"
            className="mb-5"
            onClick={() => setReload((r) => r + 1)}
          >
            Retry student overview
          </Button>
        </>
      )}
      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
        <Card className="col-span-2 p-4 sm:col-span-1">
          <div className="mb-5 flex items-center justify-between">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
              <Users size={17} />
            </span>
            <Badge tone="indigo">Live</Badge>
          </div>
          <p className="text-[11px] font-medium text-slate-500">
            Total students
          </p>
          {loading ? (
            <Skeleton className="my-2 h-8 w-20" />
          ) : (
            <p className="mt-1.5 text-[28px] font-semibold tracking-tight tabular-nums">
              {error ? "—" : (result?.totalElements?.toLocaleString() ?? "—")}
            </p>
          )}
          <p className="mt-2 text-[10px] text-slate-400">
            {error ? "Could not load student total" : "All admission records"}
          </p>
        </Card>
        <Card className="p-4">
          <div className="mb-5 flex items-center justify-between">
            <UserRoundCheck size={18} className="text-indigo-500" />
            <Badge tone="indigo">Live</Badge>
          </div>
          <Link
            to="/teachers"
            className="text-[11px] font-medium text-slate-500 hover:text-indigo-600"
          >
            Active teachers
          </Link>
          <p className="mt-1.5 text-[28px] font-semibold">
            {teacherStats?.active ?? "—"}
          </p>
          <p className="mt-2 text-[10px] text-slate-500">
            {teacherError
              ? "Teacher statistics unavailable"
              : teacherStats
                ? `${teacherStats.total} total teachers`
                : "Loading teacher statistics…"}
          </p>
          {teacherError && (
            <Button variant="ghost" onClick={() => setReload((r) => r + 1)}>
              Retry teacher statistics
            </Button>
          )}
        </Card>
        <Card className="p-4">
          <div className="mb-5 flex items-center justify-between">
            <Layers size={18} className="text-indigo-500" />
            <Badge tone="indigo">Live</Badge>
          </div>
          <Link
            className="text-[11px] font-medium text-slate-500 hover:text-indigo-600"
            to="/batches"
          >
            Active batches
          </Link>
          <p className="mt-1.5 text-[28px] font-semibold">
            {batchStats?.active ?? "—"}
          </p>
          <p className="mt-2 text-[10px] text-slate-500">
            {batchError
              ? "Batch statistics unavailable"
              : batchStats
                ? `${batchStats.total} total batches`
                : "Loading batch statistics…"}
          </p>
          {batchError && (
            <Button variant="ghost" onClick={() => setReload((r) => r + 1)}>
              Retry batch statistics
            </Button>
          )}
        </Card>
      </div>
      <NotificationDashboard />
      {["ADMIN", "TEACHER"].includes(user.role) ? (
        <div className="mb-5 flex gap-4 text-sm">
          <Link className="text-indigo-600" to="/analytics">
            Advanced Analytics
          </Link>
          <Link className="text-indigo-600" to="/reports">
            Reports Center
          </Link>
        </div>
      ) : user.role === "STUDENT" ? (
        <Link
          className="mb-5 inline-block text-sm text-indigo-600"
          to="/my-progress"
        >
          View My Progress
        </Link>
      ) : null}
      <AssignmentDashboard />
      <ResourceDashboard />
      <ExamDashboard />
      <div className="mb-6 grid gap-6 xl:grid-cols-3">
        <div className="min-w-0 xl:col-span-2">
          <FinanceDashboard />
        </div>
        <AttendanceDashboard />
      </div>
      <div className="grid gap-6 xl:grid-cols-3">
        <Card className="overflow-hidden xl:col-span-2">
          <div className="flex items-center justify-between gap-3 px-5 py-5">
            <div>
              <h2 className="text-sm font-semibold">Recent admissions</h2>
              <p className="mt-1.5 text-xs text-slate-400">
                Your most recently admitted students
              </p>
            </div>
            <Link
              to="/students"
              className="inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800"
            >
              View all
              <ArrowRight size={14} />
            </Link>
          </div>
          {loading ? (
            <TableSkeleton rows={3} />
          ) : error ? (
            <EmptyState
              compact
              title="Admissions couldn't be loaded"
              description="Retry the student overview to see recent records."
            />
          ) : !result?.content.length ? (
            <EmptyState
              compact
              icon={Users}
              title="Your next chapter starts here"
              description="New admissions will appear as you add students."
              action={
                <Link to="/students" className="button-secondary">
                  <Plus size={14} />
                  Manage admissions
                </Link>
              }
            />
          ) : (
            <Table label="Recent admissions" className="min-w-[500px]">
              <thead>
                <tr>
                  <th scope="col">Student</th>
                  <th scope="col">Admission date</th>
                  <th scope="col">Status</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((student) => (
                  <tr key={student.id}>
                    <td>
                      <div className="flex items-center gap-2.5">
                        <Avatar name={student.fullName} size="sm" />
                        <div>
                          <Link
                            to={`/students/${student.id}`}
                            className="text-xs font-medium hover:text-indigo-600"
                          >
                            {student.fullName}
                          </Link>
                          <p className="mt-1 text-[10px] text-slate-400">
                            {student.email}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="whitespace-nowrap text-xs text-slate-500">
                      {dateLabel(student.admissionDate)}
                    </td>
                    <td>
                      <StatusBadge status={student.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card>
        <Card>
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-5">
            <h2 className="text-sm font-semibold">Upcoming classes</h2>
            <CalendarDays size={17} className="text-slate-400" />
          </div>
          <EmptyState
            compact
            icon={CalendarDays}
            title="Make space for learning"
            description="Your schedule will appear here when class and batch management are available."
          />
          <div className="mx-5 mb-4 flex items-center gap-2 rounded-lg border border-indigo-100 bg-indigo-50/60 p-3">
            <Sparkles size={15} className="shrink-0 text-indigo-500" />
            <p className="text-[10px] leading-5 text-indigo-700">
              Start with your students. More institute tools are on the way.
            </p>
          </div>
        </Card>
      </div>
    </>
  );
}
