import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e", workers: 1, fullyParallel: false, timeout: 90000,
  expect: { timeout: 15000 },
  reporter: [["list"], ["json", { outputFile: "test-results/results.json" }]],
  use: { baseURL: "http://127.0.0.1:15173", browserName: "chromium", actionTimeout: 15000, screenshot: "only-on-failure", trace: "retain-on-failure" },
  webServer: { command: "npx vite preview --config vite.e2e.config.js", url: "http://127.0.0.1:15173", reuseExistingServer: false },
});
