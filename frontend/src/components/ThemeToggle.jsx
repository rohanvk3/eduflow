import { useSyncExternalStore } from "react";
import { Moon, Sun } from "lucide-react";
import { Button } from "./UI";

export const THEME_KEY = "eduflow.theme";
const systemTheme = () =>
  window.matchMedia?.("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
function savedTheme() {
  try {
    return localStorage.getItem(THEME_KEY);
  } catch {
    return null;
  }
}
function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute("content", theme === "dark" ? "#0b1220" : "#f8fafc");
}
function subscribe(notify) {
  const media = window.matchMedia?.("(prefers-color-scheme: dark)");
  const changed = () => notify();
  const synchronize = () => {
    const saved = savedTheme();
    applyTheme(saved === "light" || saved === "dark" ? saved : systemTheme());
    notify();
  };
  const storage = (event) => {
    if (event.key === THEME_KEY || event.key === null) synchronize();
  };
  const systemChanged = () => {
    if (!["light", "dark"].includes(savedTheme())) synchronize();
  };
  window.addEventListener("eduflow:theme", changed);
  window.addEventListener("storage", storage);
  media?.addEventListener("change", systemChanged);
  return () => {
    window.removeEventListener("eduflow:theme", changed);
    window.removeEventListener("storage", storage);
    media?.removeEventListener("change", systemChanged);
  };
}
export default function ThemeToggle() {
  const theme = useSyncExternalStore(
    subscribe,
    () => document.documentElement.dataset.theme || "light",
    () => "light",
  );
  const dark = theme === "dark";
  return (
    <Button
      variant="icon"
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      title={dark ? "Switch to light mode" : "Switch to dark mode"}
      aria-pressed={dark}
      onClick={() => {
        const next = dark ? "light" : "dark";
        try {
          localStorage.setItem(THEME_KEY, next);
        } catch {
          /* Keep the session usable without storage. */
        }
        applyTheme(next);
        window.dispatchEvent(new Event("eduflow:theme"));
      }}
    >
      {dark ? <Sun size={19} /> : <Moon size={19} />}
    </Button>
  );
}
