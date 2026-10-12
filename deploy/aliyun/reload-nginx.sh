#!/bin/bash
set -euo pipefail
chmod 600 /var/lib/code-notes-acme/certificates/8.155.134.37.key
/usr/sbin/nginx -t
/usr/bin/systemctl reload nginx
