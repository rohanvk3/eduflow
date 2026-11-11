import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Input,
  Pagination,
  Select,
  Table,
  TableSkeleton,
  Toast,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  FinanceStatus,
  inr,
  useFinanceLoad,
} from "../components/FinanceUI";
import {
  deactivateFeeStructure,
  getFeeStructures,
  financeError,
} from "../services/financeService";
export default function FeeStructuresPage() {
  const { user } = useAuth(),
    allowed = user.role === "ADMIN";
  const [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [course, setCourse] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const [target, setTarget] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState("");
  const state = useFinanceLoad(
    (signal) =>
      getFeeStructures(
        {
          search,
          status: status || undefined,
          courseId: course || undefined,
          page,
          size,
        },
        signal,
      ),
    [search, status, course, page, size],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  return (
    <>
      <FinanceHeading
        title="Fee Structures"
        subtitle="Define course fees and preserve historical fee agreements."
      >
        <Link to="/finance/structures/new" className="button-primary">
          <Plus size={16} />
          New fee structure
        </Link>
      </FinanceHeading>
      <Card className="mb-6 p-5">
        <div className="grid items-start gap-4 md:grid-cols-3">
          <label className="field-label">
            Search
            <Input
              className="mt-2"
              placeholder="Search fee structures…"
              value={search}
              maxLength={100}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </label>
          <label className="field-label">
            Status
            <Select
              className="mt-2"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All statuses</option>
              <option>ACTIVE</option>
              <option>INACTIVE</option>
            </Select>
          </label>
          <RecordSelect
            label="Course"
            endpoint="/courses"
            status=""
            required={false}
            value={course}
            onChange={(v) => {
              setCourse(v);
              setPage(0);
            }}
          />
        </div>
      </Card>
      <FinanceError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : !state.data?.content.length ? (
          <EmptyState
            title="No fee structures"
            description="Add a fee structure for a course before creating student fee accounts."
          />
        ) : (
          <Table label="Fee structures" className="min-w-[800px]">
            <thead>
              <tr>
                <th>Name / Course</th>
                <th>Tuition</th>
                <th>Admission</th>
                <th>Material / Other</th>
                <th>Total</th>
                <th>Effective dates</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {state.data.content.map((f) => (
                <tr key={f.id}>
                  <td>
                    <p className="font-medium">{f.name}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {f.courseName}
                    </p>
                  </td>
                  <td>{inr(f.tuitionFee)}</td>
                  <td>{inr(f.admissionFee)}</td>
                  <td>
                    {inr(f.materialFee)} / {inr(f.otherFee)}
                  </td>
                  <td className="font-medium">{inr(f.totalFee)}</td>
                  <td className="text-xs">
                    {f.effectiveFrom}
                    <br />
                    {f.effectiveTo || "Ongoing"}
                  </td>
                  <td>
                    <FinanceStatus status={f.status} />
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <Link
                        className="button-ghost"
                        to={`/finance/structures/${f.id}/edit`}
                      >
                        Edit
                      </Link>
                      {f.status === "ACTIVE" && (
                        <Button
                          variant="ghost"
                          onClick={() => {
                            setTarget(f);
                            setError("");
                          }}
                        >
                          Deactivate
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        <Pagination
          noun="fee structures"
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
      {target && (
        <ConfirmDialog
          title="Deactivate fee structure?"
          busy={busy}
          error={error}
          onClose={() => setTarget(null)}
          onConfirm={async () => {
            setBusy(true);
            try {
              await deactivateFeeStructure(target.id);
              setTarget(null);
              setToast(
                "Fee structure deactivated. Existing agreements are preserved.",
              );
              state.reload();
            } catch (e) {
              setError(await financeError(e));
            } finally {
              setBusy(false);
            }
          }}
          confirmLabel="Deactivate"
        >
          Existing student fee accounts and payments will remain unchanged.
        </ConfirmDialog>
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
