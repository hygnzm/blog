import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createLibrary } from "../src/content/library.js";

const articles = JSON.parse(readFileSync(new URL("../src/content/articles.json", import.meta.url), "utf8"));
const library = createLibrary(articles);

test("all published articles have valid content and unique routes", () => {
  assert.ok(articles.length > 0);
  for (const article of articles) assert.equal(library.detail(article.slug).content, article.content);
  assert.throws(() => createLibrary([...articles, articles[0]]), /重复/);
});

test("category and tag metadata reflect published content, including empty career category", () => {
  const metadata = library.metadata();
  assert.equal(metadata.total, articles.length);
  assert.equal(metadata.categories.reduce((sum, category) => sum + category.count, 0), articles.length);
  assert.ok(metadata.categories.some((category) => category.id === "career"));
  const backend = articles.filter((article) => article.category === "backend");
  const javaCount = backend.filter((article) => article.tags.includes("Java")).length;
  assert.equal(library.metadata({ category: "backend" }).tags.find((tag) => tag.name === "Java").count, javaCount);
});

test("combined filters preserve case-insensitive search, inclusive dates and any selected tag", () => {
  const expected = articles.filter((article) => article.category === "backend" &&
    article.tags.includes("Java") && article.publishedAt >= "2026-10-01" && article.publishedAt <= "2026-10-07");
  const result = library.list({ category: "backend", tags: ["Java", "不存在"], q: "jAvA", from: "2026-10-01", to: "2026-10-07" });
  assert.deepEqual(result.items.map((item) => item.slug).sort(), expected.map((item) => item.slug).sort());
  assert.equal(library.list({ q: "不存在的关键词" }).total, 0);
});

test("pagination returns every article once and newest/oldest reverse order", () => {
  const pages = library.list().totalPages;
  const slugs = Array.from({ length: pages }, (_, index) => library.list({ page: index + 1 }).items).flat().map((item) => item.slug);
  assert.equal(new Set(slugs).size, articles.length);
  assert.equal(library.list({ page: pages + 1 }).items.length, 0);
  assert.deepEqual(library.list({ size: 50, sort: "oldest" }).items.map((item) => item.slug), [...slugs].reverse());
  assert.ok(library.list().items.every((item) => !("content" in item)));
});

test("invalid date ranges and missing articles show understandable errors", () => {
  assert.throws(() => library.list({ from: "2026-10-07", to: "2026-10-01" }), /开始日期/);
  assert.throws(() => library.detail("missing-article"), /不存在/);
});
