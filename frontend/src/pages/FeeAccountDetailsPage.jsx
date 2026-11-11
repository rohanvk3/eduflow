import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  EmptyState,
  Loading,
  Table,
  Toast,
} from "../components/UI";
import {
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  FinanceStatus,
  PaymentsTable,
  inr,
  useFinanceLoad,
} from "../components/FinanceUI";
import {
  InstallmentScheduleForm,
  EditInstallmentDate,
  RecordPaymentForm,
  VoidPaymentDialog,
} from "../components/FeeAccountActions";
import { getFeeAccount } from "../services/financeService";
export default function FeeAccountDetailsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    admin = user.role === "ADMIN",
    allowed = admin || user.role === "STUDENT";
  const [params] = useSearchParams(),
    [action, setAction] = useState(
      params.has("recordPayment") ? "payment" : "",
    ),
    [selected, setSelected] = useState(null),
    [toast, setToast] = useState("");
  const state = useFinanceLoad(
    (signal) => getFeeAccount(id, signal),
    [id],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  if (state.loading) return <Loading label="Loading fee account…" />;
  if (state.error) return <FinanceError state={state} />;
  if (!state.data) return null;
  const { account: a, installments, payments } = state.data;
  const success = (message) => {
    setAction("");
    setSelected(null);
    setToast(message);
    state.reload();
  };
  const canPay =
    admin &&
    a.status === "ACTIVE" &&
    installments.length > 0 &&
    Number(a.outstandingAmount) > 0;
  return (
    <>
      <FinanceHeading
        title={a.studentName}
        subtitle={`${a.admissionNumber} · ${a.courseName} · ${a.batchName} · Fee account #${a.id}`}
      >
        <Link
          className="button-secondary"
          to={admin ? "/finance/accounts" : "/my-fees"}
        >
          Back to accounts
        </Link>
        {canPay && (
          <Button onClick={() => setAction("payment")}>Record payment</Button>
        )}
      </FinanceHeading>
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        {[
          ["Total payable", a.finalPayableAmount],
          ["Successful payments", a.paidAmount],
          ["Outstanding balance", a.outstandingAmount],
        ].map(([label, value]) => (
          <Card className="p-5" key={label}>
            <p className="text-xs text-slate-500">{label}</p>
            <p className="mt-3 break-words text-2xl font-semibold">
              {inr(value)}
            </p>
          </Card>
        ))}
      </div>
      <div className="mb-6 grid gap-6 md:grid-cols-2">
        <Card className="p-5">
          <h2 className="mb-5 text-sm font-semibold">Agreed fee breakdown</h2>
          <dl className="space-y-3 text-sm">
            {[
              ["Tuition", a.tuitionFee],
              ["Admission", a.admissionFee],
              ["Material", a.materialFee],
              ["Other", a.otherFee],
              ["Original amount", a.originalAmount],
              ["Discount", a.discountAmount],
              ["Final payable", a.finalPayableAmount],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between gap-3">
                <dt className="text-slate-500">{label}</dt>
                <dd className="font-medium">{inr(value)}</dd>
              </div>
            ))}
          </dl>
          <p className="mt-5 text-xs text-slate-500">
            {a.structureName} · Agreed {a.agreementDate}. This snapshot is
            preserved if catalog fees change.
          </p>
        </Card>
        <Card className="p-5">
          <h2 className="mb-5 text-sm font-semibold">Account information</h2>
          <dl className="space-y-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Payment status</dt>
              <dd>
                <FinanceStatus status={a.paymentStatus} />
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Account status</dt>
              <dd>
                <FinanceStatus status={a.status} />
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Enrollment status</dt>
              <dd>{a.enrollmentStatus}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Overdue amount</dt>
              <dd className="font-medium">{inr(a.overdueAmount)}</dd>
            </div>
          </dl>
          <p className="mt-6 text-xs leading-6 text-slate-500">
            Payments are manually verified by institute administrators.
            Withdrawing an enrollment does not cancel its financial agreement.
          </p>
        </Card>
      </div>
      <Card className="mb-6 overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 p-5">
          <h2 className="text-sm font-semibold">Installment schedule</h2>
          {admin &&
            !installments.length &&
            Number(a.finalPayableAmount) > 0 && (
              <Button onClick={() => setAction("schedule")}>
                Create schedule
              </Button>
            )}
        </div>
        {!installments.length ? (
          <EmptyState
            compact
            title={
              Number(a.finalPayableAmount) === 0
                ? "Fully discounted account"
                : "No installment schedule"
            }
            description={
              Number(a.finalPayableAmount) === 0
                ? "No payment is required."
                : "An administrator must create the full installment schedule before recording payments."
            }
          />
        ) : (
          <Table label="Fee installments" className="min-w-[700px]">
            <thead>
              <tr>
                <th>Installment</th>
                <th>Due date</th>
                <th>Amount</th>
                <th>Paid</th>
                <th>Outstanding</th>
                <th>Status</th>
                {admin && <th>Action</th>}
              </tr>
            </thead>
            <tbody>
              {installments.map((i) => (
                <tr key={i.id}>
                  <td>#{i.installmentNumber}</td>
                  <td>{i.dueDate}</td>
                  <td>{inr(i.amount)}</td>
                  <td>{inr(i.paidAmount)}</td>
                  <td>{inr(i.outstandingAmount)}</td>
                  <td>
                    <FinanceStatus status={i.status} />
                  </td>
                  {admin && (
                    <td>
                      {payments.length === 0 ? (
                        <Button
                          variant="ghost"
                          onClick={() => {
                            setSelected(i);
                            setAction("edit");
                          }}
                        >
                          Edit due date
                        </Button>
                      ) : (
                        <span className="text-xs text-slate-400">
                          Schedule locked
                        </span>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
      <Card className="overflow-hidden">
        <h2 className="p-5 text-sm font-semibold">
          Payment history & receipts
        </h2>
        <PaymentsTable
          rows={payments}
          admin={admin}
          onVoid={(p) => {
            setSelected(p);
            setAction("void");
          }}
        />
      </Card>
      {admin && action === "schedule" && (
        <InstallmentScheduleForm
          account={a}
          onClose={() => setAction("")}
          onSuccess={success}
        />
      )}
      {canPay && action === "payment" && (
        <RecordPaymentForm
          account={a}
          installments={installments}
          onClose={() => setAction("")}
          onSuccess={success}
        />
      )}
      {admin && action === "edit" && selected && (
        <EditInstallmentDate
          account={a}
          installment={selected}
          onClose={() => setAction("")}
          onSuccess={success}
        />
      )}
      {admin && action === "void" && selected && (
        <VoidPaymentDialog
          payment={selected}
          onClose={() => setAction("")}
          onSuccess={success}
        />
      )}
      <Toast message={toast} onDismiss={() => setToast("")} />
    </>
  );
}
