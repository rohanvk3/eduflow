import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Button, Card, ErrorNotice, Input, Loading } from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  AssignmentHeading,
  AssignmentDenied,
  AssignmentError,
  Field,
  FilePicker,
  UploadProgress,
  progressHandler,
  staffRole,
  useAssignmentLoad,
  assignmentError,
} from "../components/AssignmentUI";
import {
  getAssignment,
  saveAssignment,
  uploadAssignmentFiles,
} from "../services/assignmentService";
function inputTime(value) {
  return value
    ? new Intl.DateTimeFormat("sv-SE", {
        timeZone: "Asia/Kolkata",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      })
        .format(new Date(value))
        .replace(" ", "T")
    : "";
}
export default function AssignmentFormPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role);
  const state = useAssignmentLoad(
    (signal) => getAssignment(id, signal),
    [id],
    allowed && !!id,
  );
  if (!allowed) return <AssignmentDenied />;
  if (id && state.loading) return <Loading label="Loading assignment…" />;
  if (id && state.error) return <AssignmentError state={state} />;
  if (id && !state.data) return null;
  if (id && state.data.status !== "DRAFT")
    return (
      <Card className="p-6">
        Published assignment rules are immutable.{" "}
        <Link className="text-indigo-600" to={`/assignments/${id}`}>
          View assignment
        </Link>
      </Card>
    );
  return <AssignmentForm key={id || "new"} initial={state.data} id={id} />;
}
function AssignmentForm({ initial, id }) {
  const navigate = useNavigate(),
    saved = useRef(initial || null),
    lock = useRef(false);
  const [form, setForm] = useState(
    initial
      ? { ...initial, dueDateTime: inputTime(initial.dueDateTime) }
      : {
          title: "",
          description: "",
          batchId: "",
          maxMarks: "",
          dueDateTime: "",
          allowLateSubmission: false,
        },
  );
  const [files, setFiles] = useState([]),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [progress, setProgress] = useState(null);
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  return (
    <>
      <AssignmentHeading
        title={id ? "Edit Assignment" : "Create Assignment"}
        subtitle="Save a draft with attachments, then review and publish it."
      />
      <Card className="max-w-5xl p-6">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (lock.current) return;
            lock.current = true;
            setBusy(true);
            setError("");
            setProgress(null);
            try {
              const payload = {
                ...form,
                batchId: Number(form.batchId),
                dueDateTime: new Date(
                  form.dueDateTime + ":00+05:30",
                ).toISOString(),
                version: saved.current?.version,
              };
              const { data } = await saveAssignment(saved.current?.id, payload);
              saved.current = data;
              if (files.length) {
                try {
                  await uploadAssignmentFiles(
                    data.id,
                    data.version,
                    files,
                    progressHandler(setProgress),
                  );
                  setFiles([]);
                } catch (uploadError) {
                  const fresh = await getAssignment(data.id);
                  saved.current = fresh.data;
                  throw uploadError;
                }
              }
              navigate(`/assignments/${data.id}`, {
                state: {
                  message:
                    "Draft saved. Review attachments, then publish when ready.",
                },
              });
            } catch (err) {
              setError(
                (saved.current
                  ? "Your draft is saved. You can retry the remaining operation. "
                  : "") + (await assignmentError(err)),
              );
            } finally {
              lock.current = false;
              setBusy(false);
            }
          }}
        >
          <ErrorNotice message={error} />
          <fieldset disabled={busy} className="grid gap-5 md:grid-cols-2">
            <Field label="Assignment title *">
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
            <Field label="Maximum marks *">
              <Input
                required
                type="number"
                min="0.01"
                step="0.01"
                max="99999999.99"
                value={form.maxMarks}
                onChange={(e) => set("maxMarks", e.target.value)}
              />
            </Field>
            <Field label="Due date & time (IST) *">
              <Input
                required
                type="datetime-local"
                value={form.dueDateTime}
                onChange={(e) => set("dueDateTime", e.target.value)}
              />
            </Field>
            <div className="md:col-span-2">
              <Field label="Description">
                <textarea
                  className="input min-h-36"
                  maxLength={10000}
                  value={form.description || ""}
                  onChange={(e) => set("description", e.target.value)}
                />
              </Field>
            </div>
            <label className="flex items-center gap-3 text-sm text-slate-600">
              <input
                type="checkbox"
                checked={form.allowLateSubmission}
                onChange={(e) => set("allowLateSubmission", e.target.checked)}
              />
              Allow late first submissions
            </label>
          </fieldset>
          <div className="mt-6">
            <FilePicker files={files} onChange={setFiles} disabled={busy} />
          </div>
          <UploadProgress value={progress} busy={busy} />
          <div className="mt-7 flex justify-end gap-3">
            <Link
              className="button-secondary"
              to={id ? `/assignments/${id}` : "/assignments"}
            >
              Cancel
            </Link>
            <Button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save Draft"}
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
