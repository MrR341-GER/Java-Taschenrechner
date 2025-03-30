
package plugins.plotter3d.view;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.ColorChooser;
import plugins.plotter3d.Plot3DPanel;
import plugins.plotter3d.ui.Example3DPanel;
import plugins.plotter3d.ui.Plot3DFunctionManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Klasse für den Aufbau der Benutzeroberfläche des 3D-Plotters
 * Erstellt alle UI-Komponenten und Bedienelemente
 */
public class Plot3DUIBuilder {
    private final Plot3DPanel mainPanel;
    private final Plot3DFunctionManager functionManager;
    private final Plot3DViewController viewController;
    private Example3DPanel examplePanel; // Neu: Komponente für 3D-Beispiele

    // UI-Komponenten
    private JTextField functionField;
    private JComboBox<String> colorComboBox;
    private JTextField xMinField, xMaxField, yMinField, yMaxField;
    private JSlider resolutionSlider;
    private JLabel resolutionLabel;
    private JSlider rotationXSlider, rotationYSlider, rotationZSlider;
    private JLabel rotationXLabel, rotationYLabel, rotationZLabel;
    private JButton resetViewButton;
    private JCheckBox showCoordinateSystemCheckbox;
    private JCheckBox showGridCheckbox;
    private JCheckBox showHelperLinesCheckbox;
    private JCheckBox useHeatmapCheckbox;
    private JCheckBox useSolidSurfaceCheckbox;

    /**
     * Konstruktor für den UI-Builder
     * 
     * @param mainPanel       Das Hauptpanel des 3D-Plotters
     * @param functionManager Der Funktionsmanager
     * @param viewController  Der View-Controller
     */
    public Plot3DUIBuilder(Plot3DPanel mainPanel, Plot3DFunctionManager functionManager,
            Plot3DViewController viewController) {
        this.mainPanel = mainPanel;
        this.functionManager = functionManager;
        this.viewController = viewController;
    }

    /**
     * Erstellt das Kontrollpanel mit allen Steuerungselementen
     */
    public JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 1. Funktionseingabe
        JPanel functionPanel = createFunctionInputPanel();

        // Initialisiere den FunctionField für das Beispiel-Panel
        if (functionField != null) {
            examplePanel = new Example3DPanel(mainPanel, functionField);
        }

        // 2. Zweispaltige Ansicht: Links Beispiele, rechts Funktionsliste
        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        // Beispiel-Panel erstellen
        JPanel examplesPanel = null;
        if (examplePanel != null) {
            examplesPanel = examplePanel.createExamplesPanel();
        }

        // Funktionslisten-Panel erstellen
        JPanel functionsPanel = createFunctionListPanel();

        // Beide Panels einfügen
        if (examplesPanel != null) {
            splitPanel.add(examplesPanel);
        }
        splitPanel.add(functionsPanel);

        // 3. Bereichseinstellungen
        JPanel rangePanel = createRangePanel();

        // 4. Auflösungseinstellung
        JPanel resolutionPanel = createResolutionPanel();

        // 5. Anzeigeoptionen
        JPanel displayOptionsPanel = createDisplayOptionsPanel();

        // 6. Rotationssteuerung
        JPanel rotationPanel = createRotationPanel();

        // 7. Ansicht zurücksetzen Button
        resetViewButton = new JButton("Ansicht zurücksetzen");
        resetViewButton.addActionListener(e -> {
            viewController.resetView();
            // Explizites Neuzeichnen wird nun in resetView() ausgeführt
        });

        // Steuerungshinweis
        JPanel instructionPanel = createInstructionPanel();

