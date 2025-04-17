# Projektarchitektur des Java-Taschenrechners

Diese Dokumentation bietet einen Überblick über die Architektur des Java-Taschenrechners mit besonderem Fokus auf die Plotter-Komponenten.

## Übersicht

Der Java-Taschenrechner ist modular aufgebaut und besteht aus folgenden Hauptkomponenten:

1. **Core**: Enthält die Hauptfunktionalität des Taschenrechners
2. **Plugins**: Erweiterungsmodule wie 2D- und 3D-Plotter
3. **Parser**: Komponenten zur Verarbeitung mathematischer Ausdrücke
4. **Util**: Hilfsfunktionen für verschiedene Aufgaben (Debug, History, etc.)
5. **Common**: Gemeinsam genutzte Komponenten und Konstanten

Die Anwendung nutzt ein Plugin-System, das es ermöglicht, die Funktionalität zu erweitern, ohne den Hauptcode zu ändern. Jedes Plugin implementiert das `CalculatorPlugin`-Interface und kann sowohl eigene UI-Komponenten als auch eigene Funktionalität bereitstellen.

## 2D-Plotter

Der 2D-Plotter ist als Plugin implementiert und ermöglicht die Darstellung mathematischer Funktionen in einem zweidimensionalen Koordinatensystem.

### Hauptkomponenten des 2D-Plotters:

1. **PlotterPlugin**: Hauptklasse des Plugins, implementiert das `CalculatorPlugin`-Interface.

2. **PlotterPanel**: Hauptcontainer, der alle UI-Komponenten des Plotters koordiniert:
   - GraphPanel (linke Seite)
   - Steuerelemente (rechte Seite mit mehreren Unterpanels)

3. **GraphPanel**: Verantwortlich für die graphische Darstellung der Funktionen
   - Unterstützt Zoom, Schwenken und Auswahl von Funktionen
   - Verwaltet die Berechnung und Darstellung von Schnittpunkten
   - Bietet Tooltips für Punkte auf den Funktionen

4. **FunctionInputPanel**: Verwaltet die Eingabe und Verwaltung von Funktionen
   - Eingabefeld für Funktionsausdrücke
   - Liste der hinzugefügten Funktionen mit Checkboxes für Sichtbarkeit
   - Unterstützt Farbauswahl für die Funktionen

5. **ViewControlPanel**: Ermöglicht die Steuerung der Ansicht
   - Zentrierung des Graphen auf bestimmte Koordinaten
   - Zurücksetzen der Ansicht

6. **IntersectionPanel**: Verwaltet die Berechnung und Anzeige von Schnittpunkten

7. **ExamplePanel**: Stellt vordefinierte Beispielfunktionen bereit

### Wichtige Hilfsklassen:

- **CoordinateTransformer**: Konvertiert zwischen Bildschirm- und mathematischen Koordinaten
- **GridRenderer**: Zeichnet das Koordinatensystem und Gitterlinien
- **FunctionRenderer**: Zeichnet die Funktionsgraphen
- **IntersectionCalculator**: Berechnet Schnittpunkte zwischen Funktionen

## 3D-Plotter

Der 3D-Plotter ist ebenfalls als Plugin implementiert und ermöglicht die Visualisierung von Funktionen mit zwei Variablen (x,y → z).

### Hauptkomponenten des 3D-Plotters:

1. **Plot3DPlugin**: Hauptklasse des Plugins, implementiert das `CalculatorPlugin`-Interface.

2. **Plot3DPanel**: Hauptcontainer, der alle UI-Komponenten des 3D-Plotters koordiniert:
   - Darstellungsbereich für 3D-Funktionen
   - Scrollbare Steuerungsleiste

3. **Plot3DRenderer**: Verantwortlich für das Zeichnen der 3D-Funktionen:
   - Unterstützt verschiedene Darstellungsmodi (Drahtgitter, Heatmap, Solid)
   - Rendert das 3D-Koordinatensystem
   - Berechnet die Projektion von 3D auf 2D

4. **Plot3DUIBuilder**: Erstellt die Benutzeroberfläche für den 3D-Plotter:
   - Funktionseingabe und Farbauswahl
   - Auswahllisten für Funktionen und Beispiele
   - Steuerelemente für Bereich, Auflösung, Rotation
   - Anzeigeoptionen (Koordinatensystem, Gitter, Hilfslinien, etc.)

5. **Plot3DViewController**: Verwaltet die Ansichtsparameter:
   - Rotation des 3D-Modells
   - Grenzen des dargestellten Bereichs
   - Auflösung der Darstellung

6. **Plot3DModel**: Verwaltet die mathematischen Aspekte:
   - Berechnet Funktionswerte
   - Bestimmt geeignete Z-Wertebereiche

7. **Plot3DFunctionManager**: Verwaltet die Funktionen:
   - Hinzufügen, Entfernen und Bearbeiten von Funktionen
   - Liste der aktiven Funktionen
   - Farbverwaltung

8. **Example3DPanel**: Stellt vordefinierte 3D-Beispielfunktionen bereit

### Besondere Merkmale des 3D-Plotters:

- **Interaktive Rotation**: Die 3D-Darstellung kann über Slider oder direkt mit der Maus rotiert werden
- **Anpassbare Auflösung**: Die Detailgenauigkeit kann über einen Slider oder ein Texteingabefeld gesteuert werden
- **Verschiedene Darstellungsmodi**: Drahtgitter, Heatmap-Farbcodierung oder undurchsichtige Oberflächen
- **Bereichsanpassung**: Der dargestellte x/y/z-Bereich kann angepasst werden
- **Schnittlinien**: Optionale Darstellung von Schnittlinien zwischen mehreren Funktionen

## Gemeinsame Komponenten

Einige Komponenten werden von beiden Plottern verwendet:

1. **ColorChooser**: Ermöglicht die Auswahl und Verwaltung von Farben
2. **DebugManager**: Unterstützt die Ausgabe von Debug-Informationen
3. **Parser-Komponenten**: FunctionParser (2D) und Function3DParser (3D) für die Interpretation von Funktionsausdrücken

## Erweiterbarkeit

Das System ist auf Erweiterbarkeit ausgelegt:

1. **Neue Funktionen**: Können einfach zum bestehenden Parser hinzugefügt werden
2. **Neue Anzeigeoptionen**: Die UI-Builder-Klassen können erweitert werden
3. **Neue Plugins**: Durch Implementierung des CalculatorPlugin-Interfaces

## Technische Details

- **UI-Framework**: Swing für die gesamte Benutzeroberfläche
- **Rendering**: Java2D für 2D-Grafiken und simulierte 3D-Darstellung
- **Ereignisverarbeitung**: PropertyChangeSupport für Benachrichtigungen zwischen Komponenten
- **Datenaustausch**: Übergabe von Funktionen vom Taschenrechner an die Plotter 