import axios from "axios";

export const TOKEN_KEY = "eduflow.token";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY);
  const publicAuth = [
    "/auth/login",
    "/auth/register",
    "/auth/password-reset/request",
    "/auth/password-reset/confirm",
  ].includes(config.url);
  if (token && !publicAuth) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      !error.config?.url?.startsWith("/auth/login")
    ) {
      sessionStorage.removeItem(TOKEN_KEY);
      window.dispatchEvent(new Event("eduflow:unauthorized"));
    }
    return Promise.reject(error);
  },
);

export function errorMessage(error) {
  if (error.response?.status === 403)
    return "You do not have permission to perform this action.";
  if(error.response?.status>=500&&error.response?.data?.requestId)
    return `${error.response.data.message || 'Request failed'} (Request: ${error.response.data.requestId})`;
  return (
    error.response?.data?.message ||
    "Could not connect to EduFlow. Please try again."
  );
}
