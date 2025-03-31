package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Main renderer class for 3D plotting
 * Coordinates the other renderer components and provides a public API
 */
public class Plot3DRenderer {
    // Core components
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
     * Creates a new 3D renderer with the specified bounds and resolution
     */
    public Plot3DRenderer(double xMin, double xMax, double yMin, double yMax, int resolution) {
        // Initialize components
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
     * Adds a new function
     */
    public void addFunction(String functionExpression, Color color) {
        model.addFunction(functionExpression, color);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
        functionChanged(); // Cache leeren
    }

    /**
     * Removes all functions
     */
    public void clearFunctions() {
        model.clearFunctions();
        functionChanged(); // Cache leeren
    }

    /**
     * Removes a function at the specified index
     */
    public void removeFunction(int index) {
        model.removeFunction(index);
        functionChanged(); // Cache leeren
    }

    /**
     * Returns the list of functions
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
     * Sets the rotation
     */
    public void setRotation(double rotationX, double rotationY, double rotationZ) {
        view.setRotation(rotationX, rotationY, rotationZ);
        transformAndProjectAllPoints();
    }

    /**
     * Sets the scale
     */
    public void setScale(double scale) {
        view.setScale(scale);
        transformAndProjectAllPoints();
    }

    /**
     * Zooms by the specified factor
     */
    public void zoom(double factor) {
        view.zoom(factor);
        transformAndProjectAllPoints();
    }

    /**
     * Sets the pan values
     */
    public void setPan(double panX, double panY) {
        view.setPan(panX, panY);
        transformAndProjectAllPoints();
    }

    /**
     * Adds to the current pan values
     */
    public void addPan(double deltaPanX, double deltaPanY) {
        view.addPan(deltaPanX, deltaPanY);
        transformAndProjectAllPoints();
    }

    /**
     * Sets the resolution
     */
    public void setResolution(int resolution) {
        view.setResolution(resolution);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
    }

    /**
     * Sets the bounds
     */
    public void setBounds(double xMin, double xMax, double yMin, double yMax) {
        view.setBounds(xMin, xMax, yMin, yMax);
        calculateAllFunctionValues();
        transformAndProjectAllPoints();
    }

    /**
     * Toggles coordinate system visibility
     */
    public void setShowCoordinateSystem(boolean show) {
        view.setShowCoordinateSystem(show);
    }

    /**
     * Toggles grid visibility
     */
    public void setShowGrid(boolean show) {
        view.setShowGrid(show);
    }

    /**
     * Toggles helper lines visibility
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
     * Calculates all function values
     */
    private void calculateAllFunctionValues() {
        model.calculateAllFunctionValues(
                view.getXMin(), view.getXMax(),
                view.getYMin(), view.getYMax(),
                view.getResolution());
    }

    /**
     * Transforms and projects all points
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

        // Informationslabels zeichnen
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
     * Creates a snapshot image of the current plot
     */
    public BufferedImage createImage(int width, int height) {
        return functionRenderer.createImage(null, model, view, colorScheme, gridRenderer, width, height, useHeatmap);
    }

    // Getters for backward compatibility

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