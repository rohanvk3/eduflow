import { useRef, useState } from "react";
import { Button, ErrorNotice, Input, Select } from "./UI";
import RecordSelect from "./RecordSelect";
import {
  assignTeacher,
  enrollStudent,
  saveSchedule,
} from "../services/academicService";
import { errorMessage } from "../api/client";

export const weekdays = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
export default function BatchOperationForm({
  action,
  batchId,
  schedule,
  assignments,
  onClose,
  onSaved,
  onBusyChange,
}) {
  const [values, setValues] = useState({
    teacherId: schedule?.teacherId || "",
    studentId: "",
    enrollmentDate: new Date().toLocaleDateString("en-CA"),
    dayOfWeek: schedule?.dayOfWeek || "MONDAY",
    startTime: schedule?.startTime?.slice(0, 5) || "",
    endTime: schedule?.endTime?.slice(0, 5) || "",
    room: schedule?.room || "",
    active: schedule?.active ?? true,
  });
  const [error, setError] = useState("");
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const locked = useRef(false);
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
    try {
      if (action === "assign")
        await assignTeacher(batchId, Number(values.teacherId));
      else if (action === "enroll")
        await enrollStudent(batchId, {
          studentId: Number(values.studentId),
          enrollmentDate: values.enrollmentDate,
        });
      else
        await saveSchedule(batchId, schedule?.id, {
          teacherId: Number(values.teacherId),
          dayOfWeek: values.dayOfWeek,
          startTime: values.startTime,
          endTime: values.endTime,
          room: values.room,
          active: values.active === true || values.active === "true",
        });
      onSaved();
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
    <label className="field-label">
      {label}
      <Input
        className="mt-2"
        name={name}
        value={values[name]}
        onChange={change}
        {...props}
      />
      {errors[name] && (
        <span className="text-xs text-red-600">{errors[name]}</span>
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
        {action === "assign" ? (
          <RecordSelect
            endpoint="/teachers"
            label="Teacher"
            value={values.teacherId}
            onChange={(v) => setValues((old) => ({ ...old, teacherId: v }))}
          />
        ) : action === "enroll" ? (
          <>
            <RecordSelect
              endpoint="/students"
              label="Student"
              value={values.studentId}
              onChange={(v) => setValues((old) => ({ ...old, studentId: v }))}
            />
            {field("enrollmentDate", "Enrollment date *", {
              type: "date",
              required: true,
              max: new Date().toLocaleDateString("en-CA"),
            })}
            <p className="text-xs leading-5 text-slate-500">
              The enrollment date must be on or after admission and no later
              than today or the batch end. Enrollment history is retained after
              withdrawal.
            </p>
          </>
        ) : (
          <>
            <label className="field-label">
              Assigned teacher *
              <Select
                className="mt-2"
                value={values.teacherId}
                name="teacherId"
                required
                onChange={change}
              >
                <option value="">Select assigned teacher</option>
                {assignments
                  .filter(
                    (a) => a.active || a.teacherId === schedule?.teacherId,
                  )
                  .map((a) => (
                    <option key={a.teacherId} value={a.teacherId}>
                      {a.fullName}
                      {a.active ? "" : " (unassigned)"}
                    </option>
                  ))}
              </Select>
            </label>
            <div className="grid gap-5 sm:grid-cols-2">
              <label className="field-label">
                Weekday *
                <Select
                  className="mt-2"
                  name="dayOfWeek"
                  value={values.dayOfWeek}
                  onChange={change}
                >
                  {weekdays.map((d) => (
                    <option key={d}>{d}</option>
                  ))}
                </Select>
              </label>
              {field("room", "Room (optional)", { maxLength: 100 })}
              {field("startTime", "Start time *", {
                type: "time",
                required: true,
                step: 60,
              })}
              {field("endTime", "End time *", {
                type: "time",
                required: true,
                step: 60,
              })}
              <label className="field-label">
                Schedule status
                <Select
                  className="mt-2"
                  name="active"
                  value={String(values.active)}
                  onChange={change}
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </Select>
              </label>
            </div>
            <p className="text-xs leading-5 text-slate-500">
              Times use Asia/Kolkata. Adjacent classes are allowed; overlapping
              classes for this batch or teacher are rejected.
            </p>
          </>
        )}
      </fieldset>
      <div className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">
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
