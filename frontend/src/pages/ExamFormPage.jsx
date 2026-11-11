import { useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ErrorNotice,
  Input,
  Loading,
  Select,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ExamHeading,
  ExamDenied,
  ExamError,
  Field,
  staffRole,
  useExamLoad,
  examError,
} from "../components/ExamUI";
import { getExam, saveExam } from "../services/examService";
function inputTime(s) {
  if (!s) return "";
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  })
    .format(new Date(s))
    .replace(" ", "T");
}
export default function ExamFormPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role);
  const state = useExamLoad(
    (signal) => getExam(id, signal),
    [id],
    allowed && !!id,
  );
  if (!allowed) return <ExamDenied />;
  if (id && state.loading) return <Loading label="Loading exam…" />;
  if (id && state.error) return <ExamError state={state} />;
  if (id && !state.data) return null;
  if (id && state.data.status !== "DRAFT")
    return (
      <Card className="p-6">
        Published exam rules are immutable.{" "}
        <Link className="text-indigo-600" to={`/exams/${id}`}>
          View exam
        </Link>
      </Card>
    );
  return <ExamForm key={id || "new"} id={id} initial={state.data} />;
}
function ExamForm({ id, initial }) {
  const navigate = useNavigate();
  const [form, setForm] = useState(
    initial
      ? {
          ...initial,
          startDateTime: inputTime(initial.startDateTime),
          endDateTime: inputTime(initial.endDateTime),
        }
      : {
          examCode: "",
          title: "",
          description: "",
          batchId: "",
          examType: "ONLINE_MCQ",
          durationMinutes: "60",
          totalMarks: "",
          passingMarks: "",
          startDateTime: "",
          endDateTime: "",
        },
  );
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  return (
    <>
      <ExamHeading
        title={id ? "Edit Exam" : "Create Exam"}
        subtitle="Schedules use Asia/Kolkata. Online deadlines are enforced by the server."
      />
      <Card className="max-w-5xl p-6">
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            if (busy) return;
            setBusy(true);
            setError("");
            try {
              const { data } = await saveExam(id, {
                ...form,
                batchId: Number(form.batchId),
                durationMinutes: form.durationMinutes
                  ? Number(form.durationMinutes)
                  : null,
                startDateTime: new Date(
                  form.startDateTime + ":00+05:30",
                ).toISOString(),
                endDateTime: new Date(
                  form.endDateTime + ":00+05:30",
                ).toISOString(),
              });
              navigate(`/exams/${data.id}`);
            } catch (e) {
              setError(await examError(e));
            } finally {
              setBusy(false);
            }
          }}
        >
          <ErrorNotice message={error} />
          <fieldset disabled={busy} className="grid gap-5 md:grid-cols-2">
            <Field label="Exam Code *">
              <Input
                required
                pattern="[A-Za-z0-9_-]{1,40}"
                maxLength={40}
                value={form.examCode}
                onChange={(e) => set("examCode", e.target.value)}
              />
            </Field>
            <Field label="Exam Title *">
              <Input
                required
                maxLength={200}
                value={form.title}
                onChange={(e) => set("title", e.target.value)}
              />
            </Field>
            <RecordSelect
              label="Batch"
              endpoint="/batches"
              status=""
              value={form.batchId}
              selectedLabel={form.batchName}
              onChange={(v) => set("batchId", v)}
            />
            <Field label="Exam Type *">
              <Select
                value={form.examType}
                onChange={(e) => set("examType", e.target.value)}
              >
                <option value="ONLINE_MCQ">Online MCQ</option>
                <option value="OFFLINE">Offline exam</option>
              </Select>
            </Field>
            {[
              ["startDateTime", "Start date & time (IST)"],
              ["endDateTime", "End date & time (IST)"],
            ].map(([key, label]) => (
              <Field key={key} label={label + " *"}>
                <Input
                  type="datetime-local"
                  required
                  value={form[key]}
                  onChange={(e) => set(key, e.target.value)}
                />
              </Field>
            ))}
            <Field label="Duration (minutes)">
              <Input
                type="number"
                required={form.examType === "ONLINE_MCQ"}
                min="1"
                max="1440"
                value={form.durationMinutes || ""}
                onChange={(e) => set("durationMinutes", e.target.value)}
              />
            </Field>
            {[
              ["totalMarks", "Total marks"],
              ["passingMarks", "Passing marks"],
            ].map(([key, label]) => (
              <Field key={key} label={label + " *"}>
                <Input
                  required
                  type="number"
                  min={key === "totalMarks" ? "0.01" : "0"}
                  max="99999999.99"
                  step="0.01"
                  value={form[key]}
                  onChange={(e) => set(key, e.target.value)}
                />
              </Field>
            ))}
            <Field label="Description">
              <textarea
                className="input min-h-24"
                maxLength={4000}
                value={form.description || ""}
                onChange={(e) => set("description", e.target.value)}
              />
            </Field>
          </fieldset>
          <div className="mt-7 flex justify-end gap-3">
            <Link
              className="button-secondary"
              to={id ? `/exams/${id}` : "/exams"}
            >
              Cancel
            </Link>
            <Button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save draft exam"}
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
