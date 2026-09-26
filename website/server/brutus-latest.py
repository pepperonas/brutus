#!/usr/bin/env python3
"""Publish the newest Brutus release next to its product page.

Generated from templates/apps/product-page. Runs on the VPS from brutus-latest.timer (every 15 min)
and writes, each file only when its content changed:

  <webroot>/latest.json      release facts for the page's JavaScript and for agents (same origin: visitors
                             never call GitHub themselves)
  <webroot>/ssi/*            the same facts as tiny fragments, pulled into index.html / index.md by nginx
                             SSI — so they are in the document without JavaScript
  <webroot>/changelog.md     CHANGELOG.md from the default branch, for the changelog dialog
  <webroot>/ssi/stat-*.txt   the project's counts (site.json "repo_stats", a JSON file in the repository,
                             e.g. lines of code and unit tests), formatted for the page
  <webroot>/ssi/features.*   the project's feature catalogue (site.json "feature_catalog", a text file in
                             the repo), rendered as HTML + Markdown — new features appear without a deploy
  <webroot>/files/<name>     a verified copy of every release asset (site.json "mirror", default on)
  /etc/nginx/brutus-download.conf
                             /download/<target> -> 302 to that target's newest asset, and /download ->
                             the target the visitor's browser asks for (nginx map in the vhost)

Why copies instead of redirects to GitHub: GitHub serves release assets through signed CDN URLs that
expire after about an hour. An interrupted download (phone screen off, Wi-Fi to mobile data, a slow line)
resumes with the same URL and fails once it has expired - the file stays incomplete. Found on Flipper the
Ripper (2026-09-26). The copies have stable URLs with byte ranges + ETag, so a resume always works. A copy
is published only after its size and SHA-256 match the release; an asset without a digest, above
"mirror_max_mb" or failing verification keeps pointing at GitHub.

A failed GitHub call, a release missing one of the configured assets, or an odd changelog changes
nothing: the last good state stays online. nginx is reloaded only when the redirects changed, only after
`nginx -t` passed, and never while certbot is running.
"""
import datetime, glob, hashlib, html, json, os, re, subprocess, sys, tempfile, urllib.request

CONFIG = json.loads(r'''{
 "repo": "pepperonas/brutus",
 "branch": "main",
 "slug": "brutus",
 "domain": "brutus.celox.io",
 "webroot": "/var/www/brutus.celox.io",
 "nginx_include": "/etc/nginx/brutus-download.conf",
 "nginx_var": "brutus",
 "feature_catalog": null,
 "release_tag": null,
 "mirror": true,
 "mirror_max_mb": 500,
 "repo_stats": null,
 "targets": [
  {
   "id": "android",
   "label": "Download APK",
   "short": "APK",
   "asset": "^brutus-v[0-9][0-9A-Za-z.\\-]*\\.apk$",
   "requirement": "Android 8.0+"
  }
 ]
}''')

REPO = CONFIG["repo"]
BRANCH = CONFIG["branch"]
WEBROOT = os.environ.get("SITE_WEBROOT", CONFIG["webroot"])
NGINX_INC = os.environ.get("SITE_NGINX_INC", CONFIG["nginx_include"])
URL_OK = re.compile(r"^https://github\.com/" + re.escape(REPO) + r"/releases/download/[^\s;\"'{}]+$")
CHANGELOG_URL = f"https://raw.githubusercontent.com/{REPO}/{BRANCH}/CHANGELOG.md"
CHANGELOG_MAX = 2_000_000
FEATURES_PATH = CONFIG.get("feature_catalog")
FEATURES_URL = f"https://raw.githubusercontent.com/{REPO}/{BRANCH}/{FEATURES_PATH}" if FEATURES_PATH else None
STATS_CFG = CONFIG.get("repo_stats") or None
STATS_URL = f"https://raw.githubusercontent.com/{REPO}/{BRANCH}/{STATS_CFG['path']}" if STATS_CFG else None
MIRROR = CONFIG.get("mirror", True)
MIRROR_MAX = int(CONFIG.get("mirror_max_mb", 500)) * 1048576
FILES_DIR = os.path.join(WEBROOT, "files")
KEEP_RELEASES = 2  # the current and the previous release, so a download running across a release finishes
SAFE_NAME = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._+-]*$")
STAT_KEY = re.compile(r"^[a-z0-9_]{1,40}$")
STAT_REF = re.compile(r"\{([a-z0-9_]+)(?::(k|int))?\}")


