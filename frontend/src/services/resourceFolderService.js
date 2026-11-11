import { api } from "../api/client";
import { changed } from "./resourceService";
export const getResourceFolders = (params, signal) =>
  api.get("/resources/folders", { params, signal });
export const getResourceFolder = (id, signal) =>
  api.get(`/resources/folders/${id}`, { signal });
export const saveResourceFolder = (id, data) =>
  changed(
    id
      ? api.put(`/resources/folders/${id}`, data)
      : api.post("/resources/folders", data),
  );
export const deleteResourceFolder = (id) =>
  changed(api.delete(`/resources/folders/${id}`));
