import java.util.HashMap;
import java.util.Map;

/**
 * Parser für mathematische Funktionen mit zwei Variablen (x und y)
 * Erbt von AbstractExpressionParser für gemeinsame Parsing-Funktionalität
 */
public class Function3DParser extends AbstractExpressionParser {
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
    
    // Aktuelle x und y Werte für die Auswertung
    private double currentX;
    private double currentY;

    /**
     * Erstellt einen neuen 3D-Funktionsparser
     */
    public Function3DParser(String expression) {
        super(expression);
    }

    /**
     * Wertet die Funktion an einer bestimmten Stelle (x,y) aus
     */
    public double evaluateAt(double x, double y) {
        pos = 0;
        nextChar();
        this.currentX = x;
        this.currentY = y;
        double result = parseExpression();

        if (pos < expression.length()) {
            throw new RuntimeException("Unerwartetes Zeichen: " + ch);
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
                    throw new ArithmeticException("Division durch Null");
                }
                result /= divisor;
            }
            // Implizite Multiplikation für Fälle wie "2x" oder "2(x+1)" oder "x(y)"
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
        // Die Variable x oder y
        else if (ch == 'x') {
            nextChar();
            result = currentX;

            // Nach 'x' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x' && ch != 'y') || ch == '(') {
                result *= parseFactor();
            }
        }
        else if (ch == 'y') {
            nextChar();
            result = currentY;

            // Nach 'y' prüfen, ob implizite Multiplikation folgt
            if ((ch >= 'a' && ch <= 'z' && ch != 'x' && ch != 'y') || ch == '(') {
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
                // Prüfe, ob es sich um eine bekannte Funktion handelt
                if (FUNCTIONS.containsKey(name)) {
                    int argCount = FUNCTIONS.get(name);
                    
                    if (argCount == 1) {
                        // Einstellige Funktion
                        double arg = parseExpression();
                        eat(')');
                        result = evaluateFunction(name, arg);
                    } else if (argCount == 2) {
                        // Zweistellige Funktion
                        double arg1 = parseExpression();
                        if (!eat(',')) {
                            throw new RuntimeException("Erwarte zweites Argument für Funktion " + name);
                        }
                        double arg2 = parseExpression();
                        eat(')');
                        result = evaluateFunction(name, arg1, arg2);
                    } else {
                        throw new RuntimeException("Unbekannte Anzahl Argumente für Funktion " + name);
                    }
                } else {
                    // Bei unbekannter Funktion, normal parsen
                    result = parseExpression();
                    eat(')');
                    
                    // Bei unbekannten Funktionen einen Fehler werfen
                    throw new RuntimeException("Unbekannte Funktion: " + name);
                }

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
                        throw new RuntimeException("Unbekannte Konstante: " + name);
                }

                // Nach einer Konstanten prüfen, ob implizite Multiplikation folgt
                if ((ch >= 'a' && ch <= 'z' && !name.equals("x") && !name.equals("y")) || ch == '(') {
                    result *= parseFactor();
                }
            }
        } else {
            throw new RuntimeException("Unerwartetes Zeichen: " + ch);
        }

        // Exponentiation (Potenzen)
        if (eat('^')) {
            result = Math.pow(result, parseFactor());
        }

        return result;
    }
}
