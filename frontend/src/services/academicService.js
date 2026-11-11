import { api } from "../api/client";
export const getCourses = (params, signal) =>
  api.get("/courses", { params, signal });
export const getCourse = (id, signal) => api.get(`/courses/${id}`, { signal });
export const saveCourse = (id, data) =>
  id ? api.put(`/courses/${id}`, data) : api.post("/courses", data);
export const setCourseStatus = (id, active) =>
  api.patch(`/courses/${id}/${active ? "activate" : "deactivate"}`);
export const getBatches = (params, signal) =>
  api.get("/batches", { params, signal });
export const getBatch = (id, signal) => api.get(`/batches/${id}`, { signal });
export const saveBatch = (id, data) =>
  id ? api.put(`/batches/${id}`, data) : api.post("/batches", data);
export const getBatchStats = (signal) => api.get("/batches/stats", { signal });
export const getAssignments = (id, signal) =>
  api.get(`/batches/${id}/teachers`, { signal });
export const assignTeacher = (id, teacherId) =>
  api.post(`/batches/${id}/teachers`, { teacherId });
export const unassignTeacher = (id, teacherId) =>
  api.delete(`/batches/${id}/teachers/${teacherId}`);
export const getEnrollments = (id, params, signal) =>
  api.get(`/batches/${id}/enrollments`, { params, signal });
export const enrollStudent = (id, data) =>
  api.post(`/batches/${id}/enrollments`, data);
export const withdrawStudent = (id, enrollmentId, withdrawalDate) =>
  api.patch(`/batches/${id}/enrollments/${enrollmentId}/withdraw`, {
    withdrawalDate,
  });
export const getMyEnrollments = (params, signal) =>
  api.get("/batches/my-enrollments", { params, signal });
export const getSchedules = (id, signal) =>
  api.get(`/batches/${id}/schedules`, { signal });
export const saveSchedule = (batchId, id, data) =>
  id
    ? api.put(`/batches/${batchId}/schedules/${id}`, data)
    : api.post(`/batches/${batchId}/schedules`, data);
