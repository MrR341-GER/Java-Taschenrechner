# Java-Taschenrechner Dokumentation

## Übersicht

Diese Dokumentation bietet einen umfassenden Überblick über die Architektur, Funktionen und Erweiterungsmöglichkeiten des Java-Taschenrechners. Sie dient als Referenz für Entwickler, die das Projekt verstehen, erweitern oder mit ihm interagieren möchten.

## Inhalt der Dokumentation

1. **[API-Dokumentation](API-Dokumentation.md)** - Eine detaillierte Beschreibung aller Hauptkomponenten, Klassen und deren Funktionen.

2. **[Klassendiagramm](Klassendiagramm.md)** - Eine visuelle Darstellung der Klassenstruktur und deren Beziehungen.

3. **[Erweiterungsanleitung](Erweiterungsanleitung.md)** - Eine Schritt-für-Schritt-Anleitung zur Erweiterung des Taschenrechners mit neuen Funktionen und Plugins.

## Hauptmerkmale des Taschenrechners

Der Java-Taschenrechner ist eine umfassende Anwendung mit folgenden Hauptmerkmalen:

- Grundlegende arithmetische Operationen (+, -, *, /)
- Erweiterte mathematische Funktionen (Sinus, Cosinus, Logarithmus, etc.)
- 2D- und 3D-Funktionsplotter
- Plugin-System zur Erweiterung der Funktionalität
- Einheitenumrechner, statistisches Modul und weitere Plugins
- Berechnungsverlauf mit Speicherfunktion
- Debug-Modus für Entwicklung

## Architekturübersicht

Der Taschenrechner folgt einer modularen Architektur:

- **Core-Modul**: Enthält die Grundfunktionalität wie UI, Eingabeverarbeitung und Berechnungslogik
- **Parser-Modul**: Verantwortlich für das Parsen mathematischer Ausdrücke und Funktionen
- **Plugin-System**: Ermöglicht die dynamische Erweiterung um neue Funktionalitäten
- **Utility-Module**: Bieten Unterstützungsfunktionen wie Verlaufsverwaltung und Debugging

## Schnelleinstieg für Entwickler

### Voraussetzungen

- Java Development Kit (JDK) 11 oder höher
- Grundlegende Kenntnisse in Java und Swing

### Erste Schritte

1. **Projektstruktur verstehen**: Lesen Sie die [API-Dokumentation](API-Dokumentation.md)
2. **Architektur visualisieren**: Betrachten Sie das [Klassendiagramm](Klassendiagramm.md)
3. **Erweiterungen entwickeln**: Folgen Sie der [Erweiterungsanleitung](Erweiterungsanleitung.md)

## Typische Erweiterungsszenarien

1. **Neue mathematische Funktion hinzufügen** (z.B. Fakultät, komplexe Zahlen)
2. **Neues Plugin erstellen** (z.B. Finanzmathematik-Plugin)
3. **UI-Anpassungen vornehmen** (z.B. neues Farbschema, alternative Tastatur-Layouts)
4. **Grafische Darstellungsoptionen erweitern** (z.B. Polarkoordinaten im Plotter)

## Verwendung dieser Dokumentation

- **Für Einsteiger**: Beginnen Sie mit dem Klassendiagramm und der API-Dokumentation
- **Für Entwickler neuer Funktionen**: Folgen Sie der Erweiterungsanleitung
- **Für Plugin-Entwickler**: Konzentrieren Sie sich auf das Plugin-System in der API-Dokumentation 