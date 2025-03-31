package plugins.plotter2d;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import common.ColorChooser;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panel for function input, color selection, and function list management
 */
public class FunctionInputPanel {
    private final PlotterPanel plotter;

    // UI components
    private final JTextField functionField;
    private final JComboBox<String> colorComboBox;
    private final DefaultListModel<String> functionListModel;
    private final JList<String> functionList;
    private JPopupMenu functionPopup;

    // Pattern zur Extraktion von Funktionsinformationen
    private final Pattern functionPattern = Pattern.compile("f\\(x\\) = (.+) \\[(.+)\\]");

    // Aktualisiertes Muster mit Sichtbarkeitsmarkierung
    private final Pattern visibilityPattern = Pattern.compile("^(\\[x\\]|\\[ \\]) (.+)$");

    /**
     * Creates a new function input panel
     */
    public FunctionInputPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Function input field
        functionField = new JTextField();
        functionField.setToolTipText("Funktion eingeben, z.B. sin(x) oder x^2");

        // Prepare color names with "Zufällig" as first option
        String[] originalColorNames = ColorChooser.getColorNames();

        // Verwende eine Liste für die Sortierung, um Größenprobleme zu vermeiden
        List<String> sortedColorList = new ArrayList<>();

        // Füge "Zufällig" zuerst hinzu, wenn es existiert
        boolean hasRandomOption = false;
        for (String name : originalColorNames) {
            if (name.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
                sortedColorList.add(name);
                hasRandomOption = true;
                break;
            }
        }

        // Wenn keine "Zufällig"-Option gefunden wurde, füge sie hinzu
        if (!hasRandomOption) {
            sortedColorList.add(ColorChooser.RANDOM_COLOR_OPTION);
        }

        // Füge alle anderen Farben hinzu, außer "Weitere..." (falls vorhanden)
        for (String name : originalColorNames) {
            if (!name.equals(ColorChooser.RANDOM_COLOR_OPTION) && !name.equals("Weitere...")) {
                sortedColorList.add(name);
            }
        }

        // Füge "Weitere..." am Ende hinzu
        sortedColorList.add("Weitere...");

        // Konvertiere die Liste in ein Array für die ComboBox
        String[] sortedColorNames = sortedColorList.toArray(new String[0]);

        // Color combo box with sorted colors
        colorComboBox = new JComboBox<>(sortedColorNames);
        colorComboBox.setPreferredSize(new Dimension(120, functionField.getPreferredSize().height));

        // Default to "Zufällig"
        colorComboBox.setSelectedItem(ColorChooser.RANDOM_COLOR_OPTION);

