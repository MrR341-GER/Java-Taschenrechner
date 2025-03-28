import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrafischerTaschenrechner extends JFrame {
    // UI-Komponenten
    private JTextField displayField;
    private JTextArea debugTextArea;
    private JPanel debugPanel;
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private JPanel historyPanel;

    // Split Panes für anpassbare Größe
    private JSplitPane mainSplitPane;
    private JSplitPane verticalSplitPane;

    // Status-Flags
    private boolean historyVisible = true;
    private boolean debugVisible = false;
    private boolean neueZahlBegonnen = true;

    // Berechnungsverlauf
    private ArrayList<String> calculationHistory = new ArrayList<>();

    // Konstanten für Textpuffer (zusätzlicher Platz um Text herum)
    private static final int TEXT_PADDING_HORIZONTAL = 16;
    private static final int TEXT_PADDING_VERTICAL = 10;
    private static final int MINIMUM_BUTTON_WIDTH = 40;
    private static final int MINIMUM_BUTTON_HEIGHT = 35;

    public GrafischerTaschenrechner() {
        // Fenstertitel und Grundeinstellungen
        super("Taschenrechner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(400, 300));

        // Eingabefeld erstellen
        displayField = new JTextField("0");
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setFont(new Font("Arial", Font.PLAIN, 24));
        displayField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    berechneFormel();
                }
            }
        });

        // Debug-Textfeld erstellen
        debugTextArea = new JTextArea(10, 40);
        debugTextArea.setEditable(false);
        debugTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane debugScrollPane = new JScrollPane(debugTextArea);

        // Debug-Panel erstellen
        debugPanel = new JPanel(new BorderLayout());
        debugPanel.add(new JLabel("Debug-Informationen:"), BorderLayout.NORTH);
        debugPanel.add(debugScrollPane, BorderLayout.CENTER);
        debugPanel.setVisible(debugVisible);

        // History-Panel erstellen
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && historyList.getSelectedIndex() != -1) {
                String selected = historyList.getSelectedValue();
                if (selected != null && !selected.isEmpty()) {
                    // Extrahiere die Formel aus dem History-Eintrag (Format: "Formel = Ergebnis")
                    String formel = selected.split("=")[0].trim();
                    displayField.setText(formel);
                    debug("Formel aus History geladen: " + formel);
                }
            }
        });

        JScrollPane historyScrollPane = new JScrollPane(historyList);
        historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("Berechnungsverlauf:"), BorderLayout.NORTH);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        // Clear-History-Button
        JButton clearHistoryButton = new JButton("Verlauf löschen");
        adjustButtonSize(clearHistoryButton);
        clearHistoryButton.addActionListener(e -> {
            historyModel.clear();
            calculationHistory.clear();
            debug("Berechnungsverlauf gelöscht");
        });
        historyPanel.add(clearHistoryButton, BorderLayout.SOUTH);

        // Haupt-Button-Panel mit BorderLayout
        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));

        // Kontroll-Buttons erstellen
        JButton debugButton = new JButton("Debug");
        debugButton.setFont(new Font("Arial", Font.PLAIN, 16));
        debugButton.setBackground(new Color(255, 200, 200));
        adjustButtonSize(debugButton);
        debugButton.addActionListener(e -> toggleDebug());

        JButton historyButton = new JButton("History");
        historyButton.setFont(new Font("Arial", Font.PLAIN, 16));
        historyButton.setBackground(new Color(200, 230, 255));
        adjustButtonSize(historyButton);
        historyButton.addActionListener(e -> toggleHistory());

        // Panel für die Kontroll-Buttons
        JPanel controlButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlButtonPanel.add(debugButton);
        controlButtonPanel.add(historyButton);

        // Panels für die verschiedenen Tastengruppen mit festen Abständen
        JPanel ziffernPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        JPanel operatorenPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        JPanel funktionsPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JPanel wissenschaftlichPanel = new JPanel(new GridLayout(1, 6, 5, 5));

        // Zifferntasten (0-9 und .)
        String[] ziffern = { "7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "+/-" };
        for (String ziffer : ziffern) {
            JButton button = new JButton(ziffer);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            adjustButtonSize(button);
            button.addActionListener(new TastenListener());
            ziffernPanel.add(button);
        }

        // Operatoren
        String[] operatoren = { "/", "*", "-", "+", "=" };
        for (String operator : operatoren) {
            JButton button = new JButton(operator);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Leichter Flieder-Farbton
            adjustButtonSize(button);
            button.addActionListener(new TastenListener());
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

            button.addActionListener(new TastenListener());
            funktionsPanel.add(button);
        }

        // Wissenschaftliche Funktionen
        String[] wissenschaftlich = { "x²", "x³", "x^y", "√x", "³√x", "y√x" };
        for (String funktion : wissenschaftlich) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Flieder-Farbton
            adjustButtonSize(button);
            button.addActionListener(new TastenListener());
            wissenschaftlichPanel.add(button);
        }

        // Panel für Tastenlayout zusammenstellen
        JPanel allButtonsPanel = new JPanel(new BorderLayout(5, 5));
        allButtonsPanel.add(funktionsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(ziffernPanel, BorderLayout.CENTER);
        centerPanel.add(operatorenPanel, BorderLayout.EAST);

        // Wissenschaftliches Panel mit GridLayout für 5 Tasten
        JPanel wissenschaftlichesGridPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        wissenschaftlichesGridPanel.add(wissenschaftlichPanel);

        allButtonsPanel.add(centerPanel, BorderLayout.CENTER);
        allButtonsPanel.add(wissenschaftlichesGridPanel, BorderLayout.SOUTH);

        // Zusammenfügen der UI-Komponenten
        buttonPanel.add(allButtonsPanel, BorderLayout.CENTER);
        buttonPanel.add(controlButtonPanel, BorderLayout.SOUTH);

        // Layout zusammensetzen
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(displayField, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // Seitenleiste für Historie
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(historyPanel, BorderLayout.CENTER);

        // Erstelle horizontales JSplitPane für Hauptbereich und History
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, sidePanel);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setResizeWeight(0.7); // Hauptbereich bekommt 70% des Platzes bei Größenänderung

        // Erstelle vertikales JSplitPane für Hauptbereich und Debug-Panel
        verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplitPane, debugPanel);
        verticalSplitPane.setOneTouchExpandable(true);
        verticalSplitPane.setDividerLocation(400);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setResizeWeight(0.85); // Hauptbereich bekommt 85% des Platzes bei Größenänderung

        // Haupt-Layout mit SplitPanes
        setLayout(new BorderLayout());
        add(verticalSplitPane, BorderLayout.CENTER);

        debug("Taschenrechner gestartet");
    }

    // Hilfsmethode zum dynamischen Anpassen der Buttongröße basierend auf dem Text
    private void adjustButtonSize(JButton button) {
        // Berechne die Textgröße
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(button.getText());
        int textHeight = metrics.getHeight();

        // Setze die Mindestgröße des Buttons basierend auf der Textgröße plus Puffer
        int width = Math.max(textWidth + TEXT_PADDING_HORIZONTAL, MINIMUM_BUTTON_WIDTH);
        int height = Math.max(textHeight + TEXT_PADDING_VERTICAL, MINIMUM_BUTTON_HEIGHT);

        button.setMinimumSize(new Dimension(width, height));
        button.setPreferredSize(new Dimension(width, height));

        // Kleine Ränder für besseres Aussehen
        button.setMargin(new Insets(4, 4, 4, 4));

        // Debug-Ausgabe zur Kontrolle
        debug("Button '" + button.getText() + "' Größe angepasst: " + width + "x" + height +
                " (Text: " + textWidth + "x" + textHeight + ")");
    }

    private void toggleHistory() {
        historyVisible = !historyVisible;
        historyPanel.setVisible(historyVisible);
        debug("History-Modus " + (historyVisible ? "aktiviert" : "deaktiviert"));

        if (historyVisible) {
            mainSplitPane.setDividerLocation(400);
        } else {
            mainSplitPane.setDividerLocation(getWidth() - 10);
        }
    }

    private void toggleDebug() {
        debugVisible = !debugVisible;
        debugPanel.setVisible(debugVisible);
        debug("Debug-Modus " + (debugVisible ? "aktiviert" : "deaktiviert"));

        if (debugVisible) {
            verticalSplitPane.setDividerLocation(getHeight() - 200);
        } else {
            verticalSplitPane.setDividerLocation(getHeight() - 10);
        }
    }

    private void addToHistory(String calculation, String result) {
        String historyEntry = calculation + " = " + result;
        calculationHistory.add(historyEntry);
        historyModel.addElement(historyEntry);

        // Scrolle zum neuesten Eintrag
        historyList.ensureIndexIsVisible(historyModel.getSize() - 1);

        debug("Zur History hinzugefügt: " + historyEntry);
    }

    private void debug(String message) {
        System.out.println("[DEBUG] " + message);
        debugTextArea.append(message + "\n");
        // Scroll zum Ende
        debugTextArea.setCaretPosition(debugTextArea.getDocument().getLength());
    }

    private class TastenListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String eingabe = e.getActionCommand();
            String aktuellerText = displayField.getText();

            debug("Taste gedrückt: " + eingabe);

            // Logik für verschiedene Tastenaktionen
            switch (eingabe) {
                case "0":
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                    // Wenn Anzeige '0' ist oder eine neue Zahl begonnen wird
                    if (aktuellerText.equals("0")) {
                        displayField.setText(eingabe);
                        debug("Null ersetzt durch: " + eingabe);
                    } else if (neueZahlBegonnen) {
                        // Hier ist der kritische Fix: Nicht die ganze Rechnung ersetzen,
                        // sondern nur an die bestehende Rechnung anhängen
                        displayField.setText(aktuellerText + eingabe);
                        debug("Neue Zahl angehängt an Ausdruck: " + aktuellerText + eingabe);
                        neueZahlBegonnen = false;
                    } else {
                        displayField.setText(aktuellerText + eingabe);
                        debug("Ziffer angehängt: " + aktuellerText + eingabe);
                    }
                    break;

                case "+":
                case "-":
                case "*":
                case "/":
                    // Prüfen, ob das letzte Zeichen bereits ein Operator ist
                    if (!aktuellerText.isEmpty()
                            && istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                        // Ersetze den vorherigen Operator durch den neuen
                        String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1) + eingabe;
                        displayField.setText(neuerText);
                        debug("Operator ersetzt: " + aktuellerText + " -> " + neuerText);
                    } else {
                        // Operator-Taste - neue Zahl beginnt nach Operator
                        displayField.setText(aktuellerText + eingabe);
                        debug("Operator hinzugefügt: " + aktuellerText + eingabe + ", neue Zahl wird erwartet");
                    }
                    neueZahlBegonnen = true;
                    break;

                case "x^y":
                    // Potenzfunktion hinzufügen - wie ein normaler Operator
                    if (!aktuellerText.isEmpty()
                            && istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                        // Ersetze den vorherigen Operator
                        String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1) + "^";
                        displayField.setText(neuerText);
                        debug("Operator ersetzt durch Potenz: " + aktuellerText + " -> " + neuerText);
                    } else {
                        displayField.setText(aktuellerText + "^");
                        debug("Potenzoperator hinzugefügt: " + aktuellerText + "^");
                    }
                    neueZahlBegonnen = true;
                    break;

                case "x²":
                    // Quadratfunktion - fügt ^2 hinzu
                    try {
                        double zahl = evaluiereLetzteTeilzahl(aktuellerText);
                        // Ersetze die letzte Zahl durch ihre quadrierte Form
                        String neuerText = ersetzeLetzteZahl(aktuellerText, zahl * zahl);
                        displayField.setText(neuerText);
                        debug("Quadriert: " + zahl + "² = " + (zahl * zahl));
                    } catch (Exception ex) {
                        debug("Fehler beim Quadrieren: " + ex.getMessage());
                    }
                    neueZahlBegonnen = true;
                    break;

                case "x³":
                    // Kubische Funktion - fügt ^3 hinzu
                    try {
                        double zahl = evaluiereLetzteTeilzahl(aktuellerText);
                        // Ersetze die letzte Zahl durch ihre kubische Form
                        String neuerText = ersetzeLetzteZahl(aktuellerText, zahl * zahl * zahl);
                        displayField.setText(neuerText);
                        debug("Kubiert: " + zahl + "³ = " + (zahl * zahl * zahl));
                    } catch (Exception ex) {
                        debug("Fehler beim Kubieren: " + ex.getMessage());
                    }
                    neueZahlBegonnen = true;
                    break;

                case "√x":
                    // Quadratwurzelfunktion
                    try {
                        double zahl = evaluiereLetzteTeilzahl(aktuellerText);
                        if (zahl < 0) {
                            displayField.setText("Fehler");
                            debug("Fehler: Wurzel aus negativer Zahl nicht erlaubt");
                        } else {
                            // Ersetze die letzte Zahl durch ihre Quadratwurzel
                            String neuerText = ersetzeLetzteZahl(aktuellerText, Math.sqrt(zahl));
                            displayField.setText(neuerText);
                            debug("Quadratwurzel: √" + zahl + " = " + Math.sqrt(zahl));
                        }
                    } catch (Exception ex) {
                        debug("Fehler bei Quadratwurzelberechnung: " + ex.getMessage());
                    }
                    neueZahlBegonnen = true;
                    break;

                case "³√x":
                    // Kubikwurzelfunktion
                    try {
                        double zahl = evaluiereLetzteTeilzahl(aktuellerText);
                        // Bei Kubikwurzeln ist die Berechnung auch für negative Zahlen möglich
                        // Ersetze die letzte Zahl durch ihre Kubikwurzel
                        double kubikwurzel = Math.cbrt(zahl);
                        String neuerText = ersetzeLetzteZahl(aktuellerText, kubikwurzel);
                        displayField.setText(neuerText);
                        debug("Kubikwurzel: ³√" + zahl + " = " + kubikwurzel);
                    } catch (Exception ex) {
                        debug("Fehler bei Kubikwurzelberechnung: " + ex.getMessage());
                    }
                    neueZahlBegonnen = true;
                    break;

                case "y√x":
                    // Y-te Wurzel aus X
                    try {
                        // Bei dieser Operation brauchen wir zwei Zahlen:
                        // - y (den Wurzelexponenten - den wir per Popup abfragen)
                        // - x (die Zahl, aus der die Wurzel gezogen wird - die aktuelle Zahl)
                        double x = evaluiereLetzteTeilzahl(aktuellerText);

                        if (x < 0) {
                            displayField.setText("Fehler");
                            debug("Fehler: Wurzel aus negativer Zahl ist nur für ungerade Wurzeln definiert");
                            return;
                        }

                        // Frage nach dem Wurzelexponenten y
                        String yInput = JOptionPane.showInputDialog(
                                GrafischerTaschenrechner.this,
                                "Bitte Wurzelexponent eingeben:",
                                "Y-te Wurzel",
                                JOptionPane.QUESTION_MESSAGE);

                        if (yInput == null || yInput.trim().isEmpty()) {
                            debug("Wurzelberechnung abgebrochen: Kein Exponent eingegeben");
                            return;
                        }

                        try {
                            double y = Double.parseDouble(yInput);

                            if (y == 0) {
                                displayField.setText("Fehler");
                                debug("Fehler: Division durch Null (Wurzelexponent darf nicht 0 sein)");
                                return;
                            }

                            // Berechnung: x^(1/y)
                            double ergebnis = Math.pow(x, 1.0 / y);
                            String neuerText = ersetzeLetzteZahl(aktuellerText, ergebnis);
                            displayField.setText(neuerText);
                            debug("Y-te Wurzel: " + y + "√" + x + " = " + ergebnis);

                        } catch (NumberFormatException ex) {
                            displayField.setText("Fehler");
                            debug("Fehler: Ungültiger Wurzelexponent");
                        }
                    } catch (Exception ex) {
                        debug("Fehler bei Wurzelberechnung: " + ex.getMessage());
                    }
                    neueZahlBegonnen = true;
                    break;

                case "(":
                case ")":
                    // Klammern hinzufügen
                    if (aktuellerText.equals("0")) {
                        displayField.setText(eingabe);
                    } else {
                        displayField.setText(aktuellerText + eingabe);
                    }
                    neueZahlBegonnen = true;
                    debug("Klammer hinzugefügt: " + displayField.getText());
                    break;

                case ".":
                    // Punkt hinzufügen, wenn nicht bereits vorhanden
                    if (neueZahlBegonnen) {
                        displayField.setText(aktuellerText + "0.");
                        neueZahlBegonnen = false;
                        debug("Neue Dezimalzahl begonnen: " + aktuellerText + "0.");
                    } else {
                        // Prüfen, ob die aktuelle Zahl bereits einen Punkt hat
                        String aktuelleZahl = findeAktuelleZahl(aktuellerText);
                        if (!aktuelleZahl.contains(".")) {
                            displayField.setText(aktuellerText + ".");
                            debug("Dezimalpunkt hinzugefügt: " + aktuellerText + ".");
                        }
                    }
                    break;

                case "=":
                    debug("Berechne Formel: " + aktuellerText);
                    berechneFormel();
                    neueZahlBegonnen = true;
                    break;

                case "+/-":
                    // Vorzeichen der aktuellen Zahl umkehren
                    String neuerText = toggleVorzeichen(aktuellerText);
                    displayField.setText(neuerText);
                    debug("Nach +/- Taste: " + neuerText);
                    // +/- ändert nicht den neueZahlBegonnen-Status
                    break;

                case "C":
                    // Zurücksetzen
                    displayField.setText("0");
                    neueZahlBegonnen = true;
                    debug("Display zurückgesetzt");
                    break;

                case "←":
                    // Return/Backspace-Funktion - letztes Zeichen löschen
                    handleReturnButton(aktuellerText);
                    break;
            }
        }
    }

    // Hilfsmethode für den Return-Button
    private void handleReturnButton(String aktuellerText) {
        debug("Return/Backspace-Taste gedrückt für: " + aktuellerText);

        if (aktuellerText.length() <= 1) {
            // Wenn nur ein Zeichen übrig oder leer, auf 0 zurücksetzen
            displayField.setText("0");
            neueZahlBegonnen = true;
            debug("Zurückgesetzt auf 0, da keine weiteren Zeichen");
        } else {
            // Letztes Zeichen entfernen
            String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1);
            displayField.setText(neuerText);

            // Prüfen, ob das letzte Zeichen ein Operator ist
            char letzterChar = neuerText.charAt(neuerText.length() - 1);
            if (istOperator(letzterChar)) {
                neueZahlBegonnen = true;
                debug("Letztes Zeichen gelöscht, Operator erkannt: " + neuerText);
            } else {
                neueZahlBegonnen = false;
                debug("Letztes Zeichen gelöscht: " + neuerText);
            }
        }
    }

    // Findet die aktuelle Zahl, an der gerade gearbeitet wird
    private String findeAktuelleZahl(String ausdruck) {
        // Wenn der Ausdruck leer ist oder nur aus Operatoren besteht
        if (ausdruck.isEmpty() || ausdruck.matches("[+\\-*/()^]+")) {
            return "";
        }

        // Regulärer Ausdruck, um die letzte Zahl zu finden
        // Sucht nach einer Zahl (mit oder ohne Dezimalpunkt) am Ende des Ausdrucks
        Pattern pattern = Pattern.compile("[0-9]+(\\.[0-9]*)?$");
        Matcher matcher = pattern.matcher(ausdruck);

        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    // Hilfsmethode zum Evaluieren der letzten Teilzahl im Ausdruck
    private double evaluiereLetzteTeilzahl(String ausdruck) {
        String letzteZahl = findeAktuelleZahl(ausdruck);
        if (letzteZahl.isEmpty()) {
            throw new IllegalArgumentException("Keine gültige Zahl gefunden");
        }
        return Double.parseDouble(letzteZahl);
    }

    // Hilfsmethode zum Ersetzen der letzten Zahl im Ausdruck
    private String ersetzeLetzteZahl(String ausdruck, double neuerWert) {
        String letzteZahl = findeAktuelleZahl(ausdruck);
        if (letzteZahl.isEmpty()) {
            return ausdruck + neuerWert;
        }

        // Formatiere den neuen Wert
        String formatierterWert;
        if (neuerWert == (int) neuerWert) {
            formatierterWert = String.valueOf((int) neuerWert);
        } else {
            formatierterWert = String.valueOf(neuerWert);
        }

        // Ersetze die letzte Zahl durch den neuen Wert
        return ausdruck.substring(0, ausdruck.length() - letzteZahl.length()) + formatierterWert;
    }

    // Hilfsmethode zum Umkehren des Vorzeichens der aktuellen Zahl (explizit als
    // +/- anzeigen)
    private String toggleVorzeichen(String ausdruck) {
        debug("Toggle-Vorzeichen für Ausdruck: " + ausdruck);

        // Leerer Ausdruck oder nur 0
        if (ausdruck.isEmpty() || ausdruck.equals("0")) {
            return "0";
        }

        // Wenn wir gerade eine neue Zahl beginnen (nach einem Operator)
        if (neueZahlBegonnen) {
            // Prüfen, ob der letzte Charakter ein Operator ist
            char letzterChar = ausdruck.charAt(ausdruck.length() - 1);
            if (istOperator(letzterChar)) {
                // Fügen wir ein explizites Minuszeichen hinzu
                return ausdruck + "-";
            }
        }

        // Für eine bestehende Berechnung analysieren wir den Ausdruck, um die letzte
        // Zahl zu finden

        // Eine Möglichkeit wäre, vom Ende des Strings zurückzugehen, bis wir einen
        // Operator finden
        int letztesZeichenPos = ausdruck.length() - 1;

        // Zuerst prüfen, ob wir am Ende Ziffern haben (normale Zahl)
        while (letztesZeichenPos >= 0 &&
                (Character.isDigit(ausdruck.charAt(letztesZeichenPos)) ||
                        ausdruck.charAt(letztesZeichenPos) == '.')) {
            letztesZeichenPos--;
        }

        // Wenn wir am Anfang des Ausdrucks sind oder ein Operator vor der Zahl
        if (letztesZeichenPos < 0 || istOperator(ausdruck.charAt(letztesZeichenPos))) {
            // Prüfen, ob vor der Zahl ein Minuszeichen steht, was zur Zahl gehört
            if (letztesZeichenPos >= 0 && ausdruck.charAt(letztesZeichenPos) == '-') {
                // Es gibt ein Minuszeichen - prüfen, ob es ein Vorzeichen oder ein Operator ist
                if (letztesZeichenPos == 0 || istOperator(ausdruck.charAt(letztesZeichenPos - 1))) {
                    // Es ist ein Vorzeichen - wir entfernen es
                    return ausdruck.substring(0, letztesZeichenPos) + ausdruck.substring(letztesZeichenPos + 1);
                }
            }

            // Kein Minuszeichen - wir fügen eines hinzu
            if (letztesZeichenPos < 0) {
                // Die gesamte Zahl ist negativ
                return "-" + ausdruck;
            } else {
                // Wir fügen nach dem Operator ein Minuszeichen ein
                return ausdruck.substring(0, letztesZeichenPos + 1) + "-" + ausdruck.substring(letztesZeichenPos + 1);
            }
        }

        // Für komplexere Fälle (mit Klammern etc.)
        debug("Kein einfacher Fall erkannt, toggle nicht möglich");
        return ausdruck;
    }

    // Hilfsmethode, um zu prüfen, ob ein Zeichen ein Operator ist
    private boolean istOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == '^';
    }

    // Hilfsmethode, um zu prüfen, ob ein Zeichen ein normaler Operator ist (ohne
    // Klammern)
    private boolean istNormalerOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private void berechneFormel() {
        try {
            String formel = displayField.getText().trim();

            // Überprüfen, ob die Formel leer ist
            if (formel.isEmpty()) {
                displayField.setText("0");
                debug("Leere Formel, auf 0 zurückgesetzt");
                return;
            }

            debug("Originale Formel: " + formel);

            // Einfache Vorverarbeitung - doppelte Operatoren korrekt behandeln
            // --3 wird zu +3, +-3 bleibt -3
            formel = formel.replace("--", "+");
            formel = formel.replaceAll("\\+\\+", "+");
            formel = formel.replace("+-", "-"); // Korrektur für +-

            // Implizite Multiplikationen behandeln (z.B. 2(5+5) zu 2*(5+5))
            formel = ergaenzeImpliziteMultiplikationen(formel);

            debug("Vorverarbeitete Formel: " + formel);

            // Eigene Berechnung statt ScriptEngine
            double ergebnis = berechneAusdruck(formel);
            debug("Berechnetes Ergebnis: " + ergebnis);

            // Ergebnis formatieren
            String ergebnisText;
            if (ergebnis == (int) ergebnis) {
                ergebnisText = String.valueOf((int) ergebnis);
                displayField.setText(ergebnisText);
                debug("Formatiertes Ergebnis (int): " + ergebnisText);
            } else {
                ergebnisText = String.valueOf(ergebnis);
                displayField.setText(ergebnisText);
                debug("Formatiertes Ergebnis (double): " + ergebnisText);
            }

            // Zur History hinzufügen
            addToHistory(formel, ergebnisText);

        } catch (Exception e) {
            displayField.setText("Fehler");
            debug("Berechnungsfehler: " + e.getMessage());
            debug("Stack: " + e.getStackTrace()[0]);
            for (int i = 0; i < Math.min(3, e.getStackTrace().length); i++) {
                debug("  bei " + e.getStackTrace()[i]);
            }
        }
    }

    // Hilfsmethode zur Behandlung impliziter Multiplikationen
    private String ergaenzeImpliziteMultiplikationen(String formel) {
        debug("Prüfe auf implizite Multiplikationen in: " + formel);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < formel.length(); i++) {
            char aktuellesZeichen = formel.charAt(i);
            result.append(aktuellesZeichen);

            // Wenn das aktuelle Zeichen nicht das letzte ist
            if (i < formel.length() - 1) {
                char naechstesZeichen = formel.charAt(i + 1);

                // Fall 1: Zahl gefolgt von öffnender Klammer -> Multiplikationszeichen einfügen
                if (Character.isDigit(aktuellesZeichen) && naechstesZeichen == '(') {
                    result.append('*');
                    debug("Implizite Multiplikation erkannt: Zahl(" + aktuellesZeichen + ") vor Klammer");
                }

                // Fall 2: Schließende Klammer gefolgt von Zahl -> Multiplikationszeichen
                // einfügen
                else if (aktuellesZeichen == ')' && Character.isDigit(naechstesZeichen)) {
                    result.append('*');
                    debug("Implizite Multiplikation erkannt: Klammer vor Zahl(" + naechstesZeichen + ")");
                }

                // Fall 3: Schließende Klammer gefolgt von öffnender Klammer ->
                // Multiplikationszeichen einfügen
                else if (aktuellesZeichen == ')' && naechstesZeichen == '(') {
                    result.append('*');
                    debug("Implizite Multiplikation erkannt: Klammer vor Klammer");
                }
            }
        }

        String resultString = result.toString();
        if (!resultString.equals(formel)) {
            debug("Implizite Multiplikation umgewandelt: " + formel + " -> " + resultString);
        }

        return resultString;
    }

    // Eigene Implementierung eines einfachen Expression-Parsers
    private double berechneAusdruck(String ausdruck) {
        debug("Berechne Ausdruck: " + ausdruck);

        // Klammern zuerst auswerten
        while (ausdruck.contains("(")) {
            int offen = ausdruck.lastIndexOf("(");
            int geschlossen = ausdruck.indexOf(")", offen);

            if (geschlossen == -1) {
                throw new IllegalArgumentException("Fehlende schließende Klammer");
            }

            String subAusdruck = ausdruck.substring(offen + 1, geschlossen);
            debug("Gefundener Teilausdruck: " + subAusdruck);

            double teilErgebnis = berechneAusdruck(subAusdruck);
            debug("Teilausdruck ergibt: " + teilErgebnis);

            ausdruck = ausdruck.substring(0, offen) + teilErgebnis + ausdruck.substring(geschlossen + 1);
            debug("Ausdruck nach Klammer-Ersetzung: " + ausdruck);
        }

        // Addition und Subtraktion
        ArrayList<Double> zahlen = new ArrayList<>();
        ArrayList<Character> operatoren = new ArrayList<>();

        // Aufteilung in Zahlen und Operatoren
        StringBuilder aktuelleZahl = new StringBuilder();
        boolean istErsteZahl = true;
        boolean letzteWarOperator = false;

        for (int i = 0; i < ausdruck.length(); i++) {
            char c = ausdruck.charAt(i);

            if (c == '+' || c == '-') {
                // Wenn es die erste Zahl ist oder nach einem Operator kommt, ist es ein
                // Vorzeichen
                if (istErsteZahl || letzteWarOperator) {
                    aktuelleZahl.append(c);
                    letzteWarOperator = false;
                } else {
                    if (aktuelleZahl.length() > 0) {
                        zahlen.add(Double.parseDouble(aktuelleZahl.toString()));
                        aktuelleZahl = new StringBuilder();
                    }
                    operatoren.add(c);
                    letzteWarOperator = true;
                }
                istErsteZahl = false;
            } else if (c == '*' || c == '/' || c == '^') {
                if (aktuelleZahl.length() > 0) {
                    zahlen.add(Double.parseDouble(aktuelleZahl.toString()));
                    aktuelleZahl = new StringBuilder();
                }
                operatoren.add(c);
                letzteWarOperator = true;
                istErsteZahl = false;
            } else if (Character.isDigit(c) || c == '.') {
                aktuelleZahl.append(c);
                letzteWarOperator = false;
                istErsteZahl = false;
            }
        }

        if (aktuelleZahl.length() > 0) {
            zahlen.add(Double.parseDouble(aktuelleZahl.toString()));
        }

        debug("Zahlen: " + zahlen);
        debug("Operatoren: " + operatoren);

        // Erst ^ (Potenz) berechnen
        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '^') {
                double ergebnis;
                double basis = zahlen.get(i);
                double exponent = zahlen.get(i + 1);

                ergebnis = Math.pow(basis, exponent);
                debug("Potenz: " + basis + " ^ " + exponent + " = " + ergebnis);

                zahlen.set(i, ergebnis);
                zahlen.remove(i + 1);
                operatoren.remove(i);
                i--;
            }
        }

        // Dann * und / berechnen
        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '*' || operatoren.get(i) == '/') {
                double ergebnis;
                double a = zahlen.get(i);
                double b = zahlen.get(i + 1);

                if (operatoren.get(i) == '*') {
                    ergebnis = a * b;
                    debug("Multiplikation: " + a + " * " + b + " = " + ergebnis);
                } else {
                    if (b == 0) {
                        throw new ArithmeticException("Division durch Null");
                    }
                    ergebnis = a / b;
                    debug("Division: " + a + " / " + b + " = " + ergebnis);
                }

                zahlen.set(i, ergebnis);
                zahlen.remove(i + 1);
                operatoren.remove(i);
                i--;
            }
        }

        // Dann + und - berechnen
        double ergebnis = zahlen.get(0);

        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '+') {
                ergebnis += zahlen.get(i + 1);
                debug("Addition: " + ergebnis + " + " + zahlen.get(i + 1) + " = " + (ergebnis + zahlen.get(i + 1)));
            } else if (operatoren.get(i) == '-') {
                ergebnis -= zahlen.get(i + 1);
                debug("Subtraktion: " + ergebnis + " - " + zahlen.get(i + 1) + " = " + (ergebnis - zahlen.get(i + 1)));
            }
        }

        debug("Endergebnis des Ausdrucks: " + ergebnis);
        return ergebnis;
    }

    public static void main(String[] args) {
        // GUI im Event-Dispatch-Thread starten
        SwingUtilities.invokeLater(() -> {
            GrafischerTaschenrechner taschenrechner = new GrafischerTaschenrechner();
            taschenrechner.setVisible(true);
        });
    }
}