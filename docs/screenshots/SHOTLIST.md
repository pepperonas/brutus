# Screenshot shot list

**English** · [Deutsch](SHOTLIST.de.md)

The README gallery expects exactly these twelve files in this folder. Same device, same
theme, same status bar for all of them — a gallery whose shots were taken on three
different phones looks like three different apps.

## Language

The gallery in `README.md` shows the **English** UI, the one in `README.de.md`
the German one. Both point at the *same* files — so shoot the language you use
and note it in the caption rather than maintaining two sets. Switch the app
language on Android 13+ via *Settings → Apps → Brutus → Language*.

## House rules

| Rule | Why |
|------|-----|
| **Dark theme**, Material You **off** (⋮ menu → *Material You colors* = off) | The brand red is the app's signature; wallpaper-tinted shots differ per device |
| One device for all shots, portrait | Consistent aspect ratio, consistent density |
| No real personal data — use plausible alarm labels (`Work`, `Gym`, `Train 06:12`) | Screenshots end up in a public repo |
| All three reliability banners resolved before shooting the list | Otherwise every shot is dominated by red permission warnings |
| Status bar frozen via SystemUI demo mode (below) | A jumping clock/battery between shots is the classic giveaway |
| PNG, straight from `screencap`, no cropping, no device frames | Frames age badly and fight the README's own styling |

## Freeze the status bar (optional but recommended)

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0600
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

# …take all screenshots…

adb shell am broadcast -a com.android.systemui.demo -e command exit
```

## Capture

```bash
adb exec-out screencap -p > docs/screenshots/01-alarm-list.png
```

## The twelve shots

| # | File | Screen | How to get there | Must be visible |
|---|------|--------|------------------|-----------------|
| 1 | `01-alarm-list.png` | Alarm tab | Launch the app with **3–4 alarms**, at least one disabled | Countdown header, active cards in the muted red, one gray disabled card, thin outline on the next alarm, weekday strip, info chips |
| 2 | `02-alarm-edit.png` | Edit bottom sheet | Tap an alarm card | Time picker, weekday row, sound picker, challenge toggles, snooze selector, Hardcore switches |
| 3 | `03-alarm-ring.png` | Ringing alarm | Edit sheet → **Test wake modes now** on a Hardcore alarm | Large clock, `HARDCORE MODE` badge, challenge progress dots, slide-to-snooze track, **no** stop button |
| 4 | `04-math-challenge.png` | Math challenge | Test an alarm with math enabled | A problem, the on-screen keypad, the `n / N` progress |
| 5 | `05-shake-challenge.png` | Shake challenge | Test an alarm with shake enabled, shake it about a third of the way | Progress ring **partially** filled — an empty or full ring says nothing about the mechanic |
| 6 | `06-qr-challenge.png` | QR challenge | Test an alarm with QR enabled | Camera preview with the scan frame; point it at the printed code but shoot **before** it matches |
| 7 | `07-world-clock.png` | World Clock tab | Add 2–3 zones so both a day and a night zone are on screen | At least four rows, visibly different day/night treatment |
| 8 | `08-stopwatch.png` | Stopwatch tab | Start, take 3–4 laps, leave it **running** | Large readout mid-run, lap list with both columns |
| 9 | `09-timer.png` | Timer tab | Start a 5 min timer, shoot around 03:xx | Countdown readout plus the two circle buttons |
| 10 | `10-sunrise.png` | Sunrise pre-alarm | Set an alarm ~10 min out with Sunrise on, wait for it | Dawn gradient mid-ramp (not fully black, not fully bright), countdown to the main alarm |
| 11 | `11-widget.png` | Home screen | Place the Brutus widget, then screenshot the **home screen** | Widget with time, countdown and day strip — crop nothing, the home-screen context is the point |
| 12 | `12-ultra-hardcore-task.png` | Anti-snooze task | Dismiss an Ultra Hardcore alarm → notification → *Solve task* | Step counter partway through the 30 steps |

If you skip one, say so — the gallery cell gets removed rather than left pointing at a
missing file.
