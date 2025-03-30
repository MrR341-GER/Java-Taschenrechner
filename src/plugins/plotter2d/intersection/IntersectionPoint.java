
package plugins.plotter2d.intersection;

import java.awt.geom.Point2D;

/**
 * Erweiterte Klasse für Schnittpunkte mit zusätzlichen Informationen
 */
public class IntersectionPoint extends Point2D.Double {
    // Indizes der beiden Funktionen, die sich schneiden
    private final int functionIndex1;
    private final int functionIndex2;

    // Funktionsausdrücke der beiden Funktionen
    private final String function1Expression;
    private final String function2Expression;

    /**
     * Erstellt einen neuen Schnittpunkt mit Funktionsinformationen
     *
     * @param x                   X-Koordinate des Schnittpunkts
     * @param y                   Y-Koordinate des Schnittpunkts
     * @param functionIndex1      Index der ersten Funktion
     * @param functionIndex2      Index der zweiten Funktion
     * @param function1Expression Ausdruck der ersten Funktion
     * @param function2Expression Ausdruck der zweiten Funktion
     */
    public IntersectionPoint(double x, double y, int functionIndex1, int functionIndex2,
            String function1Expression, String function2Expression) {
        super(x, y);
        this.functionIndex1 = functionIndex1;
        this.functionIndex2 = functionIndex2;
        this.function1Expression = function1Expression;
        this.function2Expression = function2Expression;
    }

    /**
     * Gibt den Index der ersten Funktion zurück
     */
    public int getFunctionIndex1() {
        return functionIndex1;
    }

    /**
     * Gibt den Index der zweiten Funktion zurück
     */
    public int getFunctionIndex2() {
        return functionIndex2;
    }

    /**
     * Gibt den Ausdruck der ersten Funktion zurück
     */
    public String getFunction1Expression() {
        return function1Expression;
    }

    /**
     * Gibt den Ausdruck der zweiten Funktion zurück
     */
    public String getFunction2Expression() {
        return function2Expression;
    }

    /**
     * Formatierte Stringdarstellung des Schnittpunkts
     */
    @Override
    public String toString() {
        return String.format("Schnittpunkt (%.4f, %.4f) zwischen %s und %s",
                x, y, function1Expression, function2Expression);
    }
}