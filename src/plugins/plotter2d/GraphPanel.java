package plugins.plotter2d;

import javax.swing.*;

import parser.FunctionParser;
import plugins.plotter2d.intersection.IntersectionCalculator;
import plugins.plotter2d.intersection.IntersectionPoint;

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
    private static final int HOVER_DETECTION_THRESHOLD = 5; // Pixel distance for hover detection

    // Helper classes for different aspects of the panel
    private final CoordinateTransformer transformer;
    private final GridRenderer gridRenderer;
    private final FunctionRenderer functionRenderer;
    private final IntersectionCalculator intersectionCalculator;

    // Mouse interaction
    private Point lastMousePos; // Last mouse position (for pan)
    private boolean isDragging = false; // Is currently being dragged?
    private Point currentMousePosition = null; // Current mouse position for hover detection
    private int closestFunctionIndex = -1; // Index of the function closest to mouse
    private Point2D.Double closestPoint = null; // Closest point on any function to mouse
    private int selectedFunctionIndex = -1; // Index of the currently selected function

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

                // If this was a click (not a significant drag), select the function
                if (e.getPoint().distance(lastMousePos) < 5) {
                    // Find which function was clicked (if any)
                    if (closestPoint != null && closestFunctionIndex >= 0) {
                        // Select this function
                        selectedFunctionIndex = closestFunctionIndex;
                        repaint();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // If clicked on empty space, deselect current function
                if (closestPoint == null) {
                    selectedFunctionIndex = -1;
                    repaint();
                }
            }
        });

        // Modified mouse motion listener for dragging and tooltips
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
                // Save current mouse position
                currentMousePosition = e.getPoint();

                // Find the closest point on any function
                findClosestPointOnFunction(currentMousePosition);

                // First check if mouse is over an intersection point
                if (intersectionCalculator.isShowingIntersections()) {
                    IntersectionPoint point = findIntersectionPointNear(e.getPoint());
                    if (point != currentTooltipPoint) {
                        currentTooltipPoint = point;
                        // Trigger tooltip update
                        setToolTipText(null); // Force tooltip manager to call getToolTipText
                    }
                } else if (closestPoint != null) {
                    // If we're not showing intersections but have a closest point, update tooltip
                    setToolTipText(null); // Force tooltip manager to call getToolTipText
                }

                // Repaint to show the hover marker
                repaint();
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
     * Find the closest point on any function to the mouse position
     */
    private void findClosestPointOnFunction(Point mousePos) {
        if (mousePos == null) {
            closestPoint = null;
            closestFunctionIndex = -1;
            return;
        }

        List<FunctionRenderer.FunctionInfo> functions = functionRenderer.getFunctions();
        if (functions.isEmpty()) {
            closestPoint = null;
            closestFunctionIndex = -1;
            return;
        }

        double minDistance = HOVER_DETECTION_THRESHOLD;
        closestPoint = null;
        closestFunctionIndex = -1;

        // Convert mouse position to world coordinates
        double mouseWorldX = transformer.screenToWorldX(mousePos.x);

        // Search within a small range around the mouse X position
        double searchStep = (transformer.getXMax() - transformer.getXMin()) / 100;
        double searchRange = searchStep * 3;

        // For each function
        for (int funcIndex = 0; funcIndex < functions.size(); funcIndex++) {
            FunctionParser parser = functions.get(funcIndex).getFunction();

            // Search for the closest point in a range around mouse X
            for (double x = mouseWorldX - searchRange; x <= mouseWorldX + searchRange; x += searchStep) {
                try {
                    // Skip if x is outside the view
                    if (x < transformer.getXMin() || x > transformer.getXMax())
                        continue;

                    double y = parser.evaluateAt(x);

                    // Skip if y is outside the view or not a valid number
                    if (Double.isNaN(y) || Double.isInfinite(y) ||
                            y < transformer.getYMin() || y > transformer.getYMax())
                        continue;

                    // Convert to screen coordinates
                    int screenX = transformer.worldToScreenX(x);
                    int screenY = transformer.worldToScreenY(y);

                    // Calculate distance to mouse
                    double distance = mousePos.distance(screenX, screenY);

                    // If this is closer than our threshold and previous closest
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPoint = new Point2D.Double(x, y);
                        closestFunctionIndex = funcIndex;
                    }
                } catch (Exception e) {
                    // Skip errors in function evaluation
                    continue;
                }
            }
        }
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
            // Show intersection point tooltip
            String func1 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex2());

            return "<html><b>Schnittpunkt</b><br>" +
                    "x = " + tooltipFormat.format(currentTooltipPoint.x) + "<br>" +
                    "y = " + tooltipFormat.format(currentTooltipPoint.y) + "<br>" +
                    "zwischen:<br>" +
                    "- " + func1 + "<br>" +
                    "- " + func2 + "</html>";
        } else if (closestPoint != null) {
            // Show function point tooltip
            String funcExpr = getFunctionExpressionByIndex(closestFunctionIndex);

            return "<html><b>Koordinate</b><br>" +
                    "x = " + tooltipFormat.format(closestPoint.x) + "<br>" +
                    "y = " + tooltipFormat.format(closestPoint.y) + "<br>" +
                    "Funktion: " + funcExpr + "</html>";
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
        functionRenderer.drawFunctions(g2d, selectedFunctionIndex);

        // Draw intersection points if enabled
        intersectionCalculator.drawIntersectionPoints(g2d);

        // Draw hover marker if we have a closest point
        if (closestPoint != null) {
            int screenX = transformer.worldToScreenX(closestPoint.x);
            int screenY = transformer.worldToScreenY(closestPoint.y);

            // Draw a circle at the point
            int markerSize = 8;
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(screenX - markerSize / 2, screenY - markerSize / 2, markerSize, markerSize);

            // Draw coordinates near the point if not too close to an edge
            String coordText = "(" + tooltipFormat.format(closestPoint.x) +
                    ", " + tooltipFormat.format(closestPoint.y) + ")";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(coordText);

            // Positioning logic to keep text on screen
            int textX = screenX + 10;
            int textY = screenY - 10;

            // Adjust if too close to right edge
            if (textX + textWidth > getWidth() - 5) {
                textX = screenX - textWidth - 10;
            }

            // Draw with a background for better visibility
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(textX - 2, textY - fm.getAscent(), textWidth + 4, fm.getHeight());
            g2d.setColor(Color.BLACK);
            g2d.drawString(coordText, textX, textY);
        }

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

    /**
     * Returns the index of the currently selected function
     */
    public int getSelectedFunctionIndex() {
        return selectedFunctionIndex;
    }

    /**
     * Sets the selected function index
     */
    public void setSelectedFunctionIndex(int index) {
        this.selectedFunctionIndex = index;
        repaint();
    }
}