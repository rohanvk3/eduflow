import { useEffect, useRef, useState } from "react";
import axios from "axios";
import RecordSelect from "./RecordSelect";
import { Button, ErrorNotice, Input, Select } from "./UI";
import { localDate } from "./AttendanceUI";
import { getSchedules } from "../services/academicService";
import { createSession } from "../services/attendanceService";
import { errorMessage } from "../api/client";

export default function CreateAttendanceSession({
  batchId,
  onCreated,
  onClose,
  onBusyChange,
}) {
  const [values, setValues] = useState({
    batchId: batchId || "",
    sessionDate: localDate(),
    scheduleId: "",
    startTime: "",
    endTime: "",
    topic: "",
  });
  const [schedules, setSchedules] = useState([]);
  const [scheduleError, setScheduleError] = useState("");
  const [error, setError] = useState("");
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  useEffect(() => {
    setSchedules([]);
    setScheduleError("");
    if (!values.batchId) return;
    const c = new AbortController();
    getSchedules(values.batchId, c.signal)
      .then(({ data }) => {
        if (!c.signal.aborted) setSchedules(data.filter((s) => s.active));
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setScheduleError(errorMessage(e));
      });
    return () => c.abort();
  }, [values.batchId]);
  const change = (e) =>
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault();
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    onBusyChange(true);
    setError("");
    setErrors({});
    try {
      const { data } = await createSession({
        batchId: Number(values.batchId),
        sessionDate: values.sessionDate,
        scheduleId: values.scheduleId ? Number(values.scheduleId) : null,
        startTime: values.scheduleId ? null : values.startTime || null,
        endTime: values.scheduleId ? null : values.endTime || null,
        topic: values.topic || null,
      });
      onCreated(data);
    } catch (e) {
      setError(errorMessage(e));
      setErrors(e.response?.data?.errors || {});
    } finally {
      lock.current = false;
      setBusy(false);
      onBusyChange(false);
    }
  };
  return (
    <form onSubmit={submit}>
      <fieldset
        disabled={busy}
        className="max-h-[65dvh] space-y-5 overflow-y-auto p-6"
      >
        <ErrorNotice message={error} />
        <RecordSelect
          endpoint="/batches"
          status=""
          label="Batch"
          value={values.batchId}
          onChange={(value) =>
            setValues((v) => ({ ...v, batchId: value, scheduleId: "" }))
          }
        />
        <label className="field-label">
          Session date *
          <Input
            className="mt-2"
            name="sessionDate"
            type="date"
            required
            value={values.sessionDate}
            onChange={change}
          />
          {errors.sessionDate && (
            <span className="text-xs text-red-600">{errors.sessionDate}</span>
          )}
        </label>
        <ErrorNotice message={scheduleError} />
        <label className="field-label">
          Weekly class (optional)
          <Select
            className="mt-2"
            name="scheduleId"
            value={values.scheduleId}
            onChange={change}
          >
            <option value="">Manual class</option>
            {schedules.map((s) => (
              <option value={s.id} key={s.id}>
                {s.dayOfWeek} · {s.startTime.slice(0, 5)}–
                {s.endTime.slice(0, 5)} · {s.teacherName}
              </option>
            ))}
          </Select>
        </label>
        {!values.scheduleId && (
          <div className="grid gap-5 sm:grid-cols-2">
            <label className="field-label">
              Start time
              <Input
                className="mt-2"
                type="time"
                step={60}
                name="startTime"
                required={!!values.endTime}
                value={values.startTime}
                onChange={change}
              />
            </label>
            <label className="field-label">
              End time
              <Input
                className="mt-2"
                type="time"
                step={60}
                name="endTime"
                required={!!values.startTime}
                value={values.endTime}
                onChange={change}
              />
            </label>
          </div>
        )}
        <label className="field-label">
          Topic
          <Input
            className="mt-2"
            name="topic"
            maxLength={250}
            value={values.topic}
            onChange={change}
          />
        </label>
        <p className="text-xs leading-5 text-slate-500">
          Times use Asia/Kolkata. Multiple classes per date are supported; the
          same batch/date/start time cannot be duplicated. One untimed manual
          class per batch/date is allowed. Future sessions remain drafts.
        </p>
      </fieldset>
      <div className="flex justify-end gap-3 border-t border-slate-100 p-4">
        <Button variant="secondary" disabled={busy} onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? "Creating…" : "Create session"}
        </Button>
      </div>
    </form>
  );
}
