import { useEffect, useState } from "react";
import axios from "axios";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import {
  Plus,
  Search,
  ArrowUpRight,
  Users,
  SlidersHorizontal,
  ArrowDown,
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
  Input,
  Modal,
  Pagination,
  Select,
  StatusBadge,
  Table,
  TableSkeleton,
  Toast,
  dateLabel,
} from "../components/UI";
import StudentForm from "../components/StudentForm";

export default function StudentsPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const urlSearch = params.get("search") || "";
  const [search, setSearch] = useState(urlSearch);
  const [query, setQuery] = useState(urlSearch);
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [adding, setAdding] = useState(false);
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);
  const [success, setSuccess] = useState("");

  useEffect(() => {
    setSearch(urlSearch);
    setQuery(urlSearch);
    setPage(0);
  }, [urlSearch]);
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
    api
      .get("/students", {
        params: { search: query, page, size, status: status || undefined },
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
  }, [query, page, size, status, reload, user.role]);

  if (user.role === "STUDENT") return <Navigate to="/students/me" replace />;
  if (user.role !== "ADMIN")
    return (
      <ErrorNotice message="Student records are available to administrators and linked students." />
    );
  const saved = (student) => {
    setAdding(false);
    setSuccess(`${student.fullName} was added successfully.`);
    setPage(0);
    setReload((r) => r + 1);
  };
  const clear = () => {
    setSearch("");
    setQuery("");
    setStatus("");
    setPage(0);
  };
  const total = result?.totalElements || 0;

  return (
    <>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <h1 className="page-title">Students</h1>
            <Badge tone="indigo">Admissions</Badge>
          </div>
          <p className="text-[13px] leading-6 text-slate-500">
            A little more organized. Every student, every step of the way.
          </p>
        </div>
        <Button
          onClick={() => {
            setAdding(true);
            setSuccess("");
          }}
        >
          <Plus size={16} />
          Add student
        </Button>
      </div>
      <Card className="overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-5">
          <div className="flex items-center gap-2.5">
            <h2 className="text-sm font-semibold text-slate-900">
              Student directory
            </h2>
            <Badge>{loading ? "…" : error ? "Unavailable" : total}</Badge>
          </div>
          <p className="text-xs text-slate-400">
            {query || status
              ? "Filtered admission records"
              : "Your complete admission records"}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3 px-5 py-4">
          <label className="relative min-w-0 flex-1 basis-56">
            <span className="sr-only">Search students</span>
            <Search
              size={16}
              className="pointer-events-none absolute left-3 top-3 text-slate-400"
            />
            <Input
              className="pl-9"
              type="search"
              maxLength={100}
              placeholder="Search name, email, admission no.…"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <label className="relative min-w-36">
            <span className="sr-only">Filter by status</span>
            <SlidersHorizontal
              size={14}
              className="pointer-events-none absolute left-3 top-3.5 text-slate-500"
            />
            <Select
              className="pl-9"
              value={status}
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </Select>
          </label>
          {(search || status) && (
            <Button variant="ghost" onClick={clear}>
              Clear filters
            </Button>
          )}
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
          (!result?.content.length ? (
            <EmptyState
              icon={Users}
              title={
                search || status
                  ? "No students found"
                  : "Your student directory starts here"
              }
              description={
                search || status
                  ? "Try another search or change your filters."
                  : "Add your first student to begin organizing admissions."
              }
              action={
                search || status ? (
                  <Button variant="secondary" onClick={clear}>
                    Clear filters
                  </Button>
                ) : (
                  <Button onClick={() => setAdding(true)}>
                    <Plus size={15} />
                    Add student
                  </Button>
                )
              }
            />
          ) : (
            <Table className="min-w-[800px]">
              <caption className="sr-only">
                Students and admission details
              </caption>
              <thead>
                <tr>
                  <th scope="col">Student</th>
                  <th scope="col">Admission number</th>
                  <th scope="col">Phone</th>
                  <th scope="col">
                    <span className="inline-flex items-center gap-1">
                      Admitted
                      <ArrowDown size={12} />
                    </span>
                  </th>
                  <th scope="col">Status</th>
                  <th scope="col">
                    <span className="sr-only">View profile</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((student) => (
                  <tr key={student.id}>
                    <td>
                      <div className="flex items-center gap-3">
                        <Avatar name={student.fullName} />
                        <div className="min-w-0">
                          <Link
                            className="font-medium text-slate-800 transition-colors hover:text-indigo-600"
                            to={`/students/${student.id}`}
                          >
                            {student.fullName}
                          </Link>
                          <p className="mt-1 text-xs text-slate-400">
                            {student.email}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span
                        title={student.admissionNumber}
                        className="block max-w-36 truncate rounded-md font-mono text-[11px] text-slate-500"
                      >
                        {student.admissionNumber}
                      </span>
                    </td>
                    <td className="whitespace-nowrap text-xs text-slate-500">
                      {student.phone}
                    </td>
                    <td className="whitespace-nowrap text-xs text-slate-500">
                      {dateLabel(student.admissionDate)}
                    </td>
                    <td>
                      <StatusBadge status={student.status} />
                    </td>
                    <td>
                      <Link
                        to={`/students/${student.id}`}
                        className="icon-button"
                        aria-label={`View ${student.fullName}`}
                      >
                        <ArrowUpRight size={17} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ))
        )}
        <Pagination
          page={page}
          size={size}
          total={error ? 0 : total}
          totalPages={error ? 0 : result?.totalPages}
          loading={loading || !!error}
          onPageChange={setPage}
          onSizeChange={(value) => {
            setSize(value);
            setPage(0);
          }}
        />
      </Card>
      <p className="mt-4 flex items-center gap-2 text-[11px] text-slate-400">
        <Users size={13} />
        Student records are visible only to authorized accounts.
      </p>
      <Toast message={success} onDismiss={() => setSuccess("")} />
      {adding && (
        <Modal
          title="Add a student"
          description="Create a new admission record for your institute."
          busy={busy}
          onClose={() => setAdding(false)}
        >
          <StudentForm
            onSaved={saved}
            onClose={() => setAdding(false)}
            onBusyChange={setBusy}
          />
        </Modal>
      )}
    </>
  );
}
