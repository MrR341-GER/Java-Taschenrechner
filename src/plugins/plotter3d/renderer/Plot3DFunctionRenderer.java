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
     * Draws all functions in the model
     * 
     * @param g2d          Graphics context
     * @param model        Data model with functions
     * @param view         View parameters
     * @param displayScale Scale factor for display
     * @param xOffset      X offset for display
     * @param yOffset      Y offset for display
     * @param useHeatmap   Whether to use heatmap coloring
     */
    public void drawFunctions(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        // Draw each function in the model
        for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
            drawFunctionGrid(g2d, functionInfo, model, displayScale, xOffset, yOffset, useHeatmap);
        }
    }

    /**
     * Draws a single function as a wireframe grid
     */
    private void drawFunctionGrid(Graphics2D g2d, Plot3DModel.Function3DInfo functionInfo,
            Plot3DModel model, double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        // Set line style for the grid
        g2d.setStroke(new BasicStroke(1.0f));

        // Get the grid resolution
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        if (gridPoints == null)
            return;

        int resolution = gridPoints.length;

        // Store original transform to restore later
        Stroke originalStroke = g2d.getStroke();

        // Draw horizontal lines (along X-axis)
        for (int j = 0; j < resolution; j++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int i = 0; i < resolution; i++) {
                // Use the original and projected point
                Plot3DPoint original = gridPoints[i][j][0];
                Plot3DPoint projected = gridPoints[i][j][2];

                // Calculate screen coordinates
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                // Set color based on Z value if using heatmap
                if (useHeatmap) {
                    // Normalize Z value to range 0-1 for color mapping
                    double normalizedZ = (original.getZ() - model.getZMin()) /
                            (model.getZMax() - model.getZMin());
                    // Clamp value to 0-1 range
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    // Get color from scheme
                    g2d.setColor(colorScheme.getColorForValue(normalizedZ));
                } else {
                    // Use function's fixed color
                    g2d.setColor(functionInfo.getColor());
                }

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                    // Draw the segment with current color
                    g2d.draw(path);
                    // Start a new path from this point
                    path.reset();
                    path.moveTo(screenPos[0], screenPos[1]);
                }
            }
        }

        // Draw vertical lines (along Y-axis)
        for (int i = 0; i < resolution; i++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int j = 0; j < resolution; j++) {
                // Use the original and projected point
                Plot3DPoint original = gridPoints[i][j][0];
                Plot3DPoint projected = gridPoints[i][j][2];

                // Calculate screen coordinates
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                // Set color based on Z value if using heatmap
                if (useHeatmap) {
                    // Normalize Z value to range 0-1 for color mapping
                    double normalizedZ = (original.getZ() - model.getZMin()) /
                            (model.getZMax() - model.getZMin());
                    // Clamp value to 0-1 range
                    normalizedZ = Math.max(0, Math.min(1, normalizedZ));
                    // Get color from scheme
                    g2d.setColor(colorScheme.getColorForValue(normalizedZ));
                } else {
                    // Use function's fixed color
                    g2d.setColor(functionInfo.getColor());
                }

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                    // Draw the segment with current color
                    g2d.draw(path);
                    // Start a new path from this point
                    path.reset();
                    path.moveTo(screenPos[0], screenPos[1]);
                }
            }
        }

        // Restore original stroke
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