import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import { useAuth } from "../auth/AuthContext";
import { getMyEnrollments } from "../services/academicService";
import { errorMessage } from "../api/client";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Pagination,
  Table,
  TableSkeleton,
  dateLabel,
} from "../components/UI";
export default function MyEnrollmentsPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  useEffect(() => {
    if (user.role !== "STUDENT") return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    getMyEnrollments({ page, size }, c.signal)
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
  }, [user.role, page, size, reload]);
  if (user.role !== "STUDENT")
    return <ErrorNotice message="This page is for student accounts." />;
  return (
    <>
      <h1 className="page-title mb-2">My enrollment history</h1>
      <p className="mb-6 text-sm text-slate-500">
        Your batch memberships and historical enrollment dates.
      </p>
      <ErrorNotice message={error} />
      {error && (
        <Button onClick={() => setReload((r) => r + 1)}>Try again</Button>
      )}
      <Card className="overflow-hidden">
        {loading ? (
          <TableSkeleton />
        ) : (
          !error &&
          (!data?.content.length ? (
            <EmptyState
              title="No enrollments linked yet"
              description="Your administrator can link your student record and enroll you into a batch."
            />
          ) : (
            <Table label="My enrollment history" className="min-w-[600px]">
              <thead>
                <tr>
                  <th>Batch</th>
                  <th>Enrolled</th>
                  <th>Withdrawn</th>
                  <th>Completed</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((e) => (
                  <tr key={e.id}>
                    <td>
                      <Link
                        to={`/batches/${e.batchId}`}
                        className="text-indigo-600"
                      >
                        {e.batchName}
                      </Link>
                    </td>
                    <td>{dateLabel(e.enrollmentDate)}</td>
                    <td>{dateLabel(e.withdrawalDate)}</td>
                    <td>{dateLabel(e.completionDate)}</td>
                    <td>
                      <Badge>{e.status}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ))
        )}
        <Pagination
          noun="enrollment periods"
          page={page}
          size={size}
          total={data?.totalElements}
          totalPages={data?.totalPages}
          loading={loading || !!error}
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
