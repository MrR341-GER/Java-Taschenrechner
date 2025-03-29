import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphPanel - Ein Panel zum Zeichnen von Funktionsgraphen in einem
 * Koordinatensystem
 */
public class GraphPanel extends JPanel {
    // Konstanten für die Darstellung
    private static final int AXIS_MARGIN = 40; // Abstand der Achsen vom Rand
    private static final int TICK_LENGTH = 5; // Länge der Achsenmarkierungen
    private static final int GRID_SPACING = 50; // Abstand der Gitterlinien in Pixeln
    private static final float ZOOM_FACTOR = 1.2f; // Faktor für Zoom-Operationen
    private static final double DEFAULT_VIEW_RANGE = 20.0; // Standardbereich (-10 bis +10)
    private static final double MIN_PIXELS_PER_UNIT = 10.0; // Mindestpixel pro Einheit für Lesbarkeit
    private static final int MIN_HEIGHT = 200; // Minimale Höhe des Koordinatensystems
    private static final int MIN_WIDTH = 300; // Minimale Breite des Koordinatensystems
    private static final double MAX_SCREEN_COORDS = 10000.0; // Maximale Bildschirmkoordinaten für Java2D

    // Darstellungsparameter
    private double xMin = -10; // Minimaler X-Wert im sichtbaren Bereich
    private double xMax = 10; // Maximaler X-Wert im sichtbaren Bereich
    private double yMin = -10; // Minimaler Y-Wert im sichtbaren Bereich
    private double yMax = 10; // Maximaler Y-Wert im sichtbaren Bereich
    private double xScale; // Skalierungsfaktor für X-Werte (Pixel pro Einheit)
    private double yScale; // Skalierungsfaktor für Y-Werte (Pixel pro Einheit)

    // Verschiebungen für zentriertes Koordinatensystem
    private int xOffset;
    private int yOffset;

    // Speichert das Zentrum, um es bei Größenänderungen beizubehalten
    private Point2D.Double viewCenter = new Point2D.Double(0, 0);

    // Maus-Interaktion
    private Point lastMousePos; // Letzte Mausposition (für Pan)
    private boolean isDragging = false; // Wird gerade gezogen?

    // Funktionen, die gezeichnet werden sollen
    private List<FunctionInfo> functions = new ArrayList<>();

    // Formatter für die Achsenbeschriftung - wird dynamisch aktualisiert
    private DecimalFormat axisFormat;

    // Schnittpunkt-Funktionalität
    private boolean showIntersections = false; // Flag, ob Schnittpunkte angezeigt werden sollen
    private List<IntersectionPoint> intersectionPoints = new ArrayList<>(); // Liste der berechneten Schnittpunkte

