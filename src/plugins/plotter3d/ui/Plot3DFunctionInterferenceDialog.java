package plugins.plotter3d.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import common.ColorChooser;
import plugins.plotter3d.Plot3DPanel;

/**
 * Dialog zum Erstellen von Interferenz-Funktionen (Kombinationen mehrerer
 * Funktionen) für den 3D-Plotter
 */
public class Plot3DFunctionInterferenceDialog extends JDialog {
    private final Plot3DPanel plotter;
    private final List<Integer> selectedIndices;
    private JComboBox<String> operationComboBox;
    private JTextField customExpressionField;
    private JTextField nameField;
    private JComboBox<String> colorComboBox;
    private boolean confirmed = false;

    // Verfügbare Operationen
    private static final String[] OPERATIONS = {
            "Summe (f₁ + f₂ + ...)",
            "Differenz (f₁ - f₂ - ...)",
            "Produkt (f₁ * f₂ * ...)",
            "Quotient (f₁ / f₂ / ...)",
            "Maximum (größerer Wert aus f₁, f₂, ...)",
            "Minimum (kleinerer Wert aus f₁, f₂, ...)",
            "Durchschnitt ((f₁ + f₂ + ...) / n)",
            "Benutzerdefiniert..."
    };

    /**
     * Erstellt einen neuen Dialog zum Erstellen von Interferenz-Funktionen
     * 
     * @param parent          Der Eltern-Frame
     * @param plotter         Der Plot3DPanel
     * @param selectedIndices Die Indizes der ausgewählten Funktionen
     */
    public Plot3DFunctionInterferenceDialog(Frame parent, Plot3DPanel plotter, List<Integer> selectedIndices) {
        super(parent, "Funktionen kombinieren", true);
        this.plotter = plotter;
        this.selectedIndices = new ArrayList<>(selectedIndices);

        initComponents();
        setupLayout();

        // Fenstergröße und Position
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Initialisiert die Komponenten des Dialogs
     */
    private void initComponents() {
        // Operation auswählen
        operationComboBox = new JComboBox<>(OPERATIONS);
        operationComboBox.addActionListener(e -> {
            // Benutzerdefiniertes Feld nur aktivieren, wenn "Benutzerdefiniert..."
            // ausgewählt ist
            String selected = (String) operationComboBox.getSelectedItem();
            boolean isCustom = selected.equals("Benutzerdefiniert...");
            customExpressionField.setEnabled(isCustom);
            if (isCustom) {
                customExpressionField.requestFocus();
            }
        });

        // Feld für benutzerdefinierten Ausdruck
        customExpressionField = new JTextField(20);
        customExpressionField.setEnabled(false);
        customExpressionField.setToolTipText("Verwende f₁, f₂, ... für die ausgewählten Funktionen");

        // Feld für den Namen der neuen Funktion
        nameField = new JTextField("Kombination", 15);

        // Farbauswahl
        String[] colorNames = ColorChooser.getColorNames();
        colorComboBox = new JComboBox<>(colorNames);
        // Standardmäßig "Zufällig" auswählen
        for (int i = 0; i < colorNames.length; i++) {
            if (colorNames[i].equals(ColorChooser.RANDOM_COLOR_OPTION)) {
                colorComboBox.setSelectedIndex(i);
                break;
            }
        }

        // OK-Button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        // Abbrechen-Button
        JButton cancelButton = new JButton("Abbrechen");
        cancelButton.addActionListener(e -> dispose());

        // ESC-Taste zum Schließen
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // ENTER-Taste zum Bestätigen
        getRootPane().registerKeyboardAction(
                e -> okButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Standard-Button festlegen
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Überprüft die Eingaben auf Gültigkeit
     */
    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte geben Sie einen Namen für die neue Funktion ein.",
                    "Eingabefehler", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }

        String selectedOperation = (String) operationComboBox.getSelectedItem();
        if (selectedOperation.equals("Benutzerdefiniert...")) {
            String expression = customExpressionField.getText().trim();
            if (expression.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte geben Sie einen benutzerdefinierten Ausdruck ein.",
                        "Eingabefehler", JOptionPane.ERROR_MESSAGE);
                customExpressionField.requestFocus();
                return false;
            }
        }

        return true;
    }

