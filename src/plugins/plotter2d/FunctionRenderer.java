package plugins.plotter2d;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import parser.FunctionParser;

/**
 * Handles the rendering of mathematical functions
 */
public class FunctionRenderer {
    private final GraphPanel panel;
    private final CoordinateTransformer transformer;
    private final List<FunctionInfo> functions = new ArrayList<>();

    // Constants
    private static final int MAX_PIXEL_JUMP = 100; // Maximum pixel jump between two points
    private static final float NORMAL_STROKE_WIDTH = 2.0f; // Normal line thickness
    private static final float SELECTED_STROKE_WIDTH = 4.0f; // Thickness for selected function

    public FunctionRenderer(GraphPanel panel, CoordinateTransformer transformer) {
        this.panel = panel;
        this.transformer = transformer;
    }

    /**
     * Adds a new function to the renderer
     */
    public void addFunction(String expression, Color color) {
        FunctionParser parser = new FunctionParser(expression);
        functions.add(new FunctionInfo(parser, color));
    }

    /**
     * Removes all functions
     */
    public void clearFunctions() {
        functions.clear();
    }

    /**
     * Returns the list of functions
     */
    public List<FunctionInfo> getFunctions() {
        return functions;
    }

    /**
     * Draws all functions, with the selected one highlighted
     */
    public void drawFunctions(Graphics2D g2d, int selectedIndex) {
        // First draw all non-selected functions
        for (int i = 0; i < functions.size(); i++) {
            if (i != selectedIndex) {
                drawFunctionWithEdges(g2d, functions.get(i), false);
            }
        }

        // Then draw the selected function (if any) on top
        if (selectedIndex >= 0 && selectedIndex < functions.size()) {
            drawFunctionWithEdges(g2d, functions.get(selectedIndex), true);
        }
    }