def get(url, accept=None):
    headers = {"User-Agent": f"{CONFIG['slug']}-latest"}
    if accept:
        headers["Accept"] = accept
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=20) as r:
        return r.read(CHANGELOG_MAX + 1)


def pick_release(releases, tag_pattern):
    """The newest published release whose tag matches `tag_pattern`, pre-releases included.

    For a repository whose "latest" release is not the app — BeatByte publishes its ML models as a
    release of their own and its game builds as pre-releases, so `/releases/latest` names the models.
    GitHub lists releases newest first; drafts are skipped. Pure — tested.
    """
    pattern = re.compile(tag_pattern)
    for rel in releases:
        if not rel.get("draft") and pattern.search(rel.get("tag_name", "")):
            return rel
    raise ValueError(f"no release with a tag matching {tag_pattern!r}")


def fetch_release():
    if CONFIG.get("release_tag"):
        releases = json.loads(get(f"https://api.github.com/repos/{REPO}/releases?per_page=30",
                                  "application/vnd.github+json"))
        rel = pick_release(releases, CONFIG["release_tag"])
    else:
        rel = json.loads(get(f"https://api.github.com/repos/{REPO}/releases/latest", "application/vnd.github+json"))
    assets = []
    for t in CONFIG["targets"]:
        pattern = re.compile(t["asset"])
        hits = [a for a in rel.get("assets", []) if pattern.search(a.get("name", ""))]
        if len(hits) != 1:
            if t.get("optional"):
                continue
            raise ValueError(f"target {t['id']}: expected one asset matching {t['asset']!r}, found {len(hits)}")
        a = hits[0]
        url = a["browser_download_url"]
        if not URL_OK.match(url):
            raise ValueError(f"unexpected asset URL: {url!r}")
        digest = a.get("digest") or ""
        assets.append({
            "target": t["id"],
            "label": t["label"],
            "short": t["short"],
            "requirement": t.get("requirement", ""),
            "name": a["name"],
            "url": url,
            "size": a["size"],
            "sha256": digest.split(":", 1)[1] if digest.startswith("sha256:") else "",
        })
    if not assets:
        raise ValueError("no configured asset in the latest release")
    first = assets[0]
    return {
        "version": rel["tag_name"],
        "published": rel.get("published_at", ""),
        "notes": rel.get("html_url", ""),
        "assets": assets,
        # The first target's facts at the top level, for tools that expect a single file.
        "name": first["name"], "url": first["url"], "size": first["size"], "sha256": first["sha256"],
    }


def fetch_changelog():
    body = get(CHANGELOG_URL)
    text = body.decode("utf-8")
    if len(body) > CHANGELOG_MAX or not text.startswith("# Changelog") or "\n## [" not in text:
        raise ValueError("unexpected CHANGELOG.md content")
    return text


FEATURE_SECTION = re.compile(r"^==\s*(.+?)\s*==\s*$")
FEATURE_SEPARATORS = (" \u2014 ", " \u2013 ", " - ")
FEATURE_NAME_MAX = 80


def parse_catalog(text):
    """A feature catalogue: "== Area ==" headings, then one feature per line, "Name — description".

    Lines before the first heading (a title) are skipped. A line without a separator, or whose part
    before it is too long to be a name, becomes a feature without a name. Returns [(area, [(name, text)])].
    """
    areas = []
    for raw in text.replace("\r", "").split("\n"):
        line = raw.strip()
        m = FEATURE_SECTION.match(line)
        if m:
            areas.append((m.group(1), []))
            continue
        if not line or not areas:
            continue
        name, desc = "", line
        for sep in FEATURE_SEPARATORS:
            i = line.find(sep)
            if 0 < i <= FEATURE_NAME_MAX:
                name, desc = line[:i].strip(), line[i + len(sep):].strip()
                break
        areas[-1][1].append((name, desc))
    return [(a, items) for a, items in areas if items]


def _feature_inline(text):
    """Escaped text; `code` spans become <code>. Nothing else is interpreted."""
    parts = text.split("`")
    if len(parts) % 2 == 0:  # an unpaired backtick: keep it literal
        return html.escape(text)
    return "".join(f"<code>{html.escape(p)}</code>" if i % 2 else html.escape(p) for i, p in enumerate(parts))


