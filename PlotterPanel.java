import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

/**
 * PlotterPanel ist ein Container-Panel, das GraphPanel und Steuerelemente
 * enthält
 */
public class PlotterPanel extends JPanel {
    private final GraphPanel graphPanel;
    private final JTextField functionField;
    private final JComboBox<String> colorComboBox;
    private final DefaultListModel<String> functionListModel;
    private final JList<String> functionList;
    private JCheckBox showIntersectionsCheckbox;

    // Komponenten für die Schnittpunktliste
    private DefaultListModel<String> intersectionListModel;
    private JList<String> intersectionList;
    private JPanel intersectionPanel;

    // Eingabefelder für Zentrierung
    private JTextField xCenterField;
    private JTextField yCenterField;

    // Formatter für Koordinaten
    private final DecimalFormat coordinateFormat = new DecimalFormat("0.##");

    private final Color[] availableColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
            Color.ORANGE, Color.CYAN, new Color(128, 0, 128), // Lila
            new Color(165, 42, 42) // Braun
    };

    private final String[] colorNames = {
            "Rot", "Blau", "Grün", "Magenta",
            "Orange", "Cyan", "Lila", "Braun"
    };

    public PlotterPanel() {
        setLayout(new BorderLayout(5, 5));

        // Graph-Panel erstellen
        graphPanel = new GraphPanel();

        // Listener für dynamische Aktualisierung der Schnittpunktliste
        graphPanel.addPropertyChangeListener("intersectionsUpdated", evt -> {
            if (showIntersectionsCheckbox.isSelected()) {
                updateIntersectionList();
            }
        });

        // Listener für Aktualisierung der Zentrierung-Textfelder bei Änderung der
        // Ansicht
        graphPanel.addPropertyChangeListener("viewChanged", evt -> {
            updateCenteringFields();
        });

        // Steuerungsbereich erstellen
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // Funktionseingabefeld Panel
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        functionField = new JTextField();
        functionField.setToolTipText("Funktion eingeben, z.B. sin(x) oder x^2");

        JLabel functionLabel = new JLabel("f(x) = ");
        functionPanel.add(functionLabel, BorderLayout.WEST);
        functionPanel.add(functionField, BorderLayout.CENTER);

        // Farb-Combobox
        colorComboBox = new JComboBox<>(colorNames);
        colorComboBox.setPreferredSize(new Dimension(100, functionField.getPreferredSize().height));
        functionPanel.add(colorComboBox, BorderLayout.EAST);

        // Panel für die Button-Zeile
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton addButton = new JButton("Hinzufügen");
        JButton removeButton = new JButton("Entfernen");
        JButton clearButton = new JButton("Alle löschen");
        JButton resetViewButton = new JButton("Ansicht zurücksetzen");

        actionButtonPanel.add(addButton);
        actionButtonPanel.add(removeButton);
        actionButtonPanel.add(clearButton);

        // Panel für die Funktionsliste
        JPanel functionsPanel = new JPanel(new BorderLayout(5, 5));
        functionsPanel.setBorder(BorderFactory.createTitledBorder("Funktionen"));

        // Funktionsliste mit scrollbarem Bereich
        functionListModel = new DefaultListModel<>();
        functionList = new JList<>(functionListModel);
        JScrollPane listScrollPane = new JScrollPane(functionList);
        listScrollPane.setPreferredSize(new Dimension(300, 100));
        listScrollPane.setMinimumSize(new Dimension(100, 50));
        functionsPanel.add(listScrollPane, BorderLayout.CENTER);

        // Panel für die Zentrierung
        JPanel centeringPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        centeringPanel.setBorder(BorderFactory.createTitledBorder("Ansicht zentrieren"));

        xCenterField = new JTextField(5);
        yCenterField = new JTextField(5);

        centeringPanel.add(new JLabel("X:"));
        centeringPanel.add(xCenterField);
        centeringPanel.add(new JLabel("Y:"));
        centeringPanel.add(yCenterField);

        JButton centerButton = new JButton("Zentrieren");
        centerButton.addActionListener(e -> centerGraphView());
        centeringPanel.add(centerButton);
        centeringPanel.add(resetViewButton);

        // Checkbox für Schnittpunkte
        showIntersectionsCheckbox = new JCheckBox("Schnittpunkte anzeigen");
        showIntersectionsCheckbox.setSelected(false);
        showIntersectionsCheckbox.addActionListener(e -> {
            boolean selected = showIntersectionsCheckbox.isSelected();
            graphPanel.toggleIntersections(selected);
            intersectionPanel.setVisible(selected);

            if (selected) {
                // Aktualisiere die Schnittpunktliste
                updateIntersectionList();
            } else {
                // Lösche die Schnittpunktliste
                intersectionListModel.clear();
            }
        });

        // Panel für zusätzliche Optionen
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        optionsPanel.add(showIntersectionsCheckbox);

        // Schnittpunktliste erstellen
        intersectionListModel = new DefaultListModel<>();
        intersectionList = new JList<>(intersectionListModel);
        intersectionList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane intersectionScrollPane = new JScrollPane(intersectionList);
        intersectionScrollPane.setPreferredSize(new Dimension(300, 100));
        intersectionScrollPane.setMinimumSize(new Dimension(100, 50));

        // Panel für Schnittpunktliste
        intersectionPanel = new JPanel(new BorderLayout(5, 5));
        intersectionPanel.add(new JLabel("Gefundene Schnittpunkte:"), BorderLayout.NORTH);
        intersectionPanel.add(intersectionScrollPane, BorderLayout.CENTER);
        intersectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        intersectionPanel.setVisible(false); // Standardmäßig ausgeblendet

        // Event-Handling
        addButton.addActionListener(e -> addFunction());

        removeButton.addActionListener(e -> {
            int selectedIndex = functionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                functionListModel.remove(selectedIndex);
                updateGraphFromList();

                // Aktualisiere auch die Schnittpunktliste, wenn sie sichtbar ist
                if (showIntersectionsCheckbox.isSelected()) {
                    updateIntersectionList();
                }
            }
        });

        clearButton.addActionListener(e -> {
            functionListModel.clear();
            graphPanel.clearFunctions();

            // Aktualisiere auch die Schnittpunktliste, wenn sie sichtbar ist
            if (showIntersectionsCheckbox.isSelected()) {
                updateIntersectionList();
            }
        });

        resetViewButton.addActionListener(e -> {
            graphPanel.resetView();
            // Die Zentrierung-Felder werden durch den PropertyChangeListener aktualisiert
        });

        functionField.addActionListener(e -> addFunction());

        // Beispiele als Hilfe
        JPanel examplesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        examplesPanel.setBorder(BorderFactory.createTitledBorder("Beispiele"));

        String[] examples = {
                "x^2", "sin(x)", "cos(x)", "tan(x)", "x^3-3*x", "sqrt(x)",
                "log(x)", "ln(x)", "abs(x)", "exp(x)", "sin(x)+cos(x)"
        };

        for (String example : examples) {
            JButton exampleButton = new JButton(example);
            exampleButton.setFont(new Font("Arial", Font.PLAIN, 11));
            exampleButton.addActionListener(e -> functionField.setText(example));
            examplesPanel.add(exampleButton);
        }

        // Erstelle eine Box für das Anordnen der Kontrollpanels
        Box controlBox = Box.createVerticalBox();
        controlBox.add(functionPanel);
        controlBox.add(actionButtonPanel);
        controlBox.add(functionsPanel);
        controlBox.add(Box.createVerticalStrut(5));
        controlBox.add(centeringPanel);
        controlBox.add(Box.createVerticalStrut(5));
        controlBox.add(optionsPanel);

        // Füge Kontrollbox zum Kontrollpanel hinzu
        controlPanel.add(controlBox, BorderLayout.NORTH);
        controlPanel.add(examplesPanel, BorderLayout.SOUTH);

        // Konfiguriere ein Split-Pane für Kontrollelemente und Schnittpunkte
        JSplitPane controlSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                controlPanel,
                intersectionPanel);
        controlSplitPane.setResizeWeight(0.7);
        controlSplitPane.setContinuousLayout(true);
        controlSplitPane.setOneTouchExpandable(true);

        // Hauptfenster mit anpassbarer Größe für Control-Panel
        JSplitPane mainSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                graphPanel,
                controlSplitPane);
        mainSplitPane.setResizeWeight(1.0); // Das Graphpanel erhält den meisten Platz bei Größenänderung
        mainSplitPane.setOneTouchExpandable(true); // Ein-Klick-Verschieben aktivieren
        mainSplitPane.setContinuousLayout(true);

        // Bei Start eine angemessene Position einstellen (70% für Graph, 30% für
        // Kontrollelemente)
        mainSplitPane.setDividerLocation(0.7);

        // Haupt-Layout
        add(mainSplitPane, BorderLayout.CENTER);

        // Initialisiere die Zentrierung-Felder mit der aktuellen Ansicht
        updateCenteringFields();
    }

    /**
     * Aktualisiert die Zentrierung-Felder mit der aktuellen Ansichtsmitte
     */
    private void updateCenteringFields() {
        Point2D.Double center = graphPanel.getViewCenter();
        xCenterField.setText(coordinateFormat.format(center.x));
        yCenterField.setText(coordinateFormat.format(center.y));
    }

    private void addFunction() {
        String func = functionField.getText().trim();
        if (!func.isEmpty()) {
            try {
                // Farbe auswählen
                int colorIndex = colorComboBox.getSelectedIndex();
                Color color = availableColors[colorIndex % availableColors.length];

                // Zur Funktionsliste hinzufügen
                String listEntry = "f(x) = " + func + " [" + colorNames[colorIndex] + "]";
                functionListModel.addElement(listEntry);

                // Graph aktualisieren
                updateGraphFromList();

                // Eingabefeld leeren
                functionField.setText("");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler in der Funktion: " + ex.getMessage(),
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateGraphFromList() {
        graphPanel.clearFunctions();

        for (int i = 0; i < functionListModel.size(); i++) {
            String entry = functionListModel.get(i);

            // Extrahiere die eigentliche Funktion aus dem Listeneintrag
            String funcPart = entry.substring(entry.indexOf('=') + 1, entry.lastIndexOf('[')).trim();

            // Extrahiere die Farbe
            String colorPart = entry.substring(entry.lastIndexOf('[') + 1, entry.lastIndexOf(']')).trim();
            int colorIndex = -1;

            for (int j = 0; j < colorNames.length; j++) {
                if (colorNames[j].equals(colorPart)) {
                    colorIndex = j;
                    break;
                }
            }

            Color color = (colorIndex >= 0)
                    ? availableColors[colorIndex]
                    : availableColors[i % availableColors.length];

            // Funktion hinzufügen
            graphPanel.addFunction(funcPart, color);
        }

        // Aktualisiere Schnittpunkte, falls aktiviert
        if (showIntersectionsCheckbox.isSelected()) {
            graphPanel.toggleIntersections(true);
            updateIntersectionList();
        }
    }

    /**
     * Aktualisiert die Schnittpunktliste mit den aktuellen Schnittpunkten
     */
    private void updateIntersectionList() {
        // Liste leeren
        intersectionListModel.clear();

        // Schnittpunkte vom GraphPanel holen
        List<IntersectionPoint> intersectionPoints = graphPanel.getIntersectionPoints();

        if (intersectionPoints.isEmpty()) {
            intersectionListModel.addElement("Keine Schnittpunkte gefunden");
            return;
        }

        // Formatter für schöne Zahlenformatierung
        DecimalFormat df = new DecimalFormat("0.####");

        // Jeden Schnittpunkt zur Liste hinzufügen
        for (int i = 0; i < intersectionPoints.size(); i++) {
            IntersectionPoint point = intersectionPoints.get(i);

            // Extrahiere die Funktionsausdrücke aus der Funktionsliste für eine bessere
            // Anzeige
            String func1 = getFunctionExpressionByIndex(point.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(point.getFunctionIndex2());

            // Erstelle einen informativen Listeneintrag
            String entry = "S" + (i + 1) + ": (" + df.format(point.x) + ", " + df.format(point.y) + ") ";
            entry += "zwischen " + func1 + " und " + func2;

            intersectionListModel.addElement(entry);
        }
    }

    /**
     * Hilfsmethode, um den Funktionsausdruck anhand des Index zu ermitteln
     */
    private String getFunctionExpressionByIndex(int index) {
        if (index < 0 || index >= functionListModel.size()) {
            return "f" + (index + 1);
        }

        String entry = functionListModel.get(index);

        // Extrahiere den Funktionsausdruck (zwischen "=" und "[")
        int equalsPos = entry.indexOf('=');
        int bracketPos = entry.lastIndexOf('[');

        if (equalsPos >= 0 && bracketPos > equalsPos) {
            return entry.substring(equalsPos + 1, bracketPos).trim();
        } else {
            return "f" + (index + 1);
        }
    }

    /**
     * Parst einen Dezimalwert aus einer Zeichenkette, unterstützt sowohl Punkt als
     * auch Komma
     * als Dezimaltrennzeichen
     */
    private double parseDecimal(String text) throws NumberFormatException, ParseException {
        // Erste Methode: Direktes Parsen (für Punkt als Dezimaltrennzeichen)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Zweite Methode: Ersetze Komma durch Punkt und versuche erneut
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Dritte Methode: Verwende die aktuelle Locale
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }

    /**
     * Zentriert die Ansicht auf die eingegebenen X- und Y-Koordinaten
     */
    private void centerGraphView() {
        try {
            // Extrahiere die eingegebenen Werte
            String xText = xCenterField.getText().trim();
            String yText = yCenterField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte geben Sie gültige X- und Y-Werte ein.",
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Versuche die Werte zu parsen (unterstützt sowohl Punkt als auch Komma)
            double xCenter = parseDecimal(xText);
            double yCenter = parseDecimal(yText);

            // Rufe die Methode zum Zentrieren im GraphPanel auf
            graphPanel.centerViewAt(xCenter, yCenter);

            // Aktualisiere auch die Schnittpunktliste, wenn Schnittpunkte angezeigt werden
            if (showIntersectionsCheckbox.isSelected()) {
                updateIntersectionList();
            }

        } catch (NumberFormatException | ParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Ungültige Zahlenformate. Bitte geben Sie gültige Zahlen ein." +
                            "\n(Hinweis: Sowohl Punkt als auch Komma als Dezimaltrennzeichen werden akzeptiert)",
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}