    /**
     * Draws a function and connects it correctly with the edges of the visible area
     */
    private void drawFunctionWithEdges(Graphics2D g2d, FunctionInfo functionInfo, boolean isSelected) {
        g2d.setColor(functionInfo.getColor());

        // Use thicker stroke for selected function
        float strokeWidth = isSelected ? SELECTED_STROKE_WIDTH : NORMAL_STROKE_WIDTH;
        g2d.setStroke(new BasicStroke(strokeWidth));

        // Drawing area
        int drawingWidth = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int drawingHeight = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        // List of paths to draw
        List<Path2D> paths = new ArrayList<>();
        Path2D currentPath = null;

        // Last valid coordinates
        Double lastX = null;
        Double lastY = null;
        Integer lastScreenX = null;
        Integer lastScreenY = null;

        // Y boundaries of the visible area
        int topScreenY = transformer.getYOffset();
        int bottomScreenY = transformer.getYOffset() + drawingHeight;

        // Calculate point by point from left to right for each pixel
        for (int screenX = transformer.getXOffset(); screenX <= transformer.getXOffset() + drawingWidth; screenX++) {
            // Convert screen X to world X
            double x = transformer.screenToWorldX(screenX);

            try {
                // Calculate Y value using the function's parser
                double y = functionInfo.getFunction().evaluateAt(x);

                // Check for valid value
                if (Double.isNaN(y) || Double.isInfinite(y)) {
                    // Invalid point - end current path if necessary
                    if (currentPath != null) {
                        paths.add(currentPath);
                        currentPath = null;
                    }
                    lastX = null;
                    lastY = null;
                    lastScreenX = null;
                    lastScreenY = null;
                    continue;
                }

                // Calculate screen Y
                int screenY;

                // Determine if the point is in the visible area
                boolean inVisibleRange = (y >= transformer.getYMin() && y <= transformer.getYMax());

                // If the point is outside the visible area,
                // calculate the intersection with the edge
                if (!inVisibleRange) {
                    // Calculate the Y value for the edge
                    double boundaryY;
                    int boundaryScreenY;

                    if (y < transformer.getYMin()) {
                        boundaryY = transformer.getYMin();
                        boundaryScreenY = bottomScreenY; // Bottom edge
                    } else { // y > transformer.getYMax()
                        boundaryY = transformer.getYMax();
                        boundaryScreenY = topScreenY; // Top edge
                    }

                    // If we have a previous point, we can calculate the intersection
                    if (lastX != null && lastY != null &&
                            ((lastY >= transformer.getYMin() && lastY <= transformer.getYMax()) || // Previous point in
                                                                                                   // range
                                    (lastY < transformer.getYMin() && y > transformer.getYMax()) || // Transition from
                                                                                                    // below to above
                                    (lastY > transformer.getYMax() && y < transformer.getYMin()))) // Transition from
                                                                                                   // above to below
                    {
                        // Calculate the intersection with the edge
                        // Since we work for each pixel, the calculation is very accurate

                        if (lastY >= transformer.getYMin() && lastY <= transformer.getYMax()) {
                            // Previous point was in range - intersection with current edge
                            if (currentPath == null) {
                                currentPath = new Path2D.Double();
                                currentPath.moveTo(lastScreenX, lastScreenY);
                            }

                            // Calculate the exact X position of the intersection
                            double t = (boundaryY - lastY) / (y - lastY);
                            double intersectX = lastX + t * (x - lastX);
                            int intersectScreenX = transformer.worldToScreenX(intersectX);

                            // Draw to the edge and end the path
                            currentPath.lineTo(intersectScreenX, boundaryScreenY);
                            paths.add(currentPath);
                            currentPath = null;
                        } else if ((lastY < transformer.getYMin() && y > transformer.getYMax()) ||
                                (lastY > transformer.getYMax() && y < transformer.getYMin())) {
                            // The function skips the entire visible area
                            // Calculate both intersections and draw a line through the visible area

                            // Intersection 1 - with upper or lower edge
                            double t1 = (lastY < transformer.getYMin() ? transformer.getYMin() - lastY
                                    : transformer.getYMax() - lastY) / (y - lastY);
                            double intersectX1 = lastX + t1 * (x - lastX);
                            int intersectScreenX1 = transformer.worldToScreenX(intersectX1);
                            int intersectScreenY1 = lastY < transformer.getYMin() ? bottomScreenY : topScreenY;

                            // Intersection 2 - with the opposite edge
                            double t2 = (lastY < transformer.getYMin() ? transformer.getYMax() - lastY
                                    : transformer.getYMin() - lastY) / (y - lastY);
                            double intersectX2 = lastX + t2 * (x - lastX);
                            int intersectScreenX2 = transformer.worldToScreenX(intersectX2);
                            int intersectScreenY2 = lastY < transformer.getYMin() ? topScreenY : bottomScreenY;

                            // Draw a line between the two intersections
                            Path2D crossPath = new Path2D.Double();
                            crossPath.moveTo(intersectScreenX1, intersectScreenY1);
                            crossPath.lineTo(intersectScreenX2, intersectScreenY2);
                            paths.add(crossPath);
                        }
                    }

                    // Update the last values
                    lastX = x;
                    lastY = y;
                    lastScreenX = screenX;
                    lastScreenY = y < transformer.getYMin() ? bottomScreenY : topScreenY;
                } else {
                    // The point is in the visible area
                    screenY = transformer.worldToScreenY(y);

                    // If we have a previous point that was outside,
                    // calculate the intersection with the edge
                    if (lastX != null && lastY != null &&
                            (lastY < transformer.getYMin() || lastY > transformer.getYMax())) {
                        // Calculate the edge we intersect with
                        double boundaryY = lastY < transformer.getYMin() ? transformer.getYMin()
                                : transformer.getYMax();

                        // Calculate the exact X position of the intersection
                        double t = (boundaryY - lastY) / (y - lastY);
                        double intersectX = lastX + t * (x - lastX);
                        int intersectScreenX = transformer.worldToScreenX(intersectX);
                        int intersectScreenY = lastY < transformer.getYMin() ? bottomScreenY : topScreenY;

                        // Start a new path at the intersection
                        currentPath = new Path2D.Double();
                        currentPath.moveTo(intersectScreenX, intersectScreenY);
                        currentPath.lineTo(screenX, screenY);
                    } else if (currentPath == null) {
                        // Start a new path
                        currentPath = new Path2D.Double();
                        currentPath.moveTo(screenX, screenY);
                    } else {
                        // Add the point to the existing path
                        currentPath.lineTo(screenX, screenY);
                    }

                    // Update the last values
                    lastX = x;
                    lastY = y;
                    lastScreenX = screenX;
                    lastScreenY = screenY;
                }
            } catch (Exception e) {
                // Error in evaluation - end the current path
                if (currentPath != null) {
                    paths.add(currentPath);
                    currentPath = null;
                }
                lastX = null;
                lastY = null;
                lastScreenX = null;
                lastScreenY = null;
            }
        }

        // Add the last path if necessary
        if (currentPath != null) {
            paths.add(currentPath);
        }

        // Draw all paths
        for (Path2D path : paths) {
            g2d.draw(path);
        }
    }

    /**
     * Class for storing function information
     */
    public static class FunctionInfo {
        private final FunctionParser function;
        private final Color color;

        public FunctionInfo(FunctionParser function, Color color) {
            this.function = function;
            this.color = color;
        }

        /**
         * Getter für den FunctionParser
         * 
         * @return Der FunctionParser
         */
        public FunctionParser getFunction() {
            return function;
        }

        /**
         * Getter für die Farbe
         * 
         * @return Die Farbe
         */
        public Color getColor() {
            return color;
        }
    }
}