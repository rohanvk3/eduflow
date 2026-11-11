import { useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Select,
  ErrorNotice,
  Modal,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  NoticeHeading,
  NoticeError,
  useNoticeLoad,
  Field,
} from "../components/NotificationUI";
import {
  getAnnouncement,
  saveAnnouncement,
  publishAnnouncement,
} from "../services/announcementService";
import { notificationError } from "../services/notificationService";
const toLocal = (value) => {
  if (!value) return "";
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(value));
  const get = (k) => parts.find((p) => p.type === k).value;
  return (
    get("year") +
    "-" +
    get("month") +
    "-" +
    get("day") +
    "T" +
    get("hour") +
    ":" +
    get("minute")
  );
};
export default function AnnouncementFormPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    staff = ["ADMIN", "TEACHER"].includes(user.role),
    s = useNoticeLoad(
      (signal) =>
        id ? getAnnouncement(id, signal) : Promise.resolve({ data: {} }),
      [id],
      staff,
    );
  if (!staff)
    return (
      <Card className="p-6">
        Announcement management is restricted to administrators and assigned
        teachers.
      </Card>
    );
  if (s.loading) return <Loading label="Loading announcement…" />;
  if (s.error) return <NoticeError state={s} />;
  return (
    s.data && (
      <Editor
        key={id || "new"}
        initial={s.data}
        teacher={user.role === "TEACHER"}
      />
    )
  );
}
function Editor({ initial, teacher }) {
  const nav = useNavigate(),
    [form, setForm] = useState({
      title: "",
      content: "",
      audienceType: teacher ? "BATCH" : "ALL",
      targetRole: "",
      courseId: "",
      batchId: "",
      ...initial,
      publishAt: toLocal(initial.publishAt),
    }),
    [scheduled, setScheduled] = useState(!!initial.publishAt),
    [saved, setSaved] = useState(initial.id ? initial : null),
    [preview, setPreview] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const edit = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  if (initial.id && (!initial.canManage || initial.status !== "DRAFT"))
    return (
      <Card className="p-6">
        <p>
          This announcement cannot be edited. Published content is preserved.
        </p>
        <Link
          className="button-secondary mt-4"
          to={"/announcements/" + initial.id}
        >
          View announcement
        </Link>
      </Card>
    );
  const save = async (event, publish = false) => {
    event.preventDefault();
    if (busy) return;
    if (
      publish &&
      !window.confirm(
        scheduled
          ? "Schedule this announcement for the selected institute-local time?"
          : "Publish this announcement to the selected audience?",
      )
    )
      return;
    setBusy(true);
    setError("");
    try {
      const data = {
        title: form.title,
        content: form.content,
        audienceType: form.audienceType,
        targetRole: form.audienceType === "ROLE" ? form.targetRole : null,
        courseId: form.audienceType === "COURSE" ? Number(form.courseId) : null,
        batchId: form.audienceType === "BATCH" ? Number(form.batchId) : null,
        publishAt: scheduled
          ? new Date(form.publishAt + "+05:30").toISOString()
          : null,
        version: saved?.version,
      };
      let a = (await saveAnnouncement(saved?.id, data)).data;
      setSaved(a);
      if (publish) a = (await publishAnnouncement(a.id, a.version)).data;
      nav("/announcements/" + a.id, {
        state: {
          message: publish
            ? a.status === "SCHEDULED"
              ? "Announcement scheduled"
              : "Announcement published; delivery is queued"
            : "Draft saved",
        },
      });
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <NoticeHeading
        title={initial.id ? "Edit Announcement" : "Create Announcement"}
        subtitle="Plain-text notices with secure role, course, and batch targeting."
      />
      <ErrorNotice message={error} />
      {saved && !initial.id && (
        <p role="status" className="mb-4 text-sm text-slate-500">
          Draft #{saved.id} is saved. Retrying will update this draft.
        </p>
      )}
      <form onSubmit={(e) => save(e, false)}>
        <Card className="space-y-5 p-6">
          <Field label="Title *">
            <Input
              required
              maxLength={200}
              value={form.title}
              onChange={(e) => edit("title", e.target.value)}
            />
          </Field>
          <Field label="Content *">
            <textarea
              className="input min-h-64"
              required
              maxLength={20000}
              value={form.content}
              onChange={(e) => edit("content", e.target.value)}
            />
          </Field>
          <p className="text-xs text-slate-500">
            Plain text only. Scripts and HTML are displayed as text.
          </p>
          <div className="grid gap-5 md:grid-cols-2">
            <Field label="Audience *">
              <Select
                value={form.audienceType}
                disabled={teacher}
                onChange={(e) =>
                  setForm({
                    ...form,
                    audienceType: e.target.value,
                    targetRole: "",
                    courseId: "",
                    batchId: "",
                  })
                }
              >
                {(teacher ? ["BATCH"] : ["ALL", "ROLE", "COURSE", "BATCH"]).map(
                  (a) => (
                    <option key={a}>{a}</option>
                  ),
                )}
              </Select>
            </Field>
            {form.audienceType === "ROLE" && (
              <Field label="Target role *">
                <Select
                  required
                  value={form.targetRole || ""}
                  onChange={(e) => edit("targetRole", e.target.value)}
                >
                  <option value="">Select role</option>
                  {["ADMIN", "TEACHER", "STUDENT", "PARENT"].map((a) => (
                    <option key={a}>{a}</option>
                  ))}
                </Select>
              </Field>
            )}
            {form.audienceType === "COURSE" && (
              <RecordSelect
                endpoint="/courses"
                label="Target course"
                status=""
                value={form.courseId}
                selectedLabel={initial.courseName}
                onChange={(v) => edit("courseId", v)}
              />
            )}{" "}
            {form.audienceType === "BATCH" && (
              <RecordSelect
                endpoint="/batches"
                label="Target batch"
                status=""
                value={form.batchId}
                selectedLabel={initial.batchName}
                onChange={(v) => edit("batchId", v)}
              />
            )}
            <Field label="Publication mode">
              <Select
                value={scheduled ? "SCHEDULE" : "NOW"}
                onChange={(e) => setScheduled(e.target.value === "SCHEDULE")}
              >
                <option value="NOW">Publish immediately when confirmed</option>
                <option value="SCHEDULE">Schedule publication</option>
              </Select>
            </Field>
            {scheduled && (
              <Field label="Scheduled date & time (Asia/Kolkata) *">
                <Input
                  type="datetime-local"
                  required
                  value={form.publishAt}
                  onChange={(e) => edit("publishAt", e.target.value)}
                />
              </Field>
            )}
          </div>
          <p className="text-xs leading-6 text-slate-500">
            Audience membership is captured at publication. Links are checked
            again when recipients open the content. Parent batch/course access
            requires active verified relationships.
          </p>
        </Card>
        <div className="mt-5 flex flex-wrap gap-3">
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save draft"}
          </Button>
          <Button
            type="submit"
            variant="secondary"
            disabled={busy}
            onClick={(e) => {
              if (e.currentTarget.form.reportValidity()) save(e, true);
              else e.preventDefault();
            }}
          >
            {scheduled ? "Save and schedule" : "Save and publish"}
          </Button>
          <Button variant="secondary" onClick={() => setPreview(true)}>
            Preview
          </Button>
          <Link className="button-secondary" to="/announcements">
            Cancel
          </Link>
        </div>
      </form>
      {preview && (
        <Modal title="Announcement preview" onClose={() => setPreview(false)}>
          <article className="p-6">
            <h2 className="mb-4 break-words text-xl font-semibold">
              {form.title || "Untitled announcement"}
            </h2>
            <p className="whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
              {form.content || "No content entered."}
            </p>
            <p className="mt-5 text-xs text-slate-500">
              Audience: {form.audienceType}. This preview has not been
              published.
            </p>
          </article>
        </Modal>
      )}
    </>
  );
}
