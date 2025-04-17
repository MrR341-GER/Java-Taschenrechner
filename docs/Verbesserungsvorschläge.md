# Verbesserungsvorschläge für den Java-Taschenrechner

Dieses Dokument enthält Vorschläge für zukünftige Verbesserungen am Java-Taschenrechner, insbesondere für den 2D-Plotter, basierend auf den erfolgreichen Implementierungen im 3D-Plotter.

## 2D-Plotter Verbesserungen

### 1. Ansichtssteuerung verbessern

Die `ViewControlPanel`-Klasse des 2D-Plotters könnte ähnlich verbessert werden wie die `applyResolution`-Methode im 3D-Plotter:

```java
private void centerGraphView() {
    try {
        // Liest die eingegebenen Werte aus
        String xText = xCenterField.getText().trim();
        String yText = yCenterField.getText().trim();

        if (xText.isEmpty() || yText.isEmpty()) {
            JOptionPane.showMessageDialog(plotter,
                    "Bitte geben Sie gültige X- und Y-Werte ein.",
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Aktuelle Werte vor der Änderung (für Statusanzeige)
        Point2D.Double currentCenter = plotter.getGraphPanel().getViewCenter();
        
        // Versucht, die Werte zu parsen (unterstützt sowohl Punkt als auch Komma)
        double xCenter = parseDecimal(xText);
        double yCenter = parseDecimal(yText);

        // Optionale Validierung gegen einen sinnvollen Bereich
        double MAX_COORDINATE = 1000000;
        if (Math.abs(xCenter) > MAX_COORDINATE || Math.abs(yCenter) > MAX_COORDINATE) {
            int option = JOptionPane.showConfirmDialog(
                plotter,
                "Die eingegebenen Koordinaten sind sehr weit vom Ursprung entfernt.\n" +
                "Dies kann zu Darstellungsproblemen führen.\n\n" +
                "Möchten Sie trotzdem fortfahren?",
                "Extreme Koordinaten",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (option != JOptionPane.YES_OPTION) {
                // Zurücksetzen auf aktuelle Werte
                xCenterField.setText(plotter.getCoordinateFormat().format(currentCenter.x));
                yCenterField.setText(plotter.getCoordinateFormat().format(currentCenter.y));
                plotter.debug("Zentrierung abgebrochen wegen extremer Koordinaten");
                return;
            }
        }

        // Ruft die Methode zum Zentrieren der Ansicht im GraphPanel auf
        plotter.getGraphPanel().centerViewAt(xCenter, yCenter);
        
        // Erfolgsbestätigung mit Vergleich zum vorherigen Wert
        plotter.debug("Ansicht zentriert von (" + 
            plotter.getCoordinateFormat().format(currentCenter.x) + ", " + 
            plotter.getCoordinateFormat().format(currentCenter.y) + ") auf (" + 
            plotter.getCoordinateFormat().format(xCenter) + ", " + 
            plotter.getCoordinateFormat().format(yCenter) + ")");

        // Aktualisiert auch die Schnittmengenliste, falls Schnittpunkte angezeigt werden
        if (plotter.isShowingIntersections()) {
            plotter.updateIntersectionList();
        }

    } catch (NumberFormatException | ParseException e) {
        plotter.debug("Fehler beim Zentrieren: " + e.getMessage());
        JOptionPane.showMessageDialog(plotter,
                "Ungültige Zahlenformate. Bitte geben Sie gültige Zahlen ein." +
                        "\n(Hinweis: Sowohl Punkt als auch Komma als Dezimaltrennzeichen werden akzeptiert)",
                "Eingabefehler",
                JOptionPane.ERROR_MESSAGE);
    }
}
```

### 2. Funktionslistenverwaltung verbessern

Derzeit ist die Funktionsverwaltung im 2D-Plotter über `FunctionInputPanel` implementiert. Diese könnte verbessert werden, ähnlich dem Ansatz des `Plot3DFunctionManager`:

1. **Verbesserte Sichtbarkeitssteuerung**: Der aktuelle Checkbox-Mechanismus könnte um Statusfeedback erweitert werden:

