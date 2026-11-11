import { beforeEach, afterEach, expect, it, vi } from "vitest";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ThemeToggle, { THEME_KEY } from "../components/ThemeToggle";
import initialization from "../../public/theme-init.js?raw";

let media;
beforeEach(() => {
  localStorage.clear();
  document.documentElement.removeAttribute("data-theme");
  document.documentElement.style.colorScheme = "";
  media = {
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  };
  vi.stubGlobal(
    "matchMedia",
    vi.fn(() => media),
  );
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  localStorage.clear();
  document.documentElement.removeAttribute("data-theme");
  document.documentElement.style.colorScheme = "";
});
it("toggles dark/light, persists preference and synchronizes multiple controls", async () => {
  render(
    <>
      <ThemeToggle />
      <ThemeToggle />
    </>,
  );
  await userEvent.click(
    screen.getAllByRole("button", { name: "Switch to dark mode" })[0],
  );
  expect(document.documentElement).toHaveAttribute("data-theme", "dark");
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(localStorage.getItem(THEME_KEY)).toBe("dark");
  expect(
    screen.getAllByRole("button", { name: "Switch to light mode" }),
  ).toHaveLength(2);
  await userEvent.click(
    screen.getAllByRole("button", { name: "Switch to light mode" })[1],
  );
  expect(localStorage.getItem(THEME_KEY)).toBe("light");
});
it("applies saved dark mode before React renders", () => {
  localStorage.setItem(THEME_KEY, "dark");
  new Function(initialization)();
  expect(document.documentElement.dataset.theme).toBe("dark");
  render(<ThemeToggle />);
  expect(
    screen.getByRole("button", { name: "Switch to light mode" }),
  ).toHaveAttribute("aria-pressed", "true");
});
it("uses system preference when no explicit theme exists", () => {
  media.matches = true;
  new Function(initialization)();
  expect(document.documentElement.dataset.theme).toBe("dark");
});
it("explicit light selection overrides a dark system preference", () => {
  media.matches = true;
  localStorage.setItem(THEME_KEY, "light");
  new Function(initialization)();
  expect(document.documentElement.dataset.theme).toBe("light");
});
it("synchronizes changes made in another browser tab", () => {
  render(<ThemeToggle />);
  localStorage.setItem(THEME_KEY, "dark");
  act(() =>
    window.dispatchEvent(
      new StorageEvent("storage", { key: THEME_KEY, newValue: "dark" }),
    ),
  );
  expect(
    screen.getByRole("button", { name: "Switch to light mode" }),
  ).toBeInTheDocument();
});
it("continues working if localStorage is unavailable", async () => {
  vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
    throw new Error("unavailable");
  });
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
    throw new Error("unavailable");
  });
  new Function(initialization)();
  render(<ThemeToggle />);
  await userEvent.click(
    screen.getByRole("button", { name: "Switch to dark mode" }),
  );
  expect(document.documentElement.dataset.theme).toBe("dark");
});
