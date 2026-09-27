# Screenshot-Liste

[English](SHOTLIST.md) · **Deutsch**

Die Galerie in der README erwartet genau diese zehn Dateien in diesem Ordner. Gleiches
Gerät, gleiches Theme, gleiche Statusleiste für alle — eine Galerie aus drei verschiedenen
Telefonen sieht aus wie drei verschiedene Apps.

## Sprache

Die Galerie in `README.md` zeigt die **englische** Oberfläche, die in `README.de.md`
die deutsche. Beide verweisen auf *dieselben* Dateien — nimm also die Sprache auf,
die du verwendest, und halte das in der Bildunterschrift fest, statt zwei Sätze zu
pflegen. Die App-Sprache lässt sich ab Android 13 unter
*Einstellungen → Apps → Brutus → Sprache* umstellen.

## Hausregeln

| Regel | Warum |
|-------|-------|
| **Dunkles Theme**, Material You **aus** (⋮ → *Einstellungen & Info* → *Material-You-Farben* = aus) | Das Markenrot ist die Signatur der App; hintergrundgetönte Aufnahmen sehen auf jedem Gerät anders aus |
| Ein Gerät für alle Aufnahmen, Hochformat | Einheitliches Seitenverhältnis, einheitliche Dichte |
| Keine echten persönlichen Daten — plausible Labels nehmen (`Arbeit`, `Sport`, `Zug 06:12`) | Screenshots landen in einem öffentlichen Repository |
| Alle drei Zuverlässigkeits-Banner vorher beheben | Sonst dominieren rote Berechtigungswarnungen jedes Bild |
| Statusleiste über den SystemUI-Demo-Modus einfrieren (siehe unten) | Eine springende Uhr oder Akkuanzeige zwischen den Bildern ist das klassische Verräterdetail |
| PNG direkt aus `screencap`, kein Zuschnitt, keine Geräterahmen | Rahmen altern schlecht und kämpfen gegen das Layout der README |

## Statusleiste einfrieren (optional, aber empfohlen)

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0600
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

# …alle Screenshots aufnehmen…

adb shell am broadcast -a com.android.systemui.demo -e command exit
```

## Aufnehmen

```bash
adb exec-out screencap -p > docs/screenshots/01-alarm-list.png
```

## Die zehn Aufnahmen (v2.5.x)

| # | Datei | Ansicht | So kommst du hin | Muss sichtbar sein |
|---|-------|---------|------------------|--------------------|
| 1 | `01-alarm-list.png` | Wecker-Tab | 3–4 plausible Wecker (`Work`, `Gym`, `Weekend`) — warten, bis die Liste geladen ist | Countdown im Kopf, der nächste Wecker umrandet, Wochentagsleiste, Chips inkl. `♪`-Ton |
| 2 | `02-sound-picker.png` | Bearbeiten-Sheet | Weckerkarte antippen, bis *Weckton* scrollen | Die Ton-Chips mit dem gewählten |
| 3 | `03-math-challenge.png` | Klingelnder Wecker | Ultra-Hardcore-Wecker mit Rechnen auslösen (unten), eine Ziffer tippen | `ULTRA HARDCORE MODE`, Aufgaben-Punkte, Tastenfeld, Wischen zum Schlummern |
| 4 | `04-shake-challenge.png` | Schüttel-Aufgabe | Wecker mit Schütteln auslösen, über die Emulator-Konsole schütteln (unten) | Ring **teilweise** gefüllt |
| 5 | `05-sunrise.png` | Sunrise-Vorlauf | `SunriseActivity` starten (unten) und ~2 Min. warten | Morgenrot mitten im Übergang, Countdown zum Hauptwecker |
| 6 | `06-settings.png` | Einstellungen & Info | ⋮ → *Einstellungen & Info* | Darstellung und der Sunrise-Klang |
| 7 | `07-timer-setup.png` | Timer-Tab, bereit | Timer-Tab | Schnellwahl, Endton und der **vollständig sichtbare** Start-Knopf |
| 8 | `08-timer.png` | Laufender Timer | 5 Min. starten, bei etwa 04:xx aufnehmen | Welliger Ring, Abbrechen / Pause |
| 9 | `09-world-clock.png` | Weltuhr | 3 Zonen, eine davon nachts | Tag- und Nacht-Symbole, Versatz |
| 10 | `10-stopwatch.png` | Stoppuhr | Starten, drei Runden, weiterlaufen lassen | Anzeige und Rundenliste |

Danach den README-Streifen bauen: `python3 tools/mockups.py docs/screenshots/mockups.jpg <aufnahme>:"Beschriftung" …`.

### Emulator-Handgriffe (Root-Emulator-Image)

```bash
adb root
# Plausible Uhrzeit: Statusleiste und Gerätezeit passen zu den Weckern
adb shell settings put global auto_time 0 && adb shell "date 092806302026.00"
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0630
# Wecker <id> jetzt auslösen (die App muss seit einem Force-Stop einmal gestartet worden sein)
adb shell am broadcast -n com.pepperonas.brutus/.receiver.AlarmReceiver --el alarm_id <id>
# Die angepinnte Wecker-Benachrichtigung verdeckt bei entsperrtem Bildschirm die Uhr — nach oben wischen
adb shell input swipe 540 270 540 20 250
# Schütteln: ein Stoß alle ~0,6 s zählt als Schütteln
adb emu sensor set acceleration 30:9.8:0; adb emu sensor set acceleration 0:9.8:0
# Sunrise, Hauptwecker in 4 Minuten
adb shell am start -n com.pepperonas.brutus/.SunriseActivity --el alarm_id <id> --el main_trigger_at <epoch-ms>
```

`uiautomator dump` schlägt fehl, solange eine Animation läuft, und lässt den **vorherigen** Dump liegen —
vor dem Übernehmen von Koordinaten einen Screenshot prüfen.
