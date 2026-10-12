#!/usr/bin/env bash
set -euo pipefail
# First deployment only. Refuse to overwrite an existing blog installation.
test "$(id -u)" = 0
test ! -e /opt/code-notes
test ! -e /etc/nginx/conf.d/code-notes.conf
test ! -e /etc/systemd/system/code-notes.service
printf '%s\n' '837b999068ac002a38faf531279395ca583023d764c548d8a1c25142219492da /root/blog-aliyun-20261012.zip' | sha256sum -c -
mkdir -p /opt/code-notes/{frontend,backend,runtime,releases/20261012}
chmod 0755 /opt/code-notes
unzip -q /root/blog-aliyun-20261012.zip -d /opt/code-notes/releases/20261012
printf '%s\n' '2413149700df0f7d440500a84a8f764c535f21e5a5e87d38328b64eec2c5b500 /opt/code-notes/releases/20261012/runtime/java21-linux.tar.gz' | sha256sum -c -
tar -xzf /opt/code-notes/releases/20261012/runtime/java21-linux.tar.gz -C /opt/code-notes/runtime --strip-components=1
/opt/code-notes/runtime/bin/java -version
id codeblog >/dev/null 2>&1 || useradd --system --home-dir /opt/code-notes --shell /sbin/nologin codeblog
cp -a /opt/code-notes/releases/20261012/frontend/. /opt/code-notes/frontend/
install -m 0644 /opt/code-notes/releases/20261012/backend/blog-backend-1.0.0.jar /opt/code-notes/backend/
chmod -R a+rX /opt/code-notes/frontend /opt/code-notes/runtime
chown -R codeblog:codeblog /opt/code-notes/backend
install -m 0644 /opt/code-notes/releases/20261012/config/code-notes.service /etc/systemd/system/code-notes.service
install -m 0644 /opt/code-notes/releases/20261012/config/blog.conf /etc/nginx/conf.d/code-notes.conf
nginx -t
systemctl daemon-reload
systemctl enable --now code-notes
ready=false
for attempt in $(seq 1 30); do
  if curl --silent --fail http://127.0.0.1:8080/api/health; then ready=true; break; fi
  sleep 1
done
test "$ready" = true
systemctl enable --now nginx
systemctl reload nginx
curl --silent --fail -H 'Host: 8.155.134.37' http://127.0.0.1/api/metadata
printf '\n'
systemctl is-active code-notes nginx
ss -lntp 'sport = :80 or sport = :8080'