    /**
     * Setzt das Layout des Dialogs
     */
    private void setupLayout() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Ausgewählte Funktionen anzeigen
        JPanel selectedFunctionsPanel = new JPanel(new BorderLayout());
        selectedFunctionsPanel.setBorder(BorderFactory.createTitledBorder("Ausgewählte Funktionen"));

        JTextArea selectedFunctionsArea = new JTextArea(5, 30);
        selectedFunctionsArea.setEditable(false);
        selectedFunctionsArea.setLineWrap(true);
        selectedFunctionsArea.setWrapStyleWord(true);

        // Funktionen auflisten
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedIndices.size(); i++) {
            int index = selectedIndices.get(i);
            String functionName = getFunctionName(index);
            sb.append("f").append(i + 1).append(" = ").append(functionName);
            if (i < selectedIndices.size() - 1) {
                sb.append("\n");
            }
        }
        selectedFunctionsArea.setText(sb.toString());

        JScrollPane scrollPane = new JScrollPane(selectedFunctionsArea);
        selectedFunctionsPanel.add(scrollPane, BorderLayout.CENTER);

        // Operationen und Eingabefelder
        JPanel operationsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Operation
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        operationsPanel.add(new JLabel("Operation:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        operationsPanel.add(operationComboBox, gbc);

        // Benutzerdefinierter Ausdruck
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        operationsPanel.add(new JLabel("Eigener Ausdruck:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        operationsPanel.add(customExpressionField, gbc);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        operationsPanel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        operationsPanel.add(nameField, gbc);

        // Farbe
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        operationsPanel.add(new JLabel("Farbe:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        operationsPanel.add(colorComboBox, gbc);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(new JButton("OK") {
            {
                addActionListener(e -> {
                    if (validateInput()) {
                        confirmed = true;
                        dispose();
                    }
                });
            }
        });
        buttonPanel.add(new JButton("Abbrechen") {
            {
                addActionListener(e -> dispose());
            }
        });

        // Alles zusammenfügen
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(selectedFunctionsPanel, BorderLayout.NORTH);
        mainPanel.add(operationsPanel, BorderLayout.CENTER);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    /**
     * Gibt den Namen einer Funktion basierend auf dem Index zurück
     */
    private String getFunctionName(int index) {
        DefaultListModel<String> listModel = plotter.getFunctionManager().getFunctionListModel();
        if (index >= 0 && index < listModel.size()) {
            String entry = listModel.get(index);
            int equalsPos = entry.indexOf('=');
            int bracketPos = entry.lastIndexOf('[');

            if (equalsPos >= 0 && bracketPos > equalsPos) {
                return entry.substring(equalsPos + 1, bracketPos).trim();
            }
        }
        return "f" + (index + 1);
    }

    /**
     * Erstellt den Funktionsausdruck basierend auf der ausgewählten Operation
     */
    public String createFunctionExpression() {
        if (!confirmed)
            return null;

        String selectedOperation = (String) operationComboBox.getSelectedItem();
        List<String> functionExpressions = new ArrayList<>();

        // Sammle alle Funktionsausdrücke
        for (int i = 0; i < selectedIndices.size(); i++) {
            int index = selectedIndices.get(i);
            String expression = getFunctionExpressionByIndex(index);
            functionExpressions.add(expression);
        }

        // Wende die ausgewählte Operation an
        if (selectedOperation.startsWith("Summe")) {
            return joinWithOperator(functionExpressions, "+");
        } else if (selectedOperation.startsWith("Differenz")) {
            return joinWithOperator(functionExpressions, "-");
        } else if (selectedOperation.startsWith("Produkt")) {
            return joinWithOperator(functionExpressions, "*");
        } else if (selectedOperation.startsWith("Quotient")) {
            return joinWithOperator(functionExpressions, "/");
        } else if (selectedOperation.startsWith("Maximum")) {
            return createMaxExpression(functionExpressions);
        } else if (selectedOperation.startsWith("Minimum")) {
            return createMinExpression(functionExpressions);
        } else if (selectedOperation.startsWith("Durchschnitt")) {
            return createAverageExpression(functionExpressions);
        } else {
            // Benutzerdefinierter Ausdruck
            String customExpression = customExpressionField.getText().trim();

            // Ersetze f₁, f₂, ... durch die tatsächlichen Funktionsausdrücke
            for (int i = 0; i < functionExpressions.size(); i++) {
                String placeholder = "f" + (i + 1);
                customExpression = customExpression.replace(placeholder, "(" + functionExpressions.get(i) + ")");
            }

            return customExpression;
        }
    }

    /**
     * Extrahiert den Funktionsausdruck aus einem Listeneintrag
     */
    private String getFunctionExpressionByIndex(int index) {
        DefaultListModel<String> listModel = plotter.getFunctionManager().getFunctionListModel();

        if (index < 0 || index >= listModel.size()) {
            return "f" + (index + 1);
        }

        String entry = listModel.get(index);

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
     * Verbindet Funktionsausdrücke mit einem Operator
     */
    private String joinWithOperator(List<String> expressions, String operator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            sb.append("(").append(expressions.get(i)).append(")");
            if (i < expressions.size() - 1) {
                sb.append(" ").append(operator).append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Erstellt einen Ausdruck für das Maximum mehrerer Funktionen
     * verwendet arithmetische Ausdrücke statt ternärer Operatoren
     */
    private String createMaxExpression(List<String> expressions) {
        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        StringBuilder sb = new StringBuilder();

        // Für zwei Funktionen: (a + b + abs(a - b)) / 2
        if (expressions.size() == 2) {
            String a = expressions.get(0);
            String b = expressions.get(1);
            sb.append("((").append(a).append(") + (").append(b).append(") + abs((").append(a).append(") - (").append(b)
                    .append("))) / 2");
            return sb.toString();
        }

        // Für mehr als zwei Funktionen rekursiv vorgehen
        String first = expressions.get(0);
        List<String> rest = expressions.subList(1, expressions.size());
        String maxOfRest = createMaxExpression(rest);

        sb.append("((").append(first).append(") + (").append(maxOfRest).append(") + abs((").append(first)
                .append(") - (").append(maxOfRest).append("))) / 2");
        return sb.toString();
    }

    /**
     * Erstellt einen Ausdruck für das Minimum mehrerer Funktionen
     * verwendet arithmetische Ausdrücke statt ternärer Operatoren
     */
    private String createMinExpression(List<String> expressions) {
        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        StringBuilder sb = new StringBuilder();

        // Für zwei Funktionen: (a + b - abs(a - b)) / 2
        if (expressions.size() == 2) {
            String a = expressions.get(0);
            String b = expressions.get(1);
            sb.append("((").append(a).append(") + (").append(b).append(") - abs((").append(a).append(") - (").append(b)
                    .append("))) / 2");
            return sb.toString();
        }

        // Für mehr als zwei Funktionen rekursiv vorgehen
        String first = expressions.get(0);
        List<String> rest = expressions.subList(1, expressions.size());
        String minOfRest = createMinExpression(rest);

        sb.append("((").append(first).append(") + (").append(minOfRest).append(") - abs((").append(first)
                .append(") - (").append(minOfRest).append("))) / 2");
        return sb.toString();
    }

    /**
     * Erstellt einen Ausdruck für den Durchschnitt mehrerer Funktionen
     */
    private String createAverageExpression(List<String> expressions) {
        if (expressions.isEmpty()) {
            return "0";
        }

        // Erstelle den Summenausdruck
        String sumExpression = joinWithOperator(expressions, "+");

        // Teile durch die Anzahl der Funktionen
        return "(" + sumExpression + ")/" + expressions.size();
    }

    /**
     * Gibt den Namen für die neue Funktion zurück
     */
    public String getFunctionName() {
        return nameField.getText().trim();
    }

    /**
     * Gibt die ausgewählte Farbe zurück
     */
    public Color getSelectedColor() {
        String colorName = (String) colorComboBox.getSelectedItem();
        if (colorName.equals(ColorChooser.RANDOM_COLOR_OPTION)) {
            return ColorChooser.generateRandomColor();
        } else {
            return ColorChooser.getColorByName(colorName);
        }
    }

    /**
     * Gibt zurück, ob der Dialog bestätigt wurde
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Öffnet den Dialog und gibt zurück, ob der Benutzer ihn bestätigt hat
     */
    public static Plot3DFunctionInterferenceDialog showDialog(JFrame parent, Plot3DPanel plotter,
            List<Integer> selectedIndices) {
        Plot3DFunctionInterferenceDialog dialog = new Plot3DFunctionInterferenceDialog(parent, plotter,
                selectedIndices);
        dialog.setVisible(true);
        return dialog;
    }
}