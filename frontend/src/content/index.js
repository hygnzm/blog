import { categoryDefinitions } from "./library.js";

export { categoryDefinitions };
const base = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
// The explicit static mode is for the previously published GitHub Pages site.
const staticReader = import.meta.env.MODE === "static"
  ? import("./static.js") : null;
export async function getContent(path, params = {}, signal) {
  if (staticReader) return (await staticReader).getStaticContent(path, params, signal);
  signal?.throwIfAborted();
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (Array.isArray(value)) value.forEach((item) => query.append(key, item));
    else if (value !== "" && value != null) query.set(key, value);
  }
  const response = await fetch(`${base}/api${path}${query.size ? `?${query}` : ""}`, { signal });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `文章服务请求失败（${response.status}）`);
  }
  return response.json();
}
