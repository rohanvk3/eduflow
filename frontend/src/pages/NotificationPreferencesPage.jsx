import { useState } from "react";
import { Card, Button, Loading, ErrorNotice, Toast } from "../components/UI";
import {
  NoticeHeading,
  NoticeError,
  useNoticeLoad,
} from "../components/NotificationUI";
import {
  getNotificationPreferences,
  saveNotificationPreferences,
  notificationError,
} from "../services/notificationService";
export default function NotificationPreferencesPage() {
  const s = useNoticeLoad(getNotificationPreferences, []);
  return (
    <>
      <NoticeHeading
        title="Notification Preferences"
        subtitle="Choose the optional in-app updates you want to receive."
      />
      <NoticeError state={s} />
      {s.loading ? (
        <Loading label="Loading notification preferences…" />
      ) : (
        s.data && <Preferences initial={s.data} />
      )}
    </>
  );
}
function Preferences({ initial }) {
  const [form, setForm] = useState(initial),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const options = [
    [
      "announcementsEnabled",
      "Announcements",
      "Institute, role, course and batch notices",
    ],
    [
      "examNotificationsEnabled",
      "Exams",
      "Published exams, reminders and published results",
    ],
    [
      "assignmentNotificationsEnabled",
      "Assignments",
      "Published homework, due reminders and feedback",
    ],
    [
      "feeNotificationsEnabled",
      "Fees",
      "Installment reminders and verified payment entries",
    ],
    [
      "materialNotificationsEnabled",
      "Study Materials",
      "Published resources in eligible courses and batches",
    ],
    [
      "attendanceNotificationsEnabled",
      "Attendance",
      "Finalized attendance for verified linked children; off by default",
    ],
  ];
  return (
    <>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            const r = await saveNotificationPreferences(form);
            setForm(r.data);
            setMessage("Notification preferences saved");
          } catch (ex) {
            setError(await notificationError(ex));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <Card className="divide-y divide-slate-100">
          {options.map(([key, label, description]) => (
            <label
              key={key}
              className="flex cursor-pointer items-center justify-between gap-5 p-5"
            >
              <span>
                <span className="block text-sm font-semibold">{label}</span>
                <span className="mt-1 block text-xs leading-5 text-slate-500">
                  {description}
                </span>
              </span>
              <input
                type="checkbox"
                role="switch"
                aria-label={label}
                checked={!!form[key]}
                disabled={busy}
                className="h-5 w-5 shrink-0 accent-indigo-600"
                onChange={(e) => setForm({ ...form, [key]: e.target.checked })}
              />
            </label>
          ))}
        </Card>
        <p className="my-5 text-xs leading-6 text-slate-500">
          Preferences apply when queued notifications are delivered. They do not
          hide announcements you are authorized to read. Mandatory
          account/security updates remain enabled. This page controls in-app
          updates; email and SMS delivery are not implemented.
        </p>
        <Button type="submit" disabled={busy}>
          {busy ? "Saving…" : "Save preferences"}
        </Button>
      </form>
      <Toast message={message} onDismiss={() => setMessage("")} />
    </>
  );
}
