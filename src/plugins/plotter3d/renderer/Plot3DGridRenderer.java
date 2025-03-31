package plugins.plotter3d.renderer;

import java.awt.*;
import java.text.DecimalFormat;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Stellt das Koordinatengitter, die Achsen, Teilstriche, Beschriftungen und
 * Hilfslinien
 * für die 3D-Darstellung dar
 */
public class Plot3DGridRenderer {
    // Farben für die Gitterelemente
    private Color gridColor = new Color(200, 200, 200, 100); // Transluzentes Gitter
    private Color axisColor = Color.BLACK; // Achsen
    private Color tickColor = Color.BLACK; // Teilstriche
    private Color labelColor = Color.BLACK; // Beschriftungen
    private Color gridLineColor = new Color(220, 220, 220, 80); // Noch transluzentere Gitterlinien
    private Color helperLineColor = new Color(0, 0, 0, 100); // Transluzente Hilfslinien

    // Konstanten
    private static final int AXIS_EXTENSION = 1; // Wie weit die Achsen über den Datenbereich hinaus verlängert werden
    private static final int NUM_TICKS = 5; // Anzahl der Teilstriche pro Achse
    private static final int NUM_HELPER_LINES = 10; // Anzahl der Hilfslinien pro Richtung
    private static final int TICK_LENGTH = 5; // Länge der Teilstriche in Pixeln

    // Transformer für die Koordinatenumrechnung
    private final Plot3DTransformer transformer;

