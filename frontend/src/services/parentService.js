import { api } from "../api/client";
export { financeError as parentError, saveDownload } from "./financeService";
const changed = async (promise) => {
  const r = await promise;
  window.dispatchEvent(new Event("eduflow:parents-changed"));
  return r;
};
export const getParents = (params, signal) =>
  api.get("/parents", { params, signal });
export const getParent = (id, signal) => api.get(`/parents/${id}`, { signal });
export const createParent = (data) => changed(api.post("/parents", data));
export const updateParent = (id, data) =>
  changed(api.put(`/parents/${id}`, data));
export const getParentProfile = (signal) => api.get("/parents/me", { signal });
export const updateParentProfile = (data) =>
  changed(api.put("/parents/me", data));
export const getParentLinks = (params, signal) =>
  api.get("/parents/links", { params, signal });
export const createParentLink = (data) =>
  changed(api.post("/parents/links", data));
export const verifyParentLink = (id, data) =>
  changed(api.post(`/parents/links/${id}/verify`, data));
export const revokeParentLink = (id, data) =>
  changed(api.post(`/parents/links/${id}/revoke`, data));
export const getParentLinkHistory = (id, params, signal) =>
  api.get(`/parents/links/${id}/history`, { params, signal });
export const getMyChildren = (params, signal) =>
  api.get("/parents/me/children", { params, signal });
