# Changelog

**English** · [Deutsch](CHANGELOG.de.md)

All notable changes to Brutus are documented here. Versions follow [SemVer](https://semver.org).

## [Unreleased]

### Added — English and German localization
- **The app now ships in two languages.** Every user-visible string moved out of
  the Kotlin sources into resources: `values/strings.xml` (English, the new
  default) and `values-de/strings.xml` (German, a complete translation of the
  wording the app used before). ~180 strings, 6 plurals and a weekday array in
  each — no partial coverage, no silent fallback.
- **Per-app language on Android 13+** via `res/xml/locales_config.xml`, so
  Brutus can run in English on a German phone and vice versa.
- **Real plurals instead of concatenation** (`countdown_days`, `widget_in_days`,
  `shake_remaining`, `step_remaining`, `alarm_deleted`, `alarm_delete_all_body`).
- **Localized date patterns**, not just words: the next-alarm line and the
  world-clock date read `Mon, Aug 17, 06:30` / `Mo, 17. Aug, 06:30` because the
  pattern itself is a string resource. The hardcoded `Locale.GERMAN` in the
  alarm list and the widget is gone.
- Notification channel names, the share-sheet subject, accessibility labels and
  the widget preview text are resources too.

### Added — 24 more unit tests (219 → 243)
- `ResourceParityTest` (11) — the two languages cannot drift apart: identical key
  sets, no blank values, **matching format specifiers** (a dropped `%1$s` crashes
  with `IllegalFormatException` in one language only), complete plurals, seven
  weekdays each, no German left in the default file, and `locales_config.xml` in
  sync with the `values-*` folders.
- `LocalizedRuntimeTest` (7) — resolves every declared string through the
  resource system in both locales and renders a complete alarm card in each.
- The suites that used to pin German strings (`AlarmEntityTest`,
  `ChallengeFlagsTest`, `NextAlarmCalculatorTest`, `NextAlarmWidgetFormatTest`,
  `AlarmSoundTest`, `ChallengeDifficultyTest`) now assert **both** languages via
  the new `LocaleContexts` test helper.

### Changed
- `AlarmSound` carries `@StringRes labelRes` / `descriptionRes` instead of
  literal display names; `ChallengeDifficulty` returns resource ids;
  `ChallengeFlags.describe`, `AlarmEntity.repeatDaysString` / `soundName` /
  `challengeName`, `NextAlarmCalculator.formatCountdown` and the two widget
  formatters take a `Context`. The persisted numeric ids are untouched.
- `ChallengeFlags.describe` no longer returns an empty string for an unknown
  flag bit — it falls back to "None" / "Keine", so a corrupt or future value
  cannot render an empty chip.

## [Unreleased — earlier]

### Added
- **144 more unit tests** (75 → **219**), twelve new suites along the paths that
  had no coverage at all:
  - `AlarmSchedulerTest` (23, Robolectric/`ShadowAlarmManager`) — **what actually
    reaches AlarmManager**: trigger on the configured wall-clock time and in the
    future, passed times roll to tomorrow, weekday matching, sunrise exactly
    10 min ahead and only while it is still in the future,
    `setExactAndAllowWhileIdle` (Doze), **a regression guard for the v1.8.0
    stale-sunrise fix**, snooze intervals, and the request-code carve-out that
    keeps a main alarm, its sunrise and both follow-ups from overwriting each
    other.
  - `AlarmDaoTest` (15, in-memory Room) — real SQL instead of a mock, including
    the REPLACE conflict and the `id = 0` undo-restore path.
  - `ClockFormattingTest` (14) — stopwatch/timer readouts including **constant
    string width**, the premise the tabular numerals rest on.
  - `UltraHardcoreStoreTest` (12) — the follow-up bookkeeping that has to survive
    a reboot.
  - `NextAlarmWidgetFormatTest` (12) — the widget strings.
  - `NextAlarmCalendarEdgeTest` (11) — **daylight saving**: 23 real hours between
    two alarms on the short night, 25 on the long one (wall-clock time
    preserved), the skipped and the duplicated hour, month/year rollovers,
    February 29th.
  - `AlarmSoundTest` (9) — the **persisted sound ids** as a golden map;
    renumbering would silently change what existing alarms play.
  - `BrutusApplicationTest` (8) — notification channels are write-once:
    importance, DND bypass and silence are pinned.
  - `PermissionDeepLinkTest` (7) — the three reliability banners land on the
    right settings page (action + `package:` URI + `NEW_TASK`).
  - `AlarmSoundGeneratorPropertiesTest` (7) — invariants for **every** sound
    (loop length, determinism, DC offset, headroom); new enum entries are
    covered automatically.
  - `RoomSchemaExportTest` (7) — compares the **identity hash of the running
    database with the committed `7.json`**, so a field added without a migration
    fails in CI instead of at 6 a.m. on a user's phone.
  - `AlarmEntityDefaultsTest` (7) + `GlobalQrStoreTest` (6, the code must never
    change or every printout is worthless) + `TimerSoundStoreTest` (6).

### Fixed
- **Widget: "in 1 Tagen" → "in 1 Tag"** — the home-screen countdown had no
  singular form at exactly one day (the in-app countdown always had one).

### Changed
- `formatStopwatch`, `formatCountdown`, `labelForPreset` and the two widget
  formatters are `internal` instead of `private` so the tests can assert the
  strings the user actually reads. No behavior change.
- **14 more unit tests** (total 75): `MathProblemTest` (8 — answer/display logic,
  range and sign invariants of the problem generator per difficulty across 500
  samples, operator/difficulty fallbacks) and `WorldClockStoreTest` (6 — default
  seeding on first launch, round-trips, an empty list does not fall back to the
  defaults, blank filtering; Robolectric).

## [2.1.1] — 2026-07-15 · Card color = enabled state

### Fixed
- **Alarm cards now encode the enabled state, not the next alarm**: previously
  only the alarm ringing next got the full primaryContainer surface — three
  enabled alarms looked three different ways although they were in the same
  state. Now **every enabled** alarm sits on the same muted red (primaryContainer
  mixed toward the surface, calm even with many alarms), **disabled** ones sink
  to gray/dark as before (surfaceContainerLow). The next alarm keeps that color
  and is marked with a thin primary outline instead (the header names it
  anyway).

### Added — unit tests for the ViewModel state machines
- **20 new tests** (total 61): `StopwatchViewModelTest` (6 — segment
  accumulation, laps, reset-undo snapshot incl. single-shot semantics),
  `TimerViewModelTest` (8 — countdown/pause math, cancel-undo state machine, an
  expired timer deliberately not undoable; runs under Robolectric) and
  `AlarmEntityTest` (6 — timeString padding, repeatDaysString cases, weekday
  bitmask, hardcoreEffective).
- For that, both ViewModels gained an **injectable clock** (`now: () -> Long`,
  default `SystemClock::elapsedRealtime`) — no behavioral difference in the app,
  but the timing logic is now deterministically testable on the JVM. New test
  dependencies: kotlinx-coroutines-test, Robolectric, androidx.test:core.

versionCode 15.

## [2.1.0] — 2026-07-08 · Tabular numerals everywhere + undo everywhere

### Changed
- **Every number in stopwatch style**: `fontFeatureSettings = "tnum"` now sits on
  **every** style of the type scale (display → label) — every number in the app
  runs with tabular numerals (countdown header, chips, steppers, sunrise
  countdown, keypad, laps, …), nothing wobbles while ticking. The last unstyled
  number texts (shake/step "/ x") were lifted onto scale styles; redundant local
  `tnum` copies removed.

### Added — undo everywhere
Undo used to exist only for deleting alarms. Now every discarding action is
reversible via snackbar ("Undo", 10 s):
- **World clock**: a removed time zone comes back at its old position.
- **Stopwatch**: reset is reversible — the measurement **including all laps** is
  restored from a ViewModel snapshot.
- **Timer**: aborting a running/paused countdown is reversible — it continues
  with the remaining time (or stays paused). Stopping an **expired** timer is
  deliberately not undoable (it would revive the alarm tone).
- Deleting alarms (single + all) has had undo since v1.9.0.

versionCode 14.

## [2.0.0] — 2026-07-08 · M3 Expressive, phase 4: motion polish & consistency (final)

The Expressive redesign is complete — this closes the 2.0.0-alpha series.

### Changed
- **Direction-aware tab transitions**: switching bottom-nav tabs now runs a
  shared-axis-X hand-over (short spring slide from the side the tab sits on +
  fade-through) instead of a hard cut, driven by the expressive spatial spec.
- **Reduced-motion pass completed**: the wavy timer/shake/step rings flatten to plain
  rings (`amplitude = 0`) when system animations are disabled; breathing
  gradient and pulse hints were already gated in Phase 3.
- **Color-role sweep finished**: UHC task screen migrated (error label,
  tertiary success, primary CTA); the only surviving raw brand colors are
  documented in `Color.kt` — the seed (`BrutusRed`), the alarm wordmark
  (`BrutusRedBright`) and the alarm/UHC gradient core (`BrutusDarkRed`).
  Dead legacy surface/text aliases removed. No stray `Color(0x…)` and no
  custom ripple/indication overrides anywhere in the UI layer.
- versionCode 13, versionName 2.0.0.

## [2.0.0-alpha03] — 2026-07-08 · M3 Expressive, phase 3: screens

### Alarm list (3.1)
- Tonal card hierarchy: **next alarm on primaryContainer**, enabled on
  surfaceContainerHigh, disabled sinks to surfaceContainerLow; display-type
  time (Space Grotesk, `tnum`), animated container color.
- **Swipe-to-delete** (errorContainer reveal) on top of the undo snackbar;
  the delete button stays for TalkBack. Brand-red **FAB with press
  shape-morph**, expressive empty state, `animateItem()` springs,
  role-based chips/banners/weekday pills.

### Alarm edit sheet (3.2)
- Difficulty/sensitivity presets → **SegmentedButtonRow**; challenge chips
  with icons + an order-chain caption; **tonal danger-level rows**
  (errorContainer when a hardcore mode is armed, tertiaryContainer for
  Sunrise); tonal rounded TextField; FilledTonalButtons; sheet on
  surfaceContainerLow.

### Alarm ring & challenges (3.3)
- **Breathing brand gradient** (calm 5s swell; static under reduced motion —
  new `rememberReducedMotion()`), hero clock on displayLarge + `tnum`,
  press-morphing stop CTA.
- **MathChallenge: on-screen keypad** (12 morphing keys, primary confirm) —
  no system keyboard over the alarm anymore.
- Shake/Step rings → **CircularWavyProgressIndicator** (tertiary wave);
  QR frame as expressive squircle with error-flash on wrong scans;
  SwipeToSnooze on tertiary roles with circle→squircle thumb morph.

### Timer & stopwatch (3.4)
- Countdown inside a **wavy ring** that flips to error tones on finish;
  presets → AssistChips; controls as wide tonal/primary button pairs;
  laps as tonal rows with tabular numerals + `animateItem()`.

### World clock (3.5)
- Tonal zone cards with **day/night role indicator** (tertiary sun /
  secondary moon), display-type time, pill search field, globe empty state.

### Sunrise & widget (3.6, 3.7)
- Sunrise: role-tinted labels (tertiary), tabular-numeral hero, primary CTA;
  the physical dawn-warmth gradient stays (deliberate, brightness-coupled).
- Widget: 24dp corners, warm surfaceContainer gradient, text on
  onSurface/outline tones, red tone-80 countdown.

All screens (except the deliberately dark-pinned alarm surfaces) ship
dark + light + dynamic previews.

## [2.0.0-alpha02] — 2026-07-08 · M3 Expressive, phase 2: navigation & chrome

### Changed
- **Bottom navigation → `ShortNavigationBar`** (M3 Expressive): tonal
  `surfaceContainer` background, role-based selection colors (pill indicator in
  `secondaryContainer` — the hardcoded red selection is gone), filled/outlined
  icon swap with a soft spatial spring (`MotionScheme.fastSpatialSpec`).
  Extracted as a previewable composable with dark/light/dynamic previews.
  A `WideNavigationRail` variant was evaluated and skipped: Brutus is a
  portrait phone app (alarm/lock-screen flows), landscape tablets aren't a target.
- Icon `contentDescription` in nav items set to `null` — the always-visible
  label already names the tab; TalkBack no longer announces it twice.

### Fixed (edge-to-edge)
- The four alarm-facing activities (ring, test alarm, sunrise, UHC task) now
  call `enableEdgeToEdge()`: their gradients bleed behind the system bars while
  content respects `safeDrawingPadding()` (cutouts, gesture areas).

## [2.0.0-alpha01] — 2026-07-07 · M3 Expressive, phase 1: foundation

Visual-only redesign onto **Material 3 Expressive** begins. No behavior changes —
ViewModels, Room, scheduler, service, and receivers are untouched.

### Changed
- **Compose BOM `2026.06.01`** (was 2024.12.01), **material3 pinned to
  `1.5.0-alpha18`**: the BOM maps material3 1.4.0 stable, but there the
  Expressive entry points (`MaterialExpressiveTheme`, `MotionScheme`,
  `expressiveLightColorScheme`) are still `internal` — they graduated in the
  1.5.0-alpha channel (theme/motion alpha15–18, wavy indicators alpha18).
  alpha18 is the newest alpha still on Compose 1.11 — alpha19+ pulls
  Compose 1.12, which forces compileSdk 37 + AGP 9.1; not worth the toolchain
  churn for a visual pass. Components graduating later (ButtonGroup,
  FloatingToolbar) are used behind honest `@OptIn(ExperimentalMaterial3ExpressiveApi)`.
  Everything except material3 stays BOM-managed.
- **Full M3 color-role system** from the BrutusRed seed (`#E53935`): all
  primary/secondary/tertiary(+container) roles, error family, the complete
  `surfaceContainerLowest…Highest` tonal ladder, outlines, inverse roles.
  Orange is now the **tertiary** family (Sunrise/Timer accents).
- **Light theme support**: the app follows the system setting
  (`expressiveLightColorScheme`-based light scheme); dark remains the design
  default. Alarm-facing activities (ring, sunrise, UHC task, test alarm) stay
  pinned to dark — their black/red gradients assume light-on-dark content.
- **`MaterialExpressiveTheme`** with `MotionScheme.expressive()` (spatial
  springs) and an expressive shape scale (8/12/16/24/32 dp).
- **Expressive typography** with full type scale: Space Grotesk (variable, OFL)
  for display/headline/title, tabular numerals (`tnum`) on the display styles
  so ticking clocks don't jitter; body/labels stay on the system font.
- Theme previews (dark/light/dynamic specimen) in `ui/theme/ThemePreview.kt`.

### Added
- **Material You dynamic color** as opt-in (API 31+), persisted via DataStore
  (`ThemeSettings`), toggle in the alarm-list overflow menu. Default remains the
  red Brutus brand scheme.
- Bundled font `res/font/space_grotesk.ttf` (SIL Open Font License 1.1).

## [1.9.0] — 2026-07-07
- Undo snackbar for deletions (single + delete-all), restoring alarms incl. scheduling.
- Copy alarms: per-card ⧉ button opens a prefilled "copy alarm" edit sheet.

## [1.8.0] — 2026-07-06
- Bug-fix pass (3 parallel reviews): `goAsync()` in boot/widget receivers,
  overlapping-alarm session takeover, stale-sunrise cancel, Timer/Stopwatch
  state survives tab switches (Activity-scoped ViewModels), QR camera cleanup.
- GUI polish: single-row weekday pills in the edit sheet, pinned save CTA,
  TalkBack action for swipe-to-snooze, world-clock search placeholder.

## [1.7.0] — 2026-07-05
- Redesigned alarm cards (full-width weekday strip, info chips).
- Five extreme alarm sounds (stadium horn, jackhammer, fire alarm, dental drill, banshee).

## [1.6.x and earlier]
- See Git history / GitHub releases.
