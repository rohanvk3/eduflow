import { useState } from "react";
import { api, errorMessage } from "../api/client";
import { Button, ErrorNotice, Input } from "./UI";
import { UserRound, ShieldCheck, Link2 } from "lucide-react";

export default function StudentForm({
  student,
  onSaved,
  onClose,
  onBusyChange = () => {},
}) {
  const today = new Date().toLocaleDateString("en-CA");
  const yesterdayDate = new Date();
  yesterdayDate.setDate(yesterdayDate.getDate() - 1);
  const yesterday = yesterdayDate.toLocaleDateString("en-CA");
  const [values, setValues] = useState({
    fullName: student?.fullName || "",
    email: student?.email || "",
    phone: student?.phone || "",
    dateOfBirth: student?.dateOfBirth || "",
    gender: student?.gender || "",
    address: student?.address || "",
    guardianName: student?.guardianName || "",
    guardianPhone: student?.guardianPhone || "",
    admissionDate: student?.admissionDate || today,
    userId: student?.userId ?? "",
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const change = (event) => {
    const { name, value } = event.target;
    setValues((v) => ({ ...v, [name]: value }));
    setErrors((e) => ({ ...e, [name]: undefined }));
  };
  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    onBusyChange(true);
    setError("");
    setErrors({});
    const payload = {
      ...values,
      userId: values.userId === "" ? null : Number(values.userId),
      gender: values.gender || null,
    };
    try {
      const { data } = student
        ? await api.put(`/students/${student.id}`, payload)
        : await api.post("/students", payload);
      onSaved(data);
    } catch (failure) {
      setError(errorMessage(failure));
      setErrors(failure.response?.data?.errors || {});
    } finally {
      setBusy(false);
      onBusyChange(false);
    }
  };
  const field = (name, label, extra = {}) => (
    <label className="field-label" key={name} htmlFor={name}>
      {label}
      {extra.required && <span className="ml-1 text-indigo-500">*</span>}
      <Input
        id={name}
        name={name}
        className="mt-2"
        value={values[name]}
        onChange={change}
        aria-invalid={!!errors[name]}
        aria-describedby={errors[name] ? `${name}-error` : undefined}
        {...extra}
      />
      {errors[name] && (
        <span id={`${name}-error`} className="mt-1 block text-xs text-red-700">
          {errors[name]}
        </span>
      )}
    </label>
  );
  return (
    <form onSubmit={submit}>
      <fieldset
        disabled={busy}
        className="max-h-[min(65dvh,640px)] space-y-7 overflow-y-auto px-6 py-6"
      >
        <ErrorNotice message={error} />
        <p className="rounded-lg border border-indigo-100 bg-indigo-50/50 px-3 py-2.5 text-xs leading-5 text-indigo-700">
          {student
            ? "Keep student information up to date."
            : "Start a new student record. An admission number is assigned automatically."}
        </p>
        <section>
          <h3 className="form-section">
            <UserRound size={15} className="text-indigo-500" />
            Student information
          </h3>
          <div className="grid gap-5 sm:grid-cols-2">
            {field("fullName", "Full name", {
              required: true,
              maxLength: 100,
              autoComplete: "name",
            })}
            {field("email", "Email address", {
              required: true,
              type: "email",
              maxLength: 254,
              autoComplete: "email",
            })}
            {field("phone", "Phone number", {
              required: true,
              type: "tel",
              pattern: "\\+?[0-9]{7,15}",
              placeholder: "+919876543210",
              title: "7–15 digits, optionally starting with +",
              maxLength: 16,
            })}
            {field("dateOfBirth", "Date of birth", {
              required: true,
              type: "date",
              max: yesterday,
            })}
            {field("gender", "Gender (optional)", { maxLength: 30 })}
            {field("admissionDate", "Admission date", {
              required: true,
              type: "date",
              min: values.dateOfBirth || undefined,
              max: today,
            })}
          </div>
          <label className="field-label mt-5" htmlFor="address">
            Address <span className="text-indigo-500">*</span>
            <textarea
              id="address"
              name="address"
              required
              maxLength={500}
              rows={2}
              className="input mt-2 resize-y"
              value={values.address}
              onChange={change}
              aria-invalid={!!errors.address}
              aria-describedby={errors.address ? "address-error" : undefined}
            />
            {errors.address && (
              <span
                id="address-error"
                className="mt-1 block text-xs text-red-700"
              >
                {errors.address}
              </span>
            )}
          </label>
        </section>
        <section>
          <h3 className="form-section">
            <ShieldCheck size={15} className="text-indigo-500" />
            Guardian information
          </h3>
          <div className="grid gap-5 sm:grid-cols-2">
            {field("guardianName", "Guardian name", {
              required: true,
              maxLength: 100,
            })}
            {field("guardianPhone", "Guardian phone", {
              required: true,
              type: "tel",
              pattern: "\\+?[0-9]{7,15}",
              maxLength: 16,
              title: "7–15 digits, optionally starting with +",
            })}
          </div>
        </section>
        <section>
          <h3 className="form-section">
            <Link2 size={15} className="text-indigo-500" />
            Account access
          </h3>
          {field("userId", "Linked student user ID (optional)", {
            type: "number",
            min: 1,
            step: 1,
          })}
          <p className="mt-2 text-xs leading-5 text-slate-500">
            Enter an existing STUDENT account ID to give that user access to
            this profile. Leave blank for an unlinked record. On edit, clearing
            this field removes the link.
          </p>
        </section>
      </fieldset>
      <div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50/60 px-6 py-4">
        <Button
          type="button"
          variant="secondary"
          disabled={busy}
          onClick={onClose}
        >
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? "Saving…" : student ? "Save changes" : "Add student"}
        </Button>
      </div>
    </form>
  );
}
