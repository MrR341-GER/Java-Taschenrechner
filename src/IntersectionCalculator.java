import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates and renders intersection points between functions
 */
public class IntersectionCalculator {
    private final GraphPanel panel;
    private final CoordinateTransformer transformer;
    private final FunctionRenderer functionRenderer;

    private boolean showIntersections = false;
    private List<IntersectionPoint> intersectionPoints = new ArrayList<>();

    public IntersectionCalculator(GraphPanel panel, CoordinateTransformer transformer,
            FunctionRenderer functionRenderer) {
        this.panel = panel;
        this.transformer = transformer;
        this.functionRenderer = functionRenderer;
    }

    /**
     * Toggles the display of intersection points
     */
    public void toggleIntersections(boolean show) {
        this.showIntersections = show;

        if (show) {
            calculateIntersections();
        } else {
            intersectionPoints.clear();
        }
    }

    /**
     * Returns if intersection points are currently shown
     */
    public boolean isShowingIntersections() {
        return showIntersections;
    }

    /**
     * Returns the list of intersection points
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionPoints;
    }

    /**
     * Calculates all intersection points between the drawn functions
     */
    public void calculateIntersections() {
        List<IntersectionPoint> oldIntersections = new ArrayList<>(intersectionPoints);
        intersectionPoints.clear();

        List<FunctionRenderer.FunctionInfo> functions = functionRenderer.getFunctions();

        // We need at least two functions for intersection points
        if (functions.size() < 2) {
            // Only fire an event if something changed
            if (!oldIntersections.isEmpty()) {
                panel.fireIntersectionsUpdated(oldIntersections, intersectionPoints);
            }
            return;
        }

        // Calculate intersection points for all function pairs
        for (int i = 0; i < functions.size() - 1; i++) {
            for (int j = i + 1; j < functions.size(); j++) {
                FunctionRenderer.FunctionInfo f1 = functions.get(i);
                FunctionRenderer.FunctionInfo f2 = functions.get(j);

                // Check if the functions are identical
                if (areFunctionsIdentical(f1.function, f2.function)) {
                    continue; // Skip identical functions
                }

                // Function expressions (try to extract them from the function object)
                String expr1 = "f" + (i + 1);
                String expr2 = "f" + (j + 1);

                // Find intersection points in the current view window
                List<Point2D.Double> points = IntersectionFinder.findIntersections(
                        f1.function, f2.function, transformer.getXMin(), transformer.getXMax());

                // Add the found intersection points as IntersectionPoint objects to the total
                // list
                for (Point2D.Double point : points) {
                    IntersectionPoint ip = new IntersectionPoint(
                            point.x, point.y, i, j, expr1, expr2);

                    // Check for duplicates
                    boolean isDuplicate = false;
                    for (IntersectionPoint existingPoint : intersectionPoints) {
                        if (Math.abs(existingPoint.x - point.x) < 1e-6 &&
                                Math.abs(existingPoint.y - point.y) < 1e-6) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        intersectionPoints.add(ip);
                    }
                }
            }
        }

        // Fire event if the intersection points changed
        boolean changed = oldIntersections.size() != intersectionPoints.size();
        if (!changed) {
            // Check for different points
            for (int i = 0; i < intersectionPoints.size(); i++) {
                if (i >= oldIntersections.size() ||
                        Math.abs(intersectionPoints.get(i).x - oldIntersections.get(i).x) > 1e-6 ||
                        Math.abs(intersectionPoints.get(i).y - oldIntersections.get(i).y) > 1e-6) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            panel.fireIntersectionsUpdated(oldIntersections, intersectionPoints);
        }
    }

    /**
     * Checks if two functions are identical by comparing several test samples
     */
    private boolean areFunctionsIdentical(FunctionParser f1, FunctionParser f2) {
        // Number of test points
        final int NUM_TEST_POINTS = 10;

        // Range for the test points (current visible range)
        double min = transformer.getXMin();
        double max = transformer.getXMax();
        double step = (max - min) / (NUM_TEST_POINTS - 1);

        // Test multiple points in the current range
        for (int i = 0; i < NUM_TEST_POINTS; i++) {
            double x = min + i * step;
            try {
                double y1 = f1.evaluateAt(x);
                double y2 = f2.evaluateAt(x);

                // If a y value is NaN or infinite, skip this point
                if (Double.isNaN(y1) || Double.isInfinite(y1) ||
                        Double.isNaN(y2) || Double.isInfinite(y2)) {
                    continue;
                }

                // If y values are different, the functions are not identical
                if (Math.abs(y1 - y2) > 1e-10) {
                    return false;
                }
            } catch (Exception e) {
                // If errors occur during evaluation, one of the points is considered different
                return false;
            }
        }

        // If all samples are identical, we assume the functions are the same
        return true;
    }

    /**
     * Draws the intersection points
     */
    public void drawIntersectionPoints(Graphics2D g2d) {
        if (!showIntersections || intersectionPoints.isEmpty()) {
            return;
        }

        // Settings for intersection points
        g2d.setColor(Color.RED);
        int pointSize = 8;

        // Draw each intersection point as a small filled circle
        for (IntersectionPoint point : intersectionPoints) {
            // Check if the point is in the visible area
            if (point.x >= transformer.getXMin() && point.x <= transformer.getXMax() &&
                    point.y >= transformer.getYMin() && point.y <= transformer.getYMax()) {

                int screenX = transformer.worldToScreenX(point.x);
                int screenY = transformer.worldToScreenY(point.y);

                // Draw a filled circle
                g2d.fillOval(screenX - pointSize / 2, screenY - pointSize / 2,
                        pointSize, pointSize);

                // Draw the coordinates as text next to it with dynamic precision
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String coords = "(" + transformer.getAxisFormat().format(point.x) +
                        ", " + transformer.getAxisFormat().format(point.y) + ")";
                g2d.drawString(coords, screenX + pointSize, screenY);
            }
        }
    }
}
