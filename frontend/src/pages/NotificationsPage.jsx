import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Card,
  Button,
  Input,
  Select,
  Pagination,
  EmptyState,
  Skeleton,
  ErrorNotice,
} from "../components/UI";
import {
  NoticeHeading,
  NotificationItem,
  notificationTypes,
  typeLabel,
  useNoticeLoad,
  NoticeError,
} from "../components/NotificationUI";
import {
  getNotifications,
  markAllNotificationsRead,
  notificationError,
} from "../services/notificationService";
export default function NotificationsPage() {
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [type, setType] = useState(""),
    [readStatus, setRead] = useState("ALL"),
    [search, setSearch] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [revision, setRevision] = useState(0),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const state = useNoticeLoad(
    (signal) =>
      getNotifications(
        {
          page,
          size,
          type: type || undefined,
          readStatus,
          search,
          fromDate: from || undefined,
          toDate: to || undefined,
        },
        signal,
      ),
    [page, size, type, readStatus, search, from, to, revision],
  );
  useEffect(() => {
    const changed = () => setRevision((v) => v + 1);
    window.addEventListener("eduflow:notifications-changed", changed);
    const timer = setInterval(changed, 60000);
    return () => {
      clearInterval(timer);
      window.removeEventListener("eduflow:notifications-changed", changed);
    };
  }, []);
  const filter = (setter) => (value) => {
    setter(value);
    setPage(0);
  };
  const readAll = async () => {
    setBusy(true);
    setError("");
    try {
      await markAllNotificationsRead();
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <NoticeHeading
        title="Notifications"
        subtitle="Your institute updates, reminders, and learning activity."
      >
        <Link className="button-secondary" to="/settings/notifications">
          Notification preferences
        </Link>
        <Button disabled={busy} onClick={readAll}>
          {busy ? "Updating…" : "Mark all as read"}
        </Button>
      </NoticeHeading>
      <Card className="mb-5 grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-5">
        <Input
          aria-label="Search notifications"
          placeholder="Search notifications…"
          maxLength={100}
          value={search}
          onChange={(e) => filter(setSearch)(e.target.value)}
        />
        <Select
          aria-label="Read status"
          value={readStatus}
          onChange={(e) => filter(setRead)(e.target.value)}
        >
          <option value="ALL">All notifications</option>
          <option value="UNREAD">Unread</option>
          <option value="READ">Read</option>
        </Select>
        <Select
          aria-label="Notification type"
          value={type}
          onChange={(e) => filter(setType)(e.target.value)}
        >
          <option value="">All types</option>
          {notificationTypes.map((t) => (
            <option key={t} value={t}>
              {typeLabel(t)}
            </option>
          ))}
        </Select>
        <label className="field-label">
          From
          <Input
            aria-label="From date"
            type="date"
            value={from}
            onChange={(e) => filter(setFrom)(e.target.value)}
          />
        </label>
        <label className="field-label">
          To
          <Input
            aria-label="To date"
            type="date"
            value={to}
            onChange={(e) => filter(setTo)(e.target.value)}
          />
        </label>
      </Card>
      <NoticeError state={state} />
      <ErrorNotice message={error} />
      <Card>
        {state.loading ? (
          <div className="space-y-4 p-5">
            <Skeleton className="h-28" />
            <Skeleton className="h-28" />
          </div>
        ) : state.data?.content.length ? (
          state.data.content.map((n) => (
            <NotificationItem key={n.id} notification={n} />
          ))
        ) : (
          !state.error && (
            <EmptyState
              title="No notifications found"
              description="Adjust your filters or check back for institute updates."
            />
          )
        )}
        <Pagination
          noun="notifications"
          page={page}
          size={size}
          total={state.data?.totalElements}
          totalPages={state.data?.totalPages}
          loading={state.loading}
          onPageChange={setPage}
          onSizeChange={filter(setSize)}
        />
      </Card>
      <p className="mt-4 text-xs text-slate-500">
        Dismiss removes your copy from the inbox. It preserves the delivery
        record and does not delete an announcement.
      </p>
    </>
  );
}
