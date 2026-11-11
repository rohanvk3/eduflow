import { beforeEach, expect, it, vi } from "vitest";
import { api, TOKEN_KEY } from "../api/client";

beforeEach(() => sessionStorage.clear());

it("sends the existing access token in the Authorization header", async () => {
  sessionStorage.setItem(TOKEN_KEY, "access-token");
  const adapter = vi.fn(async (config) => ({
    data: {},
    status: 200,
    headers: {},
    config,
  }));
  await api.get("/students", { adapter });
  expect(adapter.mock.calls[0][0].headers.Authorization).toBe(
    "Bearer access-token",
  );
});

it("clears an expired token and notifies the authentication provider", async () => {
  sessionStorage.setItem(TOKEN_KEY, "expired");
  const listener = vi.fn();
  window.addEventListener("eduflow:unauthorized", listener);
  const adapter = async (config) => {
    throw { response: { status: 401 }, config };
  };
  await expect(api.get("/students", { adapter })).rejects.toBeDefined();
  expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  expect(listener).toHaveBeenCalledOnce();
  window.removeEventListener("eduflow:unauthorized", listener);
});
