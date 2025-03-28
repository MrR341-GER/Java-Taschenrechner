import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    // Darstellungsparameter
    private double xMin = -10; // Minimaler X-Wert im sichtbaren Bereich (wird dynamisch angepasst)
    private double xMax = 10; // Maximaler X-Wert im sichtbaren Bereich (wird dynamisch angepasst)
    private double yMin = -10; // Minimaler Y-Wert im sichtbaren Bereich (fest)
    private double yMax = 10; // Maximaler Y-Wert im sichtbaren Bereich (fest)
    private double xScale; // Skalierungsfaktor für X-Werte (Pixel pro Einheit)
    private double yScale; // Skalierungsfaktor für Y-Werte (Pixel pro Einheit)

    // Verschiebungen für zentriertes Koordinatensystem
    private int xOffset;
    private int yOffset;

    // Maus-Interaktion
    private Point lastMousePos; // Letzte Mausposition (für Pan)
    private boolean isDragging = false; // Wird gerade gezogen?

    // Funktionen, die gezeichnet werden sollen
    private List<FunctionInfo> functions = new ArrayList<>();

    // Formatter für die Achsenbeschriftung
    private final DecimalFormat axisFormat = new DecimalFormat("0.##");

    // Schnittpunkt-Funktionalität
    private boolean showIntersections = false; // Flag, ob Schnittpunkte angezeigt werden sollen
    private List<IntersectionPoint> intersectionPoints = new ArrayList<>(); // Liste der berechneten Schnittpunkte

    // Property Change Support für Benachrichtigungen
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Konstruktor - initialisiert das Panel und fügt Maus-Listener hinzu
     */
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));

        // Doppelpuffer für flüssiges Zeichnen aktivieren
        setDoubleBuffered(true);

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

                    lastMousePos = e.getPoint();

                    // Neu zeichnen
                    repaint();

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
            double centerY = (yMax + yMin) / 2;
            double aspectRatio = (double)(getWidth() - 2 * AXIS_MARGIN) / (getHeight() - 2 * AXIS_MARGIN);
            double newXRange = newYRange * aspectRatio;
            double centerX = (xMax + xMin) / 2;

            // Bestimme den Punkt, auf den gezoomt werden soll (Position des Mauszeigers)
            double relX = (worldMouseX - xMin) / oldXRange; // Position relativ zur Breite
            double relY = (worldMouseY - yMin) / oldYRange; // Position relativ zur Höhe

            // Berechne neue Grenzen, sodass der Mauspunkt seine relative Position behält
            xMin = worldMouseX - relX * newXRange;
            xMax = xMin + newXRange;
            yMin = worldMouseY - relY * newYRange;
            yMax = yMin + newYRange;

            repaint();

            // Wenn Schnittpunkte aktiviert sind, dynamisch neu berechnen
            if (showIntersections) {
                calculateIntersections();
            }
        });

        // ComponentListener hinzufügen, um auf Größenänderungen zu reagieren
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Beim ersten Anzeigen oder nach manueller Größenänderung des Panels
                // passen wir die Y-Werte automatisch an, um das Seitenverhältnis zu erhalten
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
     * Passt die X-Werte an, um das korrekte Seitenverhältnis basierend auf der Y-Achse zu erhalten
     */
    private void adjustViewToMaintainAspectRatio() {
        int width = getWidth() - 2 * AXIS_MARGIN;
        int height = getHeight() - 2 * AXIS_MARGIN;

        if (width <= 0 || height <= 0) return; // Verhindere Division durch Null

        double yRange = yMax - yMin;

        // Berechne das Seitenverhältnis des Panels
        double panelAspectRatio = (double) width / height;

        // Berechne den entsprechenden X-Bereich für das korrekte Seitenverhältnis
        double centerX = (xMax + xMin) / 2;
        double xRange = yRange * panelAspectRatio;

        // Setze neue X-Werte, zentriert um die aktuelle Mitte
        xMin = centerX - xRange / 2;
        xMax = centerX + xRange / 2;

        repaint();
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
        // Feste Y-Werte
        yMin = -10;
        yMax = 10;

        // X-Werte werden automatisch angepasst, um das korrekte Seitenverhältnis zu erhalten
        adjustViewToMaintainAspectRatio();
    }

    /**
     * Zentriert die Ansicht auf den angegebenen Punkt
     */
    public void centerViewAt(double xCenter, double yCenter) {
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

                // Funktionsausdrücke (versuchen, sie aus dem Funktionsobjekt zu extrahieren)
                String expr1 = "f" + (i+1);
                String expr2 = "f" + (j+1);

                // Suche Schnittpunkte im aktuellen Ansichtsfenster
                List<Point2D.Double> points = IntersectionFinder.findIntersections(
                        f1.function, f2.function, xMin, xMax);

                // Füge die gefundenen Schnittpunkte als IntersectionPoint-Objekte zur Gesamtliste hinzu
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
     * Gibt die Liste der aktuell berechneten Schnittpunkte zurück
     */
    public List<IntersectionPoint> getIntersectionPoints() {
        return intersectionPoints;
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

        // Verwende einheitliche Skalierung, um Verzerrung zu vermeiden
        yScale = height / yRange;
        xScale = yScale; // Wichtig: Wir verwenden den gleichen Skalierungsfaktor für beide Achsen

        // Da wir einheitliche Skalierung verwenden, werden die Offsets einfach auf die Ränder gesetzt
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

        // Berechne Gitterlinienabstände in Weltkoordinaten - verwende für beide Achsen den Y-Bereich
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
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, screenX - labelWidth / 2, yAxisPos + TICK_LENGTH + 12);
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
     * Zeichnet eine Funktion
     */
    private void drawFunction(Graphics2D g2d, FunctionInfo functionInfo) {
        g2d.setColor(functionInfo.color);
        g2d.setStroke(new BasicStroke(2f));

        Path2D path = new Path2D.Double();
        boolean pathStarted = false;

        // Anzahl der Punkte basierend auf der Bildschirmbreite
        int numPoints = getWidth() - 2 * AXIS_MARGIN;
        double step = (xMax - xMin) / numPoints;

        Double lastY = null;

        for (int i = 0; i <= numPoints; i++) {
            double x = xMin + i * step;

            try {
                double y = functionInfo.function.evaluateAt(x);

                // Prüfen ob der Wert gültig ist
                if (Double.isNaN(y) || Double.isInfinite(y)) {
                    // Ungültiger Punkt, neuen Pfad starten
                    pathStarted = false;
                    lastY = null;
                    continue;
                }

                // Sprunghafte Änderungen erkennen und Linien unterbrechen
                if (lastY != null) {
                    double deltaY = Math.abs(y - lastY);
                    double deltaYRelative = deltaY / (yMax - yMin);

                    // Wenn die relative Änderung zu groß ist, neuen Pfad starten
                    if (deltaYRelative > 0.2) {
                        pathStarted = false;
                    }
                }
                lastY = y;

                // Nur zeichnen, wenn im sichtbaren Bereich
                if (y >= yMin && y <= yMax) {
                    int screenX = worldToScreenX(x);
                    int screenY = worldToScreenY(y);

                    if (!pathStarted) {
                        path.moveTo(screenX, screenY);
                        pathStarted = true;
                    } else {
                        path.lineTo(screenX, screenY);
                    }
                } else {
                    // Punkt außerhalb des sichtbaren Bereichs
                    pathStarted = false;
                }
            } catch (Exception e) {
                // Bei Fehlern in der Auswertung den Pfad unterbrechen
                pathStarted = false;
            }
        }

        g2d.draw(path);
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
                g2d.fillOval(screenX - pointSize/2, screenY - pointSize/2,
                        pointSize, pointSize);

                // Zeichne die Koordinaten als Text daneben
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                DecimalFormat df = new DecimalFormat("0.##");
                String coords = "(" + df.format(point.x) + ", " + df.format(point.y) + ")";
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