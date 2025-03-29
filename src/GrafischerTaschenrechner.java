import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Erweiterter grafischer Taschenrechner mit Tab-Interface für verschiedene Modi
 * Mit 3D-Funktionsplotter Erweiterung
 */
public class GrafischerTaschenrechner extends Taschenrechner {
    private JTabbedPane tabbedPane;
    private PlotterPanel plotterPanel;
    private Plot3DPanel plot3DPanel; // Neue Variable für den 3D-Plotter
    private StatisticsPanel statisticsPanel;
    private ConverterPanel converterPanel;
    private ScientificPanel scientificPanel;
    private JMenuBar menuBar;
    private JCheckBoxMenuItem showIntersectionsItem;

    public GrafischerTaschenrechner() {
        super(); // Ruft den Konstruktor des Taschenrechners auf

        // Bestehende Komponenten holen
        Container contentPane = getContentPane();
        Component mainComponent = contentPane.getComponent(0); // Das ist das main panel
        contentPane.remove(mainComponent);

        // Menüleiste erstellen
        createMenuBar();
        setJMenuBar(menuBar);

        // TabbedPane erstellen
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Standard", mainComponent);

        // Wissenschaftlicher Taschenrechner erstellen und hinzufügen
        scientificPanel = new ScientificPanel(this);
        tabbedPane.addTab("Wissenschaftlich", scientificPanel);

        // Funktionsplotter erstellen und hinzufügen
        plotterPanel = new PlotterPanel();

        // 3D-Funktionsplotter erstellen und hinzufügen
        plot3DPanel = new Plot3DPanel(this);

        // Statistik-Panel erstellen und hinzufügen
        statisticsPanel = new StatisticsPanel(this);

        // Umrechnungs-Panel erstellen und hinzufügen
        converterPanel = new ConverterPanel(this);

        // Verbinde den DebugManager mit den Panels
        DebugManager debugManager = getDebugManager();
        if (debugManager != null) {
            plotterPanel.setDebugManager(debugManager);
            scientificPanel.setDebugManager(debugManager);
            statisticsPanel.setDebugManager(debugManager);
            converterPanel.setDebugManager(debugManager);
            plot3DPanel.setDebugManager(debugManager); // Neues Panel mit Debug verbinden
            debug("DebugManager mit allen Panels verbunden");
        } else {
            System.out.println("Warnung: DebugManager konnte nicht mit Panels verbunden werden");
        }

        tabbedPane.addTab("Funktionsplotter", plotterPanel);
        tabbedPane.addTab("3D-Funktionsplotter", plot3DPanel); // Neuen Tab für 3D-Plotter hinzufügen
        tabbedPane.addTab("Statistik", statisticsPanel);
        tabbedPane.addTab("Umrechner", converterPanel);

        // Tab-Wechsel-Event, um alles neu zu zeichnen
        tabbedPane.addChangeListener(e -> {
            tabbedPane.repaint();
            int selectedIndex = tabbedPane.getSelectedIndex();

            if (selectedIndex == 2) { // Funktionsplotter
                plotterPanel.repaint();
                debug("Zu Funktionsplotter-Tab gewechselt");

                // Aktualisiere Checkboxen im Menü entsprechend den Einstellungen im Plotter
                if (showIntersectionsItem != null) {
                    showIntersectionsItem.setSelected(plotterPanel.isShowingIntersections());
                }
            } else if (selectedIndex == 3) { // 3D-Funktionsplotter
                debug("Zu 3D-Funktionsplotter-Tab gewechselt");
                // Panel neuzeichnen
                plot3DPanel.repaint();
            } else if (selectedIndex == 1) { // Wissenschaftlich
                debug("Zu Wissenschaftlich-Tab gewechselt");
                scientificPanel.refreshDisplay();
            } else if (selectedIndex == 4) { // Statistik
                debug("Zu Statistik-Tab gewechselt");
                statisticsPanel.refresh();
            } else if (selectedIndex == 5) { // Umrechner
                debug("Zu Umrechner-Tab gewechselt");
                converterPanel.refresh();
            } else {
                debug("Zu Standard-Tab gewechselt");
            }
        });

        // TabbedPane zum Hauptfenster hinzufügen
        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Fenstergröße anpassen - etwas erhöhen für den 3D-Plotter
        setSize(950, 700);
        setLocationRelativeTo(null);

        // Schaltfläche für Transfer vom Taschenrechner zum Plotter hinzufügen
        JButton plotButton = new JButton("Plot");
        plotButton.setFont(new Font("Arial", Font.PLAIN, 16));
        plotButton.setBackground(new Color(220, 250, 220)); // Leichtes Grün

        // Minimum-Größe setzen (wie bei anderen Buttons)
        FontMetrics metrics = plotButton.getFontMetrics(plotButton.getFont());
        int textWidth = metrics.stringWidth(plotButton.getText());
        int textHeight = metrics.getHeight();
        int width = Math.max(textWidth + 16, 40);
        int height = Math.max(textHeight + 10, 35);
        plotButton.setMinimumSize(new Dimension(width, height));
        plotButton.setPreferredSize(new Dimension(width, height));

        // Aktion beim Klicken auf Plot-Button
        plotButton.addActionListener(e -> transferToPlotter());

        // Zum Taschenrechner-Control-Panel hinzufügen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(plotButton);

        // Button-Panel zum Hauptpanel hinzufügen
        if (mainComponent instanceof JPanel) {
            ((JPanel) mainComponent).add(buttonPanel, BorderLayout.SOUTH);
        }

        debug("Erweiterter grafischer Taschenrechner mit 3D-Plotter initialisiert");
    }

