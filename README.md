# 码间 · 技术博客（Vue 3 + Spring Boot）

当前默认版本采用前后端分离：Vue 展示文章及论文交互模块，Spring Boot 提供文章列表、筛选、分类统计和详情 API。访客只能浏览，没有登录、投稿或在线编辑功能。

## 本地开发

需要 Node.js 22 和 JDK 21。在项目根目录双击 start.cmd，或运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

页面：http://127.0.0.1:5173/ 。API：http://127.0.0.1:8080/api/health 。stop.cmd 停止本项目进程。前端通过 Vite 代理访问 API；start.ps1 导出文章，在需要时构建 JAR，并启动前后端。

## 构建与阿里云部署

```powershell
npm.cmd --prefix frontend run export:backend
npm.cmd --prefix frontend run build
cd backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -B package
```

前端输出 frontend/dist，后端输出 backend/target/blog-backend-1.0.0.jar。阿里云服务器用 Nginx 提供前端页面，将 /api 转发至本机 Spring Boot。服务端只需要 Nginx 与 Java 21。

部署配置与逐步操作见 [deploy/aliyun/README.md](deploy/aliyun/README.md)。已于 2026-10-12 部署至阿里云：[打开前后端分离版博客](http://8.155.134.37/)。Nginx 与 Spring Boot 已设置开机启动。

## 内容与接口

- frontend/src/content/articles.json 和 papers/*.md：文章编辑源，包含 17 篇笔记。
- frontend/scripts/export-backend.mjs：校验并将完整正文导出至 backend/src/main/resources/articles.json，供 Spring Boot 打包读取。
- backend/src/main/java/dev/notebook/blog/：只读 REST API，暂不需要数据库。
- frontend/src/content/index.js：默认通过 HTTP 请求后端数据，生产前端不嵌入文章正文。
- frontend/src/components/：Markdown 展示、图片大图与论文交互例子。
- frontend/public/images/papers/：教学配图，随前端部署。
- deploy/aliyun/：Nginx 配置、systemd 服务和部署说明。

接口为 GET /api/health、GET /api/metadata、GET /api/articles、GET /api/articles/{slug}。搜索、分类、标签、日期、分页与排序由后端执行。

更新文章后重新导出并构建后端，替换 JAR，再重启服务。更新前端页面则重新构建前端并上传 dist。

## 已有 GitHub Pages

此前公开站点 https://hygnzm.github.io/blog/ 是独立静态浏览版本。保留明确的 npm run build:static 模式与 Pages 工作流以便继续维护该站点；阿里云使用默认 npm run build 的 API 版本。GitHub Pages 本身不能运行 Spring Boot。此前静态发布方法保留在 DEPLOYMENT.md。
