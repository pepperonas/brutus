# Brutus

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Gradle%208.11-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Room](https://img.shields.io/badge/Room-2.6.1-FF6F00)](https://developer.android.com/training/data-storage/room)
[![CameraX](https://img.shields.io/badge/CameraX-1.4.1-00BCD4)](https://developer.android.com/training/camerax)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-Barcode-EA4335?logo=google&logoColor=white)](https://developers.google.com/ml-kit/vision/barcode-scanning)
[![ZXing](https://img.shields.io/badge/ZXing-3.5.3-000000)](https://github.com/zxing/zxing)
[![Material Design](https://img.shields.io/badge/Material%20Design%203-Dark%20Theme-757575?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![minSdk](https://img.shields.io/badge/minSdk-26-green)](https://apilevels.com)
[![targetSdk](https://img.shields.io/badge/targetSdk-35-green)](https://apilevels.com)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)

> **The alarm clock that makes sure you actually wake up.**

Brutus is an Android alarm clock designed for heavy sleepers. It forces you to complete a challenge before the alarm stops — no cheating, no snoozing your way back to sleep.

---

## Screenshots

| Alarm List | Edit Alarm | Math Challenge | Shake Challenge |
|:---:|:---:|:---:|:---:|
| Dark themed alarm list with toggle switches | Bottom sheet with time picker and options | Solve 3 math problems to dismiss | Shake your phone 30 times |

---

## Features

### Three Wake-Up Challenges — Combinable

| Mode | Description |
|------|-------------|
| **Math Challenge** | Solve 3 randomly generated math problems (multiplication, addition, subtraction) |
| **Shake-to-Stop** | Shake your phone 30 times with a visual progress ring |
| **QR-Code Scan** | Scan a specific QR code (e.g. taped to your bathroom mirror) via ML Kit |

**Combinations are the key Brutus feature**: enable two or three challenges and you must complete them all in sequence. Maximum brutality: Shake → Math → QR.

### Six Alarm Sounds — All Brutal

| Sound | Character |
|-------|-----------|
| **System-Alarm** | Android default alarm tone (fallback) |
| **Klaxon** | Pulsing two-tone 600/900 Hz square wave |
| **Sirene** | Sweeping 400–1200 Hz sine siren |
| **Nuclear Alert** | Rapid sharp 1 kHz beeps |
| **Nebelhorn** | Low 120 Hz droning pulse with modulation |
| **Durchdringend** | 3.5 kHz piercing square wave — maximally annoying |

All synthesized tones are generated procedurally via `AudioTrack` with `USAGE_ALARM`, seamlessly looped, and played at maximum `STREAM_ALARM` volume (overrides silent mode).

### QR Code Generator
Built-in QR code generator — create a unique code, print it, stick it somewhere far from your bed. The alarm won't stop until you scan it.

### Alarm Features
- **Exact alarm scheduling** via `AlarmManager.setAlarmClock()` — works in Doze mode
- **Repeating alarms** with individual weekday selection (Mon–Sun)
- **Configurable snooze** (5 / 10 / 15 minutes)
- **Maximum volume** override — sets `STREAM_ALARM` to max, ignores silent mode
- **Continuous vibration** pattern until dismissed
- **System alarm sound** with `USAGE_ALARM` audio attributes

### Lock Screen Overlay
- Full-screen alarm activity over the lock screen
- Screen turns on and stays on
- Keyguard dismissal — no unlock needed to see the alarm
- Back button disabled — must complete the challenge

### Reliability
- **Foreground Service** with persistent notification
- **WakeLock** prevents the device from sleeping during alarm
- **Boot receiver** re-schedules all alarms after device restart
- Room database persistence for all alarm configurations

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Repository) |
| Database | Room |
| Camera | CameraX |
| Barcode scanning | Google ML Kit |
| QR generation | ZXing |
| Background | Foreground Service + WakeLock |
| Scheduling | AlarmManager (setAlarmClock) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Precise alarm timing (Android 12+) |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |
| `WAKE_LOCK` | Keep CPU awake during alarm |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after reboot |
| `CAMERA` | QR code scanning challenge |
| `VIBRATE` | Vibration during alarm |
| `USE_FULL_SCREEN_INTENT` | Lock screen alarm overlay |
| `FOREGROUND_SERVICE` | Alarm playback service |

---

## Project Structure

```
app/src/main/java/com/pepperonas/brutus/
├── MainActivity.kt              # Main entry, alarm list
├── AlarmActivity.kt             # Lock screen overlay activity
├── BrutusApplication.kt         # App init, notification channels
├── receiver/
│   ├── AlarmReceiver.kt         # Handles alarm trigger broadcast
│   └── BootReceiver.kt         # Re-schedules alarms on boot
├── service/
│   └── AlarmService.kt          # Foreground service: audio, vibration, wake lock
├── scheduler/
│   └── AlarmScheduler.kt        # AlarmManager wrapper with setAlarmClock()
├── data/
│   ├── AlarmEntity.kt           # Room entity with all alarm config
│   ├── AlarmDao.kt              # Room DAO
│   ├── AlarmDatabase.kt         # Room database singleton
│   └── AlarmRepository.kt       # Data layer abstraction
├── viewmodel/
│   └── AlarmViewModel.kt        # UI state management
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # Brutus color palette
│   │   ├── Type.kt              # Typography
│   │   └── Theme.kt             # Dark theme config
│   ├── screens/
│   │   ├── AlarmListScreen.kt   # Main screen with alarm cards
│   │   └── AlarmEditDialog.kt   # Bottom sheet: time, days, challenge, QR
│   └── alarm/
│       ├── AlarmScreen.kt       # Full-screen alarm overlay UI
│       ├── MathChallenge.kt     # Math problem challenge
│       ├── ShakeChallenge.kt    # Accelerometer shake challenge
│       └── QrChallenge.kt       # Camera + ML Kit QR scanner
└── util/
    └── QrGenerator.kt           # ZXing QR code bitmap generator
```

---

## Build

```bash
# Clone
git clone https://github.com/pepperonas/brutus.git
cd brutus

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## How It Works

1. **Set an alarm** — pick a time, choose repeat days, select your challenge mode
2. **For QR mode** — generate a QR code in the app, print it, tape it to your bathroom mirror
3. **When the alarm fires** — a full-screen overlay appears over the lock screen at maximum volume
4. **Complete the challenge** — solve math problems, shake your phone, or scan your QR code
5. **Only then** the alarm stops

There is no easy way out. That's the point.

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
