package plugins.plotter3d.ui;

import javax.swing.*;

import common.ColorChooser;
import plugins.plotter3d.Plot3DPanel;
import plugins.plotter3d.renderer.Plot3DRenderer;
import plugins.plotter3d.view.Plot3DViewController;
import plugins.plotter3d.model.Plot3DModel;
import util.debug.DebugManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verwaltet die 3D-Funktionen des Plotters
 * Fügt Funktionen hinzu, entfernt sie und verwaltet die Funktionsliste
 */
public class Plot3DFunctionManager {
    private final Plot3DRenderer renderer;
    private final Plot3DViewController viewController;

    // Referenz auf das übergeordnete Panel
    private Plot3DPanel parentPanel;

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
     * Setzt die Referenz auf das übergeordnete Panel
     */
    public void setParentPanel(Plot3DPanel parentPanel) {
        this.parentPanel = parentPanel;
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

        JMenuItem combineItem = new JMenuItem("Kombinieren");
        combineItem.addActionListener(e -> combineSelectedFunctions());

        functionPopup.add(editItem);
        functionPopup.add(combineItem);
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
                int index = functionList.locationToIndex(e.getPoint());
                if (index < 0)
                    return;

                // Prüfen, ob der Klick im Bereich der Checkbox war
                Rectangle cellBounds = functionList.getCellBounds(index, index);
                if (cellBounds != null) {
                    Rectangle checkBoxBounds = new Rectangle(0, cellBounds.y, 20, cellBounds.height);
                    if (checkBoxBounds.contains(e.getPoint())) {
                        // Sichtbarkeit umschalten
                        toggleFunctionVisibility(index);
                        functionList.repaint();
                        return;
                    }
                }

                if (e.getClickCount() == 2) {
                    // Doppelklick - Bearbeitungsdialog öffnen
                    editSelectedFunction();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // Rechtsklick - Kontextmenü anzeigen
                    functionList.setSelectedIndex(index);
                    functionPopup.show(functionList, e.getX(), e.getY());
                }
            }
        });

        // KeyListener für Tastaturinteraktion
        functionList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    // Leertaste - Sichtbarkeit der ausgewählten Funktion umschalten
                    int selectedIndex = functionList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        toggleFunctionVisibility(selectedIndex);
                    }
                }
            }
        });
    }

    /**
     * Öffnet einen Dialog zum Bearbeiten der ausgewählten Funktion
     * (Verbesserte Version mit ColorPicker Dialog ähnlich dem 2D-Plotter)
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

            // Finde das Hauptfenster als Parent für den Dialog
            Frame parentFrame = JOptionPane.getFrameForComponent(functionList);

            // Bestimme die aktuelle Farbe
            Color currentColor = ColorChooser.getColorByName(colorName);

            // Verwende den verbesserten Dialog zum Bearbeiten
            Plot3DFunctionEditDialog.FunctionEditResult result = Plot3DFunctionEditDialog.showDialog(
                    parentFrame, function, currentColor);

            // Wenn Dialog bestätigt wurde
            if (result != null) {
                String newFunction = result.getFunction();
                Color newColor = result.getColor();

                debug("Dialog bestätigt. Neue Funktion: '" + newFunction + "'");

                // Wenn Zufallsfarbe gewählt wurde, generiere eine neue
                if (result.isUsingRandomColor()) {
                    newColor = ColorChooser.generateRandomColor();
                    debug("Neue Zufallsfarbe generiert");
                }

                // Farbname bestimmen (immer konkreten Farbnamen speichern, nie "Zufällig")
                String newColorName = ColorChooser.getColorName(newColor);
                debug("Neue Farbe: '" + newColorName + "'");

                // Funktion aktualisieren
                updateFunction(selectedIndex, newFunction, newColorName);
            } else {
                debug("Bearbeitung abgebrochen");
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

        // Alten Eintrag holen, um Sichtbarkeitsstatus zu erhalten
        String oldEntry = functionListModel.getElementAt(index);
        String visibilityPrefix = "[x] "; // Standardmäßig sichtbar

        // Überprüfe, ob der alte Eintrag bereits eine Sichtbarkeitsmarkierung hat
        Pattern visibilityPattern = Pattern.compile("^(\\[x\\]|\\[ \\]) (.+)$");
        Matcher matcher = visibilityPattern.matcher(oldEntry);
        if (matcher.find()) {
            visibilityPrefix = matcher.group(1) + " ";
        }

        // Alten Eintrag entfernen
        functionListModel.remove(index);

        // Funktion aus dem Renderer entfernen
        renderer.removeFunction(index);

        // Neue Funktion hinzufügen (an derselben Stelle)
        String newEntry = visibilityPrefix + "f(x,y) = " + function + " [" + colorName + "]";
        functionListModel.add(index, newEntry);

        try {
            // Funktion im Renderer aktualisieren
            renderer.addFunction(function, color);
            debug("Funktion aktualisiert: " + function);

            // Wenn die Funktion vorher unsichtbar war, setze sie auch jetzt auf unsichtbar
            if (visibilityPrefix.equals("[ ] ")) {
                renderer.getFunctions().get(index).setVisible(false);
            }

            // Explizites Neuzeichnen
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }
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

        // Listeneintrag erstellen - mit Sichtbarkeitsmarkierung
        String entry = "[x] f(x,y) = " + function + " [" + colorName + "]";
        functionListModel.addElement(entry);

        try {
            // Funktion zum Renderer hinzufügen
            renderer.addFunction(function, color);
            debug("Funktion hinzugefügt: " + function);

            // Explizites Neuzeichnen
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }

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

        // Explizites Neuzeichnen
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    /**
     * Entfernt alle Funktionen aus der Liste und aus dem Renderer
     */
    public void clearAllFunctions() {
        // Dialog zur Bestätigung anzeigen
        int option = JOptionPane.showConfirmDialog(
                functionList,
                "Möchten Sie wirklich alle Funktionen löschen?",
                "Alle Funktionen löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            // Alle Funktionen aus dem Modell entfernen
            renderer.clearFunctions();
            debug("Alle Funktionen aus dem Renderer entfernt");

            // Liste leeren
            functionListModel.clear();
            debug("Funktionsliste geleert");

            // Explizites Neuzeichnen, um den leeren Plot anzuzeigen
            // und um sicherzustellen, dass die Z-Wertebereiche richtig gesetzt werden
            if (parentPanel != null) {
                parentPanel.renderPlot();
                debug("Plot nach Löschen aller Funktionen neu gezeichnet");
            }
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
        private final JCheckBox checkbox = new JCheckBox();
        private final Pattern visibilityPattern = Pattern.compile("^(\\[x\\]|\\[ \\]) (.+)$");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = value.toString();

            // Prüfe auf Sichtbarkeitsmarkierung
            Matcher matcher = visibilityPattern.matcher(text);
            if (matcher.find()) {
                // Entferne die Markierung aus dem angezeigten Text und setze den
                // Checkbox-Status
                checkbox.setSelected(matcher.group(1).equals("[x]"));
                text = matcher.group(2);
            } else {
                // Keine Markierung, Funktion ist standardmäßig sichtbar
                checkbox.setSelected(true);
            }

            // Farbe aus dem String extrahieren
            Pattern colorPattern = Pattern.compile("f\\(x,y\\) = (.+) \\[(.+)\\]");
            matcher = colorPattern.matcher(text);
            if (matcher.find()) {
                String colorName = matcher.group(2);
                Color color = ColorChooser.getColorByName(colorName);

                // Setze den Label-Text ohne die Sichtbarkeitsmarkierung
                label.setText(text);

                // Farbkästchen neben dem Text
                label.setIcon(createColorIcon(color, 16, 16));
                label.setIconTextGap(10);
            } else {
                label.setText(text);
            }

            // Panel mit Checkbox und Label erstellen
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(checkbox, BorderLayout.WEST);
            panel.add(label, BorderLayout.CENTER);

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                panel.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                panel.setForeground(list.getForeground());
            }

            return panel;
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

    /**
     * Kombiniert mehrere ausgewählte Funktionen zu einer neuen Funktion
     */
    public void combineSelectedFunctions() {
        // Die ausgewählten Indizes aus der Funktionsliste holen
        int[] selectedIndices = functionList.getSelectedIndices();

        // Mindestens 2 Funktionen müssen ausgewählt sein
        if (selectedIndices.length < 2) {
            JOptionPane.showMessageDialog(functionList,
                    "Bitte wählen Sie mindestens zwei Funktionen aus, die kombiniert werden sollen.",
                    "Zu wenige Funktionen ausgewählt",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Indizes in eine Liste umwandeln
        List<Integer> selectedIndexList = new ArrayList<>();
        for (int index : selectedIndices) {
            selectedIndexList.add(index);
        }

        // Dialog anzeigen
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(functionList);
        Plot3DFunctionInterferenceDialog dialog = Plot3DFunctionInterferenceDialog.showDialog(
                parentFrame, parentPanel, selectedIndexList);

        // Wenn der Dialog bestätigt wurde, die neue Funktion erstellen
        if (dialog.isConfirmed()) {
            String functionExpression = dialog.createFunctionExpression();
            String functionName = dialog.getFunctionName();
            Color color = dialog.getSelectedColor();

            if (functionExpression != null && !functionExpression.isEmpty()) {
                // Farbe in Farbnamen umwandeln
                String colorName = ColorChooser.getColorName(color);

                // Eintrag für die Funktionsliste erstellen
                String listEntry = "f(x,y) = " + functionExpression + " [" + colorName + "]";

                // Zur Funktionsliste hinzufügen
                functionListModel.addElement(listEntry);

                try {
                    // Funktion zum Renderer hinzufügen
                    renderer.addFunction(functionExpression, color);
                    debug("Neue kombinierte Funktion erstellt: " + listEntry);

                    // Explizites Neuzeichnen
                    if (parentPanel != null) {
                        parentPanel.renderPlot();
                    }
                } catch (Exception e) {
                    // Bei einem Fehler die Funktion entfernen
                    functionListModel.removeElement(listEntry);

                    JOptionPane.showMessageDialog(
                            functionList,
                            "Fehler beim Hinzufügen der kombinierten Funktion: " + e.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                    debug("Fehler beim Hinzufügen der kombinierten Funktion: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Schaltet die Sichtbarkeit einer Funktion um
     */
    private void toggleFunctionVisibility(int index) {
        if (index < 0 || index >= functionListModel.size()) {
            return;
        }

        // Hole die Funktion aus dem Renderer
        List<Plot3DModel.Function3DInfo> functions = renderer.getFunctions();
        if (index < functions.size()) {
            // Sichtbarkeit im Modell umschalten
            functions.get(index).toggleVisibility();

            // Aktualisiere die Anzeige in der Liste
            String item = functionListModel.getElementAt(index);

            // Überprüfe, ob der Eintrag bereits eine Sichtbarkeitsmarkierung hat
            Pattern visibilityPattern = Pattern.compile("^(\\[x\\]|\\[ \\]) (.+)$");
            Matcher matcher = visibilityPattern.matcher(item);
            if (matcher.find()) {
                // Eintrag hat bereits eine Sichtbarkeitsmarkierung, aktualisiere sie
                boolean isCurrentlyVisible = matcher.group(1).equals("[x]");
                String newPrefix = isCurrentlyVisible ? "[ ] " : "[x] ";
                String restOfItem = matcher.group(2);
                functionListModel.setElementAt(newPrefix + restOfItem, index);
            } else {
                // Eintrag hat noch keine Sichtbarkeitsmarkierung, füge sie hinzu
                boolean isVisible = functions.get(index).isVisible();
                String newPrefix = isVisible ? "[x] " : "[ ] ";
                functionListModel.setElementAt(newPrefix + item, index);
            }

            // Explizites Neuzeichnen
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }
        }
    }
}