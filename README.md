# 码间 · 技术博客（静态浏览版）

Vue 3 技术博客，按后端学习、Agent 学习、论文学习、求职经历展示文章。保留搜索、标签与日期筛选、排序、分页、Markdown 详情以及手机布局。所有文章随网站打包，访客只能浏览，没有登录、投稿或在线修改功能。

当前版本只需要 Node.js 22，开发和部署均无需 Spring Boot、数据库或 JDK。旧 backend 目录作为先前版本源码保留，不参与当前启动、构建和发布。

## 本地启动

双击 start.cmd，或在项目根目录运行：

~~~powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
~~~

打开 http://127.0.0.1:5173/ 。双击 stop.cmd 停止本项目记录的进程。日志位于 .run/。启动脚本不会启动后端，也不会终止其他项目进程。

也可以直接启动前端：

~~~powershell
cd F:\idea\blog\frontend
npm.cmd ci
npm.cmd run dev
~~~

## 构建与发布

~~~powershell
cd F:\idea\blog\frontend
npm.cmd run build
npm.cmd run preview
~~~

构建先验证文章与筛选逻辑，再生成 frontend/dist。正式构建预览地址为 http://127.0.0.1:4173/ 。

GitHub Pages 的准备配置已放在 .github/workflows/pages.yml。上传源码至公开仓库的 main 分支，在 Settings → Pages 选择 GitHub Actions，运行工作流即可发布。详细步骤与文章更新方法见 [DEPLOYMENT.md](DEPLOYMENT.md)。尚未实际发布到公网。

## 内容与结构

- frontend/src/content/articles.json：15 篇演示学习笔记，正式发布前可替换为自己的文章。
- frontend/src/content/library.js：分类说明、文章校验、搜索、筛选、排序和分页。
- frontend/src/content/index.js：读取打包内容，不请求远程 API。
- frontend/src/App.vue：列表与详情界面。
- frontend/src/style.css：字号、样式与响应式布局。
- frontend/tests/content.test.js：内容与筛选行为检查，构建时自动运行。
- .github/workflows/pages.yml：GitHub Pages 自动发布。
- backend/：先前 Spring Boot 版本的保留源码，当前不用。

新增文章编辑 frontend/src/content/articles.json；分类使用 backend、agent、papers、career。标签和文章数量自动统计。品牌在 App.vue 中修改。

部署只上传构建结果，网站没有写入接口。访客仍可读取和下载公开内容；源码是否可写取决于你的 GitHub 仓库权限。

布局参考：[美团校园招聘](https://career.meituan.com/web/campus)。部署参考：[Vite 官方文档](https://vite.dev/guide/static-deploy.html#github-pages)。
