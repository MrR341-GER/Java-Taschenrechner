package plugins.plotter3d.renderer;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import parser.Function3DParser;

/**
 * Berechnet mathematisch exakte Schnittkurven zwischen 3D-Funktionen
 * mit optimierter Performance.
 */
public class Plot3DIntersectionCalculator {

    // Konstanten für die numerische Genauigkeit und Performance
    private static final double TOLERANCE = 1e-6;
    private static final int MAX_ITERATIONS = 50;
    // Anfängliche Rastergröße - größer für mehr Performance, kleiner für mehr
    // Genauigkeit
    private static final double INITIAL_STEP_SIZE = 0.2;
    // Schwellwert für adaptives Sampling
    private static final double CURVATURE_THRESHOLD = 0.05;
    // Farbe für Schnittlinien
    private static final Color INTERSECTION_COLOR = Color.RED;

    // Cache für Funktionsauswertungen zur Vermeidung redundanter Berechnungen
    private static final Map<String, Double> functionValueCache = new ConcurrentHashMap<>();

    // Thread-Pool für parallele Berechnungen
    private static final ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Innere Klasse zur Speicherung und Sortierung von Schnittpunkten
     */
    private static class IntersectionPoint implements Comparable<IntersectionPoint> {
        final double x, y, z;
        // pathDistance als nicht-final deklarieren, damit es geändert werden kann
        double pathDistance;

        public IntersectionPoint(double x, double y, double z, double pathDistance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pathDistance = pathDistance;
        }

        @Override
        public int compareTo(IntersectionPoint other) {
            return Double.compare(this.pathDistance, other.pathDistance);
        }

        public Plot3DPoint toPlot3DPoint() {
            return new Plot3DPoint(x, y, z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            IntersectionPoint other = (IntersectionPoint) obj;
            // Gleichheit basierend auf Koordinaten mit Toleranz
            return Math.abs(x - other.x) < TOLERANCE * 5 &&
                    Math.abs(y - other.y) < TOLERANCE * 5 &&
                    Math.abs(z - other.z) < TOLERANCE * 5;
        }

        @Override
        public int hashCode() {
            // Hash-Code basierend auf gerundeten Koordinaten
            long xBits = Double.doubleToLongBits(Math.round(x / (TOLERANCE * 5)));
            long yBits = Double.doubleToLongBits(Math.round(y / (TOLERANCE * 5)));
            long zBits = Double.doubleToLongBits(Math.round(z / (TOLERANCE * 5)));
            return (int) (xBits ^ (xBits >>> 32) ^
                    yBits ^ (yBits >>> 32) ^
                    zBits ^ (zBits >>> 32));
        }
    }

    /**
     * Schnelltest, ob zwei Funktionen sich überhaupt schneiden könnten
     * Prüft an Stichproben, ob die Funktionen in unterschiedlichen Bereichen liegen
     */
    private static boolean quickIntersectionCheck(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax) {

        // Prüfe an einigen Stichproben im Bereich
        int samples = 3; // Anzahl der Stichproben pro Dimension

        double minDiff = Double.POSITIVE_INFINITY;
        double maxDiff = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < samples; i++) {
            double x = xMin + (xMax - xMin) * i / (samples - 1);
            for (int j = 0; j < samples; j++) {
                double y = yMin + (yMax - yMin) * j / (samples - 1);

                double diff = evaluateDifference(function1, function2, x, y);
                if (!Double.isNaN(diff)) {
                    minDiff = Math.min(minDiff, diff);
                    maxDiff = Math.max(maxDiff, diff);

                    // Wenn wir einen Vorzeichenwechsel gefunden haben (Produkt < 0),
                    // dann gibt es wahrscheinlich einen Schnitt
                    if (minDiff < 0 && maxDiff > 0) {
                        return true;
                    }
                }
            }
        }

        // Wenn alle Differenzen das gleiche Vorzeichen haben und kein NaN,
        // dann schneiden sich die Funktionen wahrscheinlich nicht
        return (minDiff < 0 && maxDiff > 0) ||
                Double.isInfinite(minDiff) ||
                Double.isInfinite(maxDiff);
    }

    /**
     * Berechnet die Differenz zwischen zwei Funktionen mit Caching
     */
    private static double evaluateDifference(
            Function3DParser function1, Function3DParser function2,
            double x, double y) {

        try {
            // Erstelle eindeutige Cache-Schlüssel für die Funktionsauswertungen
            String key1 = function1.toString() + "_" + x + "_" + y;
            String key2 = function2.toString() + "_" + x + "_" + y;

            // Versuche Werte aus dem Cache zu lesen
            Double z1 = functionValueCache.get(key1);
            if (z1 == null) {
                z1 = function1.evaluateAt(x, y);
                functionValueCache.put(key1, z1); // Im Cache speichern
            }

            Double z2 = functionValueCache.get(key2);
            if (z2 == null) {
                z2 = function2.evaluateAt(x, y);
                functionValueCache.put(key2, z2); // Im Cache speichern
            }

            return z1 - z2;
        } catch (Exception e) {
            // Bei Fehlern (z.B. Division durch Null)
            return Double.NaN;
        }
    }

    /**
     * Berechnet eine Schnittlinie zwischen zwei Funktionen
     * mit adaptivem Raster und optimierter Nullstellensuche
     */
    private static List<Plot3DPoint> calculateIntersection(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax) {

        // Set für gefundene Schnittpunkte ohne Duplikate
        Set<IntersectionPoint> pointsSet = Collections.synchronizedSet(new HashSet<>());
        double pathParam = 0.0; // Parametrisierung für die Kurve

        // Startschritt für das adaptive Sampling
        double baseStep = INITIAL_STEP_SIZE;

        // Erste Phase: Grobes Raster zum Finden von Schnittpunkten
        scanForIntersections(function1, function2, xMin, xMax, yMin, yMax,
                baseStep, pointsSet);

        // Zweite Phase: Verfolge die Schnittlinie mit Kurvenverfolgung
        followIntersectionCurve(function1, function2, pointsSet,
                xMin, xMax, yMin, yMax, baseStep / 4);

        // Konvertiere Set in sortierte Liste und ordne Punkte entlang der Kurve
        List<IntersectionPoint> sortedPoints = new ArrayList<>(pointsSet);

        // Erste Sortierung nach X-Koordinate, um einen Startpunkt zu definieren
        sortedPoints.sort(Comparator.comparingDouble(p -> p.x));

        // Neuberechnung der Pfaddistanzen für bessere Kurvenordnung
        if (!sortedPoints.isEmpty()) {
            List<IntersectionPoint> reorderedPoints = new ArrayList<>();
            boolean[] visited = new boolean[sortedPoints.size()];

            // Starte mit dem ersten Punkt
            IntersectionPoint current = sortedPoints.get(0);
            reorderedPoints.add(current);
            visited[0] = true;

            // Greedy-Algorithmus: Füge immer den nächsten Punkt hinzu
            for (int added = 1; added < sortedPoints.size(); added++) {
                int bestNext = -1;
                double minDist = Double.MAX_VALUE;

                for (int i = 0; i < sortedPoints.size(); i++) {
                    if (!visited[i]) {
                        IntersectionPoint candidate = sortedPoints.get(i);
                        double dist = Math.sqrt(
                                Math.pow(current.x - candidate.x, 2) +
                                        Math.pow(current.y - candidate.y, 2));

                        if (dist < minDist) {
                            minDist = dist;
                            bestNext = i;
                        }
                    }
                }

                if (bestNext >= 0) {
                    current = sortedPoints.get(bestNext);
                    reorderedPoints.add(current);
                    visited[bestNext] = true;
                }
            }

            sortedPoints = reorderedPoints;
        }

        // Konvertiere zu Plot3DPoint-Liste
        List<Plot3DPoint> result = new ArrayList<>();
        for (IntersectionPoint p : sortedPoints) {
            result.add(p.toPlot3DPoint());
        }

        // Punkte ausdünnen für bessere Performance in der Darstellung
        result = simplifyPointList(result);

        return result;
    }

