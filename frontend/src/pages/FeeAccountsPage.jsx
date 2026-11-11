import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ErrorNotice,
  Input,
  Modal,
  Pagination,
  Select,
  TableSkeleton,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  AccountTable,
  Field,
  MoneyInput,
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  useFinanceLoad,
} from "../components/FinanceUI";
import {
  getFeeAccounts,
  createFeeAccount,
  financeError,
} from "../services/financeService";
import { getBatch } from "../services/academicService";
import { localDate } from "../components/AttendanceUI";
export default function FeeAccountsPage() {
  const [params] = useSearchParams();
  const { user } = useAuth(),
    allowed = user.role === "ADMIN";
  const [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [course, setCourse] = useState(""),
    [batch, setBatch] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [creating, setCreating] = useState(false);
  const state = useFinanceLoad(
    (signal) =>
      getFeeAccounts(
        {
          search,
          status: status || undefined,
          courseId: course || undefined,
          batchId: batch || undefined,
          page,
          size,
        },
        signal,
      ),
    [search, status, course, batch, page, size],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  return (
    <>
      <FinanceHeading
        title="Fee Accounts"
        subtitle="Agreed fees, installment schedules, and balances for every enrollment."
      >
        <Button onClick={() => setCreating(true)}>
          <Plus size={16} />
          Create fee account
        </Button>
      </FinanceHeading>
      <Card className="mb-6 grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-4">
        <Field label="Search students">
          <Input
            value={search}
            placeholder="Name or admission number…"
            maxLength={100}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </Field>
        <Field label="Account status">
          <Select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            <option>ACTIVE</option>
            <option>CLOSED</option>
            <option>CANCELLED</option>
          </Select>
        </Field>
        <RecordSelect
          endpoint="/courses"
          label="Course"
          status=""
          required={false}
          value={course}
          onChange={(v) => {
            setCourse(v);
            setPage(0);
          }}
        />
        <RecordSelect
          endpoint="/batches"
          label="Batch"
          status=""
          required={false}
          value={batch}
          onChange={(v) => {
            setBatch(v);
            setPage(0);
          }}
        />
      </Card>
      <FinanceError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : (
          <AccountTable
            rows={state.data?.content}
            recordPayment={params.has("recordPayment")}
          />
        )}
        <Pagination
          noun="fee accounts"
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
      {creating && <CreateAccount onClose={() => setCreating(false)} />}
    </>
  );
}
function CreateAccount({ onClose }) {
  const navigate = useNavigate();
  const [batchId, setBatch] = useState(""),
    [enrollmentId, setEnrollment] = useState(""),
    [structureId, setStructure] = useState(""),
    [discount, setDiscount] = useState("0"),
    [date, setDate] = useState(localDate),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const batch = useFinanceLoad(
    (signal) => getBatch(batchId, signal),
    [batchId],
    !!batchId,
  );
  return (
    <Modal
      title="Create student fee account"
      description="Snapshots the agreed course fees for an existing active enrollment."
      busy={busy}
      onClose={onClose}
    >
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            const { data } = await createFeeAccount({
              enrollmentId: Number(enrollmentId),
              feeStructureId: Number(structureId),
              discountAmount: discount,
              agreementDate: date,
            });
            navigate(`/finance/accounts/${data.account.id}`);
          } catch (e) {
            setError(await financeError(e));
          } finally {
            setBusy(false);
          }
        }}
        className="space-y-5 p-6"
      >
        <ErrorNotice message={error} />
        <fieldset disabled={busy} className="space-y-5">
          <RecordSelect
            endpoint="/batches"
            status=""
            label="Batch"
            value={batchId}
            onChange={(v) => {
              setBatch(v);
              setEnrollment("");
              setStructure("");
            }}
          />
          <FinanceError state={batch} />
          {batch.data && (
            <>
              <RecordSelect
                key={batchId + "enrollment"}
                endpoint={`/batches/${batchId}/enrollments`}
                label="Student enrollment"
                value={enrollmentId}
                onChange={setEnrollment}
              />
              <RecordSelect
                key={batchId + "structure"}
                endpoint={`/fees/structures?courseId=${batch.data.courseId}`}
                label="Fee structure"
                value={structureId}
                onChange={setStructure}
              />
            </>
          )}
          <Field label="Agreement date *">
            <Input
              type="date"
              required
              max={localDate()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Field>
          <Field label="Agreed discount (INR) *">
            <MoneyInput
              value={discount}
              onChange={(e) => setDiscount(e.target.value)}
            />
          </Field>
        </fieldset>
        <p className="text-xs leading-6 text-slate-500">
          The selected structure must apply on the agreement date. A later
          enrollment withdrawal preserves this fee account and all payment
          history.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={busy || !enrollmentId || !structureId}
          >
            {busy ? "Creating…" : "Create fee account"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
