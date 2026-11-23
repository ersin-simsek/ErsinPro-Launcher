[English](../README.md) | [Deutsch](README.de.md)

# ErsinPro

*Nativer Android-TV-Launcher für Geräte mit wenig Arbeitsspeicher und Steuerkreuz-Fernbedienung.*

ErsinPro ist ein nativer Java-Launcher für Fernseher und Projektoren mit Steuerkreuz-Fernbedienung. Entwickelt und getestet wurde er auf einem Yasomi Optive X5 mit Android 9 (API 28) und 1 GB Arbeitsspeicher.

**Auf dem X5 verkürzte sich die Zeit zwischen der ersten Darstellung des Werkslaunchers und der ersten Darstellung von ErsinPro von etwa 91,9 s auf 3,627 s.** Der Vergleich beruht auf einem kontrollierten Neustart pro Version. [Messung und Grenzen](performance.md)

## Bildschirmaufnahmen im Modern-Clean-Design

![ErsinPro im Modern-Clean-Design bei 1080p mit Fokus in der Unterhaltungsreihe](media/home-v0.2.7-1080p.png)

*Englische Oberfläche im Android-13-Emulator, 1920 × 1080, mit projekteigenen Test-Apps im Fokusmodus.* [Englische Oberfläche bei 1280 × 720 im selben Android-13-Emulator](media/home-v0.2.7-720p.png)

In den Bildschirmaufnahmen dargestellte Namen, Logos und Symbole von Drittanbieter-Apps gehören ihren jeweiligen Rechteinhabern. Sie dienen ausschließlich der Veranschaulichung der Launcher-Funktionen und lassen weder auf eine Verbindung zu diesen Anbietern noch auf deren Unterstützung schließen.

## Funktionen und Bedienung

- Zwei unabhängig scrollbare Reihen für **Unterhaltung** und **Apps und Werkzeuge** mit Symbolen der installierten Apps.
- Automatische und manuelle Zuordnung, Favoriten, eigene Reihenfolge, Ausblenden von Apps und Fokusmodus.
- Deutsch, Englisch und Türkisch mit lokalisierten Menüs, Datumsformaten und Begrüßungen.
- Lokal gespeicherte Einstellungen und Wiederherstellung bei einer unlesbaren Konfiguration. ErsinPro benötigt keine Internetberechtigung und betreibt keinen dauerhaften Dienst.

Mit Oben und Unten wechselst du zwischen Kopfbereich und Reihen. Links und Rechts bewegen den Fokus innerhalb einer Reihe. OK öffnet die ausgewählte App oder die Einstellungen. Halte OK auf einer Karte gedrückt oder öffne **Einstellungen → Apps verwalten**, um die App-Optionen aufzurufen. Zurück bringt den Fokus von einer Karte zu den Einstellungen. Kehrt eine externe App zu ErsinPro zurück, wird der Fokus auf der zuvor ausgewählten Karte wiederhergestellt, sofern sie sichtbar ist.

Unter **Apps verwalten → Kodi** kannst du Kodi auf der Startseite ausblenden oder wieder einblenden. Die Einstellung gilt für das gesamte Kodi-Paket und bleibt bei einem Wechsel seiner Start-Activity erhalten. Erkennt ErsinPro den Optive X5, wird Kodi beim ersten Auffinden ausgeblendet. Meldet die Firmware einen allgemeinen Modellnamen, musst du die Einstellung einmal selbst wählen. Der Werkslauncher und Casting-Apps behalten ihre eigenen Sichtbarkeitseinstellungen.

## Tests auf dem Referenzgerät

Der Yasomi Optive X5 läuft mit Android 9 (API 28) auf ARMv7, hat 1 GB Arbeitsspeicher und wird per Steuerkreuz bedient. Bei den Tests am Gerät wurden die HOME-Auflösung und die Ereignisse des ActivityManager mit ADB über TCP/IP und logcat untersucht. Die Firmware kann den Werkslauncher direkt starten, auch wenn ErsinPro als Android-HOME bevorzugt bleibt.

v0.2.6-alpha führte einen einmaligen Start über `BOOT_COMPLETED` ein. Seit v0.2.7-alpha kann ErsinPro bereits bei `LOCKED_BOOT_COMPLETED` starten, sofern der Benutzerspeicher verfügbar ist. Andernfalls wartet der Receiver auf den regulären Abschluss des Startvorgangs. Im Gerätetest blieb ErsinPro 120,3 Sekunden im Vordergrund. Außerdem wurden die Einstellungen, der Start von FileManager und die Wiederherstellung des Kartenfokus mit Zurück geprüft. Für die Rückkehr zur Vorgängerversion steht die mit demselben Schlüssel signierte APK zur Verfügung. [Gerätetests und Rückkehr zur Vorgängerversion](compatibility.md)

## Gemessene Leistung

| Messung am X5 | Ergebnis |
| --- | --- |
| Erste Darstellung des Werkslaunchers bis zur ersten Darstellung von ErsinPro, v0.2.6-alpha | ca. 91,9 s |
| Dasselbe Intervall, v0.2.7-alpha | 3,627 s |
| Verkürzung dieses Intervalls | ca. 96,1 % |
| Speicherstichprobe am Gerät, v0.2.6-alpha | 36.858 KiB Gesamt-PSS (ca. 36 MiB) |

