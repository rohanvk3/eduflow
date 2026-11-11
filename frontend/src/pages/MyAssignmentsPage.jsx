import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Card, Pagination, Select, TableSkeleton } from "../components/UI";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  SubmissionTable,
  useAssignmentLoad,
} from "../components/AssignmentUI";
import AssignmentDashboard from "../components/AssignmentDashboard";
import { getMySubmissions } from "../services/submissionService";
export default function MyAssignmentsPage() {
  const { user } = useAuth(),
    allowed = user.role === "STUDENT";
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [status, setStatus] = useState("");
  const state = useAssignmentLoad(
    (signal) =>
      getMySubmissions({ page, size, status: status || undefined }, signal),
    [page, size, status],
    allowed,
  );
  if (!allowed) return <AssignmentDenied />;
  return (
    <>
      <AssignmentHeading
        title="My Assignments"
        subtitle="Stay on top of homework, deadlines, grades, and feedback."
      >
        <Link className="button-primary" to="/assignments?pendingOnly=true">
          View pending homework
        </Link>
      </AssignmentHeading>
      <AssignmentDashboard full />
      <Card className="overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-4 p-5">
          <h2 className="text-sm font-semibold">Assignment history</h2>
          <label className="field-label">
            Submission status
            <Select
              className="mt-2"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All submitted work</option>
              {["SUBMITTED", "LATE", "GRADED"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </Select>
          </label>
        </div>
        <div className="px-5">
          <AssignmentError state={state} />
        </div>
        {state.loading ? (
          <TableSkeleton />
        ) : (
          !state.error && <SubmissionTable rows={state.data?.content} />
        )}
        <Pagination
          noun="submissions"
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
