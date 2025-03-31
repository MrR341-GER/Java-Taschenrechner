package plugins.plotter2d;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;

/**
 * Verwaltet Koordinatentransformationen und die Ansichtssteuerung für das
 * GraphPanel
 */
public class CoordinateTransformer {
    // Konstanten für die Anzeige
    private static final double DEFAULT_VIEW_RANGE = 20.0; // Standardbereich (-10 bis +10)
    private static final double MIN_PIXELS_PER_UNIT = 10.0; // Mindestanzahl an Pixeln pro Einheit für gute Lesbarkeit

    // Anzeigeparameter
    private double xMin = -10; // Minimaler X-Wert im sichtbaren Bereich
    private double xMax = 10; // Maximaler X-Wert im sichtbaren Bereich
    private double yMin = -10; // Minimaler Y-Wert im sichtbaren Bereich
    private double yMax = 10; // Maximaler Y-Wert im sichtbaren Bereich
    private double xScale; // Skalierungsfaktor für X-Werte (Pixel pro Einheit)
    private double yScale; // Skalierungsfaktor für Y-Werte (Pixel pro Einheit)

    // Versätze für ein zentriertes Koordinatensystem
    private int xOffset;
    private int yOffset;

    // Speichert das Zentrum, um es während Größenänderungen beizubehalten
    private Point2D.Double viewCenter = new Point2D.Double(0, 0);

    // Formatierer für Achsenbeschriftungen – wird dynamisch aktualisiert
    private DecimalFormat axisFormat;

    // Referenz zu den Dimensionen des Panels
    private final GraphPanel panel;

    public CoordinateTransformer(GraphPanel panel) {
        this.panel = panel;
        axisFormat = new DecimalFormat("0.##");
    }

    /**
     * Aktualisiert das gespeicherte Zentrum der Ansicht
     */
    public void updateViewCenter() {
        viewCenter.x = (xMax + xMin) / 2;
        viewCenter.y = (yMax + yMin) / 2;
    }

