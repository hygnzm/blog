# 公网 IP HTTPS（2026-10-12 已部署）

访问地址：https://8.155.134.37/ 。Vue 静态资源由 Nginx 提供，/api 转发到本机 Spring Boot 8080，访客只读。

原因：443 安全组和防火墙已放行，但 Nginx 原先只监听 80，没有 TLS 证书。现已申请 Let's Encrypt shortlived IP 证书并启用 443。证书 SAN 为 IP Address:8.155.134.37，签发者 YE1，本次到期时间为 2026-10-19 14:19:07（北京时间）。

## 文件与自动续期

- Nginx：/etc/nginx/conf.d/code-notes.conf，对应本目录 blog-https.conf。
- 原配置备份：/etc/nginx/conf.d/code-notes.conf.bak-20261012-https。
- lego 5.5.2：/opt/code-notes/acme/bin/lego，来自官方 GitHub 发布，SHA-256 为 2a35505089e7772c92e1e9ac144df91151ef2eca8568630db0ff91fca06d9bef。
- ACME 账户和证书：/var/lib/code-notes-acme（0700），证书私钥权限 0600；不要上传到代码仓库。
- code-notes-acme.timer 已开机自启，每天 00:00、12:00 各检查一次，随机延迟最多 30 分钟，并补执行停机期间错过的检查。
- lego 根据 CA 的续期信息判断是否需要更新；成功更新后运行 reload-nginx.sh，先检查配置再重载 Nginx。
- 首次手动检查成功，因证书尚未到续期时间而跳过；重载脚本已单独验证成功。尚未经过一次自然到期续期周期。

保持 80 端口可达，保留 /opt/code-notes/frontend/.well-known/acme-challenge，续期依赖 HTTP-01 验证。脚本用 umask 022 写入公开验证文件；账户目录和私钥仍受单独权限保护。

```bash
systemctl list-timers code-notes-acme.timer --no-pager
journalctl -u code-notes-acme.service -n 30 --no-pager
systemctl start code-notes-acme.service
openssl x509 -in /var/lib/code-notes-acme/certificates/8.155.134.37.crt -noout -dates -ext subjectAltName
curl -f https://8.155.134.37/api/health
```

## 验证结果

Workbench CLI 实例 i-f8z42mfqmawjwweho6om：Nginx 443 监听正常，nginx、code-notes、code-notes-acme.timer 均 active、enabled。服务器及本地 Windows 公网 HTTPS 健康接口返回 200 / status=ok，使用默认信任校验，没有跳过证书校验，本地 ssl_verify_result=0。浏览器正常打开 HTTPS 首页（17 篇）及 Transformer 正文，控制台无错误。

该证书只覆盖 IP。hygnzm.abrdns.com 的备案拦截状态另见 domain-status.md；当前 HTTPS 配置不解决域名备案问题。

官方资料：[Let's Encrypt IP 证书与短期证书](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability)、[lego 项目](https://go-acme.github.io/lego/)。IP 证书有效期为 160 小时，因此自动续期不可停用。
