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
import java.util.ArrayList;

/**
 * GraphPanel - Ein Panel zum Zeichnen von Funktionsgraphen in einem
 * Koordinatensystem
 */
public class GraphPanel extends JPanel {
    // Konstanten für die Anzeige
    public static final int AXIS_MARGIN = 40; // Abstand der Achsen vom Rand
    private static final float ZOOM_FACTOR = 1.2f; // Faktor für Zoom-Vorgänge
    public static final int MIN_HEIGHT = 200; // Minimale Höhe des Koordinatensystems
    public static final int MIN_WIDTH = 300; // Minimale Breite des Koordinatensystems
    private static final int INTERSECTION_HIT_RADIUS = 10; // Radius zur Erkennung von Mausbewegungen über
                                                           // Schnittpunkten
    private static final int HOVER_DETECTION_THRESHOLD = 5; // Pixelabstand für Hover-Erkennung

    // Hilfsklassen für verschiedene Aspekte des Panels
    private final CoordinateTransformer transformer;
    private final GridRenderer gridRenderer;
    private final FunctionRenderer functionRenderer;
    private final IntersectionCalculator intersectionCalculator;

    // Mausinteraktion
    private Point lastMousePos; // Letzte Mausposition (zum Schwenken)
    private boolean isDragging = false; // Wird aktuell gezogen?
    private Point currentMousePosition = null; // Aktuelle Mausposition für Hover-Erkennung
    private int closestFunctionIndex = -1; // Index der der Maus am nächsten liegenden Funktion
    private Point2D.Double closestPoint = null; // Nächster Punkt auf einer Funktion zur Maus
    private List<Integer> selectedFunctionIndices = new ArrayList<>(); // Indizes der ausgewählten Funktionen

    // Tooltip-Unterstützung
    private IntersectionPoint currentTooltipPoint = null;
    private DecimalFormat tooltipFormat = new DecimalFormat("0.########");

    // PropertyChange-Unterstützung für Benachrichtigungen
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // Flag zum Anzeigen/Ausblenden des Koordinatensystems
    private boolean showGrid = true;

    /**
     * Konstruktor - Initialisiert das Panel und fügt Maus-Listener hinzu
     */
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Double Buffering für flüssiges Zeichnen
        setDoubleBuffered(true);

        // Initialisiere Hilfsklassen
        transformer = new CoordinateTransformer(this);
        gridRenderer = new GridRenderer(this, transformer);
        functionRenderer = new FunctionRenderer(this, transformer);
        intersectionCalculator = new IntersectionCalculator(this, transformer, functionRenderer);

        // Aktiviere Tooltips
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(100); // Tooltip schneller anzeigen

        // Füge Maus- und Komponenten-Listener hinzu
        setupMouseListeners();
        setupComponentListeners();

        // Initialisiere die Ansicht basierend auf der aktuellen Größe
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
     * Richtet Maus-Listener für Zoom, Schwenken und Tooltips ein
     */
    private void setupMouseListeners() {
        // Mauslistener für Drücken und Loslassen
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;

                // Falls dies ein Klick war (keine signifikante Ziehbewegung), wähle die
                // Funktion aus
                if (e.getPoint().distance(lastMousePos) < 5) {
                    // Finde heraus, welche Funktion angeklickt wurde (falls vorhanden)
                    if (closestPoint != null && closestFunctionIndex >= 0) {
                        // Prüfe, ob STRG für Mehrfachauswahl gedrückt ist
                        if (e.isControlDown()) {
                            // Schalte die Auswahl dieser Funktion um
                            if (selectedFunctionIndices.contains(closestFunctionIndex)) {
                                selectedFunctionIndices.remove(Integer.valueOf(closestFunctionIndex));
                            } else {
                                selectedFunctionIndices.add(closestFunctionIndex);
                            }
                        } else {
                            // Einzelauswahl - lösche vorherige Auswahl und wähle nur diese Funktion aus
                            selectedFunctionIndices.clear();
                            selectedFunctionIndices.add(closestFunctionIndex);
                        }
                        repaint();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Falls auf leeren Raum geklickt wird und STRG nicht gedrückt ist, hebe alle
                // Funktionsauswahlen auf
                if (closestPoint == null && !e.isControlDown()) {
                    selectedFunctionIndices.clear();
                    repaint();
                }
            }
        });

