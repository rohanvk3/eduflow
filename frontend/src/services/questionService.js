import { api } from "../api/client";
export const getQuestions = (id, signal) =>
  api.get(`/exams/${id}/questions`, { signal });
export const saveQuestion = (id, qid, data) =>
  qid
    ? api.put(`/exams/${id}/questions/${qid}`, data)
    : api.post(`/exams/${id}/questions`, data);
export const removeQuestion = (id, qid, version) =>
  api.delete(`/exams/${id}/questions/${qid}`, { params: { version } });
export const reorderQuestions = (id, data) =>
  api.put(`/exams/${id}/questions/order`, data);
