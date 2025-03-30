
package plugins.plotter3d.interaction;

import javax.swing.*;

import plugins.plotter3d.Plot3DPanel;
import plugins.plotter3d.view.Plot3DViewController;
import util.debug.DebugManager;

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
     * und berücksichtigt die Position des Mauszeigers für intuitivere Steuerung
     */
    private void setupMouseListeners() {
        // Mouse-Listener für Drag-Operationen
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point dragStart;
            private Point lastPosition;

            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                lastPosition = e.getPoint();
                isDragging = true;

                // Shift-Taste gedrückt = Panning, sonst Rotation
                isPanning = e.isShiftDown();
                debug("Interaktion gestartet: " + (isPanning ? "Panning" : "Rotation") +
                        " bei Position: (" + e.getX() + "," + e.getY() + ")");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                dragStart = null;
                lastPosition = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDragging || lastPosition == null) {
                    return;
                }

                // Berechne Verschiebung relativ zur letzten Position
                int dx = e.getX() - lastPosition.x;
                int dy = e.getY() - lastPosition.y;

                // Aktuelle Position des Mauszeigers (für Drehpunkt-Berechnung)
                Point currentPosition = e.getPoint();

                // Je nachdem, ob Verschiebung oder Rotation
                if (isPanning) {
                    handlePanning(dx, dy, currentPosition);
                } else {
                    handleRotation(dx, dy, currentPosition);
                }

                // Position für nächsten Durchlauf speichern
                lastPosition = e.getPoint();
            }
        };

        // Mauslistener registrieren
        plotPanel.addMouseListener(mouseAdapter);
        plotPanel.addMouseMotionListener(mouseAdapter);

        // Mouse-Wheel-Listener für Wertebereichsänderung (wie im 2D-Plotter)
        plotPanel.addMouseWheelListener(e -> {
            try {
                // Position des Mauszeigers für zoom-zentriertes Zoomen
                Point mousePosition = e.getPoint();
                handleZoom(e.getWheelRotation(), mousePosition);
            } catch (Exception ex) {
                debug("Fehler beim Zoom: " + ex.getMessage());
            }
        });
    }

    /**
     * Verarbeitet die Rotation des 3D-Plots mit der Mausposition als Drehpunkt
     * 
     * @param dx            Horizontale Mausbewegung
     * @param dy            Vertikale Mausbewegung
     * @param mousePosition Aktuelle Position des Mauszeigers
     */
    private void handleRotation(int dx, int dy, Point mousePosition) {
        // Aktuelle Rotationswinkel holen
        double currentRotationX = viewController.getCurrentRotationX();
        double currentRotationZ = viewController.getCurrentRotationZ();

        // Anpassbare Rotationsempfindlichkeit
        double rotationSensitivity = 0.5;

        // Prüfen, ob der Graph "umgedreht" ist (X-Rotation zwischen 90° und 270°)
        double normalizedX = currentRotationX % 360;
        if (normalizedX < 0)
            normalizedX += 360;

        // Rotation der Z-Achse umkehren, wenn der Graph "umgedreht" ist
        double zRotationDirection = 1.0;
        if (normalizedX > 90 && normalizedX < 270) {
            zRotationDirection = -1.0;
        }

        // Anpassung der Rotationsgeschwindigkeit basierend auf der Position des
        // Mauszeigers
        // Nahe am Rand = schnellere Rotation, nahe der Mitte = langsamere Rotation
        double centerX = plotPanel.getWidth() / 2.0;
        double centerY = plotPanel.getHeight() / 2.0;

        // Abstand zur Mitte berechnen (0 = Zentrum, 1 = Rand)
        double distanceX = Math.abs(mousePosition.x - centerX) / centerX;
        double distanceY = Math.abs(mousePosition.y - centerY) / centerY;

        // Stelle sicher, dass wir im gültigen Bereich bleiben
        distanceX = Math.min(distanceX, 1.0);
        distanceY = Math.min(distanceY, 1.0);

        // Rotationsgeschwindigkeit basierend auf Position anpassen
        // (Optional: Dies kann weggelassen werden, wenn keine positionsabhängige
        // Geschwindigkeit gewünscht wird)
        double xSpeedFactor = 0.8 + 0.4 * distanceY; // 0.8-1.2x Geschwindigkeit
        double zSpeedFactor = 0.8 + 0.4 * distanceX; // 0.8-1.2x Geschwindigkeit

        // Rotationsänderungen berechnen
        double deltaRotationX = -dy * rotationSensitivity * xSpeedFactor;
        double deltaRotationZ = dx * rotationSensitivity * zSpeedFactor * zRotationDirection;

        // Neue Rotationswerte berechnen und normalisieren (0-360 Grad)
        double newRotationX = (currentRotationX + deltaRotationX) % 360;
        double newRotationZ = (currentRotationZ + deltaRotationZ) % 360;

        // Negative Werte korrigieren
        if (newRotationX < 0)
            newRotationX += 360;
        if (newRotationZ < 0)
            newRotationZ += 360;

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
        debug("Rotation geändert zu: X=" + formatDouble(newRotationX) +
                ", Z=" + formatDouble(newRotationZ) +
                (zRotationDirection < 0 ? " (umgedrehte Z-Richtung)" : ""));
    }

    /**
     * Verarbeitet das Verschieben der Ansicht unter Berücksichtigung der
     * Mausposition
     * und der aktuellen Rotation
     * 
     * @param dx            Horizontale Mausbewegung
     * @param dy            Vertikale Mausbewegung
     * @param mousePosition Aktuelle Position des Mauszeigers
     */
    private void handlePanning(int dx, int dy, Point mousePosition) {
        // Aktuelle Rotationswinkel in Radiant holen
        double rotationXRad = Math.toRadians(viewController.getCurrentRotationX());
        double rotationYRad = Math.toRadians(viewController.getCurrentRotationY());
        double rotationZRad = Math.toRadians(viewController.getCurrentRotationZ());

        // Sinus und Kosinus der Rotationswinkel vorberechnen
        double sinX = Math.sin(rotationXRad);
        double cosX = Math.cos(rotationXRad);
        double sinY = Math.sin(rotationYRad);
        double cosY = Math.cos(rotationYRad);
        double sinZ = Math.sin(rotationZRad);
        double cosZ = Math.cos(rotationZRad);

        // Bereichsgrößen berechnen
        double xRange = viewController.getCurrentXMax() - viewController.getCurrentXMin();
        double yRange = viewController.getCurrentYMax() - viewController.getCurrentYMin();

        // Skalierungsfaktoren basierend auf Fenstergröße
        double viewWidth = plotPanel.getWidth();
        double viewHeight = plotPanel.getHeight();
        double scaleX = xRange / viewWidth;
        double scaleY = yRange / viewHeight;

        // Position des Mauszeigers relativ zur Mitte (Bereich -1 bis 1)
        double relPosX = (mousePosition.x - viewWidth / 2) / (viewWidth / 2);
        double relPosY = (mousePosition.y - viewHeight / 2) / (viewHeight / 2);

        // Optional: Anpassung der Panning-Geschwindigkeit basierend auf der
        // Mausposition
        // Nahe am Rand = schnelleres Panning für präzisere Steuerung
        double speedFactor = 1.0 + 0.3 * (Math.abs(relPosX) + Math.abs(relPosY)) / 2.0;

        // Mausbewegung im Bildschirmraum - beide Richtungen invertieren für intuitive
        // Bewegung
        double screenDx = -dx * scaleX * speedFactor;
        double screenDy = dy * scaleY * speedFactor;

        // 1. Transformation: Mausbewegung im Bildschirmraum entsprechend Z-Rotation
        // anpassen
        double rotatedDx = screenDx * cosZ - screenDy * sinZ;
        double rotatedDy = screenDx * sinZ + screenDy * cosZ;

        // 2. Transformation: Berücksichtigung der Y-Rotation
        double panX = rotatedDx * cosY;
        double panZ = -rotatedDx * sinY;

        // 3. Transformation: Berücksichtigung der X-Rotation für Y und Z
        double tempY = rotatedDy * cosX - panZ * sinX;
        double tempZ = rotatedDy * sinX + panZ * cosX;
        double panY = tempY;

        // Endgültige Panning-Werte
        double sensitivity = 1.0;
        double finalDx = panX * sensitivity;
        double finalDy = panY * sensitivity;

        // Neue Grenzen berechnen
        double newXMin = viewController.getCurrentXMin() + finalDx;
        double newXMax = viewController.getCurrentXMax() + finalDx;
        double newYMin = viewController.getCurrentYMin() + finalDy;
        double newYMax = viewController.getCurrentYMax() + finalDy;

        // Werte im View-Controller aktualisieren
        viewController.setCurrentXMin(newXMin);
        viewController.setCurrentXMax(newXMax);
        viewController.setCurrentYMin(newYMin);
        viewController.setCurrentYMax(newYMax);

        // Neuen Plot mit verschobenem Wertebereich zeichnen
        mainPanel.renderPlot();

        debug("Wertebereich verschoben nach: x=[" + formatDouble(newXMin) + "," + formatDouble(newXMax) +
                "], y=[" + formatDouble(newYMin) + "," + formatDouble(newYMax) + "]");
    }

    /**
     * Verarbeitet Zoom-Operationen mit der Mausposition als Zentrum
     * 
     * @param wheelRotation Mausrad-Drehung (positiv = zoom out, negativ = zoom in)
     * @param mousePosition Position des Mauszeigers
     */
    private void handleZoom(int wheelRotation, Point mousePosition) {
        // Aktuelle Wertebereiche holen
        double xMin = viewController.getCurrentXMin();
        double xMax = viewController.getCurrentXMax();
        double yMin = viewController.getCurrentYMin();
        double yMax = viewController.getCurrentYMax();

        // Position des Mauszeigers im Wertebereich berechnen
        double viewWidth = plotPanel.getWidth();
        double viewHeight = plotPanel.getHeight();

        // Relative Position des Mauszeigers (0-1 im sichtbaren Bereich)
        double relMouseX = (double) mousePosition.x / viewWidth;
        double relMouseY = 1.0 - (double) mousePosition.y / viewHeight; // Y ist umgekehrt

        // Absolute Position im Wertebereich
        double mouseWorldX = xMin + relMouseX * (xMax - xMin);
        double mouseWorldY = yMin + relMouseY * (yMax - yMin);

        // Zoom-Faktor basierend auf Mausrad-Richtung
        double zoomFactor = wheelRotation < 0 ? 0.8 : 1.25; // Zoomen wir rein oder raus?

        // Neue Wertebereiche berechnen
        double newXMin = mouseWorldX - (mouseWorldX - xMin) * zoomFactor;
        double newXMax = mouseWorldX + (xMax - mouseWorldX) * zoomFactor;
        double newYMin = mouseWorldY - (mouseWorldY - yMin) * zoomFactor;
        double newYMax = mouseWorldY + (yMax - mouseWorldY) * zoomFactor;

        // Sicherstellen, dass die neuen Bereiche gültig sind (Mindestgröße)
        double minRange = 0.001; // Vermeidet zu starkes Hineinzoomen
        if (newXMax - newXMin < minRange || newYMax - newYMin < minRange) {
            debug("Zu starkes Zoomen verhindert");
            return;
        }

        // Werte im View-Controller aktualisieren
        viewController.setCurrentXMin(newXMin);
        viewController.setCurrentXMax(newXMax);
        viewController.setCurrentYMin(newYMin);
        viewController.setCurrentYMax(newYMax);

        // Neu rendern
        mainPanel.renderPlot();

        debug("Wertebereich geändert durch Mausrad bei Position (" +
                mousePosition.x + "," + mousePosition.y + "): " +
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