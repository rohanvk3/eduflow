import { useState } from "react";
import { Link } from "react-router-dom";
import { Check, Trash2, ArrowUpRight } from "lucide-react";
import { Button, Badge, ErrorNotice } from "./UI";
import {
  markNotificationRead,
  dismissNotification,
  notificationError,
  relativeNotificationTime,
  safeNotificationAction,
} from "../services/notificationService";
export {
  useFinanceLoad as useNoticeLoad,
  FinanceError as NoticeError,
  Field,
} from "./FinanceUI";
export const notificationTypes = [
  "ANNOUNCEMENT",
  "EXAM_SCHEDULED",
  "EXAM_RESULT_PUBLISHED",
  "ASSIGNMENT_CREATED",
  "ASSIGNMENT_DUE",
  "ASSIGNMENT_GRADED",
  "FEE_DUE",
  "FEE_OVERDUE",
  "PAYMENT_RECORDED",
  "STUDY_MATERIAL_PUBLISHED",
  "ATTENDANCE_FINALIZED",
  "GENERAL",
];
export const typeLabel = (value) => value.toLowerCase().replaceAll("_", " ");
export function NoticeHeading({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Institute updates</p>
        <h1 className="page-title">{title}</h1>
        {subtitle && (
          <p className="mt-2 max-w-xl text-sm text-slate-500">{subtitle}</p>
        )}
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function NotificationItem({
  notification: n,
  compact = false,
  onNavigate,
  onChanged,
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const run = async (action) => {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await action();
      onChanged?.();
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <article
      className={
        "min-w-0 border-b border-slate-100 p-4 " +
        (!n.isRead ? "bg-indigo-50/40" : "")
      }
      aria-label={n.title}
    >
      <div className="flex items-start gap-3">
        <span
          aria-label={n.isRead ? "Read" : "Unread"}
          className={
            "mt-1.5 h-2 w-2 shrink-0 rounded-full " +
            (n.isRead ? "bg-slate-200" : "bg-indigo-500")
          }
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <h3 className="break-words text-sm font-semibold">{n.title}</h3>
            <time dateTime={n.createdAt} className="text-[10px] text-slate-500">
              {relativeNotificationTime(n.createdAt)}
            </time>
          </div>
          <p
            className={
              "mt-2 break-words text-xs leading-5 text-slate-500 " +
              (compact ? "line-clamp-2" : "")
            }
          >
            {n.message}
          </p>
          {!compact && <Badge className="mt-3">{typeLabel(n.type)}</Badge>}
          <div className="mt-3 flex flex-wrap gap-2">
            {!n.isRead && (
              <Button
                variant="secondary"
                disabled={busy}
                aria-label={"Mark " + n.title + " as read"}
                onClick={() => run(() => markNotificationRead(n.id))}
              >
                <Check size={13} />
                {compact ? "Read" : "Mark as read"}
              </Button>
            )}
            {safeNotificationAction(n.actionUrl) && (
              <Link
                className="button-secondary"
                to={n.actionUrl}
                onClick={onNavigate}
              >
                Open <ArrowUpRight size={13} />
              </Link>
            )}
            {!compact && (
              <Button
                variant="icon"
                disabled={busy}
                aria-label={"Dismiss " + n.title}
                onClick={() => run(() => dismissNotification(n.id))}
              >
                <Trash2 size={15} />
              </Button>
            )}
          </div>
          <ErrorNotice message={error} />
        </div>
      </div>
    </article>
  );
}
