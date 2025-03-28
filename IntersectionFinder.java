import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasse zum Berechnen von Schnittpunkten zwischen mathematischen Funktionen
 */
public class IntersectionFinder {

    // Konstanten für die Schnittpunktberechnung
    private static final double INITIAL_STEP = 0.1; // Initiale Schrittweite bei der Suche
    private static final double PRECISION = 1e-6; // Genauigkeit für Schnittpunktberechnung
    private static final int MAX_ITERATIONS = 50; // Maximale Anzahl von Iterationen bei der Feinabstimmung

    /**
     * Berechnet alle Schnittpunkte zwischen zwei Funktionen im angegebenen Bereich
     * 
     * @param f1   Erste Funktion
     * @param f2   Zweite Funktion
     * @param xMin Linke Grenze des Bereichs
     * @param xMax Rechte Grenze des Bereichs
     * @return Liste der gefundenen Schnittpunkte
     */
    public static List<Point2D.Double> findIntersections(FunctionParser f1, FunctionParser f2,
            double xMin, double xMax) {
        List<Point2D.Double> intersections = new ArrayList<>();

        // Sicherheitsprüfung
        if (f1 == null || f2 == null) {
            return intersections;
        }

        // Erstelle eine Differenzfunktion: f(x) = f1(x) - f2(x)
        // Schnittpunkte sind, wo diese Funktion Nullstellen hat (f1(x) = f2(x))
        DifferenceFunction diffFunction = new DifferenceFunction(f1, f2);

        // Suche nach Vorzeichenwechseln mit angepasster Schrittweite basierend auf dem
        // Bereich
        double range = xMax - xMin;
        double step = Math.min(INITIAL_STEP, range / 1000); // Dynamisch angepasste Schrittweite

        double prevX = xMin;
        double prevY = evaluateSafely(diffFunction, prevX);

        for (double x = xMin + step; x <= xMax; x += step) {
            double y = evaluateSafely(diffFunction, x);

            // Prüfe auf Vorzeichenwechsel (Nullstelle der Differenzfunktion)
            if (prevY != Double.NaN && y != Double.NaN && signChanged(prevY, y)) {
                // Verfeinere die Nullstelle mit binärer Suche
                Point2D.Double intersection = refineIntersection(diffFunction, prevX, x, f1);

                // Füge den Schnittpunkt zur Liste hinzu, wenn er gültig ist
                if (intersection != null) {
                    // Prüfe, ob der Punkt bereits in der Liste ist (mit Toleranz)
                    if (!containsPoint(intersections, intersection)) {
                        intersections.add(intersection);
                    }
                }
            }

            prevX = x;
            prevY = y;
        }

        return intersections;
    }

    /**
     * Evaluiert eine Funktion sicher und gibt NaN zurück, wenn ein Fehler auftritt
     */
    private static double evaluateSafely(DifferenceFunction function, double x) {
        try {
            return function.evaluateAt(x);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * Prüft, ob sich das Vorzeichen zwischen zwei Werten geändert hat
     */
    private static boolean signChanged(double y1, double y2) {
        return (y1 < 0 && y2 > 0) || (y1 > 0 && y2 < 0) || y1 == 0 || y2 == 0;
    }

    /**
     * Verfeinert einen gefundenen Schnittpunkt mit binärer Suche
     */
    private static Point2D.Double refineIntersection(DifferenceFunction diffFunction,
            double x1, double x2, FunctionParser originalFunction) {
        double a = x1;
        double b = x2;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double c = (a + b) / 2; // Mittelpunkt
            double valueAtC = evaluateSafely(diffFunction, c);

            // Präzision erreicht
            if (Math.abs(b - a) < PRECISION || Math.abs(valueAtC) < PRECISION) {
                try {
                    double y = originalFunction.evaluateAt(c);
                    return new Point2D.Double(c, y);
                } catch (Exception e) {
                    return null; // Bei Fehler in der Auswertung
                }
            }

            double valueAtA = evaluateSafely(diffFunction, a);

            // Aktualisiere das Intervall für binäre Suche
            if (signChanged(valueAtA, valueAtC)) {
                b = c;
            } else {
                a = c;
            }
        }

        // Wenn wir hier ankommen, haben wir kein präzises Ergebnis gefunden
        return null;
    }

    /**
     * Prüft, ob ein Punkt bereits in der Liste vorhanden ist (mit Toleranz)
     */
    private static boolean containsPoint(List<Point2D.Double> points, Point2D.Double point) {
        for (Point2D.Double p : points) {
            if (Math.abs(p.x - point.x) < PRECISION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hilfsfunktion zur Darstellung der Differenz zweier Funktionen
     */
    private static class DifferenceFunction {
        private final FunctionParser f1;
        private final FunctionParser f2;

        public DifferenceFunction(FunctionParser f1, FunctionParser f2) {
            this.f1 = f1;
            this.f2 = f2;
        }

        public double evaluateAt(double x) {
            return f1.evaluateAt(x) - f2.evaluateAt(x);
        }
    }
}