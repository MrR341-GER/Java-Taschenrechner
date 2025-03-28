import javax.swing.*;
import java.awt.*;

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

        // Steuerungsbereich erstellen
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // Oberes Panel für Eingabe und Buttons
        JPanel topControlPanel = new JPanel(new BorderLayout(5, 5));

        // Funktionseingabefeld
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

        // Funktionsliste
        functionListModel = new DefaultListModel<>();
        functionList = new JList<>(functionListModel);
        JScrollPane listScrollPane = new JScrollPane(functionList);
        listScrollPane.setPreferredSize(new Dimension(300, 100));

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton addButton = new JButton("Hinzufügen");
        JButton removeButton = new JButton("Entfernen");
        JButton clearButton = new JButton("Alle löschen");
        JButton resetViewButton = new JButton("Ansicht zurücksetzen");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(resetViewButton);

        // Funktionsliste und Buttons zusammenfügen
        JPanel listAndButtonsPanel = new JPanel(new BorderLayout(5, 5));
        listAndButtonsPanel.add(listScrollPane, BorderLayout.CENTER);
        listAndButtonsPanel.add(buttonPanel, BorderLayout.SOUTH);
        listAndButtonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        // Event-Handling
        addButton.addActionListener(e -> addFunction());

        removeButton.addActionListener(e -> {
            int selectedIndex = functionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                functionListModel.remove(selectedIndex);
                updateGraphFromList();
            }
        });

        clearButton.addActionListener(e -> {
            functionListModel.clear();
            graphPanel.clearFunctions();
        });

        resetViewButton.addActionListener(e -> graphPanel.resetView());

        functionField.addActionListener(e -> addFunction());

        // Alles zusammenfügen
        topControlPanel.add(functionPanel, BorderLayout.NORTH);
        topControlPanel.add(listAndButtonsPanel, BorderLayout.CENTER);

        controlPanel.add(topControlPanel, BorderLayout.NORTH);

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

        controlPanel.add(examplesPanel, BorderLayout.SOUTH);

        // Haupt-Layout
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
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
    }
}