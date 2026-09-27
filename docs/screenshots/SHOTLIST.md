# Screenshot shot list

**English** · [Deutsch](SHOTLIST.de.md)

The README gallery expects exactly these ten files in this folder. Same device, same
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
| **Dark theme**, Material You **off** (⋮ → *Settings & info* → *Material You colors* = off) | The brand red is the app's signature; wallpaper-tinted shots differ per device |
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

## The ten shots (v2.5.x)

| # | File | Screen | How to get there | Must be visible |
|---|------|--------|------------------|-----------------|
| 1 | `01-alarm-list.png` | Alarm tab | 3–4 plausible alarms (`Work`, `Gym`, `Weekend`) — wait until the list has loaded | Countdown header, the next alarm outlined, weekday strip, info chips incl. `♪` sound |
| 2 | `02-sound-picker.png` | Edit sheet | Tap an alarm card, scroll to *Alarm sound* | The sound chips with the selected one |
| 3 | `03-math-challenge.png` | Ringing alarm | Fire an Ultra Hardcore alarm with math (below), type one digit | `ULTRA HARDCORE MODE`, challenge dots, keypad, slide-to-snooze |
| 4 | `04-shake-challenge.png` | Shake challenge | Fire an alarm with shake, shake via the emulator console (below) | Progress ring **partially** filled |
| 5 | `05-sunrise.png` | Sunrise pre-alarm | Start `SunriseActivity` (below) and wait ~2 min | Dawn gradient mid-ramp, countdown to the main alarm |
| 6 | `06-settings.png` | Settings & info | ⋮ → *Settings & info* | Appearance and the Sunrise sound |
| 7 | `07-timer-setup.png` | Timer tab, idle | Timer tab | Presets, end tone and the **fully visible** Start button |
| 8 | `08-timer.png` | Timer running | Start 5 min, shoot around 04:xx | Wavy ring, Abort / Pause |
| 9 | `09-world-clock.png` | World Clock | 3 zones, one of them at night | Day and night icons, offsets |
| 10 | `10-stopwatch.png` | Stopwatch | Start, take three laps, leave it running | Readout and lap list |

Afterwards build the README strip: `python3 tools/mockups.py docs/screenshots/mockups.jpg <shot>:"Caption" …`.

### Emulator helpers (root emulator image)

```bash
adb root
# Plausible time: status bar and device clock agree with the alarms
adb shell settings put global auto_time 0 && adb shell "date 092806302026.00"
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0630
# Fire alarm <id> now (the app must have been started once since a force stop)
adb shell am broadcast -n com.pepperonas.brutus/.receiver.AlarmReceiver --el alarm_id <id>
# The pinned alarm heads-up covers the clock while the screen is unlocked — swipe it up
adb shell input swipe 540 270 540 20 250
# Shake: one push every ~0.6 s counts as a shake
adb emu sensor set acceleration 30:9.8:0; adb emu sensor set acceleration 0:9.8:0
# Sunrise, main alarm in 4 minutes
adb shell am start -n com.pepperonas.brutus/.SunriseActivity --el alarm_id <id> --el main_trigger_at <epoch-ms>
```

`uiautomator dump` fails while an animation runs and leaves the **previous** dump in place — check a
screenshot before trusting coordinates from it.
