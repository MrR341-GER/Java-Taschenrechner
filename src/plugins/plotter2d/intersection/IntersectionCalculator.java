package plugins.plotter2d.intersection;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import parser.FunctionParser;
import plugins.plotter2d.CoordinateTransformer;
import plugins.plotter2d.FunctionRenderer;
import plugins.plotter2d.GraphPanel;

/**
 * Berechnet und zeichnet Schnittpunkte zwischen Funktionen
 */
public class IntersectionCalculator {
    private final GraphPanel panel;
    private final CoordinateTransformer transformer;
    private final FunctionRenderer functionRenderer;

    private boolean showIntersections = false;
    private List<IntersectionPoint> intersectionPoints = new ArrayList<>();

    // Konstanten für die Darstellung
    private static final int POINT_SIZE = 8;
    private static final Color POINT_COLOR = Color.BLACK;
    private static final Color HIGHLIGHT_COLOR = new Color(0, 102, 204); // Dunkelblau für Hervorhebungen

    public IntersectionCalculator(GraphPanel panel, CoordinateTransformer transformer,
            FunctionRenderer functionRenderer) {
        this.panel = panel;
        this.transformer = transformer;
        this.functionRenderer = functionRenderer;
    }

    /**
     * Schaltet die Anzeige von Schnittpunkten um
     */
    public void toggleIntersections(boolean show) {
        this.showIntersections = show;

        if (show) {
            calculateIntersections();
        } else {
            intersectionPoints.clear();
        }
    }

    /**
     * Gibt zurück, ob Schnittpunkte aktuell angezeigt werden
     */
    public boolean isShowingIntersections() {
        return showIntersections;
    }

    /**
     * Gibt die Liste der Schnittpunkte zurück
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionPoints;
    }

    /**
     * Berechnet alle Schnittpunkte zwischen den gezeichneten Funktionen
     */
    public void calculateIntersections() {
        List<IntersectionPoint> oldIntersections = new ArrayList<>(intersectionPoints);
        intersectionPoints.clear();

        List<FunctionRenderer.FunctionInfo> functions = functionRenderer.getFunctions();

        // Für Schnittpunkte werden mindestens zwei Funktionen benötigt
        if (functions.size() < 2) {
            // Ereignis nur auslösen, wenn sich etwas geändert hat
            if (!oldIntersections.isEmpty()) {
                panel.fireIntersectionsUpdated(oldIntersections, intersectionPoints);
            }
            return;
        }

        // Berechne Schnittpunkte für alle Funktionspaare
        for (int i = 0; i < functions.size() - 1; i++) {
            for (int j = i + 1; j < functions.size(); j++) {
                FunctionRenderer.FunctionInfo f1 = functions.get(i);
                FunctionRenderer.FunctionInfo f2 = functions.get(j);

                // Überprüfe, ob die Funktionen identisch sind
                if (areFunctionsIdentical(f1.getFunction(), f2.getFunction())) {
                    continue; // Identische Funktionen überspringen
                }

                // Funktionsausdrücke (versuche, diese aus dem Funktionsobjekt zu extrahieren)
                String expr1 = "f" + (i + 1);
                String expr2 = "f" + (j + 1);

                // Finde Schnittpunkte im aktuellen Sichtfenster
                List<Point2D.Double> points = IntersectionFinder.findIntersections(
                        f1.getFunction(), f2.getFunction(), transformer.getXMin(), transformer.getXMax());

                // Füge die gefundenen Schnittpunkte als IntersectionPoint-Objekte zur
                // Gesamtliste hinzu
                for (Point2D.Double point : points) {
                    IntersectionPoint ip = new IntersectionPoint(
                            point.x, point.y, i, j, expr1, expr2);

                    // Prüfe auf Duplikate
                    boolean isDuplicate = false;
                    for (IntersectionPoint existingPoint : intersectionPoints) {
                        if (Math.abs(existingPoint.x - point.x) < 1e-6 &&
                                Math.abs(existingPoint.y - point.y) < 1e-6) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        intersectionPoints.add(ip);
                    }
                }
            }
        }

        // Ereignis auslösen, falls sich die Schnittpunkte geändert haben
        boolean changed = oldIntersections.size() != intersectionPoints.size();
        if (!changed) {
            // Prüfe auf unterschiedliche Punkte
            for (int i = 0; i < intersectionPoints.size(); i++) {
                if (i >= oldIntersections.size() ||
                        Math.abs(intersectionPoints.get(i).x - oldIntersections.get(i).x) > 1e-6 ||
                        Math.abs(intersectionPoints.get(i).y - oldIntersections.get(i).y) > 1e-6) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            panel.fireIntersectionsUpdated(oldIntersections, intersectionPoints);
        }
    }

    /**
     * Prüft, ob zwei Funktionen identisch sind, indem mehrere Testwerte verglichen
     * werden
     */
    private boolean areFunctionsIdentical(FunctionParser f1, FunctionParser f2) {
        // Anzahl der Testpunkte
        final int NUM_TEST_POINTS = 10;

        // Bereich für die Testpunkte (aktueller sichtbarer Bereich)
        double min = transformer.getXMin();
        double max = transformer.getXMax();
        double step = (max - min) / (NUM_TEST_POINTS - 1);

        // Teste mehrere Punkte im aktuellen Bereich
        for (int i = 0; i < NUM_TEST_POINTS; i++) {
            double x = min + i * step;
            try {
                double y1 = f1.evaluateAt(x);
                double y2 = f2.evaluateAt(x);

                // Falls ein y-Wert NaN oder unendlich ist, überspringe diesen Punkt
                if (Double.isNaN(y1) || Double.isInfinite(y1) ||
                        Double.isNaN(y2) || Double.isInfinite(y2)) {
                    continue;
                }

                // Falls die y-Werte unterschiedlich sind, sind die Funktionen nicht identisch
                if (Math.abs(y1 - y2) > 1e-10) {
                    return false;
                }
            } catch (Exception e) {
                // Falls bei der Auswertung Fehler auftreten, wird der Punkt als unterschiedlich
                // betrachtet
                return false;
            }
        }

        // Wenn alle Testpunkte identisch sind, gehen wir davon aus, dass die Funktionen
        // gleich sind
        return true;
    }

    /**
     * Zeichnet die Schnittpunkte
     */
    public void drawIntersectionPoints(Graphics2D g2d) {
        if (!showIntersections || intersectionPoints.isEmpty()) {
            return;
        }

        // Einstellungen für die Schnittpunkte
        g2d.setColor(POINT_COLOR);

        // Zeichne jeden Schnittpunkt als kleinen gefüllten Kreis
        for (IntersectionPoint point : intersectionPoints) {
            // Überprüfe, ob der Punkt im sichtbaren Bereich liegt
            if (point.x >= transformer.getXMin() && point.x <= transformer.getXMax() &&
                    point.y >= transformer.getYMin() && point.y <= transformer.getYMax()) {

                int screenX = transformer.worldToScreenX(point.x);
                int screenY = transformer.worldToScreenY(point.y);

                // Zeichne einen gefüllten Kreis
                g2d.fillOval(screenX - POINT_SIZE / 2, screenY - POINT_SIZE / 2,
                        POINT_SIZE, POINT_SIZE);

                // Zeichne die Koordinaten als Text daneben mit dynamischer Genauigkeit
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String coords = "(" + transformer.getAxisFormat().format(point.x) +
                        ", " + transformer.getAxisFormat().format(point.y) + ")";
                g2d.drawString(coords, screenX + POINT_SIZE, screenY);
            }
        }
    }
}
