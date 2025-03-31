package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;
import parser.Function3DParser;

/**
 * Optimierter, pixelbasierter Schnittpunktsrechner für 3D-Funktionen
 * Konzentriert sich ausschließlich auf sichtbare Punkte mit adaptiver Präzision
 * basierend auf dem Zoom-Level
 */
public class PixelBasedIntersectionCalculator {
    // Konstanten für die Leistungsoptimierung
    private static final double BASE_TOLERANCE = 1e-6;
    private static final int MAX_CACHE_SIZE = 50;
    private static final Color INTERSECTION_COLOR = Color.RED;

    // Cache für berechnete Schnittpunkte – Verwendung eines LRU-Caches zur
    // Speicherverwaltung
    private static final Map<String, List<List<Plot3DPoint>>> intersectionCache = new LinkedHashMap<String, List<List<Plot3DPoint>>>(
            MAX_CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<List<Plot3DPoint>>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // Thread-Pool für parallele Berechnungen
    private static final ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Cache-Schlüsselgenerator basierend auf den Funktionen und dem sichtbaren
    // Bereich
    private static String generateCacheKey(Plot3DModel model, double xMin, double xMax, double yMin, double yMax,
            double zoom) {
        StringBuilder key = new StringBuilder();
        for (Plot3DModel.Function3DInfo func : model.getFunctions()) {
            key.append(func.getExpression()).append(":");
        }
        // Füge den sichtbaren Bereich und den Zoom mit reduzierter Genauigkeit in den
        // Schlüssel ein
        key.append(String.format("%.1f:%.1f:%.1f:%.1f:%.1f", xMin, xMax, yMin, yMax, zoom));
        return key.toString();
    }

    /**
     * Berechnet alle sichtbaren Schnittpunkte zwischen Funktionen mit
     * pixelbasierter Genauigkeit
     */
    public static List<List<Plot3DPoint>> calculateVisibleIntersections(
            Plot3DModel model, Plot3DView view,
            double displayScale, int screenWidth, int screenHeight) {

        // Überprüfe, ob mindestens zwei Funktionen vorhanden sind
        List<Plot3DModel.Function3DInfo> functions = model.getFunctions();
        if (functions.size() < 2) {
            return new ArrayList<>();
        }

        // Erzeuge einen Cache-Schlüssel, der den sichtbaren Bereich und den Zoom-Level
        // enthält
        String cacheKey = generateCacheKey(model,
                view.getXMin(), view.getXMax(),
                view.getYMin(), view.getYMax(),
                view.getScale());

        // Zuerst den Cache prüfen
        synchronized (intersectionCache) {
            if (intersectionCache.containsKey(cacheKey)) {
                return intersectionCache.get(cacheKey);
            }
        }

        // Berechne das Verhältnis von Pixel zu Weltkoordinaten für adaptive Präzision
        double pixelToWorldRatioX = (view.getXMax() - view.getXMin()) / screenWidth;
        double pixelToWorldRatioY = (view.getYMax() - view.getYMin()) / screenHeight;

        // Adaptives Sampling basierend auf Bildschirmgröße und Zoom-Level
        // Niedrigere Auflösung bei rausgezoomt, höhere Auflösung bei reingezoomt
        int baseSamples = 150; // Erhöht von 100 auf 150 für eine feinere Auflösung
        double zoomFactor = Math.min(3.0, Math.max(0.8, view.getScale()));
        int samplesX = (int) (baseSamples * zoomFactor);
        int samplesY = (int) (baseSamples * zoomFactor);

        // Anpassung an das Seitenverhältnis des Bildschirms
        double aspectRatio = (double) screenWidth / screenHeight;
        if (aspectRatio > 1) {
            samplesX = (int) (samplesX * aspectRatio);
        } else {
            samplesY = (int) (samplesY / aspectRatio);
        }

        // Begrenze auf sinnvolle Grenzen – höheres oberes Limit für mehr Präzision
        samplesX = Math.min(300, Math.max(50, samplesX));
        samplesY = Math.min(300, Math.max(50, samplesY));

        // Ergebnisliste für alle Schnittkurven
        List<List<Plot3DPoint>> allIntersections = Collections.synchronizedList(new ArrayList<>());

        // Für jedes Paar von Funktionen
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < functions.size() - 1; i++) {
            final int fi = i;
            for (int j = i + 1; j < functions.size(); j++) {
                final int fj = j;

                // Erstelle finale Kopien der Variablen zur Verwendung in der Lambda-Funktion
                final int finalSamplesX = samplesX;
                final int finalSamplesY = samplesY;
                final double finalPixelToWorldRatioX = pixelToWorldRatioX;
                final double finalPixelToWorldRatioY = pixelToWorldRatioY;

                futures.add(executor.submit(() -> {
                    Function3DParser function1 = functions.get(fi).function;
                    Function3DParser function2 = functions.get(fj).function;

                    // Berechne die Schnittpunkte für dieses Funktionspaar
                    List<Plot3DPoint> intersectionCurve = calculateIntersectionCurve(
                            function1, function2,
                            view.getXMin(), view.getXMax(), view.getYMin(), view.getYMax(),
                            finalSamplesX, finalSamplesY, finalPixelToWorldRatioX, finalPixelToWorldRatioY);

                    if (!intersectionCurve.isEmpty()) {
                        allIntersections.add(intersectionCurve);
                    }
                }));
            }
        }

        // Warte, bis alle Berechnungen abgeschlossen sind
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.err.println("Fehler bei der Berechnung der Schnittpunkte: " + e.getMessage());
            }
        }

        // Cache das Ergebnis
        synchronized (intersectionCache) {
            intersectionCache.put(cacheKey, allIntersections);
        }

        return allIntersections;
    }

    /**
     * Berechnet die Schnittkurve zwischen zwei Funktionen mit Pixelgenauigkeit
     */
    private static List<Plot3DPoint> calculateIntersectionCurve(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax,
            int samplesX, int samplesY, double pixelToWorldRatioX, double pixelToWorldRatioY) {

        // Adaptive Toleranz basierend auf Zoom-Level / Pixel-Verhältnis
        double tolerance = Math.max(BASE_TOLERANCE, Math.min(pixelToWorldRatioX, pixelToWorldRatioY));

        // Schrittweiten für das Sampling
        double stepX = (xMax - xMin) / samplesX;
        double stepY = (yMax - yMin) / samplesY;

        // Raster für Differenzwerte
        double[][] diffValues = new double[samplesX + 1][samplesY + 1];

        // Berechne Differenzwerte an den Rasterpunkten
        for (int i = 0; i <= samplesX; i++) {
            double x = xMin + i * stepX;
            for (int j = 0; j <= samplesY; j++) {
                double y = yMin + j * stepY;
                try {
                    double z1 = function1.evaluateAt(x, y);
                    double z2 = function2.evaluateAt(x, y);
                    diffValues[i][j] = z1 - z2;
                } catch (Exception e) {
                    diffValues[i][j] = Double.NaN;
                }
            }
        }

        // Finde Schnittpunkte
        List<Plot3DPoint> intersectionPoints = new ArrayList<>();

        // Suche nach Nulldurchgängen in den Rasterzellen
        for (int i = 0; i < samplesX; i++) {
            for (int j = 0; j < samplesY; j++) {
                double diff00 = diffValues[i][j];
                double diff10 = diffValues[i + 1][j];
                double diff01 = diffValues[i][j + 1];
                double diff11 = diffValues[i + 1][j + 1];

                // Überspringe, falls ein Wert NaN ist
                if (Double.isNaN(diff00) || Double.isNaN(diff10) ||
                        Double.isNaN(diff01) || Double.isNaN(diff11)) {
                    continue;
                }

                // Prüfe auf Vorzeichenwechsel entlang der Kanten
                boolean hasIntersection = false;

                // Überprüfe alle vier Kanten der Zelle
                if (diff00 * diff10 <= 0)
                    hasIntersection = true; // Obere Kante
                if (diff00 * diff01 <= 0)
                    hasIntersection = true; // Linke Kante
                if (diff10 * diff11 <= 0)
                    hasIntersection = true; // Rechte Kante
                if (diff01 * diff11 <= 0)
                    hasIntersection = true; // Untere Kante

                if (hasIntersection) {
                    // Verfeinere den Schnittpunkt mittels binärer Suche
                    Plot3DPoint refined = refineIntersectionPoint(
                            function1, function2,
                            xMin + i * stepX, xMin + (i + 1) * stepX,
                            yMin + j * stepY, yMin + (j + 1) * stepY,
                            tolerance);

                    if (refined != null) {
                        intersectionPoints.add(refined);
                    }
                }
            }
        }

        // Verbinde die Punkte zu einer Kurve
        return connectIntersectionPoints(intersectionPoints, stepX, stepY);
    }

    /**
     * Verfeinert einen Schnittpunkt mittels Gradientenabstiegsverfahren
     * Diese Methode liefert deutlich genauere Schnittpunkte
     */
    private static Plot3DPoint refineIntersectionPoint(
            Function3DParser function1, Function3DParser function2,
            double x1, double x2, double y1, double y2, double tolerance) {

        // Ausgangspunkt in der Mitte der Zelle
        double x = (x1 + x2) / 2;
        double y = (y1 + y2) / 2;

        // Verwende adaptive Iterationen – mehr für Bereiche mit höherer Präzision
        int maxIterations = 8;
        double stepSize = Math.min(x2 - x1, y2 - y1) * 0.1;
        double minStepSize = tolerance * 0.01;

        for (int i = 0; i < maxIterations && stepSize > minStepSize; i++) {
            try {
                // Berechne die Differenz der Funktionswerte
                double z1 = function1.evaluateAt(x, y);
                double z2 = function2.evaluateAt(x, y);
                double diff = z1 - z2;

                // Schnittpunkt mit ausreichender Präzision gefunden
                if (Math.abs(diff) < tolerance) {
                    return new Plot3DPoint(x, y, z1);
                }

                // Berechne den Gradienten der Differenzfunktion
                double h = Math.max(1e-6, (x2 - x1) * 0.01);

                // Numerischer Gradient in x-Richtung
                double diffX1 = function1.evaluateAt(x + h, y) - function2.evaluateAt(x + h, y);
                double diffX2 = function1.evaluateAt(x - h, y) - function2.evaluateAt(x - h, y);
                double gradX = (diffX1 - diffX2) / (2 * h);

                // Numerischer Gradient in y-Richtung
                double diffY1 = function1.evaluateAt(x, y + h) - function2.evaluateAt(x, y + h);
                double diffY2 = function1.evaluateAt(x, y - h) - function2.evaluateAt(x, y - h);
                double gradY = (diffY1 - diffY2) / (2 * h);

                // Berechne die Größe des Gradienten zur Normierung
                double gradMagnitude = Math.sqrt(gradX * gradX + gradY * gradY);

                // Vermeide Division durch Null
                if (gradMagnitude < 1e-10) {
                    // Versuche einen anderen Punkt, falls der Gradient zu klein ist
                    x = x1 + Math.random() * (x2 - x1);
                    y = y1 + Math.random() * (y2 - y1);
                    continue;
                }

                // Normalisiere den Gradienten und bewege dich in die entgegengesetzte Richtung
                double moveX = -gradX / gradMagnitude * diff * stepSize;
                double moveY = -gradY / gradMagnitude * diff * stepSize;

                // Aktualisiere die Position
                double newX = x + moveX;
                double newY = y + moveY;

                // Stelle sicher, dass wir innerhalb der Zelle bleiben
                newX = Math.max(x1, Math.min(x2, newX));
                newY = Math.max(y1, Math.min(y2, newY));

                // Überprüfe, ob wir uns signifikant bewegt haben
                double moveDist = Math.sqrt((newX - x) * (newX - x) + (newY - y) * (newY - y));
                if (moveDist < tolerance * 0.01) {
                    // Verringere die Schrittgröße, wenn kein Fortschritt erzielt wird
                    stepSize *= 0.5;
                }

                x = newX;
                y = newY;

            } catch (Exception e) {
                // Bei numerischen Fehlern, versuche einen leicht abweichenden Punkt
                x = x1 + Math.random() * (x2 - x1);
                y = y1 + Math.random() * (y2 - y1);
                stepSize *= 0.5;
            }
        }

        // Verwende die beste gefundene Annäherung
        try {
            double z1 = function1.evaluateAt(x, y);
            double z2 = function2.evaluateAt(x, y);

            // Verwende den durchschnittlichen z-Wert für bessere visuelle Ergebnisse
            double z = (z1 + z2) / 2;
            return new Plot3DPoint(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verbindet die Schnittpunkte zu einer durchgehenden Kurve
     * Erweiterte Version mit verbesserter Kurvenerkennung und -vernetzung
     */
    private static List<Plot3DPoint> connectIntersectionPoints(
            List<Plot3DPoint> points, double stepX, double stepY) {

        if (points.size() < 2) {
            return points;
        }

        // Clustere die Punkte in separate Kurven basierend auf der räumlichen Nähe
        List<List<Plot3DPoint>> curves = clusterPointsIntoCurves(points, stepX, stepY);
        List<Plot3DPoint> connectedPoints = new ArrayList<>();

        // Organisiere für jede Kurve die Punkte und füge sie dem Ergebnis hinzu
        for (List<Plot3DPoint> curve : curves) {
            if (curve.size() < 2)
                continue;

            // Ordne die Punkte entlang der Kurve für eine bessere Kontinuität
            List<Plot3DPoint> organizedCurve = organizePointsAlongCurve(curve);

            // Füge einen Kurventrenner hinzu, falls es nicht die erste Kurve ist
            if (!connectedPoints.isEmpty()) {
                connectedPoints.add(new Plot3DPoint(Double.NaN, Double.NaN, Double.NaN));
            }

            // Füge die Kurvenpunkte hinzu
            connectedPoints.addAll(organizedCurve);
        }

        // Wende Glättung an, um Unebenheiten in der Kurve zu reduzieren
        return smoothCurve(connectedPoints);
    }

    /**
     * Clustert Punkte in separate Kurven basierend auf der räumlichen Nähe
     */
    private static List<List<Plot3DPoint>> clusterPointsIntoCurves(
            List<Plot3DPoint> points, double stepX, double stepY) {

        List<List<Plot3DPoint>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[points.size()];

        // Berechne die adaptive Cluster-Distanz
        double clusterDistance = Math.sqrt(stepX * stepX + stepY * stepY) * 2.0;

        // Verarbeite jeden nicht zugeordneten Punkt
        for (int i = 0; i < points.size(); i++) {
            if (assigned[i])
                continue;

            // Beginne einen neuen Cluster
            List<Plot3DPoint> cluster = new ArrayList<>();
            cluster.add(points.get(i));
            assigned[i] = true;

            // Wachse den Cluster mithilfe von Breitensuche
            Queue<Integer> queue = new LinkedList<>();
            queue.add(i);

            while (!queue.isEmpty()) {
                int current = queue.poll();
                Plot3DPoint currentPoint = points.get(current);

                // Überprüfe alle anderen nicht zugeordneten Punkte
                for (int j = 0; j < points.size(); j++) {
                    if (assigned[j])
                        continue;

                    Plot3DPoint candidate = points.get(j);
                    double dist = Math.sqrt(
                            Math.pow(currentPoint.getX() - candidate.getX(), 2) +
                                    Math.pow(currentPoint.getY() - candidate.getY(), 2));

                    // Falls nah genug, zum Cluster hinzufügen
                    if (dist <= clusterDistance) {
                        cluster.add(candidate);
                        assigned[j] = true;
                        queue.add(j);
                    }
                }
            }

            // Füge den Cluster dem Ergebnis hinzu
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * Ordnet die Punkte entlang einer Kurve für eine bessere Kontinuität
     */
    private static List<Plot3DPoint> organizePointsAlongCurve(List<Plot3DPoint> curvePoints) {
        if (curvePoints.size() <= 2)
            return curvePoints;

        List<Plot3DPoint> organized = new ArrayList<>();
        List<Plot3DPoint> remaining = new ArrayList<>(curvePoints);

        // Erkenne, ob es sich um eine geschlossene Kurve (z. B. einen Kreis) handeln
        // könnte
        boolean potentialClosedCurve = isPotentialClosedCurve(curvePoints);

        // Für offene Kurven (in den meisten Fällen) den besten Startpunkt finden
        if (!potentialClosedCurve) {
            // Versuche zunächst, die Endpunkte zu finden
            findEndpoints(remaining, organized);
            if (!organized.isEmpty()) {
                remaining.removeAll(organized);
            } else {
                // Falls keine klaren Endpunkte vorhanden sind, beginne mit dem am weitesten
                // links oder rechts liegenden Punkt
                if (Math.random() < 0.5) {
                    // Beginne von links
                    remaining.sort((p1, p2) -> Double.compare(p1.getX(), p2.getX()));
                } else {
                    // Beginne von rechts
                    remaining.sort((p1, p2) -> Double.compare(p2.getX(), p1.getX()));
                }
                organized.add(remaining.remove(0));
            }
        } else {
            // Für geschlossene Kurven beginne mit dem am weitesten rechts liegenden Punkt
            remaining.sort((p1, p2) -> Double.compare(p2.getX(), p1.getX()));
            organized.add(remaining.remove(0));
        }

        // Falls immer noch leer (Fallback), beginne mit dem ersten Punkt
        if (organized.isEmpty() && !remaining.isEmpty()) {
            organized.add(remaining.remove(0));
        }

        // Erzeuge die Kurve mithilfe des nächstgelegenen Nachbarn
        while (!remaining.isEmpty()) {
            Plot3DPoint current = organized.get(organized.size() - 1);
            int nearestIndex = findNearestPointIndex(current, remaining);
            organized.add(remaining.remove(nearestIndex));
        }

        // Für geschlossene Kurven den Kreis gegebenenfalls explizit schließen
        if (potentialClosedCurve) {
            Plot3DPoint first = organized.get(0);
            Plot3DPoint last = organized.get(organized.size() - 1);

            // Berechne den Abstand zwischen dem ersten und letzten Punkt
            double dist = Math.sqrt(
                    Math.pow(first.getX() - last.getX(), 2) +
                            Math.pow(first.getY() - last.getY(), 2));

            // Berechne den durchschnittlichen Abstand zwischen benachbarten Punkten der
            // Kurve
            double totalDist = 0;
            int count = 0;
            for (int i = 0; i < organized.size() - 1; i++) {
                Plot3DPoint p1 = organized.get(i);
                Plot3DPoint p2 = organized.get(i + 1);
                totalDist += Math.sqrt(
                        Math.pow(p1.getX() - p2.getX(), 2) +
                                Math.pow(p1.getY() - p2.getY(), 2));
                count++;
            }
            double avgDist = count > 0 ? totalDist / count : 0;

            // Schließe nur, wenn die Endpunkte im Verhältnis zum durchschnittlichen
            // Punktabstand sehr nahe beieinander liegen
            if (dist < avgDist * 1.5) {
                organized.add(new Plot3DPoint(first.getX(), first.getY(), first.getZ()));
            }
        }

        return organized;
    }

    /**
     * Erkennt, ob ein Satz von Punkten eine geschlossene Kurve (wie einen Kreis)
     * bilden könnte
     * Konservativere Implementierung, die für Randkurven keine Schließung erzwingt
     */
    private static boolean isPotentialClosedCurve(List<Plot3DPoint> points) {
        if (points.size() < 12)
            return false; // Es werden mehr Punkte benötigt, um eine zuverlässige Erkennung einer
                          // geschlossenen Kurve zu gewährleisten

        // Überprüfe, ob der erste und der letzte Punkt sehr nahe beieinander liegen
        Plot3DPoint first = points.get(0);
        Plot3DPoint last = points.get(points.size() - 1);

        double firstLastDist = Math.sqrt(
                Math.pow(first.getX() - last.getX(), 2) +
                        Math.pow(first.getY() - last.getY(), 2));

        // Berechne den approximativen "Schwerpunkt"
        double sumX = 0, sumY = 0;
        for (Plot3DPoint p : points) {
            sumX += p.getX();
            sumY += p.getY();
        }
        double centerX = sumX / points.size();
        double centerY = sumY / points.size();

        // Berechne den durchschnittlichen Abstand zum Schwerpunkt
        double sumDist = 0;
        double maxDist = 0;
        double minDist = Double.MAX_VALUE;

        for (Plot3DPoint p : points) {
            double dist = Math.sqrt(
                    Math.pow(p.getX() - centerX, 2) +
                            Math.pow(p.getY() - centerY, 2));
            sumDist += dist;
            maxDist = Math.max(maxDist, dist);
            minDist = Math.min(minDist, dist);
        }
        double avgDist = sumDist / points.size();

        // Berechne die Varianz der Abstände zum Schwerpunkt
        double variance = 0;
        for (Plot3DPoint p : points) {
            double dist = Math.sqrt(
                    Math.pow(p.getX() - centerX, 2) +
                            Math.pow(p.getY() - centerY, 2));
            variance += Math.pow(dist - avgDist, 2);
        }
        variance /= points.size();
        double stdDev = Math.sqrt(variance);

        // Überprüfe, ob Punkte nahe dem Rand liegen, was auf eine offene Kurve
        // hinweisen würde
        boolean hasBoundaryPoints = false;
        double boundaryTolerance = 0.05; // 5 % vom Rand gelten als Grenze

        // Ermittle die Grenzen aller Punkte
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Plot3DPoint p : points) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        double xRange = maxX - minX;
        double yRange = maxY - minY;

        // Überprüfe auf Punkte am Rand
        for (Plot3DPoint p : points) {
            if (Math.abs(p.getX() - minX) < boundaryTolerance * xRange ||
                    Math.abs(p.getX() - maxX) < boundaryTolerance * xRange ||
                    Math.abs(p.getY() - minY) < boundaryTolerance * yRange ||
                    Math.abs(p.getY() - maxY) < boundaryTolerance * yRange) {
                hasBoundaryPoints = true;
                break;
            }
        }

        // Strenge Bedingungen, damit eine Kurve als geschlossen gilt:
        // 1. Der erste und der letzte Punkt müssen sehr nahe beieinander liegen
        // 2. Die Standardabweichung muss im Vergleich zum Durchschnittswert niedrig
        // sein (was auf eine kreisförmige Form hindeutet)
        // 3. Das Verhältnis von maximalem zu minimalem Abstand muss verhältnismäßig
        // niedrig sein (was auf Rundheit hinweist)
        // 4. Es sollten keine Punkte sehr nahe am Rand des Datensatzes liegen
        boolean isRoundShape = stdDev / avgDist < 0.2;
        boolean isUniformRadius = maxDist / minDist < 2.0;
        boolean endpointsClose = firstLastDist < avgDist * 0.3;

        return isRoundShape && isUniformRadius && endpointsClose && !hasBoundaryPoints;
    }

    /**
     * Versucht, die Endpunkte einer offenen Kurve zu finden
     */
    private static void findEndpoints(List<Plot3DPoint> points, List<Plot3DPoint> endpoints) {
        if (points.size() < 4)
            return;

        // Für jeden Punkt, zähle die Nachbarn innerhalb einer bestimmten Entfernung
        double neighborDist = 0;

        // Berechne den durchschnittlichen Abstand zwischen Punkten
        double totalDist = 0;
        int count = 0;
        for (int i = 0; i < Math.min(points.size(), 100); i++) {
            for (int j = i + 1; j < Math.min(points.size(), 100); j++) {
                totalDist += Math.sqrt(
                        Math.pow(points.get(i).getX() - points.get(j).getX(), 2) +
                                Math.pow(points.get(i).getY() - points.get(j).getY(), 2));
                count++;
            }
        }
        if (count > 0) {
            neighborDist = (totalDist / count) * 1.5;
        } else {
            return; // Kann die Nachbarschaftsgröße nicht bestimmen
        }

        // Zähle die Nachbarn für jeden Punkt
        int[] neighborCount = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < points.size(); j++) {
                if (i == j)
                    continue;

                double dist = Math.sqrt(
                        Math.pow(points.get(i).getX() - points.get(j).getX(), 2) +
                                Math.pow(points.get(i).getY() - points.get(j).getY(), 2));

                if (dist <= neighborDist) {
                    neighborCount[i]++;
                }
            }
        }

        // Finde Punkte mit den wenigsten Nachbarn (wahrscheinlich Endpunkte)
        int minNeighbors = Integer.MAX_VALUE;
        for (int count1 : neighborCount) {
            if (count1 > 0 && count1 < minNeighbors) {
                minNeighbors = count1;
            }
        }

        // Finde höchstens 2 Endpunkte
        for (int i = 0; i < points.size() && endpoints.size() < 2; i++) {
            if (neighborCount[i] == minNeighbors) {
                endpoints.add(points.get(i));
            }
        }
    }

    /**
     * Wendet Glättung an, um Unebenheiten in Kurven zu reduzieren
     */
    private static List<Plot3DPoint> smoothCurve(List<Plot3DPoint> points) {
        if (points.size() < 4)
            return points;

        List<Plot3DPoint> smoothed = new ArrayList<>();
        boolean inSegment = false;
        List<Plot3DPoint> currentSegment = new ArrayList<>();

        // Verarbeite die Punkte segmentweise (getrennt durch NaN)
        for (Plot3DPoint p : points) {
            if (Double.isNaN(p.getX())) {
                // Segmentende, glätten und zum Ergebnis hinzufügen
                if (!currentSegment.isEmpty()) {
                    smoothed.addAll(smoothSegment(currentSegment));
                    currentSegment.clear();
                }
                smoothed.add(p); // Füge den Trenner hinzu
                inSegment = false;
            } else {
                // Füge den Punkt dem aktuellen Segment hinzu
                currentSegment.add(p);
                inSegment = true;
            }
        }

        // Bearbeite das letzte Segment
        if (!currentSegment.isEmpty()) {
            smoothed.addAll(smoothSegment(currentSegment));
        }

        return smoothed;
    }

    /**
     * Findet den Index des nächstgelegenen Punktes zu einem Referenzpunkt
     */
    private static int findNearestPointIndex(Plot3DPoint reference, List<Plot3DPoint> points) {
        int nearest = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            Plot3DPoint candidate = points.get(i);
            double distance = Math.sqrt(
                    Math.pow(reference.getX() - candidate.getX(), 2) +
                            Math.pow(reference.getY() - candidate.getY(), 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * Wendet eine Gaußsche Glättung auf ein einzelnes Kurvensegment an
     */
    private static List<Plot3DPoint> smoothSegment(List<Plot3DPoint> segment) {
        if (segment.size() < 4)
            return segment; // Nicht genügend Punkte zum Glätten

        List<Plot3DPoint> smoothed = new ArrayList<>();

        // Behalte immer den ersten und letzten Punkt unverändert bei
        smoothed.add(segment.get(0));

        // Wende eine Gaußsche Glättung auf die inneren Punkte an
        for (int i = 1; i < segment.size() - 1; i++) {
            // Zentraler Punkt
            Plot3DPoint p0 = segment.get(i);

            // Benachbarte Punkte
            Plot3DPoint p1 = segment.get(Math.max(0, i - 1));
            Plot3DPoint p2 = segment.get(Math.min(segment.size() - 1, i + 1));

            // Gaußsche Gewichte (höheres Gewicht für den zentralen Punkt)
            double w0 = 0.6;
            double w1 = 0.2;
            double w2 = 0.2;

            // Gewichteter Durchschnitt der Koordinaten
            double x = w0 * p0.getX() + w1 * p1.getX() + w2 * p2.getX();
            double y = w0 * p0.getY() + w1 * p1.getY() + w2 * p2.getY();
            double z = w0 * p0.getZ() + w1 * p1.getZ() + w2 * p2.getZ();

            smoothed.add(new Plot3DPoint(x, y, z));
        }

        // Füge den letzten Punkt hinzu
        smoothed.add(segment.get(segment.size() - 1));

        return smoothed;
    }

    /**
     * Vereinfacht die Punktliste durch Entfernen redundanter Punkte
     */
    private static List<Plot3DPoint> simplifyPointList(List<Plot3DPoint> points, double stepX, double stepY) {
        if (points.size() <= 3) {
            return points;
        }

        List<Plot3DPoint> simplified = new ArrayList<>();
        simplified.add(points.get(0)); // Behalte immer den ersten Punkt

        // Minimale quadratische Distanz zwischen den Punkten
        double minDistSq = Math.min(stepX, stepY) * Math.min(stepX, stepY) * 0.25;

        // Behalte im Auge, ob der aktuelle Punkt Teil eines Trenner-Bereichs ist
        boolean inSeparator = false;

        for (int i = 1; i < points.size() - 1; i++) {
            Plot3DPoint prev = simplified.get(simplified.size() - 1);
            Plot3DPoint curr = points.get(i);
            Plot3DPoint next = points.get(i + 1);

            // Füge immer NaN-Trennerpunkte hinzu
            if (Double.isNaN(curr.getX())) {
                simplified.add(curr);
                inSeparator = true;
                continue;
            }

            // Setze das Trenner-Flag nach einem NaN-Punkt zurück
            if (inSeparator) {
                simplified.add(curr);
                inSeparator = false;
                continue;
            }

            // Abstand zum vorherigen Punkt
            double distSq = Math.pow(prev.getX() - curr.getX(), 2) +
                    Math.pow(prev.getY() - curr.getY(), 2);

            // Winkel, gebildet von vorheriger, aktueller und nächster Punkt
            double angle = 0;
            if (!Double.isNaN(next.getX())) {
                double dx1 = curr.getX() - prev.getX();
                double dy1 = curr.getY() - prev.getY();
                double dx2 = next.getX() - curr.getX();
                double dy2 = next.getY() - curr.getY();

                // Berechne den Winkel mittels Skalarprodukt
                double dot = dx1 * dx2 + dy1 * dy2;
                double mag1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                double mag2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

                if (mag1 > 0 && mag2 > 0) {
                    angle = Math.acos(dot / (mag1 * mag2));
                    if (Double.isNaN(angle))
                        angle = 0;
                }
            }

            // Behalte den Punkt, wenn er weit genug entfernt ist oder einen signifikanten
            // Winkel bildet
            if (distSq >= minDistSq || Math.abs(angle) >= 0.15) {
                simplified.add(curr);
            }
        }

        simplified.add(points.get(points.size() - 1)); // Behalte immer den letzten Punkt

        return simplified;
    }

    /**
     * Zeichnet Schnittkurven auf den Grafik-Kontext mit verbesserter Darstellung
     */
    public static void drawIntersectionCurves(
            Graphics2D g2d, Plot3DTransformer transformer,
            List<List<Plot3DPoint>> intersections,
            Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {

        if (intersections.isEmpty()) {
            return;
        }

        // Speichere den ursprünglichen Strich und die Rendering-Hinweise
        Stroke originalStroke = g2d.getStroke();
        Object originalAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Aktiviere Antialiasing für glattere Kurven
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Setze Farbe und Strich für Schnittpunktslinien
        g2d.setColor(INTERSECTION_COLOR);
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Transformationsparameter
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Berechne den Normierungsfaktor
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Rotationsparameter
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne vorab Sinus- und Kosinuswerte
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Zeichne jede Schnittkurve mithilfe von Pfaden für bessere Qualität
        for (List<Plot3DPoint> curve : intersections) {
            Path2D path = new Path2D.Double();
            boolean pathStarted = false;

            // Transformiere alle Punkte zunächst, um sie für die Tiefensortierung
            // vorzubereiten
            List<int[]> screenPoints = new ArrayList<>();
            List<Double> zValues = new ArrayList<>();

            for (Plot3DPoint point : curve) {
                // Überspringe NaN-Punkte (Kurventrenner)
                if (Double.isNaN(point.getX())) {
                    // Füge einen Marker für Pfadunterbrechungen hinzu
                    screenPoints.add(null);
                    zValues.add(Double.NaN);
                    continue;
                }

                // Transformiere den Punkt
                Plot3DPoint transformedPoint = transformer.transformPoint(
                        point.getX(), point.getY(), point.getZ(),
                        xCenter, yCenter, zCenter,
                        factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY());

                // Speichere den z-Wert für eine mögliche Tiefensortierung
                zValues.add(transformedPoint.getZ());

                // Projiziere in Bildschirmkoordinaten
                int[] screenPos = transformer.projectToScreen(
                        transformedPoint, displayScale, xOffset, yOffset);

                screenPoints.add(screenPos);
            }

            // Erstelle den Pfad
            for (int i = 0; i < screenPoints.size(); i++) {
                int[] screenPos = screenPoints.get(i);

                // Behandle Trenner (null-Punkte)
                if (screenPos == null) {
                    pathStarted = false;
                    continue;
                }

                // Beginne ein neues Pfadsegment
                if (!pathStarted) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    pathStarted = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                }
            }

            // Zeichne den kompletten Pfad
            g2d.draw(path);
        }

        // Stelle die ursprünglichen Grafikeinstellungen wieder her
        g2d.setStroke(originalStroke);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialias);
    }

    /**
     * Leert den Schnittpunkt-Cache
     */
    public static void clearCache() {
        synchronized (intersectionCache) {
            intersectionCache.clear();
        }
    }

    /**
     * Gibt die Farbe der Schnittpunktslinien zurück
     */
    public static Color getIntersectionColor() {
        return INTERSECTION_COLOR;
    }

    /**
     * Fährt den Executor-Service herunter
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
