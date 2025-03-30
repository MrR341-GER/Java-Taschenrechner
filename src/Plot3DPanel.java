import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Verbessertes Panel für 3D-Funktionsplots mit richtigem Koordinatensystem
 * und automatischer Wertebereichsanpassung beim Zoomen
 */
public class Plot3DPanel extends JPanel {
    private final GrafischerTaschenrechner calculator;
    private Plot3DRenderer renderer;
    private JPanel plotPanel;
    private JTextField functionField;
    private JTextField xMinField, xMaxField, yMinField, yMaxField;
    private JSlider resolutionSlider;
    private JLabel resolutionLabel;
    private JSlider rotationXSlider, rotationYSlider, rotationZSlider;
    private JLabel rotationXLabel, rotationYLabel, rotationZLabel;
    private JButton resetViewButton;
    private JButton renderButton;
    private JCheckBox showCoordinateSystemCheckbox;
    private JCheckBox showGridCheckbox;
    private JCheckBox showHelperLinesCheckbox; // Neue Checkbox für Hilfslinien

    // Standardwerte
    private static final double DEFAULT_MIN = -5.0;
    private static final double DEFAULT_MAX = 5.0;
    private static final int DEFAULT_RESOLUTION = 30;
    private static final String DEFAULT_FUNCTION = "sin(sqrt(x^2+y^2))";

    // Interaktionsstatus
    private boolean isDragging = false;
    private boolean isPanning = false; // Flag zum Unterscheiden zwischen Rotation und Panning
    private Point lastMousePos;
    private double currentRotationX = 30;
    private double currentRotationY = 0;
    private double currentRotationZ = 30;
    private double currentScale = 1.0;

    // Aktuelle Wertebereiche
    private double currentXMin = DEFAULT_MIN;
    private double currentXMax = DEFAULT_MAX;
    private double currentYMin = DEFAULT_MIN;
    private double currentYMax = DEFAULT_MAX;

    // Debug-Referenz
    private DebugManager debugManager;

    /**
     * Konstruktor für das 3D-Plot-Panel
     */
    public Plot3DPanel(GrafischerTaschenrechner calculator) {
        this.calculator = calculator;

        // Initialisiere den Renderer mit Standardwerten
        renderer = new Plot3DRenderer(
                DEFAULT_FUNCTION,
                DEFAULT_MIN, DEFAULT_MAX,
                DEFAULT_MIN, DEFAULT_MAX,
                DEFAULT_RESOLUTION);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Speichere die initialen Wertebereiche
        currentXMin = DEFAULT_MIN;
        currentXMax = DEFAULT_MAX;
        currentYMin = DEFAULT_MIN;
        currentYMax = DEFAULT_MAX;

        // UI-Komponenten erstellen
        createUI();
    }

    /**
     * Erstellt die Benutzeroberfläche
     */
    private void createUI() {
        // Hauptbereich: Plot und Steuerungsbereich
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7); // Plot erhält 70% des Platzes

