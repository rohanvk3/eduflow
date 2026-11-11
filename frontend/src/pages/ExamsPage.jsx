import { useState } from "react";
import { Link } from "react-router-dom";
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
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ExamHeading,
  ExamBadge,
  ExamDenied,
  ExamError,
  Field,
  examTime,
  staffRole,
  useExamLoad,
} from "../components/ExamUI";
import { getExams } from "../services/examService";
export default function ExamsPage() {
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT";
  const [search, setSearch] = useState(""),
    [batchId, setBatch] = useState(""),
    [examType, setType] = useState(""),
    [status, setStatus] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const state = useExamLoad(
    (signal) =>
      getExams(
        {
          search,
          batchId: batchId || undefined,
          examType: examType || undefined,
          status: status || undefined,
          page,
          size,
        },
        signal,
      ),
    [search, batchId, examType, status, page, size],
    allowed,
  );
  if (!allowed) return <ExamDenied />;
  return (
    <>
      <ExamHeading
        title="Exams"
        subtitle="Plan assessments, conduct online tests, and publish student results."
      >
        {staff && (
          <>
            <Link to="/exams/analytics" className="button-secondary">
              Performance analytics
            </Link>
            <Link to="/exams/new" className="button-primary">
              <Plus size={16} />
              Create Exam
            </Link>
          </>
        )}
        {user.role === "STUDENT" && (
          <Link className="button-secondary" to="/my-results">
            My results
          </Link>
        )}
      </ExamHeading>
      <Card className="mb-6 grid items-start gap-4 p-5 sm:grid-cols-2 xl:grid-cols-4">
        <Field label="Search">
          <Input
            value={search}
            maxLength={100}
            placeholder="Exam title or code…"
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
        <Field label="Exam type">
          <Select
            value={examType}
            onChange={(e) => {
              setType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All types</option>
            <option value="ONLINE_MCQ">Online MCQ</option>
            <option value="OFFLINE">Offline</option>
          </Select>
        </Field>
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
              ? ["DRAFT", "PUBLISHED", "COMPLETED", "CANCELLED"]
              : ["PUBLISHED", "COMPLETED"]
            ).map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        </Field>
      </Card>
      <ExamError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : state.error ? null : !state.data?.content.length ? (
          <EmptyState
            title="No exams found"
            description="Eligible published exams will appear here."
          />
        ) : (
          <Table label="Exams" className="min-w-[900px]">
            <thead>
              <tr>
                <th>Exam</th>
                <th>Batch / Type</th>
                <th>Schedule (IST)</th>
                <th>Duration</th>
                <th>Marks</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {state.data.content.map((e) => (
                <tr key={e.id}>
                  <td>
                    <p className="font-medium">{e.title}</p>
                    <p className="mt-1 text-xs text-slate-500">{e.examCode}</p>
                  </td>
                  <td>
                    {e.batchName}
                    <p className="mt-1 text-xs text-slate-500">
                      {e.examType.replaceAll("_", " ")}
                    </p>
                  </td>
                  <td className="text-xs">
                    {examTime(e.startDateTime)}
                    <p className="mt-1 text-slate-500">
                      to {examTime(e.endDateTime)}
                    </p>
                  </td>
                  <td>
                    {e.durationMinutes ? e.durationMinutes + " min" : "—"}
                  </td>
                  <td>{e.totalMarks}</td>
                  <td>
                    <ExamBadge status={e.status} />
                  </td>
                  <td>
                    <Link className="button-secondary" to={`/exams/${e.id}`}>
                      View
                    </Link>
                    {staff && e.status === "DRAFT" && (
                      <Link className="button-ghost" to={`/exams/${e.id}/edit`}>
                        Edit
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        <Pagination
          noun="exams"
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
