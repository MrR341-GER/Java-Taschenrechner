package core;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Rechen-Engine für mathematische Berechnungen im Taschenrechner
 */
public class CalculationEngine {
    private final Taschenrechner calculator;
    // Muster zur Erkennung von Funktionen (enthält 'x' und ggf. weitere
    // mathematische Operationen)
    private final Pattern functionPattern = Pattern.compile(".*[a-zA-Z&&[^eE]].*");

    public CalculationEngine(Taschenrechner calculator) {
        this.calculator = calculator;
    }

    /**
     * Berechnet das Ergebnis einer Formel
     */
    public void berechneFormel() {
        try {
            String formel = calculator.getDisplayText().trim();

            // Überprüfe, ob die Formel leer ist
            if (formel.isEmpty()) {
                calculator.setDisplayText("0");
                calculator.debug("Leere Formel, auf 0 zurückgesetzt");
                return;
            }

            calculator.debug("Originale Formel: " + formel);

            // Prüfe, ob die Formel eine Funktion sein könnte
            if (checkIfFunction(formel)) {
                calculator.debug("Mögliche Funktion erkannt: " + formel);
                // Frage den Benutzer, ob er die Funktion plotten möchte
                askToPlotFunction(formel);
                return;
            }

            // Einfache Vorverarbeitung – behandelt korrekt doppelte Operatoren
            // "--3" wird zu "+3", "+-3" bleibt "-3"
            formel = formel.replace("--", "+");
            formel = formel.replaceAll("\\+\\+", "+");
            formel = formel.replace("+-", "-");

            // Behandle implizite Multiplikationen (z.B. 2(5+5) wird zu 2*(5+5))
            formel = ergaenzeImpliziteMultiplikationen(formel);

            calculator.debug("Vorverarbeitete Formel: " + formel);

            // Berechne das Ergebnis
            double ergebnis = berechneAusdruck(formel);
            calculator.debug("Berechnetes Ergebnis: " + ergebnis);

            // Formatiere das Ergebnis
            String ergebnisText;
            if (ergebnis == (int) ergebnis) {
                ergebnisText = String.valueOf((int) ergebnis);
                calculator.setDisplayText(ergebnisText);
                calculator.debug("Formatiertes Ergebnis (int): " + ergebnisText);
            } else {
                ergebnisText = String.valueOf(ergebnis);
                calculator.setDisplayText(ergebnisText);
                calculator.debug("Formatiertes Ergebnis (double): " + ergebnisText);
            }

            // Zur Verlaufsliste hinzufügen
            calculator.addToHistory(formel, ergebnisText);

        } catch (Exception e) {
            calculator.setDisplayText("Fehler");
            calculator.debug("Berechnungsfehler: " + e.getMessage());
            calculator.debug("Stack: " + e.getStackTrace()[0]);
            for (int i = 0; i < Math.min(3, e.getStackTrace().length); i++) {
                calculator.debug("  bei " + e.getStackTrace()[i]);
            }
        }
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
            return matcher.matches();
        }
    }

    /**
     * Fragt den Benutzer, ob er die erkannte Funktion oder Konstante plotten möchte
     */
    private void askToPlotFunction(String function) {
        // Bestimme, ob es sich um eine Konstante oder Funktion handelt
        boolean isConstant = isConstantExpression(function);

        // Variable zur Bestimmung, ob es eine 3D-Funktion sein könnte:
        // Konstanten sind immer 2D, ansonsten prüfen, ob 'y' im Ausdruck vorkommt
        final boolean could3D = !isConstant && function.contains("y");

        String message;
        String title;

        if (isConstant) {
            // Angepasste Nachricht für Konstanten
            message = "Die Eingabe \"" + function + "\" ist ein konstanter Wert.\n" +
                    "Möchten Sie diesen Wert als horizontale Linie im Funktionsplotter darstellen?";
            title = "Konstante plotten?";
        } else if (could3D) {
            // Nachricht für 3D-Funktionen
            message = "Die Eingabe \"" + function + "\" enthält die Variable y und könnte eine 3D-Funktion sein.\n" +
                    "Möchten Sie diese Funktion im 3D-Funktionsplotter zeichnen?";
            title = "3D-Funktion plotten?";
        } else {
            // Nachricht für 2D-Funktionen
            message = "Die Eingabe \"" + function + "\" sieht wie eine Funktion aus.\n" +
                    "Möchten Sie diese Funktion im Funktionsplotter zeichnen?";
            title = "Funktion plotten?";
        }

        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(
                    calculator,
                    message,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                // Wenn der Benutzer zustimmt, Funktion an den Plotter übergeben
                if (calculator instanceof GrafischerTaschenrechner) {
                    GrafischerTaschenrechner graphCalc = (GrafischerTaschenrechner) calculator;

                    if (could3D) {
                        graphCalc.transferFunctionTo3DPlotter(function);
                    } else {
                        graphCalc.transferFunctionToPlotter(function);
                    }
                } else {
                    calculator.debug("Konnte Funktion nicht übertragen: Keine Instanz von GrafischerTaschenrechner");
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
     * Überträgt die Funktion zum Plotter
     */
    private void transferToPlotter(String function) {
        calculator.debug("Übertrage Funktion zum Plotter: " + function);

        // Prüfen, ob eine Instanz von GrafischerTaschenrechner vorliegt
        if (calculator instanceof GrafischerTaschenrechner) {
            GrafischerTaschenrechner graphCalc = (GrafischerTaschenrechner) calculator;
            graphCalc.transferFunctionToPlotter(function);
        } else {
            calculator.debug("Konnte Funktion nicht übertragen: Keine Instanz von GrafischerTaschenrechner");
        }
    }

    /**
     * Hilfsmethode zur Behandlung impliziter Multiplikationen
     */
    public String ergaenzeImpliziteMultiplikationen(String formel) {
        calculator.debug("Prüfe auf implizite Multiplikationen in: " + formel);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < formel.length(); i++) {
            char aktuellesZeichen = formel.charAt(i);
            result.append(aktuellesZeichen);

            // Falls das aktuelle Zeichen nicht das letzte ist
            if (i < formel.length() - 1) {
                char naechstesZeichen = formel.charAt(i + 1);

                // Fall 1: Zahl gefolgt von einer öffnenden Klammer -> Multiplikationszeichen
                // einfügen
                if (Character.isDigit(aktuellesZeichen) && naechstesZeichen == '(') {
                    result.append('*');
                    calculator.debug("Implizite Multiplikation erkannt: Zahl(" + aktuellesZeichen + ") vor Klammer");
                }

                // Fall 2: Schließende Klammer gefolgt von einer Zahl -> Multiplikationszeichen
                // einfügen
                else if (aktuellesZeichen == ')' && Character.isDigit(naechstesZeichen)) {
                    result.append('*');
                    calculator.debug("Implizite Multiplikation erkannt: Klammer vor Zahl(" + naechstesZeichen + ")");
                }

                // Fall 3: Schließende Klammer gefolgt von einer öffnenden Klammer ->
                // Multiplikationszeichen einfügen
                else if (aktuellesZeichen == ')' && naechstesZeichen == '(') {
                    result.append('*');
                    calculator.debug("Implizite Multiplikation erkannt: Klammer vor Klammer");
                }
            }
        }

        String resultString = result.toString();
        if (!resultString.equals(formel)) {
            calculator.debug("Implizite Multiplikation umgewandelt: " + formel + " -> " + resultString);
        }

        return resultString;
    }

    /**
     * Eigene Implementierung eines einfachen Ausdrucksparsers
     */
    public double berechneAusdruck(String ausdruck) {
        calculator.debug("Berechne Ausdruck: " + ausdruck);

        // Zuerst Klammern auswerten
        while (ausdruck.contains("(")) {
            int offen = ausdruck.lastIndexOf("(");
            int geschlossen = ausdruck.indexOf(")", offen);

            if (geschlossen == -1) {
                throw new IllegalArgumentException("Fehlende schließende Klammer");
            }

            String subAusdruck = ausdruck.substring(offen + 1, geschlossen);
            calculator.debug("Gefundener Teilausdruck: " + subAusdruck);

            double teilErgebnis = berechneAusdruck(subAusdruck);
            calculator.debug("Teilausdruck ergibt: " + teilErgebnis);

            ausdruck = ausdruck.substring(0, offen) + teilErgebnis + ausdruck.substring(geschlossen + 1);
            calculator.debug("Ausdruck nach Klammer-Ersetzung: " + ausdruck);
        }

        // Addition und Subtraktion
        ArrayList<Double> zahlen = new ArrayList<>();
        ArrayList<Character> operatoren = new ArrayList<>();

        // Aufteilen in Zahlen und Operatoren
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

        calculator.debug("Zahlen: " + zahlen);
        calculator.debug("Operatoren: " + operatoren);

        // Zuerst berechne ^ (Potenz)
        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '^') {
                double ergebnis;
                double basis = zahlen.get(i);
                double exponent = zahlen.get(i + 1);

                ergebnis = Math.pow(basis, exponent);
                calculator.debug("Potenz: " + basis + " ^ " + exponent + " = " + ergebnis);

                zahlen.set(i, ergebnis);
                zahlen.remove(i + 1);
                operatoren.remove(i);
                i--;
            }
        }

        // Danach berechne * und /
        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '*' || operatoren.get(i) == '/') {
                double ergebnis;
                double a = zahlen.get(i);
                double b = zahlen.get(i + 1);

                if (operatoren.get(i) == '*') {
                    ergebnis = a * b;
                    calculator.debug("Multiplikation: " + a + " * " + b + " = " + ergebnis);
                } else {
                    if (b == 0) {
                        throw new ArithmeticException("Division durch Null");
                    }
                    ergebnis = a / b;
                    calculator.debug("Division: " + a + " / " + b + " = " + ergebnis);
                }

                zahlen.set(i, ergebnis);
                zahlen.remove(i + 1);
                operatoren.remove(i);
                i--;
            }
        }

        // Danach berechne + und -
        double ergebnis = zahlen.get(0);

        for (int i = 0; i < operatoren.size(); i++) {
            if (operatoren.get(i) == '+') {
                ergebnis += zahlen.get(i + 1);
                calculator.debug(
                        "Addition: " + ergebnis + " + " + zahlen.get(i + 1) + " = " + (ergebnis + zahlen.get(i + 1)));
            } else if (operatoren.get(i) == '-') {
                ergebnis -= zahlen.get(i + 1);
                calculator.debug("Subtraktion: " + ergebnis + " - " + zahlen.get(i + 1) + " = "
                        + (ergebnis - zahlen.get(i + 1)));
            }
        }

        calculator.debug("Endergebnis des Ausdrucks: " + ergebnis);
        return ergebnis;
    }

    /**
     * Findet die aktuelle Zahl, an der gearbeitet wird
     */
    public String findeAktuelleZahl(String ausdruck) {
        // Falls der Ausdruck leer ist oder nur aus Operatoren besteht
        if (ausdruck.isEmpty() || ausdruck.matches("[+\\-*/()^]+")) {
            return "";
        }

        // Regulärer Ausdruck, um die letzte Zahl zu finden
        // Sucht eine Zahl (mit oder ohne Dezimalpunkt) am Ende des Ausdrucks
        Pattern pattern = Pattern.compile("[0-9]+(\\.[0-9]*)?$");
        Matcher matcher = pattern.matcher(ausdruck);

        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    /**
     * Hilfsmethode zur Auswertung der letzten Teilszahl im Ausdruck
     */
    public double evaluiereLetzteTeilzahl(String ausdruck) {
        String letzteZahl = findeAktuelleZahl(ausdruck);
        if (letzteZahl.isEmpty()) {
            throw new IllegalArgumentException("Keine gültige Zahl gefunden");
        }
        return Double.parseDouble(letzteZahl);
    }

    /**
     * Hilfsmethode zum Ersetzen der letzten Zahl im Ausdruck
     */
    public String ersetzeLetzteZahl(String ausdruck, double neuerWert) {
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

    /**
     * Hilfsmethode, um das Vorzeichen der aktuellen Zahl zu ändern (explizit als
     * +/- anzeigen)
     */
    public String toggleVorzeichen(String ausdruck) {
        calculator.debug("Toggle-Vorzeichen für Ausdruck: " + ausdruck);

        // Leerer Ausdruck oder nur 0
        if (ausdruck.isEmpty() || ausdruck.equals("0")) {
            return "0";
        }

        // Falls wir gerade eine neue Zahl beginnen (nach einem Operator)
        if (calculator.isNeueZahlBegonnen()) {
            // Überprüfe, ob das letzte Zeichen ein Operator ist
            char letzterChar = ausdruck.charAt(ausdruck.length() - 1);
            if (istOperator(letzterChar)) {
                // Füge ein explizites Minuszeichen hinzu
                return ausdruck + "-";
            }
        }

        // Bei einer bestehenden Berechnung analysieren wir den Ausdruck, um die letzte
        // Zahl zu finden

        // Eine Möglichkeit besteht darin, vom Ende des Strings zurückzugehen, bis ein
        // Operator gefunden wird
        int letztesZeichenPos = ausdruck.length() - 1;

        // Zuerst prüfen, ob am Ende Ziffern stehen (normale Zahl)
        while (letztesZeichenPos >= 0 &&
                (Character.isDigit(ausdruck.charAt(letztesZeichenPos)) ||
                        ausdruck.charAt(letztesZeichenPos) == '.')) {
            letztesZeichenPos--;
        }

        // Falls wir am Anfang des Ausdrucks sind oder vor der Zahl ein Operator steht
        if (letztesZeichenPos < 0 || istOperator(ausdruck.charAt(letztesZeichenPos))) {
            // Überprüfe, ob vor der Zahl ein Minuszeichen steht, das zur Zahl gehört
            if (letztesZeichenPos >= 0 && ausdruck.charAt(letztesZeichenPos) == '-') {
                // Es gibt ein Minuszeichen – prüfe, ob es ein Vorzeichen oder ein Operator ist
                if (letztesZeichenPos == 0 || istOperator(ausdruck.charAt(letztesZeichenPos - 1))) {
                    // Es ist ein Vorzeichen – entferne es
                    return ausdruck.substring(0, letztesZeichenPos) + ausdruck.substring(letztesZeichenPos + 1);
                }
            }

            // Kein Minuszeichen – füge eines hinzu
            if (letztesZeichenPos < 0) {
                // Die gesamte Zahl ist negativ
                return "-" + ausdruck;
            } else {
                // Füge nach dem Operator ein Minuszeichen ein
                return ausdruck.substring(0, letztesZeichenPos + 1) + "-" + ausdruck.substring(letztesZeichenPos + 1);
            }
        }

        // Für komplexere Fälle (mit Klammern etc.)
        calculator.debug("Kein einfacher Fall erkannt, toggle nicht möglich");
        return ausdruck;
    }

    /**
     * Hilfsmethode, um zu prüfen, ob ein Zeichen ein Operator ist
     */
    public boolean istOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == '^';
    }

    /**
     * Hilfsmethode, um zu prüfen, ob ein Zeichen ein normaler Operator ist (ohne
     * Klammern)
     */
    public boolean istNormalerOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
}
