import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

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

    // Available colors with names
    private final Color[] availableColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
            Color.ORANGE, Color.CYAN, new Color(128, 0, 128), // Purple
            new Color(165, 42, 42) // Brown
    };

    private final String[] colorNames = {
            "Rot", "Blau", "Grün", "Magenta",
            "Orange", "Cyan", "Lila", "Braun"
    };

    /**
     * Creates a new function input panel
     */
    public FunctionInputPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Function input field
        functionField = new JTextField();
        functionField.setToolTipText("Funktion eingeben, z.B. sin(x) oder x^2");

        // Color combo box
        colorComboBox = new JComboBox<>(colorNames);
        colorComboBox.setPreferredSize(new Dimension(100, functionField.getPreferredSize().height));

        // Function list
        functionListModel = new DefaultListModel<>();
        functionList = new JList<>(functionListModel);
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
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton addButton = new JButton("Hinzufügen");
        JButton removeButton = new JButton("Entfernen");
        JButton clearButton = new JButton("Alle löschen");

        // Add button action
        addButton.addActionListener(e -> addFunction());

        // Remove button action
        removeButton.addActionListener(e -> {
            int selectedIndex = functionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                functionListModel.remove(selectedIndex);
                plotter.updateGraphFromList();

                // Update intersection list if visible
                if (plotter.isShowingIntersections()) {
                    plotter.updateIntersectionList();
                }
            }
        });

        // Clear button action
        clearButton.addActionListener(e -> {
            functionListModel.clear();
            plotter.getGraphPanel().clearFunctions();

            // Update intersection list if visible
            if (plotter.isShowingIntersections()) {
                plotter.updateIntersectionList();
            }
        });

        // Enter key in function field adds the function
        functionField.addActionListener(e -> addFunction());

        actionButtonPanel.add(addButton);
        actionButtonPanel.add(removeButton);
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
        listScrollPane.setPreferredSize(new Dimension(300, 100));
        listScrollPane.setMinimumSize(new Dimension(100, 50));
        functionsPanel.add(listScrollPane, BorderLayout.CENTER);

        return functionsPanel;
    }

    /**
     * Adds a function to the list and graph
     */
    private void addFunction() {
        String func = functionField.getText().trim();
        if (!func.isEmpty()) {
            try {
                // Select color
                int colorIndex = colorComboBox.getSelectedIndex();
                Color color = availableColors[colorIndex % availableColors.length];

                // Add to function list
                String listEntry = "f(x) = " + func + " [" + colorNames[colorIndex] + "]";
                functionListModel.addElement(listEntry);

                // Update graph
                plotter.updateGraphFromList();

                // Clear input field
                functionField.setText("");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(plotter,
                        "Fehler in der Funktion: " + ex.getMessage(),
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
            }
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
        return availableColors;
    }

    /**
     * Returns the color names
     */
    public String[] getColorNames() {
        return colorNames;
    }
}