        // Alles zusammenfügen
        panel.add(functionPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(splitPanel); // Neu: Funktionsliste und Beispiele nebeneinander
        panel.add(Box.createVerticalStrut(10));
        panel.add(rangePanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(resolutionPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(displayOptionsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(rotationPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(instructionPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(resetViewButton);

        // Verbleibenden Platz füllen
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Erstellt das Panel für die Funktionseingabe
     */
    private JPanel createFunctionInputPanel() {
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.setBorder(BorderFactory.createTitledBorder("Funktion (z = f(x,y))"));

        functionField = new JTextField(Plot3DViewController.DEFAULT_FUNCTION, 20);
        functionField.setToolTipText("Geben Sie eine Funktion mit Variablen x und y ein, z.B. x^2+y^2");

        // Farbauswahl hinzufügen
        colorComboBox = new JComboBox<>(ColorChooser.getColorNames());
        colorComboBox.setPreferredSize(new Dimension(100, functionField.getPreferredSize().height));
        colorComboBox.setSelectedItem(ColorChooser.RANDOM_COLOR_OPTION);

        // Behandlung der "Weitere..."-Option
        colorComboBox.addActionListener(e -> handleCustomColorSelection());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(functionField, BorderLayout.CENTER);
        inputPanel.add(colorComboBox, BorderLayout.EAST);

        functionPanel.add(inputPanel, BorderLayout.CENTER);

        JButton renderButton = new JButton("Hinzufügen");
        renderButton.addActionListener(e -> functionManager.addFunction(functionField.getText(),
                (String) colorComboBox.getSelectedItem()));

        functionPanel.add(renderButton, BorderLayout.EAST);

        return functionPanel;
    }

    /**
     * Behandelt die Auswahl einer benutzerdefinierten Farbe
     */
    private void handleCustomColorSelection() {
        if (colorComboBox.getSelectedItem() != null &&
                colorComboBox.getSelectedItem().toString().equals("Weitere...")) {

            mainPanel.debug("Benutzerdefinierte Farbauswahl geöffnet");
            // Zeige den Farbauswahl-Dialog
            Color selectedColor = ColorChooser.showColorChooser(
                    mainPanel,
                    "Benutzerdefinierte Farbe wählen",
                    Color.RED);

            if (selectedColor != null) {
                // Farbname ermitteln oder neu erstellen
                String colorName = ColorChooser.getColorName(selectedColor);
                mainPanel.debug("Benutzerdefinierte Farbe gewählt: " + colorName);

                // Überprüfen, ob die Farbe bereits in der Liste ist
                boolean exists = false;
                for (int i = 0; i < colorComboBox.getItemCount() - 1; i++) {
                    if (colorComboBox.getItemAt(i).equals(colorName)) {
                        colorComboBox.setSelectedIndex(i);
                        exists = true;
                        break;
                    }
                }

                // Wenn nicht, hinzufügen
                if (!exists) {
                    colorComboBox.insertItemAt(colorName, colorComboBox.getItemCount() - 1);
                    colorComboBox.setSelectedIndex(colorComboBox.getItemCount() - 2);
                    mainPanel.debug("Neue Farbe zur Auswahlliste hinzugefügt: " + colorName);
                }
            } else {
                // Falls abgebrochen, zurück zur "Zufällig"-Option
                colorComboBox.setSelectedItem(ColorChooser.RANDOM_COLOR_OPTION);
                mainPanel.debug("Farbauswahl abgebrochen, zurück zu 'Zufällig'");
            }
        }
    }

    /**
     * Erstellt das Panel für die Funktionsliste
     */
    private JPanel createFunctionListPanel() {
        JPanel functionsPanel = new JPanel(new BorderLayout(5, 5));
        functionsPanel.setBorder(BorderFactory.createTitledBorder("Funktionen"));

        // Scroll-Bereich für die Liste
        JScrollPane scrollPane = new JScrollPane(functionManager.getFunctionList());
        scrollPane.setPreferredSize(new Dimension(100, 100));
        functionsPanel.add(scrollPane, BorderLayout.CENTER);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        JButton removeButton = new JButton("Entfernen");
        removeButton.addActionListener(e -> functionManager.removeSelectedFunction());

        JButton clearButton = new JButton("Alle löschen");
        clearButton.addActionListener(e -> functionManager.clearAllFunctions());

        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        functionsPanel.add(buttonPanel, BorderLayout.SOUTH);

        return functionsPanel;
    }

    /**
     * Erstellt das Panel für die Bereichseinstellungen
     */
    private JPanel createRangePanel() {
        JPanel rangePanel = new JPanel(new GridBagLayout());
        rangePanel.setBorder(BorderFactory.createTitledBorder("Wertebereich"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        // X-Bereich
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        rangePanel.add(new JLabel("X-Min:"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        xMinField = new JTextField(String.valueOf(Plot3DViewController.DEFAULT_MIN), 5);
        rangePanel.add(xMinField, c);

        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        rangePanel.add(new JLabel("X-Max:"), c);

        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        xMaxField = new JTextField(String.valueOf(Plot3DViewController.DEFAULT_MAX), 5);
        rangePanel.add(xMaxField, c);

        // Y-Bereich
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        rangePanel.add(new JLabel("Y-Min:"), c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        yMinField = new JTextField(String.valueOf(Plot3DViewController.DEFAULT_MIN), 5);
        rangePanel.add(yMinField, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        rangePanel.add(new JLabel("Y-Max:"), c);

        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        yMaxField = new JTextField(String.valueOf(Plot3DViewController.DEFAULT_MAX), 5);
        rangePanel.add(yMaxField, c);

        // Apply-Button für Wertebereich
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.weightx = 1.0;
        JButton applyRangeButton = new JButton("Bereich anwenden");
        applyRangeButton.addActionListener(e -> {
            viewController.setXMinField(xMinField);
            viewController.setXMaxField(xMaxField);
            viewController.setYMinField(yMinField);
            viewController.setYMaxField(yMaxField);
            viewController.updateViewBounds();
            // Explizites Neuzeichnen wird nun in updateViewBounds() ausgeführt
        });
        rangePanel.add(applyRangeButton, c);

        return rangePanel;
    }

    /**
     * Erstellt das Panel für die Auflösungseinstellung
     */
    private JPanel createResolutionPanel() {
        JPanel resolutionPanel = new JPanel(new BorderLayout(5, 5));
        resolutionPanel.setBorder(BorderFactory.createTitledBorder("Auflösung"));

        resolutionSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, Plot3DViewController.DEFAULT_RESOLUTION);
        resolutionSlider.setMajorTickSpacing(30);
        resolutionSlider.setMinorTickSpacing(10);
        resolutionSlider.setPaintTicks(true);
        resolutionSlider.setPaintLabels(true);

        resolutionLabel = new JLabel("Auflösung: " + Plot3DViewController.DEFAULT_RESOLUTION);
        resolutionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        resolutionSlider.addChangeListener(e -> {
            int value = resolutionSlider.getValue();
            resolutionLabel.setText("Auflösung: " + value);

            // Nur neu rendern, wenn nicht mehr am Schieben
            if (!resolutionSlider.getValueIsAdjusting()) {
                viewController.setResolution(value);
                // Explizites Neuzeichnen wird nun in setResolution() ausgeführt
            }
        });

        resolutionPanel.add(resolutionSlider, BorderLayout.CENTER);
        resolutionPanel.add(resolutionLabel, BorderLayout.SOUTH);

        return resolutionPanel;
    }

    /**
     * Erstellt das Panel für die Anzeigeoptionen
     */
    private JPanel createDisplayOptionsPanel() {
        // Anzahl der Zeilen erhöhen (von 4 auf 5 für die neue Schnittlinien-Option)
        JPanel displayOptionsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        displayOptionsPanel.setBorder(BorderFactory.createTitledBorder("Anzeigeoptionen"));

        showCoordinateSystemCheckbox = new JCheckBox("Koordinatensystem anzeigen", true);
        showCoordinateSystemCheckbox.addActionListener(e -> {
            viewController.setShowCoordinateSystem(showCoordinateSystemCheckbox.isSelected());
        });

        showGridCheckbox = new JCheckBox("Gitter anzeigen", true);
        showGridCheckbox.addActionListener(e -> {
            viewController.setShowGrid(showGridCheckbox.isSelected());
        });

        showHelperLinesCheckbox = new JCheckBox("Hilfslinien anzeigen", true);
        showHelperLinesCheckbox.addActionListener(e -> {
            viewController.setShowHelperLines(showHelperLinesCheckbox.isSelected());
            mainPanel.debug("Hilfslinien " + (showHelperLinesCheckbox.isSelected() ? "aktiviert" : "deaktiviert"));
        });

        // Checkbox für Heatmap-Modus
        useHeatmapCheckbox = new JCheckBox("Heatmap-Farben verwenden", true);
        useHeatmapCheckbox.addActionListener(e -> {
            viewController.setUseHeatmap(useHeatmapCheckbox.isSelected());
            mainPanel.debug("Heatmap-Farben " + (useHeatmapCheckbox.isSelected() ? "aktiviert" : "deaktiviert"));
        });

        // Checkbox für undurchsichtige Darstellung
        useSolidSurfaceCheckbox = new JCheckBox("Undurchsichtige Oberflächen mit Schattierung", false);
        useSolidSurfaceCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isSelected = useSolidSurfaceCheckbox.isSelected();

                // Direkt den Renderer aktualisieren
                if (mainPanel != null && mainPanel.getRenderer() != null) {
                    mainPanel.getRenderer().setUseSolidSurface(isSelected);
                    mainPanel.debug("Undurchsichtige Darstellung direkt auf Renderer gesetzt: " +
                            (isSelected ? "aktiviert" : "deaktiviert"));
                }

                // Auch über ViewController setzen
                viewController.setUseSolidSurface(isSelected);
                mainPanel.debug("Undurchsichtige Darstellung über ViewController gesetzt: " +
                        (isSelected ? "aktiviert" : "deaktiviert"));

                // Explizit neu zeichnen
                mainPanel.renderPlot();
            }
        });

        // NEUE Checkbox für Schnittlinien
        JCheckBox showIntersectionsCheckbox = new JCheckBox("Schnittlinien zwischen Funktionen anzeigen", true);
        showIntersectionsCheckbox.addActionListener(e -> {
            boolean isSelected = showIntersectionsCheckbox.isSelected();

            // Direkt den Renderer aktualisieren
            if (mainPanel != null && mainPanel.getRenderer() != null) {
                mainPanel.getRenderer().setShowIntersections(isSelected);
                mainPanel.debug("Schnittlinien direkt auf Renderer gesetzt: " +
                        (isSelected ? "aktiviert" : "deaktiviert"));
            }

            // Auch über ViewController setzen
            viewController.setShowIntersections(isSelected);
            mainPanel.debug("Schnittlinien über ViewController gesetzt: " +
                    (isSelected ? "aktiviert" : "deaktiviert"));

            // Explizit neu zeichnen
            mainPanel.renderPlot();
        });

        displayOptionsPanel.add(showCoordinateSystemCheckbox);
        displayOptionsPanel.add(showGridCheckbox);
        displayOptionsPanel.add(showHelperLinesCheckbox);
        displayOptionsPanel.add(useHeatmapCheckbox);
        displayOptionsPanel.add(useSolidSurfaceCheckbox);
        displayOptionsPanel.add(showIntersectionsCheckbox);

        return displayOptionsPanel;
    }

    /**
     * Erstellt das Panel für die Rotationssteuerung
     */
    private JPanel createRotationPanel() {
        JPanel rotationPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        rotationPanel.setBorder(BorderFactory.createTitledBorder("Rotation"));

        // X-Rotation
        JPanel rotXPanel = new JPanel(new BorderLayout(5, 5));
        rotationXSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) viewController.getCurrentRotationX());
        rotationXSlider.setMajorTickSpacing(90);
        rotationXSlider.setPaintTicks(true);
        rotationXLabel = new JLabel("X: " + (int) viewController.getCurrentRotationX() + "°");
        rotationXLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationXSlider.addChangeListener(e -> {
            viewController.setCurrentRotationX(rotationXSlider.getValue());
            rotationXLabel.setText("X: " + (int) viewController.getCurrentRotationX() + "°");

            if (!rotationXSlider.getValueIsAdjusting()) {
                viewController.updateRotation();
                mainPanel.renderPlot();
            }
        });

        rotXPanel.add(new JLabel("X:"), BorderLayout.WEST);
        rotXPanel.add(rotationXSlider, BorderLayout.CENTER);
        rotXPanel.add(rotationXLabel, BorderLayout.EAST);

        // Y-Rotation
        JPanel rotYPanel = new JPanel(new BorderLayout(5, 5));
        rotationYSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) viewController.getCurrentRotationY());
        rotationYSlider.setMajorTickSpacing(90);
        rotationYSlider.setPaintTicks(true);
        rotationYLabel = new JLabel("Y: " + (int) viewController.getCurrentRotationY() + "°");
        rotationYLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationYSlider.addChangeListener(e -> {
            viewController.setCurrentRotationY(rotationYSlider.getValue());
            rotationYLabel.setText("Y: " + (int) viewController.getCurrentRotationY() + "°");

            if (!rotationYSlider.getValueIsAdjusting()) {
                viewController.updateRotation();
                mainPanel.renderPlot();
            }
        });

        rotYPanel.add(new JLabel("Y:"), BorderLayout.WEST);
        rotYPanel.add(rotationYSlider, BorderLayout.CENTER);
        rotYPanel.add(rotationYLabel, BorderLayout.EAST);

        // Z-Rotation
        JPanel rotZPanel = new JPanel(new BorderLayout(5, 5));
        rotationZSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) viewController.getCurrentRotationZ());
        rotationZSlider.setMajorTickSpacing(90);
        rotationZSlider.setPaintTicks(true);
        rotationZLabel = new JLabel("Z: " + (int) viewController.getCurrentRotationZ() + "°");
        rotationZLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationZSlider.addChangeListener(e -> {
            viewController.setCurrentRotationZ(rotationZSlider.getValue());
            rotationZLabel.setText("Z: " + (int) viewController.getCurrentRotationZ() + "°");

            if (!rotationZSlider.getValueIsAdjusting()) {
                viewController.updateRotation();
                mainPanel.renderPlot();
            }
        });

        rotZPanel.add(new JLabel("Z:"), BorderLayout.WEST);
        rotZPanel.add(rotationZSlider, BorderLayout.CENTER);
        rotZPanel.add(rotationZLabel, BorderLayout.EAST);

        rotationPanel.add(rotXPanel);
        rotationPanel.add(rotYPanel);
        rotationPanel.add(rotZPanel);

        return rotationPanel;
    }

    /**
     * Erstellt das Panel mit Steuerungshinweisen
     */
    private JPanel createInstructionPanel() {
        JPanel instructionPanel = new JPanel(new BorderLayout());
        instructionPanel.setBorder(BorderFactory.createTitledBorder("Steuerung"));
        JTextArea instructionText = new JTextArea(
                "Rotation: Mausziehen\n" +
                        "Verschieben: Shift + Mausziehen\n" +
                        "Zoom: Mausrad");
        instructionText.setEditable(false);
        instructionText.setBackground(instructionPanel.getBackground());
        instructionPanel.add(instructionText, BorderLayout.CENTER);

        return instructionPanel;
    }

    // Getter-Methoden für Komponenten

    /**
     * Gibt das Funktionsfeld zurück
     */
    public JTextField getFunctionField() {
        return functionField;
    }

    /**
     * Gibt das Feld für X-Minimum zurück
     */
    public JTextField getXMinField() {
        return xMinField;
    }

    /**
     * Gibt das Feld für X-Maximum zurück
     */
    public JTextField getXMaxField() {
        return xMaxField;
    }

    /**
     * Gibt das Feld für Y-Minimum zurück
     */
    public JTextField getYMinField() {
        return yMinField;
    }

    /**
     * Gibt das Feld für Y-Maximum zurück
     */
    public JTextField getYMaxField() {
        return yMaxField;
    }
}