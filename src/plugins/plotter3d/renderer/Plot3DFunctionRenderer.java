package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.geom.Path2D;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Renders 3D function plots as wireframe grids
 */
public class Plot3DFunctionRenderer {
    // Transformer for coordinate conversion
    private final Plot3DTransformer transformer;
    // Color scheme for heatmap mode
    private final Plot3DColorScheme colorScheme;

    public Plot3DFunctionRenderer(Plot3DTransformer transformer) {
        this.transformer = transformer;
        this.colorScheme = Plot3DColorScheme.createDefault();
    }

    /**
     * Zeichnet alle Funktionen im Modell
     * 
     * @param g2d          Grafikkontext
     * @param model        Datenmodell mit Funktionen
     * @param view         Ansichtsparameter
     * @param displayScale Skalierungsfaktor für die Anzeige
     * @param xOffset      X-Versatz für die Anzeige
     * @param yOffset      Y-Versatz für die Anzeige
     * @param useHeatmap   Ob Heatmap-Farben verwendet werden sollen
     */
    public void drawFunctions(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        // Jede Funktion im Modell zeichnen
        for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
            drawFunctionGrid(g2d, functionInfo, model, displayScale, xOffset, yOffset,
                    useHeatmap, view.isUseSolidSurface());
        }
    }

    /**
     * Zeichnet eine einzelne Funktion als Gitter oder als undurchsichtige
     * Oberfläche mit Schattierung
     */
    private void drawFunctionGrid(Graphics2D g2d, Plot3DModel.Function3DInfo functionInfo,
            Plot3DModel model, double displayScale, int xOffset, int yOffset,
            boolean useHeatmap, boolean useSolidSurface) {

        // Stil für das Gitter setzen
        g2d.setStroke(new BasicStroke(1.0f));

        // Gitterpunkte holen
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        if (gridPoints == null)
            return;

        int resolution = gridPoints.length;

        // Originalen Zeichenstil speichern
        Stroke originalStroke = g2d.getStroke();

        // Bei undurchsichtiger Darstellung zunächst die Dreiecke/Polygone zeichnen
        if (useSolidSurface) {
            // Der Lichtquelle für die Schattierung (normalisierter Vektor)
            double[] lightSource = { 0.5, 0.5, 1.0 };
            double lightMagnitude = Math.sqrt(lightSource[0] * lightSource[0] +
                    lightSource[1] * lightSource[1] +
                    lightSource[2] * lightSource[2]);
            lightSource[0] /= lightMagnitude;
            lightSource[1] /= lightMagnitude;
            lightSource[2] /= lightMagnitude;

            // Undurchsichtige Oberfläche als Dreiecke zeichnen
            for (int i = 0; i < resolution - 1; i++) {
                for (int j = 0; j < resolution - 1; j++) {
                    // Vier Punkte, die ein Quadrat bilden
                    Plot3DPoint p1 = gridPoints[i][j][0]; // Original-Punkt für Z-Wert
                    Plot3DPoint p2 = gridPoints[i + 1][j][0];
                    Plot3DPoint p3 = gridPoints[i + 1][j + 1][0];
                    Plot3DPoint p4 = gridPoints[i][j + 1][0];

                    // Transformierte/projizierte Punkte für die Bildschirmposition
                    Plot3DPoint p1t = gridPoints[i][j][2];
                    Plot3DPoint p2t = gridPoints[i + 1][j][2];
                    Plot3DPoint p3t = gridPoints[i + 1][j + 1][2];
                    Plot3DPoint p4t = gridPoints[i][j + 1][2];

                    // Bildschirmkoordinaten berechnen
                    int[] s1 = transformer.projectToScreen(p1t, displayScale, xOffset, yOffset);
                    int[] s2 = transformer.projectToScreen(p2t, displayScale, xOffset, yOffset);
                    int[] s3 = transformer.projectToScreen(p3t, displayScale, xOffset, yOffset);
                    int[] s4 = transformer.projectToScreen(p4t, displayScale, xOffset, yOffset);

                    // Normale für das erste Dreieck (p1, p2, p3) berechnen
                    double[] v1 = { p2.getX() - p1.getX(), p2.getY() - p1.getY(), p2.getZ() - p1.getZ() };
                    double[] v2 = { p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ() };

                    double[] normal = {
                            v1[1] * v2[2] - v1[2] * v2[1], // Nx = Uy*Vz - Uz*Vy
                            v1[2] * v2[0] - v1[0] * v2[2], // Ny = Uz*Vx - Ux*Vz
                            v1[0] * v2[1] - v1[1] * v2[0] // Nz = Ux*Vy - Uy*Vx
                    };

                    // Normale normalisieren
                    double normalMagnitude = Math.sqrt(normal[0] * normal[0] +
                            normal[1] * normal[1] +
                            normal[2] * normal[2]);
                    if (normalMagnitude > 0) {
                        normal[0] /= normalMagnitude;
                        normal[1] /= normalMagnitude;
                        normal[2] /= normalMagnitude;
                    }

                    // Farbe basierend auf Z-Wert und Beleuchtung berechnen
                    Color baseColor;
                    if (useHeatmap) {
                        // Normalisierter Z-Wert für die Farbskala
                        double normalizedZ = (p1.getZ() - model.getZMin()) /
                                (model.getZMax() - model.getZMin());
                        normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                        baseColor = colorScheme.getColorForValue(normalizedZ);
                    } else {
                        baseColor = functionInfo.getColor();
                    }

                    // Skalarprodukt zwischen Normale und Lichtquelle berechnen (Lambertsche
                    // Beleuchtung)
                    double dotProduct = normal[0] * lightSource[0] +
                            normal[1] * lightSource[1] +
                            normal[2] * lightSource[2];

                    // Sicherstellen, dass das Skalarprodukt positiv ist (sonst andere Seite der
                    // Fläche)
                    dotProduct = Math.abs(dotProduct);

                    // Farbe basierend auf der Beleuchtung anpassen
                    int red = (int) (baseColor.getRed() * (0.4 + 0.6 * dotProduct));
                    int green = (int) (baseColor.getGreen() * (0.4 + 0.6 * dotProduct));
                    int blue = (int) (baseColor.getBlue() * (0.4 + 0.6 * dotProduct));

                    // Farbwerte begrenzen
                    red = Math.max(0, Math.min(255, red));
                    green = Math.max(0, Math.min(255, green));
                    blue = Math.max(0, Math.min(255, blue));

                    Color shadedColor = new Color(red, green, blue);
                    g2d.setColor(shadedColor);

                    // Erstes Dreieck (p1, p2, p3) zeichnen
                    int[] xPoints1 = { s1[0], s2[0], s3[0] };
                    int[] yPoints1 = { s1[1], s2[1], s3[1] };
                    g2d.fillPolygon(xPoints1, yPoints1, 3);

                    // Normale für das zweite Dreieck (p1, p3, p4) berechnen
                    double[] v3 = { p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ() };
                    double[] v4 = { p4.getX() - p1.getX(), p4.getY() - p1.getY(), p4.getZ() - p1.getZ() };

                    double[] normal2 = {
                            v3[1] * v4[2] - v3[2] * v4[1],
                            v3[2] * v4[0] - v3[0] * v4[2],
                            v3[0] * v4[1] - v3[1] * v4[0]
                    };

                    // Normale normalisieren
                    double normal2Magnitude = Math.sqrt(normal2[0] * normal2[0] +
                            normal2[1] * normal2[1] +
                            normal2[2] * normal2[2]);
                    if (normal2Magnitude > 0) {
                        normal2[0] /= normal2Magnitude;
                        normal2[1] /= normal2Magnitude;
                        normal2[2] /= normal2Magnitude;
                    }

                    // Zweites Dreieck (p1, p3, p4) zeichnen
                    int[] xPoints2 = { s1[0], s3[0], s4[0] };
                    int[] yPoints2 = { s1[1], s3[1], s4[1] };
                    g2d.fillPolygon(xPoints2, yPoints2, 3);
                }
            }
        }

        // Gitterlinien zeichnen (bei undurchsichtiger Darstellung optional als Overlay)
        g2d.setStroke(new BasicStroke(useSolidSurface ? 0.5f : 1.0f));

        // Horizontale Linien (entlang der X-Achse)
        for (int j = 0; j < resolution; j++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int i = 0; i < resolution; i++) {
                // Original und projizierter Punkt
                Plot3DPoint original = gridPoints[i][j][0];
                Plot3DPoint projected = gridPoints[i][j][2];

                // Bildschirmkoordinaten berechnen
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                // Farbe basierend auf Z-Wert setzen, falls Heatmap verwendet wird
                if (useHeatmap) {
                    // Z-Wert auf 0-1 Bereich normalisieren
                    double normalizedZ = (original.getZ() - model.getZMin()) /
                            (model.getZMax() - model.getZMin());
                    // Auf 0-1 begrenzen
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    // Farbe aus der Farbskala holen
                    Color color = colorScheme.getColorForValue(normalizedZ);

                    // Bei undurchsichtiger Darstellung eine dunklere Farbe für die Gitterlinien
                    // verwenden
                    if (useSolidSurface) {
                        color = new Color(
                                Math.max(0, color.getRed() - 50),
                                Math.max(0, color.getGreen() - 50),
                                Math.max(0, color.getBlue() - 50));
                    }

                    g2d.setColor(color);
                } else {
                    // Funktionsfarbe verwenden
                    Color color = functionInfo.getColor();

                    // Bei undurchsichtiger Darstellung eine dunklere Farbe für die Gitterlinien
                    // verwenden
                    if (useSolidSurface) {
                        color = new Color(
                                Math.max(0, color.getRed() - 50),
                                Math.max(0, color.getGreen() - 50),
                                Math.max(0, color.getBlue() - 50));
                    }

                    g2d.setColor(color);
                }

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                    // Segment mit aktueller Farbe zeichnen
                    g2d.draw(path);
                    // Neuen Pfad bei diesem Punkt beginnen
                    path.reset();
                    path.moveTo(screenPos[0], screenPos[1]);
                }
            }
        }

        // Vertikale Linien (entlang der Y-Achse)
        for (int i = 0; i < resolution; i++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int j = 0; j < resolution; j++) {
                // Original und projizierter Punkt
                Plot3DPoint original = gridPoints[i][j][0];
                Plot3DPoint projected = gridPoints[i][j][2];

                // Bildschirmkoordinaten berechnen
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                // Farbe basierend auf Z-Wert setzen, falls Heatmap verwendet wird
                if (useHeatmap) {
                    // Z-Wert auf 0-1 Bereich normalisieren
                    double normalizedZ = (original.getZ() - model.getZMin()) /
                            (model.getZMax() - model.getZMin());
                    // Auf 0-1 begrenzen
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    // Farbe aus der Farbskala holen
                    Color color = colorScheme.getColorForValue(normalizedZ);

                    // Bei undurchsichtiger Darstellung eine dunklere Farbe für die Gitterlinien
                    // verwenden
                    if (useSolidSurface) {
                        color = new Color(
                                Math.max(0, color.getRed() - 50),
                                Math.max(0, color.getGreen() - 50),
                                Math.max(0, color.getBlue() - 50));
                    }

                    g2d.setColor(color);
                } else {
                    // Funktionsfarbe verwenden
                    Color color = functionInfo.getColor();

                    // Bei undurchsichtiger Darstellung eine dunklere Farbe für die Gitterlinien
                    // verwenden
                    if (useSolidSurface) {
                        color = new Color(
                                Math.max(0, color.getRed() - 50),
                                Math.max(0, color.getGreen() - 50),
                                Math.max(0, color.getBlue() - 50));
                    }

                    g2d.setColor(color);
                }

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                    // Segment mit aktueller Farbe zeichnen
                    g2d.draw(path);
                    // Neuen Pfad bei diesem Punkt beginnen
                    path.reset();
                    path.moveTo(screenPos[0], screenPos[1]);
                }
            }
        }

        // Originalen Zeichenstil wiederherstellen
        g2d.setStroke(originalStroke);
    }

    /**
     * Creates a snapshot image of the current plot
     */
    public java.awt.image.BufferedImage createImage(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            Plot3DColorScheme colorScheme, Plot3DGridRenderer gridRenderer,
            int width, int height, boolean useHeatmap) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dImage = image.createGraphics();

        // Set background
        g2dImage.setColor(Color.WHITE);
        g2dImage.fillRect(0, 0, width, height);

        // Enable anti-aliasing
        g2dImage.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dImage.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate display parameters
        double displayScale = Math.min(width, height) * 0.6;
        int xOffset = width / 2;
        int yOffset = height / 2;

        // Draw coordinate system
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

        // Draw functions
        drawFunctions(g2dImage, model, view, displayScale, xOffset, yOffset, useHeatmap);

        // Draw informational labels
        gridRenderer.drawInfoLabels(g2dImage, model, view, width, height);

        // Draw color scale
        gridRenderer.drawColorScale(g2dImage, model, colorScheme, width - 30, 50, 20, height - 100);

        g2dImage.dispose();
        return image;
    }
}