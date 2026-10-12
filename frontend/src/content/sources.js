export function resolveArticleSources(articles, markdownSources) {
  return articles.map(({ contentFile, ...article }) => {
    if (!contentFile) return article;
    if (!/^(?:(?:papers|java)\/)?[a-z0-9]+(?:-[a-z0-9]+)*\.md$/.test(contentFile)) {
      throw new Error(`文章 ${article.slug} 的正文文件名无效`);
    }
    const sourcePath = contentFile.includes("/") ? `./${contentFile}` : `./papers/${contentFile}`;
    const content = markdownSources[sourcePath];
    if (typeof content !== "string" || !content.trim()) {
      throw new Error(`文章 ${article.slug} 的正文文件不存在或为空：${contentFile}`);
    }
    return { ...article, content };
  });
}
