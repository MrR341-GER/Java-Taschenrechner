
package plugins.plotter2d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * Handles drawing the grid and axes for the GraphPanel
 */
public class GridRenderer {
    private final GraphPanel panel;
    private final CoordinateTransformer transformer;

    // Constants
    private static final int TICK_LENGTH = 5; // Length of axis ticks

    public GridRenderer(GraphPanel panel, CoordinateTransformer transformer) {
        this.panel = panel;
        this.transformer = transformer;
    }

    /**
     * Draws the coordinate grid
     */
    public void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // Light gray
        g2d.setStroke(new BasicStroke(0.5f));

        // Calculate grid line spacing in world coordinates - use the Y range for both
        // axes
        double gridSpacing = calculateGridSpacing(transformer.getYMax() - transformer.getYMin());

        // Available drawing area
        int drawingWidth = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int drawingHeight = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        // X grid lines
        double x = Math.ceil(transformer.getXMin() / gridSpacing) * gridSpacing;
        while (x <= transformer.getXMax()) {
            int screenX = transformer.worldToScreenX(x);
            g2d.draw(new Line2D.Double(screenX, transformer.getYOffset(),
                    screenX, transformer.getYOffset() + drawingHeight));
            x += gridSpacing;
        }

        // Y grid lines
        double y = Math.ceil(transformer.getYMin() / gridSpacing) * gridSpacing;
        while (y <= transformer.getYMax()) {
            int screenY = transformer.worldToScreenY(y);
            g2d.draw(new Line2D.Double(transformer.getXOffset(), screenY,
                    transformer.getXOffset() + drawingWidth, screenY));
            y += gridSpacing;
        }
    }

    /**
     * Draws the X and Y axes with labels
     */
    public void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));

        // Available drawing area
        int drawingWidth = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int drawingHeight = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        // X axis
        int yAxisPos = transformer.worldToScreenY(0);
        if (yAxisPos < transformer.getYOffset())
            yAxisPos = transformer.getYOffset();
        if (yAxisPos > transformer.getYOffset() + drawingHeight)
            yAxisPos = transformer.getYOffset() + drawingHeight;

        g2d.draw(new Line2D.Double(transformer.getXOffset(), yAxisPos,
                transformer.getXOffset() + drawingWidth, yAxisPos));

        // Y axis
        int xAxisPos = transformer.worldToScreenX(0);
        if (xAxisPos < transformer.getXOffset())
            xAxisPos = transformer.getXOffset();
        if (xAxisPos > transformer.getXOffset() + drawingWidth)
            xAxisPos = transformer.getXOffset() + drawingWidth;

        g2d.draw(new Line2D.Double(xAxisPos, transformer.getYOffset(),
                xAxisPos, transformer.getYOffset() + drawingHeight));

        // Axis labels
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // Common spacing for both axes
        double gridSpacing = calculateGridSpacing(transformer.getYMax() - transformer.getYMin());

        // X axis ticks and labels
        double x = Math.ceil(transformer.getXMin() / gridSpacing) * gridSpacing;

        while (x <= transformer.getXMax()) {
            int screenX = transformer.worldToScreenX(x);

            // Only draw if within bounds
            if (screenX >= transformer.getXOffset() && screenX <= transformer.getXOffset() + drawingWidth) {
                // Tick mark
                g2d.draw(new Line2D.Double(screenX, yAxisPos - TICK_LENGTH, screenX, yAxisPos + TICK_LENGTH));

                // Label (not at 0, since that's where the axis label is)
                if (Math.abs(x) > 1e-10) { // Consider small values as zero
                    String label = transformer.getAxisFormat().format(x);
                    FontMetrics fm = g2d.getFontMetrics();

                    // Save the current transformation state
                    AffineTransform originalTransform = g2d.getTransform();

                    // Constant distance for all labels
                    int yOffset = 5; // Distance from the tick mark

                    // Position the text so it starts BELOW the axis
                    g2d.translate(screenX, yAxisPos + TICK_LENGTH + yOffset);
                    g2d.rotate(Math.PI / 2); // 90 degrees clockwise

                    // Draw text centered on the line
                    g2d.drawString(label, 0, 0);

                    // Restore the original transformation state
                    g2d.setTransform(originalTransform);
                }
            }
            x += gridSpacing;
        }

        // Y axis ticks and labels
        double y = Math.ceil(transformer.getYMin() / gridSpacing) * gridSpacing;

        while (y <= transformer.getYMax()) {
            int screenY = transformer.worldToScreenY(y);

            // Only draw if within bounds
            if (screenY >= transformer.getYOffset() && screenY <= transformer.getYOffset() + drawingHeight) {
                // Tick mark
                g2d.draw(new Line2D.Double(xAxisPos - TICK_LENGTH, screenY, xAxisPos + TICK_LENGTH, screenY));

                // Label (not at 0, since that's where the axis label is)
                if (Math.abs(y) > 1e-10) { // Consider small values as zero
                    String label = transformer.getAxisFormat().format(y);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, xAxisPos - labelWidth - 5, screenY + 4);
                }
            }
            y += gridSpacing;
        }

        // Origin label
        if (xAxisPos >= transformer.getXOffset() && xAxisPos <= transformer.getXOffset() + drawingWidth &&
                yAxisPos >= transformer.getYOffset() && yAxisPos <= transformer.getYOffset() + drawingHeight) {
            g2d.drawString("0", xAxisPos + 4, yAxisPos + 12);
        }

        // Axis labels
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("x", transformer.getXOffset() + drawingWidth + 10, yAxisPos + 4);
        g2d.drawString("y", xAxisPos - 4, transformer.getYOffset() - 10);
    }

    /**
     * Calculates a suitable spacing for grid lines
     */
    public double calculateGridSpacing(double range) {
        // Goal: About 10 grid lines in the visible area
        double rawSpacing = range / 10;

        // Normalize to powers of ten
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Round to "nice" values: 1, 2, 5, 10
        if (mantissa < 1.5)
            return Math.pow(10, exponent);
        else if (mantissa < 3.5)
            return 2 * Math.pow(10, exponent);
        else if (mantissa < 7.5)
            return 5 * Math.pow(10, exponent);
        else
            return 10 * Math.pow(10, exponent);
    }
}
