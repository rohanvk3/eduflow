import { api } from "../api/client";
export const getAnalytics = (path, params, signal) =>
  api.get("/analytics/" + path, { params, signal });
export const getOwnProgress = async (params, signal) => {
  const { data } = await api.get("/students/me", { signal });
  return getAnalytics(`students/${data.id}/progress`, params, signal);
};
