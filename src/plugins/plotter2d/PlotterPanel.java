import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlotterPanel mit Spalten-Layout - linke Spalte für den Graph, rechte Spalte
 * für die Steuerelemente
 */
public class PlotterPanel extends JPanel {
    private final GraphPanel graphPanel;
    private final FunctionInputPanel functionInputPanel;
    private final ViewControlPanel viewControlPanel;
    private final IntersectionPanel intersectionPanel;
    private final ExamplePanel examplePanel;
    private JCheckBox showGridCheckbox;

    // Debug-Referenz
    private DebugManager debugManager;

    // Formatter for coordinates
    private final DecimalFormat coordinateFormat = new DecimalFormat("0.##");

    // Pattern zur Extraktion der Farbe aus einem Funktionseintrag
    private final Pattern colorPattern = Pattern.compile("\\[(.+)\\]$");

    /**
     * Creates a new PlotterPanel
     */
    public PlotterPanel() {
        setLayout(new BorderLayout(5, 5));

        // Create graph panel
        graphPanel = new GraphPanel();
        // Sorge dafür, dass der Graph einen großen Teil des verfügbaren Raums einnimmt
        graphPanel.setPreferredSize(new Dimension(600, 400));

        // Tooltip-Einstellungen verbessern
        ToolTipManager.sharedInstance().setInitialDelay(100); // Weniger Verzögerung beim Anzeigen des Tooltips
        ToolTipManager.sharedInstance().setDismissDelay(10000); // Tooltip länger anzeigen

        // Create helper panels
        functionInputPanel = new FunctionInputPanel(this);
        viewControlPanel = new ViewControlPanel(this);
        intersectionPanel = new IntersectionPanel(this);
        examplePanel = new ExamplePanel(this, getTextField());

        // Add listeners for dynamic updates
        graphPanel.addPropertyChangeListener("intersectionsUpdated", evt -> {
            if (intersectionPanel.isShowingIntersections()) {
                updateIntersectionList();
                debug("Schnittpunktliste nach Update aktualisiert");
            }
        });

        graphPanel.addPropertyChangeListener("viewChanged", evt -> {
            viewControlPanel.updateCenteringFields();
        });

        // ===== Spaltenbasiertes Layout =====

        // Erstelle ein Panel für die rechte Spalte (Kontrollelemente)
        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BoxLayout(rightColumnPanel, BoxLayout.Y_AXIS));

        // 1. Anzeigeoptionen (ganz oben)
        JPanel displayOptionsPanel = new JPanel();
        displayOptionsPanel.setBorder(BorderFactory.createTitledBorder("Anzeigeoptionen"));
        displayOptionsPanel.setLayout(new BoxLayout(displayOptionsPanel, BoxLayout.Y_AXIS));

        // Checkbox für Koordinatensystem
        JPanel gridCheckboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showGridCheckbox = new JCheckBox("Koordinatensystem anzeigen");
        showGridCheckbox.setSelected(true); // Standard: eingeschaltet
        showGridCheckbox.addActionListener(e -> {
            graphPanel.setShowGrid(showGridCheckbox.isSelected());
            debug("Koordinatensystem-Anzeige: " + (showGridCheckbox.isSelected() ? "eingeschaltet" : "ausgeschaltet"));
        });
        gridCheckboxPanel.add(showGridCheckbox);

        // Füge Checkbox zum Optionen-Panel hinzu
        displayOptionsPanel.add(gridCheckboxPanel);

        // 2. Funktionseingabe mit Buttons
        JPanel inputSection = new JPanel(new BorderLayout(5, 5));
        inputSection.add(functionInputPanel.createFunctionInputPanel(), BorderLayout.NORTH);
        inputSection.add(functionInputPanel.createActionButtonPanel(), BorderLayout.CENTER);

        // 3. Zwei Spalten: Links Beispiele, rechts Funktionsliste
        JPanel middleSection = new JPanel(new GridLayout(1, 2, 5, 5));

