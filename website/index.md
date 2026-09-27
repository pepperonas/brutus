<!--# block name="none" --><!--# endblock -->
# Brutus — Android Alarm Clock You Can't Ignore

> Brutus is a free, open-source (MIT) alarm clock for Android 8.0 and later. A ringing alarm only stops once you complete the challenges you chose for it: solve math problems, shake the phone, or scan a QR code you placed somewhere else in your home — in any combination, one after the other. Hardcore mode keeps the alarm at full volume while it rings; Ultra Hardcore mode re-rings 10 and 15 minutes later unless you walk 30 steps. It synthesizes 25 alarm sounds on the device, notifies you before an alarm and lets you dismiss it early, and it also has a world clock, stopwatch, a timer that rings even with the app closed and a home-screen widget, runs in English and German, has no ads or tracking, and stays offline unless you switch on its update check, which then reads the newest version number once a day and notifies you. Distributed as a signed APK through GitHub Releases.

This is the Markdown version of https://brutus.celox.io/ for agents and text tools. A short summary with every link lives at https://brutus.celox.io/llms.txt.

## Download

- **Newest release:** https://brutus.celox.io/download (picks the file for your platform; always the current release)
- **Current version:** <!--# include virtual="/ssi/version.txt" stub="none" --> · released <!--# include virtual="/ssi/date.txt" stub="none" -->
- **Release data as JSON:** https://brutus.celox.io/latest.json
- **Requirements:** APK: Android 8.0+

Files in the current release:

<!--# include virtual="/ssi/files.md" stub="none" -->

## Features

- **Challenges, not a button** — Math (1–10 problems), shaking (10–100 shakes) and a QR scan. Enable one, two or all three per alarm — they run one after the other, and only then does *Stop alarm* appear.
- **Hardcore mode** — While the alarm rings, its volume stays at maximum and the volume keys do nothing. The moment it is dismissed, your phone behaves normally again.
- **No going back to bed** — Ultra Hardcore rings again 10 and 15 minutes after you dismiss it — unless you walk 30 steps with the phone. The follow-ups survive a reboot.
- **Sounds made on the phone** — 25 alarm sounds are synthesized in real time — 17 harsh ones, from klaxon and air-raid siren to steel hammer, and 8 gentle ones like singing bowl, birdsong and ocean waves — plus the system tone. Preview each one while you set the alarm.
- **Sunrise and widget** — An optional pre-alarm brightens the screen over the ten minutes before it rings, with a gentle sound of your choice. A home-screen widget shows the next alarm with a countdown.
- **Reliable, offline by default** — Exact alarms that ring in Doze and come back after a reboot — even before you unlock the phone, with banners when the system would block them. Online only if you switch on update notices — then it reads the newest version once a day.

### Hear the sounds

#### Harsh — to get you up

