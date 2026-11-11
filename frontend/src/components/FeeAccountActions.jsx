import { useRef, useState } from "react";
import { Button, ErrorNotice, Input, Modal, Select } from "./UI";
import { Field, MoneyInput, cents, decimal, inr } from "./FinanceUI";
import {
  createInstallments,
  updateInstallment,
  recordPayment,
  financeError,
  voidPayment,
} from "../services/financeService";
import { localDate } from "./AttendanceUI";

export function InstallmentScheduleForm({ account, onClose, onSuccess }) {
  const [rows, setRows] = useState([
      { amount: String(account.finalPayableAmount), dueDate: localDate() },
    ]),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const update = (index, key, value) =>
    setRows((r) => r.map((x, i) => (i === index ? { ...x, [key]: value } : x)));
  const total = rows.reduce((sum, r) => sum + (cents(r.amount) ?? 0n), 0n);
  return (
    <Modal
      title="Create installment schedule"
      description="The complete schedule is saved in a single transaction."
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          if (total !== cents(account.finalPayableAmount)) {
            setError("Schedule total must equal the final payable amount.");
            return;
          }
          setBusy(true);
          setError("");
          try {
            await createInstallments(account.id, {
              version: account.version,
              installments: rows.map((r, i) => ({
                ...r,
                installmentNumber: i + 1,
              })),
            });
            onSuccess("Installment schedule created.");
          } catch (e) {
            setError(await financeError(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <p className="text-sm text-slate-600">
          Final payable: <strong>{inr(account.finalPayableAmount)}</strong>
        </p>
        <fieldset disabled={busy} className="space-y-4">
          {rows.map((r, i) => (
            <div key={i} className="rounded-lg border border-slate-200 p-4">
              <div className="mb-3 flex justify-between text-xs font-medium">
                Installment {i + 1}
                {rows.length > 1 && (
                  <Button
                    variant="ghost"
                    onClick={() => setRows(rows.filter((_, n) => n !== i))}
                  >
                    Remove
                  </Button>
                )}
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label={`Amount ${i + 1} (INR) *`}>
                  <MoneyInput
                    min="0.01"
                    value={r.amount}
                    onChange={(e) => update(i, "amount", e.target.value)}
                  />
                </Field>
                <Field label={`Due date ${i + 1} *`}>
                  <Input
                    type="date"
                    required
                    min={account.agreementDate}
                    value={r.dueDate}
                    onChange={(e) => update(i, "dueDate", e.target.value)}
                  />
                </Field>
              </div>
            </div>
          ))}
          <Button
            variant="secondary"
            disabled={rows.length >= 120}
            onClick={() =>
              setRows([...rows, { amount: "", dueDate: localDate() }])
            }
          >
            Add installment
          </Button>
        </fieldset>
        <p className="text-xs text-slate-500">
          Schedule total: {inr(decimal(total))}. Amounts are fixed after
          creation. Due dates can be changed before the first payment.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save schedule"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
export function EditInstallmentDate({
  installment,
  account,
  onClose,
  onSuccess,
}) {
  const [date, setDate] = useState(installment.dueDate),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Modal
      title={`Edit installment ${installment.installmentNumber} due date`}
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          try {
            await updateInstallment(installment.id, {
              version: account.version,
              amount: installment.amount,
              dueDate: date,
            });
            onSuccess("Installment due date updated.");
          } catch (e) {
            setError(await financeError(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <Field label="Due date *">
          <Input
            type="date"
            required
            min={account.agreementDate}
            value={date}
            disabled={busy}
            onChange={(e) => setDate(e.target.value)}
          />
        </Field>
        <p className="text-xs text-slate-500">
          The amount remains {inr(installment.amount)} to preserve the agreed
          total.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            Save due date
          </Button>
        </div>
      </form>
    </Modal>
  );
}
export function RecordPaymentForm({
  account,
  installments,
  onClose,
  onSuccess,
}) {
  const [form, setForm] = useState({
    amount: "",
    paymentDate: localDate(),
    paymentMethod: "CASH",
    transactionReference: "",
    remarks: "",
  });
  const [allocation, setAllocation] = useState({}),
    [confirm, setConfirm] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const lock = useRef(false);
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  const total = Object.values(allocation).reduce(
    (sum, v) => sum + (cents(v) ?? 0n),
    0n,
  );
  const payload = {
    feeAccountId: account.id,
    ...form,
    allocations: installments
      .filter((i) => (cents(allocation[i.id]) ?? 0n) > 0n)
      .map((i) => ({ installmentId: i.id, amount: allocation[i.id] })),
  };
  const submit = async () => {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    const storageKey = `eduflow.paymentDraft.${account.id}`,
      signature = JSON.stringify(payload);
    try {
      let previous;
      try {
        previous = JSON.parse(sessionStorage.getItem(storageKey));
      } catch {
        /* unavailable storage is handled by the component ref below */
      }
      const current =
        previous?.signature === signature
          ? previous
          : { signature, key: crypto.randomUUID() };
      if (lastAttempt.current?.signature === signature)
        current.key = lastAttempt.current.key;
      lastAttempt.current = current;
      try {
        sessionStorage.setItem(storageKey, JSON.stringify(current));
      } catch {
        /* ref retains the key for retries */
      }
      const { data } = await recordPayment({
        ...payload,
        idempotencyKey: current.key,
      });
      try {
        sessionStorage.removeItem(storageKey);
      } catch {
        /* no persistent storage */
      }
      onSuccess(
        `Payment recorded. Receipt ${data.receiptNumber} is ready below.`,
      );
    } catch (e) {
      setError(await financeError(e));
    } finally {
      setBusy(false);
      lock.current = false;
    }
  };
  const lastAttempt = useRef(null);
  return (
    <Modal
      title={confirm ? "Confirm verified payment" : "Record Payment"}
      description={`${account.studentName} · ${account.batchName}`}
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={(e) => {
          e.preventDefault();
          setError("");
          if (!payload.allocations.length || cents(form.amount) !== total) {
            setError(
              "Positive installment allocations must add up to the payment amount.",
            );
            return;
          }
          if (cents(form.amount) > cents(account.outstandingAmount)) {
            setError("Payment exceeds the outstanding balance.");
            return;
          }
          setConfirm(true);
        }}
      >
        <ErrorNotice message={error} />
        <div className="rounded-xl bg-indigo-50 p-4">
          <p className="text-xs text-indigo-700">Current outstanding balance</p>
          <p className="mt-1 text-2xl font-semibold text-indigo-700">
            {inr(account.outstandingAmount)}
          </p>
        </div>
        {confirm ? (
          <>
            <p className="text-sm leading-7 text-slate-600">
              Confirm that <strong>{inr(form.amount)}</strong> has been received
              and verified via{" "}
              <strong>{form.paymentMethod.replaceAll("_", " ")}</strong> on{" "}
              {form.paymentDate}.
            </p>
            <ul className="space-y-2 text-xs">
              {payload.allocations.map((x) => (
                <li key={x.installmentId}>
                  Installment{" "}
                  {
                    installments.find((i) => i.id === x.installmentId)
                      .installmentNumber
                  }
                  : {inr(x.amount)}
                </li>
              ))}
            </ul>
            <p className="text-xs leading-6 text-slate-500">
              Successful payments cannot be edited. Any void requires a reason
              and is audited. If a response is interrupted, retry this same
              submission; its idempotency key is retained.
            </p>
          </>
        ) : (
          <fieldset disabled={busy} className="space-y-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Payment amount (INR) *">
                <MoneyInput
                  min="0.01"
                  max={account.outstandingAmount}
                  value={form.amount}
                  onChange={(e) => set("amount", e.target.value)}
                />
              </Field>
              <Field label="Payment date *">
                <Input
                  required
                  type="date"
                  min={account.agreementDate}
                  max={localDate()}
                  value={form.paymentDate}
                  onChange={(e) => set("paymentDate", e.target.value)}
                />
              </Field>
              <Field label="Payment method *">
                <Select
                  value={form.paymentMethod}
                  onChange={(e) => set("paymentMethod", e.target.value)}
                >
                  {["CASH", "UPI", "BANK_TRANSFER", "CHEQUE"].map((m) => (
                    <option key={m} value={m}>
                      {m.replaceAll("_", " ")}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field
                label={`Transaction reference${form.paymentMethod !== "CASH" ? " *" : ""}`}
              >
                <Input
                  maxLength={150}
                  required={form.paymentMethod !== "CASH"}
                  value={form.transactionReference}
                  onChange={(e) => set("transactionReference", e.target.value)}
                />
              </Field>
            </div>
            <h3 className="text-sm font-semibold">Allocate to installments</h3>
            {installments
              .filter((i) => cents(i.outstandingAmount) > 0n)
              .map((i) => (
                <Field
                  key={i.id}
                  label={`Installment ${i.installmentNumber} · Due ${i.dueDate} · Outstanding ${inr(i.outstandingAmount)}`}
                >
                  <MoneyInput
                    required={false}
                    max={i.outstandingAmount}
                    value={allocation[i.id] || ""}
                    onChange={(e) =>
                      setAllocation({ ...allocation, [i.id]: e.target.value })
                    }
                  />
                </Field>
              ))}
            <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
              <span>Allocated: {inr(decimal(total))}</span>
              <Button
                variant="ghost"
                onClick={() => set("amount", decimal(total))}
              >
                Use allocated total
              </Button>
            </div>
            <Field label="Remarks">
              <textarea
                className="input min-h-20"
                maxLength={1000}
                value={form.remarks}
                onChange={(e) => set("remarks", e.target.value)}
              />
            </Field>
          </fieldset>
        )}
        <div className="flex justify-end gap-3">
          <Button
            variant="secondary"
            disabled={busy}
            onClick={() => (confirm ? setConfirm(false) : onClose())}
          >
            {confirm ? "Back" : "Cancel"}
          </Button>
          {confirm ? (
            <Button disabled={busy} onClick={submit}>
              {busy ? "Recording…" : "Confirm payment"}
            </Button>
          ) : (
            <Button type="submit">Review payment</Button>
          )}
        </div>
      </form>
    </Modal>
  );
}
export function VoidPaymentDialog({ payment, onClose, onSuccess }) {
  const [reason, setReason] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Modal
      title="Void payment"
      description="This action preserves the payment and records an audit trail."
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            await voidPayment(payment.id, reason);
            onSuccess(
              "Payment voided. The balance and installment allocations have been recalculated.",
            );
          } catch (e) {
            setError(await financeError(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <p className="text-sm text-slate-600">
          Void {inr(payment.amount)} received from {payment.studentName}? The
          payment will stop contributing to paid balances. This operation does
          not issue a refund.
        </p>
        <Field label="Mandatory reason *">
          <textarea
            className="input min-h-24"
            required
            maxLength={1000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            disabled={busy}
          />
        </Field>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="danger"
            type="submit"
            disabled={busy || !reason.trim()}
          >
            {busy ? "Voiding…" : "Void payment"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
