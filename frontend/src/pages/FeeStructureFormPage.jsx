import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  Input,
  Select,
  ErrorNotice,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  Field,
  MoneyInput,
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  useFinanceLoad,
} from "../components/FinanceUI";
import {
  getFeeStructure,
  saveFeeStructure,
  financeError,
} from "../services/financeService";
import { localDate } from "../components/AttendanceUI";
export default function FeeStructureFormPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = user.role === "ADMIN";
  const state = useFinanceLoad(
    (signal) => getFeeStructure(id, signal),
    [id],
    allowed && !!id,
  );
  if (!allowed) return <FinanceDenied />;
  if (id && state.loading) return <Loading label="Loading fee structure…" />;
  if (id && state.error) return <FinanceError state={state} />;
  if (id && !state.data) return null;
  return <StructureForm key={id || "new"} initial={state.data} id={id} />;
}
function StructureForm({ initial, id }) {
  const navigate = useNavigate();
  const [form, setForm] = useState(
    initial || {
      courseId: "",
      name: "",
      tuitionFee: "",
      admissionFee: "0",
      materialFee: "0",
      otherFee: "0",
      effectiveFrom: localDate(),
      effectiveTo: "",
      status: "ACTIVE",
    },
  );
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [fields, setFields] = useState({});
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  return (
    <>
      <FinanceHeading
        title={id ? "Edit Fee Structure" : "New Fee Structure"}
        subtitle="Totals are calculated by the backend. Referenced fee amounts and dates are protected."
      />
      <Card className="max-w-4xl p-5 sm:p-7">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (busy) return;
            setBusy(true);
            setError("");
            setFields({});
            try {
              await saveFeeStructure(id, {
                ...form,
                courseId: Number(form.courseId),
                effectiveTo: form.effectiveTo || null,
              });
              navigate("/finance/structures", {
                state: { message: "Fee structure saved." },
              });
            } catch (e) {
              setError(await financeError(e));
              setFields(e.response?.data?.errors || {});
            } finally {
              setBusy(false);
            }
          }}
        >
          <ErrorNotice message={error} />
          <fieldset disabled={busy} className="grid gap-5 md:grid-cols-2">
            {id ? (
              <Field label="Course">
                <Input value={initial.courseName} disabled />
              </Field>
            ) : (
              <RecordSelect
                label="Course"
                endpoint="/courses"
                value={form.courseId}
                onChange={(v) => set("courseId", v)}
              />
            )}
            <Field label="Structure name *" error={fields.name}>
              <Input
                required
                maxLength={150}
                value={form.name}
                onChange={(e) => set("name", e.target.value)}
              />
            </Field>
            {[
              ["tuitionFee", "Tuition fee"],
              ["admissionFee", "Admission fee"],
              ["materialFee", "Material fee"],
              ["otherFee", "Other fee"],
            ].map(([key, label]) => (
              <Field key={key} label={label + " (INR) *"} error={fields[key]}>
                <MoneyInput
                  value={form[key]}
                  onChange={(e) => set(key, e.target.value)}
                />
              </Field>
            ))}
            <Field label="Effective from *">
              <Input
                type="date"
                required
                value={form.effectiveFrom}
                onChange={(e) => set("effectiveFrom", e.target.value)}
              />
            </Field>
            <Field label="Effective to">
              <Input
                type="date"
                min={form.effectiveFrom}
                value={form.effectiveTo || ""}
                onChange={(e) => set("effectiveTo", e.target.value)}
              />
            </Field>
            <Field label="Status">
              <Select
                value={form.status}
                onChange={(e) => set("status", e.target.value)}
              >
                <option>ACTIVE</option>
                <option>INACTIVE</option>
              </Select>
            </Field>
          </fieldset>
          <p className="mt-6 text-xs leading-6 text-slate-500">
            Only one active structure may apply to a course on a given date.
            Deactivating a structure preserves agreements; create a new one when
            fees change.
          </p>
          <div className="mt-7 flex justify-end gap-3">
            <Link to="/finance/structures" className="button-secondary">
              Cancel
            </Link>
            <Button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save fee structure"}
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
