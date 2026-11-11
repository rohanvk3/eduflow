import { api } from "../api/client";
export const getGradebook = (id, signal) =>
  api.get(`/exams/${id}/offline-results`, { signal });
export const saveGradebook = (id, data) =>
  api.put(`/exams/${id}/offline-results`, data);
export const publishResults = (id, version) =>
  api.post(`/exams/${id}/results/publish`, { version });
export const getExamResults = (id, params, signal) =>
  api.get(`/exams/${id}/results`, { params, signal });
export const getLeaderboard = (id, params, signal) =>
  api.get(`/exams/${id}/leaderboard`, { params, signal });
export const getMyResults = (params, signal) =>
  api.get("/exams/me/results", { params, signal });
export const getOwnResult = (id, signal) =>
  api.get(`/exams/${id}/result`, { signal });
export const correctResult = (id, rid, data) =>
  api.post(`/exams/${id}/results/${rid}/correct`, data);
export const getCorrections = (id, rid, signal) =>
  api.get(`/exams/${id}/results/${rid}/corrections`, { signal });