```java
private void toggleFunctionVisibility(int index) {
    if (index < 0 || index >= functionListModel.size())
        return;

    GraphPanel graphPanel = plotter.getGraphPanel();
    List<FunctionRenderer.FunctionInfo> functions = graphPanel.getFunctionRenderer().getFunctions();

    if (index < functions.size()) {
        // Sichtbarkeit im Modell umschalten
        boolean newVisibility = functions.get(index).toggleVisibility();
        
        // Debug-Info mit altem und neuem Status
        plotter.debug("Funktion #" + (index + 1) + " Sichtbarkeit geändert: " + 
                      (newVisibility ? "sichtbar" : "unsichtbar"));
        
        // Aktualisiere die Anzeige der Liste
        String item = functionListModel.getElementAt(index);
        
        // Aktualisiere den Eintrag in der Liste
        updateFunctionListItem(index, functions.get(index));
        
        // Neuzeichnen des Graphen
        graphPanel.repaint();
    }
}
```

2. **Funktionslöschung mit Bestätigung**:

```java
private void removeSelectedFunction() {
    int selectedIndex = functionList.getSelectedIndex();
    if (selectedIndex != -1) {
        String functionName = getFunctionName(selectedIndex);
        
        // Bestätigungsdialog für das Löschen
        int choice = JOptionPane.showConfirmDialog(
            plotter,
            "Möchten Sie die Funktion '" + functionName + "' wirklich entfernen?",
            "Funktion entfernen",
            JOptionPane.YES_NO_OPTION);
            
        if (choice == JOptionPane.YES_OPTION) {
            // Entferne die Funktion aus der Renderer-Liste
            GraphPanel graphPanel = plotter.getGraphPanel();
            List<FunctionRenderer.FunctionInfo> functions = graphPanel.getFunctionRenderer().getFunctions();
            
            if (selectedIndex < functions.size()) {
                functions.remove(selectedIndex);
                functionListModel.remove(selectedIndex);
                
                // Neuzeichnen
                graphPanel.repaint();
                
                // Schnittpunkte aktualisieren, falls sichtbar
                if (plotter.isShowingIntersections()) {
                    plotter.updateIntersectionList();
                }
                
                plotter.debug("Funktion '" + functionName + "' entfernt");
            }
        }
    }
}
```

### 3. Fehlerbehebung bei Funktionseingabe

Der Prozess zur Eingabe und Validierung von Funktionen könnte verbessert werden:

```java
private void addFunction() {
    String functionText = functionField.getText().trim();
    if (functionText.isEmpty()) {
        plotter.debug("Keine Funktion eingegeben");
        return;
    }
    
    try {
        // Versuche die Funktion zu parsen, um syntaktische Fehler zu erkennen
        parser.FunctionParser parser = new parser.FunctionParser();
        parser.parseFunction(functionText);
        
        // Aktuelle Farbe bestimmen
        Color functionColor = determineColor();
        String colorName = (String) colorComboBox.getSelectedItem();
        
        // Funktion zum GraphPanel hinzufügen
        plotter.getGraphPanel().addFunction(functionText, functionColor);
        
        // Funktioneintrag erstellen mit Sichtbarkeitsmarkierung
        String listEntry = "[x] f(x) = " + functionText + " [" + colorName + "]";
        functionListModel.addElement(listEntry);
        
        // Auswählen der neuen Funktion in der Liste
        int newIndex = functionListModel.size() - 1;
        functionList.setSelectedIndex(newIndex);
        
        // Scrolle zur neuen Funktion
        functionList.ensureIndexIsVisible(newIndex);
        
        // Debug-Meldung
        plotter.debug("Funktion hinzugefügt: f(x) = " + functionText + " mit Farbe " + colorName);
        
        // Schnittpunkte aktualisieren, falls sichtbar
        if (plotter.isShowingIntersections()) {
            plotter.updateIntersectionList();
        }
        
    } catch (Exception e) {
        // Fehlermeldung anzeigen
        plotter.debug("Fehler beim Hinzufügen der Funktion: " + e.getMessage());
        JOptionPane.showMessageDialog(
            plotter,
            "Die Funktion konnte nicht hinzugefügt werden:\n" + e.getMessage(),
            "Fehler bei der Funktionseingabe",
            JOptionPane.ERROR_MESSAGE);
    }
}
```

### 4. Anzeigeverbesserungen

#### Einstellbarer Wertebereich

Ähnlich wie im 3D-Plotter könnte ein Panel zur Einstellung des Wertebereichs hinzugefügt werden:

