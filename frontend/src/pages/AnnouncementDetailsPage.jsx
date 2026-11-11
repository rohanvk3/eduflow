import { useState } from "react";
import { Link, useParams, useLocation } from "react-router-dom";
import { Card, Button, Badge, ErrorNotice, Loading } from "../components/UI";
import {
  NoticeHeading,
  NoticeError,
  useNoticeLoad,
} from "../components/NotificationUI";
import { parentTime } from "../components/ParentUI";
import {
  getAnnouncement,
  publishAnnouncement,
  archiveAnnouncement,
} from "../services/announcementService";
import { notificationError } from "../services/notificationService";
export default function AnnouncementDetailsPage() {
  const { id } = useParams(),
    location = useLocation(),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    s = useNoticeLoad((signal) => getAnnouncement(id, signal), [id]);
  const run = async (action, message) => {
    if (busy || !window.confirm(message)) return;
    setBusy(true);
    setError("");
    try {
      await action(id, s.data.version);
      s.reload();
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  if (s.loading) return <Loading label="Loading announcement…" />;
  return (
    <>
      <NoticeHeading
        title="Announcement"
        subtitle="Institute notices and authorized audience updates."
      >
        <Link className="button-secondary" to="/announcements">
          All announcements
        </Link>
        {s.data?.canManage && s.data.status === "DRAFT" && (
          <>
            <Link
              className="button-secondary"
              to={"/announcements/" + id + "/edit"}
            >
              Edit draft
            </Link>
            <Button
              disabled={busy}
              onClick={() =>
                run(
                  publishAnnouncement,
                  s.data.publishAt
                    ? "Confirm the selected publication schedule?"
                    : "Publish this announcement now?",
                )
              }
            >
              {s.data.publishAt ? "Schedule" : "Publish now"}
            </Button>
          </>
        )}
        {s.data?.canManage && s.data.status !== "ARCHIVED" && (
          <Button
            variant="secondary"
            disabled={busy}
            onClick={() =>
              run(
                archiveAnnouncement,
                "Archive this announcement? Recipients will lose content access; delivery history is retained.",
              )
            }
          >
            Archive
          </Button>
        )}
      </NoticeHeading>
      <NoticeError state={s} />
      <ErrorNotice message={s.data?.publicationIssue} />
      <ErrorNotice message={error} />
      {location.state?.message && (
        <p role="status" className="mb-4 text-sm text-emerald-600">
          {location.state.message}
        </p>
      )}
      {s.data && (
        <>
          <Card className="p-6 md:p-8">
            <div className="mb-5 flex flex-wrap items-center gap-3">
              <Badge
                tone={
                  s.data.status === "PUBLISHED"
                    ? "success"
                    : s.data.status === "SCHEDULED"
                      ? "indigo"
                      : "neutral"
                }
              >
                {s.data.status}
              </Badge>
              <span className="text-xs text-slate-500">
                {s.data.batchName ||
                  s.data.courseName ||
                  s.data.targetRole ||
                  "Institute-wide"}
              </span>
            </div>
            <h2 className="mb-5 break-words text-2xl font-semibold">
              {s.data.title}
            </h2>
            <p className="whitespace-pre-wrap break-words text-sm leading-8 text-slate-600">
              {s.data.content}
            </p>
            <div className="mt-7 border-t border-slate-100 pt-5 text-xs leading-6 text-slate-500">
              <p>By {s.data.createdBy}</p>
              <p>
                {s.data.status === "SCHEDULED"
                  ? "Scheduled for " + parentTime(s.data.publishAt)
                  : s.data.publishedAt
                    ? "Published " + parentTime(s.data.publishedAt)
                    : "Created " + parentTime(s.data.createdAt)}
              </p>
            </div>
          </Card>
          {s.data.canManage && s.data.status === "PUBLISHED" && (
            <Card className="mt-5 p-5">
              <h3 className="mb-3 text-sm font-semibold">Delivery overview</h3>
              <p className="text-xs leading-6 text-slate-500">
                Recipients captured: {s.data.recipients} · Delivered:{" "}
                {s.data.delivered} · Skipped: {s.data.skipped} ·{" "}
                {s.data.deliveryComplete ? "Complete" : "Queued / processing"}
              </p>
              <p className="mt-2 text-xs text-slate-500">
                Skipped recipients include disabled preferences or users who no
                longer have access. Content stays immutable after publication.
              </p>
              <Button className="mt-4" variant="secondary" onClick={s.reload}>
                Refresh delivery status
              </Button>
            </Card>
          )}
          {s.data.canManage && s.data.status === "SCHEDULED" && (
            <p className="mt-5 text-xs leading-6 text-slate-500">
              Publication is handled by the database-coordinated scheduler. A
              teacher author must still be assigned to the target batch when it
              runs.
            </p>
          )}
        </>
      )}
    </>
  );
}
