// Apply the saved/system theme before React and styles load, avoiding a light flash.
(() => {
  let saved;
  try {
    saved = localStorage.getItem("eduflow.theme");
  } catch {
    /* Storage may be unavailable. */
  }
  const theme =
    saved === "dark" || saved === "light"
      ? saved
      : window.matchMedia?.("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
})();