```java
public JPanel createRangePanel() {
    JPanel rangePanel = new JPanel(new GridLayout(2, 4, 5, 5));
    rangePanel.setBorder(BorderFactory.createTitledBorder("Wertebereich"));
    
    // X-Bereich
    rangePanel.add(new JLabel("X min:"));
    JTextField xMinField = new JTextField("-10");
    rangePanel.add(xMinField);
    
    rangePanel.add(new JLabel("X max:"));
    JTextField xMaxField = new JTextField("10");
    rangePanel.add(xMaxField);
    
    // Y-Bereich
    rangePanel.add(new JLabel("Y min:"));
    JTextField yMinField = new JTextField("-10");
    rangePanel.add(yMinField);
    
    rangePanel.add(new JLabel("Y max:"));
    JTextField yMaxField = new JTextField("10");
    rangePanel.add(yMaxField);
    
    // Anwenden-Button
    JButton applyButton = new JButton("Anwenden");
    applyButton.addActionListener(e -> {
        try {
            double xMin = Double.parseDouble(xMinField.getText().trim());
            double xMax = Double.parseDouble(xMaxField.getText().trim());
            double yMin = Double.parseDouble(yMinField.getText().trim());
            double yMax = Double.parseDouble(yMaxField.getText().trim());
            
            if (xMin >= xMax || yMin >= yMax) {
                plotter.debug("Ungültiger Wertebereich: Min muss kleiner als Max sein");
                JOptionPane.showMessageDialog(
                    plotter,
                    "Der Minimum-Wert muss kleiner als der Maximum-Wert sein.",
                    "Ungültiger Wertebereich",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Implementiere setViewBounds in GraphPanel
            // graphPanel.setViewBounds(xMin, xMax, yMin, yMax);
            plotter.debug("Wertebereich gesetzt: X=[" + xMin + "," + xMax + "], Y=[" + yMin + "," + yMax + "]");
            
        } catch (NumberFormatException ex) {
            plotter.debug("Ungültige Eingabe im Wertebereich: " + ex.getMessage());
            JOptionPane.showMessageDialog(
                plotter,
                "Bitte geben Sie gültige Zahlen für den Wertebereich ein.",
                "Ungültige Eingabe",
                JOptionPane.ERROR_MESSAGE);
        }
    });
    
    rangePanel.add(applyButton);
    
    return rangePanel;
}
```

#### Verbesserte Mausbewegungsinformationen

Aktuelle Koordinaten unter dem Mauszeiger könnten in einer Statusleiste angezeigt werden:

```java
private JPanel createStatusPanel() {
    JPanel statusPanel = new JPanel(new BorderLayout());
    JLabel coordinateLabel = new JLabel("X: 0.00, Y: 0.00");
    statusPanel.add(coordinateLabel, BorderLayout.WEST);
    
    // MouseMotionListener im GraphPanel aktualisieren
    graphPanel.addMouseMotionListener(new MouseAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            // Konvertiere Bildschirmkoordinaten in mathematische Koordinaten
            Point2D.Double mathPoint = transformer.screenToMath(e.getPoint());
            
            // Aktualisiere Label mit formatierten Koordinaten
            coordinateLabel.setText(String.format("X: %.2f, Y: %.2f", 
                                   mathPoint.x, mathPoint.y));
            
            // Vorhandene mouseMoved-Logik hier beibehalten
            // ...
        }
    });
    
    return statusPanel;
}
```

## Allgemeine Verbesserungen

### 1. Einheitlicher Debug-Mechanismus

Ein vereinheitlichter Debug-Mechanismus für alle Komponenten würde die Konsistenz verbessern:

```java
public interface Debuggable {
    void debug(String message);
    DebugManager getDebugManager();
    void setDebugManager(DebugManager manager);
}
```

### 2. Einheitliches Theming

Ein zentraler Theme-Manager könnte implementiert werden, um konsistente Farben und Stile in allen Komponenten zu gewährleisten:

```java
public class ThemeManager {
    private static ThemeManager instance;
    private Color primaryColor = new Color(0, 120, 215);
    private Color backgroundColor = Color.WHITE;
    private Color gridColor = Color.LIGHT_GRAY;
    private Color textColor = Color.BLACK;
    private Font defaultFont = new Font("Arial", Font.PLAIN, 12);
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void applyTheme(JComponent component) {
        if (component instanceof JButton) {
            component.setBackground(primaryColor);
            component.setForeground(Color.WHITE);
        } else {
            component.setBackground(backgroundColor);
            component.setForeground(textColor);
        }
        component.setFont(defaultFont);
    }
    
    // Getter und Setter für Theme-Eigenschaften
}
```

Diese Verbesserungen würden die Benutzerfreundlichkeit und Konsistenz des Java-Taschenrechners weiter erhöhen und ihn zu einem noch wertvolleren Werkzeug für mathematische Berechnungen und Visualisierungen machen. 