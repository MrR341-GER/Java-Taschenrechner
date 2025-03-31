package plugins.plotter2d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import common.ColorChooser;

/**
 * Dialog zum Erstellen von Interferenz-Funktionen (Kombinationen mehrerer Funktionen)
 */
public class FunctionInterferenceDialog extends JDialog {
    private final PlotterPanel plotter;
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
        "Maximum (max(f₁, f₂, ...))",
        "Minimum (min(f₁, f₂, ...))",
        "Benutzerdefiniert..."
    };
    
    /**
     * Erstellt einen neuen Dialog zum Erstellen von Interferenz-Funktionen
     * 
     * @param parent Der Eltern-Frame
     * @param plotter Der PlotterPanel
     * @param selectedIndices Die Indizes der ausgewählten Funktionen
     */
    public FunctionInterferenceDialog(Frame parent, PlotterPanel plotter, List<Integer> selectedIndices) {
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
            // Benutzerdefiniertes Feld nur aktivieren, wenn "Benutzerdefiniert..." ausgewählt ist
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
        buttonPanel.add(new JButton("OK") {{
            addActionListener(e -> {
                if (validateInput()) {
                    confirmed = true;
                    dispose();
                }
            });
        }});
        buttonPanel.add(new JButton("Abbrechen") {{
            addActionListener(e -> dispose());
        }});
        
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
        DefaultListModel<String> listModel = plotter.getFunctionInputPanel().getFunctionListModel();
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
        if (!confirmed) return null;
        
        String operation = (String) operationComboBox.getSelectedItem();
        List<String> functionExpressions = new ArrayList<>();
        
        // Funktionsausdrücke sammeln
        for (int index : selectedIndices) {
            functionExpressions.add(getFunctionName(index));
        }
        
        if (operation.equals("Benutzerdefiniert...")) {
            // Bei benutzerdefiniertem Ausdruck: f₁, f₂, ... durch die tatsächlichen Funktionen ersetzen
            String expression = customExpressionField.getText().trim();
            for (int i = 0; i < functionExpressions.size(); i++) {
                expression = expression.replace("f" + (i + 1), "(" + functionExpressions.get(i) + ")");
            }
            return expression;
        } else {
            // Bei vordefinierten Operationen: Operation auf die Funktionen anwenden
            switch (operation) {
                case "Summe (f₁ + f₂ + ...)":
                    return joinWithOperator(functionExpressions, "+");
                case "Differenz (f₁ - f₂ - ...)":
                    return joinWithOperator(functionExpressions, "-");
                case "Produkt (f₁ * f₂ * ...)":
                    return joinWithOperator(functionExpressions, "*");
                case "Quotient (f₁ / f₂ / ...)":
                    return joinWithOperator(functionExpressions, "/");
                case "Maximum (max(f₁, f₂, ...))":
                    return "max(" + joinFunctions(functionExpressions, ",") + ")";
                case "Minimum (min(f₁, f₂, ...))":
                    return "min(" + joinFunctions(functionExpressions, ",") + ")";
                default:
                    return functionExpressions.get(0); // Fallback
            }
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
     * Verbindet Funktionsausdrücke mit einem Trennzeichen
     */
    private String joinFunctions(List<String> expressions, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            sb.append("(").append(expressions.get(i)).append(")");
            if (i < expressions.size() - 1) {
                sb.append(delimiter).append(" ");
            }
        }
        return sb.toString();
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
    public static FunctionInterferenceDialog showDialog(JFrame parent, PlotterPanel plotter, List<Integer> selectedIndices) {
        FunctionInterferenceDialog dialog = new FunctionInterferenceDialog(parent, plotter, selectedIndices);
        dialog.setVisible(true);
        return dialog;
    }
}