        // Handle "Weitere..." option
        colorComboBox.addActionListener(e -> {
            if (colorComboBox.getSelectedItem() != null &&
                    colorComboBox.getSelectedItem().toString().equals("Weitere...")) {

                debug("Benutzerdefinierte Farbauswahl geöffnet");
                // Zeige den Farbauswahl-Dialog
                Color selectedColor = ColorChooser.showColorChooser(
                        plotter,
                        "Benutzerdefinierte Farbe wählen",
                        Color.RED);

                if (selectedColor != null) {
                    // Farbname ermitteln oder neu erstellen
                    String colorName = ColorChooser.getColorName(selectedColor);
                    debug("Benutzerdefinierte Farbe gewählt: " + colorName);

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
                        debug("Neue Farbe zur Auswahlliste hinzugefügt: " + colorName);
                    }
                } else {
                    // Falls abgebrochen, zurück zur "Zufällig"-Option
                    colorComboBox.setSelectedItem(ColorChooser.RANDOM_COLOR_OPTION);
                    debug("Farbauswahl abgebrochen, zurück zu 'Zufällig'");
                }
            }
        });

        // Function list with checkboxes
        functionListModel = new DefaultListModel<>();
        functionList = new JList<>(functionListModel) {
            @Override
            public String getToolTipText(MouseEvent evt) {
                // Provide a tooltip with the function expression
                int index = locationToIndex(evt.getPoint());
                if (index != -1) {
                    String item = getModel().getElementAt(index);
                    // Extract the function expression
                    Matcher matcher = functionPattern.matcher(item);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    return item;
                }
                return null;
            }
        };
        functionList.setCellRenderer(new FunctionListCellRenderer());
        functionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Kontextmenü für die Funktionsliste
        setupContextMenu();

        // Mauslistener für Doppelklick, Kontextmenü und Checkbox-Klicks
        functionList.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = functionList.locationToIndex(e.getPoint());
                if (index < 0)
                    return;

                Rectangle checkBoxBounds = new Rectangle(0, 0, 20, functionList.getCellBounds(index, index).height);
                checkBoxBounds.y = functionList.getCellBounds(index, index).y;

                // Check if click was on the checkbox area
                if (checkBoxBounds.contains(e.getPoint())) {
                    // Toggle visibility
                    toggleFunctionVisibility(index);
                    functionList.repaint();
                    return;
                }

                if (e.getClickCount() == 2) {
                    // Doppelklick - Bearbeitungsdialog öffnen
                    debug("Doppelklick auf Funktion - öffne Bearbeitungsdialog");
                    openFunctionEditDialog();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // Rechtsklick - Kontextmenü anzeigen
                    functionList.setSelectedIndex(index);
                    debug("Rechtsklick auf Funktion #" + (index + 1) + " - zeige Kontextmenü");
                    functionPopup.show(functionList, e.getX(), e.getY());
                }
            }
        });

        functionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // Hole die ausgewählten Indizes aus der JList
                int[] selectedIndices = functionList.getSelectedIndices();

                // Aktualisiere die Auswahl im GraphPanel
                GraphPanel graphPanel = plotter.getGraphPanel();
                graphPanel.clearFunctionSelection();

                for (int index : selectedIndices) {
                    graphPanel.selectFunction(index, true);
                }

                // Debug-Meldung
                if (selectedIndices.length > 0) {
                    debug("Funktionen ausgewählt: " + Arrays.toString(selectedIndices));
                } else {
                    debug("Keine Funktionen ausgewählt");
                }
            }
        });

        // KeyListener für Funktionsliste - Entf zum Löschen, Enter zum Bearbeiten
        functionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    debug("Entf-Taste gedrückt - entferne ausgewählte Funktion");
                    removeSelectedFunction();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    debug("Enter-Taste gedrückt - bearbeite ausgewählte Funktion");
                    openFunctionEditDialog();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    debug("Leertaste gedrückt - ändere Sichtbarkeit der ausgewählten Funktion");
                    int selectedIndex = functionList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        toggleFunctionVisibility(selectedIndex);
                    }
                }
            }
        });
    }

    /**
     * Ändert die Sichtbarkeit einer Funktion (ein-/ausblenden)
     */
    private void toggleFunctionVisibility(int index) {
        if (index < 0 || index >= functionListModel.size())
            return;

        GraphPanel graphPanel = plotter.getGraphPanel();
        List<FunctionRenderer.FunctionInfo> functions = graphPanel.getFunctionRenderer().getFunctions();

        if (index < functions.size()) {
            // Toggle visibility in the model
            functions.get(index).toggleVisibility();

            // Update the list display
            String item = functionListModel.getElementAt(index);

            // Check if item already has visibility marker
            Matcher matcher = visibilityPattern.matcher(item);
            if (matcher.find()) {
                // Item already has visibility marker, update it
                boolean isCurrentlyVisible = matcher.group(1).equals("[x]");
                String newPrefix = isCurrentlyVisible ? "[ ] " : "[x] ";
                String restOfItem = matcher.group(2);
                functionListModel.set(index, newPrefix + restOfItem);
            } else {
                // Add visibility marker
                boolean isVisible = functions.get(index).isVisible();
                String prefix = isVisible ? "[x] " : "[ ] ";
                functionListModel.set(index, prefix + item);
            }

            // Repaint the graph
            graphPanel.repaint();
            debug("Sichtbarkeit der Funktion #" + (index + 1) + " geändert: " +
                    (functions.get(index).isVisible() ? "sichtbar" : "ausgeblendet"));
        }
    }

    /**
     * Helper-Methode zum Loggen von Debug-Informationen über PlotterPanel
     */
    private void debug(String message) {
        if (plotter != null) {
            plotter.debug(message);
        }
    }

    /**
     * Erstellt das Kontextmenü für die Funktionsliste
     */
    private void setupContextMenu() {
        functionPopup = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Bearbeiten");
        editItem.addActionListener(e -> {
            debug("Kontextmenü: Bearbeiten ausgewählt");
            openFunctionEditDialog();
        });

        JMenuItem removeItem = new JMenuItem("Entfernen");
        removeItem.addActionListener(e -> {
            debug("Kontextmenü: Entfernen ausgewählt");
            removeSelectedFunction();
        });

        JMenuItem toggleVisibilityItem = new JMenuItem("Sichtbarkeit umschalten");
        toggleVisibilityItem.addActionListener(e -> {
            debug("Kontextmenü: Sichtbarkeit umschalten ausgewählt");
            int selectedIndex = functionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                toggleFunctionVisibility(selectedIndex);
            }
        });

        functionPopup.add(editItem);
        functionPopup.add(toggleVisibilityItem);
        functionPopup.addSeparator();
        functionPopup.add(removeItem);
    }

    /**
     * Öffnet den Dialog zum Bearbeiten einer Funktion
     */
    private void openFunctionEditDialog() {
        int selectedIndex = functionList.getSelectedIndex();
        if (selectedIndex < 0)
            return;

        String entry = functionListModel.getElementAt(selectedIndex);
        debug("Bearbeite Funktion: " + entry);

        // Entferne ggf. Sichtbarkeitsmarkierung
        Matcher visibilityMatcher = visibilityPattern.matcher(entry);
        if (visibilityMatcher.find()) {
            entry = visibilityMatcher.group(2);
        }

        // Extrahiere Funktionsformel und Farbe
        Matcher matcher = functionPattern.matcher(entry);
        if (matcher.find()) {
            String function = matcher.group(1);
            String colorName = matcher.group(2);
            debug("Extrahierte Funktion: '" + function + "' mit Farbe: '" + colorName + "'");

            // Aktuelle Farbe ermitteln
            Color currentColor = ColorChooser.getColorByName(colorName);

            // Finde das Hauptfenster als Parent für den Dialog
            Frame parentFrame = JOptionPane.getFrameForComponent(plotter);

            // Zeige den Bearbeitungsdialog
            FunctionEditDialog.FunctionEditResult result = FunctionEditDialog.showDialog(
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

                // Neue Funktion mit geänderter Farbe erstellen
                String newEntry = "f(x) = " + newFunction + " [" + newColorName + "]";

                // Sichtbarkeit beibehalten
                boolean isVisible = true;
                if (visibilityMatcher.find()) {
                    isVisible = visibilityMatcher.group(1).equals("[x]");
                } else {
                    // Prüfe Sichtbarkeit aus dem Funktions-Renderer
                    isVisible = plotter.getGraphPanel().getFunctionRenderer().getFunctions()
                            .get(selectedIndex).isVisible();
                }

                String prefix = isVisible ? "[x] " : "[ ] ";
                newEntry = prefix + newEntry;

                // Aktualisiere Eintrag in der Liste
                functionListModel.set(selectedIndex, newEntry);
                debug("Listeneintrag aktualisiert: " + newEntry);

                // Graph aktualisieren
                plotter.updateGraphFromList();

                // Update intersection list if visible
                if (plotter.isShowingIntersections()) {
                    plotter.updateIntersectionList();
                }
            } else {
                debug("Bearbeitung abgebrochen");
            }
        }
    }

    /**
     * Entfernt die ausgewählte Funktion
     */
    private void removeSelectedFunction() {
        int selectedIndex = functionList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String removedFunction = functionListModel.getElementAt(selectedIndex);
            debug("Entferne Funktion #" + (selectedIndex + 1) + ": " + removedFunction);

            functionListModel.remove(selectedIndex);
            plotter.updateGraphFromList();

            // Update intersection list if visible
            if (plotter.isShowingIntersections()) {
                plotter.updateIntersectionList();
            }
        }
    }

    /**
     * Creates the function input area with label and color selector
     */
    public JPanel createFunctionInputPanel() {
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel functionLabel = new JLabel("f(x) = ");
        functionPanel.add(functionLabel, BorderLayout.WEST);
        functionPanel.add(functionField, BorderLayout.CENTER);
        functionPanel.add(colorComboBox, BorderLayout.EAST);

        return functionPanel;
    }

    /**
     * Creates the action button panel (add, remove, clear)
     */
    public JPanel createActionButtonPanel() {
        // Verwende GridLayout für gleichmäßige Aufteilung der Buttons
        JPanel actionButtonPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        actionButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        JButton addButton = new JButton("Hinzufügen");
        JButton removeButton = new JButton("Entfernen");
        JButton clearButton = new JButton("Alle löschen");
        JButton combineButton = new JButton("Kombinieren");

        // Add button action
        addButton.addActionListener(e -> {
            debug("'Hinzufügen'-Button geklickt");
            addFunction();
        });

        // Remove button action
        removeButton.addActionListener(e -> {
            debug("'Entfernen'-Button geklickt");
            removeSelectedFunction();
        });

        // Clear button action
        clearButton.addActionListener(e -> {
            debug("'Alle löschen'-Button geklickt");
            functionListModel.clear();
            plotter.getGraphPanel().clearFunctions();

            // Update intersection list if visible
            if (plotter.isShowingIntersections()) {
                plotter.updateIntersectionList();
            }
        });

        combineButton.addActionListener(e -> {
            debug("'Kombinieren'-Button geklickt");
            plotter.combineSelectedFunctions();
        });

        // Enter key in function field adds the function
        functionField.addActionListener(e -> {
            debug("Enter in Funktionsfeld gedrückt");
            addFunction();
        });

        actionButtonPanel.add(addButton);
        actionButtonPanel.add(removeButton);
        actionButtonPanel.add(combineButton);
        actionButtonPanel.add(clearButton);

        return actionButtonPanel;
    }

    /**
     * Creates the function list panel
     */
    public JPanel createFunctionListPanel() {
        JPanel functionsPanel = new JPanel(new BorderLayout(5, 5));
        functionsPanel.setBorder(BorderFactory.createTitledBorder("Funktionen"));

        // Function list with scrollable area
        JScrollPane listScrollPane = new JScrollPane(functionList);

        // Optimierte Größenkonfiguration für Spalten-Layout
        listScrollPane.setMinimumSize(new Dimension(100, 100));
        listScrollPane.setPreferredSize(new Dimension(150, 150));

        // Wir setzen das bevorzugte Verhalten, wie sich die Liste bei Größenänderung
        // verhält
        functionList.setVisibleRowCount(5); // Mindestens 5 Zeilen anzeigen

        // Scrollbars nur bei Bedarf anzeigen
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        functionsPanel.add(listScrollPane, BorderLayout.CENTER);

        return functionsPanel;
    }

    public void selectFunction(int index) {
        if (index >= 0 && index < functionList.getModel().getSize()) {
            functionList.setSelectedIndex(index);
            functionList.ensureIndexIsVisible(index);
        }
    }

    /**
     * Adds a function to the list and graph
     */
    private void addFunction() {
        String func = functionField.getText().trim();
        if (!func.isEmpty()) {
            debug("Füge neue Funktion hinzu: " + func);
            try {
                // Get selected color name
                String colorName = (String) colorComboBox.getSelectedItem();
                debug("Gewählte Farboption: " + colorName);

                // Wenn "Weitere..." ausgewählt ist, standardmäßig "Zufällig" verwenden
                if (colorName.equals("Weitere...")) {
                    colorName = ColorChooser.RANDOM_COLOR_OPTION;
                    debug("'Weitere...' ausgewählt, verwende 'Zufällig'");
                }

                // Die tatsächliche Farbe bestimmen
                Color color;

                // Bei "Zufällig" eine zufällige Farbe erzeugen und den konkreten Farbnamen
                // speichern, nicht den Platzhalter "Zufällig"
                if (colorName.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
                    color = ColorChooser.generateRandomColor();
                    colorName = ColorChooser.getColorName(color);
                    debug("Zufällige Farbe generiert: " + colorName);
                } else {
                    color = ColorChooser.getColorByName(colorName);
                    debug("Vorgegebene Farbe verwendet: " + colorName);
                }

                // Add to function list - wir speichern den konkreten Farbnamen, nicht
                // "Zufällig"
                String listEntry = "f(x) = " + func + " [" + colorName + "]";
                // Neue Funktionen sind standardmäßig sichtbar
                listEntry = "[x] " + listEntry;
                functionListModel.addElement(listEntry);
                debug("Funktion zur Liste hinzugefügt: " + listEntry);

                // Update graph
                plotter.updateGraphFromList();

                // Clear input field
                functionField.setText("");
                debug("Funktionsfeld geleert");

            } catch (Exception ex) {
                debug("Fehler beim Hinzufügen der Funktion: " + ex.getMessage());
                JOptionPane.showMessageDialog(plotter,
                        "Fehler in der Funktion: " + ex.getMessage(),
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            debug("Leeres Funktionsfeld - nichts hinzugefügt");
        }
    }

    /**
     * Returns the function list model
     */
    public DefaultListModel<String> getFunctionListModel() {
        return functionListModel;
    }

    /**
     * Returns the available colors
     */
    public Color[] getAvailableColors() {
        return ColorChooser.getColors();
    }

    /**
     * Returns the color names
     */
    public String[] getColorNames() {
        return ColorChooser.getColorNames();
    }

    /**
     * Benutzerdefinierter CellRenderer für die Funktionsliste mit Checkboxen
     */
    private class FunctionListCellRenderer extends DefaultListCellRenderer {
        private JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected,
                boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = value.toString();

            // Prüfe auf Sichtbarkeitsmarkierung
            Matcher matcher = visibilityPattern.matcher(text);
            if (matcher.find()) {
                // Entferne Markierung aus angezeigtem Text
                checkbox.setSelected(matcher.group(1).equals("[x]"));
                label.setText(matcher.group(2));
            } else {
                // Keine Markierung, Funktion ist standardmäßig sichtbar
                checkbox.setSelected(true);
                label.setText(text);
            }

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
    }
}