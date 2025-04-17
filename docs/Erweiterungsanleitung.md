# Erweiterungsanleitung für den Java-Taschenrechner

Diese Anleitung beschreibt detailliert, wie Sie den Java-Taschenrechner mit neuen Funktionen, Operationen und Plugins erweitern können.

## Inhaltsverzeichnis

1. [Neue mathematische Funktion hinzufügen](#neue-mathematische-funktion-hinzufügen)
2. [Neuen Operator implementieren](#neuen-operator-implementieren)
3. [Neues Plugin erstellen](#neues-plugin-erstellen)
4. [Konstanten hinzufügen](#konstanten-hinzufügen)
5. [UI-Komponenten anpassen](#ui-komponenten-anpassen)
6. [Best Practices](#best-practices)

## Neue mathematische Funktion hinzufügen

Um eine neue mathematische Funktion (wie Sinus, Logarithmus usw.) hinzuzufügen:

### 1. Erweitern der CalculationEngine

Öffnen Sie die Klasse `core.CalculationEngine` und fügen Sie eine neue Methode hinzu, die Ihre Funktion implementiert:

```java
/**
 * Berechnet die Fakultät einer Zahl
 * @param n Die Zahl, deren Fakultät berechnet werden soll
 * @return Das Ergebnis der Fakultätsberechnung
 */
private double berechneFakultaet(double n) {
    if (n < 0) {
        throw new IllegalArgumentException("Fakultät ist für negative Zahlen nicht definiert");
    }
    
    int intN = (int) n;
    if (n != intN) {
        throw new IllegalArgumentException("Fakultät ist nur für ganze Zahlen definiert");
    }
    
    double result = 1;
    for (int i = 2; i <= intN; i++) {
        result *= i;
    }
    return result;
}
```

### 2. Implementierung in der berechneAusdruck-Methode

Erweitern Sie die `berechneAusdruck`-Methode, um Ihre neue Funktion zu erkennen und zu verarbeiten:

```java
// In der berechneAusdruck-Methode, wo Funktionen erkannt werden
if (ausdruck.contains("fak(") || ausdruck.contains("factorial(")) {
    // Extrahieren des Arguments aus der Funktion
    int startIndex = ausdruck.indexOf("fak(");
    if (startIndex == -1) {
        startIndex = ausdruck.indexOf("factorial(");
    }
    
    int endIndex = findClosingBracket(ausdruck, startIndex);
    String argument = ausdruck.substring(startIndex + ausdruck.substring(startIndex).indexOf("(") + 1, endIndex);
    
    // Berechnen des Arguments
    double argumentValue = berechneAusdruck(argument);
    
    // Fakultät berechnen
    double result = berechneFakultaet(argumentValue);
    
    // Ersetzen des Funktionsaufrufs durch das Ergebnis
    String prefix = ausdruck.substring(0, startIndex);
    String suffix = ausdruck.substring(endIndex + 1);
    return berechneAusdruck(prefix + result + suffix);
}
```

### 3. Hinzufügen einer Taste im TaschenrechnerKeypad

Öffnen Sie `core.TaschenrechnerKeypad` und fügen Sie eine neue Taste für Ihre Funktion hinzu:

```java
// In der createKeypadPanel-Methode
JButton factorialButton = new JButton("n!");
factorialButton.addActionListener(e -> inputHandler.handleInput("fak("));
scientificPanel.add(factorialButton);
```

## Neuen Operator implementieren

Um einen neuen Operator (z.B. Modulo, Potenz) zu implementieren:

### 1. Operator in der istOperator-Methode registrieren

```java
public boolean istOperator(char c) {
    return c == '+' || c == '-' || c == '*' || c == '/' || c == '%'; // Modulo-Operator hinzugefügt
}
```

### 2. Implementierung in der berechneAusdruck-Methode

```java
// In der Verarbeitungsmethode für Operatoren
case '%':
    double operand1 = berechneAusdruck(leftPart);
    double operand2 = berechneAusdruck(rightPart);
    if (operand2 == 0) {
        throw new ArithmeticException("Division durch Null (Modulo)");
    }
    return operand1 % operand2;
```

### 3. UI-Taste hinzufügen

```java
JButton moduloButton = new JButton("%");
moduloButton.addActionListener(e -> inputHandler.handleOperatorInput("%"));
operatorPanel.add(moduloButton);
```

## Neues Plugin erstellen

Um ein komplett neues Plugin zu erstellen (z.B. ein Einheitenumrechner):

### 1. Interface implementieren

Erstellen Sie eine neue Klasse, die das `CalculatorPlugin`-Interface implementiert:

```java
package plugins.converter;

import javax.swing.*;
import java.awt.*;
import core.Taschenrechner;
import plugins.CalculatorPlugin;

public class UnitConverter implements CalculatorPlugin {
    private JPanel converterPanel;
    private Taschenrechner calculator;
    
    @Override
    public String getName() {
        return "Einheitenumrechner";
    }
    
    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        this.calculator = calculator;
        converterPanel = new JPanel(new BorderLayout());
        
        // UI-Komponenten erstellen
        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        JTextField inputField = new JTextField();
        JComboBox<String> fromUnitCombo = new JComboBox<>(new String[] {"Meter", "Fuß", "Zoll"});
        JComboBox<String> toUnitCombo = new JComboBox<>(new String[] {"Meter", "Fuß", "Zoll"});
        JButton convertButton = new JButton("Umrechnen");
        JTextField resultField = new JTextField();
        resultField.setEditable(false);
        
        // Event-Handling
        convertButton.addActionListener(e -> {
            try {
                double value = Double.parseDouble(inputField.getText());
                String fromUnit = (String) fromUnitCombo.getSelectedItem();
                String toUnit = (String) toUnitCombo.getSelectedItem();
                double result = convertUnit(value, fromUnit, toUnit);
                resultField.setText(String.valueOf(result));
            } catch (NumberFormatException ex) {
                resultField.setText("Fehler: Ungültige Eingabe");
            }
        });
        
        // Layout zusammenstellen
        inputPanel.add(new JLabel("Wert:"));
        inputPanel.add(inputField);
        inputPanel.add(new JLabel("Von:"));
        inputPanel.add(fromUnitCombo);
        inputPanel.add(new JLabel("Nach:"));
        inputPanel.add(toUnitCombo);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(inputPanel, BorderLayout.NORTH);
        centerPanel.add(convertButton, BorderLayout.CENTER);
        centerPanel.add(resultField, BorderLayout.SOUTH);
        
        converterPanel.add(centerPanel, BorderLayout.CENTER);
        
        return converterPanel;
    }
    
    @Override
    public void initialize() {
        // Initialisierungslogik
        calculator.debug("Einheitenumrechner initialisiert");
    }
    
    @Override
    public void shutdown() {
        // Aufräumlogik
        calculator.debug("Einheitenumrechner beendet");
    }
    
    // Hilfsmethode zur Umrechnung von Einheiten
    private double convertUnit(double value, String fromUnit, String toUnit) {
        // Umrechnungsfaktoren zu SI-Einheiten
        double fromFactor = getConversionFactor(fromUnit);
        double toFactor = getConversionFactor(toUnit);
        
        // Umrechnung: Eingabe → SI → Ausgabe
        return value * (fromFactor / toFactor);
    }
    
    private double getConversionFactor(String unit) {
        switch (unit) {
            case "Meter": return 1.0;
            case "Fuß": return 0.3048;
            case "Zoll": return 0.0254;
            default: return 1.0;
        }
    }
}
```

### 2. Plugin beim PluginManager registrieren

Fügen Sie Ihr Plugin in `plugins.PluginManager` hinzu:

```java
// In der initializeDefaultPlugins-Methode
loadPlugin("plugins.converter.UnitConverter");
```

### 3. Plugin zu einem Menü hinzufügen

Falls ein Menü für Plugins existiert, fügen Sie dort einen Eintrag hinzu:

```java
// In der Methode, die das Plugin-Menü erstellt
JMenuItem converterMenuItem = new JMenuItem("Einheitenumrechner");
converterMenuItem.addActionListener(e -> showPlugin("Einheitenumrechner"));
pluginMenu.add(converterMenuItem);
```

## Konstanten hinzufügen

Um eine neue mathematische Konstante hinzuzufügen:

### 1. Konstante in CalculatorConstants definieren

```java
// In der CalculatorConstants-Klasse
public static final double GOLDENER_SCHNITT = 1.61803398875;
```

### 2. Konstante in der CalculationEngine erkennen

```java
// In der isConstantExpression-Methode
if (expression.equals("pi") || expression.equals("e") || expression.equals("phi") ||
        expression.equals("sqrt2") || expression.equals("sqrt3") || expression.equals("golden") ||
        expression.equals("goldenerSchnitt")) { // Neue Konstante hinzugefügt
    return true;
}

// In der berechneAusdruck-Methode
if (ausdruck.equals("goldenerSchnitt") || ausdruck.equals("phi")) {
    return CalculatorConstants.GOLDENER_SCHNITT;
}
```

### 3. UI-Komponente hinzufügen (optional)

```java
JButton phiButton = new JButton("φ");
phiButton.addActionListener(e -> inputHandler.handleInput("goldenerSchnitt"));
constantsPanel.add(phiButton);
```

## UI-Komponenten anpassen

Um Steuerelemente wie Schaltflächen oder Schieberegler anzupassen:

### 1. Modifizieren existierender Tasten

Öffnen Sie die Klasse mit den Tastenkomponenten und passen Sie das Layout oder die Funktionalität an:

```java
// In TaschenrechnerKeypad
JButton equalsButton = new JButton("=");
equalsButton.setBackground(new Color(0, 120, 215)); // Blauer Hintergrund
equalsButton.setForeground(Color.WHITE); // Weiße Schrift
equalsButton.setFont(new Font("Arial", Font.BOLD, 14)); // Größere Schrift
equalsButton.addActionListener(e -> inputHandler.handleEquals());
mainPanel.add(equalsButton);
```

### 2. Hinzufügen eines Tooltips 

```java
JButton clearButton = new JButton("C");
clearButton.setToolTipText("Löscht den aktuellen Ausdruck");
clearButton.addActionListener(e -> inputHandler.handleClear());
```

### 3. Tastaturkürzel hinzufügen

```java
// Tastaturkürzel für die Taste 'C' zum Löschen
KeyStroke clearKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
clearButton.registerKeyboardAction(
    e -> inputHandler.handleClear(),
    clearKeyStroke,
    JComponent.WHEN_IN_FOCUSED_WINDOW
);
```

## Best Practices

### Erweiterungsrichtlinien

1. **Folgen Sie dem Prinzip der Einzelverantwortung (SRP)**: 
   - Eine Klasse oder Methode sollte nur für eine Sache verantwortlich sein
   - Halten Sie Methoden kurz und fokussiert

2. **Verwenden Sie defensive Programmierung**:
   - Überprüfen Sie Eingabeparameter auf Gültigkeit
   - Fangen Sie Ausnahmen an geeigneten Stellen ab
   - Dokumentieren Sie bekannte Einschränkungen

3. **Schreiben Sie Tests für neue Funktionalität**:
   - Einheitstests helfen, Regression zu verhindern
   - Verwenden Sie JUnit für automatisierte Tests

4. **Dokumentieren Sie Ihren Code**:
   - Verwenden Sie Javadoc-Kommentare für öffentliche Methoden und Klassen
   - Beschreiben Sie nicht nur WAS der Code tut, sondern auch WARUM

5. **Beibehalten der Benutzeroberflächen-Konsistenz**:
   - Folgen Sie bestehenden UI-Mustern
   - Verwenden Sie ähnliche Größen, Abstände und Farbschemata

### Häufige Fehler vermeiden

1. **Vermeiden Sie hart codierte Pfade oder Werte**:
   - Verwenden Sie Konstanten für wiederverwendete Werte
   - Lesen Sie Konfigurationswerte aus Eigenschaften-Dateien

2. **Keine Logik in der UI-Schicht**:
   - Trennen Sie Geschäftslogik von der Darstellung
   - Halten Sie UI-Klassen leichtgewichtig

3. **Vermeiden Sie Code-Duplikation**:
   - Extrahieren Sie gemeinsame Funktionalität in separate Methoden oder Klassen
   - Verwenden Sie Vererbung oder Komposition, um Code wiederzuverwenden

## 3D-Plotter anpassen

Der 3D-Plotter bietet mehrere Möglichkeiten zur Anpassung und Erweiterung. Hier sind die wichtigsten Anpassungsmöglichkeiten:

### 1. Anpassen der Benutzeroberfläche

Die Benutzeroberfläche des 3D-Plotters wird in der `Plot3DUIBuilder`-Klasse erstellt. Sie können vorhandene UI-Elemente anpassen oder neue hinzufügen:

```java
// In Plot3DUIBuilder.java, in der createControlPanel-Methode
// Neues Bedienelement hinzufügen
JPanel customPanel = new JPanel(new BorderLayout());
customPanel.setBorder(BorderFactory.createTitledBorder("Meine Anpassung"));
JButton customButton = new JButton("Benutzerdefinierte Aktion");
customButton.addActionListener(e -> {
    // Benutzerdefinierte Aktion ausführen
    mainPanel.debug("Benutzerdefinierte Aktion ausgeführt");
});
customPanel.add(customButton, BorderLayout.CENTER);

// Panel zur Steuerung hinzufügen (vor dem vertikalen Glue)
panel.add(Box.createVerticalStrut(10));
panel.add(customPanel);
```

### 2. Scrollfunktionalität anpassen

Die Scrollfunktionalität der Steuerungsleiste kann über Anpassung der `createUI`-Methode in `Plot3DPanel` geändert werden:

```java
// In Plot3DPanel.java, in der createUI-Methode
JScrollPane controlScrollPane = new JScrollPane(controlPanel);
// Scrollgeschwindigkeit anpassen
controlScrollPane.getVerticalScrollBar().setUnitIncrement(24); // Schnelleres Scrollen
// Scrollbars anpassen
controlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
```

### 3. Auflösungsslider anpassen

Der Auflösungsslider kann in der `createResolutionPanel`-Methode von `Plot3DUIBuilder` angepasst werden:

```java
// In Plot3DUIBuilder.java, in der createResolutionPanel-Methode
// Slider-Bereich ändern (z.B. von 10-100 auf 10-200)
resolutionSlider = new JSlider(JSlider.HORIZONTAL, 10, 200, sliderValue);
// Teilstriche ändern
resolutionSlider.setMajorTickSpacing(50);
resolutionSlider.setMinorTickSpacing(10);
```

### 4. Direkte Auflösungsanwendung anpassen

Das Verhalten des Auflösungssliders kann in der `stateChanged`-Methode des ChangeListener angepasst werden:

```java
// In Plot3DUIBuilder.java, im ChangeListener des Sliders
resolutionSlider.addChangeListener(new ChangeListener() {
    @Override
    public void stateChanged(ChangeEvent e) {
        int value = resolutionSlider.getValue();
        
        // Synchronisiere das Textfeld mit dem Slider
        resolutionField.setText(String.valueOf(value));
        
        // Nur bei Änderungen über 5 Einheiten oder am Ende des Schiebens aktualisieren
        // Dies reduziert die Anzahl der Neuberechnungen während des Schiebens
        if (!resolutionSlider.getValueIsAdjusting() || 
            Math.abs(value - viewController.getCurrentResolution()) > 5) {
            // Auflösung direkt über das Textfeld anwenden
            applyResolution(resolutionField);
            ((plugins.plotter3d.Plot3DPanel)mainPanel).debug("Auflösung über Slider auf " + value + " gesetzt");
        }
    }
});
```

### 5. Neue Visualisierungsoptionen hinzufügen

Sie können neue Visualisierungsoptionen in `Plot3DUIBuilder.createDisplayOptionsPanel()` hinzufügen:

```java
// In Plot3DUIBuilder.java, im createDisplayOptionsPanel
// Neue Checkbox für benutzerdefinierte Visualisierung
JCheckBox myVisualizationCheckbox = new JCheckBox("Meine benutzerdefinierte Visualisierung", false);
myVisualizationCheckbox.addActionListener(e -> {
    boolean isSelected = myVisualizationCheckbox.isSelected();
    // Benutzerdefinierte Visualisierung aktivieren/deaktivieren
    if (mainPanel != null && mainPanel.getRenderer() != null) {
        mainPanel.getRenderer().setMyCustomVisualization(isSelected);
        ((plugins.plotter3d.Plot3DPanel)mainPanel).debug("Benutzerdefinierte Visualisierung: " + 
            (isSelected ? "aktiviert" : "deaktiviert"));
    }
    // Neu zeichnen
    ((plugins.plotter3d.Plot3DPanel)mainPanel).renderPlot();
});
displayOptionsPanel.add(myVisualizationCheckbox);
```

### 6. Höhenanpassung der Funktions- und Beispiellisten

Die Mindesthöhe der Funktions- und Beispiellisten kann in `Plot3DUIBuilder.createControlPanel()` angepasst werden:

```java
// In Plot3DUIBuilder.java, im createControlPanel
// Für das Beispielpanel
if (examplesPanel != null) {
    examplesPanel.setMinimumSize(new Dimension(100, 200)); // Erhöht von 150 auf 200
    examplesPanel.setPreferredSize(new Dimension(100, 200));
}

// Oder direkt in createFunctionListPanel
JScrollPane scrollPane = new JScrollPane(functionManager.getFunctionList());
scrollPane.setPreferredSize(new Dimension(100, 200)); // Erhöht von 150 auf 200
scrollPane.setMinimumSize(new Dimension(100, 200));
```

Diese Anpassungen ermöglichen es, den 3D-Plotter an spezifische Anforderungen anzupassen und die Benutzererfahrung zu verbessern.

## Aktuelle Verbesserungen

In den neuesten Versionen wurden folgende Verbesserungen und Bugfixes implementiert:

### 1. Verbesserte applyResolution-Methode im 3D-Plotter

Die `applyResolution`-Methode in `Plot3DUIBuilder.java` wurde verbessert, um besseres Feedback an den Benutzer zu liefern und die Stabilität zu erhöhen:

```java
private void applyResolution(JTextField field) {
    try {
        // Parse und validiere den eingegebenen Wert
        int value = Integer.parseInt(field.getText().trim());

        // Aktueller Wert vor der Änderung (für Statusanzeige)
        int currentResolution = viewController.getCurrentResolution();

        // Sinnvolle Grenzen für die Auflösung
        if (value < 10) {
            value = 10; // Minimale Auflösung
            field.setText("10");
            this.mainPanel.debug("Auflösung auf Minimalwert 10 begrenzt (vorher: " + currentResolution + ")");
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "Die Auflösung wurde auf den Minimalwert 10 gesetzt.",
                    "Auflösung angepasst",
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (value > 500) {
            // Bei extrem hohen Werten eine Warnung anzeigen
            int option = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "Eine Auflösung von " + value + " ist extrem hoch und kann zu Systemabstürzen führen.\n" +
                            "Empfohlene Obergrenze: 300\n\n" +
                            "Möchten Sie den Wert auf 300 begrenzen?",
                    "Warnung: Sehr hohe Auflösung",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                value = 300;
                field.setText("300");
                this.mainPanel.debug(
                        "Auflösung auf empfohlenen Maximalwert 300 begrenzt (vorher: " + currentResolution + ")");
            } else {
                this.mainPanel.debug("Benutzer hat extrem hohen Auflösungswert " + value + " bestätigt (vorher: "
                        + currentResolution + ")");
            }
        }

        // Aktualisiere den Slider, wenn der Wert in seinem Bereich liegt
        if (value <= 100) {
            resolutionSlider.setValue(value);
        }

        // Auflösung direkt im ViewController setzen
        viewController.setResolution(value);

        // Erfolgsbestätigung mit Vergleich zum vorherigen Wert
        if (value != currentResolution) {
            this.mainPanel.debug("Auflösung erfolgreich von " + currentResolution + " auf " + value + " geändert");
        } else {
            this.mainPanel.debug("Auflösung unverändert bei " + value);
        }
    } catch (NumberFormatException ex) {
        // Bei ungültiger Eingabe den aktuellen Wert aus dem ViewController holen
        int currentResolution = viewController.getCurrentResolution();
        field.setText(String.valueOf(currentResolution));

        this.mainPanel.debug("Ungültige Auflösung eingegeben: " + ex.getMessage() + " - Wert zurückgesetzt auf "
                + currentResolution);
        JOptionPane.showMessageDialog(
                mainPanel,
                "Bitte geben Sie eine gültige Zahl für die Auflösung ein.\n" +
                        "Der Wert wurde auf " + currentResolution + " zurückgesetzt.",
                "Ungültige Eingabe",
                JOptionPane.ERROR_MESSAGE);
    }
}
```

Die wichtigsten Änderungen in dieser Methode:

1. **Verbessertes Feedback**: Die Methode speichert nun den aktuellen Wert vor der Änderung, um in Debug-Meldungen anzuzeigen, von welchem Wert zu welchem gewechselt wurde.

2. **Validierung des Sliders**: Der Slider wird nur aktualisiert, wenn der Wert innerhalb des Slider-Bereichs (≤ 100) liegt, wodurch Inkonsistenzen zwischen Slider und tatsächlicher Auflösung vermieden werden.

3. **Fehlerbehandlung**: Bei ungültiger Eingabe wird der aktuelle Wert direkt aus dem ViewController anstatt aus dem Slider abgerufen, was die Robustheit verbessert.

4. **Benutzerfreundliche Warnungen**: Bei sehr hohen Werten wird eine Warnung mit der Option zur Begrenzung angezeigt, statt den Wert stillschweigend zu reduzieren.

5. **Detailliertes Logging**: Ausführliche Debug-Meldungen dokumentieren alle Änderungen und Entscheidungen.

Diese Verbesserungen tragen zu einer stabileren und benutzerfreundlicheren Oberfläche bei, indem sie transparentes Feedback geben und potenzielle Performanceprobleme durch zu hohe Auflösungen vermeiden.

### 2. Z-Wert-Bereichsbehandlung in Plot3DModel

Zusätzlich wurde die Bereichsberechnung für Z-Werte im 3D-Plotter verbessert:

- In `Plot3DModel.java` wird nun sichergestellt, dass auch ohne aktive Funktionen oder bei fehlenden gültigen Z-Werten ein sinnvoller Z-Bereich basierend auf dem X/Y-Bereich festgelegt wird.
- In `Plot3DFunctionManager.java` wurde die `clearAllFunctions`-Methode angepasst, um sicherzustellen, dass nach dem Löschen aller Funktionen ein explizites Neuzeichnen mit aktualisierten Z-Wertebereichen erfolgt.

Diese Änderungen sorgen dafür, dass der 3D-Plotter immer einen praktischen Z-Wertebereich anzeigt, anstatt "Infinity, -Infinity" anzuzeigen, wenn keine Funktionen vorhanden sind.

```java
</rewritten_file> 