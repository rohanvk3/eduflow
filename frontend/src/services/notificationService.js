import { api } from "../api/client";
export { financeError as notificationError } from "./financeService";
export const notificationChanged = () =>
  window.dispatchEvent(new Event("eduflow:notifications-changed"));
const changed = async (promise) => {
  const r = await promise;
  notificationChanged();
  return r;
};
export const getNotifications = (params, signal) =>
  api.get("/notifications", { params, signal });
export const getUnreadCount = (signal) =>
  api.get("/notifications/unread-count", { signal });
export const markNotificationRead = (id) =>
  changed(api.patch(`/notifications/${id}/read`));
export const markAllNotificationsRead = () =>
  changed(api.patch("/notifications/read-all"));
export const dismissNotification = (id) =>
  changed(api.delete(`/notifications/${id}`));
export const getNotificationPreferences = (signal) =>
  api.get("/notifications/preferences", { signal });
export const saveNotificationPreferences = (data) =>
  changed(api.put("/notifications/preferences", data));
export const safeNotificationAction = (url) =>
  typeof url === "string" &&
  /^\/(?:announcements|notifications|exams|assignments|my-assignments|my-results|my-fees|resources|my-resources|parent|dashboard)(?:\/[A-Za-z0-9_-]+)*$/.test(
    url,
  );
export function relativeNotificationTime(value) {
  const delta = (new Date(value).getTime() - Date.now()) / 1000;
  const fmt = new Intl.RelativeTimeFormat("en", { numeric: "auto" });
  if (Math.abs(delta) < 60) return fmt.format(Math.round(delta), "second");
  if (Math.abs(delta) < 3600)
    return fmt.format(Math.round(delta / 60), "minute");
  if (Math.abs(delta) < 86400)
    return fmt.format(Math.round(delta / 3600), "hour");
  return fmt.format(Math.round(delta / 86400), "day");
}
