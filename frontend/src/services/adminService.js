import { api } from "../api/client";
const changed = async (p) => {
  const r = await p;
  window.dispatchEvent(new Event("eduflow:branding-changed"));
  return r;
};
export const getAdmin = (path, params, signal) =>
  api.get("/admin/" + path, { params, signal });
export const saveInstitute = (data) =>
  changed(api.put("/admin/institute-settings", data));
export const uploadLogo = (file) => {
  const data = new FormData();
  data.append("file", file);
  return changed(api.post("/admin/institute-settings/logo", data));
};
export const changeUserStatus = (id, status) =>
  api.patch(`/admin/users/${id}/status`, { status });
export const changeUserRole = (id, role) =>
  api.patch(`/admin/users/${id}/roles`, { role });
export const issueRecovery = (id) =>
  api.post(`/admin/users/${id}/password-reset`);
export const getProfile = (signal) => api.get("/settings/profile", { signal });
export const saveProfile = (fullName) =>
  api.put("/settings/profile", { fullName });
export const changePassword = (data) =>
  api.put("/settings/security/password", data);
export const requestRecovery = (email) =>
  api.post("/auth/password-reset/request", { email });
export const confirmRecovery = (data) =>
  api.post("/auth/password-reset/confirm", data);