    public Plot3DGridRenderer(Plot3DTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Zeichnet das Koordinatengitter
     */
    public void drawCoordinateGrid(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        // Grafikkontext einrichten
        g2d.setColor(gridLineColor);
        g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] { 2.0f, 2.0f }, 0.0f)); // Unterbrochene Linie

        // Wertebereiche
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Berechne passende Gitterabstände
        double xStep = calculateGridSpacing(xRange);
        double yStep = calculateGridSpacing(yRange);
        double zStep = calculateGridSpacing(zRange);

        // Hole Transformationsparameter
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne Sinus- und Kosinuswerte vorab
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Normierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Mittelkoordinaten
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Zeichne Gitter auf der XY-Ebene (Z = zMin)
        double yStart = Math.ceil(view.getYMin() / yStep) * yStep; // Beginne bei einem "schönen" Wert
        for (double y = yStart; y <= view.getYMax(); y += yStep) {
            // Zeichne X-Linie an dieser Y-Position
            drawTransformedLine(g2d, view.getXMin(), y, model.getZMin(), view.getXMax(), y, model.getZMin(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        double xStart = Math.ceil(view.getXMin() / xStep) * xStep; // Beginne bei einem "schönen" Wert
        for (double x = xStart; x <= view.getXMax(); x += xStep) {
            // Zeichne Y-Linie an dieser X-Position
            drawTransformedLine(g2d, x, view.getYMin(), model.getZMin(), x, view.getYMax(), model.getZMin(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Zeichne Gitter auf der XZ-Ebene (Y = yMin)
        double zStart = Math.ceil(model.getZMin() / zStep) * zStep; // Beginne bei einem "schönen" Wert
        for (double z = zStart; z <= model.getZMax(); z += zStep) {
            // Zeichne X-Linie an dieser Z-Position
            drawTransformedLine(g2d, view.getXMin(), view.getYMin(), z, view.getXMax(), view.getYMin(), z,
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // X-Gitterlinien bei verschiedenen Z-Positionen
        for (double x = xStart; x <= view.getXMax(); x += xStep) {
            // Zeichne Z-Linie an dieser X-Position
            drawTransformedLine(g2d, x, view.getYMin(), model.getZMin(), x, view.getYMin(), model.getZMax(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Zeichne Gitter auf der YZ-Ebene (X = xMin)
        for (double z = zStart; z <= model.getZMax(); z += zStep) {
            // Zeichne Y-Linie an dieser Z-Position
            drawTransformedLine(g2d, view.getXMin(), view.getYMin(), z, view.getXMin(), view.getYMax(), z,
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Y-Gitterlinien bei verschiedenen Z-Positionen
        for (double y = yStart; y <= view.getYMax(); y += yStep) {
            // Zeichne Z-Linie an dieser Y-Position
            drawTransformedLine(g2d, view.getXMin(), y, model.getZMin(), view.getXMin(), y, model.getZMax(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }
    }

    /**
     * Zeichnet Hilfslinien für eine bessere räumliche Orientierung
     */
    public void drawHelperLines(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        // Spezielle Einstellungen für Hilfslinien
        g2d.setColor(helperLineColor);
        g2d.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] { 1.0f, 2.0f }, 0.0f)); // Gepunktete Linie

        // Wertebereiche
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Berechne passende Schrittweiten
        double xStep = xRange / (NUM_HELPER_LINES - 1);
        double yStep = yRange / (NUM_HELPER_LINES - 1);
        double zStep = zRange / (NUM_HELPER_LINES - 1);

        // Hole Transformationsparameter
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne Sinus- und Kosinuswerte vorab
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Normierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Mittelkoordinaten
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Z-Höhenlinien (X-Y-Ebenen bei unterschiedlichen Z-Werten)
        for (int k = 1; k < NUM_HELPER_LINES - 1; k++) {
            double z = model.getZMin() + k * zStep;

            // Zeichne Gitter an dieser Z-Position
            for (int i = 0; i < NUM_HELPER_LINES; i += 2) {
                // X-Linien
                double y = view.getYMin() + i * yStep;
                drawTransformedLine(g2d, view.getXMin(), y, z, view.getXMax(), y, z,
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }

            for (int j = 0; j < NUM_HELPER_LINES; j += 2) {
                // Y-Linien
                double x = view.getXMin() + j * xStep;
                drawTransformedLine(g2d, x, view.getYMin(), z, x, view.getYMax(), z,
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }
        }

        // X-Y-Linien durch den 3D-Raum (bei unterschiedlichen Y-Werten)
        for (int j = 2; j < NUM_HELPER_LINES - 2; j += 2) {
            double y = view.getYMin() + j * yStep;

            // Vertikale Linien an dieser Y-Position
            for (int i = 2; i < NUM_HELPER_LINES - 2; i += 2) {
                double x = view.getXMin() + i * xStep;
                drawTransformedLine(g2d, x, y, model.getZMin(), x, y, model.getZMax(),
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }
        }

        // X-Z-Linien (vertikale Linien an bestimmten X-Positionen)
        for (int i = 2; i < NUM_HELPER_LINES - 2; i += 2) {
            double x = view.getXMin() + i * xStep;

            // Linien an unterschiedlichen Z-Positionen
            for (int k = 1; k < NUM_HELPER_LINES - 1; k += 2) {
                double z = model.getZMin() + k * zStep;
                drawTransformedLine(g2d, x, view.getYMin(), z, x, view.getYMax(), z,
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }
        }
    }

    /**
     * Zeichnet die Koordinatenachsen
     */
    public void drawAxes(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(axisColor);

        // Wertebereiche und Mittelwerte
        double xMin = view.getXMin();
        double xMax = view.getXMax();
        double yMin = view.getYMin();
        double yMax = view.getYMax();
        double zMin = model.getZMin();
        double zMax = model.getZMax();

        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;
        double zCenter = (zMax + zMin) / 2;

        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        double zRange = zMax - zMin;

        // Normierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Transformationsparameter
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne Sinus- und Kosinuswerte vorab
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Bestimme die tatsächlichen Achsenpositionen
        // X-Achsen-Position
        double xPosX = 0, yPosX = 0, zPosX = 0;
        if (yMin > 0)
            yPosX = yMin; // Unten halten
        else if (yMax < 0)
            yPosX = yMax; // Oben halten
        if (zMin > 0)
            zPosX = zMin; // Vorne halten
        else if (zMax < 0)
            zPosX = zMax; // Hinten halten

        // Y-Achsen-Position
        double xPosY = 0, yPosY = 0, zPosY = 0;
        if (xMin > 0)
            xPosY = xMin; // Links halten
        else if (xMax < 0)
            xPosY = xMax; // Rechts halten
        if (zMin > 0)
            zPosY = zMin; // Vorne halten
        else if (zMax < 0)
            zPosY = zMax; // Hinten halten

        // Z-Achsen-Position
        double xPosZ = 0, yPosZ = 0;
        if (xMin > 0)
            xPosZ = xMin; // Links halten
        else if (xMax < 0)
            xPosZ = xMax; // Rechts halten
        if (yMin > 0)
            yPosZ = yMin; // Unten halten
        else if (yMax < 0)
            yPosZ = yMax; // Oben halten

        // Zeichne X-Achse
        drawTransformedLine(g2d, xMin, yPosX, zPosX, xMax, yPosX, zPosX,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Zeichne Y-Achse
        drawTransformedLine(g2d, xPosY, yMin, zPosY, xPosY, yMax, zPosY,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Zeichne Z-Achse
        drawTransformedLine(g2d, xPosZ, yPosZ, zMin, xPosZ, yPosZ, zMax,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Zeichne Achsenbeschriftungen
        drawAxisLabels(g2d, model, view, xPosX, yPosX, zPosX, xPosY, yPosY, zPosY, xPosZ, yPosZ,
                xCenter, yCenter, zCenter, factor,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                displayScale, xOffset, yOffset);
    }

    /**
     * Zeichnet Achsenbeschriftungen
     */
    private void drawAxisLabels(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double xPosX, double yPosX, double zPosX,
            double xPosY, double yPosY, double zPosY,
            double xPosZ, double yPosZ,
            double xCenter, double yCenter, double zCenter,
            double factor,
            double sinX, double cosX, double sinY, double cosY, double sinZ, double cosZ,
            double displayScale, int xOffset, int yOffset) {
        Font originalFont = g2d.getFont();
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));

        // X-Achsen-Beschriftung
        Plot3DPoint labelPoint = transformer.transformPoint(
                view.getXMax(), yPosX, zPosX,
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        int[] screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("X", screenPos[0] + 10, screenPos[1]);

        // Y-Achsen-Beschriftung
        labelPoint = transformer.transformPoint(
                xPosY, view.getYMax(), zPosY,
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("Y", screenPos[0], screenPos[1] - 10);

        // Z-Achsen-Beschriftung
        labelPoint = transformer.transformPoint(
                xPosZ, yPosZ, model.getZMax(),
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("Z", screenPos[0], screenPos[1]);

        // Ursprüngliche Schriftart wiederherstellen
        g2d.setFont(originalFont);
    }

    /**
     * Zeichnet Teilstriche und Beschriftungen an den Achsen
     */
    public void drawTicksAndLabels(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.setStroke(new BasicStroke(1.0f));

        // Format für Teilstrich-Beschriftungen
        DecimalFormat df = new DecimalFormat("0.##");

        // Mittelkoordinaten
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Wertebereiche
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Berechne passende Gitterabstände
        double xSpacing = calculateGridSpacing(xRange);
        double ySpacing = calculateGridSpacing(yRange);
        double zSpacing = calculateGridSpacing(zRange);

        // Normierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Transformationsparameter
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne Sinus- und Kosinuswerte vorab
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Bestimme die tatsächlichen Achsenpositionen (wie in drawAxes)
        double yPosX = 0, zPosX = 0; // X-Achse
        if (view.getYMin() > 0)
            yPosX = view.getYMin();
        else if (view.getYMax() < 0)
            yPosX = view.getYMax();
        if (model.getZMin() > 0)
            zPosX = model.getZMin();
        else if (model.getZMax() < 0)
            zPosX = model.getZMax();

        double xPosY = 0, zPosY = 0; // Y-Achse
        if (view.getXMin() > 0)
            xPosY = view.getXMin();
        else if (view.getXMax() < 0)
            xPosY = view.getXMax();
        if (model.getZMin() > 0)
            zPosY = model.getZMin();
        else if (model.getZMax() < 0)
            zPosY = model.getZMax();

        double xPosZ = 0, yPosZ = 0; // Z-Achse
        if (view.getXMin() > 0)
            xPosZ = view.getXMin();
        else if (view.getXMax() < 0)
            xPosZ = view.getXMax();
        if (view.getYMin() > 0)
            yPosZ = view.getYMin();
        else if (view.getYMax() < 0)
            yPosZ = view.getYMax();

        // X-Achsen-Teilstriche und Beschriftungen
        double xStart = Math.ceil(view.getXMin() / xSpacing) * xSpacing;
        for (double tickValue = xStart; tickValue <= view.getXMax(); tickValue += xSpacing) {
            // Überspringe den Ursprung (er erhält die Beschriftung 0)
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position auf der X-Achse
            Plot3DPoint tickPoint = transformer.transformPoint(
                    tickValue, yPosX, zPosX,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Zeichne Teilstrich
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Zeichne Beschriftung
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] - 10, screenPos[1] + 15);
        }

        // Y-Achsen-Teilstriche und Beschriftungen
        double yStart = Math.ceil(view.getYMin() / ySpacing) * ySpacing;
        for (double tickValue = yStart; tickValue <= view.getYMax(); tickValue += ySpacing) {
            // Überspringe den Ursprung
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position auf der Y-Achse
            Plot3DPoint tickPoint = transformer.transformPoint(
                    xPosY, tickValue, zPosY,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Zeichne Teilstrich
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Zeichne Beschriftung
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] + 5, screenPos[1] + 4);
        }

        // Z-Achsen-Teilstriche und Beschriftungen
        double zStart = Math.ceil(model.getZMin() / zSpacing) * zSpacing;
        for (double tickValue = zStart; tickValue <= model.getZMax(); tickValue += zSpacing) {
            // Überspringe den Ursprung
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position auf der Z-Achse
            Plot3DPoint tickPoint = transformer.transformPoint(
                    xPosZ, yPosZ, tickValue,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Zeichne Teilstrich
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Zeichne Beschriftung
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] + 5, screenPos[1] - 5);
        }

        // Ursprungsbeschriftung (0)
        Plot3DPoint originPoint = transformer.transformPoint(
                0, 0, 0,
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        int[] originPos = transformer.projectToScreen(originPoint, displayScale, xOffset, yOffset);
        g2d.drawString("0", originPos[0] + 4, originPos[1] + 12);
    }

    /**
     * Zeichnet die Farbskalenlegende
     */
    public void drawColorScale(Graphics2D g2d, Plot3DModel model, Plot3DColorScheme colorScheme,
            int x, int y, int width, int height) {
        // Zeichne den Farbverlauf
        for (int i = 0; i < height; i++) {
            double normalizedValue = 1.0 - (double) i / height;
            g2d.setColor(colorScheme.getColorForValue(normalizedValue));
            g2d.fillRect(x, y + i, width, 1);
        }

        // Zeichne einen Rahmen
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);

        // Füge Min-/Max-Beschriftungen hinzu
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        DecimalFormat df = new DecimalFormat("0.##");
        g2d.drawString(df.format(model.getZMax()), x + width + 2, y + 10);
        g2d.drawString(df.format(model.getZMin()), x + width + 2, y + height);
    }

    /**
     * Zeichnet Informationsbeschriftungen
     */
    public void drawInfoLabels(Graphics2D g2d, Plot3DModel model, Plot3DView view, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Zeige Achseninformationen
        g2d.drawString("Achsen:", width - 80, height - 30);
        g2d.drawString("X - horizontal", width - 80, height - 20);
        g2d.drawString("Y - vertikal", width - 80, height - 10);
        g2d.drawString("Z - tiefe", width - 80, height);

        // Zeige Wertebereiche
        g2d.drawString(String.format("X: [%.2f, %.2f]", view.getXMin(), view.getXMax()), 10, height - 40);
        g2d.drawString(String.format("Y: [%.2f, %.2f]", view.getYMin(), view.getYMax()), 10, height - 25);
        g2d.drawString(String.format("Z: [%.2f, %.2f]", model.getZMin(), model.getZMax()), 10, height - 10);
    }

    /**
     * Hilfsmethode, um eine 3D-Linie mit Transformationen zu zeichnen
     */
    private void drawTransformedLine(Graphics2D g2d,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double xCenter, double yCenter, double zCenter,
            double factor, double scale,
            double sinX, double cosX,
            double sinY, double cosY,
            double sinZ, double cosZ,
            double panX, double panY,
            double displayScale, int xOffset, int yOffset) {
        // Transformiere den ersten Punkt
        Plot3DPoint p1 = transformer.transformPoint(
                x1, y1, z1,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                panX, panY);

        // Transformiere den zweiten Punkt
        Plot3DPoint p2 = transformer.transformPoint(
                x2, y2, z2,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                panX, panY);

        // Projiziere in Bildschirmkoordinaten
        int[] screenPos1 = transformer.projectToScreen(p1, displayScale, xOffset, yOffset);
        int[] screenPos2 = transformer.projectToScreen(p2, displayScale, xOffset, yOffset);

        // Zeichne die Linie
        g2d.drawLine(screenPos1[0], screenPos1[1], screenPos2[0], screenPos2[1]);
    }

    /**
     * Berechnet einen geeigneten Gitterabstand basierend auf dem Wertebereich
     */
    private double calculateGridSpacing(double range) {
        // Ziel: Etwa 6 Gitterlinien im sichtbaren Bereich
        double targetSteps = 6;
        double rawSpacing = range / targetSteps;

        // Normiere auf Zehnerpotenzen
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Runde auf "schöne" Werte: 1, 2, 5, 10
        if (mantissa < 1.5) {
            return Math.pow(10, exponent);
        } else if (mantissa < 3.5) {
            return 2 * Math.pow(10, exponent);
        } else if (mantissa < 7.5) {
            return 5 * Math.pow(10, exponent);
        } else {
            return 10 * Math.pow(10, exponent);
        }
    }
}