    /**
     * Vereinfacht eine Punktliste durch Entfernen redundanter Punkte
     */
    private static List<Plot3DPoint> simplifyPointList(List<Plot3DPoint> points) {
        if (points.size() <= 3) {
            return points;
        }

        List<Plot3DPoint> simplified = new ArrayList<>();
        simplified.add(points.get(0)); // Ersten Punkt behalten

        double minDistanceSquared = 0.01 * 0.01; // Mindestabstand zwischen Punkten

        for (int i = 1; i < points.size() - 1; i++) {
            Plot3DPoint prev = simplified.get(simplified.size() - 1);
            Plot3DPoint curr = points.get(i);
            Plot3DPoint next = points.get(i + 1);

            // Abstand zum letzten behaltenen Punkt
            double distSq = Math.pow(prev.getX() - curr.getX(), 2) +
                    Math.pow(prev.getY() - curr.getY(), 2);

            // Prüfe, ob der Punkt auf einer annähernd geraden Linie liegt
            double angle = angleBetween(prev, curr, next);

            // Behalte den Punkt, wenn er weit genug entfernt ist oder eine Kurve bildet
            if (distSq >= minDistanceSquared || Math.abs(angle) >= 0.15) {
                simplified.add(curr);
            }
        }

        simplified.add(points.get(points.size() - 1)); // Letzten Punkt behalten

        return simplified;
    }

    /**
     * Berechnet den Winkel zwischen drei Punkten
     */
    private static double angleBetween(Plot3DPoint a, Plot3DPoint b, Plot3DPoint c) {
        double abx = b.getX() - a.getX();
        double aby = b.getY() - a.getY();
        double cbx = b.getX() - c.getX();
        double cby = b.getY() - c.getY();

        double dot = abx * cbx + aby * cby;
        double cross = abx * cby - aby * cbx;

        return Math.atan2(cross, dot);
    }

    /**
     * Scannt einen Bereich nach Schnittpunkten mit adaptiver Schrittweite
     */
    private static void scanForIntersections(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax,
            double step, Set<IntersectionPoint> pointsSet) {

        // Anzahl der Schritte basierend auf der Schrittgröße
        int stepsX = Math.max(5, (int) ((xMax - xMin) / step));
        int stepsY = Math.max(5, (int) ((yMax - yMin) / step));

        // Arrays für Funktionswerte und Differenzen
        double[][] diffValues = new double[stepsX + 1][stepsY + 1];
        boolean[][] hasIntersection = new boolean[stepsX][stepsY];

        // Berechne alle Differenzwerte im Raster
        for (int i = 0; i <= stepsX; i++) {
            double x = xMin + i * (xMax - xMin) / stepsX;
            for (int j = 0; j <= stepsY; j++) {
                double y = yMin + j * (yMax - yMin) / stepsY;
                diffValues[i][j] = evaluateDifference(function1, function2, x, y);
            }
        }

        // Suche nach Vorzeichenwechseln in Quadraten
        for (int i = 0; i < stepsX; i++) {
            for (int j = 0; j < stepsY; j++) {
                double x = xMin + i * (xMax - xMin) / stepsX;
                double y = yMin + j * (yMax - yMin) / stepsY;
                double stepX = (xMax - xMin) / stepsX;
                double stepY = (yMax - yMin) / stepsY;

                // Eckpunkte des Quadrats
                double diff00 = diffValues[i][j];
                double diff10 = diffValues[i + 1][j];
                double diff01 = diffValues[i][j + 1];
                double diff11 = diffValues[i + 1][j + 1];

                // Prüfe auf Vorzeichenwechsel
                boolean hasZeroCrossing = false;

                if (!Double.isNaN(diff00) && !Double.isNaN(diff10) && diff00 * diff10 <= 0) {
                    hasZeroCrossing = true;
                }
                if (!Double.isNaN(diff00) && !Double.isNaN(diff01) && diff00 * diff01 <= 0) {
                    hasZeroCrossing = true;
                }
                if (!Double.isNaN(diff01) && !Double.isNaN(diff11) && diff01 * diff11 <= 0) {
                    hasZeroCrossing = true;
                }
                if (!Double.isNaN(diff10) && !Double.isNaN(diff11) && diff10 * diff11 <= 0) {
                    hasZeroCrossing = true;
                }

                if (hasZeroCrossing) {
                    // Markiere dieses Quadrat für Verfeinerung
                    hasIntersection[i][j] = true;

                    // Finde Schnittpunkt(e) in diesem Quadrat mit genauerer Suche
                    findIntersectionsInSquare(function1, function2,
                            x, y, x + stepX, y + stepY,
                            pointsSet);
                }
            }
        }
    }

