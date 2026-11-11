import { api } from "../api/client";
export const getTeachers = (params, signal) =>
  api.get("/teachers", { params, signal });
export const getTeacherById = (id, signal) =>
  api.get(`/teachers/${id}`, { signal });
export const createTeacher = (data) => api.post("/teachers", data);
export const updateTeacher = (id, data) => api.put(`/teachers/${id}`, data);
export const deactivateTeacher = (id) =>
  api.patch(`/teachers/${id}/deactivate`);
export const activateTeacher = (id) => api.patch(`/teachers/${id}/activate`);
export const getMyTeacherProfile = (signal) =>
  api.get("/teachers/me", { signal });
export const getTeacherStats = (signal) =>
  api.get("/teachers/stats", { signal });
