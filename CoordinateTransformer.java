import java.awt.geom.Point2D;
import java.text.DecimalFormat;

/**
 * Handles coordinate transformations and view management for GraphPanel
 */
public class CoordinateTransformer {
    // Constants for the display
    private static final double DEFAULT_VIEW_RANGE = 20.0; // Standard range (-10 to +10)
    private static final double MIN_PIXELS_PER_UNIT = 10.0; // Minimum pixels per unit for readability

    // Display parameters
    private double xMin = -10; // Minimum X value in the visible area
    private double xMax = 10; // Maximum X value in the visible area
    private double yMin = -10; // Minimum Y value in the visible area
    private double yMax = 10; // Maximum Y value in the visible area
    private double xScale; // Scaling factor for X values (pixels per unit)
    private double yScale; // Scaling factor for Y values (pixels per unit)

    // Offsets for centered coordinate system
    private int xOffset;
    private int yOffset;

    // Stores the center to maintain it during resizing
    private Point2D.Double viewCenter = new Point2D.Double(0, 0);

    // Formatter for axis labels - dynamically updated
    private DecimalFormat axisFormat;

    // Reference to the panel dimensions
    private final GraphPanel panel;

    public CoordinateTransformer(GraphPanel panel) {
        this.panel = panel;
        axisFormat = new DecimalFormat("0.##");
    }

    /**
     * Updates the stored center of the view
     */
    public void updateViewCenter() {
        viewCenter.x = (xMax + xMin) / 2;
        viewCenter.y = (yMax + yMin) / 2;
    }

    /**
     * Adjusts the view to maintain the correct aspect ratio based on the panel
     * size,
     * and ensures that the coordinate system remains readable
     */
    public void adjustViewToMaintainAspectRatio() {
        int width = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int height = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        if (width <= 0 || height <= 0)
            return; // Prevent division by zero

        // Calculate the panel's aspect ratio
        double panelAspectRatio = (double) width / height;

        // Calculate the available pixels per unit
        double pixelsPerUnitX = width / (xMax - xMin);
        double pixelsPerUnitY = height / (yMax - yMin);

        // If one of the values is too low, adjust both ranges
        if (pixelsPerUnitX < MIN_PIXELS_PER_UNIT || pixelsPerUnitY < MIN_PIXELS_PER_UNIT) {
            // Determine how many units in each direction can be displayed based on minimum
            // readability
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Ensure that the aspect ratio is maintained
            if (maxUnitsX / maxUnitsY < panelAspectRatio) {
                // X direction is constraining
                maxUnitsY = maxUnitsX / panelAspectRatio;
            } else {
                // Y direction is constraining
                maxUnitsX = maxUnitsY * panelAspectRatio;
            }

            // Calculate new limits based on the stored center
            double halfX = maxUnitsX / 2;
            double halfY = maxUnitsY / 2;
            xMin = viewCenter.x - halfX;
            xMax = viewCenter.x + halfX;
            yMin = viewCenter.y - halfY;
            yMax = viewCenter.y + halfY;
        } else {
            // If readability is guaranteed, just adjust the limits to maintain the aspect
            // ratio
            double xRange = xMax - xMin;
            double yRange = yMax - yMin;
            double currentAspectRatio = xRange / yRange;

            if (Math.abs(currentAspectRatio - panelAspectRatio) > 0.01) { // Tolerance for floating point comparison
                if (currentAspectRatio < panelAspectRatio) {
                    // X range needs to be increased
                    double newXRange = yRange * panelAspectRatio;
                    double halfDeltaX = (newXRange - xRange) / 2;
                    xMin -= halfDeltaX;
                    xMax += halfDeltaX;
                } else {
                    // Y range needs to be increased
                    double newYRange = xRange / panelAspectRatio;
                    double halfDeltaY = (newYRange - yRange) / 2;
                    yMin -= halfDeltaY;
                    yMax += halfDeltaY;
                }

                // Update the center
                updateViewCenter();
            }
        }

        // Update the scaling factors
        updateScaleFactors();
    }

    /**
     * Updates the scaling factors based on the current view and panel size
     */
    private void updateScaleFactors() {
        int width = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int height = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        xScale = width / (xMax - xMin);
        yScale = height / (yMax - yMin);

        xOffset = GraphPanel.AXIS_MARGIN;
        yOffset = GraphPanel.AXIS_MARGIN;
    }

    /**
     * Determines the appropriate number of decimal places based on the zoom level
     */
    public void updateAxisFormat() {
        // Calculate the value range (smaller value ranges = more decimal places)
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Use the smaller range for formatting
        double range = Math.min(xRange, yRange);

        // Logarithmic scaling for the number of decimal places
        int decimalPlaces = 2; // Minimum 2 decimal places

        if (range < 0.1) {
            decimalPlaces = 5;
        } else if (range < 1) {
            decimalPlaces = 4;
        } else if (range < 10) {
            decimalPlaces = 3;
        }

        // Create format string with variable number of decimal places
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("#");
        }

