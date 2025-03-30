import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verwaltet die 3D-Funktionen des Plotters
 * Fügt Funktionen hinzu, entfernt sie und verwaltet die Funktionsliste
 */
public class Plot3DFunctionManager {
    private final Plot3DRenderer renderer;
    private final Plot3DViewController viewController;

    // Funktionslisten-Komponenten
    private DefaultListModel<String> functionListModel;
    private JList<String> functionList;
    private JPopupMenu functionPopup;

    // Pattern für Funktionseinträge
    private final Pattern functionPattern = Pattern.compile("f\\(x,y\\) = (.+) \\[(.+)\\]");

    // Debug-Referenz
    private DebugManager debugManager;

    /**
     * Konstruktor für den Funktionsmanager
     * 
     * @param renderer       Der Plot3DRenderer
     * @param viewController Der View-Controller
     */
    public Plot3DFunctionManager(Plot3DRenderer renderer, Plot3DViewController viewController) {
        this.renderer = renderer;
        this.viewController = viewController;

        // Initialisiere die Listmodell und Liste
        functionListModel = new DefaultListModel<>();
        functionList = new JList<>(functionListModel);
        functionList.setCellRenderer(new FunctionCellRenderer());

        // Kontextmenü für die Funktionsliste
        setupContextMenu();

        // Doppelklick-Listener für Bearbeitung
        setupListListeners();
    }

    /**
     * Erstellt das Kontextmenü für die Funktionsliste
     */
    private void setupContextMenu() {
        functionPopup = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Bearbeiten");
        editItem.addActionListener(e -> editSelectedFunction());

        JMenuItem removeItem = new JMenuItem("Entfernen");
        removeItem.addActionListener(e -> removeSelectedFunction());

        functionPopup.add(editItem);
        functionPopup.addSeparator();
        functionPopup.add(removeItem);
    }

