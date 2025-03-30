/**
 * Abstrakte Basisklasse für mathematische Ausdrucksparser
 * Bietet gemeinsame Funktionalität für das Parsen von Formeln
 */
public abstract class AbstractExpressionParser {
    protected String expression;
    protected int pos;
    protected char ch;
    protected char nextCh; // Speichert das nächste Zeichen für Look-ahead

    /**
     * Konstruktor für den Parser
     * 
     * @param expression Der zu parsende Ausdruck
     */
    public AbstractExpressionParser(String expression) {
        this.expression = expression.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Liest das nächste Zeichen aus dem Ausdruck
     */
    protected void nextChar() {
        ch = (pos < expression.length()) ? expression.charAt(pos++) : '\0';
        // Speichere auch das folgende Zeichen (ohne Position zu erhöhen)
        nextCh = (pos < expression.length()) ? expression.charAt(pos) : '\0';
    }

    /**
     * Versucht, ein bestimmtes Zeichen zu "essen" (konsumieren)
     */
    protected boolean eat(char charToEat) {
        while (ch == ' ')
            nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    /**
     * Parst einen mathematischen Ausdruck (Addition/Subtraktion)
     */
    protected double parseExpression() {
        double result = parseTerm();

        while (true) {
            if (eat('+'))
                result += parseTerm();
            else if (eat('-'))
                result -= parseTerm();
            else
                return result;
        }
    }

    /**
     * Parst einen Term (Multiplikation/Division)
     */
    protected abstract double parseTerm();

    /**
     * Parst einen Faktor (Zahlen, Variablen, Funktionen, Klammern)
     */
    protected abstract double parseFactor();

    /**
     * Wertet eine mathematische Funktion aus
     */
    protected double evaluateFunction(String name, double arg) {
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
            case "floor": return Math.floor(arg);
            case "ceil": 
            case "ceiling": return Math.ceil(arg);
            case "round": return Math.round(arg);
            case "degrees":
            case "deg": return Math.toDegrees(arg);
            case "radians":
            case "rad": return Math.toRadians(arg);
            default: throw new RuntimeException("Unbekannte Funktion: " + name);
        }
    }

    /**
     * Wertet eine zweistellige mathematische Funktion aus
     */
    protected double evaluateFunction(String name, double arg1, double arg2) {
        switch (name) {
            case "max": return Math.max(arg1, arg2);
            case "min": return Math.min(arg1, arg2);
            case "pow": return Math.pow(arg1, arg2);
            case "atan2": return Math.atan2(arg1, arg2);
            default: throw new RuntimeException("Unbekannte Funktion: " + name);
        }
    }
}
