# Java-Taschenrechner API-Dokumentation

Diese Dokumentation bietet einen Überblick über die Architektur und Funktionalität des Java-Taschenrechners, um die Erweiterung mit neuen Funktionen zu erleichtern.

## Inhaltsverzeichnis

1. [Projektstruktur](#projektstruktur)
2. [Core-Komponenten](#core-komponenten)
3. [Parser-Komponenten](#parser-komponenten)
4. [Utility-Komponenten](#utility-komponenten)
5. [Plugin-System](#plugin-system)
6. [Erweiterungsanleitung](#erweiterungsanleitung)

## Projektstruktur

Der Taschenrechner ist modular aufgebaut und in folgende Hauptpakete unterteilt:

- **core**: Enthält die Hauptkomponenten des Taschenrechners
- **parser**: Enthält Parser für mathematische Ausdrücke und Funktionen
- **util**: Enthält Hilfsklassen für Fehlerbehandlung, Debugging und Verlaufsverwaltung
- **plugins**: Enthält das Plugin-System und vordefinierte Plugins
- **common**: Enthält gemeinsam genutzte Konstanten und UI-Komponenten

## Core-Komponenten

### Taschenrechner

`core.Taschenrechner` ist die Hauptklasse des Projekts. Sie verwaltet die grundlegende UI und koordiniert die Interaktion zwischen den verschiedenen Komponenten.

**Wichtige Methoden:**
- `getDisplayText()`: Gibt den aktuellen Text im Anzeigefeld zurück
- `setDisplayText(String text)`: Setzt den Text im Anzeigefeld
- `addToHistory(String calculation, String result)`: Fügt eine Berechnung zum Verlauf hinzu
- `debug(String message)`: Protokolliert eine Debug-Nachricht
- `toggleHistory()`: Zeigt oder versteckt den Verlaufs-Dialog
- `toggleDebug()`: Zeigt oder versteckt den Debug-Dialog

### CalculationEngine

`core.CalculationEngine` ist die zentrale Recheneinheit, die mathematische Ausdrücke analysiert und berechnet.

**Wichtige Methoden:**
- `berechneFormel()`: Berechnet die aktuelle Formel im Anzeigefeld
- `berechneAusdruck(String ausdruck)`: Wertet einen mathematischen Ausdruck aus
- `ergaenzeImpliziteMultiplikationen(String formel)`: Behandelt implizite Multiplikationen (z.B. 2(3+4) → 2*(3+4))
- `toggleVorzeichen(String ausdruck)`: Ändert das Vorzeichen des Ausdrucks
- `checkIfFunction(String formel)`: Prüft, ob eine Formel eine plottbare Funktion sein könnte

### InputHandler

`core.InputHandler` verarbeitet Benutzereingaben von Tastatur und Mausereignissen.

**Wichtige Methoden:**
- `handleInput(String input)`: Verarbeitet eine Benutzereingabe
- `handleNumericInput(String input)`: Verarbeitet numerische Eingaben
- `handleOperatorInput(String operator)`: Verarbeitet Operator-Eingaben
- `handleSpecialOperationInput(String operation)`: Verarbeitet spezielle Operationen

### GrafischerTaschenrechner

`core.GrafischerTaschenrechner` erweitert den Basistaschenrechner um grafische Funktionalität.

**Wichtige Methoden:**
- `transferFunctionToPlotter(String function)`: Überträgt eine Funktion zum 2D-Plotter
- `transferFunctionTo3DPlotter(String function)`: Überträgt eine Funktion zum 3D-Plotter

## Parser-Komponenten

### AbstractExpressionParser

`parser.AbstractExpressionParser` ist die Basisklasse für die verschiedenen Parser-Implementierungen.

**Wichtige Methoden:**
- `parse(String expression)`: Parst einen mathematischen Ausdruck
- `validate(String expression)`: Validiert einen mathematischen Ausdruck

### FunctionParser

`parser.FunctionParser` parst 2D-Funktionen (mit einer Variable x).

**Wichtige Methoden:**
- `parseFunction(String functionString)`: Parst einen Funktionsausdruck
- `evaluateAt(double x)`: Wertet die Funktion an einer Stelle x aus

### Function3DParser

`parser.Function3DParser` parst 3D-Funktionen (mit Variablen x und y).

**Wichtige Methoden:**
- `parseFunction(String functionString)`: Parst einen 3D-Funktionsausdruck
- `evaluateAt(double x, double y)`: Wertet die Funktion an einer Stelle (x,y) aus

## Utility-Komponenten

### HistoryManager

`util.history.HistoryManager` verwaltet den Berechnungsverlauf.

**Wichtige Methoden:**
- `addToHistory(String calculation, String result)`: Fügt eine Berechnung zum Verlauf hinzu
- `getHistory()`: Gibt den gesamten Verlauf zurück
- `clearHistory()`: Löscht den Verlauf
- `showHistoryDialog()`: Zeigt das Verlaufsfenster an

### DebugManager

`util.debug.DebugManager` unterstützt das Debugging.

**Wichtige Methoden:**
- `debug(String message)`: Protokolliert eine Debug-Nachricht
- `showDebugDialog()`: Zeigt das Debug-Fenster an

## Plugin-System

### CalculatorPlugin

`plugins.CalculatorPlugin` ist das Interface, das alle Plugins implementieren müssen.

**Methoden:**
- `getName()`: Gibt den Namen des Plugins zurück
- `createPanel(Taschenrechner calculator)`: Erstellt das Panel für das Plugin
- `initialize()`: Wird beim Laden des Plugins aufgerufen
- `shutdown()`: Wird beim Beenden des Plugins aufgerufen

### PluginManager

`plugins.PluginManager` verwaltet alle aktivierten Plugins.

**Wichtige Methoden:**
- `loadPlugin(String pluginClassName)`: Lädt ein Plugin
- `unloadPlugin(String pluginName)`: Entlädt ein Plugin
- `getLoadedPlugins()`: Gibt alle geladenen Plugins zurück

### 3D-Plotter

Der 3D-Plotter ermöglicht die Visualisierung von dreidimensionalen Funktionen. Er besteht aus mehreren Hauptkomponenten, die zusammenarbeiten, um eine interaktive 3D-Darstellung zu erzeugen.

#### Wichtige Klassen

- **Plot3DPanel**: Hauptklasse, die alle Komponenten des 3D-Plotters koordiniert. Sie enthält den `Plot3DRenderer` für die Darstellung, den `Plot3DViewController` für die Verwaltung der Ansichtsparameter und den `Plot3DUIBuilder` für den Aufbau der Benutzeroberfläche.
  - `public void addFunction(String functionString, Color color)` - Fügt eine neue Funktion zum Plotter hinzu
  - `public void renderPlot()` - Zeichnet den Plotter neu
  - `public void debug(String message)` - Gibt Debug-Informationen aus

- **Plot3DRenderer**: Verantwortlich für das Zeichnen der 3D-Funktionen.
  - `public void render(Graphics2D g2d)` - Rendert alle Funktionen
  - `public void addFunction(Function3D function, Color color)` - Fügt eine Funktion zum Renderer hinzu
  - `public void setResolution(int resolution)` - Legt die Auflösung der Darstellung fest
  - `public void setRotation(double xRotation, double yRotation)` - Legt die Rotation der Ansicht fest
  - `public void setBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ)` - Setzt die Grenzen des dargestellten Bereichs

- **Plot3DViewController**: Verwaltet die Ansichtsparameter des 3D-Plotters.
  - `public void updateRotation(double xRotation, double yRotation)` - Aktualisiert die Rotation
  - `public void updateViewBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ)` - Aktualisiert die Ansichtsgrenzen
  - `public void resetView()` - Setzt die Ansicht zurück
  - `public int getCurrentResolution()` - Gibt die aktuelle Auflösung zurück

- **Plot3DUIBuilder**: Erstellt die Benutzeroberfläche für den 3D-Plotter.
  - `public JPanel createControlPanel()` - Erstellt das Steuerungspanel
  - `public JPanel createFunctionListPanel()` - Erstellt das Panel für die Funktionsliste
  - `public JPanel createResolutionPanel()` - Erstellt das Panel für die Auflösungseinstellungen

#### Benutzeroberfläche

Die Benutzeroberfläche des 3D-Plotters bietet folgende Funktionen:

1. **Scrollbare Steuerungsleiste**: Die rechte Steuerungsleiste ist mit einem ScrollPane versehen, um auch bei vielen Steuerelementen alle Optionen zugänglich zu machen.

2. **Interaktiver Auflösungsslider**: Der Slider zur Einstellung der Auflösung aktualisiert die Darstellung direkt, sobald der Benutzer das Schieben beendet. Dies bietet eine intuitive Möglichkeit, die Detailgenauigkeit der 3D-Darstellung anzupassen, ohne bei jeder Slider-Bewegung eine Neuberechnung auszulösen.

3. **Anpassbare Rotationssteuerung**: Die Rotation der 3D-Darstellung kann über Slider oder direkt durch Ziehen mit der Maus gesteuert werden.

4. **Heatmap-Farbcodierung**: Funktionen können mit einer Heatmap-Farbcodierung dargestellt werden, um Höheninformationen visuell hervorzuheben.

5. **Anzeige von Schnittlinien**: Schnittlinien zwischen mehreren Funktionen können visualisiert werden.

#### Anpassung der Anzeige

Um den 3D-Plotter für spezifische Anforderungen anzupassen, können mehrere Parameter modifiziert werden:

1. **Auflösung**: Steuert die Genauigkeit der 3D-Darstellung. Höhere Werte erzeugen detailliertere Darstellungen, aber erhöhen die Rechenzeit. Die Auflösung kann über einen Slider (10-100) oder ein Textfeld (10-500) angepasst werden.

2. **Darstellungsoptionen**: Verschiedene Darstellungsoptionen können über Checkboxen aktiviert oder deaktiviert werden, z.B. die Anzeige von Gitterlinien, Achsen oder die Farbgebung.

3. **Funktionsliste**: Die Mindesthöhe der Funktionsliste wurde optimiert, um mehr Funktionen gleichzeitig anzuzeigen.

4. **Beispiele**: Die Liste der 3D-Beispielfunktionen hat eine angepasste Mindesthöhe für bessere Übersichtlichkeit.

### Plugin-Entwicklung

Um ein neues Plugin zu erstellen:

1. Implementieren Sie das `CalculatorPlugin`-Interface
2. Erstellen Sie ein Panel mit der gewünschten Funktionalität
3. Registrieren Sie das Plugin beim `PluginManager`

Beispiel:
```java
public class MeinPlugin implements CalculatorPlugin {
    private Taschenrechner calculator;
    
    @Override
    public String getName() {
        return "Mein Plugin";
    }
    
    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        this.calculator = calculator;
        JPanel panel = new JPanel();
        // Panel-Inhalte hinzufügen
        return panel;
    }
    
    @Override
    public void initialize() {
        // Initialisierung
    }
    
    @Override
    public void shutdown() {
        // Aufräumen
    }
}
```

### Neue Konstante hinzufügen

Um eine neue mathematische Konstante hinzuzufügen:

1. Fügen Sie die Konstante zu `CalculatorConstants` hinzu
2. Aktualisieren Sie die `isConstantExpression`-Methode in `CalculationEngine`
3. Aktualisieren Sie die Berechnungsmethode, um die neue Konstante zu erkennen und zu verwenden 