Die Messung beginnt mit der ersten Darstellung des Werkslaunchers. Die Zeit ab Betätigung der Einschalttaste wurde dabei nicht erfasst. Der Vergleich beruht auf einem beobachteten Neustart pro Version auf dem getesteten X5. Der PSS-Wert ist eine Momentaufnahme des Prozesses. [Messverfahren und Emulator-Referenzwerte](performance.md)

## Architektur und Verzeichnisstruktur

| Pfad | Aufgabe |
| --- | --- |
| `launcher/src/org/ersinpro/launcher/` | Native Oberfläche, Fokussteuerung, App-Erkennung, Symbolcache und Einstellungsmodell |
| `launcher/res/` | Themes, eigene Grafiken und lokalisierte Texte |
| `tests/` | Modelltests, Android-Instrumentierung und externer Testplayer |
| `tools/` | PowerShell-Skripte für Build, Tests und Emulator |
| `.github/workflows/` | Android-Build und Emulator-Tests in GitHub Actions |
| `docs/` | Architekturentscheidungen, Kompatibilität und Messverfahren |

Die beiden nativen horizontalen Scrollbereiche laden Symbole in der Nähe des sichtbaren Ausschnitts und geben nicht mehr sichtbare Bildreferenzen frei. Der Bitmap-Cache ist begrenzt. Für den Katalog bleiben schlanke Karten-Views erhalten. Eine dünne Fokuslinie zeigt die tatsächliche Kartengrenze. Die Sicherheitsränder leiten sich aus der Anzeigegröße in dp ab. [Architekturentscheidungen](architecture.md)

## Build und Installation

Installiere JDK 17, PowerShell 7, Android SDK Platform 34 und Build Tools 34.0.0. Setze `ANDROID_HOME` oder übergib `-SdkRoot`. Unter Windows prüft das Skript zusätzlich den Standardpfad von Android Studio.

```powershell
./tools/test.ps1
./tools/build.ps1 -Lint
```

Das Skript erzeugt `build/ersinpro-debug.apk` und beim ersten Build einen lokalen Debug-Signaturschlüssel. Scheitert der Windows-Lint-Starter an Leerzeichen im Pfad, übergib mit `-LintJar` den Pfad zur Datei `lib/lint-classpath.jar` des SDK.

Kopiere die APK auf ein USB-Laufwerk und öffne sie im Dateimanager des Projektors. Erlaube bei Bedarf vorübergehend die Installation aus dieser Quelle. Starte ErsinPro aus der App-Liste des Werkslaunchers und behalte während des Gerätetests die aktuelle HOME-Auswahl bei. Für ein direktes Update ist derselbe Signaturschlüssel erforderlich. Die App-Liste des Werkslaunchers bleibt während des Tests der Rückweg.

## Tests und CI

Für lokale Funktionstests nutze einen separaten Emulator mit englischer Systemsprache und führe `./tools/test-ui.ps1 -Serial emulator-5554` aus. GitHub Actions prüft Build, Modell, Lokalisierung und Lint. Die Fernbedienungstests laufen auf API 28 bei 720p und API 33 bei 1080p. Sie prüfen unter anderem die Fokusnavigation, die Wiederherstellung von Einstellungen und die Rückkehr aus einem separaten Testplayer. [Android-CI](https://github.com/ersin-simsek/ErsinPro-Launcher/actions/workflows/android.yml)

## Kompatibilität und bekannte Einschränkungen

Der frühe automatische Start wurde auf dem physischen X5 bestätigt. Die Firmware kann die HOME-Taste weiterhin abfangen und den Werkslauncher öffnen. Der Boot-Receiver verwendet Android-APIs und benötigt weder Root-Zugriff noch Änderungen an der Firmware. Auf anderen Geräten und Firmware-Versionen kann das Verhalten abweichen. [Kompatibilität und Gerätetests](compatibility.md)

## Entwicklungsansatz

ErsinPro wurde mit Unterstützung von OpenAI Codex entwickelt. Der Projektverantwortliche steuerte und überprüfte die Anforderungen, technischen Entscheidungen, Hardwareintegration, Fehlersuche und Validierung. Die KI-gestützte Codegenerierung wurde mit automatisierten Tests und praktischen Prüfungen am physischen Yasomi X5 kombiniert.

## Entwicklung und Ausblick

Die aktuelle Entwicklungsversion ist v0.2.7-alpha. Als Nächstes folgen weitere Neustartversuche auf dem X5, kontrollierte Warmstart- und Speichermessungen sowie Fernbedienungstests bei den unterstützten Ausgabeauflösungen. Geräteprofile, Diagnosefunktionen und adaptive Leistungsanpassung sind geplant. [Änderungsprotokoll](../CHANGELOG.md) · [Veröffentlichungsprozess](release.md)

## Mitwirken und Lizenz

Gib bei Beiträgen das Gerät oder Emulatorprofil an und dokumentiere die relevanten Regressionstests. ErsinPro steht unter der Apache License 2.0. [Mitwirken](../CONTRIBUTING.md) · [Lizenz](../LICENSE)