    /**
     * Gibt den DebugManager des Taschenrechners zurück
     */
    private DebugManager getDebugManager() {
        try {
            // Dies ist ein Reflection-basierter Ansatz, um auf den DebugManager zuzugreifen
            // In der realen Implementierung sollte es eine sauberere Lösung geben
            java.lang.reflect.Field field = Taschenrechner.class.getDeclaredField("debugManager");
            field.setAccessible(true);
            return (DebugManager) field.get(this);
        } catch (Exception e) {
            System.out.println("Fehler beim Zugriff auf DebugManager: " + e.getMessage());
            return null;
        }
    }

    /**
     * Erstellt die Menüleiste mit allen Menüs und Untermenüs
     */
    private void createMenuBar() {
        menuBar = new JMenuBar();

        // Datei-Menü
        JMenu fileMenu = new JMenu("Datei");
        fileMenu.setMnemonic(KeyEvent.VK_D);

        JMenuItem newItem = new JMenuItem("Neu", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> clearAll());

        JMenuItem exitItem = new JMenuItem("Beenden", KeyEvent.VK_B);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Bearbeiten-Menü
        JMenu editMenu = new JMenu("Bearbeiten");
        editMenu.setMnemonic(KeyEvent.VK_B);

        JMenuItem clearItem = new JMenuItem("Löschen", KeyEvent.VK_L);
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        clearItem.addActionListener(e -> {
            // Im Taschenrechner das Display löschen
            if (tabbedPane.getSelectedIndex() == 0) {
                setDisplayText("0");
            }
            // Im Wissenschaftlichen Taschenrechner
            else if (tabbedPane.getSelectedIndex() == 1) {
                scientificPanel.clearDisplay();
            }
            // Im Plotter die aktuell ausgewählte Funktion löschen
            else if (tabbedPane.getSelectedIndex() == 2) {
                // Implementierung für Plotter-Funktionslöschen
            }
            // In Statistik
            else if (tabbedPane.getSelectedIndex() == 4) {
                statisticsPanel.clearData();
            }
            // Im Umrechner
            else if (tabbedPane.getSelectedIndex() == 5) {
                converterPanel.clear();
            }
        });

        editMenu.add(clearItem);

        // Ansicht-Menü
        JMenu viewMenu = new JMenu("Ansicht");
        viewMenu.setMnemonic(KeyEvent.VK_A);

        JMenuItem calcItem = new JMenuItem("Standard Taschenrechner", KeyEvent.VK_T);
        calcItem.addActionListener(e -> tabbedPane.setSelectedIndex(0));

        JMenuItem scientificItem = new JMenuItem("Wissenschaftlicher Taschenrechner", KeyEvent.VK_W);
        scientificItem.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        JMenuItem plotterItem = new JMenuItem("Funktionsplotter", KeyEvent.VK_F);
        plotterItem.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        // Neuer Menüpunkt für 3D-Funktionsplotter
        JMenuItem plot3DItem = new JMenuItem("3D-Funktionsplotter", KeyEvent.VK_D);
        plot3DItem.addActionListener(e -> tabbedPane.setSelectedIndex(3));

        JMenuItem statsItem = new JMenuItem("Statistik", KeyEvent.VK_S);
        statsItem.addActionListener(e -> tabbedPane.setSelectedIndex(4));

        JMenuItem converterItem = new JMenuItem("Umrechner", KeyEvent.VK_U);
        converterItem.addActionListener(e -> tabbedPane.setSelectedIndex(5));

        JCheckBoxMenuItem showGridItem = new JCheckBoxMenuItem("Koordinatensystem anzeigen");
        showGridItem.setSelected(true);
        showGridItem.addActionListener(e -> {
            if (plotterPanel != null) {
                plotterPanel.getGraphPanel().setShowGrid(showGridItem.isSelected());
            }
        });

        viewMenu.add(calcItem);
        viewMenu.add(scientificItem);
        viewMenu.add(plotterItem);
        viewMenu.add(plot3DItem); // Neuen Menüpunkt einfügen
        viewMenu.add(statsItem);
        viewMenu.add(converterItem);
        viewMenu.addSeparator();
        viewMenu.add(showGridItem);

        // Extras-Menü
        JMenu toolsMenu = new JMenu("Extras");
        toolsMenu.setMnemonic(KeyEvent.VK_E);

        JMenuItem historyItem = new JMenuItem("Verlauf anzeigen");
        historyItem.addActionListener(e -> toggleHistory());

        JMenuItem debugItem = new JMenuItem("Debug-Modus");
        debugItem.addActionListener(e -> toggleDebug());

        // Schnittpunkte anzeigen ins Extras-Menü verschieben
        showIntersectionsItem = new JCheckBoxMenuItem("Schnittpunkte anzeigen");
        showIntersectionsItem.setSelected(false);
        showIntersectionsItem.addActionListener(e -> {
            if (plotterPanel != null) {
                boolean show = showIntersectionsItem.isSelected();
                // Versuche, auf die Schnittpunkt-Checkbox im Panel zuzugreifen
                // (falls wir einen direkten Zugriff haben)
                try {
                    // Dies ist ein Versuch, die Checkbox direkt zu ändern, falls möglich
                    JCheckBox intersectionCheckbox = plotterPanel.getIntersectionCheckbox();
                    if (intersectionCheckbox != null) {
                        // Wichtig: triggere den ActionListener der Checkbox
                        if (intersectionCheckbox.isSelected() != show) {
                            intersectionCheckbox.doClick();
                        }
                    } else {
                        // Falls kein direkter Zugriff, verwende die Methode im Plotter
                        plotterPanel.toggleIntersections(show);
                    }
                } catch (Exception ex) {
                    // Falls die Methode nicht existiert, verwende die grundlegende Methode
                    plotterPanel.getGraphPanel().toggleIntersections(show);
                }
            }
        });

        toolsMenu.add(historyItem);
        toolsMenu.add(debugItem);
        toolsMenu.addSeparator();
        toolsMenu.add(showIntersectionsItem);

        // Hilfe-Menü
        JMenu helpMenu = new JMenu("Hilfe");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("Über", KeyEvent.VK_U);
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Wissenschaftlicher Taschenrechner mit Funktionsplotter und 3D-Plotter\n" +
                            "Version 3.0\n" +
                            "© 2025",
                    "Über",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        helpMenu.add(aboutItem);

        // Alle Menüs zur Menüleiste hinzufügen
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
    }

