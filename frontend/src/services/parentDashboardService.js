import { api } from "../api/client";
export const getParentDashboard = (params, signal) =>
  api.get("/parents/dashboard", { params, signal });
export const getChildDashboard = (id, params, signal) =>
  api.get(`/parents/children/${id}/dashboard`, { params, signal });
export const getChild = (id, signal) =>
  api.get(`/parents/children/${id}`, { signal });
export const getChildAttendance = (id, params, signal) =>
  api.get(`/parents/children/${id}/attendance`, { params, signal });
export const getChildFees = (id, params, signal) =>
  api.get(`/parents/children/${id}/fees`, { params, signal });
export const getChildResults = (id, params, signal) =>
  api.get(`/parents/children/${id}/results`, { params, signal });
export const getChildAssignments = (id, params, signal) =>
  api.get(`/parents/children/${id}/assignments`, { params, signal });
export const getChildResources = (id, params, signal) =>
  api.get(`/parents/children/${id}/resources`, { params, signal });
export const getChildProgress = (id, params, signal) =>
  api.get(`/parents/children/${id}/progress`, { params, signal });
export const downloadChildReceipt = (id, paymentId) =>
  api.get(`/parents/children/${id}/payments/${paymentId}/receipt`, {
    responseType: "blob",
  });