    /**
     * Passt die Ansicht an, um basierend auf der Panelgröße das korrekte
     * Seitenverhältnis beizubehalten
     * und sicherzustellen, dass das Koordinatensystem gut lesbar bleibt.
     */
    public void adjustViewToMaintainAspectRatio() {
        int width = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int height = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        if (width <= 0 || height <= 0)
            return; // Division durch Null verhindern

        // Berechne das Seitenverhältnis des Panels
        double panelAspectRatio = (double) width / height;

        // Berechne die verfügbaren Pixel pro Einheit
        double pixelsPerUnitX = width / (xMax - xMin);
        double pixelsPerUnitY = height / (yMax - yMin);

        // Falls einer der Werte zu niedrig ist, passe beide Bereiche an
        if (pixelsPerUnitX < MIN_PIXELS_PER_UNIT || pixelsPerUnitY < MIN_PIXELS_PER_UNIT) {
            // Bestimme, wie viele Einheiten in jeder Richtung bei Mindestlesbarkeit
            // angezeigt werden können
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Stelle sicher, dass das Seitenverhältnis beibehalten wird
            if (maxUnitsX / maxUnitsY < panelAspectRatio) {
                // X-Richtung begrenzt die Anzeige
                maxUnitsY = maxUnitsX / panelAspectRatio;
            } else {
                // Y-Richtung begrenzt die Anzeige
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
            // Falls die Lesbarkeit garantiert ist, passe die Grenzen an, um das
            // Seitenverhältnis beizubehalten
            double xRange = xMax - xMin;
            double yRange = yMax - yMin;
            double currentAspectRatio = xRange / yRange;

            if (Math.abs(currentAspectRatio - panelAspectRatio) > 0.01) { // Toleranz für Gleitkomma-Vergleiche
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

        // Aktualisiere die Skalierungsfaktoren
        updateScaleFactors();
    }

    /**
     * Aktualisiert die Skalierungsfaktoren basierend auf der aktuellen Ansicht und
     * der Panelgröße
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
     * Bestimmt die geeignete Anzahl von Dezimalstellen basierend auf dem Zoomlevel
     */
    public void updateAxisFormat() {
        // Berechne den Wertebereich (kleinere Bereiche = mehr Dezimalstellen)
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Verwende den kleineren Bereich für das Formatieren
        double range = Math.min(xRange, yRange);

        // Logarithmische Skalierung für die Anzahl der Dezimalstellen
        int decimalPlaces = 2; // Mindestens 2 Dezimalstellen

        if (range < 0.1) {
            decimalPlaces = 5;
        } else if (range < 1) {
            decimalPlaces = 4;
        } else if (range < 10) {
            decimalPlaces = 3;
        }

        // Erstelle ein Formatmuster mit variabler Anzahl von Dezimalstellen
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("#");
        }

        // Aktualisiere das Format
        axisFormat = new DecimalFormat(pattern.toString());
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    public void resetView() {
        // Setze das Zentrum auf den Ursprung
        viewCenter.x = 0;
        viewCenter.y = 0;

        // Berechne eine passende Ansicht basierend auf der Fenstergröße
        int width = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int height = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        if (width <= 0 || height <= 0) {
            // Falls das Panel noch keine Größe hat, verwende Standardwerte
            xMin = -10;
            xMax = 10;
            yMin = -10;
            yMax = 10;
        } else {
            // Berechne, wie viele Einheiten bei Mindestlesbarkeit angezeigt werden können
            double maxUnitsX = width / MIN_PIXELS_PER_UNIT;
            double maxUnitsY = height / MIN_PIXELS_PER_UNIT;

            // Verwende den kleineren Wert oder den Standardbereich
            double halfX = Math.min(maxUnitsX, DEFAULT_VIEW_RANGE) / 2;
            double halfY = Math.min(maxUnitsY, DEFAULT_VIEW_RANGE) / 2;

            // Stelle sicher, dass das Seitenverhältnis beibehalten wird
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

        // Setze neue Grenzen um den Zielpunkt
        xMin = xCenter - xRange / 2;
        xMax = xCenter + xRange / 2;
        yMin = yCenter - yRange / 2;
        yMax = yCenter + yRange / 2;

        // Aktualisiere die Skalierungsfaktoren
        updateScaleFactors();
    }

    /**
     * Wandelt eine X-Bildschirmkoordinate in eine X-Weltkoordinate um
     */
    public double screenToWorldX(int screenX) {
        return xMin + (screenX - xOffset) / xScale;
    }

    /**
     * Wandelt eine Y-Bildschirmkoordinate in eine Y-Weltkoordinate um
     */
    public double screenToWorldY(int screenY) {
        return yMax - (screenY - yOffset) / yScale;
    }

    /**
     * Wandelt eine X-Weltkoordinate in eine X-Bildschirmkoordinate um
     */
    public int worldToScreenX(double worldX) {
        return (int) (xOffset + (worldX - xMin) * xScale);
    }

    /**
     * Wandelt eine Y-Weltkoordinate in eine Y-Bildschirmkoordinate um
     */
    public int worldToScreenY(double worldY) {
        return (int) (yOffset + (yMax - worldY) * yScale);
    }

    /**
     * Gibt die Koordinaten des aktuellen Ansichts-Zentrums zurück
     */
    public Point2D.Double getViewCenter() {
        return new Point2D.Double(viewCenter.x, viewCenter.y);
    }

    /**
     * Zoomt die Ansicht um den angegebenen Faktor, zentriert auf den angegebenen
     * Bildschirm-Punkt
     */
    public void zoom(double factor, Point2D screenPoint) {
        // Wandle den Bildschirm-Punkt in Weltkoordinaten um
        double worldMouseX = screenToWorldX((int) screenPoint.getX());
        double worldMouseY = screenToWorldY((int) screenPoint.getY());

        // Speichere die aktuellen Bereiche
        double oldYRange = yMax - yMin;
        double oldXRange = xMax - xMin;

        // Passe die Bereiche an
        double newYRange = oldYRange * factor;
        double newXRange = oldXRange * factor;

        // Bestimme den Punkt, auf den gezoomt wird (Position des Mauszeigers)
        double relX = (worldMouseX - xMin) / oldXRange; // Relative Position zur Breite
        double relY = (worldMouseY - yMin) / oldYRange; // Relative Position zur Höhe

        // Berechne neue Grenzen, sodass der Mauszeiger seine relative Position
        // beibehält
        xMin = worldMouseX - relX * newXRange;
        xMax = xMin + newXRange;
        yMin = worldMouseY - relY * newYRange;
        yMax = yMin + newYRange;

        // Aktualisiere das Zentrum
        updateViewCenter();

        // Aktualisiere die Skalierungsfaktoren
        updateScaleFactors();
    }

    /**
     * Verschiebt (pan) die Ansicht um den angegebenen Pixelbetrag
     */
    public void pan(int dx, int dy) {
        // Berechne die Verschiebung in Weltkoordinaten
        double worldDx = dx / xScale;
        double worldDy = dy / yScale;

        // Passe die Ansicht an
        xMin -= worldDx;
        xMax -= worldDx;
        yMin += worldDy;
        yMax += worldDy;

        // Aktualisiere das Zentrum
        updateViewCenter();

        // Aktualisiere die Skalierungsfaktoren
        updateScaleFactors();
    }

    // Getter
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

    // Setter
    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }
}
