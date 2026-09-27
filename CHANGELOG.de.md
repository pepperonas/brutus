# Changelog

[English](CHANGELOG.md) · **Deutsch**

Alle nennenswerten Änderungen an Brutus stehen hier. Versionen folgen [SemVer](https://semver.org).

## [2.5.1] — 2026-09-27 · Der Start-Knopf des Timers ist wieder erreichbar

### Behoben
- **Timer:** Mit acht sanften Endtönen wurde die Auswahl zwei Reihen höher und drückte den **Start**-Knopf auf
  einem 1080×2400-Handy zu einem Streifen am unteren Rand zusammen — der Timer-Bildschirm scrollte nicht.
  Schnellwahl und Töne scrollen jetzt, Start bleibt angeheftet und vollständig sichtbar.
- **Weckerliste:** Ein Kaltstart zeigte kurz *Noch kein Alarm — Alarm erstellen* und *Kein Alarm aktiv*,
  obwohl Wecker existierten — die Liste begann als „leer“, bevor die Datenbank geantwortet hatte. Jetzt bleibt
  sie bis zur ersten Antwort still.

### Geändert
- README mit dem neuen Banner, einem Screenshot-Streifen und einer Galerie von v2.5 sowie Badges für Version,
  Unit-Tests und Codezeilen, die ein Test ehrlich hält; die Produktseite zeigt das Banner im Hero.

### Tests
- 350 → 371: die Weckerliste unterscheidet „nicht geladen“ von „leer“, was jeden neuen Ton ausmacht (die
  Luftschutzsirene steigt, der Rückfahrwarner wird schneller, das Meer schwillt an …), und die Zahlen und
  Bilder im README gegen den Quellcode. Jede neue Sperre wurde einmal wieder ausgebaut, um ihren Test
  scheitern zu sehen.

## [2.5.0] — 2026-09-27 · 25 Töne, nach Gehör ausgewählt, und ein Sunrise nach Wahl

### Neu — Töne
- **Neun harte Töne:** Luftschutzsirene, U-Boot-Tauchalarm („A-OO-GA“), Autoalarm (wechselt alle 1,5 s
  das Muster), Schulglocke, ein Rückfahrwarner, der immer schneller wird, eine Shepard-Sirene, die
  scheinbar endlos steigt, Stahlhammer, Stroboskop (schneller *und* höher) und ein Evakuierungs-Whoop.
- **Acht sanfte Töne**, gebaut für Sunrise und den Timer: Klangschale, Vogelgezwitscher, Windspiel,
  Kalimba, Harfe, Meeresrauschen, E-Piano und Sonnenaufgang. Sie sind **exakt periodisch** — ausklingende
  Töne laufen an den Schleifenanfang, Dauertöne haben eine ganzzahlige Periodenzahl pro Schleife —, zehn
  Minuten Sunrise knacken also nie an der Naht.
- Nach Gehör aus zwanzig Kandidaten ausgewählt, gerendert von genau dem Code, den die App ausführt; jeder
  übernommene Ton wurde Bit für Bit gegen die angehörte Datei geprüft.
- **Alle 25 auf der Produktseite anhören** — brutus.celox.io/#sounds.

### Neu — Sunrise-Klang
- **⋮ → Einstellungen & Info → Sunrise:** wähle, was der Vorlauf spielt — einen sanften Ton oder Stille
  (nur Licht). Vorher spielte er immer das Glockenspiel. Standard: Sonnenaufgang.

### Entfernt
- **Sirene, Glockenspiel, Marimba und Morgensonne.** Wecker, Timer und Sunrise können noch einen davon
  gespeichert haben: diese Ids sind stillgelegt, werden nie neu vergeben und klingeln mit einem Nachfolger
  gleichen Charakters — Sirene → Luftschutzsirene, Glockenspiel → Windspiel, Marimba → Kalimba,
  Morgensonne → Sonnenaufgang — statt auf den System-Ton zurückzufallen. Der Wecker-Editor zeigt den
  Nachfolger als ausgewählt. Der Timer startet jetzt mit Windspiel.

### Tests
- 339 → 350: die stillgelegten Ids und ihre Nachfolger, die Sunrise-Einstellung (nur sanft, Rückfälle), ein
  Naht-Wächter für sanfte Schleifen und ein optionaler WAV-Export (in CI übersprungen). Jede neue Sperre
  wurde einmal wieder ausgebaut, um ihren Test scheitern zu sehen.

## [2.4.0] — 2026-09-26 · Einstellungen & Info, Benachrichtigungen, ein echter Timer und Motion mit Physik

### Neu — Einstellungen & Info
- **⋮ → Einstellungen & Info**, aufgebaut wie die Einstellungen von Flipper the Ripper: Design **System /
  Hell / Dunkel** (vorher folgte Brutus immer dem System), Material-You-Farben, der Vorlauf des Hinweises,
  die Update-Prüfung (aus dem ⋮-Menü hierher umgezogen) und **Über Brutus** — Version und Build, Autor,
  Links zu Website, celox.io, Quellcode und MIT-Lizenz, die Lizenz der Schrift Space Grotesk (OFL 1.1,
  bisher nur im Repository) und ein PayPal-Spendenknopf.

### Neu — Benachrichtigungen
- **Hinweis vor dem Wecker** (Aus / 30 / 60 / 120 Min., Standard 60) mit Countdown und **Vorzeitig
  beenden**, das diese eine Auslösung überspringt. Ein Hardcore-Wecker lässt sich nur über seine eigenen
  Challenges vorzeitig beenden — nie per Knopf. Die übersprungene Auslösung übersteht Neustarts; Kopfzeile
  und Widget zeigen die nächste.
- **Snooze-Countdown** — „Schlummert — klingelt wieder um 07:05“, mit „Snooze beenden“ bei normalen Weckern.
- **Countdown in der Ultra-Hardcore-Erinnerung** bis zum nächsten Re-Alarm.
- **Verpasster Wecker** — nach einem Neustart oder Uhrzeitsprung ein Hinweis auf einen Wecker, der fällig
  war, während das Handy aus war.

### Geändert — der Timer ist ein echter Wecker
- Der Countdown lebte nur im Bildschirm: Mit Zurück verlassen endete er stillschweigend, im Hintergrund
  klingelte er spät oder nie. Jetzt wird sein Zustand gespeichert, AlarmManager weckt das Handy am Ende,
  ein Vordergrund-Service klingelt, und eine Benachrichtigung zeigt den Countdown mit Pause / Weiter /
  Abbruch (die klingelnde hat Stopp). Auf dem Emulator geprüft: App verlassen, Prozess beendet, klingelte pünktlich.

### Geändert — Motion und Physik nach Material 3 Expressive
- Jede Animation holt ihre Spezifikation jetzt aus dem `MotionScheme` des Themes statt aus handgewählten Tweens.
- **Snooze-Daumen** mit echter Wurfphysik: die Feder beim Loslassen übernimmt die Geschwindigkeit des
  Fingers, Haptik-Tick an der 85-%-Schwelle; weiterhin nur positionsabhängig, ein Wisch schlummert nie versehentlich.
- **Wischen zum Löschen** folgt dem Finger (Farbe, wachsender Mülleimer, Tick, sobald Loslassen löschen würde).
- **Knopfgruppen** bei Timer und Stoppuhr (der gedrückte Knopf wird breiter, der Nachbar weicht aus),
  Beschriftungen, die zum neuen Wort rollen, Zahlen, die wie ein Zählwerk rollen, ein Hüpfer pro gezähltem
  Schütteln und ein federndes Wackeln mit Vibration bei einer falschen Rechnung.
- Bottom-Sheets **gleiten** beim Speichern jetzt hinaus, statt zu verschwinden; Banner, Liste ↔
  Leerzustand, der „Geschafft!“-Moment und der Stopp-Knopf animieren, statt zu springen;
  `LoadingIndicator`, solange die Kamera startet, `LinearWavyProgressIndicator` für Sunrise, ein fließend
  leerlaufender Timer-Ring.
- Der Alarmbildschirm wird für seinen atmenden Hintergrund **nicht mehr 60-mal pro Sekunde neu aufgebaut**,
  und das Pulsieren des Snooze-Hinweises zeichnet nur noch neu.
- „Animationen entfernen“ wird bei jeder Rückkehr in die App neu gelesen; ist es an, blenden Tab-Wechsel über statt zu gleiten.
- Das README behauptete `ButtonGroup` und `FloatingToolbar`; beides war nicht im Einsatz. `ButtonGroup` ist
  es jetzt, die Behauptung zu `FloatingToolbar` ist raus.

### Behoben
- Ein Ultra-Hardcore-Wecker, der im Bruchteil einer Sekunde vor dem Laden seines Tons beendet — oder
  in diesem Moment von einem zweiten Wecker übernommen — wurde, stellte keine Re-Alarme scharf: der
  Service kannte „Ultra Hardcore“ nur aus einem Merker, der erst nach dem Laden gesetzt wird. Jetzt
  fragt er die Datenbank. Gefunden durch einen Test, der unter Last rot wurde.

### Tests
- 33 neue Unit-Tests (306 → 339): Benachrichtigungen und vorzeitiges Beenden (inkl. Hardcore-Sperre),
  verpasste Wecker, der Timer-Controller, Info-Links und die mitgelieferte Lizenz, Design-Modus,
  Snooze-Schwelle. Jede neue Sperre wurde einmal wieder ausgebaut, um ihren Test scheitern zu sehen.

## [2.3.1] — 2026-09-26 · Klingelt vor dem ersten Entsperren, und ein Dutzend weitere Korrekturen

Eine Durchsicht des gesamten Alarm-Pfads fand Fehler, von denen jeder einzelne jemanden verschlafen
lassen konnte. Alle sind behoben und mit Tests abgedeckt; die zwei, die Unit-Tests nicht sehen, wurden
auf einem Emulator nachgestellt und abgenommen.

### Behoben
- **Wecker klingeln nach einem Neustart jetzt auch, bevor das Handy entsperrt ist.** Alles, was der
  Klingel-Pfad liest — Wecker, Ultra-Hardcore-Re-Alarme, Snoozes, der QR-Code — liegt jetzt im
  geräteverschlüsselten Speicher, und Receiver, Service und Alarmbildschirm sind direct-boot-fähig.
  Vorher konnte der gesperrte Boot-Durchlauf die Datenbank nicht lesen, und seit v2.3.0 stürzte der
  Prozess beim Start ab; bis zum ersten Entsperren war kein Wecker registriert. Vorhandene Daten
  ziehen einmalig um, sobald die App zum ersten Mal entsperrt läuft. Auf dem Emulator geprüft: Neustart
  mit PIN, nicht entsperrt, Alarm klingelte pünktlich, Challenge gelöst, Lautstärke zurückgestellt.
- **Der klingelnde Service stirbt auf einem gesperrten Handy nicht mehr eine Sekunde nach Alarmbeginn.**
  Er aktualisierte das Widget, und jeder Widget-Aufruf wirft vor dem ersten Entsperren. Auf dem Emulator
  gefunden; im gesperrten Zustand bleibt das Widget jetzt unangetastet.
- **Snooze eines Ultra-Hardcore-Weckers stellt die zwei Re-Alarme nicht mehr scharf.** Snooze lief
  über den Beenden-Pfad; ein geschlummerter Wecker klingelte dreimal und verlangte die Schrittaufgabe.
- **Ein Neustart löscht die Re-Alarme eines Ultra-Hardcore-Einmal-Weckers nicht mehr** (der Wecker
  schaltet sich beim Auslösen selbst aus; gesucht wurde nur unter aktiven). Auch die
  Erinnerungs-Benachrichtigung — der Weg zur Schrittaufgabe — kommt nach einem Neustart zurück.
- **Ein Zeitzonenwechsel hält den Wecker auf seiner Wanduhrzeit.** Registrierungen sind absolute
  Zeitpunkte; ein in Berlin gestellter 07:00-Wecker klingelte in London um 06:00. Ein neuer Receiver
  registriert bei Zeitzonen- und Uhrzeitwechseln, wieder erteilter Exakt-Alarm-Berechtigung (ein
  Entzug löscht alle Wecker) und App-Updates alles neu; die App tut es zusätzlich bei jedem Öffnen,
  was Force-Stop und Backup-Wiederherstellung abdeckt.
- **Ein zweiter Wecker, der während eines klingelnden auslöst, übernimmt jetzt den Bildschirm.** Der
  Alarmbildschirm zeigte weiter die Challenges des ersten; Snooze schlummerte den falschen Wecker, und
  die Challenge des neuen wurde übersprungen.
- **Snooze hat eine eigene Registrierung.** Er ersetzt nicht mehr die nächste reguläre Auslösung, wird
  über Neustarts gemerkt, und ein geschlummerter Re-Alarm bleibt ein Re-Alarm.
- **Die Alarm-Lautstärke wird auch zurückgestellt, wenn der Service mitten im Klingeln beendet wird**
  (sie lag nur im Speicher), und ein scheiternder Systemton oder Audio-Track fällt auf einen anderen
  Ton zurück, statt die Sitzung zu beenden.
- **Android 12/12L mit entzogenen exakten Alarmen:** Das Registrieren wirft im klingelnden Service
  keine Ausnahme mehr, sondern weicht auf ein ungenaues Wecken aus, bis die Berechtigung zurück ist.
- **Sunrise** startet jetzt über eine Vollbild-Benachrichtigung (ein Hintergrundstart aus dem Receiver
  ist ab Android 10 blockiert), das Glockenspiel verstummt, wenn der echte Alarm beginnt, und „Alarm
  stoppen“ ist bei Hardcore-Weckern ausgeblendet — ein Tipp zehn Minuten vorher entschärft sie nicht mehr.
- **Der Testmodus stellt die Alarm-Lautstärke** nach dem Test eines Hardcore-Weckers wieder zurück.
- Ausschalten oder Löschen eines Weckers entfernt auch seine Ultra-Hardcore-Erinnerung und einen
  laufenden Snooze.
- Der Update-Check zählt eine Version erst als gemeldet, wenn die Benachrichtigung wirklich gezeigt
  wurde, und startet beim Einschalten nicht mehr zwei Prüfungen gleichzeitig.

### Neu
- Banner über der Weckerliste, wenn Benachrichtigungen blockiert sind und solange
  Ultra-Hardcore-Re-Alarme ausstehen (die Erinnerungs-Benachrichtigung ist ab Android 14 wegwischbar).

### Tests
- 28 neue Unit-Tests (278 → 306): Speicher-Umzug (der ausgedruckte QR-Code überlebt ihn), Neuplanung
  nach Zeitzonenwechsel, Wiederherstellung von Re-Alarmen und Snoozes, Snooze vs. Beenden im echten
  Service, Manifest-Filter und Direct-Boot-Fähigkeit, die Widget-Sperre. Jeder Fix wurde einmal wieder
  eingebaut, um seinen Test scheitern zu sehen.

## [2.3.0] — 2026-09-26 · Update-Hinweis (opt-in) + Produktseite

### Neu — Update-Hinweis (opt-in)
- **⋮ → Nach Updates suchen** (standardmäßig aus, auch nach dem Update). Eingeschaltet fragt Brutus
  `https://brutus.celox.io/latest.json` einmal sofort und dann einmal täglich ab (WorkManager, nur mit
  Netz; GitHubs Releases-API als Ersatz) und meldet **eine** Benachrichtigung pro neuer Version über
  den neuen Kanal *App-Updates* (normale Wichtigkeit, nie durch „Nicht stören“). Ein Banner über der
  Weckerliste bietet den Download an, bis die neue Version installiert ist. Ausgeschaltet werden alle
  geplanten Prüfungen gelöscht, und keine Anfrage verlässt das Handy.
- Berechtigung `INTERNET` hinzugefügt — nur für diese Prüfung. README, Berechtigungstabelle und
  Produktseite sagen jetzt „standardmäßig offline“ statt „kein Internetzugriff“.
- 35 neue Unit-Tests (243 → 278): Versionsvergleich, beide JSON-Formate, Einmal-Benachrichtigung, Planung, der
  neue Benachrichtigungskanal — und ein Pin für einen Fehler, den der Emulator fand: der Änderungs-Flow sendete
  `Unit`, das `collectAsState` verschluckt, der Schalter blieb auf dem Bildschirm „aus“.

### Neu
- **Produktseite unter [brutus.celox.io](https://brutus.celox.io)** auf Deutsch, Englisch, Spanisch, Italienisch und Französisch, gebaut mit dem celox-Produktseiten-Kit. Sie bietet immer die neueste Version an — ein Server-Timer spiegelt GitHub Releases alle 15 Minuten — samt SHA-256, Signaturzertifikat, Changelog und `/download` als festem Link. Der Quelltext liegt in `website/`.

## [2.2.0] — 2026-08-15 · Englisch + Deutsch, und 168 weitere Tests

### Hinzugefügt — Lokalisierung Englisch und Deutsch
- **Die App gibt es jetzt in zwei Sprachen.** Jeder sichtbare String ist aus dem
  Kotlin-Code in Ressourcen gewandert: `values/strings.xml` (Englisch, der neue
  Standard) und `values-de/strings.xml` (Deutsch, vollständige Übersetzung der
  bisherigen Formulierungen). Je ~180 Strings, 6 Plurale und ein Wochentags-Array
  — keine Teilabdeckung, kein stiller Rückfall.
- **Sprache pro App ab Android 13** über `res/xml/locales_config.xml`, sodass
  Brutus auf einem deutschen Telefon englisch laufen kann und umgekehrt.
- **Echte Plurale statt Stringklebe** (`countdown_days`, `widget_in_days`,
  `shake_remaining`, `step_remaining`, `alarm_deleted`, `alarm_delete_all_body`).
- **Lokalisierte Datumsformate**, nicht nur Wörter: Die Nächster-Alarm-Zeile und
  das Weltuhr-Datum stehen als `Mo, 17. Aug, 06:30` bzw. `Mon, Aug 17, 06:30`,
  weil das Format selbst eine String-Ressource ist. Das hartcodierte
  `Locale.GERMAN` in Weckerliste und Widget ist weg.
- Auch Notification-Kanalnamen, der Betreff im Share-Sheet, Bedienhilfen-Labels
  und der Vorschautext des Widgets sind Ressourcen.

### Hinzugefügt — 24 weitere Unit-Tests (219 → 243)
- `ResourceParityTest` (11) — die beiden Sprachen können nicht auseinanderlaufen:
  identische Schlüsselsätze, keine leeren Werte, **passende Format-Platzhalter**
  (ein verlorenes `%1$s` crasht mit `IllegalFormatException` nur in einer
  Sprache), vollständige Plurale, je sieben Wochentage, kein Deutsch im
  Standardsatz und `locales_config.xml` im Einklang mit den `values-*`-Ordnern.
- `LocalizedRuntimeTest` (7) — löst jeden deklarierten String über das
  Ressourcensystem in beiden Sprachen auf und rendert je eine vollständige
  Weckerkarte.
- Die Suites, die bisher deutsche Strings festnagelten (`AlarmEntityTest`,
  `ChallengeFlagsTest`, `NextAlarmCalculatorTest`, `NextAlarmWidgetFormatTest`,
  `AlarmSoundTest`, `ChallengeDifficultyTest`), prüfen jetzt **beide** Sprachen
  über den neuen Test-Helfer `LocaleContexts`.

### Geändert
- `AlarmSound` trägt `@StringRes labelRes` / `descriptionRes` statt literaler
  Anzeigenamen; `ChallengeDifficulty` liefert Ressourcen-Ids;
  `ChallengeFlags.describe`, `AlarmEntity.repeatDaysString` / `soundName` /
  `challengeName`, `NextAlarmCalculator.formatCountdown` und die beiden
  Widget-Formatierer nehmen einen `Context` entgegen. Die persistierten
  numerischen Ids bleiben unangetastet.
- `ChallengeFlags.describe` liefert für ein unbekanntes Flag-Bit keinen leeren
  String mehr, sondern fällt auf „Keine" / „None" zurück — ein korrupter oder
  zukünftiger Wert kann so keinen leeren Chip erzeugen.

### Hinzugefügt — 144 Unit-Tests davor (75 → 219)
Zwölf neue Suites entlang der bisher ungetesteten Pfade:
- `AlarmSchedulerTest` (23, Robolectric/`ShadowAlarmManager`) — was
  **tatsächlich in AlarmManager landet**: Trigger auf der konfigurierten
  Uhrzeit und in der Zukunft, verstrichene Zeit rutscht auf morgen,
  Wochentags-Treffer, Sunrise exakt 10 min davor und nur solange er noch in der
  Zukunft liegt, `setExactAndAllowWhileIdle` (Doze), **Regressionsschutz für
  den Stale-Sunrise-Fix aus v1.8.0**, Snooze-Intervalle und die
  Request-Code-Trennung: Hauptalarm + Sunrise + zwei Re-Alarme desselben Alarms
  koexistieren, ohne sich gegenseitig zu überschreiben.
- `AlarmDaoTest` (15, In-Memory-Room) — echtes SQL statt Mock: Sortierung,
  `getEnabledAlarms` fürs Boot-Rescheduling, Vollfeld-Roundtrip,
  REPLACE-Konflikt, Undo-Restore mit `id = 0`, Repository-Durchreiche.
- `ClockFormattingTest` (14) — Stoppuhr-/Timer-Ablesungen inkl. **konstanter
  Stringbreite** (Grundlage der Tabellenziffern aus v2.1.0), Trunkierung statt
  Aufrunden, Stunden-Spalte exakt an der Stundengrenze.
- `UltraHardcoreStoreTest` (12) — die Buchführung, die einen Reboot überleben
  muss: Sequenzen unabhängig, `clearAllFor` trifft nur einen Alarm,
  Step-Target-/Baseline-Keys lecken nie in `listPending`.
- `NextAlarmWidgetFormatTest` (12) — die Widget-Strings.
- `NextAlarmCalendarEdgeTest` (11) — **Sommerzeit**: 23 h zwischen zwei Weckern
  in der kurzen, 25 h in der langen Nacht (Wanduhrzeit bleibt), übersprungene
  und doppelte Stunde, Monats-/Jahreswechsel, 29. Februar.
- `AlarmSoundTest` (9) — die **persistierten Sound-Ids** als Goldene Map; ein
  Umnummerieren würde bestehende Wecker still umklingeln.
- `BrutusApplicationTest` (8) — Notification-Channels sind write-once:
  Importance, DND-Bypass und Stummheit der Kanäle festgenagelt.
- `PermissionDeepLinkTest` (7) — die drei Reliability-Banner landen auf der
  richtigen Settings-Seite (Action + `package:`-URI + `NEW_TASK`).
- `AlarmSoundGeneratorPropertiesTest` (7) — Invarianten für **jeden** Sound
  (Loop-Länge, Determinismus, DC-Offset, Headroom); neue Enum-Einträge fallen
  automatisch hinein.
- `RoomSchemaExportTest` (7) — vergleicht den **Identity-Hash der laufenden
  Datenbank mit dem committeten `7.json`**; ein Feld ohne Migration fällt damit
  in CI auf statt beim Nutzer um 6 Uhr morgens.
- `AlarmEntityDefaultsTest` (7) + `GlobalQrStoreTest` (6, der Code darf sich nie
  ändern — sonst ist jeder Ausdruck wertlos) + `TimerSoundStoreTest` (6).

### Behoben
- **Widget: „in 1 Tagen" → „in 1 Tag"** — der Countdown des Homescreen-Widgets
  hatte bei exakt einem Tag keinen Singular (die In-App-Anzeige schon).

### Geändert
- `formatStopwatch`, `formatCountdown`, `labelForPreset` sowie die beiden
  Widget-Formatierer sind `internal` statt `private`, damit die Tests die Strings
  prüfen können, die der Nutzer wirklich liest. Kein Verhaltensunterschied.
- **14 weitere Unit-Tests** (insgesamt 75): `MathProblemTest` (8 — Antwort-/
  Display-Logik, Range- und Vorzeichen-Invarianten des Aufgaben-Generators je
  Schwierigkeitsgrad über 500 Samples, Operator-/Difficulty-Fallbacks) und
  `WorldClockStoreTest` (6 — Default-Seeding beim ersten Start, Roundtrips,
  leere Liste fällt nicht auf Defaults zurück, Blank-Filterung; Robolectric).

versionCode 16.

## [2.1.1] — 2026-07-15 · Card-Farbe = Aktiv-Zustand

### Behoben
- **Alarm-Cards kodieren jetzt den Aktiv-Zustand, nicht den nächsten Alarm**:
  vorher bekam nur der als Nächstes klingelnde Wecker die volle
  primaryContainer-Fläche — drei aktive Wecker sahen dreifach unterschiedlich
  aus, obwohl sie im selben Zustand waren. Jetzt liegen **alle aktiven** Wecker
  auf demselben gedämpften Rot (primaryContainer zur Surface hin abgemischt,
  ruhig auch bei vielen Alarmen), **inaktive** sinken wie bisher auf
  grau/dunkel (surfaceContainerLow). Der nächste Alarm behält seine Farbe und
  wird stattdessen durch eine schmale primary-Outline markiert (der Header
  nennt ihn ohnehin).

### Hinzugefügt — Unit-Tests für die ViewModel-Zustandsmaschinen
- **20 neue Tests** (insgesamt 61): `StopwatchViewModelTest` (6 — Segment-
  Akkumulation, Runden, Reset-Undo-Snapshot inkl. Single-Shot-Semantik),
  `TimerViewModelTest` (8 — Countdown-/Pause-Mathematik, Cancel-Undo-Automat,
  abgelaufener Timer bewusst nicht undoable; läuft unter Robolectric) und
  `AlarmEntityTest` (6 — timeString-Padding, repeatDaysString-Fälle,
  Wochentags-Bitmaske, hardcoreEffective).
- Dafür bekamen beide ViewModels eine **injizierbare Clock**
  (`now: () -> Long`, Default `SystemClock::elapsedRealtime`) — kein
  Verhaltensunterschied in der App, aber die Timing-Logik ist jetzt
  deterministisch auf der JVM testbar. Neue Test-Dependencies:
  kotlinx-coroutines-test, Robolectric, androidx.test:core.

versionCode 15.

## [2.1.0] — 2026-07-08 · Tabellarische Ziffern überall + Undo überall

### Geändert
- **Alle Zahlen im Stoppuhr-Look**: `fontFeatureSettings = "tnum"` sitzt jetzt
  auf **jedem** Stil der Type-Scale (display → label) — jede Zahl in der App
  läuft mit tabellarischen Ziffern (Countdown-Header, Chips, Steppers,
  Sunrise-Countdown, Keypad, Runden, …), nichts wackelt mehr beim Ticken.
  Die letzten stillosen Zahlen-Texte (Shake-/Step-"/ x") auf Scale-Stile
  gehoben; redundante lokale `tnum`-Kopien entfernt.

### Hinzugefügt — Undo überall
Bisher gab es Undo nur fürs Löschen von Alarmen. Jetzt ist jede verwerfende
Aktion per Snackbar („Rückgängig", 10 s) umkehrbar:
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

Das Expressive-Redesign ist abgeschlossen — damit endet die 2.0.0-alpha-Serie.

### Geändert
- **Richtungsbewusste Tab-Übergänge**: Der Wechsel in der Bottom-Navigation läuft
  jetzt als Shared-Axis-X-Übergabe (kurzes Federn von der Seite, auf der der Tab
  sitzt, + Fade-through) statt als harter Schnitt, getrieben von der
  Expressive-Spatial-Spec.
- **Reduzierte-Bewegung-Durchgang abgeschlossen**: Die welligen Timer-/Schüttel-/
  Schritt-Ringe werden zu glatten Ringen (`amplitude = 0`), wenn System-
  Animationen abgeschaltet sind; atmender Verlauf und Puls-Hinweise waren schon
  in Phase 3 abgesichert.
- **Farbrollen-Durchgang beendet**: Der UHC-Aufgabenbildschirm ist migriert
  (Error-Label, Tertiary-Erfolg, Primary-CTA); die einzigen verbliebenen rohen
  Markenfarben sind in `Color.kt` dokumentiert — der Seed (`BrutusRed`), die
  Alarm-Wortmarke (`BrutusRedBright`) und der Verlaufskern von Alarm/UHC
  (`BrutusDarkRed`). Tote Alt-Aliase für Flächen/Text entfernt. Kein
  verirrtes `Color(0x…)` und keine eigenen Ripple-/Indication-Overrides mehr in
  der UI-Schicht.
- versionCode 13, versionName 2.0.0.

## [2.0.0-alpha03] — 2026-07-08 · M3 Expressive, Phase 3: Bildschirme

### Weckerliste (3.1)
- Tonale Kartenhierarchie: **nächster Alarm auf primaryContainer**, aktive auf
  surfaceContainerHigh, inaktive sinken auf surfaceContainerLow; Uhrzeit im
  Display-Stil (Space Grotesk, `tnum`), animierte Containerfarbe.
- **Wischen zum Löschen** (errorContainer-Enthüllung) zusätzlich zur
  Rückgängig-Snackbar; der Löschknopf bleibt für TalkBack. Markenroter **FAB mit
  Form-Morph beim Drücken**, expressiver Leerzustand, `animateItem()`-Federn,
  rollenbasierte Chips/Banner/Wochentagspillen.

### Bearbeiten-Sheet (3.2)
- Schwierigkeits-/Empfindlichkeits-Presets → **SegmentedButtonRow**;
  Aufgaben-Chips mit Icons + „Reihenfolge"-Kette; **tonale Gefahrenstufen-Zeilen**
  (errorContainer, sobald ein Hardcore-Modus scharf ist, tertiaryContainer für
  Sunrise); tonales, abgerundetes TextField; FilledTonalButtons; Sheet auf
  surfaceContainerLow.

### Alarmbildschirm & Aufgaben (3.3)
- **Atmender Markenverlauf** (ruhiges 5-s-Anschwellen; statisch bei reduzierter
  Bewegung — neues `rememberReducedMotion()`), Hero-Uhr auf displayLarge +
  `tnum`, Stopp-CTA mit Form-Morph beim Drücken.
- **MathChallenge: eigener Ziffernblock** (12 morphende Tasten, Primary-Bestätigung)
  — keine Systemtastatur mehr über dem Alarm.
- Schüttel-/Schritt-Ringe → **CircularWavyProgressIndicator** (Tertiary-Welle);
  QR-Rahmen als expressives Squircle mit Fehlerblitz bei falschem Scan;
  SwipeToSnooze auf Tertiary-Rollen mit Kreis→Squircle-Morph des Griffs.

### Timer & Stoppuhr (3.4)
- Countdown in einem **welligen Ring**, der am Ende auf Error-Töne umschlägt;
  Presets → AssistChips; Bedienelemente als breite tonale/primäre Knopfpaare;
  Runden als tonale Zeilen mit Tabellenziffern + `animateItem()`.

### Weltuhr (3.5)
- Tonale Zonenkarten mit **Tag-/Nacht-Rollenindikator** (Tertiary-Sonne /
  Secondary-Mond), Uhrzeit im Display-Stil, Suchfeld als Pille, Globus als
  Leerzustand.

### Sunrise & Widget (3.6, 3.7)
- Sunrise: rollengetönte Labels (Tertiary), Hero mit Tabellenziffern,
  Primary-CTA; der physikalische Morgenwärme-Verlauf bleibt (bewusst, an die
  Helligkeit gekoppelt).
- Widget: 24-dp-Ecken, warmer surfaceContainer-Verlauf, Text auf
  onSurface-/Outline-Tönen, Countdown in Rot-Ton 80.

Alle Bildschirme (außer den bewusst dunkel fixierten Alarmflächen) liefern
Dark-, Light- und Dynamic-Previews mit.

## [2.0.0-alpha02] — 2026-07-08 · M3 Expressive, Phase 2: Navigation & Chrome

### Geändert
- **Bottom-Navigation → `ShortNavigationBar`** (M3 Expressive): tonaler
  `surfaceContainer`-Hintergrund, rollenbasierte Auswahlfarben (Pillen-Indikator
  in `secondaryContainer` — die hartcodierte rote Auswahl ist weg), Wechsel
  zwischen gefülltem und Outline-Icon mit weicher räumlicher Feder
  (`MotionScheme.fastSpatialSpec`). Als vorschaufähige Composable herausgezogen,
  mit Dark-/Light-/Dynamic-Previews. Eine `WideNavigationRail`-Variante wurde
  geprüft und verworfen: Brutus ist eine Hochkant-Telefon-App (Alarm- und
  Sperrbildschirm-Abläufe), Querformat-Tablets sind kein Ziel.
- `contentDescription` der Nav-Icons auf `null` gesetzt — das immer sichtbare
  Label benennt den Tab bereits; TalkBack sagt ihn nicht mehr doppelt an.

### Behoben (Edge-to-Edge)
- Die vier alarmnahen Activities (Klingeln, Testalarm, Sunrise, UHC-Aufgabe)
  rufen jetzt `enableEdgeToEdge()`: Ihre Verläufe laufen hinter die Systemleisten,
  während der Inhalt `safeDrawingPadding()` respektiert (Aussparungen,
  Gestenbereiche).

## [2.0.0-alpha01] — 2026-07-07 · M3 Expressive, Phase 1: Fundament

Das rein visuelle Redesign auf **Material 3 Expressive** beginnt. Keine
Verhaltensänderungen — ViewModels, Room, Scheduler, Service und Receiver bleiben
unangetastet.

### Geändert
- **Compose BOM `2026.06.01`** (vorher 2024.12.01), **material3 auf
  `1.5.0-alpha18` gepinnt**: Die BOM bildet material3 1.4.0 stable ab, dort sind
  die Expressive-Einstiegspunkte (`MaterialExpressiveTheme`, `MotionScheme`,
  `expressiveLightColorScheme`) aber noch `internal` — öffentlich werden sie im
  1.5.0-alpha-Kanal (Theme/Motion alpha15–18, wellige Indikatoren alpha18).
  alpha18 ist das neueste Alpha auf Compose 1.11 — ab alpha19 kommt Compose 1.12
  und damit compileSdk 37 + AGP 9.1; für einen rein visuellen Durchgang ist das
  den Toolchain-Aufwand nicht wert. Später stabil werdende Komponenten
  (ButtonGroup, FloatingToolbar) werden hinter einem ehrlichen
  `@OptIn(ExperimentalMaterial3ExpressiveApi)` genutzt. Alles außer material3
  bleibt BOM-verwaltet.
- **Vollständiges M3-Farbrollen-System** aus dem BrutusRed-Seed (`#E53935`): alle
  Primary-/Secondary-/Tertiary-Rollen (+Container), die Error-Familie, die
  komplette Tonleiter `surfaceContainerLowest…Highest`, Outlines, Inverse-Rollen.
  Orange ist jetzt die **Tertiary**-Familie (Akzente für Sunrise/Timer).
- **Light-Theme**: Die App folgt der Systemeinstellung (Light-Schema auf Basis
  von `expressiveLightColorScheme`); Dark bleibt der Design-Standard. Die
  alarmnahen Activities (Klingeln, Sunrise, UHC-Aufgabe, Testalarm) bleiben auf
  Dark fixiert — ihre Schwarz-Rot-Verläufe setzen helle Inhalte voraus.
- **`MaterialExpressiveTheme`** mit `MotionScheme.expressive()` (räumliche Federn)
  und einer expressiven Form-Skala (8/12/16/24/32 dp).
- **Expressive Typografie** mit voller Skala: Space Grotesk (variabel, OFL) für
  Display/Headline/Title, Tabellenziffern (`tnum`) auf den Display-Stilen, damit
  tickende Uhren nicht zappeln; Body/Labels bleiben auf der Systemschrift.
- Theme-Previews (Dark-/Light-/Dynamic-Muster) in `ui/theme/ThemePreview.kt`.

### Hinzugefügt
- **Material-You-Farben** als Opt-in (API 31+), gespeichert über DataStore
  (`ThemeSettings`), Schalter im Überlaufmenü der Weckerliste. Standard bleibt
  das rote Brutus-Markenschema.
- Mitgelieferte Schrift `res/font/space_grotesk.ttf` (SIL Open Font License 1.1).

## [1.9.0] — 2026-07-07
- Rückgängig-Snackbar beim Löschen (einzeln + alles), stellt Wecker inklusive
  Terminierung wieder her.
- Wecker kopieren: Der ⧉-Knopf auf der Karte öffnet ein vorbelegtes
  „Alarm kopieren"-Bearbeiten-Sheet.

## [1.8.0] — 2026-07-06
- Bugfix-Runde (3 parallele Reviews): `goAsync()` in Boot-/Widget-Receivern,
  Sitzungsübernahme bei überlappenden Alarmen, Abbruch veralteter
  Sunrise-Vorläufe, Timer-/Stoppuhr-Zustand überlebt Tab-Wechsel
  (Activity-weite ViewModels), Aufräumen der QR-Kamera.
- GUI-Politur: einzeilige Wochentagspillen im Bearbeiten-Sheet, angehefteter
  Speichern-Knopf, TalkBack-Aktion für die Snooze-Wischgeste,
  Platzhalter in der Weltuhr-Suche.

## [1.7.0] — 2026-07-05
- Neu gestaltete Weckerkarten (Wochentagsleiste über die volle Breite, Info-Chips).
- Fünf extreme Wecker-Sounds (Stadion-Horn, Presslufthammer, Feueralarm, Bohrer,
  Banshee).

## [1.6.x und älter]
- Siehe Git-Historie / GitHub-Releases.