    // Property Change Support für Benachrichtigungen
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Gibt die Liste der aktuell berechneten Schnittpunkte zurück
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionPoints;
    }

    /**
     * Konstruktor - initialisiert das Panel und fügt Maus-Listener hinzu
     */
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Doppelpuffer für flüssiges Zeichnen aktivieren
        setDoubleBuffered(true);

        // Standardformatierung für die Achsenbeschriftung
        axisFormat = new DecimalFormat("0.##");

        // Maus-Listener für Zoom und Pan
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

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Berechne die Verschiebung in Weltkoordinaten
                    double dx = (e.getX() - lastMousePos.x) / xScale;
                    double dy = (e.getY() - lastMousePos.y) / yScale * -1; // Y-Achse invertiert

                    // Verschiebe die Ansicht
                    xMin -= dx;
                    xMax -= dx;
                    yMin -= dy;
                    yMax -= dy;

                    // Aktualisiere das Zentrum
                    updateViewCenter();

                    lastMousePos = e.getPoint();

                    // Neuzeichnen
                    repaint();

                    // Benachrichtige über die Änderung der Ansicht
                    fireViewChanged();

                    // Wenn Schnittpunkte aktiviert sind, dynamisch neu berechnen
                    if (showIntersections) {
                        calculateIntersections();
                    }
                }
            }
        });

        // Mouse-Wheel Listener für Zoom
        addMouseWheelListener(e -> {
            // Speichere die ursprüngliche Mausposition in Bildschirmkoordinaten
            Point mousePoint = e.getPoint();

            // Speichere die ursprüngliche Mausposition in Weltkoordinaten
            double worldMouseX = screenToWorldX(mousePoint.x);
            double worldMouseY = screenToWorldY(mousePoint.y);

            // Zoom-Faktor basierend auf Mausrad-Richtung (umgekehrt)
            double factor = (e.getWheelRotation() > 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Aktuellen Bereich speichern
            double oldYRange = yMax - yMin;
            double oldXRange = xMax - xMin;

            // Bereiche anpassen
            double newYRange = oldYRange * factor;
            double newXRange = oldXRange * factor;

            // Bestimme den Punkt, auf den gezoomt werden soll (Position des Mauszeigers)
            double relX = (worldMouseX - xMin) / oldXRange; // Position relativ zur Breite
            double relY = (worldMouseY - yMin) / oldYRange; // Position relativ zur Höhe

            // Berechne neue Grenzen, sodass der Mauspunkt seine relative Position behält
            xMin = worldMouseX - relX * newXRange;
            xMax = xMin + newXRange;
            yMin = worldMouseY - relY * newYRange;
            yMax = yMin + newYRange;

            // Aktualisiere das Zentrum
            updateViewCenter();

            repaint();

            // Benachrichtige über die Änderung der Ansicht
            fireViewChanged();

            // Wenn Schnittpunkte aktiviert sind, dynamisch neu berechnen
            if (showIntersections) {
                calculateIntersections();
            }
        });

        // ComponentListener hinzufügen, um auf Größenänderungen zu reagieren
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Bei Größenänderung die Ansicht anpassen, aber das Zentrum beibehalten
                adjustViewToMaintainAspectRatio();

                // Wenn Schnittpunkte aktiviert sind, dynamisch neu berechnen
                if (showIntersections) {
                    calculateIntersections();
                }
            }
        });

        // Initialisiere die Ansicht basierend auf der aktuellen Größe
        resetView();
    }

    /**
     * Aktualisiert das gespeicherte Zentrum der Ansicht
     */
    private void updateViewCenter() {
        viewCenter.x = (xMax + xMin) / 2;
        viewCenter.y = (yMax + yMin) / 2;
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
     * Passt die Ansicht an, um das korrekte Seitenverhältnis basierend auf der
     * Panelgröße zu erhalten, und stellt sicher, dass das Koordinatensystem lesbar
     * bleibt
     */
    private void adjustViewToMaintainAspectRatio() {
        int width = getWidth() - 2 * AXIS_MARGIN;
        int height = getHeight() - 2 * AXIS_MARGIN;

        if (width <= 0 || height <= 0)
            return; // Verhindere Division durch Null

        // Berechne das Seitenverhältnis des Panels
        double panelAspectRatio = (double) width / height;

        // Berechne die verfügbaren Pixel pro Einheit
        double pixelsPerUnitX = width / (xMax - xMin);
        double pixelsPerUnitY = height / (yMax - yMin);

        // Wenn einer der Werte zu niedrig wird, passe beide Bereiche an
        if (pixelsPerUnitX < MIN_PIXELS_PER_UNIT || pixelsPerUnitY < MIN_PIXELS_PER_UNIT) {
            // Bestimme, wie viele Einheiten in jede Richtung basierend auf der
            // Mindestlesbarkeit dargestellt werden können
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Stelle sicher, dass das Seitenverhältnis erhalten bleibt
            if (maxUnitsX / maxUnitsY < panelAspectRatio) {
                // X-Richtung ist beschränkend
                maxUnitsY = maxUnitsX / panelAspectRatio;
            } else {
                // Y-Richtung ist beschränkend
                maxUnitsX = maxUnitsY * panelAspectRatio;
            }

            // Berechne neue Grenzen basierend auf dem gespeicherten Zentrum
            double halfX = maxUnitsX / 2;
            double halfY = maxUnitsY / 2;
            xMin = viewCenter.x - halfX;
            xMax = viewCenter.x + halfX;
            yMin = viewCenter.y - halfY;
            yMax = viewCenter.y + halfY;
        } else {
            // Wenn die Lesbarkeit gewährleistet ist, passe nur die Grenzen an, um das
            // Seitenverhältnis zu erhalten
            double xRange = xMax - xMin;
            double yRange = yMax - yMin;
            double currentAspectRatio = xRange / yRange;

            if (Math.abs(currentAspectRatio - panelAspectRatio) > 0.01) { // Toleranz für Fließkommavergleich
                if (currentAspectRatio < panelAspectRatio) {
                    // X-Bereich muss vergrößert werden
                    double newXRange = yRange * panelAspectRatio;
                    double halfDeltaX = (newXRange - xRange) / 2;
                    xMin -= halfDeltaX;
                    xMax += halfDeltaX;
                } else {
                    // Y-Bereich muss vergrößert werden
                    double newYRange = xRange / panelAspectRatio;
                    double halfDeltaY = (newYRange - yRange) / 2;
                    yMin -= halfDeltaY;
                    yMax += halfDeltaY;
                }

                // Aktualisiere das Zentrum
                updateViewCenter();
            }
        }

        repaint();
    }

    /**
     * Bestimmt die passende Anzahl der Nachkommastellen basierend auf dem
     * Zoom-Level
     */
    private void updateAxisFormat() {
        // Berechne den Wertebereich (kleinere Wertebereiche = mehr Nachkommastellen)
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Verwende den kleineren Bereich für die Formatierung
        double range = Math.min(xRange, yRange);

        // Logarithmische Skalierung für die Anzahl der Nachkommastellen
        int decimalPlaces = 2; // Minimum 2 Nachkommastellen

        if (range < 0.1) {
            decimalPlaces = 5;
        } else if (range < 1) {
            decimalPlaces = 4;
        } else if (range < 10) {
            decimalPlaces = 3;
        }

        // Formatstring mit variabler Anzahl von Nachkommastellen erstellen
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("#");
        }

        // Aktualisiere das Format
        axisFormat = new DecimalFormat(pattern.toString());
    }

    /**
     * Fügt eine neue Funktion zum Plotter hinzu
     */
    public void addFunction(String expression, Color color) {
        FunctionParser parser = new FunctionParser(expression);
        functions.add(new FunctionInfo(parser, color));

        if (showIntersections) {
            calculateIntersections();
        }

        repaint();
    }

    /**
     * Entfernt alle Funktionen
     */
    public void clearFunctions() {
        functions.clear();
        intersectionPoints.clear();
        repaint();
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    public void resetView() {
        // Setze das Zentrum auf den Ursprung
        viewCenter.x = 0;
        viewCenter.y = 0;

        // Berechne eine passende Ansicht basierend auf der Fenstergröße
        int width = getWidth() - 2 * AXIS_MARGIN;
        int height = getHeight() - 2 * AXIS_MARGIN;

        if (width <= 0 || height <= 0) {
            // Wenn das Panel noch keine Größe hat, verwende Standardwerte
            xMin = -10;
            xMax = 10;
            yMin = -10;
            yMax = 10;
        } else {
            // Berechne, wie viele Einheiten bei Mindestlesbarkeit dargestellt werden können
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Verwende das Minimum oder die Standardansicht
            double halfX = Math.min(maxUnitsX, DEFAULT_VIEW_RANGE) / 2;
            double halfY = Math.min(maxUnitsY, DEFAULT_VIEW_RANGE) / 2;

            // Stelle sicher, dass das Seitenverhältnis erhalten bleibt
            double panelAspectRatio = (double) width / height;
            if (halfX / halfY < panelAspectRatio) {
                halfY = halfX / panelAspectRatio;
            } else {
                halfX = halfY * panelAspectRatio;
            }

            // Setze die Grenzen
            xMin = -halfX;
            xMax = halfX;
            yMin = -halfY;
            yMax = halfY;
        }

        // Stelle sicher, dass die Ansicht richtig angepasst wird
        adjustViewToMaintainAspectRatio();

        // Neuzeichnen
        repaint();

        // Benachrichtige über die Änderung der Ansicht
        fireViewChanged();
    }

    /**
     * Zentriert die Ansicht auf den angegebenen Punkt
     */
    public void centerViewAt(double xCenter, double yCenter) {
        // Speichere das neue Zentrum
        viewCenter.x = xCenter;
        viewCenter.y = yCenter;

        // Berechne den aktuellen Bereich
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Setze neue Grenzen um den Zielpunkt herum
        xMin = xCenter - xRange / 2;
        xMax = xCenter + xRange / 2;
        yMin = yCenter - yRange / 2;
        yMax = yCenter + yRange / 2;

        // Neuzeichnen
        repaint();

        // Benachrichtige über die Änderung der Ansicht
        fireViewChanged();

        // Wenn Schnittpunkte aktiviert sind, neu berechnen
        if (showIntersections) {
            calculateIntersections();
        }
    }

    /**
     * Konvertiert eine X-Bildschirmkoordinate in eine X-Weltkoordinate
     */
    private double screenToWorldX(int screenX) {
        return xMin + (screenX - xOffset) / xScale;
    }

    /**
     * Konvertiert eine Y-Bildschirmkoordinate in eine Y-Weltkoordinate
     */
    private double screenToWorldY(int screenY) {
        return yMax - (screenY - yOffset) / yScale;
    }

    /**
     * Konvertiert eine X-Weltkoordinate in eine X-Bildschirmkoordinate
     */
    private int worldToScreenX(double worldX) {
        return (int) (xOffset + (worldX - xMin) * xScale);
    }

    /**
     * Konvertiert eine Y-Weltkoordinate in eine Y-Bildschirmkoordinate
     */
    private int worldToScreenY(double worldY) {
        return (int) (yOffset + (yMax - worldY) * yScale);
    }

    /**
     * Schaltet die Anzeige von Schnittpunkten ein oder aus
     */
    public void toggleIntersections(boolean show) {
        this.showIntersections = show;

        if (show) {
            calculateIntersections();
        } else {
            intersectionPoints.clear();
        }

        repaint();
    }

    /**
     * Berechnet alle Schnittpunkte zwischen den gezeichneten Funktionen
     */
    private void calculateIntersections() {
        List<IntersectionPoint> oldIntersections = new ArrayList<>(intersectionPoints);
        intersectionPoints.clear();

        // Wir brauchen mindestens zwei Funktionen für Schnittpunkte
        if (functions.size() < 2) {
            // Nur ein Event feuern, wenn sich etwas geändert hat
            if (!oldIntersections.isEmpty()) {
                pcs.firePropertyChange("intersectionsUpdated", oldIntersections, intersectionPoints);
            }
            return;
        }

        // Berechne Schnittpunkte für alle Funktionspaare
        for (int i = 0; i < functions.size() - 1; i++) {
            for (int j = i + 1; j < functions.size(); j++) {
                FunctionInfo f1 = functions.get(i);
                FunctionInfo f2 = functions.get(j);

                // Prüfe, ob die Funktionen identisch sind
                if (areFunctionsIdentical(f1.function, f2.function)) {
                    continue; // Überspringe identische Funktionen
                }

                // Funktionsausdrücke (versuchen, sie aus dem Funktionsobjekt zu extrahieren)
                String expr1 = "f" + (i + 1);
                String expr2 = "f" + (j + 1);

                // Suche Schnittpunkte im aktuellen Ansichtsfenster
                List<Point2D.Double> points = IntersectionFinder.findIntersections(
                        f1.function, f2.function, xMin, xMax);

                // Füge die gefundenen Schnittpunkte als IntersectionPoint-Objekte zur
                // Gesamtliste hinzu
                for (Point2D.Double point : points) {
                    IntersectionPoint ip = new IntersectionPoint(
                            point.x, point.y, i, j, expr1, expr2);

                    // Prüfe auf Duplikate
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

        // Event feuern, falls sich die Schnittpunkte geändert haben
        boolean changed = oldIntersections.size() != intersectionPoints.size();
        if (!changed) {
            // Prüfe auf unterschiedliche Punkte
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
            pcs.firePropertyChange("intersectionsUpdated", oldIntersections, intersectionPoints);
        }
    }

    /**
     * Prüft, ob zwei Funktionen identisch sind, indem mehrere Stichproben
     * verglichen werden
     */
    private boolean areFunctionsIdentical(FunctionParser f1, FunctionParser f2) {
        // Anzahl der Testpunkte
        final int NUM_TEST_POINTS = 10;

        // Bereich für die Testpunkte (aktueller sichtbarer Bereich)
        double min = xMin;
        double max = xMax;
        double step = (max - min) / (NUM_TEST_POINTS - 1);

        // Teste mehrere Punkte im aktuellen Bereich
        for (int i = 0; i < NUM_TEST_POINTS; i++) {
            double x = min + i * step;
            try {
                double y1 = f1.evaluateAt(x);
                double y2 = f2.evaluateAt(x);

                // Wenn ein y-Wert NaN oder unendlich ist, überspringe diesen Punkt
                if (Double.isNaN(y1) || Double.isInfinite(y1) ||
                        Double.isNaN(y2) || Double.isInfinite(y2)) {
                    continue;
                }

                // Wenn y-Werte unterschiedlich sind, sind die Funktionen nicht identisch
                if (Math.abs(y1 - y2) > 1e-10) {
                    return false;
                }
            } catch (Exception e) {
                // Bei Fehlern in der Auswertung gilt einer der Punkte als unterschiedlich
                return false;
            }
        }

        // Wenn alle Stichproben identisch sind, gehen wir davon aus, dass die
        // Funktionen gleich sind
        return true;
    }

    /**
     * Gibt die Koordinaten der aktuellen Bildmitte zurück
     */
    public Point2D.Double getViewCenter() {
        return new Point2D.Double(viewCenter.x, viewCenter.y);
    }

    /**
     * Benachrichtigt Listener über Änderungen der Ansicht
     */
    private void fireViewChanged() {
        Point2D.Double oldCenter = null; // Wir brauchen nicht den alten Wert
        Point2D.Double newCenter = getViewCenter();
        pcs.firePropertyChange("viewChanged", oldCenter, newCenter);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Anti-Aliasing für glattere Linien aktivieren
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Berechne die Skalierungsfaktoren
        int width = getWidth() - 2 * AXIS_MARGIN;
        int height = getHeight() - 2 * AXIS_MARGIN;

        // Berechne den Bereich in X- und Y-Richtung
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Aktualisiere das Format für die Achsenbeschriftung basierend auf dem
        // Zoom-Level
        updateAxisFormat();

        // Berechne Skalierungsfaktoren
        xScale = width / xRange;
        yScale = height / yRange;

        // Da wir einheitliche Skalierung verwenden, werden die Offsets einfach auf die
        // Ränder gesetzt
        xOffset = AXIS_MARGIN;
        yOffset = AXIS_MARGIN;

        // Koordinatengitter zeichnen
        drawGrid(g2d);

        // Achsen zeichnen
        drawAxes(g2d);

        // Funktionen zeichnen
        for (FunctionInfo function : functions) {
            drawFunction(g2d, function);
        }

        // Schnittpunkte zeichnen, wenn aktiviert
        if (showIntersections) {
            drawIntersectionPoints(g2d);
        }

        // Info-Text
        g2d.setColor(Color.BLACK);
        g2d.drawString("Zoom: Mausrad, Verschieben: Maus ziehen", 10, getHeight() - 10);
    }

    /**
     * Zeichnet das Koordinatengitter
     */
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // Hellgrau
        g2d.setStroke(new BasicStroke(0.5f));

        // Berechne Gitterlinienabstände in Weltkoordinaten - verwende für beide Achsen
        // den Y-Bereich
        double gridSpacing = calculateGridSpacing(yMax - yMin);

        // Verfügbarer Zeichenbereich
        int drawingWidth = getWidth() - 2 * AXIS_MARGIN;
        int drawingHeight = getHeight() - 2 * AXIS_MARGIN;

        // X-Gitterlinien
        double x = Math.ceil(xMin / gridSpacing) * gridSpacing;
        while (x <= xMax) {
            int screenX = worldToScreenX(x);
            g2d.draw(new Line2D.Double(screenX, yOffset, screenX, yOffset + drawingHeight));
            x += gridSpacing;
        }

        // Y-Gitterlinien
        double y = Math.ceil(yMin / gridSpacing) * gridSpacing;
        while (y <= yMax) {
            int screenY = worldToScreenY(y);
            g2d.draw(new Line2D.Double(xOffset, screenY, xOffset + drawingWidth, screenY));
            y += gridSpacing;
        }
    }

    /**
     * Berechnet einen geeigneten Abstand für Gitterlinien
     */
    private double calculateGridSpacing(double range) {
        // Ziel: Etwa 10 Gitterlinien im sichtbaren Bereich
        double rawSpacing = range / 10;

        // Normalisiere auf Zehnerpotenzen
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Runde auf "schöne" Werte: 1, 2, 5, 10
        if (mantissa < 1.5)
            return Math.pow(10, exponent);
        else if (mantissa < 3.5)
            return 2 * Math.pow(10, exponent);
        else if (mantissa < 7.5)
            return 5 * Math.pow(10, exponent);
        else
            return 10 * Math.pow(10, exponent);
    }

    /**
     * Zeichnet die X- und Y-Achsen mit Beschriftung
     */
    private void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));

        // Verfügbarer Zeichenbereich
        int drawingWidth = getWidth() - 2 * AXIS_MARGIN;
        int drawingHeight = getHeight() - 2 * AXIS_MARGIN;

        // X-Achse
        int yAxisPos = worldToScreenY(0);
        if (yAxisPos < yOffset)
            yAxisPos = yOffset;
        if (yAxisPos > yOffset + drawingHeight)
            yAxisPos = yOffset + drawingHeight;

        g2d.draw(new Line2D.Double(xOffset, yAxisPos, xOffset + drawingWidth, yAxisPos));

        // Y-Achse
        int xAxisPos = worldToScreenX(0);
        if (xAxisPos < xOffset)
            xAxisPos = xOffset;
        if (xAxisPos > xOffset + drawingWidth)
            xAxisPos = xOffset + drawingWidth;

        g2d.draw(new Line2D.Double(xAxisPos, yOffset, xAxisPos, yOffset + drawingHeight));

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // Gemeinsamer Abstand für beide Achsen
        double gridSpacing = calculateGridSpacing(yMax - yMin);

        // X-Achse Markierungen und Beschriftungen
        double x = Math.ceil(xMin / gridSpacing) * gridSpacing;

        while (x <= xMax) {
            int screenX = worldToScreenX(x);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenX >= xOffset && screenX <= xOffset + drawingWidth) {
                // Markierung
                g2d.draw(new Line2D.Double(screenX, yAxisPos - TICK_LENGTH, screenX, yAxisPos + TICK_LENGTH));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(x) > 1e-10) { // Kleine Werte als Null betrachten
                    String label = axisFormat.format(x);
                    FontMetrics fm = g2d.getFontMetrics();

                    // Speichere den aktuellen Transformationszustand
                    AffineTransform originalTransform = g2d.getTransform();

                    // Konstanter Abstand für alle Beschriftungen
                    int yOffset = 5; // Abstand vom Tick-Mark

                    // Positioniere den Text so, dass er UNTERHALB der Achse beginnt
                    g2d.translate(screenX, yAxisPos + TICK_LENGTH + yOffset);
                    g2d.rotate(Math.PI / 2); // 90 Grad im Uhrzeigersinn

                    // Zeichne den Text zentriert zur Linie
                    g2d.drawString(label, 0, 0);

                    // Stelle den ursprünglichen Transformationszustand wieder her
                    g2d.setTransform(originalTransform);
                }
            }
            x += gridSpacing;
        }

        // Y-Achse Markierungen und Beschriftungen
        double y = Math.ceil(yMin / gridSpacing) * gridSpacing;

        while (y <= yMax) {
            int screenY = worldToScreenY(y);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenY >= yOffset && screenY <= yOffset + drawingHeight) {
                // Markierung
                g2d.draw(new Line2D.Double(xAxisPos - TICK_LENGTH, screenY, xAxisPos + TICK_LENGTH, screenY));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(y) > 1e-10) { // Kleine Werte als Null betrachten
                    String label = axisFormat.format(y);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, xAxisPos - labelWidth - 5, screenY + 4);
                }
            }
            y += gridSpacing;
        }

        // Ursprungsbeschriftung
        if (xAxisPos >= xOffset && xAxisPos <= xOffset + drawingWidth &&
                yAxisPos >= yOffset && yAxisPos <= yOffset + drawingHeight) {
            g2d.drawString("0", xAxisPos + 4, yAxisPos + 12);
        }

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("x", xOffset + drawingWidth + 10, yAxisPos + 4);
        g2d.drawString("y", xAxisPos - 4, yOffset - 10);
    }

    /**
     * Zeichnet eine Funktion mit verbesserter Genauigkeit
     */
    private void drawFunction(Graphics2D g2d, FunctionInfo functionInfo) {
        g2d.setColor(functionInfo.color);
        g2d.setStroke(new BasicStroke(2f));

        Path2D path = new Path2D.Double();
        boolean pathStarted = false;

        // Anzahl der Punkte basierend auf der Bildschirmbreite
        int numPoints = getWidth() - 2 * AXIS_MARGIN;
        double step = (xMax - xMin) / numPoints;

        // Speichern des letzten gültigen Punktes, um Linien an den Rändern korrekt zu
        // zeichnen
        double lastX = Double.NaN;
        double lastY = Double.NaN;

        for (int i = 0; i <= numPoints; i++) {
            double x = xMin + i * step;

            try {
                double y = functionInfo.function.evaluateAt(x);

                // Prüfen ob der Wert gültig ist
                if (Double.isNaN(y) || Double.isInfinite(y)) {
                    // Ungültiger Punkt, neuen Pfad starten
                    if (pathStarted && !Double.isNaN(lastX) && !Double.isNaN(lastY)) {
                        // Wenn wir einen gültigen letzten Punkt haben, zeichne bis zur Grenze
                        if (lastY < yMin) {
                            // Berechne den Schnittpunkt mit der unteren Grenze
                            double intersectX = calculateXIntersection(lastX, lastY, x, y, yMin);
                            if (!Double.isNaN(intersectX)) {
                                int screenX = worldToScreenX(intersectX);
                                int screenY = worldToScreenY(yMin);
                                path.lineTo(screenX, screenY);
                            }
                        } else if (lastY > yMax) {
                            // Berechne den Schnittpunkt mit der oberen Grenze
                            double intersectX = calculateXIntersection(lastX, lastY, x, y, yMax);
                            if (!Double.isNaN(intersectX)) {
                                int screenX = worldToScreenX(intersectX);
                                int screenY = worldToScreenY(yMax);
                                path.lineTo(screenX, screenY);
                            }
                        }
                    }

                    pathStarted = false;
                    lastX = Double.NaN;
                    lastY = Double.NaN;
                    continue;
                }

                // Speichere den aktuellen Punkt als letzten gültigen Punkt
                lastX = x;
                lastY = y;

                // Prüfe, ob der Punkt innerhalb des sichtbaren y-Bereichs liegt
                boolean inYRange = (y >= yMin && y <= yMax);

                // Berechne die Bildschirmkoordinaten
                int screenX = worldToScreenX(x);
                int screenY;

                if (inYRange) {
                    // Normaler Fall: Punkt im sichtbaren Bereich
                    screenY = worldToScreenY(y);
                } else {
                    // Punkt außerhalb des sichtbaren Bereichs, aber wir wollen den Pfad nicht
                    // unterbrechen
                    // Stattdessen begrenzen wir den y-Wert, um übermäßige Bildschirmkoordinaten zu
                    // vermeiden
                    if (y < yMin) {
                        screenY = worldToScreenY(yMin);
                    } else { // y > yMax
                        screenY = worldToScreenY(yMax);
                    }
                }

                // Prüfe, ob der vorherige Punkt außerhalb des Bereichs war
                if (!pathStarted) {
                    if (i > 0 && inYRange) {
                        // Wenn wir einen Pfad starten und der vorherige Punkt außerhalb war,
                        // berechne den Schnittpunkt mit dem Rand
                        double prevX = xMin + (i - 1) * step;
                        try {
                            double prevY = functionInfo.function.evaluateAt(prevX);

                            if (!Double.isNaN(prevY) && !Double.isInfinite(prevY)) {
                                // Berechne den Schnittpunkt mit der Grenze
                                if (prevY < yMin) {
                                    // Schnittpunkt mit unterer Grenze
                                    double intersectX = calculateXIntersection(prevX, prevY, x, y, yMin);
                                    if (!Double.isNaN(intersectX)) {
                                        int startX = worldToScreenX(intersectX);
                                        int startY = worldToScreenY(yMin);
                                        path.moveTo(startX, startY);
                                        path.lineTo(screenX, screenY);
                                        pathStarted = true;
                                        continue;
                                    }
                                } else if (prevY > yMax) {
                                    // Schnittpunkt mit oberer Grenze
                                    double intersectX = calculateXIntersection(prevX, prevY, x, y, yMax);
                                    if (!Double.isNaN(intersectX)) {
                                        int startX = worldToScreenX(intersectX);
                                        int startY = worldToScreenY(yMax);
                                        path.moveTo(startX, startY);
                                        path.lineTo(screenX, screenY);
                                        pathStarted = true;
                                        continue;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Fehler bei der Berechnung des vorherigen Punktes, ignorieren
                        }
                    }

                    path.moveTo(screenX, screenY);
                    pathStarted = true;
                } else {
                    // Wenn der aktuelle Punkt außerhalb des Bereichs ist, berechne den Schnittpunkt
                    if (!inYRange) {
                        if (i > 0) {
                            double prevX = xMin + (i - 1) * step;
                            try {
                                double prevY = functionInfo.function.evaluateAt(prevX);

                                if (!Double.isNaN(prevY) && !Double.isInfinite(prevY) &&
                                        prevY >= yMin && prevY <= yMax) {

                                    // Der vorherige Punkt war im Bereich, berechne den Schnittpunkt
                                    if (y < yMin) {
                                        // Schnittpunkt mit unterer Grenze
                                        double intersectX = calculateXIntersection(prevX, prevY, x, y, yMin);
                                        if (!Double.isNaN(intersectX)) {
                                            int endX = worldToScreenX(intersectX);
                                            int endY = worldToScreenY(yMin);
                                            path.lineTo(endX, endY);
                                        }
                                    } else if (y > yMax) {
                                        // Schnittpunkt mit oberer Grenze
                                        double intersectX = calculateXIntersection(prevX, prevY, x, y, yMax);
                                        if (!Double.isNaN(intersectX)) {
                                            int endX = worldToScreenX(intersectX);
                                            int endY = worldToScreenY(yMax);
                                            path.lineTo(endX, endY);
                                        }
                                    }

                                    // Unterbreche den Pfad für den Teil außerhalb des Bereichs
                                    pathStarted = false;
                                    continue;
                                }
                            } catch (Exception e) {
                                // Fehler bei der Berechnung des vorherigen Punktes, ignorieren
                            }
                        }
                    }

                    // Normaler Fall: lineTo für Punkte innerhalb des Bereichs
                    path.lineTo(screenX, screenY);
                }
            } catch (Exception e) {
                // Bei Fehlern in der Auswertung den Pfad unterbrechen
                pathStarted = false;
                lastX = Double.NaN;
                lastY = Double.NaN;
            }
        }

        g2d.draw(path);
    }

    /**
     * Berechnet den x-Wert an dem eine Linie eine bestimmte y-Höhe schneidet
     */
    private double calculateXIntersection(double x1, double y1, double x2, double y2, double targetY) {
        // Überprüfe, ob die Linie die Zielhöhe schneidet
        if ((y1 < targetY && y2 < targetY) || (y1 > targetY && y2 > targetY)) {
            return Double.NaN; // Kein Schnittpunkt
        }

        // Wenn einer der Punkte genau auf der Zielhöhe liegt, gib diesen x-Wert zurück
        if (Math.abs(y1 - targetY) < 1e-10)
            return x1;
        if (Math.abs(y2 - targetY) < 1e-10)
            return x2;

        // Lineare Interpolation: x = x1 + (targetY - y1) * (x2 - x1) / (y2 - y1)
        return x1 + (targetY - y1) * (x2 - x1) / (y2 - y1);
    }

    /**
     * Zeichnet die Schnittpunkte zwischen Funktionen
     */
    private void drawIntersectionPoints(Graphics2D g2d) {
        if (intersectionPoints.isEmpty()) {
            return;
        }

        // Einstellungen für Schnittpunkte
        g2d.setColor(Color.RED);
        int pointSize = 8;

        // Zeichne jeden Schnittpunkt als kleinen ausgefüllten Kreis
        for (IntersectionPoint point : intersectionPoints) {
            // Prüfe, ob der Punkt im sichtbaren Bereich liegt
            if (point.x >= xMin && point.x <= xMax &&
                    point.y >= yMin && point.y <= yMax) {

                int screenX = worldToScreenX(point.x);
                int screenY = worldToScreenY(point.y);

                // Zeichne einen ausgefüllten Kreis
                g2d.fillOval(screenX - pointSize / 2, screenY - pointSize / 2,
                        pointSize, pointSize);

                // Zeichne die Koordinaten als Text daneben mit dynamischer Präzision
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String coords = "(" + axisFormat.format(point.x) + ", " + axisFormat.format(point.y) + ")";
                g2d.drawString(coords, screenX + pointSize, screenY);
            }
        }
    }

    /**
     * Klasse zur Speicherung von Funktionsinformationen
     */
    private static class FunctionInfo {
        final FunctionParser function;
        final Color color;

        FunctionInfo(FunctionParser function, Color color) {
            this.function = function;
            this.color = color;
        }
    }
}