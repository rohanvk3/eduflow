import { useEffect, useEffectEvent, useState } from "react";
import axios from "axios";
import { Link } from "react-router-dom";
import {
  Download,
  Wallet,
  ArrowDownLeft,
  Clock3,
  CalendarDays,
} from "lucide-react";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Input,
  Select,
  Table,
  Skeleton,
  dateLabel,
} from "./UI";
import {
  downloadReceipt,
  financeError,
  saveDownload,
} from "../services/financeService";
export const inr = (value) =>
  value == null
    ? "—"
    : new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2,
      }).format(value);
export function cents(value) {
  const s = String(value);
  if (!/^\d{1,12}(\.\d{1,2})?$/.test(s)) return null;
  const [whole, fraction = ""] = s.split(".");
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"));
}
export const decimal = (value) =>
  `${value / 100n}.${String(value % 100n).padStart(2, "0")}`;
export function useFinanceLoad(loader, dependencies, allowed = true) {
  const [data, setData] = useState(null),
    [loading, setLoading] = useState(allowed),
    [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const loadLatest = useEffectEvent((signal) => loader(signal));
  useEffect(() => {
    if (!allowed) {
      setData(null);
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setData(null);
    loadLatest(controller.signal)
      .then(({ data }) => {
        if (!controller.signal.aborted) setData(data);
      })
      .catch(async (e) => {
        if (!axios.isCancel(e)) {
          const message = await financeError(e);
          if (!controller.signal.aborted) setError(message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  // Callers supply the query inputs that trigger a load; loadLatest reads the
  // current loader without re-fetching for every inline function identity.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencies, revision, allowed]);
  return { data, loading, error, reload: () => setRevision((v) => v + 1) };
}
export function FinanceHeading({ title, subtitle, children }) {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Finance · INR</p>
        <h1 className="page-title">{title}</h1>
        <p className="mt-2 text-sm text-slate-500">{subtitle}</p>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function FinanceDenied() {
  return (
    <Card>
      <EmptyState
        title="Finance access restricted"
        description="Financial management is available to administrators. Students can view their own fees."
      />
    </Card>
  );
}
export function FinanceError({ state }) {
  return (
    state.error && (
      <>
        <ErrorNotice message={state.error} />
        <Button variant="secondary" className="mb-5" onClick={state.reload}>
          Retry
        </Button>
      </>
    )
  );
}
export function Field({ label, children, error }) {
  return (
    <label className="field-label block">
      {label}
      <div className="mt-2">{children}</div>
      {error && (
        <span className="mt-1 block text-xs text-red-600">{error}</span>
      )}
    </label>
  );
}
export function MoneyInput(props) {
  return (
    <Input
      type="number"
      min="0"
      max="999999999999.99"
      step="0.01"
      required
      {...props}
    />
  );
}
export function FinanceStatus({ status }) {
  const tone = ["SUCCESS", "PAID", "ACTIVE"].includes(status)
    ? "success"
    : ["OVERDUE", "VOIDED"].includes(status)
      ? "warning"
      : "neutral";
  return <Badge tone={tone}>{status?.replaceAll("_", " ") || "—"}</Badge>;
}
export function FinanceStats({ data, loading }) {
  const rows = [
    ["Total collections", data?.totalCollections, ArrowDownLeft],
    ["Outstanding dues", data?.outstandingBalance, Wallet],
    ["Overdue amount", data?.overdueAmount, Clock3],
    ["This month's collections", data?.monthCollections, CalendarDays],
  ];
  return (
    <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {rows.map(([label, value, Icon]) => (
        <Card className="p-5" key={label}>
          <Icon size={19} className="mb-5 text-indigo-500" />
          <p className="text-xs text-slate-500">{label}</p>
          {loading ? (
            <Skeleton className="mt-3 h-8 w-28" />
          ) : (
            <p className="mt-2 break-words text-2xl font-semibold tabular-nums">
              {inr(value)}
            </p>
          )}
        </Card>
      ))}
    </div>
  );
}
export function ReceiptButton({ payment }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <div>
      <Button
        variant="ghost"
        disabled={busy}
        aria-label={`Download receipt ${payment.receiptNumber}`}
        onClick={async () => {
          setBusy(true);
          setError("");
          try {
            const { data } = await downloadReceipt(payment.id);
            saveDownload(data, `${payment.receiptNumber}.pdf`);
          } catch (e) {
            setError(await financeError(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <Download size={14} />
        {busy
          ? "Downloading…"
          : payment.status === "VOIDED"
            ? "Void record"
            : "Receipt"}
      </Button>
      {error && (
        <p role="alert" className="max-w-52 text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
export function PaymentsTable({ rows = [], admin = false, onVoid }) {
  if (!rows.length)
    return (
      <EmptyState
        title="No payments recorded"
        description="Verified payments will appear here once recorded by an administrator."
      />
    );
  return (
    <Table label="Fee payments" className="min-w-[800px]">
      <thead>
        <tr>
          <th>Receipt / Student</th>
          <th>Payment date</th>
          <th>Amount</th>
          <th>Method</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((p) => (
          <tr key={p.id}>
            <td>
              <Link
                className="font-medium text-indigo-600"
                to={`/finance/accounts/${p.feeAccountId}`}
              >
                {p.studentName}
              </Link>
              <p className="mt-1 max-w-60 break-all text-[10px] text-slate-500">
                {p.receiptNumber}
              </p>
              <p className="mt-1 text-xs text-slate-500">{p.batchName}</p>
            </td>
            <td>{dateLabel(p.paymentDate)}</td>
            <td className="whitespace-nowrap font-medium tabular-nums">
              {inr(p.amount)}
            </td>
            <td>{p.paymentMethod.replaceAll("_", " ")}</td>
            <td>
              <FinanceStatus status={p.status} />
              {p.voidAudit && (
                <p className="mt-2 max-w-48 break-words text-xs text-slate-500">
                  Voided: {p.voidAudit.reason}
                </p>
              )}
            </td>
            <td>
              <div className="flex flex-wrap items-center gap-1">
                <ReceiptButton payment={p} />
                {admin && onVoid && p.status === "SUCCESS" && (
                  <Button variant="ghost" onClick={() => onVoid(p)}>
                    Void
                  </Button>
                )}
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
export function AccountTable({ rows = [], recordPayment = false }) {
  if (!rows.length)
    return (
      <EmptyState
        title="No fee accounts"
        description="Create an agreed fee account for an existing active enrollment to get started."
      />
    );
  return (
    <Table label="Student fee accounts" className="min-w-[860px]">
      <thead>
        <tr>
          <th>Student</th>
          <th>Course / Batch</th>
          <th>Payable</th>
          <th>Paid</th>
          <th>Outstanding</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {rows.map((a) => (
          <tr key={a.id}>
            <td>
              <p className="font-medium">{a.studentName}</p>
              <p className="mt-1 font-mono text-xs text-slate-500">
                {a.admissionNumber}
              </p>
            </td>
            <td>
              {a.courseName}
              <p className="mt-1 text-xs text-slate-500">{a.batchName}</p>
            </td>
            <td className="whitespace-nowrap">{inr(a.finalPayableAmount)}</td>
            <td className="whitespace-nowrap">{inr(a.paidAmount)}</td>
            <td className="whitespace-nowrap font-medium">
              {inr(a.outstandingAmount)}
            </td>
            <td>
              <FinanceStatus status={a.paymentStatus} />
            </td>
            <td>
              <Link
                className="button-secondary"
                to={`/finance/accounts/${a.id}${recordPayment ? "?recordPayment=1" : ""}`}
              >
                {recordPayment ? "Record payment" : "View account"}
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
