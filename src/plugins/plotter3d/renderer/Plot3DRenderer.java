package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Haupt-Renderer-Klasse für die 3D-Darstellung
 * Koordiniert die anderen Renderer-Komponenten und stellt eine öffentliche API
 * bereit
 */
public class Plot3DRenderer {
    // Kernkomponenten
    private final Plot3DModel model;
    private final Plot3DView view;
    private final Plot3DTransformer transformer;
    private final Plot3DGridRenderer gridRenderer;
    private final Plot3DFunctionRenderer functionRenderer;
    private final Plot3DColorScheme colorScheme;
    private boolean useHeatmap = true;
    private boolean useSolidSurface = false;
    private boolean showIntersections = true; // Option für Schnittlinien

    /**
     * Erzeugt einen neuen 3D-Renderer mit den angegebenen Bereichsgrenzen und der
     * Auflösung
     */
    public Plot3DRenderer(double xMin, double xMax, double yMin, double yMax, int resolution) {
        // Initialisiere die Komponenten
        this.model = new Plot3DModel();
        this.view = new Plot3DView(xMin, xMax, yMin, yMax, resolution);
        this.transformer = new Plot3DTransformer();
        this.gridRenderer = new Plot3DGridRenderer(transformer);
        this.functionRenderer = new Plot3DFunctionRenderer(transformer);
        this.colorScheme = Plot3DColorScheme.createDefault();
    }

    /**
     * Aktiviert oder deaktiviert die Anzeige von Schnittlinien zwischen Funktionen
     */
    public void setShowIntersections(boolean showIntersections) {
        this.showIntersections = showIntersections;
    }

    /**
     * Gibt zurück, ob Schnittlinien angezeigt werden
     */
    public boolean isShowIntersections() {
        return showIntersections;
    }

    /**
     * Aktiviert oder deaktiviert die undurchsichtige Oberflächendarstellung mit
     * Schattierung
     */
    public void setUseSolidSurface(boolean useSolidSurface) {
        this.useSolidSurface = useSolidSurface;
    }

    /**
     * Gibt zurück, ob die undurchsichtige Oberflächendarstellung mit Schattierung
     * aktiviert ist
     */
    public boolean isUseSolidSurface() {
        return useSolidSurface;
    }

    /**
     * Fügt eine neue Funktion hinzu
     */
    public void addFunction(String functionExpression, Color color) {
        model.addFunction(functionExpression, color);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
        functionChanged(); // Cache leeren
    }

    /**
     * Entfernt alle Funktionen
     */
    public void clearFunctions() {
        model.clearFunctions();
        functionChanged(); // Cache leeren
    }

    /**
     * Entfernt eine Funktion am angegebenen Index
     */
    public void removeFunction(int index) {
        model.removeFunction(index);
        functionChanged(); // Cache leeren
    }

    /**
     * Gibt die Liste der Funktionen zurück
     */
    public java.util.List<Plot3DModel.Function3DInfo> getFunctions() {
        return model.getFunctions();
    }

    /**
     * Setzt den Heatmap-Modus
     */
    public void setUseHeatmap(boolean useHeatmap) {
        this.useHeatmap = useHeatmap;
    }

    /**
     * Setzt die Rotation
     */
    public void setRotation(double rotationX, double rotationY, double rotationZ) {
        view.setRotation(rotationX, rotationY, rotationZ);
        transformAndProjectAllPoints();
    }

    /**
     * Setzt den Skalierungsfaktor
     */
    public void setScale(double scale) {
        view.setScale(scale);
        transformAndProjectAllPoints();
    }

    /**
     * Zoomt um den angegebenen Faktor
     */
    public void zoom(double factor) {
        view.zoom(factor);
        transformAndProjectAllPoints();
    }

    /**
     * Setzt die Pan-Werte
     */
    public void setPan(double panX, double panY) {
        view.setPan(panX, panY);
        transformAndProjectAllPoints();
    }

    /**
     * Addiert zu den aktuellen Pan-Werten
     */
    public void addPan(double deltaPanX, double deltaPanY) {
        view.addPan(deltaPanX, deltaPanY);
        transformAndProjectAllPoints();
    }

    /**
     * Setzt die Auflösung
     */
    public void setResolution(int resolution) {
        view.setResolution(resolution);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
    }

    /**
     * Setzt die Bereichsgrenzen
     */
    public void setBounds(double xMin, double xMax, double yMin, double yMax) {
        view.setBounds(xMin, xMax, yMin, yMax);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
    }

    /**
     * Schaltet die Sichtbarkeit des Koordinatensystems um
     */
    public void setShowCoordinateSystem(boolean show) {
        view.setShowCoordinateSystem(show);
    }

