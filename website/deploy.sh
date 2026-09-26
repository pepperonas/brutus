#!/usr/bin/env bash
# Deploy the product page to brutus.celox.io (generated from templates/apps/product-page).
#   ./website/deploy.sh          static files only
#   ./website/deploy.sh server   also the release timer + nginx vhost
set -euo pipefail
cd "$(dirname "$0")"
HOST=root@69.62.121.168
ROOT=/var/www/brutus.celox.io
SLUG=brutus

# latest.json, changelog.md and ssi/ are written on the server by the timer — never delete them from here.
rsync -avz --delete --exclude latest.json --exclude changelog.md --exclude ssi/ --exclude files/ --exclude server/ \
  --exclude deploy.sh --exclude README.md --exclude site.json --exclude .gitignore --exclude download.conf --exclude .DS_Store ./ "$HOST:$ROOT/"
ssh "$HOST" "chown -R root:root $ROOT && chmod -R u=rwX,go=rX $ROOT"

if [[ "${1:-}" == server ]]; then
  if ssh "$HOST" 'pgrep -x certbot >/dev/null'; then echo "certbot is running — try again later" >&2; exit 1; fi
  scp server/$SLUG-latest.py "$HOST:/usr/local/sbin/$SLUG-latest.py"
  scp server/$SLUG-latest.service server/$SLUG-latest.timer "$HOST:/etc/systemd/system/"
  scp server/nginx/brutus.celox.io "$HOST:/etc/nginx/sites-available/brutus.celox.io"
  ssh "$HOST" "chmod 755 /usr/local/sbin/$SLUG-latest.py &&
    ln -sf /etc/nginx/sites-available/brutus.celox.io /etc/nginx/sites-enabled/ &&
    nginx -t && systemctl reload nginx &&
    systemctl daemon-reload && systemctl enable --now $SLUG-latest.timer"
fi

ssh "$HOST" "systemctl start $SLUG-latest.service; journalctl -u $SLUG-latest -n 1 --no-pager -o cat"
