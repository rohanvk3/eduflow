import { useEffect } from "react";

/** BrowserRouter-compatible guard for links, browser history and refresh/close. */
export default function useUnsavedAttendance(dirty) {
  useEffect(() => {
    if (!dirty) return;
    let index = window.history.state?.idx;
    let restoring = false;
    const ask = () =>
      window.confirm(
        "You have unsaved attendance changes. Leave without saving?",
      );
    const navigation = (event) => {
      if (!ask()) event.preventDefault();
    };
    const unload = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    const links = (e) => {
      const a = e.target.closest?.("a[href]");
      if (
        !a ||
        a.target === "_blank" ||
        a.hasAttribute("download") ||
        e.ctrlKey ||
        e.metaKey ||
        e.shiftKey ||
        e.altKey
      )
        return;
      const url = new URL(a.href, location.href);
      if (
        url.href === location.href ||
        (url.hash && url.pathname === location.pathname)
      )
        return;
      if (!ask()) {
        e.preventDefault();
        e.stopPropagation();
      }
    };
    const history = (e) => {
      if (restoring) {
        restoring = false;
        return;
      }
      const next = e.state?.idx;
      if (!ask() && typeof index === "number" && typeof next === "number") {
        e.stopImmediatePropagation();
        restoring = true;
        window.history.go(index - next);
      } else index = next;
    };
    window.addEventListener("beforeunload", unload);
    window.addEventListener("eduflow:before-navigation", navigation);
    document.addEventListener("click", links, true);
    window.addEventListener("popstate", history, true);
    return () => {
      window.removeEventListener("beforeunload", unload);
      window.removeEventListener("eduflow:before-navigation", navigation);
      document.removeEventListener("click", links, true);
      window.removeEventListener("popstate", history, true);
    };
  }, [dirty]);
}
