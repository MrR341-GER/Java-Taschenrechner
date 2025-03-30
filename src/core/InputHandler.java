import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;

/**
 * Handles key input for the calculator
 */
public class InputHandler implements ActionListener {
    private final Taschenrechner calculator;
    private final CalculationEngine engine;

    public InputHandler(Taschenrechner calculator, CalculationEngine engine) {
        this.calculator = calculator;
        this.engine = engine;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String eingabe = e.getActionCommand();
        String aktuellerText = calculator.getDisplayText();

        calculator.debug("Taste gedrückt: " + eingabe);

        // Logic for different key actions
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
                // If display is '0' or a new number is being started
                if (aktuellerText.equals("0")) {
                    calculator.setDisplayText(eingabe);
                    calculator.debug("Null ersetzt durch: " + eingabe);
                } else if (calculator.isNeueZahlBegonnen()) {
                    // Critical fix: Don't replace the entire calculation,
                    // but append to the existing calculation
                    calculator.setDisplayText(aktuellerText + eingabe);
                    calculator.debug("Neue Zahl angehängt an Ausdruck: " + aktuellerText + eingabe);
                    calculator.setNeueZahlBegonnen(false);
                } else {
                    calculator.setDisplayText(aktuellerText + eingabe);
                    calculator.debug("Ziffer angehängt: " + aktuellerText + eingabe);
                }
                break;

            case "+":
            case "-":
            case "*":
            case "/":
                // Check if the last character is already an operator
                if (!aktuellerText.isEmpty()
                        && engine.istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                    // Replace the previous operator with the new one
                    String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1) + eingabe;
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Operator ersetzt: " + aktuellerText + " -> " + neuerText);
                } else {
                    // Operator key - new number begins after operator
                    calculator.setDisplayText(aktuellerText + eingabe);
                    calculator.debug("Operator hinzugefügt: " + aktuellerText + eingabe + ", neue Zahl wird erwartet");
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "x^y":
                // Add power function - like a normal operator
                if (!aktuellerText.isEmpty()
                        && engine.istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                    // Replace the previous operator
                    String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1) + "^";
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Operator ersetzt durch Potenz: " + aktuellerText + " -> " + neuerText);
                } else {
                    calculator.setDisplayText(aktuellerText + "^");
                    calculator.debug("Potenzoperator hinzugefügt: " + aktuellerText + "^");
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "x²":
                // Square function - adds ^2
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // Replace the last number with its squared form
                    String neuerText = engine.ersetzeLetzteZahl(aktuellerText, zahl * zahl);
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Quadriert: " + zahl + "² = " + (zahl * zahl));
                } catch (Exception ex) {
                    calculator.debug("Fehler beim Quadrieren: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "x³":
                // Cubic function - adds ^3
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // Replace the last number with its cubic form
                    String neuerText = engine.ersetzeLetzteZahl(aktuellerText, zahl * zahl * zahl);
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Kubiert: " + zahl + "³ = " + (zahl * zahl * zahl));
                } catch (Exception ex) {
                    calculator.debug("Fehler beim Kubieren: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "√x":
                // Square root function
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    if (zahl < 0) {
                        calculator.setDisplayText("Fehler");
                        calculator.debug("Fehler: Wurzel aus negativer Zahl nicht erlaubt");
                    } else {
                        // Replace the last number with its square root
                        String neuerText = engine.ersetzeLetzteZahl(aktuellerText, Math.sqrt(zahl));
                        calculator.setDisplayText(neuerText);
                        calculator.debug("Quadratwurzel: √" + zahl + " = " + Math.sqrt(zahl));
                    }
                } catch (Exception ex) {
                    calculator.debug("Fehler bei Quadratwurzelberechnung: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "³√x":
                // Cubic root function
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // For cubic roots, calculation is also possible for negative numbers
                    // Replace the last number with its cubic root
                    double kubikwurzel = Math.cbrt(zahl);
                    String neuerText = engine.ersetzeLetzteZahl(aktuellerText, kubikwurzel);
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Kubikwurzel: ³√" + zahl + " = " + kubikwurzel);
                } catch (Exception ex) {
                    calculator.debug("Fehler bei Kubikwurzelberechnung: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "y√x":
                // Y-th root of X
                try {
                    // For this operation we need two numbers:
                    // - y (the root exponent - which we ask via popup)
                    // - x (the number from which to take the root - the current number)
                    double x = engine.evaluiereLetzteTeilzahl(aktuellerText);

                    if (x < 0) {
                        calculator.setDisplayText("Fehler");
                        calculator.debug("Fehler: Wurzel aus negativer Zahl ist nur für ungerade Wurzeln definiert");
                        return;
                    }

                    // Ask for the root exponent y
                    String yInput = JOptionPane.showInputDialog(
                            calculator,
                            "Bitte Wurzelexponent eingeben:",
                            "Y-te Wurzel",
                            JOptionPane.QUESTION_MESSAGE);

                    if (yInput == null || yInput.trim().isEmpty()) {
                        calculator.debug("Wurzelberechnung abgebrochen: Kein Exponent eingegeben");
                        return;
                    }

                    try {
                        double y = Double.parseDouble(yInput);

                        if (y == 0) {
                            calculator.setDisplayText("Fehler");
                            calculator.debug("Fehler: Division durch Null (Wurzelexponent darf nicht 0 sein)");
                            return;
                        }

                        // Calculation: x^(1/y)
                        double ergebnis = Math.pow(x, 1.0 / y);
                        String neuerText = engine.ersetzeLetzteZahl(aktuellerText, ergebnis);
                        calculator.setDisplayText(neuerText);
                        calculator.debug("Y-te Wurzel: " + y + "√" + x + " = " + ergebnis);

                    } catch (NumberFormatException ex) {
                        calculator.setDisplayText("Fehler");
                        calculator.debug("Fehler: Ungültiger Wurzelexponent");
                    }
                } catch (Exception ex) {
                    calculator.debug("Fehler bei Wurzelberechnung: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "(":
            case ")":
                // Add parentheses
                if (aktuellerText.equals("0")) {
                    calculator.setDisplayText(eingabe);
                } else {
                    calculator.setDisplayText(aktuellerText + eingabe);
                }
                calculator.setNeueZahlBegonnen(true);
                calculator.debug("Klammer hinzugefügt: " + calculator.getDisplayText());
                break;

            case ".":
                // Add decimal point if not already present
                if (calculator.isNeueZahlBegonnen()) {
                    calculator.setDisplayText(aktuellerText + "0.");
                    calculator.setNeueZahlBegonnen(false);
                    calculator.debug("Neue Dezimalzahl begonnen: " + aktuellerText + "0.");
                } else {
                    // Check if the current number already has a decimal point
                    String aktuelleZahl = engine.findeAktuelleZahl(aktuellerText);
                    if (!aktuelleZahl.contains(".")) {
                        calculator.setDisplayText(aktuellerText + ".");
                        calculator.debug("Dezimalpunkt hinzugefügt: " + aktuellerText + ".");
                    }
                }
                break;

            case "=":
                calculator.debug("Berechne Formel: " + aktuellerText);
                engine.berechneFormel();
                calculator.setNeueZahlBegonnen(true);
                break;

            case "+/-":
                // Invert sign of current number
                String neuerText = engine.toggleVorzeichen(aktuellerText);
                calculator.setDisplayText(neuerText);
                calculator.debug("Nach +/- Taste: " + neuerText);
                // +/- does not change the isNeueZahlBegonnen status
                break;

            case "C":
                // Reset
                calculator.setDisplayText("0");
                calculator.setNeueZahlBegonnen(true);
                calculator.debug("Display zurückgesetzt");
                break;

            case "←":
                // Return/Backspace function - delete last character
                handleReturnButton(aktuellerText);
                break;
        }
    }

    /**
     * Helper method for the Return/Backspace button
     */
    private void handleReturnButton(String aktuellerText) {
        calculator.debug("Return/Backspace-Taste gedrückt für: " + aktuellerText);

        if (aktuellerText.length() <= 1) {
            // If only one character left or empty, reset to 0
            calculator.setDisplayText("0");
            calculator.setNeueZahlBegonnen(true);
            calculator.debug("Zurückgesetzt auf 0, da keine weiteren Zeichen");
        } else {
            // Remove last character
            String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1);
            calculator.setDisplayText(neuerText);

            // Check if the last character is an operator
            char letzterChar = neuerText.charAt(neuerText.length() - 1);
            if (engine.istOperator(letzterChar)) {
                calculator.setNeueZahlBegonnen(true);
                calculator.debug("Letztes Zeichen gelöscht, Operator erkannt: " + neuerText);
            } else {
                calculator.setNeueZahlBegonnen(false);
                calculator.debug("Letztes Zeichen gelöscht: " + neuerText);
            }
        }
    }
}
