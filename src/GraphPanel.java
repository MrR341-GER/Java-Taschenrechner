import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.util.List;

/**
 * GraphPanel - A panel for drawing function graphs in a coordinate system
 */
public class GraphPanel extends JPanel {
    // Constants for the display
    public static final int AXIS_MARGIN = 40; // Distance of axes from the edge
    private static final float ZOOM_FACTOR = 1.2f; // Factor for zoom operations
    public static final int MIN_HEIGHT = 200; // Minimum height of the coordinate system
    public static final int MIN_WIDTH = 300; // Minimum width of the coordinate system
    private static final int INTERSECTION_HIT_RADIUS = 10; // Radius for detecting mouseover on intersections

    // Helper classes for different aspects of the panel
    private final CoordinateTransformer transformer;
    private final GridRenderer gridRenderer;
    private final FunctionRenderer functionRenderer;
    private final IntersectionCalculator intersectionCalculator;

    // Mouse interaction
    private Point lastMousePos; // Last mouse position (for pan)
    private boolean isDragging = false; // Is currently being dragged?

    // Tooltip support
    private IntersectionPoint currentTooltipPoint = null;
    private DecimalFormat tooltipFormat = new DecimalFormat("0.########");

    // Property change support for notifications
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // Flag zum Anzeigen/Ausblenden des Koordinatensystems
    private boolean showGrid = true;

    /**
     * Constructor - initializes the panel and adds mouse listeners
     */
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Double buffering for smooth drawing
        setDoubleBuffered(true);

        // Initialize helper classes
        transformer = new CoordinateTransformer(this);
        gridRenderer = new GridRenderer(this, transformer);
        functionRenderer = new FunctionRenderer(this, transformer);
        intersectionCalculator = new IntersectionCalculator(this, transformer, functionRenderer);

        // Enable tooltips
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(100); // Show tooltip faster

        // Add mouse and component listeners
        setupMouseListeners();
        setupComponentListeners();

