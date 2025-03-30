import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JTextField;

/**
 * Verwaltet die Ansicht des 3D-Plots
 * Kontrolliert Rotation, Zoomfaktor, Grenzen und verschiedene
 * Ansichtseinstellungen
 */
public class Plot3DViewController {
    private final Plot3DRenderer renderer;

    // Standardwerte für die Ansicht
    public static final double DEFAULT_MIN = -5.0;
    public static final double DEFAULT_MAX = 5.0;
    public static final int DEFAULT_RESOLUTION = 30;
    public static final String DEFAULT_FUNCTION = "sin(sqrt(x^2+y^2))";

    // View-Einstellungen
    private double currentRotationX = 25; // Angepasst von 30 auf 25
    private double currentRotationY = 0;
    private double currentRotationZ = 35; // Angepasst von 30 auf 35
    private double currentScale = 1.2; // Erhöht von 1.0 auf 1.2

    // Textfelder für Wertebereich (werden im UI-Builder erstellt)
    private JTextField xMinField;
    private JTextField xMaxField;
    private JTextField yMinField;
    private JTextField yMaxField;

    // Aktuelle Wertebereiche
    private double currentXMin = DEFAULT_MIN;
    private double currentXMax = DEFAULT_MAX;
    private double currentYMin = DEFAULT_MIN;
    private double currentYMax = DEFAULT_MAX;

    // Ansichtsoptionen
    private boolean showCoordinateSystem = true;
    private boolean showGrid = true;
    private boolean showHelperLines = true;

    // Debug-Referenz
    private DebugManager debugManager;

    /**
     * Konstruktor für den View-Controller
     * 
     * @param renderer Der Plot3DRenderer
     */
    public Plot3DViewController(Plot3DRenderer renderer) {
        this.renderer = renderer;

        // Initialisiere den Renderer mit Standardwerten
        updateRotation();
        renderer.setScale(currentScale);
        renderer.setShowCoordinateSystem(showCoordinateSystem);
        renderer.setShowGrid(showGrid);
        renderer.setShowHelperLines(showHelperLines);
    }

    /**
     * Aktualisiert die Rotation im Renderer
     */
    public void updateRotation() {
        if (renderer != null) {
            renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
        }
    }

    /**
     * Aktualisiert die Grenzen der Ansicht basierend auf den Textfeldern
     */
    public void updateViewBounds() {
        try {
            // Bereichsangaben parsen mit verbesserter Methode
            double xMin = parseDecimal(xMinField.getText().trim());
            double xMax = parseDecimal(xMaxField.getText().trim());
            double yMin = parseDecimal(yMinField.getText().trim());
            double yMax = parseDecimal(yMaxField.getText().trim());

            // Bereichsgültigkeiten überprüfen
            if (xMin >= xMax || yMin >= yMax) {
                debug("Ungültige Bereichsangaben");
                throw new IllegalArgumentException("Die Min-Werte müssen kleiner als die Max-Werte sein.");
            }

            debug("Aktualisiere Bereich: x=[" + xMin + "," + xMax + "], y=[" + yMin + "," + yMax + "]");

            // Speichere die aktuellen Wertebereiche
            currentXMin = xMin;
            currentXMax = xMax;
            currentYMin = yMin;
            currentYMax = yMax;

            // Grenzen im Renderer setzen
            renderer.setBounds(xMin, xMax, yMin, yMax);
        } catch (ParseException e) {
            debug("Fehler beim Parsen der Bereichsangaben: " + e.getMessage());
            throw new IllegalArgumentException("Bitte geben Sie gültige Zahlen für die Bereiche ein.");
        }
    }

