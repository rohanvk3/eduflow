import { api } from "../api/client";
import { notificationChanged } from "./notificationService";
const changed = async (promise) => {
  const r = await promise;
  notificationChanged();
  window.dispatchEvent(new Event("eduflow:announcements-changed"));
  return r;
};
export const getAnnouncements = (params, signal) =>
  api.get("/announcements", { params, signal });
export const getMyAnnouncements = (params, signal) =>
  api.get("/announcements/me", { params, signal });
export const getAnnouncement = (id, signal) =>
  api.get(`/announcements/${id}`, { signal });
export const saveAnnouncement = (id, data) =>
  changed(
    id
      ? api.put(`/announcements/${id}`, data)
      : api.post("/announcements", data),
  );
export const publishAnnouncement = (id, version) =>
  changed(api.post(`/announcements/${id}/publish`, { version }));
export const archiveAnnouncement = (id, version) =>
  changed(api.post(`/announcements/${id}/archive`, { version }));
export const getDeliveryOverview = (signal) =>
  api.get("/announcements/overview", { signal });
