# Update check — design (approved 2026-09-26)

## Goal
A user who installed Brutus learns when a newer release is published (product page or GitHub).
Brutus stays offline unless the user opts in.

## Decisions
- **Opt-in**, default off (also for existing installs): switch "Check for updates" in the ⋮ menu of the
  alarm list. Turning it on schedules the checks and runs one right away; on Android 13+ it asks for the
  notification permission if missing. Turning it off cancels all work — no request leaves the phone.
- **Source**: `GET https://brutus.celox.io/latest.json` → `version`. Fallback when the page is
  unreachable: `GET https://api.github.com/repos/pepperonas/brutus/releases/latest` → `tag_name`.
  Plain GET, no identifiers, `HttpURLConnection`, 10 s timeouts, body capped at 64 KB.
- **Comparison**: numeric per dotted part, leading `v` and any `-suffix`/`+build` ignored;
  `2.10.0 > 2.9.1`; unparsable versions never count as newer.
- **Background**: WorkManager, unique periodic work every 24 h, `NetworkType.CONNECTED`; the immediate
  check is a unique one-time request with the same constraint. Failures are silent and retried at the
  next interval.
- **Notification**: once per version (last notified version persisted), new channel `brutus_updates`
  ("App updates", default importance, does **not** bypass DND). Tap opens
  `https://brutus.celox.io/download`.
- **Banner** above the alarm list (existing banner style, info colours) while the switch is on and the
  last found version is newer than the installed one; "Download" opens the same URL.
- **Docs**: manifest gains `INTERNET`; README (en/de) permissions + privacy wording, product page texts
  (feature "offline", FAQ "How do I update?", limits) and CHANGELOG are updated to say exactly this.

## Out of scope
In-app download/installation (`REQUEST_INSTALL_PACKAGES`), in-app changelog.

## Tests (JVM, no mocking framework)
Version comparison; parsing of both JSON shapes incl. missing fields/garbage; notify-once decision;
checker end-to-end with a fake fetcher (disabled → no fetch, newer → one notification, same version
twice → one notification, older/equal → none); scheduler enqueues/cancels via WorkManagerTestInitHelper;
channel properties in BrutusApplicationTest; string parity via the existing ResourceParityTest.
