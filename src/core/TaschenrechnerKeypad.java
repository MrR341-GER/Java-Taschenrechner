package core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Erstellt und verwaltet das erweiterte Tastenfeld des Taschenrechners mit
 * erweiterten Funktionen
 */
public class TaschenrechnerKeypad {
    // Konstanten für den Textpuffer (zusätzlicher Platz um den Text)
    private static final int TEXT_PADDING_HORIZONTAL = 16;
    private static final int TEXT_PADDING_VERTICAL = 10;
    private static final int MINIMUM_BUTTON_WIDTH = 40;
    private static final int MINIMUM_BUTTON_HEIGHT = 35;

    private final Taschenrechner calculator;
    private final ActionListener inputHandler;

    public TaschenrechnerKeypad(Taschenrechner calculator, ActionListener inputHandler) {
        this.calculator = calculator;
        this.inputHandler = inputHandler;
    }

    /**
     * Erstellt das komplette Tastenfeld-Panel
     */
    public JPanel createKeypadPanel() {
        // Haupttastenfeld-Panel mit BorderLayout
        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));

        // Steuerungstasten
        JButton debugButton = new JButton("Debug");
        debugButton.setFont(new Font("Arial", Font.PLAIN, 16));
        debugButton.setBackground(new Color(255, 200, 200)); // Hellrot
        adjustButtonSize(debugButton);
        debugButton.addActionListener(e -> calculator.toggleDebug());

        JButton historyButton = new JButton("History");
        historyButton.setFont(new Font("Arial", Font.PLAIN, 16));
        historyButton.setBackground(new Color(200, 230, 255)); // Hellblau
        adjustButtonSize(historyButton);
        historyButton.addActionListener(e -> calculator.toggleHistory());

        // Panel für Steuerungstasten
        JPanel controlButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlButtonPanel.add(debugButton);
        controlButtonPanel.add(historyButton);

        // Füge die Info-Taste für Funktionen hinzu
        JButton functionsInfoButton = new JButton("Funktionen Info");
        functionsInfoButton.setFont(new Font("Arial", Font.PLAIN, 16));
        functionsInfoButton.setBackground(new Color(220, 255, 220)); // Hellgrün
        adjustButtonSize(functionsInfoButton);
        functionsInfoButton.addActionListener(e -> showFunctionsInfo());
        controlButtonPanel.add(functionsInfoButton);

        // Panels für verschiedene Tasten-Gruppen mit festem Abstand
        JPanel ziffernPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        JPanel operatorenPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        JPanel funktionsPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JPanel wissenschaftlichPanel = new JPanel(new GridLayout(2, 6, 5, 5)); // Vergrößert auf 2 Zeilen

        // Zifferntasten (0-9 und .)
        String[] ziffern = { "7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "+/-" };
        for (String ziffer : ziffern) {
            JButton button = new JButton(ziffer);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            ziffernPanel.add(button);
        }

        // Operatoren
        String[] operatoren = { "/", "*", "-", "+", "=" };
        for (String operator : operatoren) {
            JButton button = new JButton(operator);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Helllila
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            operatorenPanel.add(button);
        }

        // Funktionstasten
        String[] funktionen = { "C", "(", ")", "←" };
        for (String funktion : funktionen) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            adjustButtonSize(button);

            // Spezielle Farben für Funktionstasten
            if (funktion.equals("C")) {
                button.setBackground(new Color(255, 200, 200)); // Hellrot
            } else if (funktion.equals("←")) {
                button.setBackground(new Color(255, 240, 200)); // Hellgelb
            } else {
                button.setBackground(new Color(200, 230, 255)); // Hellblau für Klammern
            }

            button.addActionListener(inputHandler);
            funktionsPanel.add(button);
        }

        // Wissenschaftliche Funktionen (erste Zeile)
        String[] wissenschaftlich1 = { "x²", "x³", "x^y", "√x", "³√x", "y√x" };
        for (String funktion : wissenschaftlich1) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Helllila
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            wissenschaftlichPanel.add(button);
        }

        // Zusätzliche wissenschaftliche Funktionen (zweite Zeile)
        String[] wissenschaftlich2 = { "sin", "cos", "tan", "log", "ln", "pi" };
        for (String funktion : wissenschaftlich2) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 250, 230)); // Hellgrün
            adjustButtonSize(button);
            // Spezielle Aktion für Konstanten wie pi, die direkt eingefügt werden
            if (funktion.equals("pi")) {
                button.addActionListener(e -> {
                    String aktuellerText = calculator.getDisplayText();
                    if (aktuellerText.equals("0")) {
                        calculator.setDisplayText("pi");
                    } else if (calculator.isNeueZahlBegonnen()) {
                        calculator.setDisplayText(aktuellerText + "pi");
                        calculator.setNeueZahlBegonnen(false);
                    } else {
                        calculator.setDisplayText(aktuellerText + "pi");
                    }
                });
            } else {
                // Für Funktionen wie sin, cos, etc. fügen wir den Funktionsnamen und eine
                // öffnende Klammer ein
                button.addActionListener(e -> {
                    String aktuellerText = calculator.getDisplayText();
                    if (aktuellerText.equals("0")) {
                        calculator.setDisplayText(funktion + "(");
                    } else {
                        calculator.setDisplayText(aktuellerText + funktion + "(");
                    }
                    calculator.setNeueZahlBegonnen(true);
                });
            }
            wissenschaftlichPanel.add(button);
        }

        // Tastenlayout zusammenstellen
        JPanel allButtonsPanel = new JPanel(new BorderLayout(5, 5));
        allButtonsPanel.add(funktionsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(ziffernPanel, BorderLayout.CENTER);
        centerPanel.add(operatorenPanel, BorderLayout.EAST);

        allButtonsPanel.add(centerPanel, BorderLayout.CENTER);
        allButtonsPanel.add(wissenschaftlichPanel, BorderLayout.SOUTH);

        // UI-Komponenten zusammenfügen
        buttonPanel.add(allButtonsPanel, BorderLayout.CENTER);
        buttonPanel.add(controlButtonPanel, BorderLayout.SOUTH);

        return buttonPanel;
    }

    /**
     * Zeigt ein Informationsfenster mit allen unterstützten Funktionen an
     */
    private void showFunctionsInfo() {
        JTextArea textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        textArea.setText(
                "Unterstützte Funktionen:\n" +
                        "--------------------\n" +
                        "sin(x), cos(x), tan(x) - Trigonometrische Grundfunktionen\n" +
                        "asin(x)/arcsin(x), acos(x)/arccos(x), atan(x)/arctan(x) - Inverse Trigonometrie\n" +
                        "sinh(x), cosh(x), tanh(x) - Hyperbolische Funktionen\n" +
                        "sqrt(x) - Quadratwurzel\n" +
                        "cbrt(x) - Kubikwurzel\n" +
                        "log(x) - Logarithmus zur Basis 10\n" +
                        "ln(x) - Natürlicher Logarithmus (Basis e)\n" +
                        "log2(x) - Logarithmus zur Basis 2\n" +
                        "abs(x) - Absolutwert\n" +
                        "exp(x) - Exponentialfunktion e^x\n" +
                        "floor(x) - Abrunden\n" +
                        "ceil(x)/ceiling(x) - Aufrunden\n" +
                        "round(x) - Kaufmännisches Runden\n" +
                        "degrees(x)/deg(x) - Konvertiert Radiant zu Grad\n" +
                        "radians(x)/rad(x) - Konvertiert Grad zu Radiant\n\n" +
                        "Unterstützte Konstanten:\n" +
                        "--------------------\n" +
                        "pi - Kreiszahl π (3.14159...)\n" +
                        "e - Eulersche Zahl (2.71828...)\n" +
                        "phi/golden - Goldener Schnitt (1.61803...)\n" +
                        "sqrt2 - Quadratwurzel von 2 (1.41421...)\n" +
                        "sqrt3 - Quadratwurzel von 3 (1.73205...)\n\n" +
                        "Hinweis: Implizite Multiplikation wie '2x' oder '3(x+1)' wird unterstützt.");

        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(calculator, scrollPane,
                "Unterstützte Mathematische Funktionen und Konstanten", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Hilfsmethode, um die Tastengröße dynamisch basierend auf dem Text anzupassen
     */
    public void adjustButtonSize(JButton button) {
        // Berechne die Textgröße
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(button.getText());
        int textHeight = metrics.getHeight();

        // Setze die minimale Tastengröße basierend auf der Textgröße plus Puffer
        int width = Math.max(textWidth + TEXT_PADDING_HORIZONTAL, MINIMUM_BUTTON_WIDTH);
        int height = Math.max(textHeight + TEXT_PADDING_VERTICAL, MINIMUM_BUTTON_HEIGHT);

        button.setMinimumSize(new Dimension(width, height));
        button.setPreferredSize(new Dimension(width, height));

        // Kleine Ränder für ein besseres Erscheinungsbild
        button.setMargin(new Insets(4, 4, 4, 4));

        // Debug-Ausgabe zur Überprüfung
        calculator.debug("Button '" + button.getText() + "' Größe angepasst: " + width + "x" + height +
                " (Text: " + textWidth + "x" + textHeight + ")");
    }
}
