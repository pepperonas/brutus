# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Brutus is a single-module Android alarm clock (Kotlin, Jetpack Compose, Material 3 Expressive) whose
alarms can only be dismissed by solving challenges (math / shake / QR), plus world clock, stopwatch
and timer tabs. Package `com.pepperonas.brutus`, minSdk 26, target/compile SDK 35, JDK 17.

## Commands

```bash
./gradlew :app:assembleDebug                      # debug APK
./gradlew :app:installDebug                       # install on connected device
./gradlew :app:testDebugUnitTest                  # full JVM suite (what CI runs)
./gradlew :app:testDebugUnitTest --tests '*NextAlarmCalculatorTest'   # one class
./gradlew :app:testDebugUnitTest --tests '*AlarmSchedulerTest.someMethod'
./gradlew :app:assembleRelease                    # R8 + resource shrinking; unsigned if no keystore
./gradlew :app:lint
```

HTML test report: `app/build/reports/tests/testDebugUnitTest/index.html`. There are **no
instrumented tests** — everything runs on the JVM; Android-dependent suites use Robolectric
(`isIncludeAndroidResources = true`). No mocking framework is used anywhere — keep it that way
(ViewModels take an injectable clock `now: () -> Long`; calendar tests pin
`TimeZone.setDefault(Europe/Berlin)`; `ShadowAlarmManager` records real registrations; Room runs
in-memory; tests that use the singleton call `AlarmDatabase.resetInstanceForTests()`). Robolectric
reports `canScheduleExactAlarms() == false` by default — scheduler tests set
`ShadowAlarmManager.setCanScheduleExactAlarms(true)`. Behaviour Robolectric cannot show (locked boot,
widget calls throwing while locked, a second alarm taking over the screen) is checked on an emulator:
use a dedicated AVD (`brutus-test`, port 5580), never an emulator another session is driving.

## Architecture

**Alarm firing path** (the core — read these together):
`scheduler/AlarmScheduler` registers `AlarmManager.setAlarmClock()` →
`receiver/AlarmReceiver` → `startForegroundService(service/AlarmService)` (wake lock, forces
`STREAM_ALARM` to max and restores it later, plays synthesized audio via `AudioTrack` or system
tone via `MediaPlayer`, re-schedules repeating alarms / disables one-shots) → launches
`AlarmActivity` (over the lock screen) which hosts `ui/alarm/AlarmScreen` iterating the active
challenges in sequence → `ACTION_STOP` / `ACTION_SNOOZE` back to the service.

- **PendingIntent request codes are partitioned** in `AlarmScheduler`: main alarm = alarm id,
  sunrise pre-alarm = `0x2D000000 | id`, snooze = `0x5A000000 | id`, Ultra-Hardcore follow-ups =
  `0x4F000000 | id<<4 | seq`.
  Any new alarm kind needs its own carve-out, or it silently overwrites another registration.
  `schedule()` always cancels a stale sunrise before re-arming.
- **Ultra Hardcore**: after dismissal the service arms two follow-ups (+10/+15 min) tracked in
  `util/UltraHardcoreStore` (must survive reboot); `UltraHardcoreTaskActivity` (step counter)
  cancels them. `util/HardcoreAudioGuard` snaps alarm volume back to max on every volume change.
- **Re-registration**: `scheduler/Rescheduler.rescheduleAll` is the one function for "AlarmManager may
  be wrong now" — called by `BootReceiver`, `receiver/SystemChangeReceiver` (time zone, clock,
  exact-alarm permission re-granted, app update) and `MainActivity.onResume`. It restores enabled
  alarms, UHC follow-ups (looked up regardless of `enabled` — one-shots disable themselves on fire)
  with their reminder (`util/UltraHardcoreNotifier`), and snoozes (`util/RingingStore`). Idempotent.
- **Direct boot**: alarms must ring before the first unlock after a reboot. The DB and every
  SharedPreferences file live in device-protected storage via `util/Storage` (`Storage.prefs`,
  `Storage.device`); a new store must use `Storage.prefs` and be listed in
  `Storage.PREFERENCE_FILES`, or it is unreadable while locked and skipped by the one-time migration.
  Nothing on the ringing path may touch credential storage or `AppWidgetManager` while locked
  (`NextAlarmWidget.refresh` checks `Storage.isUserUnlocked`); receiver, service and alarm
  activities are `directBootAware` (pinned by `SystemChangeReceiverTest`).
- **Notifications around an alarm** (`util/AlarmNotifier`): heads-up before it rings (request code
  `0x3A000000 | id`, lead in `util/AppSettings`), snooze countdown, missed-alarm notice (from
  `RingingStore.expected` in `Rescheduler`). "Dismiss early" = `scheduler/AlarmActions.skipNext` — a
  Hardcore alarm only with `solvedChallenge = true` (via `TestAlarmActivity` in early-dismiss mode);
  the skipped occurrence lives in `RingingStore.skips` and `NextAlarmCalculator` honours it.