        // Initialize the view based on the current size
        resetView();
    }

    /**
     * Ein-/Ausschalten des Koordinatensystems
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
        repaint();
    }

    /**
     * Gibt zurück, ob das Koordinatensystem angezeigt wird
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * Sets up mouse listeners for zoom, pan and tooltips
     */
    private void setupMouseListeners() {
        // Mouse listener for press and release
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        // Mouse motion listener for dragging and tooltips
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Calculate the displacement in screen coordinates
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;

                    // Move the view
                    transformer.pan(dx, dy);
                    lastMousePos = e.getPoint();

                    // Redraw
                    repaint();

                    // Notify about the view change
                    fireViewChanged();

                    // If intersection points are enabled, recalculate them
                    if (intersectionCalculator.isShowingIntersections()) {
                        intersectionCalculator.calculateIntersections();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Check if mouse is over an intersection point
                if (intersectionCalculator.isShowingIntersections()) {
                    IntersectionPoint point = findIntersectionPointNear(e.getPoint());
                    if (point != currentTooltipPoint) {
                        currentTooltipPoint = point;
                        // Trigger tooltip update
                        setToolTipText(null); // Force tooltip manager to call getToolTipText
                    }
                }
            }
        });

        // Mouse wheel listener for zoom
        addMouseWheelListener(e -> {
            // Save the original mouse position in screen coordinates
            Point mousePoint = e.getPoint();

            // Zoom factor based on mouse wheel direction (reversed)
            double factor = (e.getWheelRotation() > 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Zoom
            transformer.zoom(factor, mousePoint);

            repaint();

            // Notify about the view change
            fireViewChanged();

            // If intersection points are enabled, recalculate them
            if (intersectionCalculator.isShowingIntersections()) {
                intersectionCalculator.calculateIntersections();
            }
        });
    }

    /**
     * Sets up component listeners for resize events
     */
    private void setupComponentListeners() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // On resize, adjust the view but maintain the center
                transformer.adjustViewToMaintainAspectRatio();

                // If intersection points are enabled, recalculate them
                if (intersectionCalculator.isShowingIntersections()) {
                    intersectionCalculator.calculateIntersections();
                }
            }
        });
    }

    /**
     * Find the closest intersection point near the mouse position
     */
    private IntersectionPoint findIntersectionPointNear(Point mousePos) {
        if (!intersectionCalculator.isShowingIntersections()) {
            return null;
        }

        List<IntersectionPoint> points = intersectionCalculator.getIntersectionPoints();
        IntersectionPoint closest = null;
        double minDistance = INTERSECTION_HIT_RADIUS;

        for (IntersectionPoint point : points) {
            // Check if the point is in the visible area
            if (point.x >= transformer.getXMin() && point.x <= transformer.getXMax() &&
                    point.y >= transformer.getYMin() && point.y <= transformer.getYMax()) {

                int screenX = transformer.worldToScreenX(point.x);
                int screenY = transformer.worldToScreenY(point.y);

                double distance = mousePos.distance(screenX, screenY);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = point;
                }
            }
        }

        return closest;
    }

    /**
     * Returns tooltip text for the current mouse position
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        if (currentTooltipPoint != null) {
            String func1 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex2());

            return "<html><b>Schnittpunkt</b><br>" +
                    "x = " + tooltipFormat.format(currentTooltipPoint.x) + "<br>" +
                    "y = " + tooltipFormat.format(currentTooltipPoint.y) + "<br>" +
                    "zwischen:<br>" +
                    "- " + func1 + "<br>" +
                    "- " + func2 + "</html>";
        }
        return null;
    }

    /**
     * Helper method to get a function expression by its index
     */
    private String getFunctionExpressionByIndex(int index) {
        List<FunctionRenderer.FunctionInfo> functions = functionRenderer.getFunctions();
        if (index >= 0 && index < functions.size()) {
            return "f" + (index + 1) + "(x)";
        }
        return "f" + (index + 1) + "(x)";
    }

    /**
     * Adds a PropertyChangeListener
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Fires an event for view changes
     */
    public void fireViewChanged() {
        Point2D.Double oldCenter = null; // We don't need the old value
        Point2D.Double newCenter = getViewCenter();
        pcs.firePropertyChange("viewChanged", oldCenter, newCenter);
    }

    /**
     * Fires an event for intersection updates
     */
    public void fireIntersectionsUpdated(List<IntersectionPoint> oldIntersections,
            List<IntersectionPoint> newIntersections) {
        pcs.firePropertyChange("intersectionsUpdated", oldIntersections, newIntersections);
    }

    /**
     * Paints the panel including coordinate system and functions
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Update the axis format based on the zoom level
        transformer.updateAxisFormat();

        // Draw the coordinate grid and axes only if enabled
        if (showGrid) {
            gridRenderer.drawGrid(g2d);
            gridRenderer.drawAxes(g2d);
        }

        // Draw the functions
        functionRenderer.drawFunctions(g2d);

        // Draw intersection points if enabled
        intersectionCalculator.drawIntersectionPoints(g2d);

        // Info text
        g2d.setColor(Color.BLACK);
        g2d.drawString("Zoom: Mausrad, Verschieben: Maus ziehen", 10, getHeight() - 10);
    }

    /**
     * Adds a new function to the plotter
     */
    public void addFunction(String expression, Color color) {
        functionRenderer.addFunction(expression, color);

        if (intersectionCalculator.isShowingIntersections()) {
            intersectionCalculator.calculateIntersections();
        }

        repaint();
    }

    /**
     * Removes all functions
     */
    public void clearFunctions() {
        functionRenderer.clearFunctions();
        intersectionCalculator.getIntersectionPoints().clear();
        repaint();
    }

    /**
     * Returns the coordinates of the current view center
     */
    public Point2D.Double getViewCenter() {
        return transformer.getViewCenter();
    }

    /**
     * Resets the view to standard values
     */
    public void resetView() {
        transformer.resetView();
        repaint();
        fireViewChanged();
    }

    /**
     * Centers the view on the specified point
     */
    public void centerViewAt(double xCenter, double yCenter) {
        transformer.centerViewAt(xCenter, yCenter);
        repaint();
        fireViewChanged();

        if (intersectionCalculator.isShowingIntersections()) {
            intersectionCalculator.calculateIntersections();
        }
    }

    /**
     * Toggles the display of intersection points
     */
    public void toggleIntersections(boolean show) {
        intersectionCalculator.toggleIntersections(show);
        repaint();
    }

    /**
     * Returns the list of intersection points
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionCalculator.getIntersectionPoints();
    }
}