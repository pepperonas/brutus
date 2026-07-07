# Changelog

All notable changes to Brutus are documented here. Versions follow [SemVer](https://semver.org).

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