    /**
     * Löscht alle Eingaben und setzt die Anwendung zurück
     */
    private void clearAll() {
        int selectedTab = tabbedPane.getSelectedIndex();

        if (selectedTab == 0) {
            // Standard Taschenrechner zurücksetzen
            setDisplayText("0");
        } else if (selectedTab == 1) {
            // Wissenschaftlicher Taschenrechner zurücksetzen
            scientificPanel.clearDisplay();
        } else if (selectedTab == 2) {
            // Funktionsplotter zurücksetzen - alle Funktionen löschen
            plotterPanel.getGraphPanel().clearFunctions();
            // Ansicht zurücksetzen
            plotterPanel.getGraphPanel().resetView();
        } else if (selectedTab == 3) {
            // 3D-Funktionsplotter zurücksetzen (wenn möglich)
            // Dies erfordert eine entsprechende Methode im Plot3DPanel
        } else if (selectedTab == 4) {
            // Statistik zurücksetzen
            statisticsPanel.clearData();
        } else if (selectedTab == 5) {
            // Umrechner zurücksetzen
            converterPanel.clear();
        }
    }

    /**
     * Überträgt den aktuellen Taschenrechner-Ausdruck zum Plotter
     */
    private void transferToPlotter() {
        // Aktuellen Ausdruck vom Taschenrechner holen
        String expression = getDisplayText();

        // Entscheide, ob es sich um eine 2D- oder 3D-Funktion handelt
        if (expression != null && !expression.isEmpty() && expression.contains("y")) {
            // Vermutlich eine 3D-Funktion (enthält 'y')
            transferFunctionTo3DPlotter(expression);
        } else {
            // Vermutlich eine normale 2D-Funktion
            transferFunctionToPlotter(expression);
        }
    }