        // Angepasster Mausbewegungs-Listener für Ziehen und Tooltips
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Berechne die Verschiebung in Bildschirmkoordinaten
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;

                    // Verschiebe die Ansicht
                    transformer.pan(dx, dy);
                    lastMousePos = e.getPoint();

                    // Neu zeichnen
                    repaint();

                    // Benachrichtige über die Änderung der Ansicht
                    fireViewChanged();

                    // Falls Schnittpunkte aktiviert sind, berechne sie neu
                    if (intersectionCalculator.isShowingIntersections()) {
                        intersectionCalculator.calculateIntersections();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Speichere die aktuelle Mausposition
                currentMousePosition = e.getPoint();

                // Finde den nächsten Punkt auf einer beliebigen Funktion
                findClosestPointOnFunction(currentMousePosition);

                // Überprüfe zunächst, ob die Maus über einem Schnittpunkt liegt
                if (intersectionCalculator.isShowingIntersections()) {
                    IntersectionPoint point = findIntersectionPointNear(e.getPoint());
                    if (point != currentTooltipPoint) {
                        currentTooltipPoint = point;
                        // Aktualisiere den Tooltip
                        setToolTipText(null); // Erzwinge, dass der Tooltip-Manager getToolTipText aufruft
                    }
                } else if (closestPoint != null) {
                    // Falls keine Schnittpunkte angezeigt werden, aber ein nächster Punkt vorhanden
                    // ist, aktualisiere den Tooltip
                    setToolTipText(null); // Erzwinge, dass der Tooltip-Manager getToolTipText aufruft
                }

                // Neu zeichnen, um den Hover-Marker anzuzeigen
                repaint();
            }
        });

        // Mausrad-Listener für Zoom
        addMouseWheelListener(e -> {
            // Speichere die ursprüngliche Mausposition in Bildschirmkoordinaten
            Point mousePoint = e.getPoint();

            // Zoomfaktor basierend auf der Richtung des Mausrads (umgekehrt)
            double factor = (e.getWheelRotation() > 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Zoomen
            transformer.zoom(factor, mousePoint);

            repaint();

            // Benachrichtige über die Änderung der Ansicht
            fireViewChanged();

            // Falls Schnittpunkte aktiviert sind, berechne sie neu
            if (intersectionCalculator.isShowingIntersections()) {
                intersectionCalculator.calculateIntersections();
            }
        });
    }

    /**
     * Richtet Komponenten-Listener für Größenänderungen ein
     */
    private void setupComponentListeners() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Bei Größenänderung, passe die Ansicht an, behalte jedoch das Zentrum bei
                transformer.adjustViewToMaintainAspectRatio();

                // Falls Schnittpunkte aktiviert sind, berechne sie neu
                if (intersectionCalculator.isShowingIntersections()) {
                    intersectionCalculator.calculateIntersections();
                }
            }
        });
    }

    /**
     * Findet den nächsten Punkt auf einer beliebigen Funktion zur Mausposition
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

        // Konvertiere die Mausposition in Weltkoordinaten
        double mouseWorldX = transformer.screenToWorldX(mousePos.x);

        // Suche in einem kleinen Bereich um die Maus-X-Position
        double searchStep = (transformer.getXMax() - transformer.getXMin()) / 100;
        double searchRange = searchStep * 3;

        // Für jede Funktion
        for (int funcIndex = 0; funcIndex < functions.size(); funcIndex++) {
            FunctionParser parser = functions.get(funcIndex).getFunction();

            // Suche den nächsten Punkt in einem Bereich um die Maus-X-Position
            for (double x = mouseWorldX - searchRange; x <= mouseWorldX + searchRange; x += searchStep) {
                try {
                    // Überspringe, falls x außerhalb der Ansicht liegt
                    if (x < transformer.getXMin() || x > transformer.getXMax())
                        continue;

                    double y = parser.evaluateAt(x);

                    // Überspringe, falls y außerhalb der Ansicht liegt oder keine gültige Zahl ist
                    if (Double.isNaN(y) || Double.isInfinite(y) ||
                            y < transformer.getYMin() || y > transformer.getYMax())
                        continue;

                    // Konvertiere in Bildschirmkoordinaten
                    int screenX = transformer.worldToScreenX(x);
                    int screenY = transformer.worldToScreenY(y);

                    // Berechne die Distanz zur Maus
                    double distance = mousePos.distance(screenX, screenY);

                    // Falls dieser Punkt näher ist als unser Schwellwert und der bisher
                    // nächstgelegene Punkt
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPoint = new Point2D.Double(x, y);
                        closestFunctionIndex = funcIndex;
                    }
                } catch (Exception e) {
                    // Überspringe Fehler bei der Funktionsauswertung
                    continue;
                }
            }
        }
    }

    /**
     * Findet den nächsten Schnittpunkt in der Nähe der Mausposition
     */
    private IntersectionPoint findIntersectionPointNear(Point mousePos) {
        if (!intersectionCalculator.isShowingIntersections()) {
            return null;
        }

        List<IntersectionPoint> points = intersectionCalculator.getIntersectionPoints();
        IntersectionPoint closest = null;
        double minDistance = INTERSECTION_HIT_RADIUS;

        for (IntersectionPoint point : points) {
            // Prüfe, ob der Punkt im sichtbaren Bereich liegt
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
     * Gibt den Tooltip-Text für die aktuelle Mausposition zurück
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        if (currentTooltipPoint != null) {
            // Zeige Tooltip für Schnittpunkt
            String func1 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(currentTooltipPoint.getFunctionIndex2());

            return "<html><b>Schnittpunkt</b><br>" +
                    "x = " + tooltipFormat.format(currentTooltipPoint.x) + "<br>" +
                    "y = " + tooltipFormat.format(currentTooltipPoint.y) + "<br>" +
                    "zwischen:<br>" +
                    "- " + func1 + "<br>" +
                    "- " + func2 + "</html>";
        } else if (closestPoint != null) {
            // Zeige Tooltip für Funktionspunkt
            String funcExpr = getFunctionExpressionByIndex(closestFunctionIndex);

            return "<html><b>Koordinate</b><br>" +
                    "x = " + tooltipFormat.format(closestPoint.x) + "<br>" +
                    "y = " + tooltipFormat.format(closestPoint.y) + "<br>" +
                    "Funktion: " + funcExpr + "</html>";
        }
        return null;
    }

    /**
     * Hilfsmethode, um einen Funktionsausdruck anhand seines Index zu erhalten
     */
    private String getFunctionExpressionByIndex(int index) {
        List<FunctionRenderer.FunctionInfo> functions = functionRenderer.getFunctions();
        if (index >= 0 && index < functions.size()) {
            return "f" + (index + 1) + "(x)";
        }
        return "f" + (index + 1) + "(x)";
    }

    /**
     * Fügt einen PropertyChangeListener hinzu
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Entfernt einen PropertyChangeListener
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Löst ein Ereignis für Ansichtsänderungen aus
     */
    public void fireViewChanged() {
        Point2D.Double oldCenter = null; // Wir benötigen den alten Wert nicht
        Point2D.Double newCenter = getViewCenter();
        pcs.firePropertyChange("viewChanged", oldCenter, newCenter);
    }

    /**
     * Löst ein Ereignis für Schnittpunkt-Aktualisierungen aus
     */
    public void fireIntersectionsUpdated(List<IntersectionPoint> oldIntersections,
            List<IntersectionPoint> newIntersections) {
        pcs.firePropertyChange("intersectionsUpdated", oldIntersections, newIntersections);
    }

    /**
     * Zeichnet das Panel inklusive Koordinatensystem und Funktionen
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Aktiviere Anti-Aliasing für glattere Linien
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Aktualisiere das Achsenformat basierend auf dem Zoomlevel
        transformer.updateAxisFormat();

        // Zeichne das Koordinatengitter und die Achsen nur, wenn aktiviert
        if (showGrid) {
            gridRenderer.drawGrid(g2d);
            gridRenderer.drawAxes(g2d);
        }

        // Zeichne die Funktionen
        functionRenderer.drawFunctions(g2d, selectedFunctionIndices);

        // Zeichne Schnittpunkte, falls aktiviert
        intersectionCalculator.drawIntersectionPoints(g2d);

        // Zeichne den Hover-Marker, falls ein nächster Punkt vorhanden ist
        if (closestPoint != null) {
            int screenX = transformer.worldToScreenX(closestPoint.x);
            int screenY = transformer.worldToScreenY(closestPoint.y);

            // Zeichne einen Kreis an der Stelle
            int markerSize = 8;
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(screenX - markerSize / 2, screenY - markerSize / 2, markerSize, markerSize);

            // Zeichne die Koordinaten in der Nähe des Punkts, falls nicht zu nah am Rand
            String coordText = "(" + tooltipFormat.format(closestPoint.x) +
                    ", " + tooltipFormat.format(closestPoint.y) + ")";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(coordText);

            // Positionierungslogik, um den Text auf dem Bildschirm zu halten
            int textX = screenX + 10;
            int textY = screenY - 10;

            // Anpassen, falls zu nah am rechten Rand
            if (textX + textWidth > getWidth() - 5) {
                textX = screenX - textWidth - 10;
            }

            // Zeichne mit einem Hintergrund für bessere Sichtbarkeit
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(textX - 2, textY - fm.getAscent(), textWidth + 4, fm.getHeight());
            g2d.setColor(Color.BLACK);
            g2d.drawString(coordText, textX, textY);
        }

        // Informationstext
        g2d.setColor(Color.BLACK);
        g2d.drawString("Zoom: Mausrad, Verschieben: Maus ziehen", 10, getHeight() - 10);
    }

    /**
     * Fügt dem Plotter eine neue Funktion hinzu
     */
    public void addFunction(String expression, Color color) {
        functionRenderer.addFunction(expression, color);

        if (intersectionCalculator.isShowingIntersections()) {
            intersectionCalculator.calculateIntersections();
        }

        repaint();
    }

    /**
     * Getter für den FunctionRenderer
     * Benötigt für direkten Zugriff auf die Funktionsliste
     * 
     * @return Der FunctionRenderer
     */
    public FunctionRenderer getFunctionRenderer() {
        return functionRenderer;
    }

    /**
     * Fügt dem Plotter eine neue Funktion hinzu und wählt sie aus
     */
    public void addFunctionAndSelect(String expression, Color color, boolean addToSelection) {
        int newIndex = functionRenderer.getFunctions().size();
        addFunction(expression, color);

        if (!addToSelection) {
            selectedFunctionIndices.clear();
        }
        selectedFunctionIndices.add(newIndex);

        repaint();
    }

    /**
     * Entfernt alle Funktionen
     */
    public void clearFunctions() {
        functionRenderer.clearFunctions();
        intersectionCalculator.getIntersectionPoints().clear();
        repaint();
    }

    /**
     * Gibt die Koordinaten des aktuellen Ansichts-Zentrums zurück
     */
    public Point2D.Double getViewCenter() {
        return transformer.getViewCenter();
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    public void resetView() {
        transformer.resetView();
        repaint();
        fireViewChanged();
    }

    /**
     * Zentriert die Ansicht auf den angegebenen Punkt
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
     * Schaltet die Anzeige der Schnittpunkte um
     */
    public void toggleIntersections(boolean show) {
        intersectionCalculator.toggleIntersections(show);
        repaint();
    }

    /**
     * Gibt die Liste der Schnittpunkte zurück
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionCalculator.getIntersectionPoints();
    }

    /**
     * Gibt die Liste der ausgewählten Funktionsindizes zurück
     */
    public List<Integer> getSelectedFunctionIndices() {
        return selectedFunctionIndices;
    }

    /**
     * Wählt eine Funktion mit dem angegebenen Index aus
     *
     * @param index          Der Index der auszuwählenden Funktion
     * @param addToSelection Wenn true, wird zur aktuellen Auswahl hinzugefügt; wenn
     *                       false,
     *                       wird sie ersetzt
     */
    public void selectFunction(int index, boolean addToSelection) {
        if (!addToSelection) {
            selectedFunctionIndices.clear();
        }

        if (index >= 0 && !selectedFunctionIndices.contains(index)) {
            selectedFunctionIndices.add(index);
        }

        repaint();
    }

    /**
     * Hebt die Auswahl der Funktion mit dem angegebenen Index auf
     */
    public void deselectFunction(int index) {
        selectedFunctionIndices.remove(Integer.valueOf(index));
        repaint();
    }

    /**
     * Löscht alle Funktionsauswahlen
     */
    public void clearFunctionSelection() {
        selectedFunctionIndices.clear();
        repaint();
    }
}
