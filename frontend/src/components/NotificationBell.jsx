import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Bell, X } from "lucide-react";
import { Button, EmptyState, Skeleton, ErrorNotice } from "./UI";
import { NotificationItem } from "./NotificationUI";
import {
  getNotifications,
  getUnreadCount,
  notificationError,
} from "../services/notificationService";
import { useAuth } from "../auth/AuthContext";
export default function NotificationBell() {
  const { user } = useAuth(),
    [open, setOpen] = useState(false),
    [count, setCount] = useState(null),
    [rows, setRows] = useState([]),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [revision, setRevision] = useState(0),
    ref = useRef(null),
    button = useRef(null),
    location = useLocation();
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      getUnreadCount(controller.signal),
      getNotifications({ page: 0, size: 5 }, controller.signal),
    ])
      .then(([c, n]) => {
        if (!controller.signal.aborted) {
          setCount(c.data.count);
          setRows(n.data.content);
        }
      })
      .catch(async (e) => {
        if (!controller.signal.aborted) {
          setCount(null);
          setError(await notificationError(e));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [revision, user.id, user.userId, user.email]);
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
  useEffect(() => setOpen(false), [location.pathname]);
  useEffect(() => {
    if (!open) return;
    const outside = (e) => {
      if (!ref.current?.contains(e.target)) setOpen(false);
    };
    const escape = (e) => {
      if (e.key === "Escape") {
        setOpen(false);
        button.current?.focus();
      }
    };
    window.addEventListener("pointerdown", outside);
    window.addEventListener("keydown", escape);
    return () => {
      window.removeEventListener("pointerdown", outside);
      window.removeEventListener("keydown", escape);
    };
  }, [open]);
  return (
    <div className="relative" ref={ref}>
      <button
        ref={button}
        type="button"
        className="button-icon relative"
        aria-label={
          count == null
            ? "Open notifications"
            : `Open notifications, ${count} unread`
        }
        aria-expanded={open}
        aria-haspopup="dialog"
        aria-controls="notification-popover"
        onClick={() => setOpen((v) => !v)}
      >
        <Bell size={19} />
        {count > 0 && (
          <span className="absolute -right-1 -top-1 min-w-4 rounded-full bg-indigo-600 px-1 text-[9px] leading-4 text-white">
            {count > 99 ? "99+" : count}
          </span>
        )}
      </button>
      {open && (
        <div
          id="notification-popover"
          role="dialog"
          aria-label="Recent notifications"
          className="fixed left-4 right-4 top-[70px] z-50 max-h-[75vh] overflow-y-auto rounded-xl border border-slate-200 bg-surface shadow-lg sm:absolute sm:left-auto sm:right-0 sm:top-12 sm:w-[380px]"
        >
          <div className="flex items-center justify-between border-b border-slate-100 p-4">
            <h2 className="text-sm font-semibold">Notifications</h2>
            <Button
              variant="icon"
              aria-label="Close notifications"
              onClick={() => {
                setOpen(false);
                button.current?.focus();
              }}
            >
              <X size={16} />
            </Button>
          </div>
          {loading ? (
            <div className="space-y-3 p-4">
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
            </div>
          ) : error ? (
            <div className="p-4">
              <ErrorNotice message={error} />
              <Button
                variant="secondary"
                onClick={() => setRevision((v) => v + 1)}
              >
                Retry
              </Button>
            </div>
          ) : rows.length ? (
            rows.map((n) => (
              <NotificationItem
                key={n.id}
                notification={n}
                compact
                onNavigate={() => setOpen(false)}
              />
            ))
          ) : (
            <EmptyState
              compact
              title="You're all caught up"
              description="Your institute updates will appear here."
            />
          )}
          <div className="flex justify-between gap-2 border-t border-slate-100 p-4">
            <Link
              to="/notifications"
              className="text-xs font-medium text-indigo-600"
              onClick={() => setOpen(false)}
            >
              View all notifications
            </Link>
            <Link
              to="/settings/notifications"
              className="text-xs text-slate-500"
              onClick={() => setOpen(false)}
            >
              Preferences
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
