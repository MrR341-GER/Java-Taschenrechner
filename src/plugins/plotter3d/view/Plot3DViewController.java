package plugins.plotter3d.view;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

import plugins.plotter3d.Plot3DPanel;
import plugins.plotter3d.renderer.Plot3DRenderer;
import util.debug.DebugManager;

/**
 * Verwaltet die Ansicht des 3D-Plots
 * Kontrolliert Rotation, Zoomfaktor, Grenzen und verschiedene
 * Ansichtseinstellungen
 */
public class Plot3DViewController {
    private final Plot3DRenderer renderer;

    // Referenz auf das übergeordnete Panel
    private Plot3DPanel parentPanel;

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
    private boolean useSolidSurface = false;
    private boolean showIntersections = true; // Neue Eigenschaft für Schnittlinien

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
        renderer.setShowIntersections(showIntersections); // Initialwert für Schnittlinien setzen
    }

    /**
     * Aktiviert oder deaktiviert die Anzeige von Schnittlinien zwischen Funktionen
     */
    public void setShowIntersections(boolean show) {
        this.showIntersections = show;
        if (renderer != null) {
            renderer.setShowIntersections(show);

            // Explizites Neuzeichnen, wenn verfügbar
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }
        }
    }

    /**
     * Gibt zurück, ob Schnittlinien angezeigt werden
     */
    public boolean isShowIntersections() {
        if (renderer != null) {
            return renderer.isShowIntersections();
        }
        return showIntersections;
    }

    /**
     * Aktiviert oder deaktiviert die undurchsichtige Oberflächendarstellung mit
     * Schattierung
     */
    public void setUseSolidSurface(boolean useSolidSurface) {
        this.useSolidSurface = useSolidSurface;

        if (renderer != null) {
            renderer.setUseSolidSurface(useSolidSurface);

            // Explizites Neuzeichnen, wenn verfügbar
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }
        }

        debug("Undurchsichtige Darstellung mit Schattierung: " +
                (useSolidSurface ? "aktiviert" : "deaktiviert"));
    }

    /**
     * Gibt zurück, ob die undurchsichtige Oberflächendarstellung mit Schattierung
     * aktiviert ist
     */
    public boolean isUseSolidSurface() {
        if (renderer != null) {
            return renderer.isUseSolidSurface();
        }
        return useSolidSurface;
    }

    /**
     * Setzt die Referenz auf das übergeordnete Panel
     */
    public void setParentPanel(Plot3DPanel panel) {
        this.parentPanel = panel;
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

            // Explizites Neuzeichnen, wenn verfügbar
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }

        } catch (ParseException e) {
            debug("Fehler beim Parsen der Bereichsangaben: " + e.getMessage());
            throw new IllegalArgumentException("Bitte geben Sie gültige Zahlen für die Bereiche ein.");
        }
    }

    /**
     * Aktualisiert die Auflösung im Renderer
     */
    public void setResolution(int resolution) {
        if (resolution <= 0) {
            throw new IllegalArgumentException("Die Auflösung muss größer als 0 sein.");
        }

        // Hohe Auflösungswerte können die Performance beeinträchtigen
        if (resolution > 300) {
            // Bei extremen Werten eine Warnung anzeigen
            int option = JOptionPane.showConfirmDialog(
                    parentPanel,
                    "Eine Auflösung von " + resolution + " kann sehr rechenintensiv sein und das System verlangsamen.\n"
                            +
                            "Empfohlene Obergrenze: 300\n\n" +
                            "Möchten Sie trotzdem fortfahren?",
                    "Warnung: Hohe Auflösung",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option != JOptionPane.YES_OPTION) {
                debug("Hoher Auflösungswert abgelehnt: " + resolution);
                return;
            }
        } else if (resolution > 200) {
            // Einfacher Hinweis (kein Dialog) für Werte zwischen 200 und 300
            debug("Hinweis: Hohe Auflösung " + resolution + " kann die Darstellung verlangsamen");
        }

        // Erfolgreiche Aktualisierung protokollieren
        debug("Auflösung geändert auf " + resolution);
        renderer.setResolution(resolution);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
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

            // Clear intersection cache when view resets
            renderer.clearIntersectionCache();

            debug("Ansicht auf Standardwerte zurückgesetzt");

            // Explizites Neuzeichnen, wenn verfügbar
            if (parentPanel != null) {
                parentPanel.renderPlot();
            }

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

    /**
     * Aktiviert oder deaktiviert den Heatmap-Modus
     */
    public void setUseHeatmap(boolean useHeatmap) {
        if (renderer != null) {
            renderer.setUseHeatmap(useHeatmap);

            // Explizites Neuzeichnen, wenn verfügbar
            if (parentPanel != null) {
                parentPanel.getPlotPanel().repaint();
            }
        }
    }

    /**
     * Gibt zurück, ob der Heatmap-Modus aktiv ist
     */
    public boolean isUseHeatmap() {
        if (renderer != null) {
            return renderer.isUseHeatmap();
        }
        return true; // Standardmäßig an
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

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void setPan(double panX, double panY) {
        renderer.setPan(panX, panY);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void addPan(double deltaPanX, double deltaPanY) {
        renderer.addPan(deltaPanX, deltaPanY);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void zoom(double factor) {
        currentScale *= factor;
        renderer.zoom(factor);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void setShowCoordinateSystem(boolean show) {
        this.showCoordinateSystem = show;
        renderer.setShowCoordinateSystem(show);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        renderer.setShowGrid(show);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
    }

    public void setShowHelperLines(boolean show) {
        this.showHelperLines = show;
        renderer.setShowHelperLines(show);

        // Explizites Neuzeichnen, wenn verfügbar
        if (parentPanel != null) {
            parentPanel.renderPlot();
        }
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

    /**
     * Gibt die aktuelle Auflösung zurück
     */
    public int getCurrentResolution() {
        if (renderer != null) {
            return renderer.getResolution();
        }
        return DEFAULT_RESOLUTION;
    }
}