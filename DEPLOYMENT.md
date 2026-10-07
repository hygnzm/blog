# GitHub Pages 静态发布

当前博客完全从打包后的文章文件读取内容。访客可以阅读、搜索、筛选和翻页，没有登录、投稿、评论或在线编辑入口；不需要 Java、Spring Boot 或数据库。

## 第一次发布

1. 在 GitHub 创建一个公开仓库，例如 `blog`。免费 Pages 的仓库需要公开。
2. 将 `F:\idea\blog` 中的源码上传至仓库的 `main` 分支，务必包含隐藏目录 `.github/workflows/pages.yml`。
3. 建议上传 `frontend/`、`.github/`、`.gitignore` 和说明文档。忽略 `node_modules/`、`dist/`、`.run/` 和 `backend/target/`；旧 `backend/` 不参与静态发布，可以不上传。
4. 打开仓库的 **Settings → Pages → Build and deployment → Source**，选择 **GitHub Actions**。
5. 打开 **Actions → Publish blog to GitHub Pages → Run workflow**，选择 `main` 并运行。之后每次推送到 `main` 会自动更新。
6. 构建和部署成功后，在 Pages 设置页获取公网地址：`https://你的用户名.github.io/blog/`。

工作流会自动安装前端依赖、检查文章和筛选逻辑、构建页面，然后发布 `frontend/dist`。不需要配置后端地址、部署密钥或服务器。Vite 使用相对资源路径，仓库换名和用户首页仓库都能使用；文章详情使用 `#/articles/文章标识`，直接打开和刷新不会因服务器缺少路由而返回 404。

已于 2026-10-07 发布到公网：[博客](https://hygnzm.github.io/blog/) · [仓库](https://github.com/hygnzm/blog) · [成功部署记录](https://github.com/hygnzm/blog/actions/runs/37626383895)。仓库公开，后续向 main 推送前端或工作流更新会自动发布。

## 更新自己的文章

编辑 `frontend/src/content/articles.json`，再提交并推送到 `main`。目前共 17 篇文章，其中 13 篇为内置学习示例，另 4 篇为 BDI、DQN、Transformer 和 RAG 的阅读笔记。

```json
{
  "slug": "my-interview-review",
  "title": "我的面试复盘",
  "category": "career",
  "tags": ["面试", "Java"],
  "publishedAt": "2026-10-07",
  "readingMinutes": 5,
  "featured": false,
  "summary": "列表页显示的简短介绍。",
  "content": "## 准备过程\n\n这里填写 Markdown 正文。"
}
```

`slug` 必须唯一，使用小写英文、数字和连字符。分类为 `backend`（后端学习）、`agent`（Agent 学习）、`papers`（论文学习）、`career`（求职经历）。分类名称和说明在 `frontend/src/content/library.js`，标签和文章数量自动计算。多标签筛选匹配任意一个所选标签；日期包含起止当天。

长文也可以把正文放在 `frontend/src/content/papers/`，元数据使用 `"contentFile": "transformer-attention-notes.md"`，不再填写 `content`。文件名使用小写英文、数字和连字符。构建会检查正文文件是否存在；Markdown 会被打包进网站，不需要额外接口。

论文配图放在 `frontend/public/images/papers/`，正文使用 `./images/papers/文件名.svg`，保留开头的相对路径以适配 GitHub Pages 子目录。文章配图可以放在 `figure.paper-figure` 中，用 `button.diagram-open` 包住图片，并给按钮和图片填写说明；读者点开后可以查看原尺寸。

Transformer 正文中的 `<!-- demo:attention -->`、`<!-- demo:shift -->` 和 `<!-- demo:position -->` 会分别显示注意力计算、目标对齐和位置编码例子。这三种标记适用于本项目的文章组件；导出到其他 Markdown 阅读器时需要另行处理。

访客修改浏览器里的显示不会改变已发布文章。站点内容与源码均按公开内容发布，任何人可以阅读或下载，网站不提供内容保密或禁止复制功能。更新权由你的 GitHub 仓库写入权限控制。

## 本地检查与打包

只需要 Node.js 22 和 npm，无需 JDK：

```powershell
cd F:\idea\blog\frontend
npm.cmd ci
npm.cmd run build
npm.cmd run preview
```

打开 `http://127.0.0.1:4173/` 检查正式构建。`frontend/dist` 就是完整静态发布文件，可以放到任何静态托管服务。直接双击 HTML 文件不等同于 HTTP 托管，请通过本地预览查看。

开发时可在项目根目录双击 `start.cmd`，打开 `http://127.0.0.1:5173/`；双击 `stop.cmd` 停止。

## 常见问题

- **Actions 未自动运行**：确认分支为 `main`，并允许仓库运行 GitHub Actions。
- **部署失败，提示 Pages 未启用**：先在 Settings → Pages 选择 GitHub Actions，再手动重新运行工作流。
- **新增文章构建失败**：在 Actions 的 Build 日志查看错误，检查 JSON 格式、重复 slug、分类和日期。
- **内容还是旧版**：确认最新 Actions 部署成功，再刷新页面。

参考：[GitHub Pages 创建站点](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site)、[Vite 的 GitHub Pages 部署说明](https://vite.dev/guide/static-deploy.html#github-pages)。
