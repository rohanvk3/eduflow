import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Input,
  Select,
  Pagination,
  TableSkeleton,
  Toast,
} from "../components/UI";
import { DateRange } from "../components/AttendanceUI";
import {
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  Field,
  PaymentsTable,
  useFinanceLoad,
} from "../components/FinanceUI";
import { VoidPaymentDialog } from "../components/FeeAccountActions";
import { getPayments } from "../services/financeService";
import { financeRange } from "./FinancePage";
export default function FinancePaymentsPage() {
  const { user } = useAuth(),
    allowed = user.role === "ADMIN";
  const [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [range, setRange] = useState(financeRange),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [target, setTarget] = useState(null),
    [toast, setToast] = useState("");
  const state = useFinanceLoad(
    (signal) =>
      getPayments(
        { ...range, search, status: status || undefined, page, size },
        signal,
      ),
    [range.fromDate, range.toDate, search, status, page, size],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  return (
    <>
      <FinanceHeading
        title="Payments"
        subtitle="Verified payment records, receipts, and audited voids."
      >
        <Link className="button-primary" to="/finance/accounts?recordPayment=1">
          Record payment
        </Link>
      </FinanceHeading>
      <Card className="mb-6 space-y-4 p-5">
        <DateRange
          value={range}
          onChange={(v) => {
            setRange(v);
            setPage(0);
          }}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Search payments">
            <Input
              value={search}
              maxLength={100}
              placeholder="Receipt, student, or admission number…"
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </Field>
          <Field label="Status">
            <Select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All payments</option>
              <option>SUCCESS</option>
              <option>VOIDED</option>
            </Select>
          </Field>
        </div>
      </Card>
      <FinanceError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : (
          <PaymentsTable rows={state.data?.content} admin onVoid={setTarget} />
        )}
        <Pagination
          noun="payments"
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
        <VoidPaymentDialog
          payment={target}
          onClose={() => setTarget(null)}
          onSuccess={(message) => {
            setTarget(null);
            setToast(message);
            state.reload();
          }}
        />
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