- **Klaxon** — Pulsing two-tone alarm ([listen](https://brutus.celox.io/assets/sounds/klaxon.m4a))
- **Nuclear alert** — Fast, sharp beeping ([listen](https://brutus.celox.io/assets/sounds/nuclear.m4a))
- **Piercing** — Ultra-high beep — maximum annoyance ([listen](https://brutus.celox.io/assets/sounds/piercing.m4a))
- **Stadium horn** — Blaring air horn made of detuned sawtooths ([listen](https://brutus.celox.io/assets/sounds/airhorn.m4a))
- **Jackhammer** — Pounding low-end bursts like a construction site ([listen](https://brutus.celox.io/assets/sounds/jackhammer.m4a))
- **Fire alarm** — Temporal-3 smoke-detector pattern at 3.1 kHz ([listen](https://brutus.celox.io/assets/sounds/fire-alarm.m4a))
- **Dental drill** — FM-modulated dental drill — screeching grind ([listen](https://brutus.celox.io/assets/sounds/dentist.m4a))
- **Banshee** — Dissonant, rising cluster of beating tones ([listen](https://brutus.celox.io/assets/sounds/banshee.m4a))
- **Air-raid siren** — Motor siren: winds up, holds, winds down — full of overtones ([listen](https://brutus.celox.io/assets/sounds/air-raid.m4a))
- **Dive alarm** — “A-OO-GA” — a two-step horn blast in quick succession ([listen](https://brutus.celox.io/assets/sounds/dive.m4a))
- **Car alarm** — Changes pattern every 1.5 s: wail, yelp, horn, warble ([listen](https://brutus.celox.io/assets/sounds/car-alarm.m4a))
- **School bell** — Mechanical clapper, about 22 strikes a second, metallic ([listen](https://brutus.celox.io/assets/sounds/school-bell.m4a))
- **Reverse beeper** — Low, hard warning beep that keeps getting faster ([listen](https://brutus.celox.io/assets/sounds/reverse-beeper.m4a))
- **Shepard siren** — A tone that seems to rise forever — restless, never resolves ([listen](https://brutus.celox.io/assets/sounds/shepard.m4a))
- **Steel hammer** — Irregular hammer blows on metal with a ringing tail ([listen](https://brutus.celox.io/assets/sounds/steel-hammer.m4a))
- **Strobe** — High beeps that get faster and higher ([listen](https://brutus.celox.io/assets/sounds/strobe.m4a))
- **Evacuation whoop** — Industrial alarm: fast upward sweep, hard restart ([listen](https://brutus.celox.io/assets/sounds/whoop.m4a))

#### Gentle — for Sunrise and the timer

- **Singing bowl** — Deep, long-ringing bowl tone with a gentle beat ([listen](https://brutus.celox.io/assets/sounds/singing-bowl.m4a))
- **Birdsong** — Quiet chirps, loosely spread, like early morning ([listen](https://brutus.celox.io/assets/sounds/birds.m4a))
- **Wind chimes** — Pentatonic bells, set off at random by the wind ([listen](https://brutus.celox.io/assets/sounds/wind-chimes.m4a))
- **Kalimba** — Thumb-piano motif, round and woody ([listen](https://brutus.celox.io/assets/sounds/kalimba.m4a))
- **Harp** — Rising harp arpeggios over two chords ([listen](https://brutus.celox.io/assets/sounds/harp.m4a))
- **Ocean waves** — A wave that rolls in and draws back ([listen](https://brutus.celox.io/assets/sounds/ocean.m4a))
- **Electric piano** — Calm Rhodes chords, softly struck ([listen](https://brutus.celox.io/assets/sounds/electric-piano.m4a))
- **Daybreak** — A sound pad that breathes and brightens ([listen](https://brutus.celox.io/assets/sounds/sunrise.m4a))

## Install

1. **Download the APK** — One file for every phone with Android 8.0 or later — there is nothing to pick.
2. **Allow the install** — Open the file. Android asks once whether your browser may install apps — allow it.
3. **Clear the banners** — If the alarm list shows a banner — exact alarms, battery optimization, full-screen alarm — tap it and flip the switch. Then test your wake-up chain with *Test wake modes now*.

## Verify

<!--# include virtual="/ssi/checksums.md" stub="none" -->
- Signing certificate SHA-256: `69d67a10a826cf4050da4b271af9b5ed500c962bfab07a8f8fe863e3d7600382`
- Every release since 1.0.0 is signed with the same key. Compare with `apksigner verify --print-certs`.

## FAQ

**Is Brutus free?** Yes. It is free and open source under the MIT licence, with no ads, no account and no tracking.

**Why is it not on Google Play?** It is distributed directly as a signed APK on GitHub Releases. Each release is built from the public source by GitHub Actions, and the workflow refuses to publish an APK that is not signed with the Brutus certificate.

**Which phones does it run on?** Android 8.0 or later. The QR challenge also needs Google Play Services, which supplies the barcode model; math and shake work on every device.

**How do I update?** Switch on *Check for updates* under ⋮ → *Settings & info*: Brutus then looks once a day for a new release and notifies you. Download the APK from this page and install it over the existing app; your alarms and settings stay.

**What does it do besides alarms?** A world clock, a stopwatch with laps, a timer that rings even after you leave the app, and a home-screen widget. Before an alarm a notification counts down and lets you dismiss it early. Theme (light, dark or system) is set under ⋮ → Settings & info. The app is in English and German and follows the system language.

**How do I know the APK is genuine?** Compare its SHA-256 with the value above and check the signing certificate with apksigner verify --print-certs; the certificate is the same for every release since 1.0.0.

## Limits

- Android only; not on Google Play — distributed as an APK through GitHub Releases.
- The QR challenge needs Google Play Services, which delivers the barcode model; math and shake work without it.
- Available in English and German only.
- Android offers no way to truly lock the volume: Hardcore mode snaps it back to maximum and swallows the volume keys while the alarm rings.
- It does not download or install updates by itself — it only tells you, and you install the new APK over the old one.

## Links

- Source code: https://github.com/pepperonas/brutus
- Changelog: https://brutus.celox.io/changelog.md
- Licence (MIT): https://github.com/pepperonas/brutus/blob/main/LICENSE
- Support the project: https://www.paypal.com/donate/?business=martin.pfeffer@celox.io&currency_code=EUR&item_name=Brutus
- Author: Martin Pfeffer, https://celox.io — Imprint https://celox.io/impressum/ · Privacy https://celox.io/datenschutz/
