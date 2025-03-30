
package plugins.plotter3d.view;

/**
 * Manages view-related parameters for 3D plotting
 * Handles bounds, rotation, scale, and display options
 */
public class Plot3DView {
    // View bounds
    private double xMin, xMax, yMin, yMax;
    private double originalXMin, originalXMax, originalYMin, originalYMax; // For reset

    // Resolution (grid density)
    private int resolution;

    // Rotation angles (in degrees)
    private double rotationX; // Rotation around X-axis
    private double rotationY; // Rotation around Y-axis
    private double rotationZ; // Rotation around Z-axis

    // Scale and pan
    private double scale = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Display options
    private boolean showCoordinateSystem = true;
    private boolean showGrid = true;
    private boolean showHelperLines = true;

    // Farbmodus
    private boolean useHeatmap = true; // Standardmäßig Heatmap verwenden

    /**
     * Creates a new view with the specified bounds and resolution
     */
    public Plot3DView(double xMin, double xMax, double yMin, double yMax, int resolution) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        // Store original bounds for reset
        this.originalXMin = xMin;
        this.originalXMax = xMax;
        this.originalYMin = yMin;
        this.originalYMax = yMax;

        this.resolution = Math.max(10, Math.min(100, resolution)); // Min 10, Max 100

        // Default rotation
        this.rotationX = 30;
        this.rotationY = 0;
        this.rotationZ = 30;
    }

    /**
     * Resets the view to its original state
     */
    public void resetView() {
        xMin = originalXMin;
        xMax = originalXMax;
        yMin = originalYMin;
        yMax = originalYMax;

        rotationX = 30;
        rotationY = 0;
        rotationZ = 30;

        scale = 1.0;
        panX = 0.0;
        panY = 0.0;
    }

    /**
     * Setzt den Heatmap-Modus
     */
    public void setUseHeatmap(boolean useHeatmap) {
        this.useHeatmap = useHeatmap;
    }

    /**
     * Gibt zurück, ob der Heatmap-Modus aktiv ist
     */
    public boolean isUseHeatmap() {
        return useHeatmap;
    }

    /**
     * Sets new bounds
     */
    public void setBounds(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    /**
     * Sets rotation angles
     */
    public void setRotation(double rotationX, double rotationY, double rotationZ) {
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    /**
     * Sets scale factor
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.1, Math.min(10.0, scale)); // Limit scale to reasonable range
    }

    /**
     * Zooms by the given factor
     */
    public void zoom(double factor) {
        setScale(scale * factor);
    }

    /**
     * Sets pan values
     */
    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
    }

    /**
     * Adds to the current pan values
     */
    public void addPan(double deltaPanX, double deltaPanY) {
        this.panX += deltaPanX;
        this.panY += deltaPanY;
    }

    /**
     * Sets the resolution (grid density)
     */
    public void setResolution(int resolution) {
        this.resolution = Math.max(10, Math.min(100, resolution)); // Min 10, Max 100
    }

    /**
     * Toggles coordinate system visibility
     */
    public void setShowCoordinateSystem(boolean show) {
        this.showCoordinateSystem = show;
    }

    /**
     * Toggles grid visibility
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }

    /**
     * Toggles helper lines visibility
     */
    public void setShowHelperLines(boolean show) {
        this.showHelperLines = show;
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

    public int getResolution() {
        return resolution;
    }

    public double getRotationX() {
        return rotationX;
    }

    public double getRotationY() {
        return rotationY;
    }

    public double getRotationZ() {
        return rotationZ;
    }

    public double getScale() {
        return scale;
    }

    public double getPanX() {
        return panX;
    }

    public double getPanY() {
        return panY;
    }

    public boolean isShowCoordinateSystem() {
        return showCoordinateSystem;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public boolean isShowHelperLines() {
        return showHelperLines;
    }
}