    /**
     * Schaltet die Gitteranzeige um
     */
    public void setShowGrid(boolean show) {
        view.setShowGrid(show);
    }

    /**
     * Schaltet die Anzeige von Hilfslinien um
     */
    public void setShowHelperLines(boolean show) {
        view.setShowHelperLines(show);
    }

    /**
     * Gibt zurück, ob der Heatmap-Modus aktiv ist
     */
    public boolean isUseHeatmap() {
        return useHeatmap;
    }

    /**
     * Berechnet alle Funktionswerte
     */
    private void calculateAllFunctionValues() {
        model.calculateAllFunctionValues(
                view.getXMin(), view.getXMax(),
                view.getYMin(), view.getYMax(),
                view.getResolution());
    }

    /**
     * Transformiert und projiziert alle Punkte
     */
    private void transformAndProjectAllPoints() {
        transformer.transformAndProjectAllPoints(
                model, view, (model.getZMax() + model.getZMin()) / 2);
    }

    /**
     * Rendert den 3D-Plot auf dem bereitgestellten Grafikkontext
     */
    public void render(Graphics2D g2d, int width, int height) {
        // Anti-Aliasing aktivieren
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Anzeigeeinstellungen berechnen
        double displayScale = Math.min(width, height) * 0.6;
        int xOffset = width / 2;
        int yOffset = height / 2;

        // Koordinatensystem zeichnen
        if (view.isShowCoordinateSystem()) {
            if (view.isShowGrid()) {
                gridRenderer.drawCoordinateGrid(g2d, model, view, displayScale, xOffset, yOffset);
            }

            if (view.isShowHelperLines()) {
                gridRenderer.drawHelperLines(g2d, model, view, displayScale, xOffset, yOffset);
            }

            gridRenderer.drawAxes(g2d, model, view, displayScale, xOffset, yOffset);
            gridRenderer.drawTicksAndLabels(g2d, model, view, displayScale, xOffset, yOffset);
        }

        // Funktionen zeichnen - mit allen Parametern
        functionRenderer.drawFunctions(g2d, model, view, displayScale, xOffset, yOffset, useHeatmap, useSolidSurface);

        // Schnittlinien zwischen Funktionen zeichnen, wenn aktiviert
        if (showIntersections && model.getFunctions().size() >= 2) {
            drawIntersectionCurves(g2d, displayScale, xOffset, yOffset, width, height);
        }

        // Informationsbeschriftungen zeichnen
        gridRenderer.drawInfoLabels(g2d, model, view, width, height);

        // Farbskala zeichnen
        gridRenderer.drawColorScale(g2d, model, colorScheme, width - 30, 50, 20, height - 100);
    }

    /**
     * Zeichnet Schnittlinien zwischen Funktionen mit optimierter pixelbasierter
     * Berechnung
     */
    private void drawIntersectionCurves(Graphics2D g2d, double displayScale, int xOffset, int yOffset, int width,
            int height) {
        // Verwende den pixelbasierten Schnittlinien-Rechner
        List<List<Plot3DPoint>> intersections = PixelBasedIntersectionCalculator.calculateVisibleIntersections(
                model, view, displayScale, width, height);

        // Zeichne die Schnittlinien mit der optimierten Methode
        PixelBasedIntersectionCalculator.drawIntersectionCurves(
                g2d, transformer, intersections, model, view,
                displayScale, xOffset, yOffset);
    }

    /**
     * Informiert den Renderer über Funktionsänderungen, um den Cache zu leeren
     */
    public void functionChanged() {
        // Cache leeren, wenn Funktionen hinzugefügt/entfernt/geändert werden
        PixelBasedIntersectionCalculator.clearCache();
    }

    /**
     * Leert den Schnittlinien-Cache
     */
    public void clearIntersectionCache() {
        PixelBasedIntersectionCalculator.clearCache();
    }

    /**
     * Gibt verwendete Ressourcen frei
     */
    public void shutdown() {
        PixelBasedIntersectionCalculator.shutdown();
    }

    /**
     * Erstellt ein Schnappschussbild des aktuellen Plots
     */
    public BufferedImage createImage(int width, int height) {
        return functionRenderer.createImage(null, model, view, colorScheme, gridRenderer, width, height, useHeatmap);
    }

    // Getter für Rückwärtskompatibilität

    public double getXMin() {
        return view.getXMin();
    }

    public double getXMax() {
        return view.getXMax();
    }

    public double getYMin() {
        return view.getYMin();
    }

    public double getYMax() {
        return view.getYMax();
    }
}
