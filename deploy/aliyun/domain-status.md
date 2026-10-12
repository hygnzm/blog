# 域名绑定状态

检查日期：2026-10-12（Asia/Shanghai）。

- 域名：hygnzm.abrdns.com，用户确认是 ClouDNS 免费子域名。
- ClouDNS 管理区域：hygnzm.abrdns.com。已新增根主机 A 记录指向 8.155.134.37，TTL 为 1 小时，现有 NS 记录保留。
- 从 ECS 查询权威服务器：`dig +short hygnzm.abrdns.com @ns71.cloudns.net` 返回 8.155.134.37。递归服务器在检查时仍缓存原来的停放地址，可能需要等待 TTL 过期。
- Nginx 已配置 `server_name hygnzm.abrdns.com 8.155.134.37;`，原配置备份至 `/etc/nginx/conf.d/code-notes.conf.bak-20261012-domain`。`nginx -t` 通过且重载成功。
- 服务端带该 Host 的请求返回 HTTP 200，文章 API 健康检查正常。
- 从外部直接向正确公网 IP 发送该域名请求，返回 HTTP 403，响应 Server 为 Beaver，HTML 标题为 `Non-compliance ICP Filing`，页面加载阿里云的备案阻断提示。这是公网备案限制，不能标记为域名已可访问。

## 已知限制与后续方案

河源服务器位于中国内地。个人网站需要满足ICP备案和阿里云接入要求；共享免费子域名不能由当前用户独立完成根域名所有者的实名认证及备案。

可选择：

1. 保留河源服务器：使用自己注册并持有的可备案域名，完成阿里云ICP备案或接入备案，再绑定域名。
2. 保留该免费子域名：选择中国内地以外的服务器，迁移前后端并更新解析。这需要用户确定服务器选择，当前没有购买或创建新实例。

当前没有配置 HTTPS，也没有尝试规避备案拦截。

参考：[域名核验要求](https://help.aliyun.com/zh/icp-filing/basic-icp-service/user-guide/prepare-and-check-the-domain-name)、[备案与子域名问题](https://help.aliyun.com/zh/icp-filing/basic-icp-service/support/for-the-record-domain-faq/)。
