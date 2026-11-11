import { api } from "../api/client";
import { changed } from "./resourceService";
export const getResourceCategories = (params, signal) =>
  api.get("/resources/categories", { params, signal });
export const getResourceCategory = (id, signal) =>
  api.get(`/resources/categories/${id}`, { signal });
export const saveResourceCategory = (id, data) =>
  changed(
    id
      ? api.put(`/resources/categories/${id}`, data)
      : api.post("/resources/categories", data),
  );
export const deactivateResourceCategory = (id) =>
  changed(api.patch(`/resources/categories/${id}/deactivate`));
