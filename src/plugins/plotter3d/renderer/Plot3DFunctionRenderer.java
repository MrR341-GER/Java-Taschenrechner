import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Renders 3D function plots as wireframe grids
 */
public class Plot3DFunctionRenderer {
    // Transformer for coordinate conversion
    private final Plot3DTransformer transformer;

    public Plot3DFunctionRenderer(Plot3DTransformer transformer) {
        this.transformer = transformer;
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
     */
    public void drawFunctions(Graphics2D g2d, Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        // Draw each function in the model
        for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
            drawFunctionGrid(g2d, functionInfo, displayScale, xOffset, yOffset, useHeatmap);
        }
    }

    /**
     * Draws a single function as a wireframe grid
     */
    private void drawFunctionGrid(Graphics2D g2d, Plot3DModel.Function3DInfo functionInfo,
            double displayScale, int xOffset, int yOffset, boolean useHeatmap) {
        // Set line style for the grid
        g2d.setStroke(new BasicStroke(1.0f));

        // Die Farbwahl hängt nun vom useHeatmap-Parameter ab
        if (useHeatmap) {
            // Heatmap-Modus: Farbe basierend auf Z-Wert
            // Bestehende Logik für Heatmap-Darstellung...
        } else {
            // Feste Farbe verwenden
            g2d.setColor(functionInfo.getColor());
        }

        // Get the grid resolution
        Plot3DPoint[][][] gridPoints = functionInfo.gridPoints;
        if (gridPoints == null)
            return;

        int resolution = gridPoints.length;

        // Draw horizontal lines (along X-axis)
        for (int j = 0; j < resolution; j++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int i = 0; i < resolution; i++) {
                // Use the projected point (phase 2)
                Plot3DPoint projected = gridPoints[i][j][2];

                // Calculate screen coordinates
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                }
            }

            // Draw the path
            g2d.draw(path);
        }

        // Draw vertical lines (along Y-axis)
        for (int i = 0; i < resolution; i++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int j = 0; j < resolution; j++) {
                // Use the projected point (phase 2)
                Plot3DPoint projected = gridPoints[i][j][2];

                // Calculate screen coordinates
                int[] screenPos = transformer.projectToScreen(projected, displayScale, xOffset, yOffset);

                if (!started) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    started = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                }
            }

            // Draw the path
            g2d.draw(path);
        }
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