export const categoryDefinitions = [
  { id: "backend", name: "后端学习", description: "从基础到架构，构建可靠的服务" },
  { id: "agent", name: "Agent 学习", description: "连接模型与工具，让想法付诸行动" },
  { id: "papers", name: "论文学习", description: "拆解核心思路，读懂研究的下一步" },
  { id: "career", name: "求职经历", description: "记录投递、面试与复盘的每一步" },
];

export function createLibrary(articles) {
  const slugs = new Set();
  for (const article of articles) {
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(article.slug) || slugs.has(article.slug))
      throw new Error(`文章 slug 无效或重复：${article.slug}`);
    slugs.add(article.slug);
    if (!categoryDefinitions.some((category) => category.id === article.category))
      throw new Error(`未知分类：${article.category}`);
    for (const key of ["title", "summary", "content", "publishedAt"]) {
      if (typeof article[key] !== "string" || !article[key].trim())
        throw new Error(`文章 ${article.slug} 缺少 ${key}`);
    }
    const parsedDate = new Date(`${article.publishedAt}T00:00:00Z`);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(article.publishedAt) ||
        Number.isNaN(parsedDate.getTime()) || parsedDate.toISOString().slice(0, 10) !== article.publishedAt)
      throw new Error(`文章 ${article.slug} 日期无效`);
    if (!Array.isArray(article.tags) || article.tags.some((tag) => typeof tag !== "string" || !tag.trim()))
      throw new Error(`文章 ${article.slug} 标签无效`);
    if (!Number.isInteger(article.readingMinutes) || article.readingMinutes < 1)
      throw new Error(`文章 ${article.slug} 阅读时长无效`);
  }

  function validateCategory(category) {
    if (category && !categoryDefinitions.some((item) => item.id === category))
      throw new Error("未知的文章分类");
  }

  return {
    metadata({ category = "" } = {}) {
      validateCategory(category);
      const counts = new Map();
      for (const article of articles.filter((item) => !category || item.category === category)) {
        for (const tag of new Set(article.tags)) counts.set(tag, (counts.get(tag) || 0) + 1);
      }
      return {
        categories: categoryDefinitions.map((item) => ({
          ...item, count: articles.filter((article) => article.category === item.id).length,
        })),
        tags: [...counts].map(([name, count]) => ({ name, count })),
        total: articles.length,
      };
    },
    list({ q = "", category = "", tags = [], from = "", to = "", sort = "newest", page = 1, size = 6 } = {}) {
      validateCategory(category);
      if (!Number.isInteger(page) || page < 1 || !Number.isInteger(size) || size < 1 || size > 50)
        throw new Error("页码或每页数量无效");
      if (from && to && from > to) throw new Error("开始日期不能晚于结束日期");
      if (!["newest", "oldest"].includes(sort)) throw new Error("文章排序无效");
      const keyword = q.trim().toLowerCase();
      const filtered = articles.filter((article) =>
        (!category || article.category === category) &&
        (!tags.length || tags.some((tag) => article.tags.includes(tag))) &&
        (!from || article.publishedAt >= from) && (!to || article.publishedAt <= to) &&
        (!keyword || `${article.title} ${article.summary} ${article.tags.join(" ")}`.toLowerCase().includes(keyword))
      ).sort((a, b) => {
        const comparison = a.publishedAt.localeCompare(b.publishedAt) || a.slug.localeCompare(b.slug);
        return sort === "oldest" ? comparison : -comparison;
      });
      return {
        items: filtered.slice((page - 1) * size, page * size).map(({ content, ...summary }) => summary),
        total: filtered.length, page, size, totalPages: Math.ceil(filtered.length / size),
      };
    },
    detail(slug) {
      const article = articles.find((item) => item.slug === slug);
      if (!article) throw new Error("这篇文章不存在");
      return { ...article, tags: [...article.tags] };
    },
  };
}
