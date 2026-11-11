import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Card, EmptyState, Skeleton } from "./UI";
import { NoticeError, useNoticeLoad, NotificationItem } from "./NotificationUI";
import { getNotifications } from "../services/notificationService";
import {
  getMyAnnouncements,
  getAnnouncements,
  getDeliveryOverview,
} from "../services/announcementService";
import { parentTime } from "./ParentUI";
export default function NotificationDashboard() {
  const { user } = useAuth(),
    staff = ["ADMIN", "TEACHER"].includes(user.role),
    [revision, setRevision] = useState(0);
  const own = useNoticeLoad(
    (signal) => getNotifications({ page: 0, size: 3 }, signal),
    [revision],
  );
  const news = useNoticeLoad(
    (signal) => getMyAnnouncements({ page: 0, size: 3 }, signal),
    [revision],
  );
  const delivery = useNoticeLoad(getDeliveryOverview, [revision], staff);
  const authored = useNoticeLoad(
    (signal) =>
      getAnnouncements({ page: 0, size: 3, createdByMe: true }, signal),
    [revision],
    user.role === "TEACHER",
  );
  const scheduled = useNoticeLoad(
    (signal) =>
      getAnnouncements({ page: 0, size: 3, status: "SCHEDULED" }, signal),
    [revision],
    staff,
  );
  useEffect(() => {
    const refresh = () => setRevision((v) => v + 1),
      timer = setInterval(refresh, 60000);
    window.addEventListener("eduflow:notifications-changed", refresh);
    window.addEventListener("focus", refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener("eduflow:notifications-changed", refresh);
      window.removeEventListener("focus", refresh);
    };
  }, []);
  return (
    <Card className="mb-6 p-5">
      <div className="mb-5 flex flex-wrap justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold">
            Notifications & announcements
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            Your updates refresh every 60 seconds and on focus.
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Link
            className="text-xs font-medium text-indigo-600"
            to="/notifications"
          >
            Notification center
          </Link>
          <Link
            className="text-xs font-medium text-indigo-600"
            to="/announcements"
          >
            {staff ? "Manage announcements" : "Read announcements"}
          </Link>
        </div>
      </div>
      <div className="grid gap-5 lg:grid-cols-2">
        <div>
          <h3 className="mb-3 text-xs font-semibold">Recent notifications</h3>
          <NoticeError state={own} />
          {own.loading ? (
            <Skeleton className="h-24" />
          ) : own.data?.content.length ? (
            own.data.content.map((n) => (
              <NotificationItem key={n.id} notification={n} compact />
            ))
          ) : (
            !own.error && <EmptyState compact title="No recent updates" />
          )}
        </div>
        <div>
          <h3 className="mb-3 text-xs font-semibold">
            Recent announcements for you
          </h3>
          <NoticeError state={news} />
          {news.loading ? (
            <Skeleton className="h-24" />
          ) : news.data?.content.length ? (
            news.data.content.map((a) => (
              <Link
                key={a.id}
                className="mb-3 block rounded-lg bg-slate-50 p-3 text-xs"
                to={"/announcements/" + a.id}
              >
                <span className="font-medium">{a.title}</span>
                <span className="mt-1 block text-slate-500">
                  {parentTime(a.publishedAt)}
                </span>
              </Link>
            ))
          ) : (
            !news.error && (
              <p className="text-xs text-slate-500">
                No published announcements for your current audience.
              </p>
            )
          )}
        </div>
      </div>
      {staff && (
        <div className="mt-5 border-t border-slate-100 pt-5">
          <NoticeError state={delivery} />
          {delivery.data && (
            <p className="text-xs leading-6 text-slate-500">
              {user.role === "ADMIN"
                ? "Institute notification delivery"
                : "Assigned-batch announcement delivery"}
              : {delivery.data.jobs} jobs · {delivery.data.pendingJobs} pending
              · {delivery.data.delivered} delivered · {delivery.data.skipped}{" "}
              skipped · {delivery.data.scheduledAnnouncements} scheduled
              announcements
            </p>
          )}
          <h3 className="mb-3 mt-4 text-xs font-semibold">
            Scheduled announcements
          </h3>
          <NoticeError state={scheduled} />
          {scheduled.data?.content.map((a) => (
            <Link
              key={a.id}
              className="mb-2 block text-xs text-indigo-600"
              to={"/announcements/" + a.id}
            >
              {a.title} · {parentTime(a.publishAt)}
            </Link>
          ))}
          {scheduled.data && !scheduled.data.content.length && (
            <p className="text-xs text-slate-500">
              No scheduled announcements in your management scope.
            </p>
          )}
        </div>
      )}
      {user.role === "TEACHER" && (
        <div className="mt-5 border-t border-slate-100 pt-5">
          <h3 className="mb-3 text-xs font-semibold">My announcements</h3>
          <NoticeError state={authored} />
          {authored.loading && <Skeleton className="h-16" />}
          {authored.data?.content.map((a) => (
            <Link
              key={a.id}
              className="mb-2 block text-xs text-indigo-600"
              to={"/announcements/" + a.id}
            >
              {a.title} · {a.status.toLowerCase()}
            </Link>
          ))}
          {authored.data && !authored.data.content.length && (
            <p className="text-xs text-slate-500">
              You have no announcements in your current assigned batches.
            </p>
          )}
        </div>
      )}
    </Card>
  );
}