- **Timer** (`timer/`): `TimerController` is the source of truth (persisted, elapsed-time AlarmManager
  wake-up at code `0x71000000`, ongoing notification); `TimerReceiver` handles fire/pause/resume/cancel,
  `TimerRingService` rings. `TimerViewModel` only mirrors it and runs the repaint ticker.
- **Settings & info** (`ui/settings/`): route `settings` in `HomeScreen`'s NavHost, opened from the ⋮
  menu; About facts in `AboutLinks` (pinned by `AboutLinksTest`).
- **Motion**: specs come from `MaterialTheme.motionScheme` (files opt in with
  `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`); decorative loops are read in draw/layer
  lambdas, never in composition; `rememberReducedMotion()` gates them.
- **Snooze vs. dismiss**: `AlarmService.stopAlarm(dismissed)` — only a completed challenge arms UHC
  follow-ups. Snoozes use their own request-code space `0x5A000000 | id`.
- `SunriseActivity`, `TestAlarmActivity` and `widget/NextAlarmWidget` are side entry points;
  `util/NextAlarmCalculator` is the shared next-trigger arithmetic (DST-sensitive).

**Update check** (`update/`, opt-in, off by default): the only code that touches the network.
`UpdateScheduler` enqueues a unique 24 h `PeriodicWorkRequest` + one immediate check (both
`NetworkType.CONNECTED`) when the ⋮-menu switch is on and cancels everything when it is off;
`UpdateChecker` asks `ReleaseSource` (`brutus.celox.io/latest.json`, GitHub releases API as fallback),
compares via `AppVersion`, posts once per version on `CHANNEL_UPDATES` and records the finding in
`UpdateCheckStore`, which the alarm-list banner follows. Keep the privacy wording in README/website in
sync with any change here.

**Data**: Room `data/AlarmDatabase` (version 7, `exportSchema = true`, schemas committed under
`app/schemas/`). Adding/changing an `AlarmEntity` field requires a bumped version, a `Migration`,
and the new exported schema — `RoomSchemaExportTest` compares the runtime identity hash against the
committed JSON. Small non-alarm state lives in SharedPreferences/DataStore stores in `util/` and `ui/theme/`
(`GlobalQrStore`, `WorldClockStore`, `TimerSoundStore`, `ThemeSettings`).

**Persisted contracts — do not renumber**: `AlarmSound` ids (pinned as a golden map in
`AlarmSoundTest`), `ChallengeFlags` bits (`MATH=1, SHAKE=2, QR=4`; `sanitize()` turns 0 into MATH),
`AlarmEntity` constructor defaults, and the global QR payload (users print it).

**UI**: single-activity `MainActivity` with a 4-tab bottom nav (`ui/screens/HomeScreen`).
Theme in `ui/theme/` (Space Grotesk font, brand red; Material You dynamic color is an opt-in).
`material3` is deliberately pinned to `1.5.0-alpha18` past the Compose BOM for the Expressive APIs —
don't bump it without reading the comment in `app/build.gradle.kts` (alpha19+ needs compileSdk 37 /
AGP 9.1). Lint check `NullSafeMutableLiveData` is disabled on purpose (AGP lint crash).

## Localization

English is the default (`values/`), German a complete translation (`values-de/`). Every
user-visible string — including notification channel names, a11y labels, widget text and **date
patterns** — must be a resource, added to **both** files with matching format specifiers and real
`<plurals>`. `ResourceParityTest` and `LocalizedRuntimeTest` enforce this; `locales_config.xml`
must list every `values-*` locale.

Docs are bilingual too: `README.md`/`README.de.md`, `CHANGELOG.md`/`CHANGELOG.de.md`,
`docs/screenshots/SHOTLIST{,.de}.md` — update both languages together. The README carries test
counts per suite; keep them in sync when adding tests.

## Releasing

Bump `versionCode` + `versionName` in `app/build.gradle.kts`, add a CHANGELOG entry (both
languages), then push a `v*` tag. `.github/workflows/release.yml` runs the tests, decodes the
keystore from secrets, builds `assembleRelease`, **verifies the APK certificate SHA-256
(`69d67a10…`)** and fails otherwise, then attaches `brutus-<tag>.apk` to the GitHub release. The key
alias is a repo *variable* (`RELEASE_KEY_ALIAS`), not a secret. Local signing reads
`brutus.storeFile/storePassword/keyAlias/keyPassword` from `local.properties` (git-ignored).
`workflow_dispatch` runs the full signed build without publishing.

## Product page (`website/`)

`https://brutus.celox.io`, generated by the product-page kit (`~/claude/_templates/apps/product-page`)
from `website/site.json` — edit texts there (en/de/es/it/fr, only true statements), never the generated
files. Rebuild: `python3 <kit>/build.py website/site.json website --force` (plus `--hero/--screens/--icon`
when images change; the sources were rendered from `docs/screenshots/`), then `build.py --check website`
and `./website/deploy.sh` (`server` also installs timer + vhost). Releases need no deploy: the
`brutus-latest.timer` on the VPS mirrors the newest GitHub release every 15 minutes, matching
`^brutus-v….apk$` — keep that asset name in `release.yml`.
