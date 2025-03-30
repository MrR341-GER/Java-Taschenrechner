package plugins.plotter3d.renderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;

/**
 * Detectiert und berechnet Schnittlinien zwischen 3D-Funktionen
 */
public class Plot3DIntersectionDetector {

    // Konstanten zur Anpassung der Genauigkeit und des Aussehens
    private static final double INTERSECTION_THRESHOLD = 0.5; // Toleranz für Schnittpunkte
    private static final Color DEFAULT_INTERSECTION_COLOR = Color.RED; // Standardfarbe für Schnittlinien

    /**
     * Innere Klasse zur Speicherung von Schnittpunkten
     */
    public static class IntersectionPoint {
        public final double x, y, z; // Koordinaten des Schnittpunkts
        public final int gridI1, gridJ1; // Gitterkoordinaten der ersten Funktion
        public final int gridI2, gridJ2; // Gitterkoordinaten der zweiten Funktion

        public IntersectionPoint(double x, double y, double z, int gridI1, int gridJ1, int gridI2, int gridJ2) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.gridI1 = gridI1;
            this.gridJ1 = gridJ1;
            this.gridI2 = gridI2;
            this.gridJ2 = gridJ2;
        }
    }

    /**
     * Innere Klasse für ein Liniensegment der Schnittlinie
     */
    public static class IntersectionSegment {
        public final Plot3DPoint p1, p2; // Start- und Endpunkt des Segments

        public IntersectionSegment(Plot3DPoint p1, Plot3DPoint p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    /**
     * Findet alle Schnittlinien zwischen allen Funktionen
     * 
     * @param model Das 3D-Modell mit allen Funktionen
     * @return Eine Liste von Listen von Schnittliniensegmenten für jedes
     *         Funktionspaar
     */
    public static List<List<IntersectionSegment>> findAllIntersections(Plot3DModel model) {
        List<List<IntersectionSegment>> allIntersections = new ArrayList<>();
        List<Plot3DModel.Function3DInfo> functions = model.getFunctions();

        // Wenn weniger als 2 Funktionen vorhanden sind, gibt es keine Schnitte
        if (functions.size() < 2) {
            return allIntersections;
        }

        // Alle Funktionspaare durchgehen
        for (int i = 0; i < functions.size() - 1; i++) {
            for (int j = i + 1; j < functions.size(); j++) {
                Plot3DModel.Function3DInfo func1 = functions.get(i);
                Plot3DModel.Function3DInfo func2 = functions.get(j);

                // Schnittlinie für dieses Funktionspaar berechnen
                List<IntersectionSegment> intersection = findIntersection(func1, func2);
                if (!intersection.isEmpty()) {
                    allIntersections.add(intersection);
                }
            }
        }

        return allIntersections;
    }

    /**
     * Findet die Schnittlinie zwischen zwei Funktionen
     * 
     * @param func1 Erste Funktion
     * @param func2 Zweite Funktion
     * @return Liste von Liniensegmenten, die die Schnittlinie bilden
     */
    private static List<IntersectionSegment> findIntersection(Plot3DModel.Function3DInfo func1,
            Plot3DModel.Function3DInfo func2) {
        List<IntersectionSegment> segments = new ArrayList<>();
        List<IntersectionPoint> intersectionPoints = findIntersectionPoints(func1, func2);

        // Keine Schnittpunkte gefunden
        if (intersectionPoints.size() < 2) {
            return segments;
        }

        // Schnittpunkte zu einer Linie verbinden
        // sortIntersectionPoints(intersectionPoints);

        // Segmente aus aufeinanderfolgenden Punkten erstellen
        for (int i = 0; i < intersectionPoints.size() - 1; i++) {
            IntersectionPoint ip1 = intersectionPoints.get(i);
            IntersectionPoint ip2 = intersectionPoints.get(i + 1);

            // Nur Segmente erstellen, wenn die Punkte nahe genug sind (um Sprünge zu
            // vermeiden)
            double distance = Math.sqrt(
                    Math.pow(ip1.x - ip2.x, 2) +
                            Math.pow(ip1.y - ip2.y, 2) +
                            Math.pow(ip1.z - ip2.z, 2));

            // Maximalabstand abhängig von der Gittergröße
            double gridSize = 10.0 / func1.getGridPoints().length; // Annahme: Wertebereich ca. 10 Einheiten
            double maxDistance = gridSize * 2.0; // Punkteabstand darf maximal 2 Gitterzellen sein

            if (distance <= maxDistance) {
                Plot3DPoint p1 = new Plot3DPoint(ip1.x, ip1.y, ip1.z);
                Plot3DPoint p2 = new Plot3DPoint(ip2.x, ip2.y, ip2.z);
                segments.add(new IntersectionSegment(p1, p2));
            }
        }

        return segments;
    }

    /**
     * Findet alle Schnittpunkte zwischen zwei Funktionen
     */
    private static List<IntersectionPoint> findIntersectionPoints(
            Plot3DModel.Function3DInfo func1, Plot3DModel.Function3DInfo func2) {

        List<IntersectionPoint> points = new ArrayList<>();
        Plot3DPoint[][][] grid1 = func1.getGridPoints();
        Plot3DPoint[][][] grid2 = func2.getGridPoints();

        if (grid1 == null || grid2 == null) {
            return points;
        }

        int resolution1 = grid1.length;
        int resolution2 = grid2.length;

        // Jedes Quadrat im Gitter untersuchen
        for (int i1 = 0; i1 < resolution1 - 1; i1++) {
            for (int j1 = 0; j1 < resolution1 - 1; j1++) {
                // Vier Eckpunkte des Quadrats aus der ersten Funktion
                Plot3DPoint p1 = grid1[i1][j1][0];
                Plot3DPoint p2 = grid1[i1 + 1][j1][0];
                Plot3DPoint p3 = grid1[i1 + 1][j1 + 1][0];
                Plot3DPoint p4 = grid1[i1][j1 + 1][0];

                if (p1 == null || p2 == null || p3 == null || p4 == null) {
                    continue;
                }

                // Jedes Quadrat im Gitter der zweiten Funktion
                for (int i2 = 0; i2 < resolution2 - 1; i2++) {
                    for (int j2 = 0; j2 < resolution2 - 1; j2++) {
                        // Vier Eckpunkte des Quadrats aus der zweiten Funktion
                        Plot3DPoint q1 = grid2[i2][j2][0];
                        Plot3DPoint q2 = grid2[i2 + 1][j2][0];
                        Plot3DPoint q3 = grid2[i2 + 1][j2 + 1][0];
                        Plot3DPoint q4 = grid2[i2][j2 + 1][0];

                        if (q1 == null || q2 == null || q3 == null || q4 == null) {
                            continue;
                        }

                        // Überprüfen, ob sich die Flächen schneiden könnten
                        if (potentialIntersection(p1, p2, p3, p4, q1, q2, q3, q4)) {
                            // Mittelpunkt der Zelle als Annäherung an den Schnittpunkt
                            double x1 = (p1.getX() + p2.getX() + p3.getX() + p4.getX()) / 4.0;
                            double y1 = (p1.getY() + p2.getY() + p3.getY() + p4.getY()) / 4.0;
                            double z1 = (p1.getZ() + p2.getZ() + p3.getZ() + p4.getZ()) / 4.0;

                            double x2 = (q1.getX() + q2.getX() + q3.getX() + q4.getX()) / 4.0;
                            double y2 = (q1.getY() + q2.getY() + q3.getY() + q4.getY()) / 4.0;
                            double z2 = (q1.getZ() + q2.getZ() + q3.getZ() + q4.getZ()) / 4.0;

                            // Wenn die mittleren Punkte nahe genug sind, nehme Mittelpunkt als Schnittpunkt
                            if (Math.abs(z1 - z2) < INTERSECTION_THRESHOLD &&
                                    Math.abs(x1 - x2) < INTERSECTION_THRESHOLD * 5 &&
                                    Math.abs(y1 - y2) < INTERSECTION_THRESHOLD * 5) {

                                // Nehme Mittelwert als interpolierten Schnittpunkt
                                double x = (x1 + x2) / 2.0;
                                double y = (y1 + y2) / 2.0;
                                double z = (z1 + z2) / 2.0;

                                points.add(new IntersectionPoint(x, y, z, i1, j1, i2, j2));
                            }
                        }
                    }
                }
            }
        }

        return points;
    }

    /**
     * Überprüft, ob zwei Gitterzellen potentiell einen Schnitt haben könnten
     */
    private static boolean potentialIntersection(
            Plot3DPoint p1, Plot3DPoint p2, Plot3DPoint p3, Plot3DPoint p4,
            Plot3DPoint q1, Plot3DPoint q2, Plot3DPoint q3, Plot3DPoint q4) {

        // Minimum und Maximum z-Werte für beide Vierecke
        double minZ1 = Math.min(Math.min(p1.getZ(), p2.getZ()), Math.min(p3.getZ(), p4.getZ()));
        double maxZ1 = Math.max(Math.max(p1.getZ(), p2.getZ()), Math.max(p3.getZ(), p4.getZ()));
        double minZ2 = Math.min(Math.min(q1.getZ(), q2.getZ()), Math.min(q3.getZ(), q4.getZ()));
        double maxZ2 = Math.max(Math.max(q1.getZ(), q2.getZ()), Math.max(q3.getZ(), q4.getZ()));

        // Prüfen, ob sich die z-Bereiche überlappen
        if (maxZ1 < minZ2 || maxZ2 < minZ1) {
            return false;
        }

        // Minimum und Maximum x/y-Werte für beide Vierecke
        double minX1 = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
        double maxX1 = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
        double minY1 = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
        double maxY1 = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));

        double minX2 = Math.min(Math.min(q1.getX(), q2.getX()), Math.min(q3.getX(), q4.getX()));
        double maxX2 = Math.max(Math.max(q1.getX(), q2.getX()), Math.max(q3.getX(), q4.getX()));
        double minY2 = Math.min(Math.min(q1.getY(), q2.getY()), Math.min(q3.getY(), q4.getY()));
        double maxY2 = Math.max(Math.max(q1.getY(), q2.getY()), Math.max(q3.getY(), q4.getY()));

        // Prüfen, ob sich die Vierecke in x/y-Ebene überlappen
        return !(maxX1 < minX2 || maxX2 < minX1 || maxY1 < minY2 || maxY2 < minY1);
    }

    /**
     * Sortiert die Schnittpunkte, um eine zusammenhängende Linie zu bilden
     * Verwendet einen einfachen Nearest-Neighbor-Algorithmus
     */
    private static void sortIntersectionPoints(List<IntersectionPoint> points) {
        if (points.size() <= 2) {
            return; // Nichts zu sortieren bei 0, 1 oder 2 Punkten
        }

        List<IntersectionPoint> sorted = new ArrayList<>();
        sorted.add(points.get(0));
        points.remove(0);

        // Greedy-Algorithmus: Füge immer den nächsten Punkt hinzu
        while (!points.isEmpty()) {
            IntersectionPoint last = sorted.get(sorted.size() - 1);
            int nextIndex = findNearestPoint(last, points);
            sorted.add(points.get(nextIndex));
            points.remove(nextIndex);
        }

        // Originalliste aktualisieren
        points.addAll(sorted);
    }

    /**
     * Findet den nächsten Punkt zur gegebenen Referenz
     */
    private static int findNearestPoint(IntersectionPoint reference, List<IntersectionPoint> points) {
        int nearest = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            IntersectionPoint p = points.get(i);
            double distance = Math.sqrt(
                    Math.pow(reference.x - p.x, 2) +
                            Math.pow(reference.y - p.y, 2) +
                            Math.pow(reference.z - p.z, 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * Gibt die Standardfarbe für Schnittlinien zurück
     */
    public static Color getDefaultIntersectionColor() {
        return DEFAULT_INTERSECTION_COLOR;
    }
}