    /**
     * Richtet die Listener für die Funktionsliste ein
     */
    private void setupListListeners() {
        functionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Doppelklick - Bearbeitungsdialog öffnen
                    editSelectedFunction();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // Rechtsklick - Kontextmenü anzeigen
                    int index = functionList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        functionList.setSelectedIndex(index);
                        functionPopup.show(functionList, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     * Öffnet einen Dialog zum Bearbeiten der ausgewählten Funktion
     */
    public void editSelectedFunction() {
        int selectedIndex = functionList.getSelectedIndex();
        if (selectedIndex < 0)
            return;

        // Extrahiere die Funktionsdaten
        String entry = functionListModel.getElementAt(selectedIndex);
        Matcher matcher = functionPattern.matcher(entry);

        if (matcher.find()) {
            String function = matcher.group(1);
            String colorName = matcher.group(2);

            // Setze die Werte in die Eingabefelder
            // In der refaktorisierten Version müssen wir stattdessen einen Dialog erstellen
            String newFunction = JOptionPane.showInputDialog(
                    functionList,
                    "Bearbeiten Sie die Funktion:",
                    function);

            if (newFunction != null && !newFunction.isEmpty()) {
                // Farbauswahl
                Object[] colorOptions = ColorChooser.getColorNames();
                String newColorName = (String) JOptionPane.showInputDialog(
                        functionList,
                        "Wählen Sie eine Farbe:",
                        "Farbauswahl",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        colorOptions,
                        colorName);

                if (newColorName != null) {
                    updateFunction(selectedIndex, newFunction, newColorName);
                }
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Funktion
     */
    private void updateFunction(int index, String function, String colorName) {
        if (function.isEmpty()) {
            JOptionPane.showMessageDialog(functionList, "Bitte geben Sie eine Funktion ein.");
            return;
        }

        // Farbauswahl verarbeiten
        Color color;

        if (colorName.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
            // Bei "Zufällig" eine neue Farbe generieren
            color = ColorChooser.generateRandomColor();
            colorName = ColorChooser.getColorName(color);
        } else if (colorName.equals("Weitere...")) {
            // Bei "Weitere..." auf "Zufällig" zurückfallen
            color = ColorChooser.generateRandomColor();
            colorName = ColorChooser.getColorName(color);
        } else {
            // Sonst die ausgewählte Farbe verwenden
            color = ColorChooser.getColorByName(colorName);
        }

        // Alten Eintrag entfernen
        functionListModel.remove(index);

        // Funktion aus dem Renderer entfernen
        renderer.removeFunction(index);

        // Neue Funktion hinzufügen (an derselben Stelle)
        String newEntry = "f(x,y) = " + function + " [" + colorName + "]";
        functionListModel.add(index, newEntry);

        try {
            // Funktion im Renderer aktualisieren
            renderer.addFunction(function, color);
            debug("Funktion aktualisiert: " + function);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    functionList,
                    "Fehler beim Aktualisieren der Funktion: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            debug("Fehler beim Aktualisieren der Funktion: " + e.getMessage());
        }
    }

    /**
     * Fügt eine neue Funktion hinzu
     */
    public void addFunction(String function, String colorName) {
        function = function.trim();

        if (function.isEmpty()) {
            JOptionPane.showMessageDialog(functionList, "Bitte geben Sie eine Funktion ein.");
            return;
        }

        // Farbauswahl verarbeiten
        Color color;

        if (colorName.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
            // Bei "Zufällig" eine neue Farbe generieren
            color = ColorChooser.generateRandomColor();
            colorName = ColorChooser.getColorName(color);
        } else if (colorName.equals("Weitere...")) {
            // Bei "Weitere..." auf "Zufällig" zurückfallen
            color = ColorChooser.generateRandomColor();
            colorName = ColorChooser.getColorName(color);
        } else {
            // Sonst die ausgewählte Farbe verwenden
            color = ColorChooser.getColorByName(colorName);
        }

        // Listeneintrag erstellen
        String entry = "f(x,y) = " + function + " [" + colorName + "]";
        functionListModel.addElement(entry);

        try {
            // Funktion zum Renderer hinzufügen
            renderer.addFunction(function, color);
            debug("Funktion hinzugefügt: " + function);
        } catch (Exception e) {
            // Fehler behandeln - Eintrag wieder entfernen
            functionListModel.removeElement(entry);

            JOptionPane.showMessageDialog(
                    functionList,
                    "Fehler beim Hinzufügen der Funktion: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            debug("Fehler beim Hinzufügen der Funktion: " + e.getMessage());
        }
    }

    /**
     * Entfernt die ausgewählte Funktion
     */
    public void removeSelectedFunction() {
        int selectedIndex = functionList.getSelectedIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(functionList,
                    "Bitte wählen Sie eine Funktion zum Entfernen aus.");
            return;
        }

        // Funktion aus dem Listmodell entfernen
        functionListModel.remove(selectedIndex);

        // Funktion aus dem Renderer entfernen
        renderer.removeFunction(selectedIndex);

        debug("Funktion an Position " + selectedIndex + " entfernt");
    }

    /**
     * Entfernt alle Funktionen
     */
    public void clearAllFunctions() {
        int count = functionListModel.getSize();
        if (count == 0)
            return;

        int option = JOptionPane.showConfirmDialog(
                functionList,
                "Möchten Sie wirklich alle " + count + " Funktionen löschen?",
                "Alle Funktionen löschen",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            // Alle Funktionen aus dem Listmodell entfernen
            functionListModel.clear();

            // Alle Funktionen aus dem Renderer entfernen
            renderer.clearFunctions();

            debug("Alle Funktionen gelöscht");
        }
    }

    /**
     * Setzt den DebugManager für Logging
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
    }

    /**
     * Schreibt Debug-Informationen in das Debug-Fenster
     */
    private void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[3D-Funktionen] " + message);
        } else {
            System.out.println("[3D-Funktionen] " + message);
        }
    }

    /**
     * Gibt die Funktionsliste zurück
     */
    public JList<String> getFunctionList() {
        return functionList;
    }

    /**
     * Gibt das Funktionslisten-Modell zurück
     */
    public DefaultListModel<String> getFunctionListModel() {
        return functionListModel;
    }

    /**
     * Benutzerdefinierter Cell Renderer für die Funktionsliste mit Farbvorschau
     */
    private static class FunctionCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // Farbe aus dem String extrahieren
            String entry = (String) value;
            Pattern colorPattern = Pattern.compile("f\\(x,y\\) = (.+) \\[(.+)\\]");
            Matcher matcher = colorPattern.matcher(entry);
            if (matcher.find()) {
                String colorName = matcher.group(2);
                Color color = ColorChooser.getColorByName(colorName);

                // Farbkästchen links neben dem Text
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setIcon(createColorIcon(color, 16, 16));
                    label.setIconTextGap(10);
                }
            }
            return c;
        }

        private Icon createColorIcon(Color color, int width, int height) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(color);
                    g.fillRect(x, y, width, height);
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, width - 1, height - 1);
                }

                @Override
                public int getIconWidth() {
                    return width;
                }

                @Override
                public int getIconHeight() {
                    return height;
                }
            };
        }
    }
}