# Product page — brutus.celox.io

The app's website: <https://brutus.celox.io>. Generated from
[`templates/apps/product-page`](https://github.com/pepperonas/templates/tree/main/apps/product-page)
(private) — the same page as <https://flipper-the-ripper.celox.io>, which is the reference.

**`site.json` is the source.** Every text (in English, German, Spanish, Italian and French), the
download targets, the colours and the hosting settings live there. Change it, then regenerate:

```bash
python3 ~/claude/_templates/apps/product-page/build.py website/site.json website --force
python3 ~/claude/_templates/apps/product-page/build.py --check website
./website/deploy.sh            # or: ./website/deploy.sh server   (timer + vhost too)
```

Hand edits to generated files are overwritten by the next build — put lasting changes into
`site.json`, or into the template (then every page gets them).

A static page with no build step at serve time, served by nginx on the VPS. Two things keep it
current without anyone touching this folder: a **server-side timer** that follows GitHub Releases and
the changelog, and **nginx SSI**, which puts those facts into the HTML at serve time. The page is
written for three readers — people, search engines, and **AI agents** — and each gets the same facts.

- [What is where](#what-is-where)
- [How "always the newest release" works](#how-always-the-newest-release-works)
- [For agents and tools](#for-agents-and-tools)
- [Languages](#languages)
- [Design notes](#design-notes)
- [Privacy and security](#privacy-and-security)
- [Deploying](#deploying)
- [Setting it up from scratch](#setting-it-up-from-scratch)
- [Checking it](#checking-it)
- [Traps we have already stepped in](#traps-we-have-already-stepped-in)

## What is where

| File | Purpose |
|------|---------|
| `index.html` | The page. English in the markup (what crawlers and agents read without JavaScript). Contains the SSI includes for the release facts and the JSON-LD. |
| `styles.css` | All styling. Colours are the app's own dark scheme (app/src/main/java/com/pepperonas/brutus/ui/theme/Color.kt — dark red-seed roles (primary lifted to red tone 80 for text contrast on the web)) as tokens on `:root`, from `site.json` → `theme`. |
| `app.js` | Language switching and menu, the download button and meta line from `latest.json`, licence dialog, sticky bar, reveal animation. |
| `i18n.js` | German, Spanish, Italian and French strings, keyed like the `data-i18n` attributes. A missing key falls back to the English in the markup. |
| `changelog.js` | Changelog dialog: loads `changelog.md` (same origin) and renders it with a small escaping Markdown renderer. |
| `index.md` | The page as Markdown for agents, with the same SSI includes. |
| `llms.txt` | One-screen summary for LLMs and agents: what the app is, every link, install, verification, limits. |
| `robots.txt`, `sitemap.xml` | Allow everything; point at the sitemap, `llms.txt` and `index.md`. |
| `assets/` | Hero art (`hero-1400.webp`, `hero-2400.webp`, `hero.jpg` fallback), `og.jpg` (1200 × 630), `screens.webp/.jpg` or the gallery images `gallery/<id>.webp/.jpg`, easter-egg images `eggs/<id>.webp/.jpg`, `mark.svg` icon, `apple-touch-icon.png`. |
| `server/brutus-latest.py` + `.service` / `.timer` | The timer (see below). Installed to `/usr/local/sbin/` and `/etc/systemd/system/`. |
| `server/nginx/brutus.celox.io` | Vendored vhost — the live file is `/etc/nginx/sites-available/brutus.celox.io`. |
| `deploy.sh` | Ships the page; `deploy.sh server` also installs timer and vhost. |

Written **on the server** by the timer, never in this folder (and excluded from `deploy.sh`'s
`--delete`): `latest.json`, `changelog.md`, `ssi/`, `/etc/nginx/brutus-download.conf`.

## How "always the newest release" works

`brutus-latest.timer` runs `brutus-latest.py` every 15 minutes (and 2 minutes after boot). It asks the
GitHub API for the latest release and matches every **target** from `site.json` (`targets[].asset`, a
regular expression) to **exactly one** asset — an optional target may be missing. Then it writes, each
file only when its content changed:

| Output | Used by |
|--------|---------|
| `latest.json` — `version`, `published`, `notes`, `assets[]` (`target`, `label`, `short`, `requirement`, `name`, `url`, `size`, `sha256` from the API's `digest`), plus the first target's `name`/`url`/`size`/`sha256` at the top level | The page's JavaScript (button, localised meta line, other platforms, checksums) and agents |
| `ssi/version.txt`, `size.txt`, `date.txt`, `sha.txt`, `meta.html`, `others.html`, `checksums.html`, `checksums.md`, `files.md` | nginx SSI in `index.html` (meta line, other platforms, checksums, JSON-LD `softwareVersion`/`fileSize`/`dateModified`) and `index.md` |
| `changelog.md` — `CHANGELOG.md` from the default branch via `raw.githubusercontent.com` | The changelog dialog and agents |
| `ssi/stat-<key>.txt`, `stat-<key>-detail.txt` — only with `repo_stats` in `site.json`: the counts from the repository's stats JSON (e.g. lines of code, unit tests), validated and formatted (`stats_fragments`); a bad or missing number keeps the last good set | The **In numbers** strip under the platform chips, and `index.md` |
| `ssi/features.html`, `features.md`, `features-count.txt`, `features-areas.txt` — only with `feature_catalog` in `site.json`: the repo's feature catalogue, parsed (`parse_catalog`) and escaped | The **Every feature** section in `index.html` and `index.md` |
| `/etc/nginx/brutus-download.conf` — `/download/<target>` → 302 to that asset; `/download` → the visitor's platform (a `map` on `User-Agent` in the vhost, first target by default) | The download button in the HTML and every link that should survive releases |

A failed GitHub call, a release missing a required target, or a changelog that does not start with
`# Changelog` changes **nothing** — the last good state stays online. nginx is reloaded only when the
redirect changed, only after `nginx -t` passed (a failing test restores the previous include), and
**never while certbot is running**.

So after a release nothing needs doing here. To make the site show it at once instead of within
15 minutes:

```bash
ssh root@69.62.121.168 'systemctl start brutus-latest.service; journalctl -u brutus-latest -n 3 -o cat'
# brutus-latest: v1.11.0 json=new ssi=new changelog=new nginx=reloaded
```

Give nginx a few seconds after the reload before checking `/download` — right after it, one request
can still see the old target.

## For agents and tools

Everything a person sees is available without running JavaScript:

| What | Where | Notes |
|------|-------|-------|
| Newest release | `https://brutus.celox.io/download`, `/download/<target>` | 302 to the current GitHub release asset — `/download` for the visitor's platform. The download button's `href` in the HTML is `/download`; JavaScript picks the platform in the browser and swaps in the direct asset URL. |
| Release facts | HTML (meta line, *Verify*), JSON-LD, `index.md` | Filled in by SSI at serve time — version, size, date, SHA-256. |
| Release as JSON | `/latest.json` | Fields as above. `Cache-Control: no-cache`. |
| Page as Markdown | `/index.md`, or `/` with `Accept: text/markdown` | Content negotiation; responses carry `Vary: Accept`. |
| Summary | `/llms.txt` | The [llms.txt](https://llmstxt.org) convention: purpose, links, install, verify, features, limits. |
| Changelog | `/changelog.md` | `text/markdown; charset=utf-8`, mirrored from GitHub. |
| Discovery | `<link rel="alternate">` in `<head>` and an HTTP `Link` header on `/` | Both point at `llms.txt`, `index.md` and `latest.json`. |
| Structured data | JSON-LD in `<head>` | `MobileApplication` (features, languages, licence, price 0, download URL, version, size), `HowTo` (install steps), `FAQPage` (the six FAQ answers). |

Try it:

```bash
curl -sI https://brutus.celox.io/ | grep -i -E '^link|^vary'
curl -s  -H 'Accept: text/markdown' https://brutus.celox.io/ | head -20
curl -s  https://brutus.celox.io/latest.json
```

Rules that keep it that way: every fact on the page must also be in `index.md` and, briefly, in
`llms.txt`; the FAQ text exists three times (HTML, JSON-LD `FAQPage`, `index.md`) and changes in all
three; controls are real `<button>`/`<a>` elements with readable names; one `<h1>`.

## Languages

- **English** is in `index.html`. `app.js` captures it on the first switch, so it is written once.
- `i18n.js` (generated) holds **de, es, it, fr** as `key → HTML`. Every element with `data-i18n="key"` has its
  `innerHTML` replaced; `data-i18n-alt` and `data-i18n-aria` do the same for `alt` and `aria-label`.
- The language comes from `navigator.languages` (first of en/de/es/it/fr), is remembered in
  `localStorage` (`brutus-lang`) and switched with the flag menu (keyboard: arrows, Home/End, Enter,
  Escape). Flags are CSS gradients (the Union Jack a data-URI SVG) — no image requests.
- Size and date in the meta line use `toLocaleString` / `toLocaleDateString` of the chosen language.
- French uses a narrow no-break space (U+202F) before `: ? ! ;` and inside `« »`.
- **Changing or adding a text:** edit `site.json` → `content` (every text has `en`, `de`, `es`, `it`, `fr`)
  and rebuild. `build.py --check` fails on a missing language, a leftover `TODO` or an unknown placeholder.
- **Adding a language:** in the template (`build.py` `LANGS`, `files/i18n.generic.json`, `files/app.js`
  `LANGS`/`LANG_NAMES`, a `.flag-xx` rule in `files/styles.css`), then rebuild every page.

## Design notes

- Hero: the banner fills the section as a background (`object-fit: cover`), its motif on the left
  stays bright, a gradient sinks the banner's own wordmark under the text on the right.
  Served as WebP in 1400 and 2400 px through `srcset` — never scaled up, which is what made the first
  version look soft. On phones the whole image sits above the text.
- Material 3 Expressive in the app's colours: pill buttons, 28 px radii, spring easings. Everything
  that moves is off under `prefers-reduced-motion`.
- System fonts only (`system-ui`, monospace `ui-monospace, "SF Mono", …`).
- Dialogs are `<dialog>` + `showModal()`; the licence text is re-flowed from `LICENSE` (the file's
  hard 78-column breaks look broken in a narrower box); focus starts on the content, not the close button.
- Asset URLs carry `?v=N` and are served `immutable` for a year — **a changed file needs a new
  number**, or returning visitors keep the old one. All references share one number: raise
  `asset_version` in `site.json` and rebuild.

## Privacy and security

- **No request to a third party** from the visitor's browser: no CDN, no web fonts, no analytics, no
  GitHub API call (the timer does that on the server). The only outside traffic is the file download
  a visitor starts.
- CSP `default-src 'self'` with `img-src 'self' data:` — so no inline scripts and no `style="…"`
  attributes (JSON-LD is fine; it is not executed).
- HSTS, `nosniff`, `Referrer-Policy`, `Permissions-Policy`. nginx drops inherited `add_header`s in any
  `location` that sets its own — every such block repeats the headers it needs.
- External links open in a new tab with `rel="noopener noreferrer"`; the download does not.
- `/ssi/` is `internal` — only reachable as an SSI subrequest (a direct request is 404).

## Deploying

```bash
./website/deploy.sh          # page and assets (rsync --delete, keeps the timer's files)
./website/deploy.sh server   # also: brutus-latest.py, its units, the vhost, nginx reload
```

`deploy.sh server` refuses to run while certbot is active. Both end with a run of the timer.

## Setting it up from scratch

Only needed on a new server or after losing the VPS.

1. **DNS:** A records for the slug and every alias → `69.62.121.168` in the
   `celox.io` zone, via the Hostinger API from raspi5 (token in `/root/.acme.sh/account.conf`, quoted —
   strip with `tr -d "\047\042"`). Check with `dig @1.1.1.1`, not the local resolver (Pi-hole and macOS
   cache a negative answer).
2. **Certificate:** put an HTTP-only vhost in place that serves `/.well-known/acme-challenge/` from
   `/var/www/html`, reload, then
   `certbot certonly --webroot -w /var/www/html -d brutus.celox.io -d <alias> … --deploy-hook "systemctl reload nginx"`.
3. `mkdir -p /var/www/brutus.celox.io`, then `./website/deploy.sh server`.

The vhost defines `map $http_accept $ftr_wants_markdown` at `http` level (files in `sites-enabled/`
are included inside `http {}`) and needs nginx ≥ 1.24 syntax `listen 443 ssl http2;`.

## Checking it

```bash
B=https://brutus.celox.io
curl -s  -o /dev/null -w '%{http_code}\n' $B/                          # 200
curl -sI $B/download | grep -i ^location                                # current file for this platform
curl -sI $B/download/<target> | grep -i ^location                       # …/files/<asset> — served here, not GitHub
curl -sI -H 'Range: bytes=0-99' "$(curl -sI $B/download/<target> | awk 'tolower($1)=="location:"{print $2}' | tr -d '\r')" | head -1   # HTTP/2 206 — resumable
curl -s  $B/ | grep -c '<!--#'                                          # 0 — no unprocessed SSI
curl -s  $B/ | python3 -c "import sys,re,json;json.loads(re.search(r'ld\+json\">(.*?)</script>',sys.stdin.read(),re.S).group(1));print('JSON-LD ok')"
curl -s  -H 'Accept: text/markdown' $B/ | grep 'Current version'       # real version, not a directive
curl -sI $B/changelog.md | grep -i content-type                          # text/markdown; charset=utf-8
curl -sI https://<alias>/x?y=1 | grep -i ^location                        # 301 to the main name, path kept
echo | openssl s_client -connect brutus.celox.io:443 2>/dev/null | openssl x509 -noout -ext subjectAltName -enddate
ssh root@69.62.121.168 'systemctl list-timers brutus-latest.timer --no-pager'
```

In a browser (Playwright): 1440 × 900 and 390 × 844, every language once, the language menu by
keyboard, both dialogs, zero console messages, zero horizontal overflow.

## Traps we have already stepped in

- **SSI blocks must be defined before their first use.** The fallbacks (`<!--# block … -->`) sit at
  the top of `<head>`; when they were in `<body>`, the JSON-LD in `<head>` rendered
  `[an error occurred while processing the directive]`.
- **SSI attributes inside JSON use single quotes** (`include virtual='/ssi/version.txt'`). Double
  quotes get escaped to `\"` in JSON, which nginx does not parse.
- **`ssi_types` compares the bare MIME type.** `default_type "text/markdown; charset=utf-8"` made SSI
  skip `index.md`; the charset now comes from `charset_types`.
- **Test without JavaScript.** All three faults above were invisible in a browser — the JavaScript
  paints over them. `curl` showed them.
- **Negative DNS cache:** looking a new name up before its record exists makes Pi-hole and macOS
  remember "does not exist" for minutes.
- **`deploy.sh` uses `rsync --delete`** — anything the timer writes into the webroot must be listed
  in its excludes, or a deploy deletes it until the next timer run.
- **The changelog dialog renders by version, in batches** (10 on open, 25 per "Older versions"). A
  changelog with 539 releases rendered at once built ~11 000 DOM nodes (750 KB of HTML) and stalled
  older machines. Version headings accept `-`, `–` and `—` as separator — a project that switched to
  the em dash had 494 of its releases shown as sub-headings. Pinned in `tests/changelog.test.cjs`.
- **No `backdrop-filter` on the fixed top bar.** It re-blurs everything under it on every scroll
  frame; on a 2015 Intel Mac scrolling dropped to ~24 fps.
- **Downloads that hang at 100 % on Android.** GitHub release asset links are signed and expire after
  about an hour; Chrome — above all a Custom Tab opened from another app — could not finish or resume
  them and sat at *24,52 MB von 24,52 MB* forever (XCam on a Galaxy S24, Flipper the Ripper). Invisible
  to `curl`, desktop browsers and the nginx log, because the button went straight to `github.com`. The
  timer now keeps a verified copy under `/files/` and `/download` points there (`mirror`, default on);
  pages built before 2026-09-26 need `build.py --force` **and** `deploy.sh server`.
- **Hero images:** a 1024 px banner stretched over a 1440 px screen looks soft. Deliver at least the
  display width, in two sizes.
