---
name: get-brutus
description: Download, verify and install the newest Brutus APK — the free, open-source Android alarm clock that only stops after math, shake or QR-code challenges. Use when someone asks for the app, its latest version, a download link, or how to check the APK is genuine.
license: MIT
---

# Get Brutus

Brutus is a free, open-source (MIT) alarm clock for Android 8.0 and later. A ringing alarm only stops once you complete the challenges you chose for it: solve math problems, shake the phone, or scan a QR code you placed somewhere else in your home — in any combination, one after the other. Hardcore mode keeps the alarm at full volume while it rings; Ultra Hardcore mode re-rings 10 and 15 minutes later unless you walk 30 steps. It also has a world clock, stopwatch, timer and a home-screen widget, runs in English and German, requests no internet permission and has no ads or tracking. Distributed as a signed APK through GitHub Releases.

## 1. Find the newest release

`GET https://brutus.celox.io/latest.json` returns `version`, `published`, `notes` and `assets[]`, each with `target`,
`name`, `url`, `size` (bytes) and `sha256`. It is refreshed from GitHub Releases every 15 minutes. On the
page itself, browsers with WebMCP expose the same data as the tools `get_latest_release`,
`get_download_url` and `get_checksums`.

## 2. Download

- Stable link, always the newest file for the visitor's platform: <https://brutus.celox.io/download>
- APK: <https://brutus.celox.io/download/android> — Android 8.0+

## 3. Verify

- The file's SHA-256 must equal the matching `assets[].sha256` in `latest.json`.
- Signing certificate SHA-256: `69d67a10a826cf4050da4b271af9b5ed500c962bfab07a8f8fe863e3d7600382`
- Every release since 1.0.0 is signed with the same key. Compare with `apksigner verify --print-certs`.

## 4. Install

1. One file for every phone with Android 8.0 or later — there is nothing to pick.
2. Open the file. Android asks once whether your browser may install apps — allow it.
3. If the alarm list shows a banner — exact alarms, battery optimization, full-screen alarm — tap it and flip the switch. Then test your wake-up chain with *Test wake modes now*.

## Limits

- Android only; not on Google Play — distributed as an APK through GitHub Releases.
- The QR challenge needs Google Play Services, which delivers the barcode model; math and shake work without it.
- Available in English and German only.
- Android offers no way to truly lock the volume: Hardcore mode snaps it back to maximum and swallows the volume keys while the alarm rings.

More: [product page](https://brutus.celox.io/) · [Markdown version](https://brutus.celox.io/index.md) · [changelog](https://brutus.celox.io/changelog.md) · [source](https://github.com/pepperonas/brutus)
