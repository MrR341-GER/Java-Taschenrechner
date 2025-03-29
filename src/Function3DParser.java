import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser für mathematische Funktionen mit zwei Variablen (x und y)
 * Unterstützt implizite Multiplikation (2x statt 2*x) und standardmäßige mathematische Funktionen
 */
public class Function3DParser {
    private final String expression;
    private int pos;
    private char ch;
    private char nextCh; // Speichert das nächste Zeichen für Look-ahead

    // Mapping für Funktionen und Anzahl ihrer Argumente
    private static final Map<String, Integer> FUNCTIONS = new HashMap<>();
    
    static {
        // Mathematische Standardfunktionen
        FUNCTIONS.put("sin", 1);
        FUNCTIONS.put("cos", 1);
        FUNCTIONS.put("tan", 1);
        FUNCTIONS.put("asin", 1);
        FUNCTIONS.put("acos", 1);
        FUNCTIONS.put("atan", 1);
        FUNCTIONS.put("atan2", 2);
        FUNCTIONS.put("sinh", 1);
        FUNCTIONS.put("cosh", 1);
        FUNCTIONS.put("tanh", 1);
        FUNCTIONS.put("sqrt", 1);
        FUNCTIONS.put("cbrt", 1);
        FUNCTIONS.put("log", 1);
        FUNCTIONS.put("log10", 1);
        FUNCTIONS.put("log2", 1);
        FUNCTIONS.put("exp", 1);
        FUNCTIONS.put("abs", 1);
        FUNCTIONS.put("max", 2);
        FUNCTIONS.put("min", 2);
        FUNCTIONS.put("pow", 2);
    }

    public Function3DParser(String expression) {
        this.expression = expression.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Wertet die Funktion an einer bestimmten Stelle (x,y) aus
     */
    public double evaluateAt(double x, double y) {
        pos = 0;
        nextChar();
        double result = parseExpression(x, y);

        if (pos < expression.length()) {
            throw new RuntimeException("Unerwartetes Zeichen: " + ch);
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

    private double parseExpression(double x, double y) {
        double result = parseTerm(x, y);

        while (true) {
            if (eat('+'))
                result += parseTerm(x, y);
            else if (eat('-'))
                result -= parseTerm(x, y);
            else
                return result;
        }
    }

    private double parseTerm(double x, double y) {
        double result = parseFactor(x, y);

        while (true) {
            if (eat('*'))
                result *= parseFactor(x, y);
            else if (eat('/')) {
                double divisor = parseFactor(x, y);
                if (Math.abs(divisor) < 1e-10) {
                    throw new ArithmeticException("Division durch Null");
                }
                result /= divisor;
            }
            // Implizite Multiplikation für Fälle wie "2x" oder "2(x+1)" oder "x(y)"
            else if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                // Implizite Multiplikation erkannt - multiply mit dem nächsten Faktor
                result *= parseFactor(x, y);
            } else
                return result;
        }
    }

    private double parseFactor(double x, double y) {
        if (eat('+'))
            return parseFactor(x, y);
        if (eat('-'))
            return -parseFactor(x, y);

        double result;

        // Klammern
        if (eat('(')) {
            result = parseExpression(x, y);
            eat(')');

            // Nach schließender Klammer prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                result *= parseFactor(x, y);
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
                result *= parseFactor(x, y);
            }
        }
        // Die Variable x oder y
        else if (ch == 'x') {
            nextChar();
            result = x;

            // Nach 'x' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x' && ch != 'y') || ch == '(') {
                result *= parseFactor(x, y);
            }
        }
        else if (ch == 'y') {
            nextChar();
            result = y;

            // Nach 'y' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x' && ch != 'y') || ch == '(') {
                result *= parseFactor(x, y);
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
                // Prüfe, ob es sich um eine bekannte Funktion handelt
                if (FUNCTIONS.containsKey(name)) {
                    int argCount = FUNCTIONS.get(name);
                    
                    if (argCount == 1) {
                        // Einstellige Funktion
                        double arg = parseExpression(x, y);
                        eat(')');
                        result = evaluateFunction(name, arg);
                    } else if (argCount == 2) {
                        // Zweistellige Funktion
                        double arg1 = parseExpression(x, y);
                        if (!eat(',')) {
                            throw new RuntimeException("Erwarte zweites Argument für Funktion " + name);
                        }
                        double arg2 = parseExpression(x, y);
                        eat(')');
                        result = evaluateFunction(name, arg1, arg2);
                    } else {
                        throw new RuntimeException("Unbekannte Anzahl Argumente für Funktion " + name);
                    }
                } else {
                    // Bei unbekannter Funktion, normal parsen
                    result = parseExpression(x, y);
                    eat(')');
                    
                    // Bei unbekannten Funktionen einen Fehler werfen
                    throw new RuntimeException("Unbekannte Funktion: " + name);
                }

                // Nach einer Funktionsauswertung prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z') || ch == '(') {
                    result *= parseFactor(x, y);
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
                        throw new RuntimeException("Unbekannte Konstante: " + name);
                }

                // Nach einer Konstanten prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z' && !name.equals("x") && !name.equals("y")) || ch == '(') {
                    result *= parseFactor(x, y);
                }
            }
        } else {
            throw new RuntimeException("Unerwartetes Zeichen: " + ch);
        }

        // Exponentiation (Potenzen)
        if (eat('^')) {
            result = Math.pow(result, parseFactor(x, y));
        }

        return result;
    }
    
    /**
     * Wertet eine einstellige mathematische Funktion aus
     */
    private double evaluateFunction(String name, double arg) {
        switch (name) {
            case "sin": return Math.sin(arg);
            case "cos": return Math.cos(arg);
            case "tan": return Math.tan(arg);
            case "asin": 
                if (arg < -1 || arg > 1)
                    throw new ArithmeticException("Arcsin-Argument außerhalb des Bereichs [-1, 1]");
                return Math.asin(arg);
            case "acos": 
                if (arg < -1 || arg > 1)
                    throw new ArithmeticException("Arccos-Argument außerhalb des Bereichs [-1, 1]");
                return Math.acos(arg);
            case "atan": return Math.atan(arg);
            case "sinh": return Math.sinh(arg);
            case "cosh": return Math.cosh(arg);
            case "tanh": return Math.tanh(arg);
            case "sqrt": 
                if (arg < 0)
                    throw new ArithmeticException("Quadratwurzel aus negativer Zahl");
                return Math.sqrt(arg);
            case "cbrt": return Math.cbrt(arg);
            case "log": 
            case "log10":
                if (arg <= 0)
                    throw new ArithmeticException("Logarithmus einer nicht-positiven Zahl");
                return Math.log10(arg);
            case "log2":
                if (arg <= 0)
                    throw new ArithmeticException("Logarithmus einer nicht-positiven Zahl");
                return Math.log(arg) / Math.log(2);
            case "ln":
                if (arg <= 0)
                    throw new ArithmeticException("Natürlicher Logarithmus einer nicht-positiven Zahl");
                return Math.log(arg);
            case "exp": return Math.exp(arg);
            case "abs": return Math.abs(arg);
            default: throw new RuntimeException("Unbekannte Funktion: " + name);
        }
    }
    
    /**
     * Wertet eine zweistellige mathematische Funktion aus
     */
    private double evaluateFunction(String name, double arg1, double arg2) {
        switch (name) {
            case "max": return Math.max(arg1, arg2);
            case "min": return Math.min(arg1, arg2);
            case "pow": return Math.pow(arg1, arg2);
            case "atan2": return Math.atan2(arg1, arg2);
            default: throw new RuntimeException("Unbekannte Funktion: " + name);
        }
    }
}