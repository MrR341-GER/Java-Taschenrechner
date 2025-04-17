package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Rendert 3D-Funktionsplots als Gitternetze
 * Mit verbesserter Tiefensortierung für korrekte Darstellung von
 * Überschneidungen
 */
public class Plot3DFunctionRenderer {
    // Transformer für Koordinatenumrechnung
    private final Plot3DTransformer transformer;
    // Farbschema für Heatmap-Modus
    private final Plot3DColorScheme colorScheme;

    // Innere Klasse zur Speicherung von Dreiecken mit Tiefeninformation
    private static class Triangle implements Comparable<Triangle> {
        int[] xPoints;
        int[] yPoints;
        Color color;
        double depth; // Mittlere Z-Koordinate für die Tiefensortierung

        Triangle(int[] xPoints, int[] yPoints, Color color, double depth) {
            this.xPoints = xPoints;
            this.yPoints = yPoints;
            this.color = color;
            this.depth = depth;
        }

        @Override
        public int compareTo(Triangle other) {
            // Sortiere von hinten nach vorne (größerer Z-Wert = weiter hinten)
            return Double.compare(other.depth, this.depth);
        }
    }

    // Innere Klasse zur Speicherung von Liniensegmenten mit Tiefeninformation
    private static class LineSegment implements Comparable<LineSegment> {
        int x1, y1, x2, y2;
        Color color;
        double depth; // Mittlere Z-Koordinate für die Tiefensortierung

        LineSegment(int x1, int y1, int x2, int y2, Color color, double depth) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.depth = depth;
        }

