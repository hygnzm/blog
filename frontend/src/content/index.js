import articles from "./articles.json";
import { categoryDefinitions, createLibrary } from "./library.js";

export { categoryDefinitions };
const library = createLibrary(articles);

// Read from bundled content. Browsing never makes an API request or writes data.
export async function getContent(path, params = {}, signal) {
  signal?.throwIfAborted();
  if (path === "/metadata") return library.metadata(params);
  if (path === "/articles") return library.list(params);
  if (path.startsWith("/articles/")) return library.detail(path.slice("/articles/".length));
  throw new Error("未知的内容路径");
}
