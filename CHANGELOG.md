# Changelog

All notable changes to Brutus are documented here. Versions follow [SemVer](https://semver.org).

## [2.1.1] — 2026-07-15 · Card-Farbe = Aktiv-Zustand

### Fixed
- **Alarm-Cards kodieren jetzt den Aktiv-Zustand, nicht den nächsten Alarm**:
  vorher bekam nur der als Nächstes klingelnde Wecker die volle
  primaryContainer-Fläche — drei aktive Wecker sahen dreifach unterschiedlich
  aus, obwohl sie im selben Zustand waren. Jetzt liegen **alle aktiven** Wecker
  auf demselben gedämpften Rot (primaryContainer zur Surface hin abgemischt,
  ruhig auch bei vielen Alarmen), **inaktive** sinken wie bisher auf
  grau/dunkel (surfaceContainerLow). Der nächste Alarm behält seine Farbe und
  wird stattdessen durch eine schmale primary-Outline markiert (der Header
  nennt ihn ohnehin).

versionCode 15.

## [2.1.0] — 2026-07-08 · Tabellarische Ziffern überall + Undo überall

### Changed
- **Alle Zahlen im Stoppuhr-Look**: `fontFeatureSettings = "tnum"` sitzt jetzt
  auf **jedem** Stil der Type-Scale (display → label) — jede Zahl in der App
  läuft mit tabellarischen Ziffern (Countdown-Header, Chips, Steppers,
  Sunrise-Countdown, Keypad, Runden, …), nichts wackelt mehr beim Ticken.
  Die letzten stillosen Zahlen-Texte (Shake-/Step-"/ x") auf Scale-Stile
  gehoben; redundante lokale `tnum`-Kopien entfernt.

### Added — Undo überall
Bisher gab es Undo nur fürs Löschen von Alarmen. Jetzt ist jede verwerfende
Aktion per Snackbar ("Rückgängig", 10 s) umkehrbar:
- **Weltuhr**: entfernte Zeitzone kommt an ihrer alten Position zurück.
- **Stoppuhr**: Reset ist umkehrbar — Messung **inkl. aller Runden** wird aus
  einem ViewModel-Snapshot wiederhergestellt.
- **Timer**: Abbruch eines laufenden/pausierten Countdowns ist umkehrbar —
  läuft mit der Restzeit weiter (bzw. bleibt pausiert). Das Stoppen eines
  **abgelaufenen** Timers ist bewusst nicht undoable (würde den Alarmton
  wiederbeleben).
- Alarm-Löschen (einzeln + alle) hatte Undo bereits seit v1.9.0.

versionCode 14.

## [2.0.0] — 2026-07-08 · M3 Expressive, Phase 4: Motion-Polish & Konsistenz (final)

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

## [2.0.0-alpha03] — 2026-07-08 · M3 Expressive, Phase 3: Screens

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
  with icons + "Reihenfolge"-chain caption; **tonal danger-level rows**
  (errorContainer when a hardcore mode is armed, tertiaryContainer for
  Sunrise); tonal rounded TextField; FilledTonalButtons; sheet on
  surfaceContainerLow.

### Alarm ring & challenges (3.3)
- **Breathing brand gradient** (calm 5s swell; static under reduced motion —
  new `rememberReducedMotion()`), hero clock on displayLarge + `tnum`,
  press-morphing ALARM STOPPEN CTA.
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

## [2.0.0-alpha02] — 2026-07-08 · M3 Expressive, Phase 2: Navigation & Chrome

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

### Fixed (Edge-to-Edge)
- The four alarm-facing activities (ring, test alarm, sunrise, UHC task) now
  call `enableEdgeToEdge()`: their gradients bleed behind the system bars while
  content respects `safeDrawingPadding()` (cutouts, gesture areas).

## [2.0.0-alpha01] — 2026-07-07 · M3 Expressive, Phase 1: Fundament

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
- Copy alarms: per-card ⧉ button opens a prefilled "Alarm kopieren" edit sheet.

## [1.8.0] — 2026-07-06
- Bug-fix pass (3 parallel reviews): `goAsync()` in boot/widget receivers,
  overlapping-alarm session takeover, stale-sunrise cancel, Timer/Stopwatch
  state survives tab switches (Activity-scoped ViewModels), QR camera cleanup.
- GUI polish: single-row weekday pills in the edit sheet, pinned save CTA,
  TalkBack action for swipe-to-snooze, world-clock search placeholder.

## [1.7.0] — 2026-07-05
- Redesigned alarm cards (full-width weekday strip, info chips).
- Five extreme alarm sounds (Stadion-Horn, Presslufthammer, Feueralarm, Bohrer, Banshee).

## [1.6.x and earlier]
- See Git history / GitHub releases.