    /**
     * Aktualisiert die Auflösung im Renderer
     */
    public void setResolution(int resolution) {
        renderer.setResolution(resolution);
        debug("Auflösung geändert auf " + resolution);
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    public void resetView() {
        // Standardwerte für Rotation
        currentRotationX = 25;
        currentRotationY = 0;
        currentRotationZ = 35;
        currentScale = 1.2;

        // Wertebereiche auf Standardwerte zurücksetzen
        currentXMin = DEFAULT_MIN;
        currentXMax = DEFAULT_MAX;
        currentYMin = DEFAULT_MIN;
        currentYMax = DEFAULT_MAX;

        // Textfelder aktualisieren
        if (xMinField != null)
            xMinField.setText(String.valueOf(DEFAULT_MIN));
        if (xMaxField != null)
            xMaxField.setText(String.valueOf(DEFAULT_MAX));
        if (yMinField != null)
            yMinField.setText(String.valueOf(DEFAULT_MIN));
        if (yMaxField != null)
            yMaxField.setText(String.valueOf(DEFAULT_MAX));

        // Renderer aktualisieren
        try {
            // Grenzen im Renderer aktualisieren
            renderer.setBounds(DEFAULT_MIN, DEFAULT_MAX, DEFAULT_MIN, DEFAULT_MAX);

            // Rotation und Skalierung aktualisieren
            renderer.setRotation(currentRotationX, currentRotationY, currentRotationZ);
            renderer.setScale(currentScale);

            debug("Ansicht auf Standardwerte zurückgesetzt");
        } catch (Exception e) {
            debug("Fehler beim Zurücksetzen der Ansicht: " + e.getMessage());
        }
    }

    /**
     * Parst einen Dezimalwert aus einem String, unterstützt sowohl Punkt als auch
     * Komma als Dezimaltrennzeichen
     */
    public static double parseDecimal(String text) throws ParseException {
        // Erst direkt parsen versuchen (für Punkt als Dezimaltrennzeichen)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Falls fehlgeschlagen, Komma durch Punkt ersetzen und erneut versuchen
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Als letzten Versuch die aktuelle Locale verwenden
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }

    /**
     * Setzt den DebugManager für Logging
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
    }

    /**
     * Schreibt Debug-Informationen in das Debug-Fenster
     */
    private void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[3D-View] " + message);
        } else {
            System.out.println("[3D-View] " + message);
        }
    }

    // Getter und Setter für die View-Eigenschaften

    public double getCurrentRotationX() {
        return currentRotationX;
    }

    public void setCurrentRotationX(double rotationX) {
        this.currentRotationX = rotationX;
    }

    public double getCurrentRotationY() {
        return currentRotationY;
    }

    public void setCurrentRotationY(double rotationY) {
        this.currentRotationY = rotationY;
    }

    public double getCurrentRotationZ() {
        return currentRotationZ;
    }

    public void setCurrentRotationZ(double rotationZ) {
        this.currentRotationZ = rotationZ;
    }

    public double getCurrentScale() {
        return currentScale;
    }

    public void setCurrentScale(double scale) {
        this.currentScale = scale;
        renderer.setScale(scale);
    }

    public void setPan(double panX, double panY) {
        renderer.setPan(panX, panY);
    }

    public void addPan(double deltaPanX, double deltaPanY) {
        renderer.addPan(deltaPanX, deltaPanY);
    }

    public void zoom(double factor) {
        currentScale *= factor;
        renderer.zoom(factor);
    }

    public void setShowCoordinateSystem(boolean show) {
        this.showCoordinateSystem = show;
        renderer.setShowCoordinateSystem(show);
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        renderer.setShowGrid(show);
    }

    public void setShowHelperLines(boolean show) {
        this.showHelperLines = show;
        renderer.setShowHelperLines(show);
    }

    public void setXMinField(JTextField field) {
        this.xMinField = field;
    }

    public void setXMaxField(JTextField field) {
        this.xMaxField = field;
    }

    public void setYMinField(JTextField field) {
        this.yMinField = field;
    }

    public void setYMaxField(JTextField field) {
        this.yMaxField = field;
    }

    public double getCurrentXMin() {
        return currentXMin;
    }

    public double getCurrentXMax() {
        return currentXMax;
    }

    public double getCurrentYMin() {
        return currentYMin;
    }

    public double getCurrentYMax() {
        return currentYMax;
    }

    public void setCurrentXMin(double xMin) {
        this.currentXMin = xMin;
        if (xMinField != null) {
            DecimalFormat df = new DecimalFormat("0.##");
            xMinField.setText(df.format(xMin));
        }
    }

    public void setCurrentXMax(double xMax) {
        this.currentXMax = xMax;
        if (xMaxField != null) {
            DecimalFormat df = new DecimalFormat("0.##");
            xMaxField.setText(df.format(xMax));
        }
    }

    public void setCurrentYMin(double yMin) {
        this.currentYMin = yMin;
        if (yMinField != null) {
            DecimalFormat df = new DecimalFormat("0.##");
            yMinField.setText(df.format(yMin));
        }
    }

    public void setCurrentYMax(double yMax) {
        this.currentYMax = yMax;
        if (yMaxField != null) {
            DecimalFormat df = new DecimalFormat("0.##");
            yMaxField.setText(df.format(yMax));
        }
    }
}