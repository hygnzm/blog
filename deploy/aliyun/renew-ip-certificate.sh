#!/bin/bash
set -euo pipefail
# Only public challenge files need world-read access. ACME state remains private.
umask 022
install -d -m 700 /var/lib/code-notes-acme
install -d -m 755 /opt/code-notes/frontend/.well-known/acme-challenge
chmod 755 /opt/code-notes/frontend/.well-known
exec /opt/code-notes/acme/bin/lego run \
    --accept-tos --account-id code-notes-ip \
    --domains 8.155.134.37 --profile shortlived \
    --http --http.webroot /opt/code-notes/frontend \
    --path /var/lib/code-notes-acme --cert.timeout 120 \
    --deploy-hook /opt/code-notes/acme/bin/reload-nginx.sh
