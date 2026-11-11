import { api } from "../api/client";
export const getExams = (params, signal) =>
  api.get("/exams", { params, signal });
export const getExam = (id, signal) => api.get(`/exams/${id}`, { signal });
export const saveExam = (id, data) =>
  id ? api.put(`/exams/${id}`, data) : api.post("/exams", data);
export const publishExam = (id, version) =>
  api.post(`/exams/${id}/publish`, { version });
export const cancelExam = (id, data) => api.post(`/exams/${id}/cancel`, data);
export const getExamAnalytics = (params, signal) =>
  api.get("/exams/analytics", { params, signal });
