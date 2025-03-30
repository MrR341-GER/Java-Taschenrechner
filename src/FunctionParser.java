/**
 * Parser für mathematische Funktionen mit einer Variablen (x)
 * Erbt von AbstractExpressionParser für gemeinsame Parsing-Funktionalität
 */
public class FunctionParser extends AbstractExpressionParser {
    // Aktueller x-Wert für die Auswertung
    private double currentX;

    /**
     * Erstellt einen neuen Funktionsparser
     */
    public FunctionParser(String expression) {
        super(expression);
    }

    /**
     * Wertet die Funktion an einer bestimmten Stelle x aus
     */
    public double evaluateAt(double x) {
        pos = 0;
        nextChar();
        this.currentX = x;
        double result = parseExpression();

        if (pos < expression.length()) {
            throw new RuntimeException("Unexpected character: " + ch);
        }

        return result;
    }

    @Override
    protected double parseTerm() {
        double result = parseFactor();

        while (true) {
            if (eat('*'))
                result *= parseFactor();
            else if (eat('/')) {
                double divisor = parseFactor();
                if (Math.abs(divisor) < 1e-10) {
                    throw new ArithmeticException("Division by zero");
                }
                result /= divisor;
            }
            // Implizite Multiplikation für Fälle wie "2x" oder "2(x+1)"
            else if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                // Implizite Multiplikation erkannt - multiply mit dem nächsten Faktor
                result *= parseFactor();
            } else
                return result;
        }
    }

    @Override
    protected double parseFactor() {
        if (eat('+'))
            return parseFactor();
        if (eat('-'))
            return -parseFactor();

        double result;

        // Klammern
        if (eat('(')) {
            result = parseExpression();
            eat(')');

            // Nach schließender Klammer prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                result *= parseFactor();
            }
        }
        // Zahlen
        else if ((ch >= '0' && ch <= '9') || ch == '.') {
            StringBuilder sb = new StringBuilder();
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                sb.append(ch);
                nextChar();
            }
            result = Double.parseDouble(sb.toString());

            // Nach einer Zahl prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                result *= parseFactor();
            }
        }
        // Die Variable x
        else if (ch == 'x') {
            nextChar();
            result = currentX;

            // Nach 'x' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x') || ch == '(') {
                result *= parseFactor();
            }
        }
        // Funktionen wie sin, cos, etc.
        else if (ch >= 'a' && ch <= 'z') {
            StringBuilder funcName = new StringBuilder();
            while (ch >= 'a' && ch <= 'z') {
                funcName.append(ch);
                nextChar();
            }

            String name = funcName.toString();
            if (eat('(')) {
                result = parseExpression();
                eat(')');

                // Bekannte Funktionen auswerten
                result = evaluateFunction(name, result);

                // Nach einer Funktionsauswertung prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                    result *= parseFactor();
                }
            } else {
                // Mathematische Konstanten
                switch (name) {
                    case "pi":
                        result = Math.PI;
                        break;
                    case "e":
                        result = Math.E;
                        break;
                    case "phi":
                    case "golden":
                        result = (1 + Math.sqrt(5)) / 2; // Goldener Schnitt (≈ 1.618033988749895)
                        break;
                    case "sqrt2":
                        result = Math.sqrt(2);
                        break;
                    case "sqrt3":
                        result = Math.sqrt(3);
                        break;
                    case "inf":
                    case "infinity":
                        result = Double.POSITIVE_INFINITY;
                        break;
                    case "nan":
                        result = Double.NaN;
                        break;
                    default:
                        throw new RuntimeException("Unknown identifier: " + name);
                }

                // Nach einer Konstanten prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z' && !name.equals("x")) || ch == '(') {
                    result *= parseFactor();
                }
            }
        } else {
            throw new RuntimeException("Unexpected: " + ch);
        }

        // Exponentation (Potenzen)
        if (eat('^')) {
            result = Math.pow(result, parseFactor());
        }

        return result;
    }
}