    /**
     * Überträgt eine spezifische Funktion zum Plotter
     * (Wird sowohl vom manuellen Übertragen als auch von der automatischen
     * Funktionserkennung verwendet)
     */
    public void transferFunctionToPlotter(String expression) {
        if (expression != null && !expression.isEmpty() && !expression.equals("0")) {
            // Zum Plotter-Tab wechseln
            tabbedPane.setSelectedIndex(2);
            debug("Ausdruck zum Plotter übertragen: " + expression);

            // Ausdruck in das Funktionsfeld übertragen
            try {
                Component[] components = plotterPanel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        transferExpressionToTextField((JPanel) comp, expression);
                    } else if (comp instanceof JSplitPane) {
                        // Bei Spalten-Layout die rechte Komponente durchsuchen
                        JSplitPane splitPane = (JSplitPane) comp;
                        Component rightComponent = splitPane.getRightComponent();
                        if (rightComponent instanceof JPanel) {
                            transferExpressionToTextField((JPanel) rightComponent, expression);
                        }
                    }
                }
            } catch (Exception e) {
                debug("Fehler beim Übertragen des Ausdrucks: " + e.getMessage());
                System.err.println("Fehler beim Übertragen des Ausdrucks: " + e.getMessage());
            }
        } else {
            debug("Versuch, leeren Ausdruck zu übertragen");
            JOptionPane.showMessageDialog(this,
                    "Bitte zuerst einen Ausdruck im Taschenrechner eingeben.",
                    "Leerer Ausdruck",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Überträgt eine Funktion zum 3D-Plotter
     */
    public void transferFunctionTo3DPlotter(String expression) {
        if (expression != null && !expression.isEmpty() && !expression.equals("0")) {
            // Zum 3D-Plotter-Tab wechseln
            tabbedPane.setSelectedIndex(3);
            debug("Ausdruck zum 3D-Plotter übertragen: " + expression);

            // Ausdruck in das Funktionsfeld übertragen
            try {
                // Zugriff auf das Funktionsfeld im Plot3DPanel
                Component[] components = plot3DPanel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JSplitPane) {
                        JSplitPane splitPane = (JSplitPane) comp;
                        Component rightComponent = splitPane.getRightComponent();
                        if (rightComponent instanceof JPanel) {
                            transferExpressionTo3DTextField((JPanel) rightComponent, expression);
                        }
                    }
                }
            } catch (Exception e) {
                debug("Fehler beim Übertragen des Ausdrucks zum 3D-Plotter: " + e.getMessage());
                System.err.println("Fehler beim Übertragen des Ausdrucks zum 3D-Plotter: " + e.getMessage());
            }
        } else {
            debug("Versuch, leeren Ausdruck zum 3D-Plotter zu übertragen");
            JOptionPane.showMessageDialog(this,
                    "Bitte zuerst einen Ausdruck im Taschenrechner eingeben.",
                    "Leerer Ausdruck",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Sucht rekursiv nach dem Textfeld im Plotter und setzt den Ausdruck
     */
    private boolean transferExpressionToTextField(JPanel panel, String expression) {
        Component[] components = panel.getComponents();

        for (Component comp : components) {
            if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                // Wir nehmen an, dass das Textfeld für die Funktionseingabe ist
                textField.setText(expression);
                return true;
            } else if (comp instanceof JPanel) {
                if (transferExpressionToTextField((JPanel) comp, expression)) {
                    return true;
                }
            } else if (comp instanceof JSplitPane) {
                JSplitPane splitPane = (JSplitPane) comp;
                Component left = splitPane.getLeftComponent();
                Component right = splitPane.getRightComponent();

                if (left instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) left, expression)) {
                        return true;
                    }
                }
                if (right instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) right, expression)) {
                        return true;
                    }
                }
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) view, expression)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Sucht rekursiv nach dem Textfeld im 3D-Plotter und setzt den Ausdruck
     */
    private boolean transferExpressionTo3DTextField(JPanel panel, String expression) {
        Component[] components = panel.getComponents();

        for (Component comp : components) {
            if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                // Das Erste gefundene TextField nehmen wir als Funktionsfeld an
                textField.setText(expression);
                return true;
            } else if (comp instanceof JPanel) {
                if (transferExpressionTo3DTextField((JPanel) comp, expression)) {
                    return true;
                }
            } else if (comp instanceof JSplitPane) {
                JSplitPane splitPane = (JSplitPane) comp;
                Component left = splitPane.getLeftComponent();
                Component right = splitPane.getRightComponent();

                if (left instanceof JPanel) {
                    if (transferExpressionTo3DTextField((JPanel) left, expression)) {
                        return true;
                    }
                }
                if (right instanceof JPanel) {
                    if (transferExpressionTo3DTextField((JPanel) right, expression)) {
                        return true;
                    }
                }
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JPanel) {
                    if (transferExpressionTo3DTextField((JPanel) view, expression)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Hauptmethode
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GrafischerTaschenrechner rechner = new GrafischerTaschenrechner();
            rechner.setVisible(true);
        });
    }
}