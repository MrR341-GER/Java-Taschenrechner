import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panel für den wissenschaftlichen Taschenrechner mit erweiterten Funktionen
 */
public class ScientificPanel extends JPanel {
    private final GrafischerTaschenrechner calculator;
    private JTextField displayField;
    private CalculationEngine calculationEngine;
    private JComboBox<String> angleUnitComboBox;

    // Status-Flags
    private boolean neueZahlBegonnen = true;
    private boolean shiftMode = false;
    private boolean memoryHasValue = false;
    private double memoryValue = 0.0;

    // Formatierung für Zahlenwerte
    private final DecimalFormat decimalFormat = new DecimalFormat("0.##########");

    // Debug-Referenz
    private DebugManager debugManager;

    // Muster zur Erkennung von Funktionen (enthält 'x' und ggf. weitere
    // mathematische Operationen)
    private final Pattern functionPattern = Pattern.compile(".*[a-zA-Z&&[^eE]].*");

    public ScientificPanel(GrafischerTaschenrechner calculator) {
        this.calculator = calculator;

        // Layout des Panels
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialisiere die CalculationEngine
        calculationEngine = new CalculationEngine(calculator);

        // UI-Komponenten erstellen
        createUI();
    }

    /**
     * Erstellt die Benutzeroberfläche des wissenschaftlichen Taschenrechners
     */
    private void createUI() {
        // Display-Bereich
        JPanel displayPanel = createDisplayPanel();
        add(displayPanel, BorderLayout.NORTH);

        // Tastatur-Bereich
        JPanel keypadPanel = createKeypadPanel();
        add(keypadPanel, BorderLayout.CENTER);
    }

    /**
     * Erstellt den Display-Bereich mit Eingabefeld und Statusanzeigen
     */
    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Eingabefeld
        displayField = new JTextField("0");
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setFont(new Font("Arial", Font.PLAIN, 24));
        displayField.setEditable(true);

