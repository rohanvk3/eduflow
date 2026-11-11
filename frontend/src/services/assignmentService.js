import { api } from "../api/client";
export const changed = async (promise) => {
  const result = await promise;
  window.dispatchEvent(new Event("eduflow:assignments-changed"));
  return result;
};
export const getAssignments = (params, signal) =>
  api.get("/assignments", { params, signal });
export const getAssignment = (id, signal) =>
  api.get(`/assignments/${id}`, { signal });
export const saveAssignment = (id, data) =>
  changed(
    id ? api.put(`/assignments/${id}`, data) : api.post("/assignments", data),
  );
export const publishAssignment = (id, version) =>
  changed(api.post(`/assignments/${id}/publish`, { version }));
export const closeAssignment = (id, version) =>
  changed(api.post(`/assignments/${id}/close`, { version }));
export const getAssignmentStats = (params, signal) =>
  api.get("/assignments/stats", { params, signal });
export const getAssignmentFiles = (id, signal) =>
  api.get(`/assignments/${id}/attachments`, { signal });
export const getUploadPolicy = (signal) =>
  api.get("/assignments/upload-policy", { signal });
export const uploadAssignmentFiles = (id, version, files, onUploadProgress) => {
  const form = new FormData();
  files.forEach((file) => form.append("files", file));
  return changed(
    api.post(`/assignments/${id}/attachments`, form, {
      params: { version },
      onUploadProgress,
    }),
  );
};
export const removeAssignmentFile = (id, version) =>
  changed(
    api.delete(`/assignments/attachments/${id}`, { params: { version } }),
  );
export const downloadAssignmentFile = (id) =>
  api.get(`/assignments/attachments/${id}/download`, { responseType: "blob" });
