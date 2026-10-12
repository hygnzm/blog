# 阿里云前后端分离部署

目标：Alibaba Cloud Linux 3，公网 IP 8.155.134.37。

已于 2026-10-12 在实例 `i-f8z42mfqmawjwweho6om`（cn-heyuan）完成部署：http://8.155.134.37/ 。使用 Nginx 1.24、独立 Temurin Java 21 运行环境与 Spring Boot；17 篇文章、搜索、正文刷新和配图大图已通过公网浏览器验证。

浏览器 → Nginx（80，前端文件）→ /api 转发至 Spring Boot（127.0.0.1:8080）。前后端独立构建和运行，无需数据库；文章暂存于后端 JAR 内的只读 JSON。网站保持只有浏览功能。

## 先确认环境

在 Workbench 执行，发送输出后再决定安装哪些软件：

```bash
whoami
java -version
nginx -v
sudo ss -lntp
sudo dnf list --available 'java-21-openjdk*' nginx
```

需要 Java 21 与 Nginx。软件源安装方式如下（Java 21 包需先确认存在）：

```bash
sudo dnf install -y java-21-openjdk-headless nginx unzip
java -version
```

确认 Java 显示 21；已有多个 Java 时通过 `sudo alternatives --config java` 选择 21。若没有 Java 21 软件包，使用阿里云的 OpenJDK 扩展安装 21，或按官方文档安装 JDK 21，不要运行需要 Java 21 的 JAR 之前猜测系统默认版本。

本次该服务器的软件源未提供 Java 21，实际采用经过官方 SHA-256 校验的 Eclipse Temurin 21 JRE，独立安装至 `/opt/code-notes/runtime`。服务配置已固定使用 `/opt/code-notes/runtime/bin/java`，不依赖全局 `java` 命令。部署包 `runtime/java21-linux.tar.gz` 内提供了同一运行环境，首次安装可解压至该目录：

```bash
sudo mkdir -p /opt/code-notes/runtime
sudo tar -xzf ~/blog-release/runtime/java21-linux.tar.gz -C /opt/code-notes/runtime --strip-components=1
sudo chmod 0755 /opt/code-notes
/opt/code-notes/runtime/bin/java -version
```

## 本地构建

Windows 项目根目录运行：

```powershell
npm.cmd --prefix frontend run export:backend
npm.cmd --prefix frontend run build
cd backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -B package
```

服务器只需正式文件，不需要安装 Node.js 或 Maven。部署包中包含：`frontend/`（dist 内容）、`backend/blog-backend-1.0.0.jar`、`config/blog.conf`、`config/code-notes.service`。

## 上传和安装

通过 Workbench 文件上传或你自己的 SSH 登录方式，将部署包上传到服务器用户目录，例如 `~/blog-aliyun.zip`。以下步骤用于首次部署；已有站点占用 IP 或端口时先检查现有配置，已有同名文件请先备份。

```bash
mkdir -p ~/blog-release
unzip ~/blog-aliyun.zip -d ~/blog-release
sudo mkdir -p /opt/code-notes/frontend /opt/code-notes/backend
sudo chmod 0755 /opt/code-notes
id codeblog || sudo useradd --system --home-dir /opt/code-notes --shell /sbin/nologin codeblog
sudo cp -a ~/blog-release/frontend/. /opt/code-notes/frontend/
sudo cp ~/blog-release/backend/blog-backend-1.0.0.jar /opt/code-notes/backend/
sudo chmod -R a+rX /opt/code-notes/frontend
sudo chown -R codeblog:codeblog /opt/code-notes/backend
sudo cp ~/blog-release/config/code-notes.service /etc/systemd/system/code-notes.service
sudo cp ~/blog-release/config/blog.conf /etc/nginx/conf.d/code-notes.conf
sudo systemctl daemon-reload
sudo systemctl enable --now code-notes
curl -f http://127.0.0.1:8080/api/health
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx
curl -f -H 'Host: 8.155.134.37' http://127.0.0.1/api/metadata
```

Nginx 的主配置需要包含 `/etc/nginx/conf.d/*.conf`，安装包通常已设置。`nginx -t` 不通过时修复后再重载。若运行 SELinux 且静态目录或反向代理被拒绝，按审计记录为网站目录设置 `httpd_sys_content_t`，并启用 `httpd_can_network_connect`；不要关闭 SELinux。

阿里云实例安全组入方向放行 TCP 80；若 firewalld 正在运行，再允许 HTTP 服务：

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --reload
```

浏览器打开 http://8.155.134.37/ 。8080 保持仅在本机监听，无需在安全组开放。域名和 HTTPS 可在网站验证后配置。

## 排查和更新

```bash
sudo systemctl status code-notes nginx --no-pager
sudo journalctl -u code-notes -n 100 --no-pager
```

更新文章：修改本地 JSON/Markdown，重新导出并构建后端，替换服务器 JAR 后运行 `sudo systemctl restart code-notes`。更新前端：重新构建并替换前端文件，保持前后端来自同一版本。

实际部署目录为 `/opt/code-notes/frontend`、`/opt/code-notes/backend` 和 `/opt/code-notes/runtime`。服务为 `nginx` 与 `code-notes`，均已开机自启。阿里云安全组已补充 TCP 80 入方向规则；后端保持 127.0.0.1:8080。原宝塔面板保持运行。当前使用 IP 的 HTTP 访问，域名与 HTTPS 尚未配置。

参考：[阿里云 OpenJDK 安装](https://help.aliyun.com/zh/ecs/user-guide/manually-deploy-openjdk)、[OpenJDK 扩展安装](https://help.aliyun.com/zh/ecs/user-guide/install-openjdk-through-extensions)、[阿里云 Nginx 部署](https://help.aliyun.com/zh/ecs/user-guide/deploy-the-lnmp-environment)。
