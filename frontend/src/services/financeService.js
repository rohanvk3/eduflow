import { api } from "../api/client";
export const getFeeStructures = (params, signal) =>
  api.get("/fees/structures", { params, signal });
export const getFeeStructure = (id, signal) =>
  api.get(`/fees/structures/${id}`, { signal });
export const saveFeeStructure = (id, data) =>
  id
    ? api.put(`/fees/structures/${id}`, data)
    : api.post("/fees/structures", data);
export const deactivateFeeStructure = (id) =>
  api.patch(`/fees/structures/${id}/deactivate`);
export const getFeeAccounts = (params, signal) =>
  api.get("/fees/accounts", { params, signal });
export const getFeeAccount = (id, signal) =>
  api.get(`/fees/accounts/${id}`, { signal });
export const createFeeAccount = (data) => api.post("/fees/accounts", data);
export const getMyFees = (params, signal) =>
  api.get("/fees/me", { params, signal });
export const createInstallments = (id, data) =>
  api.post(`/fees/accounts/${id}/installments`, data);
export const updateInstallment = (id, data) =>
  api.put(`/fees/installments/${id}`, data);
export const recordPayment = (data) => api.post("/fees/payments", data);
export const getPayments = (params, signal) =>
  api.get("/fees/payments", { params, signal });
export const getPayment = (id, signal) =>
  api.get(`/fees/payments/${id}`, { signal });
export const voidPayment = (id, reason) =>
  api.post(`/fees/payments/${id}/void`, { reason });
export const downloadReceipt = (id) =>
  api.get(`/fees/payments/${id}/receipt`, { responseType: "blob" });
export const getFinanceDashboard = (params, signal) =>
  api.get("/fees/dashboard", { params, signal });
export const getFinanceReport = (params, signal) =>
  api.get("/fees/reports", { params, signal });
export const exportFinanceReport = (params) =>
  api.get("/fees/reports/export", { params, responseType: "blob" });

export function saveDownload(blob, name) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export async function financeError(error) {
  if (error.response?.data instanceof Blob) {
    try {
      const data = JSON.parse(await error.response.data.text());
      return data.message || "Download failed";
    } catch {
      /* use the shared fallback */
    }
  }
  const { errorMessage } = await import("../api/client");
  const fields =
    error.response?.data?.fieldErrors || error.response?.data?.errors;
  return fields && Object.keys(fields).length
    ? Object.entries(fields)
        .map(([key, value]) => `${key}: ${value}`)
        .join(". ")
    : errorMessage(error);
}
