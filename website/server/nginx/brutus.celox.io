# Brutus — product page. Generated from templates/apps/product-page.
# Live copy: /etc/nginx/sites-available/brutus.celox.io (files in sites-enabled/ are included inside http {},
# so the maps below are legal here). One certificate covers every name; the aliases answer with a 301.

# Agents that ask for Markdown get the Markdown version of the page at /.
map $http_accept $brutus_wants_markdown {
    default 0;
    "~*text/markdown" 1;
}

# /download follows the visitor's platform; first match wins, default = first target.
map $http_user_agent $brutus_target {
    default android;
    "~*android" android;
}

server {
    listen 80;
    listen [::]:80;
    server_name brutus.celox.io;
    location /.well-known/acme-challenge/ { root /var/www/html; }
    location / { return 301 https://brutus.celox.io$request_uri; }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name brutus.celox.io;
    ssl_certificate     /etc/letsencrypt/live/brutus.celox.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/brutus.celox.io/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    root /var/www/brutus.celox.io;
    index index.html;
    charset utf-8;
    charset_types text/plain text/css application/javascript application/json text/xml text/markdown;

    # Security headers are repeated in every location that sets its own add_header
    # (an add_header in a block drops everything inherited).
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
    add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'none'" always;

    # /download and /download/<target>: written by brutus-latest.py (glob: absent file is fine).
    include /etc/nginx/brutus-download*.conf;

    # index.html and index.md pull the release facts from /ssi/ (written by the timer) at serve time.
    ssi on;
    ssi_last_modified off;
    location ^~ /ssi/ {
        internal;
        default_type text/plain;
    }

    # Content negotiation: `Accept: text/markdown` on / returns the Markdown version of the page.
    location = / {
        if ($brutus_wants_markdown) { rewrite ^ /index.md last; }
        try_files /index.html =404;
        add_header Vary "Accept" always;
        add_header Link '</llms.txt>; rel="alternate"; type="text/markdown", </index.md>; rel="alternate"; type="text/markdown", </latest.json>; rel="alternate"; type="application/json", </.well-known/ai-catalog.json>; rel="ai-catalog"; type="application/json"' always;
        add_header Cache-Control "no-cache" always;
        add_header Strict-Transport-Security "max-age=31536000" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
        add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'none'" always;
    }
    location = /index.md {
        types { }
        default_type text/markdown;  # charset comes from charset_types; with it here ssi_types would not match
        ssi_types text/markdown;
        add_header Vary "Accept" always;
        add_header Cache-Control "no-cache" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    # Agent resource discovery (ARD): the catalog, also under the spec's newer name, and the skill it lists.
    location = /.well-known/ai-catalog.json {
        default_type application/json;
        add_header Cache-Control "no-cache" always;
        add_header Access-Control-Allow-Origin "*" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location = /.well-known/ard.json {
        alias /var/www/brutus.celox.io/.well-known/ai-catalog.json;
        default_type application/json;
        add_header Cache-Control "no-cache" always;
        add_header Access-Control-Allow-Origin "*" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location ~ ^/skills/.+\.md$ {
        types { }
        default_type text/markdown;
        add_header Cache-Control "no-cache" always;
        add_header X-Content-Type-Options "nosniff" always;
    }

    # Copied from GitHub by the timer; read by the changelog dialog and by agents.
    location = /changelog.md {
        types { }
        default_type text/markdown;
        add_header Cache-Control "no-cache" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    # Verified copies of the release assets (written by the timer). Versioned names never change:
    # cache for good. Byte ranges + ETag let an interrupted download resume - the reason they exist.
    location ^~ /files/ {
        # The stock mime.types on the VPS has no entry for .apk (or .msi/.appimage) — without these the
        # files go out as application/octet-stream instead of what GitHub sends.
        types {
            application/vnd.android.package-archive apk;
            application/x-apple-diskimage dmg;
            application/x-msi msi;
            application/vnd.microsoft.portable-executable exe;
            application/vnd.appimage AppImage;
            application/vnd.debian.binary-package deb;
            text/plain txt;
        }
        default_type application/octet-stream;
        # An add_header here drops the server-level ones, so HSTS is repeated.
        add_header Strict-Transport-Security "max-age=31536000" always;
        add_header Content-Disposition "attachment" always;
        add_header Cache-Control "public, max-age=31536000, immutable" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location = /latest.json {
        add_header Cache-Control "no-cache" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location /assets/ {
        add_header Cache-Control "public, max-age=31536000, immutable" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location ~ \.(css|js)$ {
        add_header Cache-Control "public, max-age=31536000, immutable" always;
        add_header X-Content-Type-Options "nosniff" always;
    }
    location / {
        add_header Cache-Control "no-cache" always;
        add_header Strict-Transport-Security "max-age=31536000" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
        add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'none'" always;
        try_files $uri $uri/ =404;
    }
}
