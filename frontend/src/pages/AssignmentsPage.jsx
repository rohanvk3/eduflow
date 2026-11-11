import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  EmptyState,
  Input,
  Pagination,
  Select,
  Table,
  TableSkeleton,
  Badge,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  AssignmentStats,
  Field,
  staffRole,
  useAssignmentLoad,
  useAssignmentStats,
  assignmentTime,
} from "../components/AssignmentUI";
import { getAssignments } from "../services/assignmentService";
export default function AssignmentsPage() {
  const [params, setParams] = useSearchParams();
  const pendingOnly = params.get("pendingOnly") === "true";
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const [search, setSearch] = useState(""),
    [batchId, setBatch] = useState(""),
    [status, setStatus] = useState(""),
    [fromDate, setFrom] = useState(""),
    [toDate, setTo] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const state = useAssignmentLoad(
    (signal) =>
      getAssignments(
        {
          search,
          batchId: batchId || undefined,
          status: status || undefined,
          fromDate: fromDate || undefined,
          toDate: toDate || undefined,
          page,
          size,
          pendingOnly: !staff && pendingOnly,
        },
        signal,
      ),
    [search, batchId, status, fromDate, toDate, page, size, pendingOnly],
    allowed,
  );
  const stats = useAssignmentStats(batchId, allowed);
  if (!allowed) return <AssignmentDenied />;
  return (
    <>
      <AssignmentHeading
        title="Assignments"
        subtitle="Plan homework, track submissions, and give meaningful feedback."
      >
        {staff ? (
          <Link className="button-primary" to="/assignments/new">
            <Plus size={16} />
            Create Assignment
          </Link>
        ) : (
          <Link className="button-secondary" to="/my-assignments">
            My homework dashboard
          </Link>
        )}
      </AssignmentHeading>
      <AssignmentError state={stats} />
      <AssignmentStats state={stats} student={!staff} />
      <Card className="mb-6 grid items-start gap-4 p-5 sm:grid-cols-2 xl:grid-cols-5">
        <Field label="Search assignments">
          <Input
            value={search}
            maxLength={100}
            placeholder="Title…"
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </Field>
        <RecordSelect
          label="Batch"
          endpoint="/batches"
          status=""
          required={false}
          value={batchId}
          onChange={(v) => {
            setBatch(v);
            setPage(0);
          }}
        />
        <Field label="Status">
          <Select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            {(staff
              ? ["DRAFT", "PUBLISHED", "CLOSED"]
              : ["PUBLISHED", "CLOSED"]
            ).map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        </Field>
        <Field label="Due from (IST)">
          <Input
            type="date"
            value={fromDate}
            onChange={(e) => {
              setFrom(e.target.value);
              setPage(0);
            }}
          />
        </Field>
        <Field label="Due to (IST)">
          <Input
            type="date"
            value={toDate}
            onChange={(e) => {
              setTo(e.target.value);
              setPage(0);
            }}
          />
        </Field>
      </Card>
      {!staff && (
        <label className="mb-5 flex items-center gap-3 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={pendingOnly}
            onChange={(e) => {
              setParams({ pendingOnly: String(e.target.checked) });
              setPage(0);
            }}
          />
          Show only pending homework
        </label>
      )}
      <AssignmentError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : state.error ? null : !state.data?.content.length ? (
          <EmptyState
            title="No assignments found"
            description={
              staff
                ? "Create a draft assignment for one of your batches."
                : "Published homework assigned to you will appear here."
            }
          />
        ) : (
          <Table label="Assignments" className="min-w-[900px]">
            <thead>
              <tr>
                <th>Assignment</th>
                <th>Batch / Course</th>
                <th>Created by</th>
                <th>Due (IST)</th>
                <th>Submissions</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {state.data.content.map((a) => (
                <tr key={a.id}>
                  <td className="font-medium">
                    {a.title}
                    <p className="mt-1 text-xs font-normal text-slate-500">
                      {a.maxMarks} marks
                    </p>
                  </td>
                  <td>
                    {a.batchName}
                    <p className="mt-1 text-xs text-slate-500">
                      {a.courseName}
                    </p>
                  </td>
                  <td>{a.createdBy}</td>
                  <td className="text-xs">{assignmentTime(a.dueDateTime)}</td>
                  <td>
                    {staff
                      ? a.counts.submitted + " / " + a.counts.eligible
                      : a.ownSubmissionStatus || "Pending"}
                  </td>
                  <td>
                    <Badge
                      tone={a.status === "PUBLISHED" ? "indigo" : "neutral"}
                    >
                      {a.status}
                    </Badge>
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <Link
                        className="button-secondary"
                        to={`/assignments/${a.id}`}
                      >
                        View
                      </Link>
                      {staff && a.status === "DRAFT" && (
                        <Link
                          className="button-ghost"
                          to={`/assignments/${a.id}/edit`}
                        >
                          Edit
                        </Link>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        <Pagination
          noun="assignments"
          page={page}
          size={size}
          total={state.data?.totalElements}
          totalPages={state.data?.totalPages}
          loading={state.loading}
          onPageChange={setPage}
          onSizeChange={(v) => {
            setSize(v);
            setPage(0);
          }}
        />
      </Card>
    </>
  );
}