        // Plot-Panel mit Canvas für 3D-Darstellung
        plotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (renderer != null) {
                    renderer.render((Graphics2D) g, getWidth(), getHeight());
                }
            }
        };
        plotPanel.setBackground(Color.WHITE);

        // Mindestgröße für den Plot festlegen
        plotPanel.setMinimumSize(new Dimension(400, 300));

        // Maus-Listener für Interaktion hinzufügen
        setupMouseListeners();

        // Steuerungsbereich mit Eingabefeldern und Buttons
        JPanel controlPanel = createControlPanel();
        controlPanel.setMinimumSize(new Dimension(250, 300));

        // Panes zusammenfügen
        splitPane.setLeftComponent(plotPanel);
        splitPane.setRightComponent(controlPanel);

        add(splitPane, BorderLayout.CENTER);

        // Initialen Plot rendern
        renderPlot();
    }

    /**
     * Erstellt das Kontrollpanel mit allen Einstellungsmöglichkeiten
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 1. Funktionseingabe
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.setBorder(BorderFactory.createTitledBorder("Funktion (z = f(x,y))"));

        functionField = new JTextField(DEFAULT_FUNCTION, 20);
        functionField.setToolTipText("Geben Sie eine Funktion mit Variablen x und y ein, z.B. x^2+y^2");

        renderButton = new JButton("Zeichnen");
        renderButton.addActionListener(e -> renderPlot());

        functionPanel.add(functionField, BorderLayout.CENTER);
        functionPanel.add(renderButton, BorderLayout.EAST);

        // 2. Bereichseinstellungen
        JPanel rangePanel = new JPanel(new GridBagLayout());
        rangePanel.setBorder(BorderFactory.createTitledBorder("Wertebereich"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        // X-Bereich
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        rangePanel.add(new JLabel("X-Min:"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        xMinField = new JTextField(String.valueOf(DEFAULT_MIN), 5);
        rangePanel.add(xMinField, c);

        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        rangePanel.add(new JLabel("X-Max:"), c);

        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        xMaxField = new JTextField(String.valueOf(DEFAULT_MAX), 5);
        rangePanel.add(xMaxField, c);

        // Y-Bereich
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        rangePanel.add(new JLabel("Y-Min:"), c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        yMinField = new JTextField(String.valueOf(DEFAULT_MIN), 5);
        rangePanel.add(yMinField, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        rangePanel.add(new JLabel("Y-Max:"), c);

        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        yMaxField = new JTextField(String.valueOf(DEFAULT_MAX), 5);
        rangePanel.add(yMaxField, c);

        // 3. Auflösungseinstellung
        JPanel resolutionPanel = new JPanel(new BorderLayout(5, 5));
        resolutionPanel.setBorder(BorderFactory.createTitledBorder("Auflösung"));

        resolutionSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, DEFAULT_RESOLUTION);
        resolutionSlider.setMajorTickSpacing(30);
        resolutionSlider.setMinorTickSpacing(10);
        resolutionSlider.setPaintTicks(true);
        resolutionSlider.setPaintLabels(true);

        resolutionLabel = new JLabel("Auflösung: " + DEFAULT_RESOLUTION);
        resolutionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        resolutionSlider.addChangeListener(e -> {
            int value = resolutionSlider.getValue();
            resolutionLabel.setText("Auflösung: " + value);

            // Nur neu rendern, wenn nicht mehr am Schieben
            if (!resolutionSlider.getValueIsAdjusting()) {
                renderPlot();
            }
        });

        resolutionPanel.add(resolutionSlider, BorderLayout.CENTER);
        resolutionPanel.add(resolutionLabel, BorderLayout.SOUTH);

        // 4. Anzeigeoptionen
        JPanel displayOptionsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        displayOptionsPanel.setBorder(BorderFactory.createTitledBorder("Anzeigeoptionen"));

        showCoordinateSystemCheckbox = new JCheckBox("Koordinatensystem anzeigen", true);
        showCoordinateSystemCheckbox.addActionListener(e -> {
            renderer.setShowCoordinateSystem(showCoordinateSystemCheckbox.isSelected());
            plotPanel.repaint();
        });

        showGridCheckbox = new JCheckBox("Gitter anzeigen", true);
        showGridCheckbox.addActionListener(e -> {
            renderer.setShowGrid(showGridCheckbox.isSelected());
            plotPanel.repaint();
        });

        // Neue Checkbox für Hilfslinien
        showHelperLinesCheckbox = new JCheckBox("Hilfslinien anzeigen", true);
        showHelperLinesCheckbox.addActionListener(e -> {
            renderer.setShowHelperLines(showHelperLinesCheckbox.isSelected());
            plotPanel.repaint();
            debug("Hilfslinien " + (showHelperLinesCheckbox.isSelected() ? "aktiviert" : "deaktiviert"));
        });

        displayOptionsPanel.add(showCoordinateSystemCheckbox);
        displayOptionsPanel.add(showGridCheckbox);
        displayOptionsPanel.add(showHelperLinesCheckbox);

        // 5. Rotationssteuerung
        JPanel rotationPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        rotationPanel.setBorder(BorderFactory.createTitledBorder("Rotation"));

        // X-Rotation
        JPanel rotXPanel = new JPanel(new BorderLayout(5, 5));
        rotationXSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) currentRotationX);
        rotationXSlider.setMajorTickSpacing(90);
        rotationXSlider.setPaintTicks(true);
        rotationXLabel = new JLabel("X: " + (int) currentRotationX + "°");
        rotationXLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationXSlider.addChangeListener(e -> {
            currentRotationX = rotationXSlider.getValue();
            rotationXLabel.setText("X: " + (int) currentRotationX + "°");

            if (!rotationXSlider.getValueIsAdjusting()) {
                updateRotation();
            }
        });

        rotXPanel.add(new JLabel("X:"), BorderLayout.WEST);
        rotXPanel.add(rotationXSlider, BorderLayout.CENTER);
        rotXPanel.add(rotationXLabel, BorderLayout.EAST);

        // Y-Rotation
        JPanel rotYPanel = new JPanel(new BorderLayout(5, 5));
        rotationYSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) currentRotationY);
        rotationYSlider.setMajorTickSpacing(90);
        rotationYSlider.setPaintTicks(true);
        rotationYLabel = new JLabel("Y: " + (int) currentRotationY + "°");
        rotationYLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationYSlider.addChangeListener(e -> {
            currentRotationY = rotationYSlider.getValue();
            rotationYLabel.setText("Y: " + (int) currentRotationY + "°");

            if (!rotationYSlider.getValueIsAdjusting()) {
                updateRotation();
            }
        });

        rotYPanel.add(new JLabel("Y:"), BorderLayout.WEST);
        rotYPanel.add(rotationYSlider, BorderLayout.CENTER);
        rotYPanel.add(rotationYLabel, BorderLayout.EAST);

        // Z-Rotation
        JPanel rotZPanel = new JPanel(new BorderLayout(5, 5));
        rotationZSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, (int) currentRotationZ);
        rotationZSlider.setMajorTickSpacing(90);
        rotationZSlider.setPaintTicks(true);
        rotationZLabel = new JLabel("Z: " + (int) currentRotationZ + "°");
        rotationZLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rotationZSlider.addChangeListener(e -> {
            currentRotationZ = rotationZSlider.getValue();
            rotationZLabel.setText("Z: " + (int) currentRotationZ + "°");

            if (!rotationZSlider.getValueIsAdjusting()) {
                updateRotation();
            }
        });

        rotZPanel.add(new JLabel("Z:"), BorderLayout.WEST);
        rotZPanel.add(rotationZSlider, BorderLayout.CENTER);
        rotZPanel.add(rotationZLabel, BorderLayout.EAST);

        rotationPanel.add(rotXPanel);
        rotationPanel.add(rotYPanel);
        rotationPanel.add(rotZPanel);

        // 6. Ansicht zurücksetzen Button
        resetViewButton = new JButton("Ansicht zurücksetzen");
        resetViewButton.addActionListener(e -> resetView());

        // Steuerungshinweis
        JPanel instructionPanel = new JPanel(new BorderLayout());
        instructionPanel.setBorder(BorderFactory.createTitledBorder("Steuerung"));
        JTextArea instructionText = new JTextArea(
                "Rotation: Mausziehen\n" +
                        "Verschieben: Shift + Mausziehen\n" +
                        "Zoom: Mausrad");
        instructionText.setEditable(false);
        instructionText.setBackground(panel.getBackground());
        instructionPanel.add(instructionText, BorderLayout.CENTER);

        // Beispiel-Funktionen
        JPanel examplesPanel = createExamplesPanel();

        // Alles zusammenfügen
        panel.add(functionPanel);
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
        panel.add(Box.createVerticalStrut(10));
        panel.add(examplesPanel);

        // Verbleibenden Platz füllen
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Erstellt ein Panel mit Beispielfunktionen
     */
    private JPanel createExamplesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Beispielfunktionen"));

        // Liste mit Beispielfunktionen
        String[] examples = {
                "sin(sqrt(x^2+y^2))",
                "cos(x)*sin(y)",
                "sin(x*y)/(x*y+0.1)",
                "x^2-y^2",
                "sin(x)*cos(y)",
                "exp(-(x^2+y^2))",
                "x^2+y^2",
                "sin(x+y)",
                "cos(x-y)",
                "sin(x^2+y^2)"
        };

        // Beispielliste erstellen
        JList<String> exampleList = new JList<>(examples);
        exampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Doppelklick-Listener
        exampleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = exampleList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        functionField.setText(examples[index]);
                        renderPlot();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(exampleList);
        scrollPane.setPreferredSize(new Dimension(200, 100));

        panel.add(scrollPane);

        return panel;
    }

    /**
     * Richtet die Maus-Listener für Interaktionen ein
     */
    private void setupMouseListeners() {
        // Mouse-Listener für Rotation und Zoom
        plotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;

                // Shift-Taste gedrückt = Panning, sonst Rotation
                isPanning = e.isShiftDown();
                debug("Interaktion gestartet: " + (isPanning ? "Panning" : "Rotation"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        plotPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Berechne Verschiebung
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;

                    if (isPanning) {
                        // Pan-Modus: Verschiebe den Wertebereich
                        // Berechne die Verschiebung im Wertebereich
                        // Die Verschiebung ist umgekehrt zur Mausbewegung, daher negativ
                        double xRange = currentXMax - currentXMin;
                        double yRange = currentYMax - currentYMin;

                        // Skaliere die Verschiebung relativ zur Größe des Wertebereichs
                        // und der Größe des Anzeigebereichs
                        double viewWidth = plotPanel.getWidth();
                        double viewHeight = plotPanel.getHeight();

                        double deltaX = -dx * (xRange / viewWidth);
                        double deltaY = dy * (yRange / viewHeight); // Y-Achse ist invertiert

                        // Aktualisiere die Wertebereiche
                        currentXMin += deltaX;
                        currentXMax += deltaX;
                        currentYMin += deltaY;
                        currentYMax += deltaY;

                        // Update the fields
                        DecimalFormat df = new DecimalFormat("0.##");
                        xMinField.setText(df.format(currentXMin));
                        xMaxField.setText(df.format(currentXMax));
                        yMinField.setText(df.format(currentYMin));
                        yMaxField.setText(df.format(currentYMax));

                        // Neuen Plot mit verschobenem Wertebereich zeichnen
                        renderPlot();

                        debug("Wertebereich verschoben nach: x=[" + currentXMin + "," + currentXMax +
                                "], y=[" + currentYMin + "," + currentYMax + "]");
                    } else {
                        // Rotations-Modus
                        double rotationScale = 0.5;
                        currentRotationX += dy * rotationScale;
                        currentRotationZ += dx * rotationScale;

                        // Begrenze Rotation auf 0-360 Grad
                        currentRotationX = (currentRotationX + 360) % 360;
                        currentRotationZ = (currentRotationZ + 360) % 360;

                        // UI-Elemente aktualisieren
                        rotationXSlider.setValue((int) currentRotationX);
                        rotationZSlider.setValue((int) currentRotationZ);

                        // Aktualisiere Rotation im Renderer
                        updateRotation();
                    }

                    lastMousePos = e.getPoint();
                }
            }
        });

        // Mouse-Wheel-Listener für Wertebereichsänderung (wie im 2D-Plotter)
        plotPanel.addMouseWheelListener(e -> {
            try {
                // Aktuelle Wertebereiche holen (mit verbessertem Parsen)
                double xMin = parseDecimal(xMinField.getText().trim());
                double xMax = parseDecimal(xMaxField.getText().trim());
                double yMin = parseDecimal(yMinField.getText().trim());
                double yMax = parseDecimal(yMaxField.getText().trim());

                // Bereichsgröße berechnen
                double xRange = xMax - xMin;
                double yRange = yMax - yMin;

                // Zoom-Faktor basierend auf Mausrad-Richtung
                double zoomFactor = e.getWheelRotation() < 0 ? 0.8 : 1.25; // Zoomen wir rein oder raus?

                // Neue Wertebereiche berechnen (zentriert)
                double newXRange = xRange * zoomFactor;
                double newYRange = yRange * zoomFactor;

                // Mittelpunkt beibehalten
                double xCenter = (xMax + xMin) / 2;
                double yCenter = (yMax + yMin) / 2;

                // Neue Grenzen berechnen
                double newXMin = xCenter - newXRange / 2;
                double newXMax = xCenter + newXRange / 2;
                double newYMin = yCenter - newYRange / 2;
                double newYMax = yCenter + newYRange / 2;

                // Felder aktualisieren
                DecimalFormat df = new DecimalFormat("0.##");
                xMinField.setText(df.format(newXMin));
                xMaxField.setText(df.format(newXMax));
                yMinField.setText(df.format(newYMin));
                yMaxField.setText(df.format(newYMax));

                // Aktualisiere die aktuellen Wertebereiche
                currentXMin = newXMin;
                currentXMax = newXMax;
                currentYMin = newYMin;
                currentYMax = newYMax;

                // Neu rendern
                renderPlot();

                debug("Wertebereich geändert durch Mausrad: " +
                        "[" + df.format(newXMin) + ", " + df.format(newXMax) + "] x " +
                        "[" + df.format(newYMin) + ", " + df.format(newYMax) + "]");

            } catch (Exception ex) {
                debug("Fehler beim Ändern des Wertebereichs: " + ex.getMessage());
            }
        });
    }

    /**
     * Aktualisiert die Bereichs-Eingabefelder aus den aktuellen Werten des
     * Renderers
     */
    private void updateRangeFields() {
        try {
            // Felder aktualisieren
            DecimalFormat df = new DecimalFormat("0.##");
            xMinField.setText(df.format(currentXMin));
            xMaxField.setText(df.format(currentXMax));
            yMinField.setText(df.format(currentYMin));
            yMaxField.setText(df.format(currentYMax));
        } catch (Exception ex) {
            // Wenn es Probleme gibt, Felder unverändert lassen
            debug("Konnte Bereichsfelder nicht aktualisieren: " + ex.getMessage());
        }
    }

    /**
     * Rendert den Plot mit aktuellen Einstellungen
     */
    private void renderPlot() {
        try {
            String functionExpr = functionField.getText().trim();

            // Überprüfe auf leere Funktion
            if (functionExpr.isEmpty()) {
                debug("Leere Funktionseingabe");
                JOptionPane.showMessageDialog(
                        this,
                        "Bitte geben Sie eine Funktion ein.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Bereichsangaben parsen mit verbesserter Methode
            double xMin = parseDecimal(xMinField.getText().trim());
            double xMax = parseDecimal(xMaxField.getText().trim());
            double yMin = parseDecimal(yMinField.getText().trim());
            double yMax = parseDecimal(yMaxField.getText().trim());

            // Bereichsgültigkeiten überprüfen
            if (xMin >= xMax || yMin >= yMax) {
                debug("Ungültige Bereichsangaben");
                JOptionPane.showMessageDialog(
                        this,
                        "Die Min-Werte müssen kleiner als die Max-Werte sein.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            debug("Rendere Funktion: " + functionExpr + " im Bereich x=[" + xMin + "," + xMax + "], y=[" + yMin + ","
                    + yMax + "]");

            // Speichere die aktuellen Wertebereiche
            currentXMin = xMin;
            currentXMax = xMax;
            currentYMin = yMin;
            currentYMax = yMax;

            // Auflösung aus Slider holen
            int resolution = resolutionSlider.getValue();

            // Funktion und Grenzen im Renderer setzen
            if (renderer != null) {
                renderer.setFunction(functionExpr);
                renderer.setBounds(xMin, xMax, yMin, yMax);
                renderer.setResolution(resolution);
                renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
                renderer.setScale(currentScale);
                renderer.setShowCoordinateSystem(showCoordinateSystemCheckbox.isSelected());
                renderer.setShowGrid(showGridCheckbox.isSelected());
                renderer.setShowHelperLines(showHelperLinesCheckbox.isSelected());
            } else {
                renderer = new Plot3DRenderer(functionExpr, xMin, xMax, yMin, yMax, resolution);
                renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
                renderer.setScale(currentScale);
                renderer.setShowCoordinateSystem(showCoordinateSystemCheckbox.isSelected());
                renderer.setShowGrid(showGridCheckbox.isSelected());
                renderer.setShowHelperLines(showHelperLinesCheckbox.isSelected());
            }

            // Plot neu zeichnen
            plotPanel.repaint();

        } catch (NumberFormatException e) {
            debug("Fehler beim Parsen der Zahlen: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Bitte geben Sie gültige Zahlen für die Bereiche ein.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            debug("Fehler beim Rendern: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Fehler beim Rendern der Funktion: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Parst einen Dezimalwert aus einem String, unterstützt sowohl Punkt als auch
     * Komma als Dezimaltrennzeichen
     */
    private double parseDecimal(String text) throws NumberFormatException, ParseException {
        // Erst direkt parsen versuchen (für Punkt als Dezimaltrennzeichen)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Falls fehlgeschlagen, Komma durch Punkt ersetzen und erneut versuchen
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Als letzten Versuch die aktuelle Locale verwenden
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }

    /**
     * Aktualisiert die Rotation im Renderer
     */
    private void updateRotation() {
        if (renderer != null) {
            renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
            plotPanel.repaint();
        }
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    private void resetView() {
        // Standardwerte für Rotation
        currentRotationX = 30;
        currentRotationY = 0;
        currentRotationZ = 30;
        currentScale = 1.0;

        // UI aktualisieren
        rotationXSlider.setValue((int) currentRotationX);
        rotationYSlider.setValue((int) currentRotationY);
        rotationZSlider.setValue((int) currentRotationZ);

        // Wertebereiche auf Standardwerte zurücksetzen
        currentXMin = DEFAULT_MIN;
        currentXMax = DEFAULT_MAX;
        currentYMin = DEFAULT_MIN;
        currentYMax = DEFAULT_MAX;

        // Bereichsfelder auf Originalwerte zurücksetzen
        xMinField.setText(String.valueOf(DEFAULT_MIN));
        xMaxField.setText(String.valueOf(DEFAULT_MAX));
        yMinField.setText(String.valueOf(DEFAULT_MIN));
        yMaxField.setText(String.valueOf(DEFAULT_MAX));

        // Renderer aktualisieren
        try {
            // Grenzen im Renderer aktualisieren
            renderer.setBounds(DEFAULT_MIN, DEFAULT_MAX, DEFAULT_MIN, DEFAULT_MAX);

            // Rotation und Skalierung aktualisieren
            renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
            renderer.setScale(currentScale);

            plotPanel.repaint();

            debug("Ansicht auf Standardwerte zurückgesetzt");
        } catch (Exception e) {
            debug("Fehler beim Zurücksetzen der Ansicht: " + e.getMessage());
        }
    }

    /**
     * Gibt ein BufferedImage des aktuellen Plots zurück
     */
    public BufferedImage getPlotImage(int width, int height) {
        if (renderer != null) {
            return renderer.createImage(width, height);
        }
        return null;
    }

    /**
     * Setzt den DebugManager für Debug-Ausgaben
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
    }

    /**
     * Schreibt Debug-Informationen
     */
    private void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[3D-Plotter] " + message);
        } else {
            System.out.println("[3D-Plotter] " + message);
        }
    }
}