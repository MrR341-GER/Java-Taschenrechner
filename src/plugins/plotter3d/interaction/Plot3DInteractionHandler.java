import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

/**
 * Verwaltet die Benutzerinteraktionen mit dem 3D-Plot
 * Behandelt Mausereignisse für Rotation, Zoom und Verschiebung
 */
public class Plot3DInteractionHandler {
    private final JPanel plotPanel;
    private final Plot3DViewController viewController;
    private final Plot3DPanel mainPanel;

    // Mouse-Interaktionsparameter
    private Point lastMousePos;
    private boolean isDragging = false;
    private boolean isPanning = false;

    // Debug-Referenz
    private DebugManager debugManager;

    /**
     * Konstruktor für den Interaktions-Handler
     * 
     * @param plotPanel      Das Plot-Panel, auf dem gezeichnet wird
     * @param viewController Der View-Controller
     * @param mainPanel      Das Hauptpanel des 3D-Plotters
     */
    public Plot3DInteractionHandler(JPanel plotPanel, Plot3DViewController viewController,
            Plot3DPanel mainPanel) {
        this.plotPanel = plotPanel;
        this.viewController = viewController;
        this.mainPanel = mainPanel;

        // Mouse-Listener einrichten
        setupMouseListeners();
    }

    /**
     * Richtet die Maus-Listener für Interaktionen ein
     */
    private void setupMouseListeners() {
        // Mouse-Listener für Rotation und Zoom
        plotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;

                // Shift-Taste gedrückt = Panning, sonst Rotation
                isPanning = e.isShiftDown();
                debug("Interaktion gestartet: " + (isPanning ? "Panning" : "Rotation"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        plotPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Berechne Verschiebung
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;

                    if (isPanning) {
                        handlePanning(dx, dy);
                    } else {
                        handleRotation(dx, dy);
                    }

                    lastMousePos = e.getPoint();
                }
            }
        });

        // Mouse-Wheel-Listener für Wertebereichsänderung (wie im 2D-Plotter)
        plotPanel.addMouseWheelListener(e -> {
            try {
                handleZoom(e.getWheelRotation());
            } catch (Exception ex) {
                debug("Fehler beim Zoom: " + ex.getMessage());
            }
        });
    }

    /**
     * Verarbeitet die Rotation des 3D-Plots
     * 
     * @param dx Horizontale Mausbewegung
     * @param dy Vertikale Mausbewegung
     */
    private void handleRotation(int dx, int dy) {
        // Rotations-Modus
        double rotationScale = 0.5;
        double newRotationX = viewController.getCurrentRotationX() + dy * rotationScale;
        double newRotationZ = viewController.getCurrentRotationZ() + dx * rotationScale;

        // Begrenze Rotation auf 0-360 Grad
        newRotationX = (newRotationX + 360) % 360;
        newRotationZ = (newRotationZ + 360) % 360;

        // Werte im View-Controller aktualisieren
        viewController.setCurrentRotationX(newRotationX);
        viewController.setCurrentRotationZ(newRotationZ);

        // Aktualisiere Rotation im Renderer
        viewController.updateRotation();

        // Plot neu zeichnen
        plotPanel.repaint();

        // Sicherstellen, dass das Panel vollständig aktualisiert wird
        if (mainPanel != null) {
            mainPanel.renderPlot();
        }
    }

    /**
     * Verarbeitet das Verschieben der Ansicht
     * 
     * @param dx Horizontale Mausbewegung
     * @param dy Vertikale Mausbewegung
     */
    private void handlePanning(int dx, int dy) {
        // Pan-Modus: Verschiebe den Wertebereich
        // Berechne die Verschiebung im Wertebereich
        // Die Verschiebung ist umgekehrt zur Mausbewegung, daher negativ
        double xRange = viewController.getCurrentXMax() - viewController.getCurrentXMin();
        double yRange = viewController.getCurrentYMax() - viewController.getCurrentYMin();

        // Skaliere die Verschiebung relativ zur Größe des Wertebereichs
        // und der Größe des Anzeigebereichs
        double viewWidth = plotPanel.getWidth();
        double viewHeight = plotPanel.getHeight();

        double deltaX = -dx * (xRange / viewWidth);
        double deltaY = dy * (yRange / viewHeight); // Y-Achse ist invertiert

        // Neue Grenzen berechnen
        double newXMin = viewController.getCurrentXMin() + deltaX;
        double newXMax = viewController.getCurrentXMax() + deltaX;
        double newYMin = viewController.getCurrentYMin() + deltaY;
        double newYMax = viewController.getCurrentYMax() + deltaY;

        // Werte im View-Controller aktualisieren
        viewController.setCurrentXMin(newXMin);
        viewController.setCurrentXMax(newXMax);
        viewController.setCurrentYMin(newYMin);
        viewController.setCurrentYMax(newYMax);

        // Neuen Plot mit verschobenem Wertebereich zeichnen
        mainPanel.renderPlot();

        debug("Wertebereich verschoben nach: x=[" + newXMin + "," + newXMax +
                "], y=[" + newYMin + "," + newYMax + "]");
    }

    /**
     * Verarbeitet Zoom-Operationen
     * 
     * @param wheelRotation Mausrad-Drehung
     */
    private void handleZoom(int wheelRotation) {
        // Aktuelle Wertebereiche holen
        double xMin = viewController.getCurrentXMin();
        double xMax = viewController.getCurrentXMax();
        double yMin = viewController.getCurrentYMin();
        double yMax = viewController.getCurrentYMax();

        // Bereichsgröße berechnen
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        // Zoom-Faktor basierend auf Mausrad-Richtung
        double zoomFactor = wheelRotation < 0 ? 0.8 : 1.25; // Zoomen wir rein oder raus?

        // Neue Wertebereiche berechnen (zentriert)
        double newXRange = xRange * zoomFactor;
        double newYRange = yRange * zoomFactor;

        // Mittelpunkt beibehalten
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;

        // Neue Grenzen berechnen
        double newXMin = xCenter - newXRange / 2;
        double newXMax = xCenter + newXRange / 2;
        double newYMin = yCenter - newYRange / 2;
        double newYMax = yCenter + newYRange / 2;

        // Werte im View-Controller aktualisieren
        viewController.setCurrentXMin(newXMin);
        viewController.setCurrentXMax(newXMax);
        viewController.setCurrentYMin(newYMin);
        viewController.setCurrentYMax(newYMax);

        // Neu rendern
        mainPanel.renderPlot();

        debug("Wertebereich geändert durch Mausrad: " +
                "[" + formatDouble(newXMin) + ", " + formatDouble(newXMax) + "] x " +
                "[" + formatDouble(newYMin) + ", " + formatDouble(newYMax) + "]");
    }

    /**
     * Formatiert eine Dezimalzahl für Debug-Ausgaben
     */
    private String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(value);
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
            debugManager.debug("[3D-Interaktion] " + message);
        } else {
            System.out.println("[3D-Interaktion] " + message);
        }
    }
}