<div align="center">

<a href="https://brutus.celox.io"><img src="docs/banner.jpg" alt="Brutus — Weckt jeden. Schläft nie. · brutus.celox.io" width="100%"></a>

# ⏰ Brutus

**English** · [Deutsch](README.de.md)

**The alarm clock you can’t ignore: it only stops once you solve math, shake the phone or scan a QR code — plus world clock, stopwatch, timer and 25 alarm sounds made on the phone.**

<p>
  <a href="https://brutus.celox.io"><img alt="Website: brutus.celox.io" height="56" src="https://img.shields.io/badge/%F0%9F%8C%90_Website-brutus.celox.io-E53935?style=for-the-badge"></a>
  &nbsp;
  <a href="https://brutus.celox.io/download"><img alt="APK" height="56" src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_Download-newest_APK-3DDC84?style=for-the-badge&logo=android&logoColor=white"></a>
</p>

<h3>👉 <a href="https://brutus.celox.io">brutus.celox.io</a> — features, <a href="https://brutus.celox.io/#sounds">all 25 sounds to listen to</a>, FAQ and always the newest APK</h3>

<!-- BADGES:BIG — version, unit tests and lines of code; ReadmeSyncTest keeps them true. -->
[![version](https://img.shields.io/badge/version-2.5.2-E53935?style=for-the-badge&logo=android&logoColor=white)](https://github.com/pepperonas/brutus/releases/latest)
[![unit tests](https://img.shields.io/badge/unit%20tests-376-2E9E5B?style=for-the-badge&logo=junit5&logoColor=white)](#tests-and-ci)
[![lines of code](https://img.shields.io/badge/lines%20of%20code-11.3k-4B6BDF?style=for-the-badge&logo=kotlin&logoColor=white)](app/src/main/java/com/pepperonas/brutus)
[![test code](https://img.shields.io/badge/test%20code-5.6k-2E9E5B?style=for-the-badge&logo=kotlin&logoColor=white)](app/src/test/java/com/pepperonas/brutus)
[![sounds](https://img.shields.io/badge/sounds-25-FF5252?style=for-the-badge&logo=audiomack&logoColor=white)](https://brutus.celox.io/#sounds)

[![Donate with PayPal](https://img.shields.io/badge/PayPal-support%20this%20project-00457C?style=for-the-badge&logo=paypal&logoColor=white)](https://www.paypal.com/donate/?business=martin.pfeffer@celox.io&currency_code=EUR&item_name=Brutus)

</div>

<!-- Project status — the GitHub badges are live and update themselves. -->

[![Tests](https://img.shields.io/github/actions/workflow/status/pepperonas/brutus/tests.yml?branch=main&label=tests&logo=githubactions&logoColor=white)](https://github.com/pepperonas/brutus/actions/workflows/tests.yml)
[![Release build](https://img.shields.io/github/actions/workflow/status/pepperonas/brutus/release.yml?label=release%20build&logo=githubactions&logoColor=white)](https://github.com/pepperonas/brutus/actions/workflows/release.yml)
[![Release](https://img.shields.io/github/v/release/pepperonas/brutus?color=FF5252&logo=github&logoColor=white)](https://github.com/pepperonas/brutus/releases/latest)
[![Release date](https://img.shields.io/github/release-date/pepperonas/brutus?color=FF5252&logo=github&logoColor=white)](https://github.com/pepperonas/brutus/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/pepperonas/brutus/total?label=APK%20downloads&color=success&logo=github&logoColor=white)](https://github.com/pepperonas/brutus/releases)
[![Latest downloads](https://img.shields.io/github/downloads/pepperonas/brutus/latest/total?label=latest%20release&color=success&logo=github&logoColor=white)](https://github.com/pepperonas/brutus/releases/latest)
[![Last commit](https://img.shields.io/github/last-commit/pepperonas/brutus?logo=git&logoColor=white)](https://github.com/pepperonas/brutus/commits/main)
[![Commit activity](https://img.shields.io/github/commit-activity/m/pepperonas/brutus?logo=git&logoColor=white)](https://github.com/pepperonas/brutus/commits/main)
[![Code size](https://img.shields.io/github/languages/code-size/pepperonas/brutus?logo=files&logoColor=white)](#project-structure)
[![Repo size](https://img.shields.io/github/repo-size/pepperonas/brutus?logo=github&logoColor=white)](https://github.com/pepperonas/brutus)
[![Top language](https://img.shields.io/github/languages/top/pepperonas/brutus?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Open issues](https://img.shields.io/github/issues/pepperonas/brutus?logo=github&logoColor=white)](https://github.com/pepperonas/brutus/issues)
[![Stars](https://img.shields.io/github/stars/pepperonas/brutus?logo=github&logoColor=white)](https://github.com/pepperonas/brutus/stargazers)
[![Forks](https://img.shields.io/github/forks/pepperonas/brutus?logo=github&logoColor=white)](https://github.com/pepperonas/brutus/forks)
[![License](https://img.shields.io/github/license/pepperonas/brutus?color=blue)](LICENSE)
[![Made by celox.io](https://img.shields.io/badge/made%20by-celox.io-E53935)](https://celox.io)

<!-- Platform & runtime -->

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26%20%C2%B7%20Android%208.0-3DDC84?logo=android&logoColor=white)](https://apilevels.com)
[![targetSdk](https://img.shields.io/badge/targetSdk-35%20%C2%B7%20Android%2015-3DDC84?logo=android&logoColor=white)](https://apilevels.com)
[![compileSdk](https://img.shields.io/badge/compileSdk-35-3DDC84?logo=android&logoColor=white)](https://developer.android.com/tools/releases/platforms)
[![JDK](https://img.shields.io/badge/JDK-17-437291?logo=openjdk&logoColor=white)](https://adoptium.net)
[![APK size](https://img.shields.io/badge/APK-4.4%20MB-blueviolet?logo=android&logoColor=white)](https://github.com/pepperonas/brutus/releases/latest)

<!-- Language, build & toolchain -->

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Coroutines](https://img.shields.io/badge/Coroutines-StateFlow-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/coroutines-overview.html)
[![Gradle](https://img.shields.io/badge/Gradle-8.11.1-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![AGP](https://img.shields.io/badge/AGP-8.7.3-02303A?logo=androidstudio&logoColor=white)](https://developer.android.com/build)
[![KSP](https://img.shields.io/badge/KSP-2.1.0--1.0.29-7F52FF?logo=kotlin&logoColor=white)](https://github.com/google/ksp)
[![R8](https://img.shields.io/badge/R8-minify%20%2B%20shrink-02303A?logo=android&logoColor=white)](https://developer.android.com/build/shrink-code)

<!-- UI layer -->

[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.06.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Expressive-6750A4?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![material3](https://img.shields.io/badge/material3-1.5.0--alpha18-6750A4?logo=materialdesign&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/compose-material3)
[![Navigation](https://img.shields.io/badge/Navigation%20Compose-2.8.5-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/navigation)
[![Icons](https://img.shields.io/badge/Icons-Material%20Extended-6750A4?logo=materialdesign&logoColor=white)](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary)
[![Type](https://img.shields.io/badge/Type-Space%20Grotesk%20%C2%B7%20OFL-FF5252?logo=googlefonts&logoColor=white)](THIRD_PARTY_LICENSES/SpaceGrotesk-OFL.txt)
[![Languages](https://img.shields.io/badge/Languages-English%20%C2%B7%20Deutsch-4285F4?logo=googletranslate&logoColor=white)](#languages)

<!-- Data & device APIs -->

[![Room](https://img.shields.io/badge/Room-2.6.1%20%C2%B7%20schema%20v7-FF6F00?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![DataStore](https://img.shields.io/badge/DataStore-1.1.1-FF6F00?logo=android&logoColor=white)](https://developer.android.com/topic/libraries/architecture/datastore)
[![Lifecycle](https://img.shields.io/badge/Lifecycle-2.8.7-FF6F00?logo=android&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/lifecycle)
[![CameraX](https://img.shields.io/badge/CameraX-1.4.1-00BCD4?logo=android&logoColor=white)](https://developer.android.com/training/camerax)
[![ML Kit](https://img.shields.io/badge/ML%20Kit%20Barcode-18.3.1%20unbundled-EA4335?logo=google&logoColor=white)](https://developers.google.com/ml-kit/vision/barcode-scanning)
[![ZXing](https://img.shields.io/badge/ZXing-3.5.3-000000)](https://github.com/zxing/zxing)
[![AlarmManager](https://img.shields.io/badge/AlarmManager-setAlarmClock-3DDC84?logo=android&logoColor=white)](https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock)
[![Audio](https://img.shields.io/badge/Audio-AudioTrack%20PCM-3DDC84?logo=android&logoColor=white)](https://developer.android.com/reference/android/media/AudioTrack)

<!-- Engineering practice -->

[![Architecture](https://img.shields.io/badge/Architecture-MVVM-795548)](#project-structure)
[![JUnit](https://img.shields.io/badge/JUnit-4.13.2-25A162)](https://junit.org/junit4/)
[![Robolectric](https://img.shields.io/badge/Robolectric-4.14.1-25A162)](https://robolectric.org)
[![Coroutines Test](https://img.shields.io/badge/coroutines--test-1.9.0-25A162?logo=kotlin&logoColor=white)](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
[![Conventional Commits](https://img.shields.io/badge/Commits-Conventional-FE5196?logo=conventionalcommits&logoColor=white)](https://www.conventionalcommits.org)
[![SemVer](https://img.shields.io/badge/SemVer-2.0.0-303030?logo=semver&logoColor=white)](https://semver.org)
[![Changelog](https://img.shields.io/badge/Changelog-Keep%20a%20Changelog-E05735?logo=keepachangelog&logoColor=white)](CHANGELOG.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](http://makeapullrequest.com)

<!-- What makes it hard to sleep through -->

[![Challenges](https://img.shields.io/badge/challenges-Math%20%C2%B7%20Shake%20%C2%B7%20QR-E53935)](#combinable-wake-up-challenges)
[![Sounds](https://img.shields.io/badge/sounds-25%20synthesized%20on%20device-FF5252?logo=audiomack&logoColor=white)](#alarm-sounds)
[![Hardcore](https://img.shields.io/badge/Hardcore-volume%20locked-B71C1C)](#hardcore-mode)
[![Ultra Hardcore](https://img.shields.io/badge/Ultra%20Hardcore-re--alarms%20%2B%2030%20steps-B71C1C)](#ultra-hardcore-mode)
[![Sunrise](https://img.shields.io/badge/Sunrise-10%20min%20light%20%2B%20sound-FFB74D)](#sunrise-pre-alarm)
[![Direct boot](https://img.shields.io/badge/direct%20boot-rings%20before%20unlock-3DDC84?logo=android&logoColor=white)](#reliability)
[![Signed](https://img.shields.io/badge/APK-certificate%20pinned%20in%20CI-2E7D32?logo=githubactions&logoColor=white)](#release-signing)
[![Product page](https://img.shields.io/badge/product%20page-5%20languages-E53935?logo=googlechrome&logoColor=white)](https://brutus.celox.io)

<!-- What Brutus deliberately does not do -->

[![Offline](https://img.shields.io/badge/Offline-first-2E7D32)](#permissions)
[![Network: opt-in](https://img.shields.io/badge/network-opt--in%20update%20check%20only-2E7D32)](#update-notice)
[![No trackers](https://img.shields.io/badge/Trackers-none-2E7D32)](#permissions)
[![No ads](https://img.shields.io/badge/Ads-none-2E7D32)](#permissions)
[![No account](https://img.shields.io/badge/Account-not%20required-2E7D32)](#permissions)

> **The alarm clock that makes sure you actually wake up.**

Brutus is a complete Android clock suite — **Alarm · World Clock · Stopwatch · Timer** — with one mission hiding behind the polished **Material 3 Expressive** UI (tonal surfaces, spatial springs, wavy progress, Space Grotesk display type, dark/light + optional Material You): its alarm module forces you to complete a configurable challenge (or a chain of challenges) before ringing stops. No cheating, no auto-dismissing, no snoozing your way back to sleep.

Everything is packed into a four-tab bottom navigation that keeps the brutal alarm engine one tap away while still giving you a proper clock app for everyday use.

---

## Table of Contents

- [Screenshots](#screenshots)
- [New in 2.5](#new-in-25)
- [Why Brutus?](#why-brutus)
- [App structure](#app-structure)
- [Features](#features)
  - [Combinable wake-up challenges](#combinable-wake-up-challenges)
  - [Difficulty + sensitivity presets](#difficulty--sensitivity-presets)
  - [Alarm sounds](#alarm-sounds)
  - [Hardcore Mode](#hardcore-mode)
  - [Ultra Hardcore Mode](#ultra-hardcore-mode)
  - [Sunrise pre-alarm](#sunrise-pre-alarm)
  - [Home-screen widget](#home-screen-widget)
  - [Reliability banners](#reliability-banners)
  - [Update notice](#update-notice)
  - [Global QR code](#global-qr-code)
  - [Slide-to-snooze gesture](#slide-to-snooze-gesture)
  - [Test mode](#test-mode)
  - [World Clock](#world-clock)
  - [Stopwatch](#stopwatch)
  - [Timer](#timer)
  - [Settings & info](#settings--info)
  - [Notifications](#notifications)
  - [Motion and physics](#motion-and-physics)
  - [Theming and Material You](#theming-and-material-you)
  - [Languages](#languages)
  - [Scheduling](#scheduling)
  - [Lock-screen overlay](#lock-screen-overlay)
  - [Reliability](#reliability)
- [Install](#install)
- [Permissions](#permissions)
- [Build from source](#build-from-source)
- [Release signing](#release-signing)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Tests and CI](#tests-and-ci)
- [How it works](#how-it-works)
- [Design philosophy](#design-philosophy)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Developer](#developer)
- [Donate](#donate)
- [License](#license)

---

## Screenshots

<img src="docs/screenshots/mockups.jpg" alt="The alarm list, the sound picker, a ringing math challenge, the Sunrise pre-alarm and Settings & info" width="100%">

<sub>Captured on **v2.5.x**, dark theme, brand colours (Material You off), English UI. Regenerate the strip from the raw captures with
`python3 tools/mockups.py docs/screenshots/mockups.jpg docs/screenshots/01-alarm-list.png:"Alarms" …` — the shot list is in
[`docs/screenshots/SHOTLIST.md`](docs/screenshots/SHOTLIST.md). **Hear the sounds on the [product page](https://brutus.celox.io/#sounds).**</sub>

<table>
  <tr>
    <td width="33%" valign="top">
      <img src="docs/screenshots/01-alarm-list.png" alt="Alarm list" width="100%" />
      <br /><sub><b>Alarms</b> — countdown header, the next alarm outlined, weekday strip, mode · challenge · snooze · sound chips.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/02-sound-picker.png" alt="Sound picker in the edit sheet" width="100%" />
      <br /><sub><b>Sound picker</b> — all 25 synthesized sounds as chips; tapping one previews it.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/03-math-challenge.png" alt="Ringing alarm with a math challenge" width="100%" />
      <br /><sub><b>Ringing</b> — Ultra Hardcore alarm, challenge 1 of 2, keypad and slide-to-snooze.</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" valign="top">
      <img src="docs/screenshots/04-shake-challenge.png" alt="Shake challenge" width="100%" />
      <br /><sub><b>Shake</b> — wavy progress ring, 17 of 40 shakes.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/05-sunrise.png" alt="Sunrise pre-alarm" width="100%" />
      <br /><sub><b>Sunrise</b> — the screen warms up over the ten minutes before the alarm, a gentle sound swells.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/06-settings.png" alt="Settings & info" width="100%" />
      <br /><sub><b>Settings & info</b> — theme, Material You, the Sunrise sound, notifications and About.</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" valign="top">
      <img src="docs/screenshots/07-timer-setup.png" alt="Timer setup" width="100%" />
      <br /><sub><b>Timer</b> — presets and the gentle end tone; Start stays in reach.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/08-timer.png" alt="Running timer" width="100%" />
      <br /><sub><b>Running timer</b> — a wavy ring that drains continuously; it rings even with the app closed.</sub>
    </td>
    <td width="33%" valign="top">
      <img src="docs/screenshots/09-world-clock.png" alt="World clock" width="100%" />
      <br /><sub><b>World clock</b> — day and night per zone, UTC offset, date.</sub>
    </td>
  </tr>
</table>

<p align="center"><img src="docs/screenshots/10-stopwatch.png" alt="Stopwatch with laps" width="32%" /><br /><sub><b>Stopwatch</b> — running readout and a lap list with lap and total times.</sub></p>

## New in 2.5

- **25 alarm sounds, chosen by ear** — nine new harsh ones (air-raid siren, dive alarm, car alarm, school bell,
  reverse beeper, Shepard siren, steel hammer, strobe, evacuation whoop) and eight gentle ones built to loop
  seamlessly for Sunrise and the timer. [Listen to all of them](https://brutus.celox.io/#sounds).
- **Pick the Sunrise sound** under ⋮ → Settings & info — any gentle sound, or silence for light only.
- **Removed sounds keep ringing:** alarms saved with Siren, Chime, Marimba or Morning sun play a successor of
  the same character instead of the system tone.
- **2.5.1:** the Timer's Start button stays visible (the longer tone list had squeezed it), and a cold start no
  longer flashes “No alarms yet” before the list has loaded.
- **2.5.2:** on an unlocked phone the pinned alarm notification no longer covers the alarm screen's clock —
  it gives way while the screen is in front and comes back when you leave it.
- Everything in detail: [CHANGELOG](CHANGELOG.md).

## Why Brutus?

Stock Android alarms are polite. They ring, you tap _Dismiss_ with your eyes closed, and you're back asleep in seconds. Brutus breaks that loop on purpose.

- **No dismiss button up front** — the big "Stop" button only appears after you've completed every challenge you selected for that alarm
- **No silent mode escape** — Brutus overrides `STREAM_ALARM` to maximum and ignores Do Not Disturb via `USAGE_ALARM` audio attributes
- **No snoozing without effort** — the snooze button is a slide-to-unlock gesture, not a tap
- **No workarounds after power loss** — alarms are persisted in Room and re-registered on boot via a `BOOT_COMPLETED` receiver
- **No cheating with a printed screenshot of your QR code in bed** — you can put the QR far from your bed (like the bathroom mirror) and you must physically walk there to scan it

---

## App structure

Starting with v1.2.0 Brutus ships as a full clock suite. A persistent bottom navigation bar offers four tabs:

| Tab | Icon | Purpose |
|-----|------|---------|
| **Alarm** | ⏰ | Expressive alarm list with countdown header, state-coded cards (enabled = muted red, disabled = gray; next alarm ringed by a primary outline), swipe-to-delete + undo, per-alarm edit sheet, all the brutal wake modes |
| **World Clock** | 🌐 | Live multi-time-zone board powered by `java.time.ZoneId` — add/remove cities, ticks every second |
| **Stopwatch** | ⏱ | Start / Stop / Lap stopwatch with centisecond precision via `SystemClock.elapsedRealtime()` |
| **Timer** | ⌛ | HMS-picker countdown timer with quick presets (1m, 3m, 5m, 10m, 15m, 30m) — rings system alarm tone on finish |

The Alarm tab stays the heart of the app: its cards show a large thin time reading, the repeat summary (_Once_ / _Every day_ / explicit day list), an optional label, and a toggle switch on the right. Below sits a **full-width weekday strip** — seven equal `Mon Tue Wed Thu Fri Sat Sun` pills that always render on a single line, active days filled bright red. A row of **info chips** underneath surfaces the mode (`ULTRA HC` / `HARDCORE`), `☀ Sunrise`, the challenge (`Math + Shake`…), the snooze interval (`Snooze 5m`) and the `♪` sound at a glance _(redesigned in v1.7.0)_. A countdown header (e.g. _"Alarm in 13 hours, 29 minutes"_) sits above the list and refreshes every 30 s.

Since **v1.9.0** every card also has a **copy button** (⧉): it opens the edit sheet prefilled with all of the source alarm's settings ("Copy alarm") — adjust the time, save, done; no confusing identical duplicate is created behind your back. And every delete — single or _Delete all_ — shows an **undo snackbar** ("Undo") that restores the alarm(s) including their scheduling.

A premium **monogram app icon** (radial dark-red gradient + gradient-filled "B" with hairline highlight) replaces the previous alarm-bell icon.

---

## Features

### Combinable wake-up challenges

Three independent challenge types. Enable one, two, or all three per alarm. Brutus runs them in sequence — you have to complete every one of them before the "Stop alarm" button appears.

| Mode | Description | Configurable |
|------|-------------|--------------|
| **Math** | Solve randomly generated problems (multiplication, addition, subtraction) with numeric answers | Count: **1–10 problems** (default 3) |
| **Shake** | Shake the phone with a circular progress ring visualizing how many shakes are left | Count: **10–100 shakes**, step 5 (default 30) |
| **QR scan** | Scan a specific QR code using ML Kit Barcode Scanning via CameraX | Uses the global QR code — see below |

Combination examples:
- **Easy**: Math (3 problems) only
- **Medium**: Math (5) + Shake (50)
- **Brutus Mode**: Shake (100) → Math (10) → QR scan across the house

### Difficulty + sensitivity presets

Both Math and Shake now ship with a 3-step preset selector that appears inline in the edit dialog when the respective challenge is enabled.

| Math difficulty | Operator pool | Operand range |
|-----------------|---------------|---------------|
| **Easy** | `+`, `-` | 1–20 (subtraction always ≥ 0) |
| **Hard** _(default)_ | `+`, `-`, `*` | up to 50 × 20 or three-digit add/sub |
| **Brutal** | `+`, `-`, `*` (biased) | two-digit × two-digit, four-digit sums |

| Shake sensitivity | Accelerometer delta threshold (m/s²) |
|-------------------|--------------------------------------|
| **Sensitive** | ≥ 9 — even light wrist flicks count |
| **Normal** _(default)_ | ≥ 12 — the previous behavior |
| **Strong** | ≥ 16 — only deliberate, vigorous shakes register |

Settings are per-alarm and persist in the same Room row.

### Alarm sounds

**25 synthesized sounds** plus the system alarm tone and silence — **hear every one on the [product page](https://brutus.celox.io/#sounds)**. Each is generated on-device in real time using `AudioTrack` with `USAGE_ALARM` and `CONTENT_TYPE_SONIFICATION` attributes: no audio files, a tiny APK, seamless loops. The v2.5.0 set was chosen by ear from twenty candidates rendered by the very code the app runs.

**Harsh sounds** — built for wake-the-dead alarm duty:

| Sound | Character | Signal |
|-------|-----------|--------|
| **Silent** | No audio — useful for rehearsing wake modes quietly | — |
| **System alarm** | Android default alarm (fallback) | `RingtoneManager.TYPE_ALARM` |
| **Klaxon** | Pulsing two-tone alarm | 600/900 Hz square wave, 300 ms each |
| **Nuclear Alert** | Rapid sharp beeping | 1 kHz square, 100 ms on / 100 ms off |
| **Piercing** | Piercing continuous beep | 3.5 kHz square wave with 8 Hz pulse — the most annoying one by design |
| **Stadium horn** _(v1.7.0)_ | Brash stadium air-horn blat | Three detuned sawtooth voices (Bb3 / ~Eb4 / Bb4) stacked, 0.9 s |
| **Jackhammer** _(v1.7.0)_ | Pounding construction-site rattle | ~73 Hz square gated 28 ms on / 22 ms off, with a clattering 5th-harmonic grit |
| **Fire alarm** _(v1.7.0)_ | Standardized T-3 smoke-alarm cadence | 3.1 kHz square, three 0.5 s beeps + 1.5 s pause, looped |
| **Dental drill** _(v1.7.0)_ | Screeching dental drill | 1.6 kHz FM carrier, 42 Hz modulator (index 9) with a slow ±220 Hz wail |
| **Banshee** _(v1.7.0)_ | Dissonant rising wail | Four tightly-detuned voices (620–652 Hz) beating while the cluster sweeps +90 % up |
| **Air-raid siren** _(v2.5.0)_ | Motor siren winding up, holding, winding down | 8-harmonic sawtooth gliding 160 → 720 Hz with a 5.5 Hz wobble, soft-clipped, 6 s |
| **Dive alarm** _(v2.5.0)_ | Submarine "A-OO-GA" | Square-ish horn gliding 190 → 460 → 410 Hz, 1.4 s |
| **Car alarm** _(v2.5.0)_ | Four patterns, 1.5 s each | Wail, yelp, gated 420 Hz horn, 850/1150 Hz warble |
| **School bell** _(v2.5.0)_ | Mechanical clapper | 22 strikes/s exciting four inharmonic bell partials around 1.48 kHz |
| **Reverse beeper** _(v2.5.0)_ | Truck warning beep that keeps accelerating | 1.04 kHz saturated square, 2 → 12 beeps/s over 4 s |
| **Shepard siren** _(v2.5.0)_ | Tone that seems to rise forever | Eight octave-spaced voices with a Gaussian envelope, one octave per 6 s loop |
| **Steel hammer** _(v2.5.0)_ | Irregular blows on metal | Noise transients + five inharmonic partials, eight hits per 3 s |
| **Strobe** _(v2.5.0)_ | High beeps, faster **and** higher | 4 → 30 beeps/s while the pitch climbs 1.5 → 4 kHz |
| **Evacuation whoop** _(v2.5.0)_ | Industrial "whoop" | Upward sweep 380 → 1480 Hz in 0.8 s, hard restart |

**Gentle sounds** _(v2.5.0)_ — for the timer and the Sunrise pre-alarm, capped at 60 % amplitude and built to be **exactly periodic**: decaying notes are rendered circularly (their tail wraps round to the start of the loop) and sustained tones use frequencies with a whole number of cycles per loop, so ten minutes of Sunrise never click at the seam (`gentle sounds loop without a click` pins it).

| Sound | Character | Signal |
|-------|-----------|--------|
| **Singing bowl** | Deep, long-ringing bowl with a gentle beat | 196 Hz + a 0.9 Hz-detuned twin + partials at ×2.71/×5.1, 6 s |
| **Birdsong** | Quiet chirps, loosely spread | 2.8–4.2 kHz chirps with vibrato in groups of two to four (fixed seed) |
| **Wind chimes** | Pentatonic bells set off at random | C6–C7 pentatonic, bell partial ×2.76, 1.3 s decay |
| **Kalimba** | Thumb-piano motif | Eight notes in G, tine partial ×5.4 |
| **Harp** | Rising arpeggios | Cmaj7 and Fmaj7 over two octaves |
| **Ocean waves** | A wave rolling in and back | Two-pole low-passed noise, cutoff and level swelling over 6 s |
| **Electric piano** | Calm Rhodes chords | FM with a decaying index over Fmaj7 · Em7 · Dm7 · Cmaj7 |
| **Daybreak** | A pad that breathes and brightens | D-major pad whose upper partials open and close once per loop |

Choosing **Silent** skips the audio path entirely; vibration still runs so the alarm is noticeable if you need it.

**Removed in v2.5.0:** Siren, Chime, Marimba and Morning sun. Their ids are **retired, never reused**, and map to a successor of the same character (`AlarmSound.RETIRED`): Siren → Air-raid siren, Chime → Wind chimes, Marimba → Kalimba, Morning sun → Daybreak. An alarm, timer or Sunrise setting saved with one of them keeps ringing with its successor instead of falling back to the system tone.

Sound preview works directly inside the edit dialog — tap a chip to hear it, tap _Stop preview_ when you're done. The Timer screen has its own sound picker (gentle sounds only) — defaults to **Wind chimes**. The Sunrise sound is picked under ⋮ → **Settings & info** (default **Daybreak**, or silence for light only). To audition sounds on a computer: `BRUTUS_SOUND_EXPORT=~/Desktop/Brutus-Sounds ./gradlew :app:testDebugUnitTest --tests '*SoundExportTest'` writes every sound as WAV plus an overview page; `scripts/sound-previews.sh` rebuilds the product page's previews.

### Hardcore Mode

An opt-in per-alarm switch that makes the alarm immune to volume tampering. When a **Hardcore-Mode alarm is ringing**, Brutus:

1. Clamps `STREAM_ALARM` to its maximum value and keeps it there
2. Registers a `VOLUME_CHANGED_ACTION` receiver that snaps any user-driven volume change back to max within a few milliseconds
3. Overrides `dispatchKeyEvent()` in both `AlarmActivity` and `TestAlarmActivity` to **consume volume-up / volume-down / mute key events** — the hardware buttons effectively become inert

The `HARDCORE` tag is shown on the alarm card in the list, and a red `HARDCORE MODE` badge flashes above the clock while the alarm screen is visible. Hardcore is never enabled by default — toggle it per alarm inside the edit sheet.

The guard is strictly scoped to the ringing window: as soon as the alarm is dismissed or snoozed, the receiver detaches and Android's normal volume behavior resumes. Outside of a firing alarm, the hardware volume keys behave normally, so the setting has zero footprint during daily use.

> Android intentionally offers no API to globally "lock" a stream volume. Brutus achieves the locked-feel via immediate re-clamping + key event consumption — the cleanest approach available without requiring system-level permissions.

### Ultra Hardcore Mode

Hardcore Mode keeps you from silencing a ringing alarm. **Ultra Hardcore Mode** (v1.4.0) keeps you from going _back_ to sleep after you dismiss it.

When enabled per alarm:

1. **Ultra Hardcore implies Hardcore.** The volume lock + volume-key consumption apply automatically while either the main alarm or a follow-up is ringing.
2. **As soon as you dismiss the main alarm, Brutus schedules two follow-up alarms** via `AlarmManager.setAlarmClock()` — one at **+10 minutes** from dismiss, another at **+15 minutes**. Both run the same challenge chain, the same sound, and the same Hardcore guard.
3. **A persistent reminder notification** is posted from a dedicated `IMPORTANCE_HIGH` / bypass-DND channel. Title: _"Ultra Hardcore active"_. It has a `Solve task` action — tapping it opens the **anti-snooze task**.
4. **The anti-snooze task** is a step-counter challenge: walk **30 steps** (configurable per install) with the phone in your hand or pocket. Uses `Sensor.TYPE_STEP_COUNTER` when available, falls back to `TYPE_STEP_DETECTOR`, and finally to an accelerometer impulse heuristic on older hardware that lacks a pedometer.
5. **Completing the task cancels both pending follow-ups** and clears the reminder notification. Cancelling _without_ completing it leaves both follow-ups armed — Brutus will ring again.
6. **Reboot survives.** Pending follow-ups are mirrored to `SharedPreferences` (`UltraHardcoreStore`). After `BOOT_COMPLETED`, any follow-up whose trigger time is still in the future is re-registered with `AlarmManager`; expired ones are cleaned out.

The alarm screen shows a brighter **`ULTRA HARDCORE MODE`** badge instead of the regular Hardcore one, and the follow-up firings display _"Re-alarm 1/2 — you did not get away"_ above the clock. Cards in the list carry an orange **`ULTRA HC`** tag.

Requires the **`ACTIVITY_RECOGNITION`** runtime permission (API 29+) for the step counter. If the user denies it, the step challenge degrades to the accelerometer fallback automatically — no Ultra Hardcore alarm ever locks the user out.

> Ultra Hardcore can be disabled per alarm at any time. Toggling it off in the edit dialog also cancels any currently-armed follow-ups and dismisses the notification immediately.

### Sunrise pre-alarm

A per-alarm opt-in (v1.6.0) that gives you a 10-minute gentle wake-up window _before_ the main alarm starts. When enabled:

- A separate `setExactAndAllowWhileIdle` registration fires 10 min before the main trigger and launches `SunriseActivity` on top of the lock screen.
- The activity ramps the **screen brightness** linearly from ~5 % to 100 % and the background gradient shifts from black to dawn-orange.
- The gentle sound chosen under ⋮ → **Settings & info** loops softly (default **Daybreak**, or silence for light only) — no max-volume override, no Hardcore guard. Just an ambient cue.
- The clock continues to tick centered on the screen with a live countdown to the main alarm.
- Two buttons: **Stop alarm** (disables the alarm entirely, same effect as toggling it off in the list) and **Already awake — close sunrise** (closes the pre-alarm; main alarm still fires at the configured time).
- Sunrise has _no_ challenge requirements and _no_ Hardcore behavior. The brutal alarm path takes over exactly at the configured time regardless of whether the Sunrise activity is still open.

Sunrise is intentionally lightweight (~70 lines of Compose, no schema work beyond a single `sunriseEnabled` column on `AlarmEntity` v6→v7) so it can be layered on top of any challenge / Hardcore / Ultra Hardcore combo.

### Home-screen widget

A 2×1 cell widget (resizable horizontally / vertically) added in v1.6.0. Shows:

- **Time** of the next upcoming alarm (large, light-weight)
- **Countdown** — "in 7h 12m" / "in 23 min" / "in 2 days" (localized)
- **Day strip** — repeat-day shorthand for repeating alarms, or the weekday name for one-shot alarms
- A small **BRUTUS** marker in the brand red

Tapping the time opens the app. Updates every 30 minutes via `AppWidgetProvider.updatePeriodMillis`, plus an immediate `ACTION_APPWIDGET_UPDATE` broadcast on every alarm add / toggle / delete / fire so the widget never lags behind by more than a few seconds during user interaction. Boot recovery refreshes it too.

The widget reads from the same Room database the app uses, so widgets always agree with the in-app countdown.

### Reliability banners

The alarm list shows _two_ red/orange banners when system state would silently break alarms:

1. **Exact alarms disabled** _(v1.3.0)_ — `AlarmManager.canScheduleExactAlarms()` is false (Samsung's default on Android 12+). Deep-links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
2. **Battery optimization active** _(v1.6.0)_ — `PowerManager.isIgnoringBatteryOptimizations()` is false (default on every install). Aggressive battery managers on Xiaomi/Huawei/Samsung devices routinely kill background apps and silently swallow alarm broadcasts. Deep-links to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` so the user can whitelist Brutus with two taps. Falls back to the general battery-optimization list if the per-app dialog isn't supported.
3. **Full-screen alarm blocked** _(v1.6.1)_ — Android 14+ no longer grants `USE_FULL_SCREEN_INTENT` by default to apps outside the Calling / Default Alarm categories. Without it, Brutus's lock-screen overlay is silently downgraded to a heads-up notification and the app does _not_ pop to the foreground when the alarm fires. The banner uses `NotificationManager.canUseFullScreenIntent()` and deep-links to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` so the user can flip the switch.

All three banners disappear automatically as soon as the corresponding system state is fixed — re-checks run on every `ON_RESUME`.

### Update notice

Opt-in since v2.3.0: **⋮ → Settings & info → Check for updates** (off by default, also for existing installs; until v2.4.0 the switch sat directly in the ⋮ menu).

- Switched **on**, Brutus asks once right away and then once a day — WorkManager, only with a
  network — for the newest version: `GET https://brutus.celox.io/latest.json`, falling back to
  `api.github.com/repos/pepperonas/brutus/releases/latest` when the product page is unreachable.
  A plain request, no identifiers, nothing else is sent.
- A newer version posts **one** notification per version on its own *App updates* channel (default
  importance, never through Do Not Disturb) and shows a banner above the alarm list until it is
  installed. Both open [brutus.celox.io/download](https://brutus.celox.io/download).
- Switched **off**, all scheduled checks are cancelled — Brutus makes no network request at all.

Brutus does not download or install anything by itself; you install the new APK over the old one.

### Global QR code

Brutus generates **one unique QR code** per installation, stored once in `SharedPreferences`, valid for every alarm forever. You never need to regenerate it. Workflow:

1. Create any alarm with the QR challenge enabled
2. The QR code is displayed inside the edit dialog
3. Tap **Save as PNG** — writes a 1024×1024 PNG to `Pictures/Brutus/` via MediaStore (Android 10+) or legacy storage (Android 8–9, with permission). The file is visible in any gallery app
4. Tap **Share** — sends the PNG through the Android share sheet (Gmail, WhatsApp, Bluetooth, etc.) via FileProvider
5. Print it, tape it to your bathroom mirror / fridge / front door — the farther from bed, the better

The code format is `brutus:{UUIDv4}`, ~43 characters. Any ML Kit-compatible QR scanner can read it, but only Brutus's own scanner verifies the match.

### Slide-to-snooze gesture

Snoozing is available during every phase of the alarm (including while a challenge is running) — but only by swiping an orange thumb horizontally across a track past the 85% threshold. A tap does nothing. Features:

- Animated gradient fill behind the thumb grows with drag progress
- Pulsing _"Swipe to snooze"_ hint with a drifting chevron icon, fades out as you drag
- Springs back on incomplete swipe (damping 0.55)
- Snaps to the end on successful trigger, then fires the snooze
- Snooze duration configurable per alarm: **Off, 2, 5, 10, or 15 minutes** (default 5). Set to _Off_ to hide the snooze button entirely on the alarm screen — no escape except finishing the challenge

### Test mode

Every edit dialog has a **Test wake modes now** button. It opens the full alarm screen with your chosen sound and challenge chain — but without registering a real alarm, without max-volume override, and without lock-screen flags. Finish the test or just back out. Useful for:

- Checking how many math problems actually feels right
- Calibrating shake threshold to your phone's accelerometer
- Confirming you can scan your printed QR in realistic lighting
- Auditioning alarm sounds in context

### World Clock

A live time-zone board powered by `java.time.ZoneId` and `ZonedDateTime`. Each row shows the city (derived from the IANA zone ID), the region, the UTC offset, the local date, and the current time — all refreshed once per second. Add-sheet offers a searchable list of the ~600 available zone IDs on the device. Selection persists across launches via `SharedPreferences` (newline-separated zone IDs).

Default seeded set on first launch: **Europe/Berlin**, **America/New_York**, **Asia/Tokyo**. All removable, all replaceable.

### Stopwatch

Centisecond-precision stopwatch built on `SystemClock.elapsedRealtime()` (unaffected by wall-clock jumps). A single large monospace-width readout (using the Material 3 Light weight for elegance) and an expressive button group — **Reset / Lap** and **Start / Stop**, whose labels roll to their new word. Laps are persisted in-memory during the session and shown as a list with per-lap and cumulative columns. Lap button becomes available automatically while the timer is running.

Since **v1.8.0** the stopwatch (and the timer) keep their entire state — including a running measurement, laps, and a timer's finish sound — in Activity-scoped ViewModels, so switching bottom-nav tabs no longer resets them.

### Timer

HMS picker (hours 0–23, minutes 0–59, seconds 0–59) with up/down steppers on each column. Quick-preset row for common durations (1m, 3m, 5m, 10m, 15m, 30m). A **gentle-sound picker** (added in v1.5.0) below the presets lets you pick the finish tone — defaults to **Wind chimes**, choice persists across launches via `TimerSoundStore`. Tapping a chip previews the sound; **Stop** halts the preview.

During the countdown the screen switches to a large 56 sp readout in a continuously draining wavy ring and a button group (**Abort / Pause-Resume**). When the timer expires the chosen synthesized sound (or the system ringtone if **System alarm** is picked) plays in a loop with `USAGE_ALARM` audio attributes until **Stop** is pressed — behavior mirrors a classic kitchen timer rather than a brutal wake mode.

Since **v2.4.0 the timer is a real alarm**: its state is persisted (`timer/TimerController`), AlarmManager wakes the phone at the end and a foreground service rings — also when the app was left with Back or its process was killed (before, it silently ended). A notification shows the countdown with Pause/Resume/Abort; the ringing one has **Stop**. Verified on an emulator: timer started, app left, process killed, it rang on time.

### Settings & info

Since **v2.4.0**: **⋮ → Settings & info**, built like Flipper the Ripper's settings — a section title
in the primary colour over a rounded card.

- **Appearance** — theme **System / Light / Dark** (the alarm screens stay dark), **Material You colors**
  (Android 12+).
- **Sunrise** _(v2.5.0)_ — the sound of the Sunrise pre-alarm: one of the eight gentle sounds or silence
  (light only), default **Daybreak**; tapping a chip previews it.
- **Notifications** — how long before an alarm the heads-up appears (**Off / 30 / 60 / 120 min**), and the
  opt-in **update check**.
- **About Brutus** — the app mark, version and build, "Made by Martin Pfeffer", chips for the
  [website](https://brutus.celox.io), celox.io, the source code and the MIT licence, the **Space Grotesk
  font licence (OFL 1.1)** in a dialog, and a PayPal donate button that springs under the finger.

### Notifications

| Notification | When | What it offers |
|---|---|---|
| **Heads-up** | the chosen lead before an alarm (default 60 min) | countdown to the alarm; **Dismiss early** skips this one occurrence — for **Hardcore alarms only after solving the alarm's own challenges** (silent, no snooze) |
| **Snoozed** | while an alarm is snoozed | countdown to when it rings again; **Cancel snooze** (not for Hardcore alarms) |
| **Ultra Hardcore** | while follow-ups are armed | countdown to the next re-alarm; opens the step task |
| **Missed alarm** | after a reboot or clock jump, if an alarm was due while the phone was off | the time it was due |
| **Timer** | while the timer runs | countdown with **Pause / Resume / Abort**; when it ends, a ringing notification with **Stop** |

A skipped occurrence is remembered, so a reboot cannot bring it back; the list header and the widget show
the occurrence after it. The heads-up and the snooze countdown use a quiet channel (*Upcoming alarms*,
low importance), missed alarms their own (*Missed alarms*); none of them breaks through Do Not Disturb.

### Motion and physics

Brutus runs on `MaterialExpressiveTheme` with `MotionScheme.expressive()`, and since v2.4.0 every
animation takes its spec from `MaterialTheme.motionScheme` instead of hand-picked tweens.

- **Snooze thumb** — dragged with `draggable`, released onto a spring that carries the finger's
  **fling velocity**; a haptic tick at the 85 % point of no return. Only the position decides — a quick
  flick never snoozes by accident.
- **Swipe to delete** — the red deepens and the bin grows with the drag, jumps once more (and ticks) when
  letting go would delete.
- **Button groups** (`ButtonGroup` + `animateWidth`) on Timer and Stopwatch: the pressed button widens, its
  neighbour yields; labels roll to their new word (Start → Stop, Pause → Resume).
- **Numbers roll** like a mechanical counter in every stepper; the shake counter pops on each shake; a
  wrong math answer shakes the readout on a lightly damped spring with a warning buzz.
- **Transitions instead of jumps** — bottom sheets slide out on save, banners slide in and out, list ↔ empty
  state crossfades, the "Done!" moment and the stop button spring in, the challenge dots swell.
- **Loading and progress** — `LoadingIndicator` while the camera starts, `LinearWavyProgressIndicator` for
  the Sunrise lead-in, a continuously draining timer ring.
- **Cheap to draw** — the breathing alarm background and the snooze hint pulse are read in draw/layer
  lambdas, so they repaint without recomposing the screen every frame.
- **Reduced motion** is re-read every time the app comes back; with animations off, decorative loops stop
  and tab changes fade instead of sliding.

### Theming and Material You

The whole app runs on `MaterialExpressiveTheme` with `MotionScheme.expressive()` — the tonal
surfaces, spatial springs and shape scale come from one place (`ui/theme/`), not from
per-screen styling.

- **Dark / light follows the system setting.** The three alarm-facing activities (ring, Sunrise,
  Ultra Hardcore task) deliberately pass `darkTheme = true` regardless — their layered black/red
  gradients assume light-on-dark content, and a white flash at 6 a.m. is its own kind of cruelty.
- **Material You is an opt-in, not the default.** **⋮ → Settings & info** → *Material You colors*
  swaps the red brand scheme for the wallpaper-derived one. Shown only on API 31+, persisted in
  DataStore, applied instantly across every screen.
- **Space Grotesk** carries the display scale, with **tabular numerals on the entire type scale**
  since v2.1.0 — every clock, countdown and lap time ticks without the digits jittering sideways.
- **Reduced motion is respected.** `rememberReducedMotion()` reads `ANIMATOR_DURATION_SCALE` and
  gates the decorative loops (breathing backgrounds, pulsing snooze hint); state-driven
  transitions simply snap instead.

### Languages

Brutus ships in **English and German**. English is the default resource set
(`values/`), German is a full translation (`values-de/`) — there is no partial
coverage and no silent fallback: every single string, plural and weekday label
exists in both.

- **Follows the system language** out of the box.
- **Per-app language on Android 13+**: the app declares
  [`res/xml/locales_config.xml`](app/src/main/res/xml/locales_config.xml), so
  *Settings → Apps → Brutus → Language* lets you run Brutus in English on a
  German phone (or the other way round) without touching the system language.
- **Plurals are real plurals**, not string concatenation: `1 day` / `2 days`,
  `in 1 Tag` / `in 2 Tagen`, `One more shake!` / `4 more shakes!`. The widget
  countdown used to read "in 1 Tagen" — that bug class is now structurally
  impossible.
- **Date patterns are localized too**, not just the words: the next-alarm line
  renders as `Mon, Aug 17, 06:30` in English and `Mo, 17. Aug, 06:30` in German,
  because the pattern itself is a string resource.

Everything the user can read comes from resources — including notification
channel names, the share sheet subject, accessibility labels and the widget.

Adding a third language is a translation job only, no code change: copy
`values/strings.xml` to `values-<lang>/`, translate, add the locale to
`locales_config.xml`. [`ResourceParityTest`](#tests-and-ci) then enforces that
the new file stays complete and that its format specifiers match.

### Scheduling

- **Exact alarm time** via `AlarmManager.setAlarmClock()` — shown in system status bar, works in Doze, survives battery optimization
- **Per-weekday repeat** — bitmask, Mon/Tue/Wed/Thu/Fri/Sat/Sun independently selectable
- **One-shot mode** — no days selected means fire once, then disable
- **Automatic re-scheduling** after the alarm fires (for repeating alarms)
- **Boot receiver** re-registers every enabled alarm after device restart or quick boot (`LOCKED_BOOT_COMPLETED`)

### Lock-screen overlay

The firing alarm presents a full-screen activity **over** the lock screen:

- `showWhenLocked = true` / `turnScreenOn = true` / `FLAG_KEEP_SCREEN_ON`
- `KeyguardManager.requestDismissKeyguard()` to skip PIN entry
- Full-screen notification with `CATEGORY_ALARM` and `setFullScreenIntent()`
- Large centered digital clock ticking in real time
- "BRUTUS ALARM" banner with animated progress dots for multi-challenge sequences
- Back button is intentionally blocked during the challenge
- Excluded from recent apps (`excludeFromRecents`)

### Reliability

| Concern | Mechanism |
|---------|-----------|
| Keeping audio playing with screen off | Foreground service with `mediaPlayback` type + `PARTIAL_WAKE_LOCK` (10 min timeout) |
| Surviving silent / DND | `STREAM_ALARM` with maximum volume set at start, restored when dismissed |
| Surviving reboot | Room persistence + `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` receiver, held open via `goAsync()` so the reschedule can't be killed mid-flight (v1.8.0) |
| Ringing before the first unlock | Since v2.3.1 everything the ringing path reads (alarms, follow-ups, snoozes, the QR code) lives in **device-protected storage**, and receiver, service and alarm screen are `directBootAware` — an alarm rings after an overnight OS-update reboot even while the phone still waits for its PIN. Verified on an emulator: locked reboot, alarm rang, challenge solved, volume restored |
| Clock / zone / permission changes | `SystemChangeReceiver` re-registers everything on `TIMEZONE_CHANGED`, `TIME_SET`, exact-alarm permission re-granted and app update; the app does the same on every resume (force stop, backup restore). A 07:00 alarm stays 07:00 after flying to London (v2.3.1) |
| Exact alarms revoked (Android 12/12L) | Registrations degrade to inexact instead of throwing `SecurityException` inside the ringing service (v2.3.1) |
| Surviving app kill | `START_STICKY` service, alarm is re-scheduled before firing; the volume from before the alarm is persisted, so a service killed mid-ring still puts it back (v2.3.1) |
| Preventing accidental snooze | Slide-to-snooze gesture with 85% drag threshold |
| Overlapping alarms | If a second alarm fires while one is still ringing, the old session is cleanly finished first — audio released, and a UHC main alarm's follow-ups get armed instead of silently dropped (v1.8.0). Since v2.3.1 the screen switches to the new alarm's challenges too (before, it kept showing the first alarm, and Snooze snoozed the wrong one) |
| Snooze | Own request-code space, so it neither replaces the next regular occurrence nor gets lost on reboot; snoozing an Ultra Hardcore alarm no longer arms the follow-ups (v2.3.1) |
| Rescheduling with Sunrise | `schedule()` always cancels a previously armed sunrise pre-alarm before re-arming, so no stale sunrise can fire at the old time (v1.8.0) |

---

## Install

### Pre-built APK (recommended)

Grab the latest signed APK from the product page **[brutus.celox.io](https://brutus.celox.io)** — [`/download`](https://brutus.celox.io/download) always points at the newest release, with its SHA-256 listed on the page — or from the [Releases](https://github.com/pepperonas/brutus/releases/latest) page:

```
https://github.com/pepperonas/brutus/releases/latest
```

1. Download `brutus-v*.apk` on your Android device
2. Open the file — Android will prompt to allow install from this source if not already enabled
3. Tap **Install**

The APK is signed with a permanent keystore, so future updates install cleanly over this one.

### Verify what you downloaded

Sideloading means you are trusting a file from the internet — here is everything you need to
check that it is really the one this repository published.

| Property | Value |
|----------|-------|
| Signing certificate | `CN=Brutus, OU=Pepperonas, O=Pepperonas, L=Berlin, ST=Berlin, C=DE` |
| Key | RSA 4096, valid 2026-04-12 → 2053-08-28 (10,000 days) |
| Signature scheme | APK Signature Scheme v2 |
| **Certificate SHA-256** | `69d67a10a826cf4050da4b271af9b5ed500c962bfab07a8f8fe863e3d7600382` |
| v2.5.1 APK | 4,607,650 bytes · SHA-256 `5af7e1d9ae607fa8f20e6013eae9185610b4af8908b3e3c14fcd5e5cdef49416` |

The **certificate** fingerprint is the durable one — it stays identical across every release,
so a mismatch means the APK did not come from here. The APK hash changes with every version.

```bash
shasum -a 256 brutus-v2.5.1.apk
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs -v brutus-v2.5.1.apk
```

### Samsung note

Samsung's One UI by default revokes `SCHEDULE_EXACT_ALARM` for third-party apps. **v1.3.0 detects this automatically** and shows a red _"Exact alarms disabled"_ banner above the alarm list with an _Enable_ button that deep-links straight into the right settings page (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`). Tap it once, toggle the switch, and you're back. The banner re-checks on every app resume and disappears as soon as the permission is granted.

If you'd rather do it manually:

**Settings → Apps → Brutus → Alarms & reminders → Allow**

---

## Permissions

| Permission | Purpose | When granted |
|-----------|---------|---------------|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Precise alarm timing via `setAlarmClock()` | Install time (API 33+: settings toggle) |
| `POST_NOTIFICATIONS` | Foreground service notification | Runtime, first app launch (API 33+) |
| `WAKE_LOCK` | Keep CPU awake during alarm playback | Install time |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after reboot | Install time |
| `CAMERA` | QR code scanning challenge | Runtime, when the alarm fires and QR challenge is active |
| `ACTIVITY_RECOGNITION` (since v1.4.0) | Step counter for the Ultra Hardcore anti-snooze task | Runtime, when enabling Ultra Hardcore Mode or opening the task screen |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (since v1.6.0) | Lets the battery-optimization banner deep-link the system whitelist dialog | Install time (the dialog itself is opt-in per device) |
| `INTERNET` (since v2.3.0) | Only for the opt-in update check (Settings & info → Check for updates, off by default) | Install time — unused until you switch the check on |
| `VIBRATE` | Vibration pattern during alarm | Install time |
| `USE_FULL_SCREEN_INTENT` | Lock-screen alarm overlay | Install time |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Alarm playback service | Install time |
| `WRITE_EXTERNAL_STORAGE` (API ≤ 28 only) | Save QR PNG on legacy Android | Runtime, when saving QR |
| `ACCESS_NETWORK_STATE` (since v1.3.0) | Lets Play Services check connectivity for the one-time ML Kit Barcode model download. **Not declared by Brutus** — it is merged into the final manifest from the ML Kit dependency | Install time |

Everything in that table except the last row is declared in [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml); you can diff it against this list in ten seconds.

Brutus sends no data anywhere. Since v2.3.0 it declares `INTERNET` for exactly one purpose: the [update notice](#update-notice), which is **off by default** — with it off, Brutus makes no network request; with it on, it fetches the newest version number once a day and nothing else. Starting with v1.3.0 the ML Kit Barcode model is shipped _unbundled_ — the model itself is delivered via Google Play Services and pre-fetched at install time (`com.google.mlkit.vision.DEPENDENCIES = barcode` meta-data). This adds an `ACCESS_NETWORK_STATE` permission so Play Services can check connectivity for the one-time model download, but that happens inside Play Services, not in Brutus.

---

## Build from source

### Prerequisites

- **JDK 17** (Temurin, Homebrew OpenJDK, or Android Studio's bundled JBR)
- **Android SDK** with Platform 35 and Build-Tools 35.0.0+
- **Gradle 8.11+** (the included wrapper pulls it automatically)

### Clone and build debug

```bash
git clone https://github.com/pepperonas/brutus.git
cd brutus

# Create local.properties with your SDK path (first build only)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build and install debug APK on a connected device
./gradlew installDebug
```

### Build release APK

Requires the signing keystore — see the next section.

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Run on device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.pepperonas.brutus -c android.intent.category.LAUNCHER 1
```

---

## Release signing

Release builds are signed using a keystore stored outside the repo. The Gradle config reads signing credentials from either `local.properties` or environment variables — whichever is present.

### Local builds

Add to `local.properties` (already in `.gitignore`):

```properties
brutus.storeFile=/absolute/path/to/brutus-release.jks
brutus.storePassword=your_store_password
brutus.keyAlias=brutus
brutus.keyPassword=your_key_password
```

### CI / GitHub Actions

Set these repository secrets, then expose them as env vars in the workflow:

| Secret | Contents | Used by |
|--------|----------|---------|
| `RELEASE_STORE_BASE64` | the `.jks` keystore, base64-encoded (`base64 -i brutus-release.jks \| pbcopy`) | decoded to disk by `release.yml`, which then exports `RELEASE_STORE_FILE` |
| `RELEASE_STORE_PASSWORD` | store password | Gradle `signingConfigs.release` |
| `RELEASE_KEY_ALIAS` | key alias (`brutus`) | Gradle `signingConfigs.release` |
| `RELEASE_KEY_PASSWORD` | key password | Gradle `signingConfigs.release` |

`RELEASE_STORE_FILE` is **not** a secret you set — the workflow writes it into `$GITHUB_ENV`
after decoding `RELEASE_STORE_BASE64`. Gradle prefers those environment variables over
`local.properties` whenever `RELEASE_STORE_FILE` is present, which is what makes the same
`build.gradle.kts` work locally and in CI.

The workflow can also be started by hand from the **Actions** tab
(`workflow_dispatch`) — it then runs the full signed build and verifies the
certificate, but skips the upload because there is no tag to attach to. That is
the way to check the signing setup without cutting a release.

> **Forks:** without any keystore, `:app:assembleRelease` produces a plain
> **unsigned** APK — `signingConfig` is only assigned when a `storeFile` is
> actually present. The release workflow additionally verifies that the built
> APK carries the expected certificate (`69d67a10…`) and fails otherwise, so a
> missing secret can never end up as a silently unsigned APK on the releases
> page.

### Creating a new keystore (for forks)

```bash
keytool -genkeypair -v \
  -keystore brutus-release.jks \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -alias brutus \
  -dname "CN=Brutus, OU=YourOrg, O=YourOrg, L=City, ST=State, C=DE"
```

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1.0 (JVM target 17) |
| UI toolkit | Jetpack Compose — BOM 2026.06.01 |
| Design system | Material 3 **Expressive** — `material3` pinned to 1.5.0-alpha18 (see note below) + Material Icons Extended |
| Type | Space Grotesk (bundled, [SIL OFL](THIRD_PARTY_LICENSES/SpaceGrotesk-OFL.txt)) with tabular numerals |
| Languages | English (default) + German, `locales_config.xml` for the Android 13+ per-app picker |
| Navigation | Navigation Compose 2.8.5 — one `NavHost` across the four tabs |
| Architecture | MVVM (AndroidViewModel + Repository), `StateFlow` + `stateIn` |
| Lifecycle | androidx.lifecycle 2.8.7 (runtime-ktx + viewmodel-compose) |
| Database | Room 2.6.1 with KSP code generation, schema v7 exported to `app/schemas/` |
| Preferences | DataStore Preferences 1.1.1 (theme) + `SharedPreferences` (QR, world clock, timer sound, UHC follow-ups) |
| Scheduling | `AlarmManager.setAlarmClock()` |
| Background | Foreground Service (`mediaPlayback` type) + `PARTIAL_WAKE_LOCK` |
| Audio | `AudioTrack` (procedural PCM synthesis) + `MediaPlayer` (system ringtone) |
| Sensors | Accelerometer (shake), `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR` (Ultra Hardcore) |
| Camera | CameraX 1.4.1 |
| Barcode scanning | Google ML Kit Barcode Scanning (unbundled) 18.3.1 |
| QR generation | ZXing Core 3.5.3 |
| Tests | JUnit 4.13.2, kotlinx-coroutines-test 1.9.0, Robolectric 4.14.1, androidx.test:core 1.6.1 |
| Build | Gradle 8.11.1, AGP 8.7.3, KSP 2.1.0-1.0.29, R8 (`minify` + `shrinkResources`) |
| SDK levels | min 26 (Android 8.0) · target/compile 35 (Android 15) |

**Why `material3` is pinned past the BOM:** BOM 2026.06.01 maps `material3` 1.4.0, where the
Expressive APIs (`MaterialExpressiveTheme`, `MotionScheme`, `expressiveLightColorScheme`) are
still `internal`. They graduate in the 1.5.0-alpha channel, and **1.5.0-alpha18 is the newest
alpha still built against Compose 1.11** — alpha19+ pulls Compose 1.12 and would force
compileSdk 37 + AGP 9.1. Components that had not graduated by alpha18 (`ButtonGroup`,
`LoadingIndicator`, `LinearWavyProgressIndicator`) are used behind an explicit `@OptIn(ExperimentalMaterial3ExpressiveApi)`.

No Hilt, no Koin, no Dagger — manual DI via the Application class. No Retrofit, no coroutines channels, no Flow operators beyond `stateIn`. The codebase is small on purpose: **80 Kotlin files, ~12,000 lines** — 53 of them production code.

---

## Project structure

```
app/src/main/java/com/pepperonas/brutus/
├── MainActivity.kt                  Entry activity, hosts the HomeScreen (4-tab NavHost)
├── AlarmActivity.kt                 Lock-screen overlay activity for the firing alarm
├── TestAlarmActivity.kt             Preview activity for "test wake modes"
├── UltraHardcoreTaskActivity.kt     Anti-snooze step-counter task (v1.4.0)
├── SunriseActivity.kt               10-minute gentle pre-alarm with brightness ramp (v1.6.0)
├── BrutusApplication.kt             App init, notification channels
├── receiver/
│   ├── AlarmReceiver.kt             BroadcastReceiver for the AlarmManager trigger
│   └── BootReceiver.kt              Re-schedules alarms on BOOT_COMPLETED
├── service/
│   └── AlarmService.kt              Foreground service — audio, wake lock, vibration, hardcore guard
├── scheduler/
│   └── AlarmScheduler.kt            AlarmManager wrapper with next-occurrence math
├── data/
│   ├── AlarmEntity.kt               Room entity — time, days bitmask, challenge flags, counts, hardcoreMode
│   ├── AlarmDao.kt                  DAO with Flow-based reactive queries
│   ├── AlarmDatabase.kt             Room database — schema v7, migrations 4→5→6→7
│   └── AlarmRepository.kt           Single data access abstraction
├── viewmodel/
│   ├── AlarmViewModel.kt            State container with StateFlow of alarms
│   ├── TimerViewModel.kt            Countdown state + ticker, survives tab switches (v1.8.0)
│   └── StopwatchViewModel.kt        Stopwatch state + laps, survives tab switches (v1.8.0)
├── ui/
│   ├── theme/
│   │   ├── Color.kt                 Full M3 color-role sets (dark + light) from the BrutusRed seed
│   │   ├── Type.kt                  Expressive type scale — Space Grotesk display + tabular numerals
│   │   ├── Shape.kt                 Expressive shape scale (8/12/16/24/32 dp)
│   │   ├── Theme.kt                 MaterialExpressiveTheme: springs, dark/light, Material You opt-in
│   │   ├── ThemeSettings.kt         DataStore-backed theme prefs (dynamic color)
│   │   ├── Motion.kt                rememberReducedMotion() — gates decorative loops
│   │   └── ThemePreview.kt          Dark/light/dynamic theme specimen previews
│   ├── screens/
│   │   ├── HomeScreen.kt            Bottom-nav shell with NavHost across the four tabs
│   │   ├── AlarmListScreen.kt       Expressive alarm list: tonal cards, swipe-to-delete, FAB, MDMDFSS strip
│   │   ├── AlarmEditDialog.kt       Modal bottom sheet: time, days, sound, challenges, counts, QR, snooze, hardcore, test
│   │   ├── WorldClockScreen.kt      Live multi-zone board, add/remove sheet
│   │   ├── StopwatchScreen.kt       Start / Stop / Lap with centisecond precision
│   │   └── TimerScreen.kt           HMS picker + countdown + quick presets + ringtone finish
│   └── alarm/
│       ├── AlarmScreen.kt           Full-screen overlay with clock + challenge carousel + HARDCORE badge
│       ├── MathChallenge.kt         Numeric input + random multiplication/addition/subtraction (3 difficulties)
│       ├── ShakeChallenge.kt        Accelerometer listener + circular progress ring (3 sensitivities)
│       ├── StepChallenge.kt         Step-counter Compose UI for the Ultra Hardcore anti-snooze task (v1.4.0)
│       ├── QrChallenge.kt           CameraX preview + ML Kit barcode analyzer
│       └── SwipeToSnoozeButton.kt   Custom gesture composable with spring-back animation
├── widget/
│   └── NextAlarmWidget.kt           Home-screen widget (next alarm time + countdown + days) (v1.6.0)
└── util/
    ├── AlarmSound.kt                Enum of available alarm sounds
    ├── AlarmSoundGenerator.kt       Procedural PCM synthesis for all non-system sounds
    ├── BatteryOptimizationPermission.kt  isIgnoring() check + Settings deep-link Intent (v1.6.0)
    ├── ChallengeDifficulty.kt       Math/shake preset descriptions + shake delta threshold (v1.4.0)
    ├── ChallengeFlags.kt            Bitmask helpers for challenge combinations
    ├── ExactAlarmPermission.kt      canScheduleExactAlarms() check + deep-link Intent (v1.3.0+)
    ├── FullScreenIntentPermission.kt canUseFullScreenIntent() check + deep-link Intent (v1.6.1)
    ├── GlobalQrStore.kt             SharedPreferences-backed global QR persistence
    ├── HardcoreAudioGuard.kt        Volume clamp + VOLUME_CHANGED receiver for Hardcore Mode
    ├── Haptics.kt                   BrutusHaptics wrapper around HapticFeedbackConstants (v1.3.1+)
    ├── NextAlarmCalculator.kt       Finds the soonest trigger across all alarms (for the list header)
    ├── QrGenerator.kt               ZXing wrapper + save + share helpers
    ├── SoundPreviewPlayer.kt        AudioTrack wrapper for in-dialog previews (handles all AlarmSound types)
    ├── TimerSoundStore.kt           SharedPreferences-backed timer-finish sound (v1.5.0)
    ├── UltraHardcoreStore.kt        SharedPreferences-backed follow-up alarm registry (v1.4.0)
    └── WorldClockStore.kt           SharedPreferences-backed time-zone selection
```

Resources that matter for the two languages:

```
app/src/main/res/
├── values/strings.xml          English — the default set: ~180 strings, 6 plurals, weekday array
├── values-de/strings.xml       German — full translation, identical key set
├── xml/locales_config.xml      Declares en + de for the Android 13+ per-app language picker
└── layout/widget_next_alarm.xml  Widget layout; its preview texts are resources too
```

**80 Kotlin files, ~12,000 lines** — 53 in `main`, 27 in `test`.

---

## Tests and CI

376 JVM unit tests guard the parts where a bug means someone oversleeps: what actually lands in
`AlarmManager`, the alarm-time arithmetic, persistence, the completeness of both translations, and
every string the user reads on a clock face. There are no instrumented tests — the whole suite runs
on the JVM in seconds.

| Suite | Tests | What it pins down |
|-------|-------|-------------------|
| `scheduler/AlarmSchedulerTest` | 28 | the registrations that actually reach `AlarmManager`: trigger on the configured wall-clock time, passed times roll to tomorrow, weekday matching, sunrise exactly 10 min ahead (and skipped when it would be in the past), `setExactAndAllowWhileIdle` against Doze, the v1.8.0 stale-sunrise fix, snooze intervals, and the request-code carve-out that keeps main / sunrise / two follow-ups from overwriting each other (Robolectric) |
| `util/NextAlarmCalculatorTest` | 17 | one-shot today vs. tomorrow, repeating wrap-around, weekend selection, `formatCountdown` |
| `data/AlarmDaoTest` | 15 | real SQL on an in-memory Room database: ordering, `getEnabledAlarms` for boot recovery, full-field round-trip, REPLACE on conflict, undo-restore via `id = 0`, repository pass-through (Robolectric) |
| `ui/screens/ClockFormattingTest` | 14 | stopwatch and timer readouts: truncation instead of rounding up, the hour column appearing exactly at the hour, and a **constant string width** — the premise of the tabular numerals |
| `util/UltraHardcoreStoreTest` | 12 | the follow-up bookkeeping that has to survive a reboot: sequences tracked independently, `clearAllFor` scoped to one alarm, step-target keys never leaking into the pending list (Robolectric) |
| `widget/NextAlarmWidgetFormatTest` | 12 | the two strings on the home screen in both languages, including singular/plural and the no-rounding-up rule (Robolectric) |
| `util/NextAlarmCalendarEdgeTest` | 11 | **daylight saving**: 23 real hours between triggers on the short night, 25 on the long one, wall-clock time preserved; the skipped and the duplicated hour; month, year and leap-day rollovers |
| `util/AlarmSoundTest` | 14 | the **persisted** sound ids as a golden map — renumbering would silently change what existing alarms play — the retired ids of v2.5.0 and their successors of the same character, plus the display names in both languages |
| `ResourceParityTest` | 11 | the two languages cannot drift: identical key sets, no blank values, **matching format specifiers**, complete plurals, seven weekdays each, no German left in the default file, and `locales_config.xml` in sync with the `values-*` folders |
| `BrutusApplicationTest` | 10 | notification channels are write-once: importance, DND bypass, silence — and the update channel never breaks through DND (Robolectric) |
| `update/UpdateCheckerTest` | 10 | the opt-in update check end to end with a fake source: off means **no request at all**, one notification per version, never for the installed or an older one, the tap opens the download page, the banner follows the switch (Robolectric) |
| `update/ReleaseSourceTest` | 9 | reading the version from the product page's `latest.json` and GitHub's release, garbage never throws, GitHub only asked when the page fails |
| `update/AppVersionTest` | 8 | release tags vs. the installed version: `2.10.0 > 2.9.1`, `v`-prefix and `-beta` suffix, garbage is never "newer" |
| `update/UpdateCheckStoreTest` | 2 | every change reaches the screen (a constant emission is swallowed by `collectAsState`), switching off forgets the finding |
| `scheduler/AlarmNotificationsTest` | 12 | snooze countdown (and "Cancel snooze" only for normal alarms), heads-up armed the configured lead before the alarm, "Dismiss early" skips one occurrence and survives a reboot, never for a Hardcore alarm without its challenge, missed-alarm notice once — not for an alarm ringing right now |
| `timer/TimerControllerTest` | 6 | the timer as a real alarm: elapsed-time wake-up at the end, persisted state, pause/resume re-arm, rings exactly once, a timer from before a reboot reads as idle |
| `ui/settings/AboutLinksTest` | 5 | the About links, PayPal recipient/currency/note, the licence named in-app is the repo's, the bundled font licence equals `THIRD_PARTY_LICENSES` |
| `ui/settings/ThemeModeTest` | 3 | System/Light/Dark round-trips; stored names are a persisted contract |
| `ui/alarm/SnoozeGestureTest` | 3 | the 85 % point of no return is position-only |
| `ui/theme/RollingNumberTest` | 2 | numbers roll in the direction of the change |
| `util/StorageTest` | 7 | the one-time move to device-protected storage: the printed QR code and pending follow-ups survive it, nothing moves while locked, it never runs twice, every store is covered |
| `scheduler/ReschedulerTest` | 6 | a time-zone change keeps the wall-clock time, follow-ups of a self-disabled one-shot alarm survive with their notification, snoozes restored, running twice never stacks |
| `service/AlarmServiceScreenNotificationTest` | 5 | the pinned alarm heads-up gives way to a quiet notification while the alarm screen is in front and comes back when it leaves; dismiss removes both; no lingering service without an alarm (Robolectric) |
| `service/AlarmServiceUltraHardcoreTest` | 5 | the real service: snoozing does not arm the follow-ups, dismissing does (even before the sound has loaded), the volume comes back even after a mid-ring kill |
| `receiver/SystemChangeReceiverTest` | 3 | every handled action is in the manifest filter, the whole ringing path is `directBootAware` |
| `widget/NextAlarmWidgetLockedTest` | 2 | the widget is never touched before the first unlock |
| `update/UpdateSchedulerTest` | 6 | switching on schedules a daily, network-bound check plus one immediate check; off cancels everything; default stays off after updating (WorkManager test driver) |
| `ui/alarm/MathProblemTest` | 8 | answer/display correctness, per-difficulty operand range and sign invariants across 500 samples, operator fallback |
| `viewmodel/TimerViewModelTest` | 8 | countdown/pause math, cancel-undo state machine, an expired timer is deliberately *not* undoable (Robolectric) |
| `util/ChallengeFlagsTest` | 8 | `describe` / `activeList` / `has` / `sanitize` bitmask edge cases, incl. the unknown-bit fallback |
| `util/ChallengeDifficultyTest` | 8 | math operand ranges, shake threshold ordering (9 / 12 / 16 m/s²), distinct labels per preset in both languages |
| `util/AlarmSoundGeneratorTest` | 7 | PCM buffer length, peak amplitudes, gentle loops without a click at the seam, gentle vs. harsh vs. extreme classification |
| `util/SynthSoundsTest` | 11 | what makes each v2.5.0 sound itself, measured on the buffer — the air-raid siren winds up and down, the reverse beeper and the strobe speed up, the ocean swells, Daybreak brightens; harsh at full scale, gentle at 60 %, six-second gentle loops |
| `viewmodel/AlarmViewModelTest` | 3 | the alarm list tells "not loaded yet" (`null`) from "no alarms" — a cold start no longer flashes the empty state (Robolectric + Room) |
| `ReadmeSyncTest` | 7 | the README's version, unit-test and lines-of-code badges against `build.gradle.kts` and the source, both changelogs start with the current version, the PayPal button matches the app's, every shown image exists in both languages |
| `util/AlarmSoundGeneratorPropertiesTest` | 7 | invariants every synthesized sound must hold — loop length budget, determinism, DC offset, gentle headroom; a new enum entry is covered automatically |
| `util/PermissionDeepLinkTest` | 7 | the three reliability banners land on the right settings page (action + `package:` URI + `NEW_TASK`) (Robolectric) |
| `data/RoomSchemaExportTest` | 7 | the running database's **identity hash against the committed `7.json`** — a field added without a migration fails here instead of on a user's device |
| `LocalizedRuntimeTest` | 7 | resolves every declared string through the resource system in **both** locales and renders a complete alarm card in each |
| `data/AlarmEntityDefaultsTest` | 7 | the constructor defaults, which are a persisted contract |
| `data/AlarmEntityTest` | 7 | `timeString` padding, `repeatDaysString` / `soundName` / `challengeName` in English **and** German, weekday bitmask, `hardcoreEffective` |
| `util/GlobalQrStoreTest` | 6 | the installation's QR code never changes — every printed copy depends on it (Robolectric) |
| `util/WorldClockStoreTest` | 6 | default seeding on first launch, round-trips, an empty list stays empty, blank filtering (Robolectric) |
| `util/TimerSoundStoreTest` | 7 | timer tone persistence, id 0 vs. "unset", corrupt id degrades to the system tone, a retired id resolves to its successor (Robolectric) |
| `util/SunriseSoundSettingTest` | 5 | the Sunrise sound: default, round-trip, only gentle sounds or silence, harsh/unknown ids fall back, retired ids resolve (Robolectric) |
| `util/SoundExportTest` | 2 | writes every sound as WAV + overview page, and the product page previews — skipped unless `BRUTUS_SOUND_EXPORT` / `BRUTUS_SOUND_PREVIEWS` is set |
| `viewmodel/StopwatchViewModelTest` | 6 | segment accumulation, laps, reset-undo snapshot semantics |
| `scheduler/AlarmSchedulerConstantsTest` | 4 | Ultra Hardcore offsets, sunrise lead time, intent-extra uniqueness |

```bash
./gradlew :app:testDebugUnitTest          # all 376
./gradlew :app:testDebugUnitTest --tests '*NextAlarmCalculatorTest'
# HTML report: app/build/reports/tests/testDebugUnitTest/index.html
```

**Testability by design:** both ViewModels take an injectable clock (`now: () -> Long`,
defaulting to `SystemClock::elapsedRealtime`), so the timing state machines run
deterministically on the JVM — no `Thread.sleep`, no flakiness. Calendar-sensitive suites pin
`TimeZone.setDefault(Europe/Berlin)` rather than trusting the host zone, and the
Android-dependent suites run under **Robolectric** against real shadows — `ShadowAlarmManager`
records the actual registrations, Room runs in-memory against the generated implementation. No
mocking framework is used anywhere; the code under test is exercised, not simulated.

### Workflows

| Workflow | Trigger | Does |
|----------|---------|------|
| [`tests.yml`](.github/workflows/tests.yml) | push to `main`, every PR | JDK 17 + Gradle cache → `:app:testDebugUnitTest`, uploads the HTML report as an artifact when it fails |
| [`release.yml`](.github/workflows/release.yml) | tag `v*` | runs the tests, decodes the keystore from `RELEASE_STORE_BASE64`, builds `assembleRelease`, renames the APK to `brutus-<tag>.apk` and attaches it to the GitHub release |

### Test layout

```
app/src/test/java/com/pepperonas/brutus/
├── BrutusApplicationTest.kt            9 — notification channels: importance, DND bypass, silence
├── LocaleContexts.kt                       test helper: a Context pinned to en / de
├── LocalizedRuntimeTest.kt             7 — every string resolves in both languages, full card render
├── ReadmeSyncTest.kt                   7 — README badges, changelog heads and images against the source
├── ResourceParityTest.kt              11 — values/ vs. values-de/: keys, plurals, format specifiers
├── data/
│   ├── AlarmDaoTest.kt                15 — in-memory Room: ordering, enabled filter, round-trip, REPLACE, undo-restore
│   ├── AlarmEntityTest.kt              7 — timeString, repeatDaysString / soundName / challengeName in en + de
│   ├── AlarmEntityDefaultsTest.kt      7 — the persisted constructor defaults
│   └── RoomSchemaExportTest.kt         7 — runtime identity hash vs. committed schema, per-version columns
├── scheduler/
│   ├── AlarmSchedulerTest.kt          28 — what reaches AlarmManager: triggers, sunrise, snooze, follow-up request codes
│   ├── AlarmSchedulerConstantsTest.kt  4 — UHC offsets, sunrise lead, intent extra uniqueness
│   └── ReschedulerTest.kt              6 — zone change keeps wall-clock time, follow-ups + snoozes restored
├── receiver/
│   └── SystemChangeReceiverTest.kt     3 — manifest filter covers every handled action, direct-boot awareness
├── service/
│   ├── AlarmServiceScreenNotificationTest.kt 5 — heads-up gives way while the alarm screen is in front
│   └── AlarmServiceUltraHardcoreTest.kt 5 — snooze ≠ dismiss, volume restored after a mid-ring kill
├── ui/
│   ├── alarm/MathProblemTest.kt        8 — answer/display, per-difficulty range + sign invariants (500 samples)
│   └── screens/ClockFormattingTest.kt 14 — stopwatch/timer readouts, hour column, constant string width
├── update/
│   ├── AppVersionTest.kt               8 — tag vs. installed version, v-prefix, suffixes, garbage
│   ├── ReleaseSourceTest.kt            9 — latest.json + GitHub release parsing, fallback order
│   ├── UpdateCheckStoreTest.kt         2 — every change reaches the screen, off forgets the finding
│   ├── UpdateCheckerTest.kt           10 — off = no request, notify once per shown version, banner, tap target
│   └── UpdateSchedulerTest.kt          6 — daily network-bound work, immediate check, cancel on off
├── util/
│   ├── AlarmSoundTest.kt              14 — golden map of the persisted sound ids, retired ids, names per language
│   ├── AlarmSoundGeneratorTest.kt      7 — PCM length, peak amplitudes, seamless gentle loops, gentle vs harsh
│   ├── AlarmSoundGeneratorPropertiesTest.kt  7 — invariants across every synthesized sound
│   ├── ChallengeFlagsTest.kt           8 — describe / activeList / has / sanitize, unknown-bit fallback
│   ├── ChallengeDifficultyTest.kt      8 — operand ranges, threshold ordering, distinct labels
│   ├── GlobalQrStoreTest.kt            6 — the installation QR code is generated once and never changes
│   ├── NextAlarmCalculatorTest.kt     17 — one-shot today/tomorrow, repeating wrap, weekend selection
│   ├── NextAlarmCalendarEdgeTest.kt   11 — DST nights (23 h / 25 h), skipped + duplicated hour, rollovers
│   ├── StorageTest.kt                  7 — move to device-protected storage keeps the QR code
│   ├── PermissionDeepLinkTest.kt       7 — the three reliability banners land on the right settings page
│   ├── TimerSoundStoreTest.kt          7 — timer tone persistence, corrupt-id fallback, retired ids
│   ├── SunriseSoundSettingTest.kt      5 — Sunrise sound: gentle only, fallbacks, retired ids
│   ├── SoundExportTest.kt              2 — WAV export + product page previews (opt-in)
│   ├── SynthSoundsTest.kt             11 — what makes each v2.5.0 sound itself
│   ├── UltraHardcoreStoreTest.kt      12 — reboot-surviving follow-up bookkeeping
│   └── WorldClockStoreTest.kt          6 — default seeding, round-trips, empty list sticks
├── viewmodel/
│   ├── AlarmViewModelTest.kt           3 — "not loaded yet" is not "no alarms"
│   ├── StopwatchViewModelTest.kt       6 — segment accumulation, laps, reset-undo snapshot semantics
│   └── TimerViewModelTest.kt           8 — countdown/pause math, cancel-undo, finished-not-undoable
└── widget/
    ├── NextAlarmWidgetFormatTest.kt   12 — the home-screen countdown and repeat-day strip
    └── NextAlarmWidgetLockedTest.kt    2 — never touched before the first unlock
```

---

## How it works

### Alarm firing timeline

```
T − ∞     User creates alarm   →   AlarmScheduler.schedule()
                                     ↓
                            AlarmManager.setAlarmClock(triggerTime)
                                     ↓
T  0s     AlarmReceiver.onReceive()
                                     ↓
            startForegroundService(AlarmService, ACTION_START)
                                     ↓
T + 50ms  AlarmService.startAlarm()
            • Acquire PARTIAL_WAKE_LOCK (10 min)
            • Set STREAM_ALARM volume → max (save previous)
            • Start vibration pattern
            • Load alarm entity from Room (IO thread)
            • Play sound via AudioTrack (synthesized) OR MediaPlayer (system)
            • Launch AlarmActivity (FLAG_ACTIVITY_NEW_TASK)
            • Re-schedule for next occurrence if repeating, else disable
                                     ↓
T + 100ms AlarmActivity renders
            • setShowWhenLocked / setTurnScreenOn / dismissKeyguard
            • AlarmScreen composable reads challengeFlags, math/shake counts, QR
            • Iterates active challenges in sequence
                                     ↓
          User completes all challenges
                                     ↓
          AlarmActivity.stopAlarm()
            • Intent(AlarmService, ACTION_STOP)
            • finishAndRemoveTask()
                                     ↓
          AlarmService.stopAlarm()
            • Stop MediaPlayer/AudioTrack, cancel vibration
            • Restore original STREAM_ALARM volume
            • Release wake lock
            • stopForeground + stopSelf
```

### Snooze path

The same flow, but after the user slide-triggers the snooze button:

- `AlarmActivity` sends `ACTION_SNOOZE` with the alarm id to the service
- Service calls `AlarmScheduler.scheduleSnooze()` which registers a fresh `setAlarmClock()` for `now + snoozeDuration minutes`
- Current alarm is fully torn down
- The snooze fires exactly like a regular alarm — same challenges, same sound

### Ultra Hardcore follow-up timeline

```
T  0s     Main alarm fires (same flow as a regular alarm)
                                     ↓
          User completes challenges and dismisses
                                     ↓
T + 1s    AlarmService.stopAlarm() sees ultraHardcoreMode=true
            • AlarmScheduler.scheduleFollowup(seq=1, T+10min)
            • AlarmScheduler.scheduleFollowup(seq=2, T+15min)
            • UltraHardcoreStore.recordFollowup(...) × 2
            • Persistent reminder notification posted (CHANNEL_ULTRA_HARDCORE)
                                     ↓
        ┌── User opens task → walks N steps ──→ both follow-ups cancelled, notification cleared
        │
T + 10m AlarmReceiver fires with EXTRA_IS_FOLLOWUP=true, seq=1
            • AlarmService skips reschedule logic (one-shot)
            • Same challenge chain + Hardcore guard apply
            • UltraHardcoreStore.clearFollowup(seq=1) on dismiss
                                     ↓
T + 15m AlarmReceiver fires with EXTRA_IS_FOLLOWUP=true, seq=2
            • Same flow
            • Notification auto-cleared once no follow-up entries remain for this alarmId
```

### Boot recovery

`BootReceiver` listens for both `ACTION_BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED` (`directBootAware = true`) and calls `Rescheduler.rescheduleAll()` — the same function `SystemChangeReceiver` and `MainActivity.onResume` use. It registers every enabled alarm, every pending Ultra Hardcore follow-up whose `triggerAt` is still in the future (looked up regardless of the alarm's enabled flag — a one-shot alarm disables itself when it fires, its follow-ups must survive anyway) together with the reminder notification, and every remembered snooze. Expired entries get cleaned out.

Since v2.3.1 this works **before the first unlock**: the data lives in device-protected storage (`util/Storage.kt`); installs from earlier versions move it there once, the first time the app runs unlocked. Until that move has happened, the locked pass does nothing and `BOOT_COMPLETED` does the work. The home-screen widget is not touched while locked — every `AppWidgetManager` call throws then, which on the emulator killed the ringing service a second into the alarm.

---

## Design philosophy

- **Every decision favors waking the user up over UX politeness.** If you need a polite alarm, use the system clock.
- **Challenges are configurable because brains are different.** Some people need math; others just need physical movement. Some need both.
- **No account, no tracking, offline by default.** Brutus only goes online if you switch on the update notice — and then only to read a version number.
- **APK size matters more than we initially thought.** v1.2.0 was 35 MB because of bundled ML Kit; v1.3.0 switched to the unbundled variant and turned on R8 minification + resource shrinking, cutting the download by roughly 88 % without losing any functionality. v2.2.0 ships at **4,414,213 bytes (≈ 4.2 MiB)** — and that now includes the full Material 3 Expressive theme layer *and* a second language.
- **Procedural audio beats licensed samples.** Synthesized sounds mean no copyright issues, no asset loading, no file cache — and the sounds can be tuned to be as nasty as needed.
- **Destructive DB migration is acceptable during pre-1.0 development.** Once Brutus hits a real release cadence, proper Room migrations will replace the current fallback.

---

## Troubleshooting

### Alarm doesn't fire at the exact time
Check that _Alarms & reminders_ is allowed for Brutus in system settings. On Samsung, Xiaomi, and Huawei devices this is frequently denied by default. Also disable battery optimization for Brutus (_Settings → Apps → Brutus → Battery → Unrestricted_).

### Alarm rings but no sound
Confirm `STREAM_ALARM` is not muted at the system level (some phones have a dedicated hardware mute for alarms). Try switching the alarm sound to _System_ — if that works, one of the synthesized sounds hit a device-specific AudioTrack bug; please open an issue.

### QR scan never triggers
Make sure the printed QR has good lighting and enough contrast. Test mode with the phone camera in realistic conditions first. The scanner only accepts an exact string match — if you regenerate the QR globally or re-install the app, the old printed code becomes invalid.

### Notification persists after dismissing
Force-stop Brutus once via system settings. This is typically an edge case when the service didn't finish cleanly after the alarm was killed by aggressive battery management.

### App crashes after update
Starting with v1.3.0 Brutus uses proper Room migrations and exports its schemas to `app/schemas/`. Migration paths from v4 onward preserve user alarms across updates. Pre-v1.0.0 dev versions (1, 2, 3) still fall back to a destructive recreate — anyone on those was a developer-tester anyway.

---

## Roadmap

Planned, no specific timeline:

- [x] Samsung-style multi-tab clock suite (v1.2.0)
- [x] World Clock, Stopwatch, Timer (v1.2.0)
- [x] Hardcore Mode — volume lock + volume-key consumption (v1.2.0)
- [x] Premium monogram app icon (v1.2.0)
- [x] Proper Room migrations + schema export (v1.3.0)
- [x] R8 / ProGuard rules for size-optimized release builds (v1.3.0)
- [x] Unbundled ML Kit Barcode for slim APK (v1.3.0)
- [x] Exact-alarm permission banner with deep link to system settings (v1.3.0)
- [x] Tasteful haptic feedback on key interactions (v1.3.1)
- [x] JUnit test coverage for alarm-time math (v1.3.1)
- [x] GitHub Actions workflow for tests + tagged releases (v1.3.1)
- [x] Ultra Hardcore Mode — two follow-up alarms + step-counter anti-snooze task (v1.4.0)
- [x] Configurable shake sensitivity (v1.4.0)
- [x] Math difficulty presets (easy / hard / brutal) (v1.4.0)
- [x] Gentle alarm sounds + configurable timer finish tone (v1.5.0)
- [x] Sunrise pre-alarm with screen brightness ramp + chime fade-in (v1.6.0)
- [x] Home-screen widget showing next upcoming alarm (v1.6.0)
- [x] Battery-optimization detection banner with deep-link to system whitelist (v1.6.0)
- [x] Pop alarm to the foreground via full-screen-intent banner + hardened activity launch (v1.6.1)
- [x] Redesigned alarm cards — full-width weekday strip + info chips (mode, sunrise, challenge, snooze, sound) (v1.7.0)
- [x] Five extreme alarm sounds — stadium horn, jackhammer, fire alarm, dental drill, banshee (v1.7.0)
- [x] Bug-fix pass: `goAsync()` in boot/widget receivers, overlapping-alarm session takeover, stale-sunrise cancel, camera release after QR scan, siren loop click (v1.8.0)
- [x] Timer & stopwatch survive tab switches via Activity-scoped ViewModels (v1.8.0)
- [x] GUI polish: delete-all confirmation, one-line weekday picker in the edit sheet, pinned save CTA, 48 dp delete target, TalkBack snooze action, search placeholder (v1.8.0)
- [x] Undo snackbar for deletions (single + delete-all, restores scheduling) (v1.9.0)
- [x] Copy alarms — prefilled "Alarm kopieren" edit sheet per card (v1.9.0)
- [x] **Material 3 Expressive redesign** — full color-role system (dark + light + Material You opt-in), `MaterialExpressiveTheme` with spatial springs, Space Grotesk display type with tabular numerals, tonal card hierarchy, swipe-to-delete, morphing FAB/CTAs, on-screen math keypad, wavy progress rings, day/night world-clock roles, shared-axis tab transitions, reduced-motion support (v2.0.0)
- [x] Tabular numerals on the entire type scale — every number in the app ticks like the stopwatch, zero jitter (v2.1.0)
- [x] Undo everywhere — world-clock zone removal (restored in place), stopwatch reset (elapsed + laps), timer abort (resumes with remaining time), on top of the existing alarm-delete undo (v2.1.0)
- [x] Alarm-card color encodes the enabled state — all active alarms share one muted red, disabled ones sink to gray, the next alarm wears a thin primary outline instead of a confusing different fill (v2.1.1)
- [x] ViewModel unit tests — injectable clock, 20 new tests for the stopwatch/timer state machines (incl. undo snapshots) + AlarmEntity helpers, Robolectric for the timer (v2.1.1)
- [ ] Per-alarm sound override at runtime
- [ ] Multi-QR support (different codes for different alarms)
- [ ] Wear OS companion
- [x] **English + German localization** — every string in `values/` + `values-de/`, per-app language picker on Android 13+
- [ ] Localization beyond English and German
- [ ] Sleep statistics tab (how often, dismiss latency, snooze rate)
- [ ] Backup / restore alarm list as JSON

Contributions welcome on any of these — open an issue first to coordinate.

---

## Developer

**Martin Pfeffer** · [celox.io](https://celox.io)

GitHub: [@pepperonas](https://github.com/pepperonas) · Email: [martin.pfeffer@celox.io](mailto:martin.pfeffer@celox.io)

Brutus is part of a family of small, focused Android apps published under [pepperonas](https://github.com/pepperonas) — all built solo, all offline-first, all opinionated.

---

## Donate

If Brutus actually gets you out of bed in the morning, consider buying me a coffee (or a louder alarm) via **PayPal**:

[![Donate via PayPal](https://img.shields.io/badge/PayPal-Donate-00457C?logo=paypal&logoColor=white&style=for-the-badge)](https://www.paypal.com/paypalme/martinpfeffer)

Donations are never expected — the app is and will stay free, ad-free, and offline by default. Every contribution funds more brutal alarm experiments.

---

## License

```
MIT License

Copyright (c) 2026 pepperonas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### Third-party components

| Component | License |
|-----------|---------|
| [Space Grotesk](https://github.com/floriankarsten/space-grotesk) — bundled in `res/font/` | SIL Open Font License 1.1 · [full text](THIRD_PARTY_LICENSES/SpaceGrotesk-OFL.txt) |
| AndroidX · Jetpack Compose · Material 3 · Room · CameraX · DataStore | Apache License 2.0 |
| [ZXing Core](https://github.com/zxing/zxing) | Apache License 2.0 |
| Google ML Kit Barcode Scanning (via Play Services) | Android SDK Terms + [ML Kit terms](https://developers.google.com/ml-kit/terms) |

The bundled font is the only third-party asset shipped inside the APK — every alarm sound is
synthesized at runtime, so there is no sample licensing to track.