        // Tastatur-Events für das Eingabefeld
        displayField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    calculateResult();
                }
            }
        });

        // Panel für Statusanzeigen (Winkeleinheit, Speicher, etc.)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Winkeleinheit-Auswahl
        JLabel angleLabel = new JLabel("Winkeleinheit:");
        angleUnitComboBox = new JComboBox<>(new String[] { "Radiant", "Grad" });
        angleUnitComboBox.addActionListener(e -> {
            debug("Winkeleinheit geändert: " + angleUnitComboBox.getSelectedItem());
        });

        // Speicher-Anzeige
        JLabel memoryLabel = new JLabel("M: ");
        JLabel memoryStatus = new JLabel("0");
        memoryStatus.setForeground(Color.GRAY);

        statusPanel.add(angleLabel);
        statusPanel.add(angleUnitComboBox);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(memoryLabel);
        statusPanel.add(memoryStatus);

        panel.add(displayField, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Erstellt das erweiterte Tastenfeld des wissenschaftlichen Taschenrechners
     */
    private JPanel createKeypadPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Funktionstasten (oberste Reihe)
        JPanel functionPanel = new JPanel(new GridLayout(1, 6, 5, 5));

        String[] functionKeys = { "MC", "MR", "MS", "M+", "M-", "Shift" };
        for (String key : functionKeys) {
            JButton button = createButton(key, new Color(220, 220, 240));
            functionPanel.add(button);
        }

        // Wissenschaftliche Funktionen (linke Seite)
        JPanel sciPanel = new JPanel(new GridLayout(6, 3, 5, 5));

        // Erste Reihe: Trig-Funktionen
        sciPanel.add(createButton("sin", new Color(220, 240, 220)));
        sciPanel.add(createButton("cos", new Color(220, 240, 220)));
        sciPanel.add(createButton("tan", new Color(220, 240, 220)));

        // Zweite Reihe: Inverse Trig-Funktionen
        sciPanel.add(createButton("asin", new Color(220, 240, 220)));
        sciPanel.add(createButton("acos", new Color(220, 240, 220)));
        sciPanel.add(createButton("atan", new Color(220, 240, 220)));

        // Dritte Reihe: Hyperbolische Funktionen
        sciPanel.add(createButton("sinh", new Color(220, 240, 220)));
        sciPanel.add(createButton("cosh", new Color(220, 240, 220)));
        sciPanel.add(createButton("tanh", new Color(220, 240, 220)));

        // Vierte Reihe: Logarithmen und Exponentialfunktionen
        sciPanel.add(createButton("ln", new Color(240, 220, 220)));
        sciPanel.add(createButton("log", new Color(240, 220, 220)));
        sciPanel.add(createButton("log2", new Color(240, 220, 220)));

        // Fünfte Reihe: Potenz-Funktionen
        sciPanel.add(createButton("x²", new Color(240, 220, 220)));
        sciPanel.add(createButton("x³", new Color(240, 220, 220)));
        sciPanel.add(createButton("x^y", new Color(240, 220, 220)));

        // Sechste Reihe: Wurzel-Funktionen
        sciPanel.add(createButton("√x", new Color(240, 220, 220)));
        sciPanel.add(createButton("³√x", new Color(240, 220, 220)));
        sciPanel.add(createButton("y√x", new Color(240, 220, 220)));

        // Standard-Taschenrechner (rechte Seite)
        JPanel numPanel = new JPanel(new GridLayout(6, 4, 5, 5));

        // Erste Reihe: Konstanten und Löschen
        numPanel.add(createButton("π", new Color(240, 240, 220)));
        numPanel.add(createButton("e", new Color(240, 240, 220)));
        numPanel.add(createButton("C", new Color(255, 200, 200)));
        numPanel.add(createButton("←", new Color(255, 240, 200)));

        // Zweite Reihe: Wissenschaftliche Operationen
        numPanel.add(createButton("(", new Color(220, 230, 255)));
        numPanel.add(createButton(")", new Color(220, 230, 255)));
        numPanel.add(createButton("abs", new Color(220, 230, 255)));
        numPanel.add(createButton("/", new Color(230, 230, 250)));

        // Dritte bis fünfte Reihe: Ziffern und Operatoren
        String[][] stdButtons = {
                { "7", "8", "9", "*" },
                { "4", "5", "6", "-" },
                { "1", "2", "3", "+" }
        };

        for (String[] row : stdButtons) {
            for (String label : row) {
                JButton button = createButton(label,
                        Character.isDigit(label.charAt(0)) ? new Color(255, 255, 255) : new Color(230, 230, 250));
                numPanel.add(button);
            }
        }

        // Sechste Reihe: Nullen, Dezimalpunkt, Vorzeichen und Ergebnis
        numPanel.add(createButton("0", Color.WHITE));
        numPanel.add(createButton("00", Color.WHITE));
        numPanel.add(createButton(".", Color.WHITE));
        numPanel.add(createButton("=", new Color(230, 230, 250)));

        // Layout zusammensetzen
        panel.add(functionPanel, BorderLayout.NORTH);
        panel.add(sciPanel, BorderLayout.WEST);
        panel.add(numPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Erstellt einen Button mit Beschriftung und Farbe
     */
    private JButton createButton(String label, Color color) {
        JButton button = new JButton(label);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setBackground(color);

        // Setze entsprechende Aktion für den Button
        button.addActionListener(e -> handleButtonClick(label));

        return button;
    }

    /**
     * Verarbeitet einen Tastendruck auf dem wissenschaftlichen Taschenrechner
     */
    private void handleButtonClick(String key) {
        debug("Wissenschaftliche Taste gedrückt: " + key);
        String aktuellerText = displayField.getText();

        switch (key) {
            case "Shift":
                shiftMode = !shiftMode;
                debug("Shift-Modus " + (shiftMode ? "aktiviert" : "deaktiviert"));
                return;

            case "MC":
                memoryValue = 0.0;
                memoryHasValue = false;
                debug("Speicher gelöscht");
                return;

            case "MR":
                if (memoryHasValue) {
                    displayField.setText(decimalFormat.format(memoryValue));
                    neueZahlBegonnen = true;
                    debug("Speicherwert abgerufen: " + memoryValue);
                }
                return;

            case "MS":
                try {
                    memoryValue = Double.parseDouble(aktuellerText);
                    memoryHasValue = true;
                    debug("Wert im Speicher gespeichert: " + memoryValue);
                } catch (NumberFormatException ex) {
                    debug("Fehler beim Speichern im Speicher: " + ex.getMessage());
                }
                return;

            case "M+":
                try {
                    memoryValue += Double.parseDouble(aktuellerText);
                    memoryHasValue = true;
                    debug("Zum Speicher addiert: neuer Wert = " + memoryValue);
                } catch (NumberFormatException ex) {
                    debug("Fehler bei M+: " + ex.getMessage());
                }
                return;

            case "M-":
                try {
                    memoryValue -= Double.parseDouble(aktuellerText);
                    memoryHasValue = true;
                    debug("Vom Speicher subtrahiert: neuer Wert = " + memoryValue);
                } catch (NumberFormatException ex) {
                    debug("Fehler bei M-: " + ex.getMessage());
                }
                return;

            case "C":
                displayField.setText("0");
                neueZahlBegonnen = true;
                debug("Display zurückgesetzt");
                return;

            case "←":
                if (aktuellerText.length() <= 1) {
                    displayField.setText("0");
                    neueZahlBegonnen = true;
                } else {
                    displayField.setText(aktuellerText.substring(0, aktuellerText.length() - 1));
                }
                debug("Zeichen gelöscht: " + displayField.getText());
                return;

            case "=":
                calculateResult();
                return;
        }

        // Ziffern und Operatoren
        if (key.matches("[0-9]") || key.equals("00")) {
            if (aktuellerText.equals("0") || neueZahlBegonnen) {
                displayField.setText(key);
                neueZahlBegonnen = false;
            } else {
                displayField.setText(aktuellerText + key);
            }
        } else if (key.equals(".")) {
            if (neueZahlBegonnen) {
                displayField.setText("0.");
                neueZahlBegonnen = false;
            } else if (!aktuellerText.contains(".")) {
                displayField.setText(aktuellerText + ".");
            }
        } else if (key.matches("[+\\-*/^]")) {
            if (aktuellerText.length() > 0 && "+-*/^".indexOf(aktuellerText.charAt(aktuellerText.length() - 1)) >= 0) {
                displayField.setText(aktuellerText.substring(0, aktuellerText.length() - 1) + key);
            } else {
                displayField.setText(aktuellerText + key);
            }
            neueZahlBegonnen = false;
        } else if (key.equals("(") || key.equals(")")) {
            if (aktuellerText.equals("0")) {
                displayField.setText(key);
            } else {
                displayField.setText(aktuellerText + key);
            }
            neueZahlBegonnen = false;
        }
        // Konstanten
        else if (key.equals("π")) {
            insertConstantOrFunction("pi");
        } else if (key.equals("e")) {
            insertConstantOrFunction("e");
        }
        // Funktionen
        else if (isMathFunction(key)) {
            insertConstantOrFunction(key);
        }
    }

    /**
     * Fügt eine Konstante oder Funktion in das Display ein
     */
    private void insertConstantOrFunction(String function) {
        String aktuellerText = displayField.getText();

        // Liste der Funktionen, die Parameter benötigen
        String[] paramFunctions = { "sin", "cos", "tan", "asin", "acos", "atan",
                "sinh", "cosh", "tanh", "log", "ln", "log2",
                "abs", "sqrt", "cbrt" };

        boolean needsParam = false;
        for (String func : paramFunctions) {
            if (function.equals(func)) {
                needsParam = true;
                break;
            }
        }

        if (needsParam) {
            // Funktion mit Parameter: füge Funktion und öffnende Klammer hinzu
            if (aktuellerText.equals("0")) {
                displayField.setText(function + "(");
            } else {
                displayField.setText(aktuellerText + function + "(");
            }
        } else {
            // Konstante oder Funktion ohne Parameter: füge nur den Namen ein
            if (aktuellerText.equals("0")) {
                displayField.setText(function);
            } else {
                displayField.setText(aktuellerText + function);
            }
        }

        neueZahlBegonnen = false;
    }

    /**
     * Prüft, ob ein String eine mathematische Funktion ist
     */
    private boolean isMathFunction(String function) {
        String[] mathFunctions = {
                "sin", "cos", "tan", "asin", "acos", "atan",
                "sinh", "cosh", "tanh", "ln", "log", "log2",
                "abs", "sqrt", "cbrt", "x²", "x³", "x^y", "√x", "³√x", "y√x"
        };

        for (String func : mathFunctions) {
            if (function.equals(func))
                return true;
        }

        return false;
    }

    /**
     * Berechnet das Ergebnis des eingegebenen Ausdrucks
     */
    private void calculateResult() {
        String ausdruck = displayField.getText();
        debug("Berechne wissenschaftlichen Ausdruck: " + ausdruck);

        // Prüfe, ob die Eingabe eine Funktion oder Konstante ist, die geplottet werden
        // kann
        if (checkIfFunction(ausdruck)) {
            debug("Mögliche Funktion oder Konstante erkannt: " + ausdruck);
            askToPlotFunction(ausdruck);
            return;
        }

        // Ersetze spezielle Tastenbezeichnungen durch Funktionsaufrufe
        ausdruck = ausdruck.replace("x²", "x^2");
        ausdruck = ausdruck.replace("x³", "x^3");
        ausdruck = ausdruck.replace("√x", "sqrt(x)");
        ausdruck = ausdruck.replace("³√x", "cbrt(x)");

        // Winkeleinheit-Konvertierung für trigonometrische Funktionen
        if (angleUnitComboBox.getSelectedIndex() == 1) { // Grad ausgewählt
            // Konvertiere Grad zu Radiant für trigonometrische Funktionen
            ausdruck = convertAngleUnit(ausdruck);
        }

        try {
            // Sonderfall: Einzelne Zahl im Display - keine Berechnung nötig
            if (ausdruck.matches("[0-9.]+")) {
                return;
            }

            // Direkte Auswertung über ein FunctionParser-Objekt
            FunctionParser parser = new FunctionParser(ausdruck);
            double ergebnis = parser.evaluateAt(0); // x spielt hier keine Rolle

            // Ergebnis formatieren und anzeigen
            String ergebnisText;
            if (ergebnis == (int) ergebnis) {
                ergebnisText = String.valueOf((int) ergebnis);
            } else {
                ergebnisText = decimalFormat.format(ergebnis);
            }

            displayField.setText(ergebnisText);
            neueZahlBegonnen = true;

            debug("Wissenschaftliches Ergebnis: " + ergebnisText);

            // Zur Historie hinzufügen
            calculator.addToHistory(ausdruck, ergebnisText);

        } catch (Exception e) {
            displayField.setText("Fehler");
            debug("Wissenschaftlicher Rechenfehler: " + e.getMessage());
        }
    }

    /**
     * Konvertiert Winkelangaben in trigonometrischen Funktionen von Grad zu Radiant
     */
    private String convertAngleUnit(String expression) {
        // Hier wäre ein guter Ort für eine komplexere Konvertierung mit Regex etc.
        // Als Vereinfachung ersetzen wir sin(x) mit sin(rad(x)) usw.
        String result = expression;

        // Ersetze sin(x) mit sin(rad(x)) usw.
        result = result.replaceAll("sin\\(", "sin(rad(");
        result = result.replaceAll("cos\\(", "cos(rad(");
        result = result.replaceAll("tan\\(", "tan(rad(");

        // Füge zusätzliche schließende Klammern ein
        result = result.replace(")", "))");

        debug("Winkeleinheit konvertiert: " + expression + " → " + result);
        return result;
    }

    /**
     * Prüft, ob eine Formel möglicherweise eine Funktion oder Konstante ist,
     * die im Plotter dargestellt werden kann
     */
    private boolean checkIfFunction(String formel) {
        // Fehler oder ungültige mathematische Ausdrücke überspringen
        if (formel.equals("Fehler") || formel.equals("NaN") || formel.equals("Infinity")) {
            return false;
        }

        // Bekannte Konstanten identifizieren
        if (formel.equals("pi") || formel.equals("e") || formel.equals("phi") ||
                formel.equals("sqrt2") || formel.equals("sqrt3") || formel.equals("golden")) {
            return true; // Konstanten können als horizontale Linien gezeichnet werden
        }

        // Prüfe auf numerische Werte (keine Variablen) mit maximal einer Operation
        try {
            // Versuche, die Formel als Zahl zu parsen
            Double.parseDouble(formel);
            // Es ist eine reine Zahl, kann als horizontale Linie dargestellt werden
            return true;
        } catch (NumberFormatException e) {
            // Keine reine Zahl, prüfe auf Variablen
            Matcher matcher = functionPattern.matcher(formel);

            // Wenn es Variablen enthält (insbesondere 'x'), ist es eine plottbare Funktion
            return matcher.matches() && formel.contains("x");
        }
    }

    /**
     * Fragt den Benutzer, ob er die erkannte Funktion oder Konstante plotten möchte
     */
    private void askToPlotFunction(String function) {
        String message;
        String title;

        // Bestimme, ob es sich um eine Konstante oder Funktion handelt
        boolean isConstant = isConstantExpression(function);

        if (isConstant) {
            // Für Konstanten angepasste Nachricht
            message = "Die Eingabe \"" + function + "\" ist ein konstanter Wert.\n" +
                    "Möchten Sie diesen Wert als horizontale Linie im Funktionsplotter darstellen?";
            title = "Konstante plotten?";
        } else {
            // Für Funktionen originale Nachricht
            message = "Die Eingabe \"" + function + "\" sieht wie eine Funktion aus.\n" +
                    "Möchten Sie diese Funktion im Funktionsplotter zeichnen?";
            title = "Funktion plotten?";
        }

        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                // Wenn der Benutzer zustimmt, Funktion an den Plotter übergeben
                if (calculator instanceof GrafischerTaschenrechner) {
                    GrafischerTaschenrechner graphCalc = (GrafischerTaschenrechner) calculator;
                    graphCalc.transferFunctionToPlotter(function);
                } else {
                    debug("Konnte Funktion nicht übertragen: Keine Instanz von GrafischerTaschenrechner");
                }
            }
        });
    }

    /**
     * Prüft, ob ein Ausdruck eine Konstante ist (keine Variablen enthält)
     */
    private boolean isConstantExpression(String expression) {
        // Bekannte Konstanten
        if (expression.equals("pi") || expression.equals("e") || expression.equals("phi") ||
                expression.equals("sqrt2") || expression.equals("sqrt3") || expression.equals("golden")) {
            return true;
        }

        // Prüfe auf reine Zahlen
        try {
            Double.parseDouble(expression);
            return true;
        } catch (NumberFormatException e) {
            // Kein konkreter Wert, prüfe auf Variable 'x'
            return !expression.contains("x");
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
            debugManager.debug("[Wissenschaftlich] " + message);
        } else {
            System.out.println("[Wissenschaftlich] " + message);
        }
    }

    /**
     * Aktualisiert die Anzeige des wissenschaftlichen Taschenrechners
     */
    public void refreshDisplay() {
        // Kann verwendet werden, um die Anzeige bei Tab-Wechsel zu aktualisieren
    }

    /**
     * Leert das Display des wissenschaftlichen Taschenrechners
     */
    public void clearDisplay() {
        displayField.setText("0");
        neueZahlBegonnen = true;
    }
}