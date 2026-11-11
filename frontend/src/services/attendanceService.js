import { api } from "../api/client";
export const createSession = (data) => api.post("/attendance/sessions", data);
export const getSessions = (params, signal) =>
  api.get("/attendance/sessions", { params, signal });
export const getSessionById = (id, signal) =>
  api.get(`/attendance/sessions/${id}`, { signal });
export const saveAttendanceRecords = (id, data) =>
  api.put(`/attendance/sessions/${id}/records`, data);
export const finalizeSession = (id, version) =>
  api.post(`/attendance/sessions/${id}/finalize`, { version });
export const getBatchRegister = (id, params, signal) =>
  api.get(`/attendance/batches/${id}/register`, { params, signal });
export const getBatchSummary = (id, params, signal) =>
  api.get(`/attendance/batches/${id}/summary`, { params, signal });
export const getStudentSummary = (id, params, signal) =>
  api.get(`/attendance/students/${id}/summary`, { params, signal });
export const getMyAttendance = (params, signal) =>
  api.get("/attendance/me", { params, signal });
export const getAttendanceReports = (params, signal) =>
  api.get("/attendance/reports", { params, signal });
export const getAttendanceOverview = (params, signal) =>
  api.get("/attendance/overview", { params, signal });
export const exportAttendanceReport = (params) =>
  api.get("/attendance/reports/export", { params, responseType: "blob" });
