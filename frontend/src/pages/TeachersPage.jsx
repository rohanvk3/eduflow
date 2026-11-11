import { useEffect, useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import axios from "axios";
import {
  Plus,
  Search,
  UserRoundCheck,
  Pencil,
  ArrowUpRight,
  Power,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getTeachers,
  activateTeacher,
  deactivateTeacher,
} from "../services/teacherService";
import {
  Avatar,
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorNotice,
  Input,
  Select,
  Pagination,
  StatusBadge,
  Table,
  TableSkeleton,
  Toast,
} from "../components/UI";

export default function TeachersPage() {
  const { user } = useAuth();
  const location = useLocation();
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [sort, setSort] = useState("createdAt,desc");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [target, setTarget] = useState(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState(location.state?.notice || "");
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);
  useEffect(() => {
    if (user.role !== "ADMIN") return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getTeachers(
      { page, size, search: query, status: status || undefined, sort },
      controller.signal,
    )
      .then(({ data }) => {
        if (!controller.signal.aborted) setData(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [user.role, page, size, query, status, sort, reload]);
  if (user.role === "TEACHER") return <Navigate to="/teachers/me" replace />;
  if (user.role !== "ADMIN")
    return (
      <ErrorNotice message="Teacher management is available to administrators only." />
    );
  const toggle = async () => {
    if (busy) return;
    setBusy(true);
    setActionError("");
    try {
      await (target.status === "ACTIVE"
        ? deactivateTeacher(target.id)
        : activateTeacher(target.id));
      setNotice("Teacher status updated.");
      setTarget(null);
      setReload((r) => r + 1);
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Teachers</h1>
          <p className="mt-2 text-[13px] text-slate-500">
            Manage faculty members, specializations, and teaching profiles.
          </p>
        </div>
        <Link className="button-primary" to="/teachers/new">
          <Plus size={16} />
          Add Teacher
        </Link>
      </div>
      <Card className="overflow-hidden">
        <div className="flex items-center gap-3 border-b border-slate-100 p-5">
          <h2 className="text-sm font-semibold">Faculty directory</h2>
          <Badge>
            {loading ? "…" : error ? "Unavailable" : (data?.totalElements ?? 0)}
          </Badge>
        </div>
        <div className="flex flex-wrap gap-3 p-5">
          <label className="relative min-w-0 flex-1 basis-56">
            <span className="sr-only">Search teachers</span>
            <Search
              size={16}
              className="absolute left-3 top-3 text-slate-400"
            />
            <Input
              type="search"
              maxLength={100}
              className="pl-9"
              placeholder="Search name, employee ID, email, specialization…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </label>
          <Select
            aria-label="Teacher status"
            className="!w-auto"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
          <Select
            aria-label="Sort teachers"
            className="!w-auto"
            value={sort}
            onChange={(e) => {
              setSort(e.target.value);
              setPage(0);
            }}
          >
            <option value="createdAt,desc">Newest first</option>
            <option value="fullName,asc">Name A–Z</option>
            <option value="employeeId,asc">Employee ID</option>
            <option value="joiningDate,desc">Joining date</option>
          </Select>
        </div>
        {error && (
          <div className="px-5 pb-5">
            <ErrorNotice message={error} />
            <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
              Try again
            </Button>
          </div>
        )}
        {loading ? (
          <TableSkeleton />
        ) : (
          !error &&
          (!data?.content.length ? (
            <EmptyState
              icon={UserRoundCheck}
              title="No teachers found"
              description="Add your first faculty member or adjust your search and filters."
            />
          ) : (
            <Table label="Teachers" className="min-w-[1050px]">
              <thead>
                <tr>
                  {[
                    "Employee ID",
                    "Teacher name",
                    "Email",
                    "Phone",
                    "Specialization",
                    "Experience",
                    "Status",
                    "Actions",
                  ].map((h) => (
                    <th scope="col" key={h}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.content.map((teacher) => (
                  <tr key={teacher.id}>
                    <td className="font-mono text-xs">{teacher.employeeId}</td>
                    <td>
                      <Link
                        className="flex items-center gap-3 font-medium hover:text-indigo-600"
                        to={`/teachers/${teacher.id}`}
                      >
                        <Avatar name={teacher.fullName} />
                        {teacher.fullName}
                      </Link>
                    </td>
                    <td className="text-xs text-slate-500">{teacher.email}</td>
                    <td className="whitespace-nowrap text-xs">
                      {teacher.phone || "—"}
                    </td>
                    <td className="text-xs">{teacher.specialization || "—"}</td>
                    <td className="text-xs">
                      {teacher.experienceYears == null
                        ? "—"
                        : `${teacher.experienceYears} years`}
                    </td>
                    <td>
                      <StatusBadge status={teacher.status} />
                    </td>
                    <td>
                      <div className="flex">
                        <Link
                          className="icon-button"
                          aria-label={`View ${teacher.fullName}`}
                          to={`/teachers/${teacher.id}`}
                        >
                          <ArrowUpRight size={16} />
                        </Link>
                        <Link
                          className="icon-button"
                          aria-label={`Edit ${teacher.fullName}`}
                          to={`/teachers/${teacher.id}/edit`}
                        >
                          <Pencil size={15} />
                        </Link>
                        <Button
                          variant="icon"
                          aria-label={`${teacher.status === "ACTIVE" ? "Deactivate" : "Activate"} ${teacher.fullName}`}
                          onClick={() => {
                            setTarget(teacher);
                            setActionError("");
                          }}
                        >
                          <Power size={15} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ))
        )}
        <Pagination
          noun="teachers"
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
      <Toast message={notice} onDismiss={() => setNotice("")} />
      {target && (
        <ConfirmDialog
          title={`${target.status === "ACTIVE" ? "Deactivate" : "Activate"} teacher?`}
          busy={busy}
          error={actionError}
          onClose={() => setTarget(null)}
          onConfirm={toggle}
          confirmLabel={
            target.status === "ACTIVE"
              ? "Deactivate teacher"
              : "Activate teacher"
          }
        >
          Update the status of <strong>{target.fullName}</strong>? Their record
          and linked account will be retained.
        </ConfirmDialog>
      )}
    </>
  );
}
