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

        // Mindesthöhe für das gesamte Split-Panel festlegen
        splitPanel.setMinimumSize(new Dimension(0, 180));
        splitPanel.setPreferredSize(new Dimension(0, 180));

        // Beispiel-Panel erstellen
        JPanel examplesPanel = null;
        if (examplePanel != null) {
            examplesPanel = examplePanel.createExamplesPanel();

            // Falls möglich, setze explizit eine Mindesthöhe für das Beispielpanel
            if (examplesPanel != null) {
                examplesPanel.setMinimumSize(new Dimension(100, 150));
                examplesPanel.setPreferredSize(new Dimension(100, 150));
            }
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

        // Damit jede Komponente ihre bevorzugte Größe hat
        functionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        splitPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rangePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resolutionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        displayOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rotationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        instructionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetViewButton.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        functionField = new JTextField("", 20);
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
        scrollPane.setPreferredSize(new Dimension(100, 150));
        scrollPane.setMinimumSize(new Dimension(100, 150));
        functionsPanel.add(scrollPane, BorderLayout.CENTER);

        // Button-Panel - von GridLayout(1, 2, 5, 5) auf GridLayout(1, 3, 5, 5) ändern,
        // um Platz für den Kombinieren-Button zu schaffen
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));

        JButton removeButton = new JButton("Entfernen");
        removeButton.addActionListener(e -> functionManager.removeSelectedFunction());

        JButton combineButton = new JButton("Kombinieren");
        combineButton.addActionListener(e -> functionManager.combineSelectedFunctions());

        JButton clearButton = new JButton("Alle löschen");
        clearButton.addActionListener(e -> functionManager.clearAllFunctions());

        buttonPanel.add(removeButton);
        buttonPanel.add(combineButton);
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

        // Hauptpanel mit Slider und Eingabefeld
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        // Panel für das Eingabefeld mit Erklärung
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("Auflösung einstellen (10-500)")));

        JPanel fieldPanel = new JPanel(new BorderLayout(5, 5));
        JLabel inputLabel = new JLabel("Wert: ");

        // Hole die aktuelle Auflösung vom Controller
        int currentResolution = viewController.getCurrentResolution();

        // Textfeld für manuelle Eingabe der Auflösung mit aktuellem Wert
        JTextField resolutionField = new JTextField(String.valueOf(currentResolution), 4);
        resolutionField.setToolTipText("Geben Sie hier Werte von 10 bis 500 ein (max. empfohlen: 300)");

        fieldPanel.add(inputLabel, BorderLayout.WEST);
        fieldPanel.add(resolutionField, BorderLayout.CENTER);

        // Button zum Anwenden der eingegebenen Auflösung
        JButton applyButton = new JButton("Anwenden");
        fieldPanel.add(applyButton, BorderLayout.EAST);

        // Erklärung für den Benutzer
        JLabel infoLabel = new JLabel(
                "<html><small>Für höhere Details geben Sie größere Werte ein (z.B. 150, 200, 300).</small></html>");
        infoLabel.setForeground(Color.DARK_GRAY);

        inputPanel.add(fieldPanel, BorderLayout.NORTH);
        inputPanel.add(infoLabel, BorderLayout.CENTER);

        // Slider-Panel mit Titel
        JPanel sliderPanel = new JPanel(new BorderLayout(5, 5));
        sliderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 0, 0, 0),
                BorderFactory.createTitledBorder("Schnelleinstellung")));

        // Slider für Auflösung - auf aktuellen Wert setzen, aber maximal 100
        int sliderValue = Math.min(currentResolution, 100);
        resolutionSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, sliderValue);
        resolutionSlider.setMajorTickSpacing(30);
        resolutionSlider.setMinorTickSpacing(10);
        resolutionSlider.setPaintTicks(true);
        resolutionSlider.setPaintLabels(true);

        // Label für den Slider
        resolutionLabel = new JLabel("Größere Werte erhöhen Details, benötigen aber mehr Rechenleistung");
        resolutionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resolutionLabel.setFont(resolutionLabel.getFont().deriveFont(Font.PLAIN, 11f));

        // Listener für den Slider - AKTUALISIERT TEXTFELD UND SETZT BEI ENDE DES
        // SCHIEBENS DIE AUFLÖSUNG
        resolutionSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = resolutionSlider.getValue();

                // Synchronisiere das Textfeld mit dem Slider
                resolutionField.setText(String.valueOf(value));

                // Nur wenn der Benutzer das Schieben beendet hat, die Auflösung aktualisieren
                if (!resolutionSlider.getValueIsAdjusting()) {
                    // Auflösung direkt über das Textfeld anwenden
                    applyResolution(resolutionField);
                    // Cast zu Plot3DPanel nötig, da debug-Methode in dieser Klasse definiert ist
                    ((plugins.plotter3d.Plot3DPanel) mainPanel)
                            .debug("Auflösung über Slider auf " + value + " gesetzt");
                }
            }
        });

        // Listener für das Textfeld und den Button
        applyButton.addActionListener(e -> applyResolution(resolutionField));

        // Auch Enter im Textfeld verarbeiten
        resolutionField.addActionListener(e -> applyResolution(resolutionField));

        sliderPanel.add(resolutionSlider, BorderLayout.CENTER);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(sliderPanel, BorderLayout.CENTER);

        resolutionPanel.add(mainPanel, BorderLayout.CENTER);
        resolutionPanel.add(resolutionLabel, BorderLayout.SOUTH);

        return resolutionPanel;
    }

    /**
     * Wendet die eingegebene Auflösung an
     */
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
            // Bei ungültiger Eingabe Standardwert wiederherstellen
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

    /**
     * Gibt den Rotations-X-Schieberegler zurück
     */
    public JSlider getRotationXSlider() {
        return rotationXSlider;
    }

    /**
     * Gibt den Rotations-Y-Schieberegler zurück
     */
    public JSlider getRotationYSlider() {
        return rotationYSlider;
    }

    /**
     * Gibt den Rotations-Z-Schieberegler zurück
     */
    public JSlider getRotationZSlider() {
        return rotationZSlider;
    }

    /**
     * Gibt das Rotations-X-Label zurück
     */
    public JLabel getRotationXLabel() {
        return rotationXLabel;
    }

    /**
     * Gibt das Rotations-Y-Label zurück
     */
    public JLabel getRotationYLabel() {
        return rotationYLabel;
    }

    /**
     * Gibt das Rotations-Z-Label zurück
     */
    public JLabel getRotationZLabel() {
        return rotationZLabel;
    }
}