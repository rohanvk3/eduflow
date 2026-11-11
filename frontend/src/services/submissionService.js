import { api } from "../api/client";
import { changed } from "./assignmentService";
export const getSubmissions = (id, params, signal) =>
  api.get(`/assignments/${id}/submissions`, { params, signal });
export const getPendingRecipients = (id, params, signal) =>
  api.get(`/assignments/${id}/pending-recipients`, { params, signal });
export const getSubmission = (id, signal) =>
  api.get(`/assignments/submissions/${id}`, { signal });
export const getOwnSubmission = (id, signal) =>
  api.get(`/assignments/${id}/my-submission`, { signal });
export const getMySubmissions = (params, signal) =>
  api.get("/assignments/me/submissions", { params, signal });
export const getSubmissionHistory = (id, signal) =>
  api.get(`/assignments/submissions/${id}/history`, { signal });
export const getSubmissionFiles = (id, signal) =>
  api.get(`/assignments/submissions/${id}/attachments`, { signal });
export const saveSubmission = (
  assignmentId,
  id,
  data,
  files = [],
  onUploadProgress,
) => {
  const path = id
    ? `/assignments/submissions/${id}`
    : `/assignments/${assignmentId}/submissions`;
  let body = data;
  const config = { onUploadProgress };
  if (files.length) {
    body = new FormData();
    body.append(
      "data",
      new Blob([JSON.stringify(data)], { type: "application/json" }),
    );
    files.forEach((file) => body.append("files", file));
  }
  return changed(
    id ? api.put(path, body, config) : api.post(path, body, config),
  );
};
export const uploadSubmissionFiles = (id, version, files, onUploadProgress) => {
  const form = new FormData();
  files.forEach((file) => form.append("files", file));
  return changed(
    api.post(`/assignments/submissions/${id}/attachments`, form, {
      params: { version },
      onUploadProgress,
    }),
  );
};
export const downloadSubmissionFile = (id) =>
  api.get(`/assignments/submission-attachments/${id}/download`, {
    responseType: "blob",
  });
export const gradeSubmission = (id, data) =>
  changed(api.put(`/assignments/submissions/${id}/grade`, data));
