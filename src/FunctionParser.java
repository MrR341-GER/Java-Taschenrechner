/**
 * Parser für mathematische Funktionen
 * Diese Klasse verwendet Rekursiven Abstieg zum Parsen von Ausdrücken
 * Unterstützt implizite Multiplikation (2x statt 2*x)
 */
public class FunctionParser {
    private final String expression;
    private int pos;
    private char ch;
    private char nextCh; // Speichert das nächste Zeichen für Look-ahead

    public FunctionParser(String expression) {
        this.expression = expression.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Wertet die Funktion an einer bestimmten Stelle x aus
     */
    public double evaluateAt(double x) {
        pos = 0;
        nextChar();
        double result = parseExpression(x);

        if (pos < expression.length()) {
            throw new RuntimeException("Unexpected character: " + ch);
        }

        return result;
    }

    private void nextChar() {
        ch = (pos < expression.length()) ? expression.charAt(pos++) : '\0';
        // Speichere auch das folgende Zeichen (ohne Position zu erhöhen)
        nextCh = (pos < expression.length()) ? expression.charAt(pos) : '\0';
    }

    private boolean eat(char charToEat) {
        while (ch == ' ')
            nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parseExpression(double x) {
        double result = parseTerm(x);

        while (true) {
            if (eat('+'))
                result += parseTerm(x);
            else if (eat('-'))
                result -= parseTerm(x);
            else
                return result;
        }
    }

    private double parseTerm(double x) {
        double result = parseFactor(x);

        while (true) {
            if (eat('*'))
                result *= parseFactor(x);
            else if (eat('/')) {
                double divisor = parseFactor(x);
                if (Math.abs(divisor) < 1e-10) {
                    throw new ArithmeticException("Division by zero");
                }
                result /= divisor;
            }
            // Implizite Multiplikation für Fälle wie "2x" oder "2(x+1)" oder "x(y)"
            else if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                // Implizite Multiplikation erkannt - multiply mit dem nächsten Faktor
                result *= parseFactor(x);
            } else
                return result;
        }
    }

    private double parseFactor(double x) {
        if (eat('+'))
            return parseFactor(x);
        if (eat('-'))
            return -parseFactor(x);

        double result;

        // Klammern
        if (eat('(')) {
            result = parseExpression(x);
            eat(')');

            // Nach schließender Klammer prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                result *= parseFactor(x);
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
                result *= parseFactor(x);
            }
        }
        // Die Variable x
        else if (ch == 'x') {
            nextChar();
            result = x;

            // Nach 'x' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x') || ch == '(') {
                result *= parseFactor(x);
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
                result = parseExpression(x);
                eat(')');

                // Bekannte Funktionen auswerten
                switch (name) {
                    case "sin":
                        result = Math.sin(result);
                        break;
                    case "cos":
                        result = Math.cos(result);
                        break;
                    case "tan":
                        result = Math.tan(result);
                        break;
                    case "sqrt":
                        if (result < 0)
                            throw new ArithmeticException("Square root of negative number");
                        result = Math.sqrt(result);
                        break;
                    case "log":
                        if (result <= 0)
                            throw new ArithmeticException("Logarithm of non-positive number");
                        result = Math.log10(result);
                        break;
                    case "ln":
                        if (result <= 0)
                            throw new ArithmeticException("Natural logarithm of non-positive number");
                        result = Math.log(result);
                        break;
                    case "abs":
                        result = Math.abs(result);
                        break;
                    case "exp":
                        result = Math.exp(result);
                        break;
                    // Neue Funktionen
                    case "asin":
                    case "arcsin":
                        if (result < -1 || result > 1)
                            throw new ArithmeticException("Arcsin argument out of range [-1, 1]");
                        result = Math.asin(result);
                        break;
                    case "acos":
                    case "arccos":
                        if (result < -1 || result > 1)
                            throw new ArithmeticException("Arccos argument out of range [-1, 1]");
                        result = Math.acos(result);
                        break;
                    case "atan":
                    case "arctan":
                        result = Math.atan(result);
                        break;
                    case "sinh":
                        result = Math.sinh(result);
                        break;
                    case "cosh":
                        result = Math.cosh(result);
                        break;
                    case "tanh":
                        result = Math.tanh(result);
                        break;
                    case "log2":
                        if (result <= 0)
                            throw new ArithmeticException("Logarithm of non-positive number");
                        result = Math.log(result) / Math.log(2);
                        break;
                    case "floor":
                        result = Math.floor(result);
                        break;
                    case "ceil":
                    case "ceiling":
                        result = Math.ceil(result);
                        break;
                    case "round":
                        result = Math.round(result);
                        break;
                    case "cbrt":
                        result = Math.cbrt(result);
                        break;
                    case "degrees":
                    case "deg":
                        result = Math.toDegrees(result);
                        break;
                    case "radians":
                    case "rad":
                        result = Math.toRadians(result);
                        break;
                    default:
                        throw new RuntimeException("Unknown function: " + name);
                }

                // Nach einer Funktionsauswertung prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                    result *= parseFactor(x);
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
                    case "x":
                        result = x;
                        break;
                    // Neue Konstanten
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
                    result *= parseFactor(x);
                }
            }
        } else {
            throw new RuntimeException("Unexpected: " + ch);
        }

        // Exponentation (Potenzen)
        if (eat('^')) {
            result = Math.pow(result, parseFactor(x));
        }

        return result;
    }
}