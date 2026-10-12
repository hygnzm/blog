import { readFile, readdir, writeFile } from "node:fs/promises";
import { resolveArticleSources } from "../src/content/sources.js";
import { createLibrary } from "../src/content/library.js";
const contentRoot = new URL("../src/content/", import.meta.url);
const articles = JSON.parse(await readFile(new URL("articles.json", contentRoot), "utf8"));
const sources = {};
for (const file of await readdir(new URL("papers/", contentRoot))) {
  if (file.endsWith(".md")) sources[`./papers/${file}`] = await readFile(new URL(`papers/${file}`, contentRoot), "utf8");
}
const resolved = resolveArticleSources(articles, sources);
createLibrary(resolved);
const target = new URL("../../backend/src/main/resources/articles.json", import.meta.url);
const output = JSON.stringify(resolved, null, 2) + "\n";
if (await readFile(target, "utf8").catch(() => "") !== output) await writeFile(target, output);
console.log(`Exported ${resolved.length} articles with full Markdown content for Spring Boot.`);
