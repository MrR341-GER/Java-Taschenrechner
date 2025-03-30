
package plugins.plotter3d.renderer;

import java.awt.*;
import java.text.DecimalFormat;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Renders coordinate grid, axes, ticks, labels, and helper lines for 3D
 * plotting
 */
public class Plot3DGridRenderer {
    // Colors for grid elements
    private Color gridColor = new Color(200, 200, 200, 100); // Translucent grid
    private Color axisColor = Color.BLACK; // Axes
    private Color tickColor = Color.BLACK; // Tick marks
    private Color labelColor = Color.BLACK; // Labels
    private Color gridLineColor = new Color(220, 220, 220, 80); // More translucent grid lines
    private Color helperLineColor = new Color(0, 0, 0, 100); // Translucent helper lines

    // Constants
    private static final int AXIS_EXTENSION = 1; // How far to extend axes beyond data range
    private static final int NUM_TICKS = 5; // Number of ticks per axis
    private static final int NUM_HELPER_LINES = 10; // Number of helper lines per direction
    private static final int TICK_LENGTH = 5; // Length of tick marks in pixels

    // Transformer for coordinate conversion
    private final Plot3DTransformer transformer;

    public Plot3DGridRenderer(Plot3DTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Draws the coordinate grid
     */
    public void drawCoordinateGrid(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        // Set up the graphics context
        g2d.setColor(gridLineColor);
        g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] { 2.0f, 2.0f }, 0.0f)); // Dashed line

        // Value ranges
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Calculate appropriate grid spacings
        double xStep = calculateGridSpacing(xRange);
        double yStep = calculateGridSpacing(yRange);
        double zStep = calculateGridSpacing(zRange);

        // Get transformation parameters
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Precalculate sine and cosine values
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Normalization factor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Center coordinates
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Draw grid on the XY-plane (Z = zMin)
        double yStart = Math.ceil(view.getYMin() / yStep) * yStep; // Start at a "nice" value
        for (double y = yStart; y <= view.getYMax(); y += yStep) {
            // Draw X-line at this Y position
            drawTransformedLine(g2d, view.getXMin(), y, model.getZMin(), view.getXMax(), y, model.getZMin(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        double xStart = Math.ceil(view.getXMin() / xStep) * xStep; // Start at a "nice" value
        for (double x = xStart; x <= view.getXMax(); x += xStep) {
            // Draw Y-line at this X position
            drawTransformedLine(g2d, x, view.getYMin(), model.getZMin(), x, view.getYMax(), model.getZMin(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Draw grid on the XZ-plane (Y = yMin)
        double zStart = Math.ceil(model.getZMin() / zStep) * zStep; // Start at a "nice" value
        for (double z = zStart; z <= model.getZMax(); z += zStep) {
            // Draw X-line at this Z position
            drawTransformedLine(g2d, view.getXMin(), view.getYMin(), z, view.getXMax(), view.getYMin(), z,
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // X grid lines at different Z positions
        for (double x = xStart; x <= view.getXMax(); x += xStep) {
            // Draw Z-line at this X position
            drawTransformedLine(g2d, x, view.getYMin(), model.getZMin(), x, view.getYMin(), model.getZMax(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Draw grid on the YZ-plane (X = xMin)
        for (double z = zStart; z <= model.getZMax(); z += zStep) {
            // Draw Y-line at this Z position
            drawTransformedLine(g2d, view.getXMin(), view.getYMin(), z, view.getXMin(), view.getYMax(), z,
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }

        // Y grid lines at different Z positions
        for (double y = yStart; y <= view.getYMax(); y += yStep) {
            // Draw Z-line at this Y position
            drawTransformedLine(g2d, view.getXMin(), y, model.getZMin(), view.getXMin(), y, model.getZMax(),
                    xCenter, yCenter, zCenter, factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY(),
                    displayScale, xOffset, yOffset);
        }
    }

    /**
     * Draws helper lines for better spatial orientation
     */
    public void drawHelperLines(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        // Special settings for helper lines
        g2d.setColor(helperLineColor);
        g2d.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] { 1.0f, 2.0f }, 0.0f)); // Dotted line

        // Value ranges
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Calculate appropriate step sizes
        double xStep = xRange / (NUM_HELPER_LINES - 1);
        double yStep = yRange / (NUM_HELPER_LINES - 1);
        double zStep = zRange / (NUM_HELPER_LINES - 1);

        // Get transformation parameters
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Precalculate sine and cosine values
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Normalization factor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Center coordinates
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Z height lines (X-Y planes at different Z values)
        for (int k = 1; k < NUM_HELPER_LINES - 1; k++) {
            double z = model.getZMin() + k * zStep;

            // Draw grid at this Z position
            for (int i = 0; i < NUM_HELPER_LINES; i += 2) {
                // X lines
                double y = view.getYMin() + i * yStep;
                drawTransformedLine(g2d, view.getXMin(), y, z, view.getXMax(), y, z,
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }

            for (int j = 0; j < NUM_HELPER_LINES; j += 2) {
                // Y lines
                double x = view.getXMin() + j * xStep;
                drawTransformedLine(g2d, x, view.getYMin(), z, x, view.getYMax(), z,
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }
        }

        // X-Y lines through 3D space (at different Y values)
        for (int j = 2; j < NUM_HELPER_LINES - 2; j += 2) {
            double y = view.getYMin() + j * yStep;

            // Vertical lines at this Y position
            for (int i = 2; i < NUM_HELPER_LINES - 2; i += 2) {
                double x = view.getXMin() + i * xStep;
                drawTransformedLine(g2d, x, y, model.getZMin(), x, y, model.getZMax(),
                        xCenter, yCenter, zCenter, factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY(),
                        displayScale, xOffset, yOffset);
            }
        }

        // X-Z lines (vertical lines at specific X positions)
        for (int i = 2; i < NUM_HELPER_LINES - 2; i += 2) {
            double x = view.getXMin() + i * xStep;

            // Lines at different Z positions
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
     * Draws the coordinate axes
     */
    public void drawAxes(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(axisColor);

        // Value ranges and centers
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

        // Normalization factor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Transformation parameters
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Precalculate sine and cosine values
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Determine actual axis positions
        // X axis position
        double xPosX = 0, yPosX = 0, zPosX = 0;
        if (yMin > 0)
            yPosX = yMin; // Keep at bottom
        else if (yMax < 0)
            yPosX = yMax; // Keep at top
        if (zMin > 0)
            zPosX = zMin; // Keep at front
        else if (zMax < 0)
            zPosX = zMax; // Keep at back

        // Y axis position
        double xPosY = 0, yPosY = 0, zPosY = 0;
        if (xMin > 0)
            xPosY = xMin; // Keep at left
        else if (xMax < 0)
            xPosY = xMax; // Keep at right
        if (zMin > 0)
            zPosY = zMin; // Keep at front
        else if (zMax < 0)
            zPosY = zMax; // Keep at back

        // Z axis position
        double xPosZ = 0, yPosZ = 0;
        if (xMin > 0)
            xPosZ = xMin; // Keep at left
        else if (xMax < 0)
            xPosZ = xMax; // Keep at right
        if (yMin > 0)
            yPosZ = yMin; // Keep at bottom
        else if (yMax < 0)
            yPosZ = yMax; // Keep at top

        // Draw X axis
        drawTransformedLine(g2d, xMin, yPosX, zPosX, xMax, yPosX, zPosX,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Draw Y axis
        drawTransformedLine(g2d, xPosY, yMin, zPosY, xPosY, yMax, zPosY,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Draw Z axis
        drawTransformedLine(g2d, xPosZ, yPosZ, zMin, xPosZ, yPosZ, zMax,
                xCenter, yCenter, zCenter, factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY(),
                displayScale, xOffset, yOffset);

        // Draw axis labels
        drawAxisLabels(g2d, model, view, xPosX, yPosX, zPosX, xPosY, yPosY, zPosY, xPosZ, yPosZ,
                xCenter, yCenter, zCenter, factor,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                displayScale, xOffset, yOffset);
    }

    /**
     * Draws axis labels
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

        // X axis label
        Plot3DPoint labelPoint = transformer.transformPoint(
                view.getXMax(), yPosX, zPosX,
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        int[] screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("X", screenPos[0] + 10, screenPos[1]);

        // Y axis label
        labelPoint = transformer.transformPoint(
                xPosY, view.getYMax(), zPosY,
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("Y", screenPos[0], screenPos[1] - 10);

        // Z axis label
        labelPoint = transformer.transformPoint(
                xPosZ, yPosZ, model.getZMax(),
                xCenter, yCenter, zCenter,
                factor, view.getScale(),
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                view.getPanX(), view.getPanY());

        screenPos = transformer.projectToScreen(labelPoint, displayScale, xOffset, yOffset);
        g2d.drawString("Z", screenPos[0], screenPos[1]);

        // Restore original font
        g2d.setFont(originalFont);
    }

    /**
     * Draws ticks and labels on the axes
     */
    public void drawTicksAndLabels(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.setStroke(new BasicStroke(1.0f));

        // Format for tick labels
        DecimalFormat df = new DecimalFormat("0.##");

        // Center coordinates
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Value ranges
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();

        // Calculate appropriate grid spacings
        double xSpacing = calculateGridSpacing(xRange);
        double ySpacing = calculateGridSpacing(yRange);
        double zSpacing = calculateGridSpacing(zRange);

        // Normalization factor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Transformation parameters
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Precalculate sine and cosine values
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Determine actual axis positions (as in drawAxes)
        double yPosX = 0, zPosX = 0; // X-axis position
        if (view.getYMin() > 0)
            yPosX = view.getYMin();
        else if (view.getYMax() < 0)
            yPosX = view.getYMax();
        if (model.getZMin() > 0)
            zPosX = model.getZMin();
        else if (model.getZMax() < 0)
            zPosX = model.getZMax();

        double xPosY = 0, zPosY = 0; // Y-axis position
        if (view.getXMin() > 0)
            xPosY = view.getXMin();
        else if (view.getXMax() < 0)
            xPosY = view.getXMax();
        if (model.getZMin() > 0)
            zPosY = model.getZMin();
        else if (model.getZMax() < 0)
            zPosY = model.getZMax();

        double xPosZ = 0, yPosZ = 0; // Z-axis position
        if (view.getXMin() > 0)
            xPosZ = view.getXMin();
        else if (view.getXMax() < 0)
            xPosZ = view.getXMax();
        if (view.getYMin() > 0)
            yPosZ = view.getYMin();
        else if (view.getYMax() < 0)
            yPosZ = view.getYMax();

        // X-axis ticks and labels
        double xStart = Math.ceil(view.getXMin() / xSpacing) * xSpacing;
        for (double tickValue = xStart; tickValue <= view.getXMax(); tickValue += xSpacing) {
            // Skip the origin (it will have 0 label)
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position on X axis
            Plot3DPoint tickPoint = transformer.transformPoint(
                    tickValue, yPosX, zPosX,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Draw tick mark
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Draw label
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] - 10, screenPos[1] + 15);
        }

        // Y-axis ticks and labels
        double yStart = Math.ceil(view.getYMin() / ySpacing) * ySpacing;
        for (double tickValue = yStart; tickValue <= view.getYMax(); tickValue += ySpacing) {
            // Skip the origin
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position on Y axis
            Plot3DPoint tickPoint = transformer.transformPoint(
                    xPosY, tickValue, zPosY,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Draw tick mark
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Draw label
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] + 5, screenPos[1] + 4);
        }

        // Z-axis ticks and labels
        double zStart = Math.ceil(model.getZMin() / zSpacing) * zSpacing;
        for (double tickValue = zStart; tickValue <= model.getZMax(); tickValue += zSpacing) {
            // Skip the origin
            if (Math.abs(tickValue) < 1e-10)
                continue;

            // Position on Z axis
            Plot3DPoint tickPoint = transformer.transformPoint(
                    xPosZ, yPosZ, tickValue,
                    xCenter, yCenter, zCenter,
                    factor, view.getScale(),
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    view.getPanX(), view.getPanY());

            int[] screenPos = transformer.projectToScreen(tickPoint, displayScale, xOffset, yOffset);

            // Draw tick mark
            g2d.setColor(tickColor);
            g2d.drawLine(screenPos[0] - 2, screenPos[1] - 2, screenPos[0] + 2, screenPos[1] + 2);

            // Draw label
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            g2d.drawString(label, screenPos[0] + 5, screenPos[1] - 5);
        }

        // Origin label (0)
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
     * Draws the color scale legend
     */
    public void drawColorScale(Graphics2D g2d, Plot3DModel model, Plot3DColorScheme colorScheme,
            int x, int y, int width, int height) {
        // Draw the color gradient
        for (int i = 0; i < height; i++) {
            double normalizedValue = 1.0 - (double) i / height;
            g2d.setColor(colorScheme.getColorForValue(normalizedValue));
            g2d.fillRect(x, y + i, width, 1);
        }

        // Draw a border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);

        // Add min/max labels
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        DecimalFormat df = new DecimalFormat("0.##");
        g2d.drawString(df.format(model.getZMax()), x + width + 2, y + 10);
        g2d.drawString(df.format(model.getZMin()), x + width + 2, y + height);
    }

    /**
     * Draws info labels
     */
    public void drawInfoLabels(Graphics2D g2d, Plot3DModel model, Plot3DView view, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Show axis information
        g2d.drawString("Achsen:", width - 80, height - 30);
        g2d.drawString("X - horizontal", width - 80, height - 20);
        g2d.drawString("Y - vertikal", width - 80, height - 10);
        g2d.drawString("Z - tiefe", width - 80, height);

        // Show value ranges
        g2d.drawString(String.format("X: [%.2f, %.2f]", view.getXMin(), view.getXMax()), 10, height - 40);
        g2d.drawString(String.format("Y: [%.2f, %.2f]", view.getYMin(), view.getYMax()), 10, height - 25);
        g2d.drawString(String.format("Z: [%.2f, %.2f]", model.getZMin(), model.getZMax()), 10, height - 10);
    }

    /**
     * Helper method to draw a 3D line with transformations
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
        // Transform the first point
        Plot3DPoint p1 = transformer.transformPoint(
                x1, y1, z1,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                panX, panY);

        // Transform the second point
        Plot3DPoint p2 = transformer.transformPoint(
                x2, y2, z2,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                panX, panY);

        // Project to screen coordinates
        int[] screenPos1 = transformer.projectToScreen(p1, displayScale, xOffset, yOffset);
        int[] screenPos2 = transformer.projectToScreen(p2, displayScale, xOffset, yOffset);

        // Draw the line
        g2d.drawLine(screenPos1[0], screenPos1[1], screenPos2[0], screenPos2[1]);
    }

    /**
     * Calculates an appropriate grid spacing based on the value range
     */
    private double calculateGridSpacing(double range) {
        // Goal: About 6 grid lines in the visible area
        double targetSteps = 6;
        double rawSpacing = range / targetSteps;

        // Normalize to powers of ten
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Round to "nice" values: 1, 2, 5, 10
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