        // Update the format
        axisFormat = new DecimalFormat(pattern.toString());
    }

    /**
     * Resets the view to standard values
     */
    public void resetView() {
        // Set the center to the origin
        viewCenter.x = 0;
        viewCenter.y = 0;

        // Calculate a suitable view based on the window size
        int width = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int height = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        if (width <= 0 || height <= 0) {
            // If the panel doesn't have a size yet, use standard values
            xMin = -10;
            xMax = 10;
            yMin = -10;
            yMax = 10;
        } else {
            // Calculate how many units can be displayed with minimum readability
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Use the minimum or the standard view
            double halfX = Math.min(maxUnitsX, DEFAULT_VIEW_RANGE) / 2;
            double halfY = Math.min(maxUnitsY, DEFAULT_VIEW_RANGE) / 2;

            // Ensure that the aspect ratio is maintained
            double panelAspectRatio = (double) width / height;
            if (halfX / halfY < panelAspectRatio) {
                halfY = halfX / panelAspectRatio;
            } else {
                halfX = halfY * panelAspectRatio;
            }

            // Set the limits
            xMin = -halfX;
            xMax = halfX;
            yMin = -halfY;
            yMax = halfY;
        }

        // Make sure the view is properly adjusted
        adjustViewToMaintainAspectRatio();
    }

    /**
     * Centers the view on the specified point
     */
    public void centerViewAt(double xCenter, double yCenter) {
        // Store the new center
        viewCenter.x = xCenter;
        viewCenter.y = yCenter;

        // Calculate the current range
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Set new limits around the target point
        xMin = xCenter - xRange / 2;
        xMax = xCenter + xRange / 2;
        yMin = yCenter - yRange / 2;
        yMax = yCenter + yRange / 2;

        // Update the scaling factors
        updateScaleFactors();
    }

    /**
     * Converts an X screen coordinate to an X world coordinate
     */
    public double screenToWorldX(int screenX) {
        return xMin + (screenX - xOffset) / xScale;
    }

    /**
     * Converts a Y screen coordinate to a Y world coordinate
     */
    public double screenToWorldY(int screenY) {
        return yMax - (screenY - yOffset) / yScale;
    }

    /**
     * Converts an X world coordinate to an X screen coordinate
     */
    public int worldToScreenX(double worldX) {
        return (int) (xOffset + (worldX - xMin) * xScale);
    }

    /**
     * Converts a Y world coordinate to a Y screen coordinate
     */
    public int worldToScreenY(double worldY) {
        return (int) (yOffset + (yMax - worldY) * yScale);
    }

    /**
     * Returns the coordinates of the current image center
     */
    public Point2D.Double getViewCenter() {
        return new Point2D.Double(viewCenter.x, viewCenter.y);
    }

    /**
     * Zooms the view by the specified factor, centered on the specified screen
     * point
     */
    public void zoom(double factor, Point2D screenPoint) {
        // Convert screen point to world coordinates
        double worldMouseX = screenToWorldX((int) screenPoint.getX());
        double worldMouseY = screenToWorldY((int) screenPoint.getY());

        // Store current ranges
        double oldYRange = yMax - yMin;
        double oldXRange = xMax - xMin;

        // Adjust ranges
        double newYRange = oldYRange * factor;
        double newXRange = oldXRange * factor;

        // Determine the point to zoom on (position of the mouse pointer)
        double relX = (worldMouseX - xMin) / oldXRange; // Position relative to width
        double relY = (worldMouseY - yMin) / oldYRange; // Position relative to height

        // Calculate new limits, so that the mouse point maintains its relative position
        xMin = worldMouseX - relX * newXRange;
        xMax = xMin + newXRange;
        yMin = worldMouseY - relY * newYRange;
        yMax = yMin + newYRange;

        // Update the center
        updateViewCenter();

        // Update the scaling factors
        updateScaleFactors();
    }

    /**
     * Pans the view by the specified pixel amount
     */
    public void pan(int dx, int dy) {
        // Calculate the displacement in world coordinates
        double worldDx = dx / xScale;
        double worldDy = dy / yScale;

        // Adjust the view
        xMin -= worldDx;
        xMax -= worldDx;
        yMin += worldDy;
        yMax += worldDy;

        // Update the center
        updateViewCenter();

        // Update the scaling factors
        updateScaleFactors();
    }

    // Getters
    public double getXMin() {
        return xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public double getYMin() {
        return yMin;
    }

    public double getYMax() {
        return yMax;
    }

    public double getXScale() {
        return xScale;
    }

    public double getYScale() {
        return yScale;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public DecimalFormat getAxisFormat() {
        return axisFormat;
    }

    // Setters
    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }
}
