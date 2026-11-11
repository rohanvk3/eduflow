import { useRef, useState } from "react";
import { Button, ErrorNotice, Input, Select } from "./UI";
import RecordSelect from "./RecordSelect";
import { saveCourse, saveBatch } from "../services/academicService";
import { errorMessage } from "../api/client";

export default function AcademicForm({
  kind,
  record,
  onSaved,
  onClose,
  onBusyChange = () => {},
}) {
  const batch = kind === "batches";
  const today = new Date().toLocaleDateString("en-CA");
  const [values, setValues] = useState(
    batch
      ? {
          code: record?.code || "",
          name: record?.name || "",
          courseId: record?.courseId || "",
          capacity: record?.capacity || 30,
          startDate: record?.startDate || today,
          endDate: record?.endDate || today,
          status: record?.status || "PLANNED",
        }
      : {
          code: record?.code || "",
          name: record?.name || "",
          description: record?.description || "",
          durationWeeks: record?.durationWeeks || 12,
        },
  );
  const [busy, setBusy] = useState(false);
  const locked = useRef(false);
  const [error, setError] = useState("");
  const [errors, setErrors] = useState({});
  const change = (e) =>
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault();
    if (locked.current) return;
    locked.current = true;
    setBusy(true);
    onBusyChange(true);
    setError("");
    setErrors({});
    const data = batch
      ? {
          ...values,
          courseId: Number(values.courseId),
          capacity: Number(values.capacity),
        }
      : { ...values, durationWeeks: Number(values.durationWeeks) };
    try {
      const response = await (batch
        ? saveBatch(record?.id, data)
        : saveCourse(record?.id, data));
      onSaved(response.data);
    } catch (e) {
      setError(errorMessage(e));
      setErrors(e.response?.data?.errors || {});
    } finally {
      locked.current = false;
      setBusy(false);
      onBusyChange(false);
    }
  };
  const field = (name, label, props = {}) => (
    <label className="field-label" key={name} htmlFor={name}>
      {label} *
      <Input
        className="mt-2"
        id={name}
        name={name}
        value={values[name]}
        required
        onChange={change}
        aria-invalid={!!errors[name]}
        {...props}
      />
      {errors[name] && (
        <span className="mt-1 block text-xs text-red-600">{errors[name]}</span>
      )}
    </label>
  );
  return (
    <form onSubmit={submit}>
      <fieldset
        disabled={busy}
        className="max-h-[65dvh] space-y-5 overflow-y-auto p-6"
      >
        <ErrorNotice message={error} />
        <div className="grid gap-5 sm:grid-cols-2">
          {field("code", "Code", { maxLength: 40, pattern: "[A-Za-z0-9_-]+" })}
          {field("name", "Name", { maxLength: 150 })}
          {batch ? (
            <>
              <RecordSelect
                endpoint="/courses"
                label="Course"
                value={values.courseId}
                selectedLabel={record?.courseName}
                onChange={(v) => setValues((old) => ({ ...old, courseId: v }))}
              />
              {field("capacity", "Capacity", {
                type: "number",
                min: 1,
                max: 10000,
              })}
              {field("startDate", "Start date", { type: "date" })}
              {field("endDate", "End date", {
                type: "date",
                min: values.startDate,
              })}
              <label className="field-label">
                Status
                <Select
                  className="mt-2"
                  value={values.status}
                  name="status"
                  onChange={change}
                >
                  {["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"].map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </Select>
              </label>
            </>
          ) : (
            <>
              {field("durationWeeks", "Duration (weeks)", {
                type: "number",
                min: 1,
                max: 520,
              })}
              <label className="field-label sm:col-span-2">
                Description
                <textarea
                  className="input mt-2"
                  name="description"
                  value={values.description}
                  maxLength={2000}
                  rows={4}
                  onChange={change}
                />
              </label>
            </>
          )}
        </div>
        {batch && (
          <p className="text-xs leading-5 text-slate-500">
            Completed/cancelled batches are read-only. Course and dates cannot
            change after enrollment or timetable history exists. Capacity cannot
            be lower than active enrollments.
          </p>
        )}
      </fieldset>
      <div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-4">
        <Button variant="secondary" disabled={busy} onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? "Saving…" : "Save"}
        </Button>
      </div>
    </form>
  );
}