def feature_fragments(areas):
    total = sum(len(items) for _, items in areas)
    blocks = []
    for n, (area, items) in enumerate(areas):
        lis = "".join(
            f"<li>{'<strong>' + _feature_inline(name) + '</strong> — ' if name else ''}{_feature_inline(desc)}</li>"
            for name, desc in items
        )
        blocks.append(f'<details class="fc-area"{" open" if n == 0 else ""}><summary>{html.escape(area)}'
                      f' <span class="fc-count">{len(items)}</span></summary><ul>{lis}</ul></details>')
    md = []
    for area, items in areas:
        md.append(f"\n### {area}\n")
        md.extend(f"- **{name}** — {desc}" if name else f"- {desc}" for name, desc in items)
    return {
        "features.html": "\n".join(blocks),
        "features-count.txt": str(total),
        "features-areas.txt": str(len(areas)),
        "features.md": "\n".join(md).strip() + "\n",
    }


def fetch_features():
    body = get(FEATURES_URL)
    if len(body) > CHANGELOG_MAX:
        raise ValueError("feature catalogue too large")
    areas = parse_catalog(body.decode("utf-8"))
    if not areas:
        raise ValueError("feature catalogue has no \"== Area ==\" sections")
    return feature_fragments(areas)


def format_stat(value, fmt):
    """"~185k" for fmt "k" (a count under 1000 stays exact), else digits grouped with a narrow no-break
    space — neutral in every page language, where "4,570" would read as a decimal in German."""
    if fmt == "k" and value >= 1000:
        return f"~{round(value / 1000)}k"
    return f"{value:,}".replace(",", "\u202f")


def _stat_value(data, key):
    v = data.get(key)
    # bool is an int in Python — a stray true must not show up as "1".
    if isinstance(v, bool) or not isinstance(v, int) or v < 0:
        raise ValueError(f"repo stats: '{key}' is {v!r}, expected a non-negative integer")
    return v


def parse_stats(body):
    try:
        data = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as e:
        raise ValueError(f"repo stats: not JSON ({e})")
    if not isinstance(data, dict):
        raise ValueError("repo stats: expected a JSON object")
    return data


def stats_fragments(data, items):
    """One value and one detail fragment per configured item. Any bad or missing number raises, so the
    page keeps the last good set instead of showing half of a new one."""
    out = {}
    for item in items:
        key, fmt = item["key"], item.get("format", "int")
        if not STAT_KEY.match(key):
            raise ValueError(f"repo stats: key {key!r} is not a safe name")
        out[f"stat-{key}.txt"] = format_stat(_stat_value(data, key), fmt)
        detail = STAT_REF.sub(lambda m: format_stat(_stat_value(data, m.group(1)), m.group(2) or "int"),
                              item.get("detail", ""))
        out[f"stat-{key}-detail.txt"] = html.escape(detail, quote=False)
    return out


def fetch_stats():
    body = get(STATS_URL)
    if len(body) > 100_000:
        raise ValueError("repo stats file too large")
    return stats_fragments(parse_stats(body), STATS_CFG["items"])


def mb(size):
    return f"{size / 1048576:.1f} MB"


def ssi_fragments(d):
    """English; the page's JavaScript localises. Plain text or escaped HTML only."""
    first = d["assets"][0]
    day = d["published"][:10]
    try:
        pretty = datetime.date.fromisoformat(day).strftime("%-d %b %Y")
    except ValueError:
        pretty = day
    meta = " · ".join(filter(None, [d["version"], mb(first["size"]), pretty, first["requirement"]]))
    others = "".join(
        f'<li><a href="/download/{html.escape(a["target"])}">{html.escape(a["short"])} <small>{mb(a["size"])}</small></a></li>'
        for a in d["assets"][1:]
    )
    sums_html = "".join(
        f"<dt>{html.escape(a['name'])}</dt><dd><code>{html.escape(a['sha256'])}</code></dd>"
        for a in d["assets"] if a["sha256"]
    )
    sums_md = "\n".join(f"- `{a['name']}` — SHA-256 `{a['sha256']}`" for a in d["assets"] if a["sha256"])
    files_md = "\n".join(
        f"- {a['short']}: https://{CONFIG['domain']}/download/{a['target']} ({a['name']}, {mb(a['size'])})"
        for a in d["assets"]
    )
    return {
        "version.txt": d["version"].lstrip("v"),
        "size.txt": mb(first["size"]),
        "date.txt": day,
        "sha.txt": first["sha256"],
        "meta.html": html.escape(meta),
        "others.html": others,
        "checksums.html": sums_html,
        "checksums.md": sums_md,
        "files.md": files_md,
    }