        @Override
        public int compareTo(LineSegment other) {
            // Sortiere von hinten nach vorne (größerer Z-Wert = weiter hinten)
            return Double.compare(other.depth, this.depth);
        }
    }

    public Plot3DFunctionRenderer(Plot3DTransformer transformer) {
        this.transformer = transformer;
        this.colorScheme = Plot3DColorScheme.createDefault();
    }

    /**
     * Zeichnet alle Funktionen im Modell mit korrekter Tiefensortierung,
     * sodass Überschneidungen korrekt dargestellt werden
     */
    public void drawFunctions(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset,
            boolean useHeatmap, boolean useSolidSurface) {

        if (useSolidSurface) {
            // Für undurchsichtige Oberflächendarstellung:
            // Sammle zuerst alle Dreiecke von allen Funktionen
            List<Triangle> allTriangles = new ArrayList<>();

            // Verarbeite jede Funktion, um ihre Dreiecke zu sammeln
            for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
                // Überspringe unsichtbare Funktionen
                if (!functionInfo.isVisible()) {
                    continue;
                }

                allTriangles.addAll(collectTrianglesFromFunction(
                        functionInfo, model, displayScale, xOffset, yOffset, useHeatmap));
            }

            // Sortiere alle Dreiecke nach Tiefe (Painter's algorithm)
            Collections.sort(allTriangles);

            // Zeichne alle Dreiecke in sortierter Reihenfolge (von hinten nach vorne)
            for (Triangle triangle : allTriangles) {
                g2d.setColor(triangle.color);
                g2d.fillPolygon(triangle.xPoints, triangle.yPoints, 3);
            }

            // Nach dem Füllen aller Dreiecke, zeichne das Drahtgitter darüber, falls
            // gewünscht
            if (view.isShowGrid()) {
                drawWireframeOverlay(g2d, model, view, displayScale, xOffset, yOffset, useHeatmap);
            }
        } else {
            // Für Drahtgittermodus:
            // Sammle alle Liniensegmente von allen Funktionen
            List<LineSegment> allLineSegments = new ArrayList<>();

            // Verarbeite jede Funktion, um ihre Liniensegmente zu sammeln
            for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
                // Überspringe unsichtbare Funktionen
                if (!functionInfo.isVisible()) {
                    continue;
                }

                allLineSegments.addAll(collectLineSegmentsFromFunction(
                        functionInfo, model, displayScale, xOffset, yOffset, useHeatmap));
            }

            // Sortiere alle Liniensegmente nach Tiefe
            Collections.sort(allLineSegments);

            // Zeichne alle Liniensegmente in sortierter Reihenfolge (von hinten nach vorne)
            for (LineSegment segment : allLineSegments) {
                g2d.setColor(segment.color);
                g2d.drawLine(segment.x1, segment.y1, segment.x2, segment.y2);
            }
        }
    }

    /**
     * Überladene Methode für Rückwärtskompatibilität
     */
    public void drawFunctions(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        drawFunctions(g2d, model, view, displayScale, xOffset, yOffset, useHeatmap, view.isUseSolidSurface());
    }

    /**
     * Sammelt Dreiecke einer Funktion für die undurchsichtige
     * Oberflächendarstellung
     */
    private List<Triangle> collectTrianglesFromFunction(
            Plot3DModel.Function3DInfo functionInfo, Plot3DModel model,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {

        List<Triangle> triangles = new ArrayList<>();

        // Gitterpunkte für diese Funktion holen
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        if (gridPoints == null)
            return triangles;

        int resolution = gridPoints.length;

        // Lichtquellenvektor für die Schattierung (normalisiert)
        double[] lightSource = { 0.5, 0.5, 1.0 };
        double lightMagnitude = Math.sqrt(
                lightSource[0] * lightSource[0] +
                        lightSource[1] * lightSource[1] +
                        lightSource[2] * lightSource[2]);
        lightSource[0] /= lightMagnitude;
        lightSource[1] /= lightMagnitude;
        lightSource[2] /= lightMagnitude;

        // Jedes Gitterquadrat (in zwei Dreiecke aufgeteilt) verarbeiten
        for (int i = 0; i < resolution - 1; i++) {
            for (int j = 0; j < resolution - 1; j++) {
                // Vier Punkte, die ein Quadrat bilden
                Plot3DPoint p1 = gridPoints[i][j][0]; // Originalpunkte für Z-Wert und Normalenberechnung
                Plot3DPoint p2 = gridPoints[i + 1][j][0];
                Plot3DPoint p3 = gridPoints[i + 1][j + 1][0];
                Plot3DPoint p4 = gridPoints[i][j + 1][0];

                // Transformierte Punkte für die Tiefenberechnung
                Plot3DPoint p1t = gridPoints[i][j][1];
                Plot3DPoint p2t = gridPoints[i + 1][j][1];
                Plot3DPoint p3t = gridPoints[i + 1][j + 1][1];
                Plot3DPoint p4t = gridPoints[i][j + 1][1];

                // Projizierte Punkte für die Bildschirmkoordinaten
                Plot3DPoint p1p = gridPoints[i][j][2];
                Plot3DPoint p2p = gridPoints[i + 1][j][2];
                Plot3DPoint p3p = gridPoints[i + 1][j + 1][2];
                Plot3DPoint p4p = gridPoints[i][j + 1][2];

                // Bildschirmkoordinaten berechnen
                int[] s1 = transformer.projectToScreen(p1p, displayScale, xOffset, yOffset);
                int[] s2 = transformer.projectToScreen(p2p, displayScale, xOffset, yOffset);
                int[] s3 = transformer.projectToScreen(p3p, displayScale, xOffset, yOffset);
                int[] s4 = transformer.projectToScreen(p4p, displayScale, xOffset, yOffset);

                // Normale für das erste Dreieck (p1, p2, p3) berechnen
                double[] v1 = { p2.getX() - p1.getX(), p2.getY() - p1.getY(), p2.getZ() - p1.getZ() };
                double[] v2 = { p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ() };

                double[] normal = {
                        v1[1] * v2[2] - v1[2] * v2[1], // Nx = Uy*Vz - Uz*Vy
                        v1[2] * v2[0] - v1[0] * v2[2], // Ny = Uz*Vx - Ux*Vz
                        v1[0] * v2[1] - v1[1] * v2[0] // Nz = Ux*Vy - Uy*Vx
                };

                // Normale normalisieren
                double normalMagnitude = Math.sqrt(
                        normal[0] * normal[0] +
                                normal[1] * normal[1] +
                                normal[2] * normal[2]);
                if (normalMagnitude > 0) {
                    normal[0] /= normalMagnitude;
                    normal[1] /= normalMagnitude;
                    normal[2] /= normalMagnitude;
                }

                // Backface Culling - nur Dreiecke zeichnen, die zur Kamera zeigen
                boolean isVisible = normal[2] >= 0;

                if (isVisible) {
                    // Farbe basierend auf Z-Wert oder Funktionsfarbe bestimmen
                    Color baseColor;
                    if (useHeatmap) {
                        // Z-Wert auf 0-1 Bereich normalisieren für Farbzuordnung
                        double normalizedZ = (p1.getZ() - model.getZMin()) /
                                (model.getZMax() - model.getZMin());
                        normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                        baseColor = colorScheme.getColorForValue(normalizedZ);
                    } else {
                        baseColor = functionInfo.getColor();
                    }

                    // Beleuchtungseffekt berechnen (Lambertsche Schattierung)
                    double dotProduct = normal[0] * lightSource[0] +
                            normal[1] * lightSource[1] +
                            normal[2] * lightSource[2];
                    dotProduct = Math.abs(dotProduct);

                    // Farbe basierend auf Beleuchtung anpassen
                    int red = (int) (baseColor.getRed() * (0.4 + 0.6 * dotProduct));
                    int green = (int) (baseColor.getGreen() * (0.4 + 0.6 * dotProduct));
                    int blue = (int) (baseColor.getBlue() * (0.4 + 0.6 * dotProduct));

                    // Farbwerte in gültigen Bereich bringen
                    red = Math.max(0, Math.min(255, red));
                    green = Math.max(0, Math.min(255, green));
                    blue = Math.max(0, Math.min(255, blue));

                    Color shadedColor = new Color(red, green, blue);

                    // Tiefe für das erste Dreieck berechnen (mittlerer Z-Wert der transformierten
                    // Punkte)
                    double depth1 = (p1t.getZ() + p2t.getZ() + p3t.getZ()) / 3.0;

                    // Dreiecksobjekt für das erste Dreieck erstellen
                    int[] xPoints1 = { s1[0], s2[0], s3[0] };
                    int[] yPoints1 = { s1[1], s2[1], s3[1] };
                    triangles.add(new Triangle(xPoints1, yPoints1, shadedColor, depth1));

                    // Normale für das zweite Dreieck (p1, p3, p4) berechnen
                    double[] v3 = { p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ() };
                    double[] v4 = { p4.getX() - p1.getX(), p4.getY() - p1.getY(), p4.getZ() - p1.getZ() };

                    double[] normal2 = {
                            v3[1] * v4[2] - v3[2] * v4[1],
                            v3[2] * v4[0] - v3[0] * v4[2],
                            v3[0] * v4[1] - v3[1] * v4[0]
                    };

                    // Zweite Normale normalisieren
                    double normal2Magnitude = Math.sqrt(
                            normal2[0] * normal2[0] +
                                    normal2[1] * normal2[1] +
                                    normal2[2] * normal2[2]);
                    if (normal2Magnitude > 0) {
                        normal2[0] /= normal2Magnitude;
                        normal2[1] /= normal2Magnitude;
                        normal2[2] /= normal2Magnitude;
                    }

                    // Backface Culling für das zweite Dreieck
                    boolean isVisible2 = normal2[2] >= 0;

                    if (isVisible2) {
                        // Beleuchtung für das zweite Dreieck berechnen
                        double dotProduct2 = normal2[0] * lightSource[0] +
                                normal2[1] * lightSource[1] +
                                normal2[2] * lightSource[2];
                        dotProduct2 = Math.abs(dotProduct2);

                        // Farbe für das zweite Dreieck anpassen
                        int red2 = (int) (baseColor.getRed() * (0.4 + 0.6 * dotProduct2));
                        int green2 = (int) (baseColor.getGreen() * (0.4 + 0.6 * dotProduct2));
                        int blue2 = (int) (baseColor.getBlue() * (0.4 + 0.6 * dotProduct2));

                        red2 = Math.max(0, Math.min(255, red2));
                        green2 = Math.max(0, Math.min(255, green2));
                        blue2 = Math.max(0, Math.min(255, blue2));

                        Color shadedColor2 = new Color(red2, green2, blue2);

                        // Tiefe für das zweite Dreieck berechnen
                        double depth2 = (p1t.getZ() + p3t.getZ() + p4t.getZ()) / 3.0;

                        // Dreiecksobjekt für das zweite Dreieck erstellen
                        int[] xPoints2 = { s1[0], s3[0], s4[0] };
                        int[] yPoints2 = { s1[1], s3[1], s4[1] };
                        triangles.add(new Triangle(xPoints2, yPoints2, shadedColor2, depth2));
                    }
                }
            }
        }

        return triangles;
    }

    /**
     * Sammelt Liniensegmente einer Funktion für die Drahtgitterdarstellung
     */
    private List<LineSegment> collectLineSegmentsFromFunction(
            Plot3DModel.Function3DInfo functionInfo, Plot3DModel model,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {

        List<LineSegment> lineSegments = new ArrayList<>();

        // Gitterpunkte für diese Funktion holen
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        if (gridPoints == null)
            return lineSegments;

        int resolution = gridPoints.length;

        // Horizontale Linien (X-Richtung)
        for (int j = 0; j < resolution; j++) {
            for (int i = 0; i < resolution - 1; i++) {
                // Originalpunkte für die Farbbestimmung
                Plot3DPoint p1 = gridPoints[i][j][0];
                Plot3DPoint p2 = gridPoints[i + 1][j][0];

                // Transformierte Punkte für die Tiefenberechnung
                Plot3DPoint p1t = gridPoints[i][j][1];
                Plot3DPoint p2t = gridPoints[i + 1][j][1];

                // Projizierte Punkte für die Bildschirmkoordinaten
                Plot3DPoint p1p = gridPoints[i][j][2];
                Plot3DPoint p2p = gridPoints[i + 1][j][2];

                // Bildschirmkoordinaten berechnen
                int[] s1 = transformer.projectToScreen(p1p, displayScale, xOffset, yOffset);
                int[] s2 = transformer.projectToScreen(p2p, displayScale, xOffset, yOffset);

                // Farbe bestimmen
                Color color;
                if (useHeatmap) {
                    // Mittlerer Z-Wert für die Segmentfarbe
                    double avgZ = (p1.getZ() + p2.getZ()) / 2.0;
                    double normalizedZ = (avgZ - model.getZMin()) / (model.getZMax() - model.getZMin());
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    color = colorScheme.getColorForValue(normalizedZ);
                } else {
                    color = functionInfo.getColor();
                }

                // Segmenttiefe berechnen (mittlerer Z-Wert der transformierten Punkte)
                double depth = (p1t.getZ() + p2t.getZ()) / 2.0;

                // Liniensegment hinzufügen
                lineSegments.add(new LineSegment(s1[0], s1[1], s2[0], s2[1], color, depth));
            }
        }

        // Vertikale Linien (Y-Richtung)
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution - 1; j++) {
                // Originalpunkte für die Farbbestimmung
                Plot3DPoint p1 = gridPoints[i][j][0];
                Plot3DPoint p2 = gridPoints[i][j + 1][0];

                // Transformierte Punkte für die Tiefenberechnung
                Plot3DPoint p1t = gridPoints[i][j][1];
                Plot3DPoint p2t = gridPoints[i][j + 1][1];

                // Projizierte Punkte für die Bildschirmkoordinaten
                Plot3DPoint p1p = gridPoints[i][j][2];
                Plot3DPoint p2p = gridPoints[i][j + 1][2];

                // Bildschirmkoordinaten berechnen
                int[] s1 = transformer.projectToScreen(p1p, displayScale, xOffset, yOffset);
                int[] s2 = transformer.projectToScreen(p2p, displayScale, xOffset, yOffset);

                // Farbe bestimmen
                Color color;
                if (useHeatmap) {
                    // Mittlerer Z-Wert für die Segmentfarbe
                    double avgZ = (p1.getZ() + p2.getZ()) / 2.0;
                    double normalizedZ = (avgZ - model.getZMin()) / (model.getZMax() - model.getZMin());
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    color = colorScheme.getColorForValue(normalizedZ);
                } else {
                    color = functionInfo.getColor();
                }

                // Segmenttiefe berechnen (mittlerer Z-Wert der transformierten Punkte)
                double depth = (p1t.getZ() + p2t.getZ()) / 2.0;

                // Liniensegment hinzufügen
                lineSegments.add(new LineSegment(s1[0], s1[1], s2[0], s2[1], color, depth));
            }
        }

        return lineSegments;
    }

    /**
     * Zeichnet ein Drahtgitteroverlay über undurchsichtige Oberflächen
     */
    private void drawWireframeOverlay(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {

        // Alle Liniensegmente mit Tiefeninformation sammeln
        List<LineSegment> allLineSegments = new ArrayList<>();

        for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
            // Überspringe unsichtbare Funktionen
            if (!functionInfo.isVisible()) {
                continue;
            }

            // Dunklere Version jeder Linie für bessere Sichtbarkeit über undurchsichtigen
            // Oberflächen erstellen
            List<LineSegment> functionSegments = collectLineSegmentsFromFunction(
                    functionInfo, model, displayScale, xOffset, yOffset, useHeatmap);

            // Jede Linienfarbe dunkler machen für die Sichtbarkeit über der
            // undurchsichtigen Oberfläche
            for (LineSegment segment : functionSegments) {
                Color originalColor = segment.color;
                segment.color = new Color(
                        Math.max(0, originalColor.getRed() - 50),
                        Math.max(0, originalColor.getGreen() - 50),
                        Math.max(0, originalColor.getBlue() - 50));
            }

            allLineSegments.addAll(functionSegments);
        }

        // Linien sortieren und zeichnen
        Collections.sort(allLineSegments);

        // Dünneren Strich für das Overlay verwenden
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(0.5f));

        // Alle Liniensegmente zeichnen
        for (LineSegment segment : allLineSegments) {
            g2d.setColor(segment.color);
            g2d.drawLine(segment.x1, segment.y1, segment.x2, segment.y2);
        }

        // Originalstrich wiederherstellen
        g2d.setStroke(originalStroke);
    }

    /**
     * Veraltete Methode, die für die Rückwärtskompatibilität beibehalten wird
     */
    private void drawFunctionGrid(Graphics2D g2d, Plot3DModel.Function3DInfo functionInfo,
            Plot3DModel model, double displayScale, int xOffset, int yOffset,
            boolean useHeatmap, boolean useSolidSurface) {

        // Diese Methode wird nur als Fallback für die Rückwärtskompatibilität verwendet
        // Neuer Code verwendet die collectTriangles und collectLineSegments Methoden

        // Stil für das Gitter
        g2d.setStroke(new BasicStroke(1.0f));

        // Gitterpunkte holen
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        if (gridPoints == null)
            return;

        int resolution = gridPoints.length;

        // Originalstrich speichern
        Stroke originalStroke = g2d.getStroke();

        // Bei undurchsichtiger Darstellung Dreiecke zeichnen
        if (useSolidSurface) {
            // Code für veraltete undurchsichtige Oberflächendarstellung
            // ...
        }

        // Gitterlinien zeichnen
        g2d.setStroke(new BasicStroke(useSolidSurface ? 0.5f : 1.0f));

        // Code für veraltete Drahtgitterdarstellung
        // ...

        // Originalstrich wiederherstellen
        g2d.setStroke(originalStroke);
    }

    /**
     * Erstellt ein Schnappschuss-Bild des aktuellen Plots
     */
    public java.awt.image.BufferedImage createImage(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            Plot3DColorScheme colorScheme, Plot3DGridRenderer gridRenderer,
            int width, int height, boolean useHeatmap) {

        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dImage = image.createGraphics();

        // Hintergrund setzen
        g2dImage.setColor(Color.WHITE);
        g2dImage.fillRect(0, 0, width, height);

        // Anti-Aliasing aktivieren
        g2dImage.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dImage.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Anzeigeeinstellungen berechnen
        double displayScale = Math.min(width, height) * 0.6;
        int xOffset = width / 2;
        int yOffset = height / 2;

        // Koordinatensystem zeichnen
        if (view.isShowCoordinateSystem()) {
            if (view.isShowGrid()) {
                gridRenderer.drawCoordinateGrid(g2dImage, model, view, displayScale, xOffset, yOffset);
            }

            if (view.isShowHelperLines()) {
                gridRenderer.drawHelperLines(g2dImage, model, view, displayScale, xOffset, yOffset);
            }

            gridRenderer.drawAxes(g2dImage, model, view, displayScale, xOffset, yOffset);
            gridRenderer.drawTicksAndLabels(g2dImage, model, view, displayScale, xOffset, yOffset);
        }

        // Funktionen mit der verbesserten tiefensortierten Methode zeichnen
        drawFunctions(g2dImage, model, view, displayScale, xOffset, yOffset,
                useHeatmap, view.isUseSolidSurface());

        // Informationslabels zeichnen
        gridRenderer.drawInfoLabels(g2dImage, model, view, width, height);

        // Farbskala zeichnen
        gridRenderer.drawColorScale(g2dImage, model, colorScheme, width - 30, 50, 20, height - 100);

        g2dImage.dispose();
        return image;
    }
}