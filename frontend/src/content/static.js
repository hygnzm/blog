import articles from "./articles.json";
import { createLibrary } from "./library.js";
import { resolveArticleSources } from "./sources.js";
const markdownSources = import.meta.glob(["./papers/*.md", "./java/*.md"], { query: "?raw", import: "default", eager: true });
const library = createLibrary(resolveArticleSources(articles, markdownSources));
export async function getStaticContent(path, params = {}, signal) {
  signal?.throwIfAborted();
  if (path === "/metadata") return library.metadata(params);
  if (path === "/articles") return library.list(params);
  if (path.startsWith("/articles/")) return library.detail(path.slice("/articles/".length));
  throw new Error("未知的内容路径");
}
