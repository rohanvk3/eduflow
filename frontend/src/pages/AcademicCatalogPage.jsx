import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import {
  Plus,
  Pencil,
  Power,
  BookOpen,
  Layers,
  ArrowUpRight,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getCourses,
  getBatches,
  setCourseStatus,
} from "../services/academicService";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorNotice,
  Input,
  Modal,
  Pagination,
  Select,
  Table,
  TableSkeleton,
  Toast,
  dateLabel,
} from "../components/UI";
import AcademicForm from "../components/AcademicForm";

export default function AcademicCatalogPage({ kind }) {
  const batch = kind === "batches";
  const title = batch ? "Batches" : "Courses";
  const { user } = useAuth();
  const admin = user.role === "ADMIN";
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [target, setTarget] = useState(null);
  const [actionError, setActionError] = useState("");
  const allowed = ["ADMIN", "TEACHER", "STUDENT"].includes(user.role);
  useEffect(() => {
    setSearch("");
    setQuery("");
    setStatus("");
    setPage(0);
    setData(null);
  }, [kind]);
  useEffect(() => {
    if (search === query) return;
    const timer = setTimeout(() => {
      setQuery(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search, query]);
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    (batch ? getBatches : getCourses)(
      { search: query, page, size, status: status || undefined },
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
  }, [allowed, batch, query, page, size, status, reload]);
  if (!allowed)
    return (
      <ErrorNotice message="Your role does not have academic workspace access." />
    );
  const toggle = async () => {
    setBusy(true);
    setActionError("");
    try {
      await setCourseStatus(target.id, target.status !== "ACTIVE");
      setTarget(null);
      setNotice("Course status updated.");
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
          <h1 className="page-title">{title}</h1>
          <p className="mt-2 text-[13px] text-slate-500">
            {batch
              ? "Organize batches, faculty, enrollment history and weekly timetables."
              : "Manage your institute’s course catalog and learning programs."}
          </p>
        </div>
        <div className="flex gap-2">
          {batch && user.role === "STUDENT" && (
            <Link className="button-secondary" to="/batches/my-enrollments">
              My enrollment history
            </Link>
          )}
          {admin && (
            <Button onClick={() => setEditing({})}>
              <Plus size={16} />
              Add {batch ? "batch" : "course"}
            </Button>
          )}
        </div>
      </div>
      <Card className="overflow-hidden">
        <div className="flex flex-wrap gap-3 p-5">
          <Input
            type="search"
            aria-label={`Search ${title.toLowerCase()}`}
            className="max-w-lg"
            placeholder={`Search ${title.toLowerCase()} by name or code…`}
            maxLength={100}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {(batch || admin) && (
            <Select
              aria-label="Filter status"
              className="!w-auto"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All statuses</option>
              {(batch
                ? ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"]
                : ["ACTIVE", "INACTIVE"]
              ).map((s) => (
                <option key={s}>{s}</option>
              ))}
            </Select>
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
          (!data?.content.length ? (
            <EmptyState
              icon={batch ? Layers : BookOpen}
              title={`No ${title.toLowerCase()} found`}
              description={
                batch
                  ? "Your authorized batches will appear here."
                  : "Add a course or adjust your search."
              }
            />
          ) : (
            <Table label={title} className="min-w-[650px]">
              <thead>
                <tr>
                  <th scope="col">Code</th>
                  <th scope="col">Name</th>
                  <th scope="col">{batch ? "Course" : "Duration"}</th>
                  {batch && (
                    <>
                      <th scope="col">Seats</th>
                      <th scope="col">Dates</th>
                    </>
                  )}
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id}>
                    <td className="font-mono text-xs">{r.code}</td>
                    <td>
                      <Link
                        className="font-medium hover:text-indigo-600"
                        to={`/${kind}/${r.id}`}
                      >
                        {r.name}
                      </Link>
                    </td>
                    <td className="text-xs text-slate-500">
                      {batch ? r.courseName : `${r.durationWeeks} weeks`}
                    </td>
                    {batch && (
                      <>
                        <td className="text-xs">
                          {r.activeEnrollments} / {r.capacity}
                        </td>
                        <td className="whitespace-nowrap text-xs">
                          {dateLabel(r.startDate)} – {dateLabel(r.endDate)}
                        </td>
                      </>
                    )}
                    <td>
                      <Badge
                        tone={r.status === "ACTIVE" ? "success" : "neutral"}
                      >
                        {r.status}
                      </Badge>
                    </td>
                    <td>
                      <div className="flex">
                        <Link
                          className="icon-button"
                          aria-label={`View ${r.name}`}
                          to={`/${kind}/${r.id}`}
                        >
                          <ArrowUpRight size={16} />
                        </Link>
                        {admin &&
                          (!batch ||
                            !["COMPLETED", "CANCELLED"].includes(r.status)) && (
                            <Button
                              variant="icon"
                              aria-label={`Edit ${r.name}`}
                              onClick={() => setEditing(r)}
                            >
                              <Pencil size={15} />
                            </Button>
                          )}
                        {admin && !batch && (
                          <Button
                            variant="icon"
                            aria-label={`Change ${r.name} status`}
                            onClick={() => {
                              setTarget(r);
                              setActionError("");
                            }}
                          >
                            <Power size={15} />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ))
        )}
        <Pagination
          noun={kind}
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
      {editing && (
        <Modal
          title={`${editing.id ? "Edit" : "Add"} ${batch ? "batch" : "course"}`}
          busy={busy}
          onClose={() => setEditing(null)}
        >
          <AcademicForm
            kind={kind}
            record={editing}
            onBusyChange={setBusy}
            onClose={() => setEditing(null)}
            onSaved={() => {
              setEditing(null);
              setNotice(`${batch ? "Batch" : "Course"} saved.`);
              setReload((r) => r + 1);
            }}
          />
        </Modal>
      )}
      {target && (
        <ConfirmDialog
          title="Change course status?"
          error={actionError}
          busy={busy}
          onClose={() => setTarget(null)}
          onConfirm={toggle}
        >
          This updates the catalog status. Existing batches and records are
          retained.
        </ConfirmDialog>
      )}
      <Toast message={notice} onDismiss={() => setNotice("")} />
    </>
  );
}
