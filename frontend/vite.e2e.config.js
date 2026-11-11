import { mergeConfig } from "vite";
import base from "./vite.config.js";
export default mergeConfig(base, {
  server: { host: "127.0.0.1", port: 15173, strictPort: true, proxy: { "/api": "http://127.0.0.1:18081" } },
  preview: { host: "127.0.0.1", port: 15173, strictPort: true, proxy: { "/api": "http://127.0.0.1:18081" } },
});