        // Beispiele
        JPanel examplesWrapper = new JPanel(new BorderLayout());
        examplesWrapper.add(examplePanel.createExamplesPanel(), BorderLayout.CENTER);

        // Funktionsliste
        JPanel functionsWrapper = new JPanel(new BorderLayout());
        functionsWrapper.add(functionInputPanel.createFunctionListPanel(), BorderLayout.CENTER);

        middleSection.add(examplesWrapper);
        middleSection.add(functionsWrapper);

        // 4. Ansicht zentrieren
        JPanel viewSection = viewControlPanel.createViewControlPanel();

        // Füge alle Abschnitte zur rechten Spalte hinzu (neue Reihenfolge)
        rightColumnPanel.add(displayOptionsPanel);
        rightColumnPanel.add(Box.createVerticalStrut(10));
        rightColumnPanel.add(inputSection);
        rightColumnPanel.add(Box.createVerticalStrut(10));
        rightColumnPanel.add(middleSection);
        rightColumnPanel.add(Box.createVerticalStrut(10));
        rightColumnPanel.add(viewSection);
        rightColumnPanel.add(Box.createVerticalGlue()); // Füllt verbleibenden Platz

        // Erstelle einen Split zwischen Graph und Steuerelementen
        JSplitPane mainSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                graphPanel,
                rightColumnPanel);
        mainSplitPane.setResizeWeight(0.7); // Graph bekommt 70% des Platzes
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);

        // Hauptlayout
        add(mainSplitPane, BorderLayout.CENTER);

        // Initialisiere Zentrierung
        viewControlPanel.updateCenteringFields();

        // Stelle sicher, dass die rechte Spalte eine angemessene Mindestbreite hat
        rightColumnPanel.setMinimumSize(new Dimension(250, 300));

        // Zeichne das Panel neu nach der Initialisierung
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(0.7);
            revalidate();
            repaint();
        });

        debug("Funktionsplotter initialisiert");
    }

    /**
     * Setzt den DebugManager für Logging
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
        debug("Debug-Manager für Funktionsplotter gesetzt");
    }

    /**
     * Schreibt Debug-Informationen in das Debug-Fenster
     */
    public void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[Plotter] " + message);
        } else {
            // Fallback, wenn kein DebugManager verfügbar ist
            System.out.println("[Plotter] " + message);
        }
    }

    /**
     * Returns the graph panel
     */
    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    /**
     * Returns the coordinate format
     */
    public DecimalFormat getCoordinateFormat() {
        return coordinateFormat;
    }

    /**
     * Returns the grid checkbox
     */
    public JCheckBox getGridCheckbox() {
        return showGridCheckbox;
    }

    /**
     * Returns the intersection checkbox
     */
    public JCheckBox getIntersectionCheckbox() {
        return intersectionPanel.getShowIntersectionsCheckbox();
    }

    /**
     * Aktiviert oder deaktiviert die Anzeige von Schnittpunkten und deren Tooltips
     */
    public void toggleIntersections(boolean show) {
        JCheckBox checkbox = intersectionPanel.getShowIntersectionsCheckbox();
        if (checkbox.isSelected() != show) {
            checkbox.doClick(); // Simuliere Klick auf Checkbox (löst ActionListener aus)
        }
        debug("Schnittpunkte-Anzeige: " + (show ? "eingeschaltet" : "ausgeschaltet") +
                ", Tooltip-Support " + (show ? "aktiviert" : "deaktiviert"));
    }

    /**
     * Returns the function field from the function input panel
     */
    private JTextField getTextField() {
        // This is a workaround since we don't have direct access to the function field
        // In a real refactoring, we would modify the FunctionInputPanel class to expose
        // this
        Component[] components = functionInputPanel.createFunctionInputPanel().getComponents();
        for (Component component : components) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            }
        }
        return new JTextField(); // Fallback, should never happen
    }

    /**
     * Updates the graph from the function list
     */
    public void updateGraphFromList() {
        debug("Aktualisiere Graphen aus der Funktionsliste");
        graphPanel.clearFunctions();

        DefaultListModel<String> listModel = functionInputPanel.getFunctionListModel();

        for (int i = 0; i < listModel.size(); i++) {
            String entry = listModel.get(i);

            // Extract the actual function from the list entry
            int equalsPos = entry.indexOf('=');
            int bracketPos = entry.lastIndexOf('[');

            if (equalsPos < 0 || bracketPos < 0) {
                debug("Ungültiger Eintrag gefunden: " + entry);
                continue; // Ungültiger Eintrag
            }

            String funcPart = entry.substring(equalsPos + 1, bracketPos).trim();

            // Extract the color using regex
            Matcher matcher = colorPattern.matcher(entry);
            Color color;

            if (matcher.find()) {
                String colorName = matcher.group(1);
                // Verwende deterministische Zufallsfarbe bei "Zufällig"
                if (colorName.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
                    color = ColorChooser.generateDeterministicRandomColor(funcPart);
                    debug("Deterministische Zufallsfarbe für Funktion '" + funcPart + "' generiert");
                } else {
                    color = ColorChooser.getColorByName(colorName);
                    debug("Farbe '" + colorName + "' für Funktion '" + funcPart + "' verwendet");
                }
            } else {
                // Fallback - dynamische Farbe generieren
                color = ColorChooser.generateColor();
                debug("Fallback-Farbe für Funktion '" + funcPart + "' generiert");
            }

            // Add function
            graphPanel.addFunction(funcPart, color);
            debug("Funktion hinzugefügt: " + funcPart);
        }

        // Update intersections if enabled
        if (intersectionPanel.isShowingIntersections()) {
            graphPanel.toggleIntersections(true);
            updateIntersectionList();
            debug("Schnittpunkte aktualisiert");
        }
    }

    /**
     * Returns if intersections are being shown
     */
    public boolean isShowingIntersections() {
        return intersectionPanel.isShowingIntersections();
    }

    /**
     * Updates the intersection list with the current intersection points
     */
    public void updateIntersectionList() {
        debug("Aktualisiere Schnittpunktliste");
        // Clear the list
        DefaultListModel<String> listModel = intersectionPanel.getIntersectionListModel();
        listModel.clear();

        // Get intersection points from the GraphPanel
        List<IntersectionPoint> intersectionPoints = graphPanel.getIntersectionPoints();

        if (intersectionPoints.isEmpty()) {
            listModel.addElement("Keine Schnittpunkte gefunden");
            debug("Keine Schnittpunkte gefunden");
            return;
        }

        // For each intersection point, add to the list
        for (int i = 0; i < intersectionPoints.size(); i++) {
            IntersectionPoint point = intersectionPoints.get(i);

            // Extract function expressions from the function list for better display
            String func1 = getFunctionExpressionByIndex(point.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(point.getFunctionIndex2());

            // Create an informative list entry
            String entry = "S" + (i + 1) + ": (" + coordinateFormat.format(point.x) + ", " +
                    coordinateFormat.format(point.y) + ") ";
            entry += "zwischen " + func1 + " und " + func2;

            listModel.addElement(entry);
            debug("Schnittpunkt gefunden: " + entry);
        }
    }

    /**
     * Helper method to get the function expression by index
     */
    private String getFunctionExpressionByIndex(int index) {
        DefaultListModel<String> listModel = functionInputPanel.getFunctionListModel();

        if (index < 0 || index >= listModel.size()) {
            return "f" + (index + 1);
        }

        String entry = listModel.get(index);

        // Extract the function expression (between "=" and "[")
        int equalsPos = entry.indexOf('=');
        int bracketPos = entry.lastIndexOf('[');

        if (equalsPos >= 0 && bracketPos > equalsPos) {
            return entry.substring(equalsPos + 1, bracketPos).trim();
        } else {
            return "f" + (index + 1);
        }
    }
}