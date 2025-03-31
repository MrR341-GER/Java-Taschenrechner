package core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;

/**
 * Verarbeitet Tastatureingaben für den Taschenrechner
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

        // Logik für unterschiedliche Tastenaktionen
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
                // Falls das Display "0" anzeigt oder eine neue Zahl begonnen wird
                if (aktuellerText.equals("0")) {
                    calculator.setDisplayText(eingabe);
                    calculator.debug("Null ersetzt durch: " + eingabe);
                } else if (calculator.isNeueZahlBegonnen()) {
                    // Wichtige Korrektur: Ersetze nicht den gesamten Ausdruck,
                    // sondern hänge die neue Zahl an den bestehenden Ausdruck an
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
                // Überprüfe, ob das letzte Zeichen bereits ein normaler Operator ist
                if (!aktuellerText.isEmpty()
                        && engine.istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                    // Ersetze den vorherigen Operator durch den neuen
                    String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1) + eingabe;
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Operator ersetzt: " + aktuellerText + " -> " + neuerText);
                } else {
                    // Operator-Taste – nach dem Operator beginnt eine neue Zahl
                    calculator.setDisplayText(aktuellerText + eingabe);
                    calculator.debug("Operator hinzugefügt: " + aktuellerText + eingabe + ", neue Zahl wird erwartet");
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "x^y":
                // Fügt die Potenzfunktion hinzu – wie ein normaler Operator
                if (!aktuellerText.isEmpty()
                        && engine.istNormalerOperator(aktuellerText.charAt(aktuellerText.length() - 1))) {
                    // Ersetze den vorherigen Operator
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
                // Quadratfunktion – fügt ^2 hinzu
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // Ersetze die letzte Zahl durch ihre quadrierte Form
                    String neuerText = engine.ersetzeLetzteZahl(aktuellerText, zahl * zahl);
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Quadriert: " + zahl + "² = " + (zahl * zahl));
                } catch (Exception ex) {
                    calculator.debug("Fehler beim Quadrieren: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "x³":
                // Kubikfunktion – fügt ^3 hinzu
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // Ersetze die letzte Zahl durch ihre kubische Form
                    String neuerText = engine.ersetzeLetzteZahl(aktuellerText, zahl * zahl * zahl);
                    calculator.setDisplayText(neuerText);
                    calculator.debug("Kubiert: " + zahl + "³ = " + (zahl * zahl * zahl));
                } catch (Exception ex) {
                    calculator.debug("Fehler beim Kubieren: " + ex.getMessage());
                }
                calculator.setNeueZahlBegonnen(true);
                break;

            case "√x":
                // Quadratwurzelfunktion
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    if (zahl < 0) {
                        calculator.setDisplayText("Fehler");
                        calculator.debug("Fehler: Wurzel aus negativer Zahl nicht erlaubt");
                    } else {
                        // Ersetze die letzte Zahl durch ihre Quadratwurzel
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
                // Kubikwurzelfunktion
                try {
                    double zahl = engine.evaluiereLetzteTeilzahl(aktuellerText);
                    // Bei Kubikwurzeln ist die Berechnung auch für negative Zahlen möglich
                    // Ersetze die letzte Zahl durch ihre Kubikwurzel
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
                // y-te Wurzel aus x
                try {
                    // Für diesen Vorgang benötigen wir zwei Zahlen:
                    // - y (den Wurzelexponenten – wird per Popup abgefragt)
                    // - x (die Zahl, aus der die Wurzel gezogen werden soll – die aktuelle Zahl)
                    double x = engine.evaluiereLetzteTeilzahl(aktuellerText);

                    if (x < 0) {
                        calculator.setDisplayText("Fehler");
                        calculator.debug("Fehler: Wurzel aus negativer Zahl ist nur für ungerade Wurzeln definiert");
                        return;
                    }

                    // Frage nach dem Wurzelexponenten y
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

                        // Berechnung: x^(1/y)
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
                // Füge Klammern hinzu
                if (aktuellerText.equals("0")) {
                    calculator.setDisplayText(eingabe);
                } else {
                    calculator.setDisplayText(aktuellerText + eingabe);
                }
                calculator.setNeueZahlBegonnen(true);
                calculator.debug("Klammer hinzugefügt: " + calculator.getDisplayText());
                break;

            case ".":
                // Füge einen Dezimalpunkt hinzu, falls noch nicht vorhanden
                if (calculator.isNeueZahlBegonnen()) {
                    calculator.setDisplayText(aktuellerText + "0.");
                    calculator.setNeueZahlBegonnen(false);
                    calculator.debug("Neue Dezimalzahl begonnen: " + aktuellerText + "0.");
                } else {
                    // Überprüfe, ob die aktuelle Zahl bereits einen Dezimalpunkt enthält
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
                // Invertiere das Vorzeichen der aktuellen Zahl
                String neuerText = engine.toggleVorzeichen(aktuellerText);
                calculator.setDisplayText(neuerText);
                calculator.debug("Nach +/- Taste: " + neuerText);
                // +/– ändert nicht den Status von isNeueZahlBegonnen
                break;

            case "C":
                // Zurücksetzen
                calculator.setDisplayText("0");
                calculator.setNeueZahlBegonnen(true);
                calculator.debug("Display zurückgesetzt");
                break;

            case "←":
                // Rücktaste/Backspace-Funktion – lösche das letzte Zeichen
                handleReturnButton(aktuellerText);
                break;
        }
    }

    /**
     * Hilfsmethode für die Rücktaste/Backspace-Taste
     */
    private void handleReturnButton(String aktuellerText) {
        calculator.debug("Return/Backspace-Taste gedrückt für: " + aktuellerText);

        if (aktuellerText.length() <= 1) {
            // Wenn nur ein Zeichen übrig ist oder der Ausdruck leer ist, auf 0 zurücksetzen
            calculator.setDisplayText("0");
            calculator.setNeueZahlBegonnen(true);
            calculator.debug("Zurückgesetzt auf 0, da keine weiteren Zeichen");
        } else {
            // Lösche das letzte Zeichen
            String neuerText = aktuellerText.substring(0, aktuellerText.length() - 1);
            calculator.setDisplayText(neuerText);

            // Überprüfe, ob das letzte Zeichen ein Operator ist
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
