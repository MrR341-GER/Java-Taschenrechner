package plugins.plotter3d.view;

/**
 * Verwaltet ansichtsbezogene Parameter für 3D-Darstellungen
 * Behandelt Grenzen, Rotation, Skalierung und Anzeigeoptionen
 */
public class Plot3DView {
    // Ansichtsgrenzen
    private double xMin, xMax, yMin, yMax;
    private double originalXMin, originalXMax, originalYMin, originalYMax; // Für Zurücksetzen

    // Auflösung (Gitterdichte)
    private int resolution;

    // Rotationswinkel (in Grad)
    private double rotationX; // Rotation um die X-Achse
    private double rotationY; // Rotation um die Y-Achse
    private double rotationZ; // Rotation um die Z-Achse

    // Skalierung und Verschiebung
    private double scale = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Anzeigeoptionen
    private boolean showCoordinateSystem = true;
    private boolean showGrid = true;
    private boolean showHelperLines = true;
    private boolean useSolidSurface = false;
    private boolean showIntersections = true; // Neue Option für Schnittlinien

    // Farbmodus
    private boolean useHeatmap = true; // Standardmäßig Heatmap verwenden

    /**
     * Erzeugt eine neue Ansicht mit den angegebenen Grenzen und der Auflösung
     */
    public Plot3DView(double xMin, double xMax, double yMin, double yMax, int resolution) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        // Speichert die ursprünglichen Grenzen für das Zurücksetzen
        this.originalXMin = xMin;
        this.originalXMax = xMax;
        this.originalYMin = yMin;
        this.originalYMax = yMax;

        // Nur Minimalwert begrenzen, keine Obergrenze mehr
        this.resolution = Math.max(10, resolution);

        // Standardrotation
        this.rotationX = 30;
        this.rotationY = 0;
        this.rotationZ = 30;
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
     * Setzt die Ansicht auf ihren ursprünglichen Zustand zurück
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
     * Setzt neue Grenzen
     */
    public void setBounds(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    /**
     * Setzt die Rotationswinkel
     */
    public void setRotation(double rotationX, double rotationY, double rotationZ) {
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    /**
     * Setzt den Skalierungsfaktor
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.1, Math.min(10.0, scale)); // Skalierung auf einen vernünftigen Bereich begrenzen
    }

    /**
     * Zoomt um den angegebenen Faktor
     */
    public void zoom(double factor) {
        setScale(scale * factor);
    }

    /**
     * Setzt die Verschiebungswerte
     */
    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
    }

    /**
     * Erhöht die aktuellen Verschiebungswerte
     */
    public void addPan(double deltaPanX, double deltaPanY) {
        this.panX += deltaPanX;
        this.panY += deltaPanY;
    }

    /**
     * Setzt die Auflösung (Gitterdichte)
     */
    public void setResolution(int resolution) {
        // Nur Minimalwert begrenzen, keine Obergrenze mehr
        this.resolution = Math.max(10, resolution);
    }

    /**
     * Schaltet die Sichtbarkeit des Koordinatensystems um
     */
    public void setShowCoordinateSystem(boolean show) {
        this.showCoordinateSystem = show;
    }

    /**
     * Schaltet die Sichtbarkeit des Gitters um
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }

    /**
     * Schaltet die Sichtbarkeit der Hilfslinien um
     */
    public void setShowHelperLines(boolean show) {
        this.showHelperLines = show;
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
