import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Input,
  Loading,
  Pagination,
  Select,
  Table,
  TableSkeleton,
} from "../components/UI";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  SubmissionTable,
  Field,
  staffRole,
  useAssignmentLoad,
  pct,
} from "../components/AssignmentUI";
import { getAssignment } from "../services/assignmentService";
import {
  getSubmissions,
  getPendingRecipients,
} from "../services/submissionService";
export default function AssignmentReviewPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role);
  const [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const assignment = useAssignmentLoad(
    (signal) => getAssignment(id, signal),
    [id],
    allowed,
  );
  const state = useAssignmentLoad(
    (signal) =>
      status === "PENDING"
        ? getPendingRecipients(id, { page, size, search }, signal)
        : getSubmissions(
            id,
            { page, size, search, status: status || undefined },
            signal,
          ),
    [id, page, size, search, status],
    allowed,
  );
  if (!allowed) return <AssignmentDenied />;
  if (assignment.loading) return <Loading label="Loading review…" />;
  if (assignment.error) return <AssignmentError state={assignment} />;
  if (!assignment.data) return null;
  const a = assignment.data;
  return (
    <>
      <AssignmentHeading
        title={a.title + " · Submissions"}
        subtitle={a.batchName + " · Review student work and publish feedback."}
      >
        <Link className="button-secondary" to={`/assignments/${id}`}>
          Back to assignment
        </Link>
      </AssignmentHeading>
      <div className="mb-6 grid grid-cols-2 gap-4 xl:grid-cols-6">
        {[
          ["Eligible", a.counts.eligible],
          ["Submitted", a.counts.submitted],
          ["Pending", a.counts.pending],
          ["Late", a.counts.late],
          ["Graded", a.counts.graded],
          ["Completion", pct(a.counts.completionPercentage)],
        ].map(([label, value]) => (
          <Card key={label} className="p-5">
            <p className="text-xs text-slate-500">{label}</p>
            <p className="mt-2 text-2xl font-semibold">{value}</p>
          </Card>
        ))}
      </div>
      <Card className="mb-6 grid gap-4 p-5 sm:grid-cols-2">
        <Field label="Search students">
          <Input
            placeholder="Name or admission number…"
            value={search}
            maxLength={100}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </Field>
        <Field label="Submission status">
          <Select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All submitted work</option>
            {["PENDING", "SUBMITTED", "LATE", "GRADED"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        </Field>
      </Card>
      <AssignmentError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : state.error ? null : status === "PENDING" ? (
          <Table label="Pending students">
            <thead>
              <tr>
                <th>Admission number</th>
                <th>Student</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {state.data?.content.map((s) => (
                <tr key={s.studentId}>
                  <td>{s.admissionNumber}</td>
                  <td>{s.studentName}</td>
                  <td>Not submitted</td>
                </tr>
              ))}
              {!state.data?.content.length && (
                <tr>
                  <td colSpan={3}>No pending students.</td>
                </tr>
              )}
            </tbody>
          </Table>
        ) : (
          <SubmissionTable rows={state.data?.content} staff />
        )}
        <Pagination
          noun={status === "PENDING" ? "students" : "submissions"}
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
