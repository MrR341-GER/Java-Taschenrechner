import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Engine for mathematical calculations in the calculator
 */
public class CalculationEngine {
    private final Taschenrechner calculator;

    public CalculationEngine(Taschenrechner calculator) {
        this.calculator = calculator;
    }

    /**
     * Calculates the result of a formula
     */
    public void berechneFormel() {
        try {
            String formel = calculator.getDisplayText().trim();

            // Check if formula is empty
            if (formel.isEmpty()) {
                calculator.setDisplayText("0");
                calculator.debug("Leere Formel, auf 0 zurückgesetzt");
                return;
            }

            calculator.debug("Originale Formel: " + formel);

            // Simple preprocessing - correctly handle double operators
            // --3 becomes +3, +-3 remains -3
            formel = formel.replace("--", "+");
            formel = formel.replaceAll("\\+\\+", "+");
            formel = formel.replace("+-", "-");

            // Handle implicit multiplications (e.g. 2(5+5) to 2*(5+5))
            formel = ergaenzeImpliziteMultiplikationen(formel);

            calculator.debug("Vorverarbeitete Formel: " + formel);

            // Calculate the result
            double ergebnis = berechneAusdruck(formel);
            calculator.debug("Berechnetes Ergebnis: " + ergebnis);

            // Format the result
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

            // Add to history
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
     * Helper method for handling implicit multiplications
     */
    public String ergaenzeImpliziteMultiplikationen(String formel) {
        calculator.debug("Prüfe auf implizite Multiplikationen in: " + formel);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < formel.length(); i++) {
            char aktuellesZeichen = formel.charAt(i);
            result.append(aktuellesZeichen);

            // If the current character is not the last one
            if (i < formel.length() - 1) {
                char naechstesZeichen = formel.charAt(i + 1);

                // Case 1: Number followed by opening parenthesis -> Insert multiplication sign
                if (Character.isDigit(aktuellesZeichen) && naechstesZeichen == '(') {
                    result.append('*');
                    calculator.debug("Implizite Multiplikation erkannt: Zahl(" + aktuellesZeichen + ") vor Klammer");
                }

                // Case 2: Closing parenthesis followed by number -> Insert multiplication sign
                else if (aktuellesZeichen == ')' && Character.isDigit(naechstesZeichen)) {
                    result.append('*');
                    calculator.debug("Implizite Multiplikation erkannt: Klammer vor Zahl(" + naechstesZeichen + ")");
                }

                // Case 3: Closing parenthesis followed by opening parenthesis -> Insert
                // multiplication sign
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
     * Custom implementation of a simple expression parser
     */
    public double berechneAusdruck(String ausdruck) {
        calculator.debug("Berechne Ausdruck: " + ausdruck);

        // Evaluate parentheses first
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

        // Addition and subtraction
        ArrayList<Double> zahlen = new ArrayList<>();
        ArrayList<Character> operatoren = new ArrayList<>();

        // Split into numbers and operators
        StringBuilder aktuelleZahl = new StringBuilder();
        boolean istErsteZahl = true;
        boolean letzteWarOperator = false;

        for (int i = 0; i < ausdruck.length(); i++) {
            char c = ausdruck.charAt(i);

            if (c == '+' || c == '-') {
                // If it's the first number or comes after an operator, it's a sign
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

        // First calculate ^ (power)
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

        // Then calculate * and /
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

        // Then calculate + and -
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
     * Finds the current number being worked on
     */
    public String findeAktuelleZahl(String ausdruck) {
        // If the expression is empty or consists only of operators
        if (ausdruck.isEmpty() || ausdruck.matches("[+\\-*/()^]+")) {
            return "";
        }

        // Regular expression to find the last number
        // Looks for a number (with or without decimal point) at the end of the
        // expression
        Pattern pattern = Pattern.compile("[0-9]+(\\.[0-9]*)?$");
        Matcher matcher = pattern.matcher(ausdruck);

        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    /**
     * Helper method to evaluate the last partial number in the expression
     */
    public double evaluiereLetzteTeilzahl(String ausdruck) {
        String letzteZahl = findeAktuelleZahl(ausdruck);
        if (letzteZahl.isEmpty()) {
            throw new IllegalArgumentException("Keine gültige Zahl gefunden");
        }
        return Double.parseDouble(letzteZahl);
    }

    /**
     * Helper method to replace the last number in the expression
     */
    public String ersetzeLetzteZahl(String ausdruck, double neuerWert) {
        String letzteZahl = findeAktuelleZahl(ausdruck);
        if (letzteZahl.isEmpty()) {
            return ausdruck + neuerWert;
        }

        // Format the new value
        String formatierterWert;
        if (neuerWert == (int) neuerWert) {
            formatierterWert = String.valueOf((int) neuerWert);
        } else {
            formatierterWert = String.valueOf(neuerWert);
        }

        // Replace the last number with the new value
        return ausdruck.substring(0, ausdruck.length() - letzteZahl.length()) + formatierterWert;
    }

    /**
     * Helper method to flip the sign of the current number (explicitly show as +/-)
     */
    public String toggleVorzeichen(String ausdruck) {
        calculator.debug("Toggle-Vorzeichen für Ausdruck: " + ausdruck);

        // Empty expression or just 0
        if (ausdruck.isEmpty() || ausdruck.equals("0")) {
            return "0";
        }

        // If we're just starting a new number (after an operator)
        if (calculator.isNeueZahlBegonnen()) {
            // Check if the last character is an operator
            char letzterChar = ausdruck.charAt(ausdruck.length() - 1);
            if (istOperator(letzterChar)) {
                // Add an explicit minus sign
                return ausdruck + "-";
            }
        }

        // For an existing calculation, we analyze the expression to find the last
        // number

        // One way would be to go back from the end of the string until we find an
        // operator
        int letztesZeichenPos = ausdruck.length() - 1;

        // First check if we have digits at the end (normal number)
        while (letztesZeichenPos >= 0 &&
                (Character.isDigit(ausdruck.charAt(letztesZeichenPos)) ||
                        ausdruck.charAt(letztesZeichenPos) == '.')) {
            letztesZeichenPos--;
        }

        // If we're at the beginning of the expression or there's an operator before the
        // number
        if (letztesZeichenPos < 0 || istOperator(ausdruck.charAt(letztesZeichenPos))) {
            // Check if there's a minus sign before the number that belongs to the number
            if (letztesZeichenPos >= 0 && ausdruck.charAt(letztesZeichenPos) == '-') {
                // There's a minus sign - check if it's a sign or an operator
                if (letztesZeichenPos == 0 || istOperator(ausdruck.charAt(letztesZeichenPos - 1))) {
                    // It's a sign - remove it
                    return ausdruck.substring(0, letztesZeichenPos) + ausdruck.substring(letztesZeichenPos + 1);
                }
            }

            // No minus sign - add one
            if (letztesZeichenPos < 0) {
                // The entire number is negative
                return "-" + ausdruck;
            } else {
                // Insert a minus sign after the operator
                return ausdruck.substring(0, letztesZeichenPos + 1) + "-" + ausdruck.substring(letztesZeichenPos + 1);
            }
        }

        // For more complex cases (with parentheses etc.)
        calculator.debug("Kein einfacher Fall erkannt, toggle nicht möglich");
        return ausdruck;
    }

    /**
     * Helper method to check if a character is an operator
     */
    public boolean istOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == '^';
    }

    /**
     * Helper method to check if a character is a normal operator (without
     * parentheses)
     */
    public boolean istNormalerOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
}