    /**
     * Findet Schnittpunkte in einem Quadrat mit adaptiver Verfeinerung
     */
    private static void findIntersectionsInSquare(
            Function3DParser function1, Function3DParser function2,
            double x1, double y1, double x2, double y2,
            Set<IntersectionPoint> pointsSet) {

        // Berechne Mittelpunkte der Kanten
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        // Werte an den Ecken und Mittelpunkten
        double diff00 = evaluateDifference(function1, function2, x1, y1);
        double diff10 = evaluateDifference(function1, function2, x2, y1);
        double diff01 = evaluateDifference(function1, function2, x1, y2);
        double diff11 = evaluateDifference(function1, function2, x2, y2);
        double diffMid = evaluateDifference(function1, function2, midX, midY);

        // Wenn die Differenz im Mittelpunkt nahe 0 ist, haben wir einen Schnittpunkt
        // gefunden
        if (Math.abs(diffMid) < TOLERANCE) {
            double z = function1.evaluateAt(midX, midY);
            // Nehme den Mittelpunkt als Näherung für den Schnittpunkt
            IntersectionPoint ip = new IntersectionPoint(midX, midY, z, 0);
            pointsSet.add(ip);
            return;
        }

        // Suche nach Vorzeichenwechseln auf den Kanten und verwende
        // das Newton-Raphson-Verfahren für eine genauere Nullstellensuche

        // Kante unten: (x1,y1) -> (x2,y1)
        if (diff00 * diff10 <= 0 && !Double.isNaN(diff00) && !Double.isNaN(diff10)) {
            double x = findRootNewton(function1, function2, x1, x2, y1, true);
            if (x != Double.NaN) {
                double z = function1.evaluateAt(x, y1);
                // Parametrisierter Punkt für die Sortierung
                double param = Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y1 - y1, 2));
                pointsSet.add(new IntersectionPoint(x, y1, z, param));
            }
        }

        // Kante links: (x1,y1) -> (x1,y2)
        if (diff00 * diff01 <= 0 && !Double.isNaN(diff00) && !Double.isNaN(diff01)) {
            double y = findRootNewton(function1, function2, y1, y2, x1, false);
            if (y != Double.NaN) {
                double z = function1.evaluateAt(x1, y);
                double param = Math.sqrt(Math.pow(x1 - x1, 2) + Math.pow(y - y1, 2));
                pointsSet.add(new IntersectionPoint(x1, y, z, param));
            }
        }

        // Kante oben: (x1,y2) -> (x2,y2)
        if (diff01 * diff11 <= 0 && !Double.isNaN(diff01) && !Double.isNaN(diff11)) {
            double x = findRootNewton(function1, function2, x1, x2, y2, true);
            if (x != Double.NaN) {
                double z = function1.evaluateAt(x, y2);
                double param = Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y2 - y1, 2));
                pointsSet.add(new IntersectionPoint(x, y2, z, param));
            }
        }

        // Kante rechts: (x2,y1) -> (x2,y2)
        if (diff10 * diff11 <= 0 && !Double.isNaN(diff10) && !Double.isNaN(diff11)) {
            double y = findRootNewton(function1, function2, y1, y2, x2, false);
            if (y != Double.NaN) {
                double z = function1.evaluateAt(x2, y);
                double param = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y - y1, 2));
                pointsSet.add(new IntersectionPoint(x2, y, z, param));
            }
        }

        // Optional: Rekursiv verfeinern für komplexe Schnittlinien
        double size = Math.max(x2 - x1, y2 - y1);
        if (size > TOLERANCE * 100 && hasComplexCurve(diff00, diff10, diff01, diff11)) {
            // Unterteile in vier Quadranten und prüfe jeden
            findIntersectionsInSquare(function1, function2, x1, y1, midX, midY, pointsSet);
            findIntersectionsInSquare(function1, function2, midX, y1, x2, midY, pointsSet);
            findIntersectionsInSquare(function1, function2, x1, midY, midX, y2, pointsSet);
            findIntersectionsInSquare(function1, function2, midX, midY, x2, y2, pointsSet);
        }
    }

    /**
     * Schnelltest, ob die Schnittlinie in diesem Bereich komplex sein könnte
     */
    private static boolean hasComplexCurve(double v00, double v10, double v01, double v11) {
        // Prüfe auf mehrere Vorzeichenwechsel oder starke Gradienten
        int sign00 = v00 > 0 ? 1 : (v00 < 0 ? -1 : 0);
        int sign10 = v10 > 0 ? 1 : (v10 < 0 ? -1 : 0);
        int sign01 = v01 > 0 ? 1 : (v01 < 0 ? -1 : 0);
        int sign11 = v11 > 0 ? 1 : (v11 < 0 ? -1 : 0);

        // Zähle Vorzeichenwechsel
        int changes = 0;
        if (sign00 != sign10 && sign00 != 0 && sign10 != 0)
            changes++;
        if (sign00 != sign01 && sign00 != 0 && sign01 != 0)
            changes++;
        if (sign01 != sign11 && sign01 != 0 && sign11 != 0)
            changes++;
        if (sign10 != sign11 && sign10 != 0 && sign11 != 0)
            changes++;

        // Komplexe Kurve wenn mehr als 2 Vorzeichenwechsel
        return changes > 2;
    }

    /**
     * Optimierte Nullstellensuche mit dem Newton-Raphson-Verfahren
     */
    private static double findRootNewton(
            Function3DParser function1, Function3DParser function2,
            double start, double end, double fixed, boolean isXVariable) {

        // Starte in der Mitte des Intervalls
        double current = (start + end) / 2;
        double prev;

        // Newton-Raphson-Iteration
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double diff, diffDeriv;

            if (isXVariable) {
                // Suche Nullstelle in x-Richtung
                diff = evaluateDifference(function1, function2, current, fixed);

                // Numerische Ableitung bilden
                double h = Math.max(1e-8, Math.abs(current) * 1e-8);
                double diffPlus = evaluateDifference(function1, function2, current + h, fixed);
                diffDeriv = (diffPlus - diff) / h;
            } else {
                // Suche Nullstelle in y-Richtung
                diff = evaluateDifference(function1, function2, fixed, current);

                // Numerische Ableitung bilden
                double h = Math.max(1e-8, Math.abs(current) * 1e-8);
                double diffPlus = evaluateDifference(function1, function2, fixed, current + h);
                diffDeriv = (diffPlus - diff) / h;
            }

            // Abbruch bei sehr kleiner Differenz
            if (Math.abs(diff) < TOLERANCE) {
                return current;
            }

            // Spezialfall: Ableitung nahe Null - verhindert Division durch (fast) Null
            if (Math.abs(diffDeriv) < 1e-10) {
                // Fallback auf Bisektion
                return (start + end) / 2;
            }

            // Newton-Schritt
            prev = current;
            current = current - diff / diffDeriv;

            // Begrenze auf das Intervall [start, end]
            if (current < start)
                current = start;
            if (current > end)
                current = end;

            // Konvergenz-Check
            if (Math.abs(current - prev) < TOLERANCE) {
                return current;
            }
        }

        // Fallback wenn keine Konvergenz
        double diff = evaluateDifference(function1, function2,
                isXVariable ? current : fixed,
                isXVariable ? fixed : current);

        return Math.abs(diff) < TOLERANCE * 10 ? current : Double.NaN;
    }

    /**
     * Verfolgt eine gefundene Schnittlinie durch Prediktor-Korrektor-Verfahren
     */
    private static void followIntersectionCurve(
            Function3DParser function1, Function3DParser function2,
            Set<IntersectionPoint> pointsSet,
            double xMin, double xMax, double yMin, double yMax,
            double step) {

        if (pointsSet.isEmpty()) {
            return; // Keine Startpunkte zum Verfolgen
        }

        // Konvertiere zu Liste, um einen guten Startpunkt auszuwählen
        List<IntersectionPoint> pointsList = new ArrayList<>(pointsSet);

        // Sortiere nach X-Koordinate für sinnvollen Startpunkt
        pointsList.sort(Comparator.comparingDouble(p -> p.x));

        // Wähle Punkte nahe am Rand als Startpunkte
        double margin = (xMax - xMin) * 0.1;
        List<IntersectionPoint> startPoints = new ArrayList<>();

        // Punkte nahe am linken Rand
        for (IntersectionPoint p : pointsList) {
            if (p.x < xMin + margin) {
                startPoints.add(p);
                break;
            }
        }

        // Punkte nahe am rechten Rand
        for (int i = pointsList.size() - 1; i >= 0; i--) {
            IntersectionPoint p = pointsList.get(i);
            if (p.x > xMax - margin) {
                startPoints.add(p);
                break;
            }
        }

        // Falls keine Randpunkte, nimm den ersten und letzten Punkt
        if (startPoints.isEmpty() && pointsList.size() >= 2) {
            startPoints.add(pointsList.get(0));
            startPoints.add(pointsList.get(pointsList.size() - 1));
        } else if (startPoints.isEmpty() && !pointsList.isEmpty()) {
            startPoints.add(pointsList.get(0));
        }

        // Kurvenverfolgung für jeden Startpunkt
        for (IntersectionPoint startPoint : startPoints) {
            // Verfolge in beide Richtungen
            traceIntersectionCurve(function1, function2, startPoint,
                    step, 1, xMin, xMax, yMin, yMax, pointsSet);
            traceIntersectionCurve(function1, function2, startPoint,
                    step, -1, xMin, xMax, yMin, yMax, pointsSet);
        }
    }

    /**
     * Verfolgt eine Schnittlinie von einem Startpunkt aus in eine Richtung
     */
    private static void traceIntersectionCurve(
            Function3DParser function1, Function3DParser function2,
            IntersectionPoint startPoint, double step, int direction,
            double xMin, double xMax, double yMin, double yMax,
            Set<IntersectionPoint> pointsSet) {

        double x = startPoint.x;
        double y = startPoint.y;
        double z = startPoint.z;

        // Parameter für die Kurve
        double paramDist = startPoint.pathDistance;

        // Maximal 100 Schritte, um Endlosschleifen zu vermeiden
        for (int i = 0; i < 100; i++) {
            // Prüfe, ob wir den Bereich verlassen haben
            if (x < xMin || x > xMax || y < yMin || y > yMax) {
                break;
            }

            // Berechne Tangente (numerische Ableitung)
            double h = step * 0.01;
            double diffX1 = evaluateDifference(function1, function2, x + h, y);
            double diffX2 = evaluateDifference(function1, function2, x - h, y);
            double diffY1 = evaluateDifference(function1, function2, x, y + h);
            double diffY2 = evaluateDifference(function1, function2, x, y - h);

            // Approximiere den Gradienten der Differenzfunktion
            double gradX = (diffX1 - diffX2) / (2 * h);
            double gradY = (diffY1 - diffY2) / (2 * h);

            // Tangentialrichtung (senkrecht zum Gradienten)
            double tangentX = -gradY;
            double tangentY = gradX;

            // Richtung anpassen
            tangentX *= direction;
            tangentY *= direction;

            // Normalisieren
            double length = Math.sqrt(tangentX * tangentX + tangentY * tangentY);
            if (length < 1e-10) {
                break; // Ende der Kurve oder Problem mit der Tangente
            }

            tangentX /= length;
            tangentY /= length;

            // Prädiktor-Schritt: Bewege in Tangentialrichtung
            double nextX = x + step * tangentX;
            double nextY = y + step * tangentY;

            // Korrektor-Schritt: Finde den genauen Punkt auf der Schnittlinie
            double correctedX = nextX;
            double correctedY = nextY;

            // Newtons Methode zur Korrektur
            for (int j = 0; j < 5; j++) {
                double diff = evaluateDifference(function1, function2, correctedX, correctedY);

                // Abbruch wenn nahe genug an der Nullstelle
                if (Math.abs(diff) < TOLERANCE) {
                    break;
                }

                // Gradienten berechnen (numerisch)
                double cGradX = (evaluateDifference(function1, function2, correctedX + h, correctedY) - diff) / h;
                double cGradY = (evaluateDifference(function1, function2, correctedX, correctedY + h) - diff) / h;

                // Newton-Schritt in Richtung der Nullstelle
                double gradLength = cGradX * cGradX + cGradY * cGradY;
                if (gradLength < 1e-10) {
                    break; // Problem mit dem Gradienten
                }

                correctedX -= diff * cGradX / gradLength;
                correctedY -= diff * cGradY / gradLength;

                // Begrenze auf den erlaubten Bereich
                correctedX = Math.max(xMin, Math.min(xMax, correctedX));
                correctedY = Math.max(yMin, Math.min(yMax, correctedY));
            }

            // Z-Wert berechnen
            double correctedZ = function1.evaluateAt(correctedX, correctedY);

            // Berechne neue Parameterdistanz
            paramDist += Math.sqrt(Math.pow(correctedX - x, 2) + Math.pow(correctedY - y, 2));

            // Punkt zur Ergebnismenge hinzufügen
            IntersectionPoint newPoint = new IntersectionPoint(
                    correctedX, correctedY, correctedZ, paramDist);

            // Prüfe, ob dieser Punkt bereits bekannt ist (verhindert Endlosschleifen)
            boolean isDuplicate = false;
            for (IntersectionPoint p : pointsSet) {
                if (Math.abs(p.x - correctedX) < TOLERANCE * 10 &&
                        Math.abs(p.y - correctedY) < TOLERANCE * 10) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                break; // Ende der Verfolgung bei bekanntem Punkt
            }

            pointsSet.add(newPoint);

            // Update für den nächsten Schritt
            x = correctedX;
            y = correctedY;
            z = correctedZ;
        }
    }

    /**
     * Leert den Cache der Funktionswerte
     */
    private static void clearCache() {
        functionValueCache.clear();
    }

    /**
     * Gibt die Standardfarbe für Schnittlinien zurück
     */
    public static Color getIntersectionColor() {
        return INTERSECTION_COLOR;
    }

    /**
     * Beendet den Thread-Pool bei Programmende
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // In der Plot3DIntersectionCalculator.java Datei:

    /**
     * Berechnet alle Schnittlinien zwischen allen Funktionspaaren im Modell
     * mit optimierter Performance und vollständiger Erkennung.
     */
    public static List<List<Plot3DPoint>> calculateAllIntersections(
            Plot3DModel model, double xMin, double xMax, double yMin, double yMax) {

        List<Plot3DModel.Function3DInfo> functions = model.getFunctions();
        if (functions.size() < 2) {
            return new ArrayList<>(); // Keine Schnitte möglich mit weniger als 2 Funktionen
        }

        // Liste für alle Schnittlinien aller Funktionspaare
        List<List<Plot3DPoint>> allIntersections = Collections.synchronizedList(new ArrayList<>());

        // WICHTIG: Sicherstellen, dass wir ALLE Funktionspaare überprüfen
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < functions.size() - 1; i++) {
            final int fi = i;
            for (int j = i + 1; j < functions.size(); j++) {
                final int fj = j;

                futures.add(executor.submit(() -> {
                    Function3DParser function1 = functions.get(fi).function;
                    Function3DParser function2 = functions.get(fj).function;

                    // Schnelltest deaktivieren oder sensitiver machen
                    // Immer eine vollständige Überprüfung durchführen, auch wenn
                    // der Schnelltest keine Schnitte vermutet
                    List<Plot3DPoint> intersection = calculateIntersectionRobust(
                            function1, function2, xMin, xMax, yMin, yMax);

                    if (!intersection.isEmpty()) {
                        allIntersections.add(intersection);
                    }
                }));
            }
        }

        // Auf Abschluss aller Berechnungen warten
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Fehler bei der parallelen Berechnung: " + e.getMessage());
            }
        }

        // Cache leeren, um Speicher freizugeben
        clearCache();

        return allIntersections;
    }

    /**
     * Robustere Version der Schnittlinienberechnung mit feinerem Raster
     * und sensitiverer Erkennung von Schnittpunkten.
     */
    private static List<Plot3DPoint> calculateIntersectionRobust(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax) {

        // Set für gefundene Schnittpunkte ohne Duplikate
        Set<IntersectionPoint> pointsSet = Collections.synchronizedSet(new HashSet<>());

        // Feineres Raster für die initiale Suche verwenden
        double baseStep = INITIAL_STEP_SIZE / 2.0; // Halbiere die Standard-Schrittweite

        // Zusätzliche adaptive Toleranz basierend auf der Wertebereichsgröße
        double domainSize = Math.max(xMax - xMin, yMax - yMin);
        double adaptiveTolerance = TOLERANCE * Math.max(1.0, Math.log10(domainSize) * 2);

        // Erste Phase: Verfeinertes Raster zum Finden von Schnittpunkten
        scanForIntersections(function1, function2, xMin, xMax, yMin, yMax,
                baseStep, pointsSet);

        // Zusätzliches Raster mit versetztem Ursprung für bessere Abdeckung
        scanForIntersections(function1, function2,
                xMin + baseStep / 2, xMax, yMin + baseStep / 2, yMax,
                baseStep, pointsSet);

        // Mehrfache Kurvenverfolgung mit verschiedenen Strategien
        if (!pointsSet.isEmpty()) {
            // Standard-Kurvenverfolgung
            followIntersectionCurve(function1, function2, pointsSet,
                    xMin, xMax, yMin, yMax, baseStep / 4);

            // Zusätzlich: Sekundäre Kurvenverfolgung mit anderen Parametern
            // zur Erkennung abgetrennter Kurventeile
            Set<IntersectionPoint> additionalPoints = new HashSet<>(pointsSet);
            followIntersectionCurveAdaptive(function1, function2, additionalPoints,
                    xMin, xMax, yMin, yMax);
            pointsSet.addAll(additionalPoints);
        }

        // Spezielle Behandlung für geschlossene Kurven (z.B. Kreise, Ellipsen)
        handleClosedCurves(function1, function2, pointsSet, xMin, xMax, yMin, yMax);

        // Wenn keine Punkte gefunden, früh zurückkehren
        if (pointsSet.isEmpty()) {
            return new ArrayList<>();
        }

        // Cluster die Punkte in separate Kurven basierend auf räumlicher Nähe
        List<List<IntersectionPoint>> clusters = clusterIntersectionPoints(pointsSet);

        // Konvertiere alle Cluster zu Plot3DPoint-Listen und füge sie zusammen
        List<Plot3DPoint> result = new ArrayList<>();

        for (List<IntersectionPoint> cluster : clusters) {
            // Organisiere die Punkte dieses Clusters
            if (cluster.size() >= 2) {
                // Für die Kurvenverfolgung und ähnliche Operationen wird ein Set benötigt
                // Konvertiere die Liste in ein Set für diese Operationen
                HashSet<IntersectionPoint> clusterSet = new HashSet<>(cluster);

                // Prüfe, ob dieser Cluster eine geschlossene Kurve sein könnte
                checkForClosedCurve(cluster);

                // Punkte dieses Clusters organisieren
                organizeCurvePoints(cluster);

                // Konvertiere zu Plot3DPoint und füge an Ergebnis an
                List<Plot3DPoint> clusterPoints = new ArrayList<>();
                for (IntersectionPoint p : cluster) {
                    clusterPoints.add(p.toPlot3DPoint());
                }

                // Ausdünnen, aber nicht zu stark
                clusterPoints = refinePointList(clusterPoints);

                // Zum Gesamtergebnis hinzufügen, mit einem Marker für die Trennung
                if (!result.isEmpty()) {
                    // Nur wenn nicht das erste Cluster, füge einen NaN-Punkt als Separator ein
                    // Damit wird verhindert, dass Lines zwischen verschiedenen Kurven gezeichnet
                    // werden
                    result.add(new Plot3DPoint(Double.NaN, Double.NaN, Double.NaN));
                }
                result.addAll(clusterPoints);
            }
        }

        return result;
    }

    /**
     * Cluster-Bildung: Teilt Schnittpunkte in separate Gruppen (Kurven) auf
     * basierend auf ihrem räumlichen Abstand
     */
    private static List<List<IntersectionPoint>> clusterIntersectionPoints(Set<IntersectionPoint> points) {
        List<IntersectionPoint> pointsList = new ArrayList<>(points);
        List<List<IntersectionPoint>> clusters = new ArrayList<>();

        if (pointsList.isEmpty()) {
            return clusters;
        }

        // Bestimme einen sinnvollen Cluster-Radius basierend auf dem Punkt-Abstand
        double clusterRadius = determineClusterRadius(pointsList);

        // Array für Cluster-Zugehörigkeit
        boolean[] assigned = new boolean[pointsList.size()];

        // Für jeden unzugewiesenen Punkt ein neues Cluster starten
        for (int i = 0; i < pointsList.size(); i++) {
            if (assigned[i])
                continue;

            List<IntersectionPoint> cluster = new ArrayList<>();
            IntersectionPoint seed = pointsList.get(i);
            cluster.add(seed);
            assigned[i] = true;

            // Wachstumsprozess: Füge nahe Punkte zum Cluster hinzu
            boolean changed;
            do {
                changed = false;
                for (int j = 0; j < pointsList.size(); j++) {
                    if (assigned[j])
                        continue;

                    // Prüfe Abstand zu jedem Punkt im aktuellen Cluster
                    boolean closeEnough = false;
                    for (IntersectionPoint clusterPoint : cluster) {
                        double dist = distance(clusterPoint, pointsList.get(j));
                        if (dist <= clusterRadius) {
                            closeEnough = true;
                            break;
                        }
                    }

                    if (closeEnough) {
                        cluster.add(pointsList.get(j));
                        assigned[j] = true;
                        changed = true;
                    }
                }
            } while (changed);

            // Cluster zur Liste der Cluster hinzufügen
            clusters.add(cluster);
        }

        return clusters;
    }

    /**
     * Berechnet den Abstand zwischen zwei Punkten
     */
    private static double distance(IntersectionPoint p1, IntersectionPoint p2) {
        return Math.sqrt(
                Math.pow(p1.x - p2.x, 2) +
                        Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Bestimmt einen angemessenen Radius für das Clustering
     * basierend auf der Verteilung der Punkte
     */
    private static double determineClusterRadius(List<IntersectionPoint> points) {
        if (points.size() <= 1) {
            return INITIAL_STEP_SIZE / 2;
        }

        // Berechne den minimalen Punktabstand
        double minDist = Double.POSITIVE_INFINITY;

        // HINWEIS: Hier für größere Punktmengen optimierter Algorithmus verwenden!
        // Für kleine bis mittlere Punktmengen ist dieser Ansatz jedoch akzeptabel.
        for (int i = 0; i < points.size() - 1; i++) {
            for (int j = i + 1; j < points.size(); j++) {
                double dist = distance(points.get(i), points.get(j));
                if (dist > 0 && dist < minDist) {
                    minDist = dist;
                }
            }
        }

        // Wenn kein minimaler Abstand gefunden (z.B. alle Punkte identisch)
        if (Double.isInfinite(minDist)) {
            return INITIAL_STEP_SIZE / 2;
        }

        // Histogramm der Punktabstände erstellen
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            for (int j = i + 1; j < points.size(); j++) {
                double dist = distance(points.get(i), points.get(j));
                if (dist > 0) {
                    distances.add(dist);
                }
            }
        }

        // Sortieren für Perzentilberechnung
        Collections.sort(distances);

        // Verwende 25. Perzentil als Basis und multipliziere mit einem Faktor
        int idx = Math.max(0, (int) (distances.size() * 0.25) - 1);
        double percentile25 = distances.get(idx);

        // Der Cluster-Radius sollte etwa 2-3 mal größer sein als der typische
        // Punktabstand
        // innerhalb einer zusammenhängenden Kurve, aber kleiner als der typische
        // Abstand
        // zwischen getrennten Kurven
        return Math.max(percentile25 * 2.5, minDist * 5.0);
    }

    /**
     * Scannt potenzielle Problembereiche mit höherer Auflösung
     */
    private static void scanPotentialProblemAreas(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax,
            Set<IntersectionPoint> pointsSet) {

        // Speziell für kreisförmige Schnitte: Scanne den Bereich um den Ursprung
        // Viele mathematische Funktionen haben Symmetrie oder Besonderheiten am
        // Ursprung
        if (Math.abs(xMin) <= 5 && Math.abs(xMax) >= 5 &&
                Math.abs(yMin) <= 5 && Math.abs(yMax) >= 5) {

            double originX = 0;
            double originY = 0;
            double radius = 2.0; // Radius um den Ursprung für intensives Scannen

            // Feines Raster im Bereich um den Ursprung
            scanForIntersections(function1, function2,
                    originX - radius, originX + radius,
                    originY - radius, originY + radius,
                    INITIAL_STEP_SIZE / 4, pointsSet);
        }

        // Weiterer spezieller Scan für potenzielle Schnittkreise
        // Suche speziell in einem Ring-Bereich, wo typischerweise Kreisschnitte
        // auftreten
        for (double radius = 1.0; radius <= 5.0; radius += 1.0) {
            // Scan entlang des Kreises in Polarkoordinaten
            int numSamples = Math.max(20, (int) (2 * Math.PI * radius / (INITIAL_STEP_SIZE / 3)));
            for (int i = 0; i < numSamples; i++) {
                double angle = 2 * Math.PI * i / numSamples;
                double x = radius * Math.cos(angle);
                double y = radius * Math.sin(angle);

                // Kleiner Bereich um diesen Punkt
                double probeSize = INITIAL_STEP_SIZE / 3;
                scanForIntersections(function1, function2,
                        x - probeSize, x + probeSize,
                        y - probeSize, y + probeSize,
                        probeSize / 2, pointsSet);
            }
        }
    }

    /**
     * Prüft, ob ein Punktcluster eine geschlossene Kurve bilden könnte
     * und fügt ggf. verbindende Punkte hinzu
     */
    private static void checkForClosedCurve(List<IntersectionPoint> cluster) {
        if (cluster.size() < 8) {
            return; // Zu wenige Punkte für sinnvolle Analyse
        }

        // Sortiere nach X-Wert für einheitlichen Ausgangspunkt
        cluster.sort(Comparator.comparingDouble(p -> p.x));

        // Prüfe, ob der erste und letzte Punkt nahe beieinander liegen
        double maxDist = INITIAL_STEP_SIZE * 1.5;
        double minDist = Double.POSITIVE_INFINITY;
        int closestStartIdx = -1;
        int closestEndIdx = -1;

        // Suche Paare von Punkten, die nahe beieinander liegen, aber in der Liste weit
        // entfernt sind
        for (int i = 0; i < cluster.size() / 3; i++) { // Ersten Drittel mit
            for (int j = cluster.size() * 2 / 3; j < cluster.size(); j++) { // Letztem Drittel vergleichen
                double dist = distance(cluster.get(i), cluster.get(j));
                if (dist < maxDist && dist < minDist) {
                    minDist = dist;
                    closestStartIdx = i;
                    closestEndIdx = j;
                }
            }
        }

        // Wenn ein geeignetes Paar gefunden wurde, prüfe auf geschlossene Kurve
        if (closestStartIdx >= 0 && closestEndIdx >= 0) {
            IntersectionPoint start = cluster.get(closestStartIdx);
            IntersectionPoint end = cluster.get(closestEndIdx);

            // Berechne den durchschnittlichen Abstand zwischen benachbarten Punkten in der
            // Liste
            double totalDist = 0;
            int count = 0;
            for (int i = 0; i < cluster.size() - 1; i++) {
                double dist = distance(cluster.get(i), cluster.get(i + 1));
                if (dist > 0) {
                    totalDist += dist;
                    count++;
                }
            }
            double avgDist = count > 0 ? totalDist / count : INITIAL_STEP_SIZE;

            // Wenn der Abstand zwischen den gefundenen Punkten nicht zu groß ist
            // im Vergleich zum durchschnittlichen Punktabstand, handelt es sich
            // wahrscheinlich um eine geschlossene Kurve
            if (minDist <= avgDist * 3.0) {
                // Füge interpolierte Punkte zwischen den beiden Punkten ein
                int steps = Math.max(1, (int) (minDist / avgDist));
                for (int i = 1; i < steps; i++) {
                    double t = (double) i / steps;
                    double interpX = start.x + t * (end.x - start.x);
                    double interpY = start.y + t * (end.y - start.y);
                    double interpZ = start.z + t * (end.z - start.z);

                    cluster.add(new IntersectionPoint(interpX, interpY, interpZ, 0.0));
                }
            }
        }
    }

    /**
     * Verbesserte Version der adaptiven Kurvenverfolgung
     */
    private static void followIntersectionCurveAdaptive(
            Function3DParser function1, Function3DParser function2,
            Set<IntersectionPoint> pointsSet,
            double xMin, double xMax, double yMin, double yMax) {

        // Heuristiken für die Erkennung mehrerer getrennter Kurvensegmente
        // 1. Teile den Bereich in mehrere Unterbereiche auf
        int subdivisions = 3;
        double xStep = (xMax - xMin) / subdivisions;
        double yStep = (yMax - yMin) / subdivisions;

        for (int i = 0; i < subdivisions; i++) {
            for (int j = 0; j < subdivisions; j++) {
                double subXMin = xMin + i * xStep;
                double subXMax = xMin + (i + 1) * xStep;
                double subYMin = yMin + j * yStep;
                double subYMax = yMin + (j + 1) * yStep;

                // Prüfe, ob wir in diesem Unterbereich Punkte haben
                boolean hasPoints = false;
                IntersectionPoint startPoint = null;

                for (IntersectionPoint p : pointsSet) {
                    if (p.x >= subXMin && p.x <= subXMax &&
                            p.y >= subYMin && p.y <= subYMax) {
                        hasPoints = true;
                        startPoint = p;
                        break;
                    }
                }

                // Wenn der Unterbereich Punkte enthält, starte dort eine zusätzliche
                // Kurvenverfolgung mit feinerem Schritt
                if (hasPoints && startPoint != null) {
                    double fineStep = Math.min(xStep, yStep) / 10.0;
                    traceIntersectionCurve(function1, function2, startPoint,
                            fineStep, 1, subXMin, subXMax, subYMin, subYMax,
                            pointsSet);
                    traceIntersectionCurve(function1, function2, startPoint,
                            fineStep, -1, subXMin, subXMax, subYMin, subYMax,
                            pointsSet);
                }
                // Sonst: Prüfe, ob es dennoch Schnittpunkte in diesem Unterbereich gibt
                else {
                    scanForIntersectionsDetailed(function1, function2,
                            subXMin, subXMax, subYMin, subYMax,
                            pointsSet);
                }
            }
        }
    }

    /**
     * Detailliertere Suche nach Schnittpunkten in einem kleineren Unterbereich
     */
    private static void scanForIntersectionsDetailed(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax,
            Set<IntersectionPoint> pointsSet) {

        // Feineres Raster für kleine Bereiche
        double fineStep = Math.min(xMax - xMin, yMax - yMin) / 8.0;

        // Prüfe, ob es in diesem Bereich potenzielle Schnitte gibt
        // durch Auswertung an den Eckpunkten und im Zentrum
        double centerX = (xMin + xMax) / 2;
        double centerY = (yMin + yMax) / 2;

        double diff00 = evaluateDifference(function1, function2, xMin, yMin);
        double diff10 = evaluateDifference(function1, function2, xMax, yMin);
        double diff01 = evaluateDifference(function1, function2, xMin, yMax);
        double diff11 = evaluateDifference(function1, function2, xMax, yMax);
        double diffCenter = evaluateDifference(function1, function2, centerX, centerY);

        // Prüfe auf Vorzeichenwechsel zwischen den Eckpunkten und dem Zentrum
        boolean potentialIntersection = false;

        // Wenn die Differenz am Zentrum nahe 0 ist, gibt es wahrscheinlich einen
        // Schnittpunkt
        if (Math.abs(diffCenter) < TOLERANCE * 10) {
            potentialIntersection = true;
        }

        // Prüfe Vorzeichenwechsel zwischen Ecken und Zentrum
        if ((diff00 * diffCenter <= 0 && !Double.isNaN(diff00) && !Double.isNaN(diffCenter)) ||
                (diff10 * diffCenter <= 0 && !Double.isNaN(diff10) && !Double.isNaN(diffCenter)) ||
                (diff01 * diffCenter <= 0 && !Double.isNaN(diff01) && !Double.isNaN(diffCenter)) ||
                (diff11 * diffCenter <= 0 && !Double.isNaN(diff11) && !Double.isNaN(diffCenter))) {
            potentialIntersection = true;
        }

        // Wenn potenzieller Schnitt gefunden, verwende ein feineres Raster
        if (potentialIntersection) {
            scanForIntersections(function1, function2, xMin, xMax, yMin, yMax,
                    fineStep, pointsSet);
        }
    }

    /**
     * Organisiert die Punkte einer Kurve in einer sinnvollen Reihenfolge
     * mit spezieller Behandlung für geschlossene Kurven
     */
    private static void organizeCurvePoints(List<IntersectionPoint> points) {
        if (points.size() <= 2) {
            return; // Nichts zu sortieren bei 0, 1 oder 2 Punkten
        }

        // Sortiere zuerst nach X-Koordinate für einen sinnvollen Startpunkt
        points.sort(Comparator.comparingDouble(p -> p.x));

        List<IntersectionPoint> organized = new ArrayList<>();
        organized.add(points.get(0));
        points.remove(0);

        // Greedy-Algorithmus: Füge immer den nächsten Punkt hinzu
        while (!points.isEmpty()) {
            IntersectionPoint last = organized.get(organized.size() - 1);
            int nextIndex = findNearestPoint(last, points);
            organized.add(points.get(nextIndex));
            points.remove(nextIndex);
        }

        // Überprüfung, ob die Kurve möglicherweise geschlossen ist
        IntersectionPoint first = organized.get(0);
        IntersectionPoint last = organized.get(organized.size() - 1);
        double distFirstLast = Math.sqrt(
                Math.pow(first.x - last.x, 2) +
                        Math.pow(first.y - last.y, 2));

        // Wenn der erste und letzte Punkt sehr nahe beieinander liegen,
        // handelt es sich möglicherweise um eine geschlossene Kurve.
        boolean isClosedCurve = distFirstLast < INITIAL_STEP_SIZE / 2;

        // HIER IST DIE FEHLENDE LOGIK:
        // Behandlung für geschlossene Kurven
        if (isClosedCurve) {
            // Bei geschlossenen Kurven wählen wir einen besseren Startpunkt
            // z.B. den Punkt mit dem kleinsten y-Wert (am weitesten unten)
            int lowestPointIndex = 0;
            double minY = organized.get(0).y;

            for (int i = 1; i < organized.size(); i++) {
                if (organized.get(i).y < minY) {
                    minY = organized.get(i).y;
                    lowestPointIndex = i;
                }
            }

            // Rotiere die Liste, sodass der niedrigste Punkt am Anfang steht
            if (lowestPointIndex > 0) {
                List<IntersectionPoint> rotated = new ArrayList<>();
                // Füge Punkte von lowestPointIndex bis Ende hinzu
                for (int i = lowestPointIndex; i < organized.size(); i++) {
                    rotated.add(organized.get(i));
                }
                // Füge Punkte von Anfang bis lowestPointIndex hinzu
                for (int i = 0; i < lowestPointIndex; i++) {
                    rotated.add(organized.get(i));
                }
                organized = rotated;
            }

            // Füge interpolierte Punkte zwischen dem letzten und dem ersten Punkt hinzu,
            // um die Kurve explizit zu schließen
            if (distFirstLast > 0) {
                first = organized.get(0);
                last = organized.get(organized.size() - 1);

                // Füge 3 interpolierte Punkte hinzu, um einen glatteren Übergang zu
                // gewährleisten
                for (int i = 1; i <= 3; i++) {
                    double t = i / 4.0; // 0.25, 0.5, 0.75
                    double interpX = last.x + t * (first.x - last.x);
                    double interpY = last.y + t * (first.y - last.y);
                    double interpZ = last.z + t * (first.z - last.z);

                    // Erzeuge interpolierten Punkt
                    IntersectionPoint interpPoint = new IntersectionPoint(
                            interpX, interpY, interpZ, 0.0); // pathDistance wird später aktualisiert

                    organized.add(interpPoint);
                }

                // Optional: Füge den ersten Punkt nochmal am Ende hinzu für vollständigen Kreis
                organized.add(new IntersectionPoint(
                        first.x, first.y, first.z, 0.0)); // pathDistance wird später aktualisiert
            }
        }

        // Berechne neue Pfaddistanzen für alle Punkte
        double cumulativeDistance = 0.0;
        for (int i = 0; i < organized.size(); i++) {
            IntersectionPoint current = organized.get(i);

            // Setze die Pfaddistanz des aktuellen Punktes
            current.pathDistance = cumulativeDistance;

            // Berechne die Distanz zum nächsten Punkt, falls es einen gibt
            if (i < organized.size() - 1) {
                IntersectionPoint next = organized.get(i + 1);
                double distance = Math.sqrt(
                        Math.pow(current.x - next.x, 2) +
                                Math.pow(current.y - next.y, 2));
                cumulativeDistance += distance;
            }
        }

        // Leere die Originalliste und füge die organisierten Punkte hinzu
        points.clear();
        points.addAll(organized);
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
                            Math.pow(reference.y - p.y, 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * Spezielle Behandlung für geschlossene Kurven wie Kreise und Ellipsen
     */
    private static void handleClosedCurves(
            Function3DParser function1, Function3DParser function2,
            Set<IntersectionPoint> pointsSet,
            double xMin, double xMax, double yMin, double yMax) {

        if (pointsSet.isEmpty()) {
            return; // Keine Punkte zum Analysieren
        }

        // Konvertiere zu Liste für die Analyse
        List<IntersectionPoint> points = new ArrayList<>(pointsSet);

        // Einfacher Test für geschlossene Kurven:
        // Prüfe, ob es Punkte gibt, die nahe beieinander liegen, aber
        // in der Parametrisierung weit entfernt sind (Hinweis auf geschlossene Kurve)
        if (points.size() >= 10) { // Nur bei genügend Punkten
            double minDistance = Double.POSITIVE_INFINITY;
            int minIdx1 = -1, minIdx2 = -1;

            // Suche die zwei am nächsten liegenden Punkte, die in der Liste
            // nicht direkt aufeinander folgen
            for (int i = 0; i < points.size(); i++) {
                for (int j = i + 3; j < points.size(); j++) { // Mindestabstand 3 in der Liste
                    IntersectionPoint p1 = points.get(i);
                    IntersectionPoint p2 = points.get(j);

                    double dist = Math.sqrt(
                            Math.pow(p1.x - p2.x, 2) +
                                    Math.pow(p1.y - p2.y, 2));

                    if (dist < minDistance) {
                        minDistance = dist;
                        minIdx1 = i;
                        minIdx2 = j;
                    }
                }
            }

            // Wenn zwei Punkte gefunden wurden, die geometrisch nahe, aber in der Liste
            // weit entfernt sind, könnte es eine geschlossene Kurve sein
            // Füge dann zusätzliche Punkte zwischen ihnen ein, um die Kurve zu schließen
            if (minIdx1 >= 0 && minIdx2 >= 0 && minDistance < INITIAL_STEP_SIZE / 4) {
                IntersectionPoint p1 = points.get(minIdx1);
                IntersectionPoint p2 = points.get(minIdx2);

                // Berechne einen oder mehrere Zwischenpunkte
                for (double t = 0.25; t < 1.0; t += 0.25) {
                    double interpX = p1.x + t * (p2.x - p1.x);
                    double interpY = p1.y + t * (p2.y - p1.y);

                    // Finde genauen Z-Wert durch Neuberechnung
                    double z = function1.evaluateAt(interpX, interpY);

                    // Füge interpolierten Punkt hinzu
                    IntersectionPoint newPoint = new IntersectionPoint(
                            interpX, interpY, z, p1.pathDistance + t * minDistance);
                    pointsSet.add(newPoint);
                }
            }
        }
    }

    /**
     * Verbesserte Punktlisten-Verfeinerung mit Erhalt wichtiger Details
     */
    private static List<Plot3DPoint> refinePointList(List<Plot3DPoint> points) {
        if (points.size() <= 6) {
            return points; // Bereits kurze Liste nicht weiter reduzieren
        }

        List<Plot3DPoint> refined = new ArrayList<>();
        refined.add(points.get(0)); // Ersten Punkt behalten

        // Adaptive Toleranz basierend auf der Kurvenlänge und Komplexität
        double totalLength = 0;
        for (int i = 1; i < points.size(); i++) {
            Plot3DPoint prev = points.get(i - 1);
            Plot3DPoint curr = points.get(i);
            totalLength += Math.sqrt(
                    Math.pow(prev.getX() - curr.getX(), 2) +
                            Math.pow(prev.getY() - curr.getY(), 2) +
                            Math.pow(prev.getZ() - curr.getZ(), 2));
        }

        // Minium von 20 Punkten oder 1 Punkt pro 0.1 Einheiten Kurvenlänge
        int minPoints = Math.max(20, (int) (totalLength / 0.1));

        // Douglas-Peucker ähnlicher Algorithmus mit adaptiever Toleranz
        // Bewahre mehr Punkte an gekrümmten Stellen
        double minDistanceSquared = 0.005 * 0.005; // Basismindestabstand zwischen Punkten

        for (int i = 1; i < points.size() - 1; i++) {
            Plot3DPoint prev = refined.get(refined.size() - 1);
            Plot3DPoint curr = points.get(i);
            Plot3DPoint next = points.get(i + 1);

            // Abstand zum letzten behaltenen Punkt
            double distSq = Math.pow(prev.getX() - curr.getX(), 2) +
                    Math.pow(prev.getY() - curr.getY(), 2) +
                    Math.pow(prev.getZ() - curr.getZ(), 2);

            // Winkel zwischen den Vektoren (prev->curr) und (curr->next)
            double angle = calculateAngle(prev, curr, next);

            // Behalte den Punkt, wenn einer der folgenden Fälle zutrifft:
            // 1. Punkt ist weit genug vom letzten entfernt
            // 2. Kurve macht einen signifikanten Knick (großer Winkel)
            // 3. Punkt gehört zur Stützungsminimalmenge (gleichmäßige Verteilung)
            if (distSq >= minDistanceSquared ||
                    Math.abs(angle) >= 0.1 ||
                    refined.size() < minPoints / 2 && i % (points.size() / minPoints + 1) == 0) {
                refined.add(curr);
            }
        }

        refined.add(points.get(points.size() - 1)); // Letzten Punkt behalten

        return refined;
    }

    /**
     * Berechnet den Winkel zwischen drei 3D-Punkten
     */
    private static double calculateAngle(Plot3DPoint a, Plot3DPoint b, Plot3DPoint c) {
        // Vektoren AB und BC
        double abx = b.getX() - a.getX();
        double aby = b.getY() - a.getY();
        double abz = b.getZ() - a.getZ();

        double bcx = c.getX() - b.getX();
        double bcy = c.getY() - b.getY();
        double bcz = c.getZ() - b.getZ();

        // Längen der Vektoren
        double abLength = Math.sqrt(abx * abx + aby * aby + abz * abz);
        double bcLength = Math.sqrt(bcx * bcx + bcy * bcy + bcz * bcz);

        if (abLength < 1e-10 || bcLength < 1e-10) {
            return 0; // Verhindere Division durch Null
        }

        // Normalisiere Vektoren
        abx /= abLength;
        aby /= abLength;
        abz /= abLength;

        bcx /= bcLength;
        bcy /= bcLength;
        bcz /= bcLength;

        // Skalarprodukt der normalisierten Vektoren
        double dotProduct = abx * bcx + aby * bcy + abz * bcz;

        // Begrenze den Bereich auf [-1, 1] für numerische Stabilität
        dotProduct = Math.max(-1, Math.min(1, dotProduct));

        // Winkel in Radianten
        return Math.acos(dotProduct);
    }
}