import { api } from "../api/client";
export const getReport = (params, signal) =>
  api.get("/reports", { params, signal });
export async function downloadReport(format, params) {
  const { data } = await api.get("/reports/export/" + format, {
    params,
    responseType: "blob",
  });
  const url = URL.createObjectURL(data);
  const a = document.createElement("a");
  a.href = url;
  a.download = "eduflow-report." + format;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
