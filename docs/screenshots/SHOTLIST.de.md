# Screenshot-Liste

[English](SHOTLIST.md) · **Deutsch**

Die Galerie in der README erwartet genau diese zwölf Dateien in diesem Ordner. Gleiches
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
| **Dunkles Theme**, Material You **aus** (⋮-Menü → *Material You Farben* = aus) | Das Markenrot ist die Signatur der App; hintergrundgetönte Aufnahmen sehen auf jedem Gerät anders aus |
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

## Die zwölf Aufnahmen

| # | Datei | Bildschirm | Wie man hinkommt | Was sichtbar sein muss |
|---|-------|------------|------------------|------------------------|
| 1 | `01-alarm-list.png` | Alarm-Tab | App mit **3–4 Weckern** starten, mindestens einer inaktiv | Countdown-Kopfzeile, aktive Karten im gedämpften Rot, eine graue inaktive Karte, dünne Kontur am nächsten Wecker, Wochentagsleiste, Info-Chips |
| 2 | `02-alarm-edit.png` | Bearbeiten-Sheet | Auf eine Weckerkarte tippen | Zeit-Picker, Wochentagsreihe, Sound-Auswahl, Aufgaben-Schalter, Snooze-Wahl, Hardcore-Schalter |
| 3 | `03-alarm-ring.png` | Klingelnder Wecker | Bearbeiten-Sheet → **Weckmodi jetzt testen** bei einem Hardcore-Wecker | Große Uhr, `HARDCORE MODE`-Badge, Fortschrittspunkte der Aufgaben, Snooze-Bahn, **kein** Stopp-Knopf |
| 4 | `04-math-challenge.png` | Mathe-Aufgabe | Wecker mit aktivierter Mathe-Aufgabe testen | Eine Aufgabe, der Ziffernblock, der Fortschritt `n / N` |
| 5 | `05-shake-challenge.png` | Schüttel-Aufgabe | Wecker mit Schüttel-Aufgabe testen, etwa ein Drittel schütteln | Fortschrittsring **teilweise** gefüllt — ein leerer oder voller Ring sagt nichts über die Mechanik |
| 6 | `06-qr-challenge.png` | QR-Aufgabe | Wecker mit QR-Aufgabe testen | Kamera-Vorschau mit Scan-Rahmen; auf den Ausdruck halten, aber **vor** dem Treffer auslösen |
| 7 | `07-world-clock.png` | Weltuhr-Tab | 2–3 Zonen hinzufügen, sodass eine Tag- und eine Nacht-Zone sichtbar sind | Mindestens vier Zeilen, sichtbar unterschiedliche Tag-/Nacht-Darstellung |
| 8 | `08-stopwatch.png` | Stoppuhr-Tab | Starten, 3–4 Runden nehmen, **laufen lassen** | Große Anzeige mitten im Lauf, Rundenliste mit beiden Spalten |
| 9 | `09-timer.png` | Timer-Tab | 5-Minuten-Timer starten, bei etwa 03:xx auslösen | Countdown-Anzeige plus die beiden Kreisknöpfe |
| 10 | `10-sunrise.png` | Sunrise-Vorlauf | Wecker ~10 min in die Zukunft mit Sunrise setzen und abwarten | Morgenverlauf mitten in der Rampe (weder ganz schwarz noch ganz hell), Countdown zum Hauptalarm |
| 11 | `11-widget.png` | Homescreen | Brutus-Widget platzieren, dann den **Homescreen** aufnehmen | Widget mit Uhrzeit, Countdown und Tagesleiste — nichts zuschneiden, der Homescreen-Kontext ist der Punkt |
| 12 | `12-ultra-hardcore-task.png` | Anti-Schlummer-Aufgabe | Ultra-Hardcore-Wecker verwerfen → Notification → *Aufgabe lösen* | Schrittzähler mitten in den 30 Schritten |

Wenn eine Aufnahme entfällt, sag Bescheid — dann fliegt die Zelle aus der Galerie, statt
auf eine fehlende Datei zu zeigen.