def sha256_of(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def mirror_asset(a, files_dir=None, opener=None):
    """Keep a verified copy of one asset in files/. Returns its path; raises if it cannot be verified."""
    files_dir = files_dir or FILES_DIR
    if not SAFE_NAME.match(a["name"]):
        raise ValueError(f"unsafe asset name {a['name']!r}")
    if not re.fullmatch(r"[0-9a-f]{64}", a.get("sha256") or ""):
        raise ValueError(f"{a['name']}: no SHA-256 digest, cannot verify a copy")
    if a["size"] > MIRROR_MAX:
        raise ValueError(f"{a['name']}: larger than mirror_max_mb")
    os.makedirs(files_dir, exist_ok=True)
    target = os.path.join(files_dir, a["name"])
    side = target + ".sha256"
    if os.path.exists(target) and os.path.getsize(target) == a["size"] and same(side, a["sha256"] + "\n"):
        return target
    fd, tmp = tempfile.mkstemp(dir=files_dir, prefix=".latest-")
    try:
        h, n = hashlib.sha256(), 0
        src = (opener or (lambda u: urllib.request.urlopen(
            urllib.request.Request(u, headers={"User-Agent": f"{CONFIG['slug']}-latest"}), timeout=60)))(a["github_url"])
        with os.fdopen(fd, "wb") as out, src as r:
            for chunk in iter(lambda: r.read(1 << 20), b""):
                out.write(chunk)
                h.update(chunk)
                n += len(chunk)
        if n != a["size"] or h.hexdigest() != a["sha256"]:
            raise ValueError(f"{a['name']}: copy does not match the release (size {n})")
        os.chmod(tmp, 0o644)
        os.replace(tmp, target)
    finally:
        if os.path.exists(tmp):
            os.remove(tmp)
    write_if_changed(side, a["sha256"] + "\n")
    return target


def prune_files(keep_names, files_dir=None):
    """Delete copies that belong to neither the current nor the previous release (by mtime)."""
    files_dir = files_dir or FILES_DIR
    others = sorted((p for p in glob.glob(os.path.join(files_dir, "*")) if not p.endswith(".sha256")
                     and os.path.basename(p) not in keep_names), key=os.path.getmtime, reverse=True)
    # Everything older than the newest len(keep_names) * (KEEP_RELEASES - 1) other files goes.
    for old in others[len(keep_names) * (KEEP_RELEASES - 1):]:
        os.remove(old)
        if os.path.exists(old + ".sha256"):
            os.remove(old + ".sha256")


def apply_mirror(d):
    """Point each asset at its verified copy where one exists; GitHub stays the source otherwise."""
    local = []
    for a in d["assets"]:
        a["github_url"] = a["url"]
        if not MIRROR:
            continue
        try:
            mirror_asset(a)
        except Exception as e:  # noqa: BLE001 - one asset failing must not stop the others
            print(f"{CONFIG['slug']}-latest: no local copy of {a['name']}, GitHub stays ({e})", file=sys.stderr)
            continue
        a["url"] = f"https://{CONFIG['domain']}/files/{a['name']}"
        local.append(a["name"])
    if local:
        prune_files(set(local))
    first = d["assets"][0]
    d["url"], d["github_url"] = first["url"], first["github_url"]
    return local


def download_conf(d):
    lines = [f"# Written by {CONFIG['slug']}-latest.py - do not edit."]
    for a in d["assets"]:
        lines.append(f"location = /download/{a['target']} {{\n    add_header Cache-Control \"no-store\" always;\n"
                     f"    return 302 {a['url'].replace('https://' + CONFIG['domain'], '')};\n}}")
    ids = {a["target"] for a in d["assets"]}
    var = CONFIG["nginx_var"]
    # /download follows the visitor's platform (map in the vhost); a platform without an asset in this
    # release falls back to the first target.
    lines.append(f"location = /download {{\n    add_header Cache-Control \"no-store\" always;\n"
                 f"    add_header Vary \"User-Agent\" always;\n    return 302 /download/${var}_target;\n}}")
    fallbacks = [t["id"] for t in CONFIG["targets"] if t["id"] not in ids]
    for missing in fallbacks:
        lines.append(f"location = /download/{missing} {{\n    return 302 /download/{d['assets'][0]['target']};\n}}")
    return "\n".join(lines) + "\n"


def same(path, text):
    try:
        with open(path) as f:
            return f.read() == text
    except FileNotFoundError:
        return False


def write_if_changed(path, text, mode=0o644):
    if same(path, text):
        return False
    fd, tmp = tempfile.mkstemp(dir=os.path.dirname(path), prefix=".latest-")
    with os.fdopen(fd, "w") as f:
        f.write(text)
    os.chmod(tmp, mode)
    os.replace(tmp, path)
    return True


def main():
    try:
        d = fetch_release()
    except Exception as e:  # network, rate limit, malformed release: keep the last good state
        print(f"{CONFIG['slug']}-latest: keeping previous state ({e})", file=sys.stderr)
        return 1
    local = apply_mirror(d)
    changed_json = write_if_changed(os.path.join(WEBROOT, "latest.json"), json.dumps(d, indent=2) + "\n")
    ssi_dir = os.path.join(WEBROOT, "ssi")
    os.makedirs(ssi_dir, exist_ok=True)
    changed_ssi = False
    for name, text in ssi_fragments(d).items():
        changed_ssi |= write_if_changed(os.path.join(ssi_dir, name), text)
    try:
        changed_log = write_if_changed(os.path.join(WEBROOT, "changelog.md"), fetch_changelog())
    except Exception as e:  # the dialog keeps showing the last good copy
        print(f"{CONFIG['slug']}-latest: changelog not refreshed ({e})", file=sys.stderr)
        changed_log = False

    changed_feat = False
    if FEATURES_URL:
        try:
            for name, text in fetch_features().items():
                changed_feat |= write_if_changed(os.path.join(ssi_dir, name), text)
        except Exception as e:  # the page keeps showing the last good list
            print(f"{CONFIG['slug']}-latest: features not refreshed ({e})", file=sys.stderr)

    changed_stats = False
    if STATS_URL:
        try:
            for name, text in fetch_stats().items():
                changed_stats |= write_if_changed(os.path.join(ssi_dir, name), text)
        except Exception as e:  # the page keeps showing the last good numbers
            print(f"{CONFIG['slug']}-latest: repo stats not refreshed ({e})", file=sys.stderr)

    conf = download_conf(d)
    changed_conf = False
    if not same(NGINX_INC, conf) and not os.environ.get("SITE_NO_NGINX"):
        if subprocess.run(["pgrep", "-x", "certbot"], capture_output=True).returncode == 0:
            print(f"{CONFIG['slug']}-latest: certbot running, nginx update deferred", file=sys.stderr)
            return 0
        backup = open(NGINX_INC).read() if os.path.exists(NGINX_INC) else None
        write_if_changed(NGINX_INC, conf)
        test = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
        if test.returncode != 0:
            print(test.stderr, file=sys.stderr)
            if backup is None:
                os.remove(NGINX_INC)
            else:
                write_if_changed(NGINX_INC, backup)
            return 1
        subprocess.run(["systemctl", "reload", "nginx"], check=True)
        changed_conf = True
    elif os.environ.get("SITE_NO_NGINX"):
        write_if_changed(NGINX_INC, conf)
    print(
        f"{CONFIG['slug']}-latest: {d['version']} targets={','.join(a['target'] for a in d['assets'])} local={len(local)}/{len(d['assets'])} "
        f"json={'new' if changed_json else 'same'} ssi={'new' if changed_ssi else 'same'} "
        f"changelog={'new' if changed_log else 'same'} "
        f"features={'new' if changed_feat else ('off' if not FEATURES_URL else 'same')} "
        f"stats={'new' if changed_stats else ('off' if not STATS_URL else 'same')} nginx={'reloaded' if changed_conf else 'same'}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
