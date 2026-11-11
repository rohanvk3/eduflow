import { api } from "../api/client";
export const startAttempt = (id) => api.post(`/exams/${id}/attempts/start`);
export const getAttempt = (id, signal) =>
  api.get(`/exams/attempts/${id}`, { signal });
export const saveAnswers = (id, data) =>
  api.put(`/exams/attempts/${id}/answers`, data);
export const submitAttempt = (id) => api.post(`/exams/attempts/${id}/submit`);
