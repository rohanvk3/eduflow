import { api } from "../api/client";
export { financeError as resourceError, saveDownload } from "./financeService";
export const changed = async (promise) => {
  const response = await promise;
  window.dispatchEvent(new Event("eduflow:resources-changed"));
  return response;
};
export const getMaterials = (params, signal) =>
  api.get("/resources/materials", { params, signal });
export const getMaterial = (id, signal) =>
  api.get(`/resources/materials/${id}`, { signal });
export const getResourceOptions = (signal) =>
  api.get("/resources/options", { signal });
export const getResourcePolicy = (signal) =>
  api.get("/resources/upload-policy", { signal });
export const getResourceStats = (signal) =>
  api.get("/resources/stats", { signal });
export const getMyResources = (params, signal) =>
  api.get("/resources/me", { params, signal });
export const saveMaterial = (id, data, file, onUploadProgress) => {
  if (id) return changed(api.put(`/resources/materials/${id}`, data));
  if (!file) return changed(api.post("/resources/materials", data));
  const form = new FormData();
  form.append(
    "data",
    new Blob([JSON.stringify(data)], { type: "application/json" }),
  );
  form.append("file", file);
  return changed(api.post("/resources/materials", form, { onUploadProgress }));
};
export const publishMaterial = (id, version) =>
  changed(api.post(`/resources/materials/${id}/publish`, { version }));
export const archiveMaterial = (id, version) =>
  changed(api.post(`/resources/materials/${id}/archive`, { version }));
export const getMaterialVersions = (id, signal) =>
  api.get(`/resources/materials/${id}/versions`, { signal });
export const uploadMaterialVersion = (
  id,
  version,
  file,
  changeNotes,
  onUploadProgress,
) => {
  const form = new FormData();
  form.append("file", file);
  form.append("changeNotes", changeNotes || "");
  return changed(
    api.post(`/resources/materials/${id}/versions`, form, {
      params: { version },
      onUploadProgress,
    }),
  );
};
export const downloadMaterial = (id, versionId) =>
  api.get(
    versionId
      ? `/resources/materials/${id}/versions/${versionId}/download`
      : `/resources/materials/${id}/download`,
    { responseType: "blob" },
  );
export const previewMaterial = (id) =>
  api.get(`/resources/materials/${id}/preview`, { responseType: "blob" });
