
package plugins.plotter3d.interaction;

import javax.swing.*;
import javax.swing.event.ChangeListener;

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
     * und aktualisiert die Schieberegler in der UI
     * 
     * @param dx            Horizontale Mausbewegung
     * @param dy            Vertikale Mausbewegung
     * @param mousePosition Aktuelle Position des Mauszeigers
     */
    private void handleRotation(int dx, int dy, Point mousePosition) {
        // Aktuelle Rotationswinkel holen
        double currentRotationX = viewController.getCurrentRotationX();
        double currentRotationY = viewController.getCurrentRotationY();
        double currentRotationZ = viewController.getCurrentRotationZ();

        // Für Debug-Zwecke - Ausgangsrotation speichern
        double startRotX = currentRotationX;
        double startRotY = currentRotationY;
        double startRotZ = currentRotationZ;

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
        double centerX = plotPanel.getWidth() / 2.0;
        double centerY = plotPanel.getHeight() / 2.0;

        // Abstand zur Mitte berechnen (0 = Zentrum, 1 = Rand)
        double distanceX = Math.abs(mousePosition.x - centerX) / centerX;
        double distanceY = Math.abs(mousePosition.y - centerY) / centerY;

        // Stelle sicher, dass wir im gültigen Bereich bleiben
        distanceX = Math.min(distanceX, 1.0);
        distanceY = Math.min(distanceY, 1.0);

        // Rotationsgeschwindigkeit basierend auf Position anpassen
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

        // Schieberegler in der UI aktualisieren
        updateRotationSliders(newRotationX, currentRotationY, newRotationZ);

        // Aktualisiere Rotation im Renderer
        viewController.updateRotation();

        // Plot neu zeichnen
        plotPanel.repaint();

        // Sicherstellen, dass das Panel vollständig aktualisiert wird
        if (mainPanel != null) {
            mainPanel.renderPlot();
        }

        // Erweiterte Debug-Ausgabe
        debugRotation(startRotX, startRotY, startRotZ,
                newRotationX, currentRotationY, newRotationZ,
                dx, dy);
    }

    /**
     * Erweiterte Debug-Ausgabe für Rotationen
     */
    private void debugRotation(double oldRotX, double oldRotY, double oldRotZ,
            double newRotX, double newRotY, double newRotZ,
            int mouseDx, int mouseDy) {

        // Grundlegende Rotation-Info
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("Rotation geändert: ");
        debugInfo.append(String.format("X=%.1f→%.1f, Y=%.1f, Z=%.1f→%.1f ",
                oldRotX, newRotX, newRotY, oldRotZ, newRotZ));
        debugInfo.append(String.format("(Maus dx=%d, dy=%d)", mouseDx, mouseDy));

        // Berechnen der Rotationsmatrix für detailliertere Analyse
        double[] axisAngle = calculateRotationAxisAngle(newRotX, newRotY, newRotZ);

        debugInfo.append("\nRotationsanalyse: ");

        // Ausgabe der Rotationsachse und des Rotationswinkels
        if (axisAngle != null) {
            double axisX = axisAngle[0];
            double axisY = axisAngle[1];
            double axisZ = axisAngle[2];
            double angle = axisAngle[3];

            debugInfo.append(String.format("Hauptrotation: %.1f° um Achse [%.2f, %.2f, %.2f]",
                    angle, axisX, axisY, axisZ));

            // Interpretation der Rotation
            debugInfo.append(" - ");

            // Bestimme die dominante Achse
            if (Math.abs(axisX) > Math.abs(axisY) && Math.abs(axisX) > Math.abs(axisZ)) {
                debugInfo.append(axisX > 0 ? "Kippung nach rechts" : "Kippung nach links");
            } else if (Math.abs(axisY) > Math.abs(axisX) && Math.abs(axisY) > Math.abs(axisZ)) {
                debugInfo.append(axisY > 0 ? "Drehung im Uhrzeigersinn" : "Drehung gegen Uhrzeigersinn");
            } else if (Math.abs(axisZ) > Math.abs(axisX) && Math.abs(axisZ) > Math.abs(axisY)) {
                debugInfo.append(axisZ > 0 ? "Kippung nach hinten" : "Kippung nach vorne");
            } else {
                debugInfo.append("Komplexe Rotation");
            }
        } else {
            debugInfo.append("Keine signifikante Rotation");
        }

        // Zusätzliche Orientierungsinfo
        double[][] rotMatrix = calculateRotationMatrix(newRotX, newRotY, newRotZ);
        if (rotMatrix != null) {
            // Z-Vektor gibt die "Oben"-Richtung der Kamera an
            double upX = rotMatrix[2][0];
            double upY = rotMatrix[2][1];
            double upZ = rotMatrix[2][2];

            // Bestimme die aktuelle Orientierung des Graphen relativ zur Kamera
            debugInfo.append("\nGraph-Orientierung: ");

            // Vereinfachte Orientierungsanalyse
            if (upZ > 0.7) {
                debugInfo.append("Aufsicht");
            } else if (upZ < -0.7) {
                debugInfo.append("Untersicht");
            } else if (upX > 0.7) {
                debugInfo.append("Seitenansicht rechts");
            } else if (upX < -0.7) {
                debugInfo.append("Seitenansicht links");
            } else if (upY > 0.7) {
                debugInfo.append("Frontansicht");
            } else if (upY < -0.7) {
                debugInfo.append("Rückansicht");
            } else {
                debugInfo.append("Schräge Perspektive");
            }
        }

        debug(debugInfo.toString());
    }

    /**
     * Berechnet die Rotationsmatrix für die gegebenen Euler-Winkel
     * In der Rotationsfolge Z → Y → X
     */
    private double[][] calculateRotationMatrix(double rotX, double rotY, double rotZ) {
        // Umwandeln in Radiant
        double rx = Math.toRadians(rotX);
        double ry = Math.toRadians(rotY);
        double rz = Math.toRadians(rotZ);

        // Sinus und Kosinus vorberechnen
        double sx = Math.sin(rx);
        double cx = Math.cos(rx);
        double sy = Math.sin(ry);
        double cy = Math.cos(ry);
        double sz = Math.sin(rz);
        double cz = Math.cos(rz);

        // 3x3 Rotationsmatrix
        double[][] matrix = new double[3][3];

        // Berechnung der Rotationsmatrix - Z→Y→X Reihenfolge
        // Erste Spalte
        matrix[0][0] = cy * cz;
        matrix[1][0] = cy * sz;
        matrix[2][0] = -sy;

        // Zweite Spalte
        matrix[0][1] = sx * sy * cz - cx * sz;
        matrix[1][1] = sx * sy * sz + cx * cz;
        matrix[2][1] = sx * cy;

        // Dritte Spalte
        matrix[0][2] = cx * sy * cz + sx * sz;
        matrix[1][2] = cx * sy * sz - sx * cz;
        matrix[2][2] = cx * cy;

        return matrix;
    }

    /**
     * Berechnet die Achse-Winkel-Darstellung der Rotation
     * 
     * @return Array mit 4 Elementen: [achseX, achseY, achseZ, winkel]
     */
    private double[] calculateRotationAxisAngle(double rotX, double rotY, double rotZ) {
        // Berechne erst die Rotationsmatrix
        double[][] rotMatrix = calculateRotationMatrix(rotX, rotY, rotZ);

        // Der Rotationswinkel kann aus der Spur (trace) der Matrix abgeleitet werden
        double trace = rotMatrix[0][0] + rotMatrix[1][1] + rotMatrix[2][2];
        double angle = Math.acos((trace - 1) / 2);

        // Wenn der Winkel zu klein ist, gibt es keine signifikante Rotation
        if (Math.abs(angle) < 0.001 || Math.abs(angle - Math.PI) < 0.001) {
            return null;
        }

        // Berechne die Rotationsachse
        double x = (rotMatrix[2][1] - rotMatrix[1][2]) / (2 * Math.sin(angle));
        double y = (rotMatrix[0][2] - rotMatrix[2][0]) / (2 * Math.sin(angle));
        double z = (rotMatrix[1][0] - rotMatrix[0][1]) / (2 * Math.sin(angle));

        // Normalisiere die Achse
        double length = Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;

        // Konvertiere den Winkel in Grad
        angle = Math.toDegrees(angle);

        return new double[] { x, y, z, angle };
    }

    /**
     * Aktualisiert die Rotations-Schieberegler in der UI
     * 
     * @param rotX X-Rotationswert
     * @param rotY Y-Rotationswert
     * @param rotZ Z-Rotationswert
     */
    private void updateRotationSliders(double rotX, double rotY, double rotZ) {
        try {
            // Hole Referenzen auf die Schieberegler aus dem UI-Builder
            JSlider rotXSlider = mainPanel.getUIBuilder().getRotationXSlider();
            JSlider rotYSlider = mainPanel.getUIBuilder().getRotationYSlider();
            JSlider rotZSlider = mainPanel.getUIBuilder().getRotationZSlider();

            JLabel rotXLabel = mainPanel.getUIBuilder().getRotationXLabel();
            JLabel rotYLabel = mainPanel.getUIBuilder().getRotationYLabel();
            JLabel rotZLabel = mainPanel.getUIBuilder().getRotationZLabel();

            // Wenn die Schieberegler existieren, ihre Werte aktualisieren
            if (rotXSlider != null) {
                // Verhindere Event-Schleifen, indem wir ChangeListener vorübergehend entfernen
                ChangeListener[] xListeners = rotXSlider.getChangeListeners();
                for (ChangeListener listener : xListeners) {
                    rotXSlider.removeChangeListener(listener);
                }

                // Setze neuen Wert
                rotXSlider.setValue((int) rotX);

                // Füge Listener wieder hinzu
                for (ChangeListener listener : xListeners) {
                    rotXSlider.addChangeListener(listener);
                }

                // Beschriftung aktualisieren
                if (rotXLabel != null) {
                    rotXLabel.setText("X: " + (int) rotX + "°");
                }
            }

            if (rotYSlider != null) {
                // Verhindere Event-Schleifen
                ChangeListener[] yListeners = rotYSlider.getChangeListeners();
                for (ChangeListener listener : yListeners) {
                    rotYSlider.removeChangeListener(listener);
                }

                rotYSlider.setValue((int) rotY);

                for (ChangeListener listener : yListeners) {
                    rotYSlider.addChangeListener(listener);
                }

                if (rotYLabel != null) {
                    rotYLabel.setText("Y: " + (int) rotY + "°");
                }
            }

            if (rotZSlider != null) {
                // Verhindere Event-Schleifen
                ChangeListener[] zListeners = rotZSlider.getChangeListeners();
                for (ChangeListener listener : zListeners) {
                    rotZSlider.removeChangeListener(listener);
                }

                rotZSlider.setValue((int) rotZ);

                for (ChangeListener listener : zListeners) {
                    rotZSlider.addChangeListener(listener);
                }

                if (rotZLabel != null) {
                    rotZLabel.setText("Z: " + (int) rotZ + "°");
                }
            }

        } catch (Exception e) {
            // Falls ein Fehler auftritt (z.B. fehlende UI-Komponenten)
            debug("Fehler bei der Aktualisierung der Schieberegler: " + e.getMessage());
        }
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
        // Rotationswinkel in Radiant
        double rotX = Math.toRadians(viewController.getCurrentRotationX());
        double rotY = Math.toRadians(viewController.getCurrentRotationY());
        double rotZ = Math.toRadians(viewController.getCurrentRotationZ());

        // Sinus und Kosinus der Rotationswinkel vorberechnen
        double sinX = Math.sin(rotX);
        double cosX = Math.cos(rotX);
        double sinY = Math.sin(rotY);
        double cosY = Math.cos(rotY);
        double sinZ = Math.sin(rotZ);
        double cosZ = Math.cos(rotZ);

        // Skalierungsfaktor für die Mausbewegung
        double xRange = viewController.getCurrentXMax() - viewController.getCurrentXMin();
        double yRange = viewController.getCurrentYMax() - viewController.getCurrentYMin();
        double scaleX = xRange / plotPanel.getWidth();
        double scaleY = yRange / plotPanel.getHeight();

        // Die Basis-Bewegung im Bildschirm, negiert für natürliche Bewegung
        double screenDx = -dx * scaleX;
        double screenDy = dy * scaleY; // Y ist auf dem Bildschirm invertiert

        // Die Basis-Verschiebungsvektoren im Weltraum vor jeglicher Rotation
        // Diese Vektoren werden durch die Rotation transformiert
        double[] rightVector = { 1, 0, 0 }; // Rechts-Vektor (entlang x-Achse)
        double[] upVector = { 0, 1, 0 }; // Hoch-Vektor (entlang y-Achse)

        // Transformiere diese Vektoren durch die Rotationsoperationen
        // In der gleichen Reihenfolge wie im Renderer: Z → Y → X

        // 1. Z-Rotation auf den rechten Vektor anwenden
        double rightX = rightVector[0] * cosZ - rightVector[1] * sinZ;
        double rightY = rightVector[0] * sinZ + rightVector[1] * cosZ;
        double rightZ = rightVector[2];

        // 2. Y-Rotation auf den resultierenden Vektor anwenden
        double tempX = rightX * cosY + rightZ * sinY;
        double tempZ = -rightX * sinY + rightZ * cosY;
        rightX = tempX;
        rightZ = tempZ;

        // 3. X-Rotation auf den resultierenden Vektor anwenden
        tempZ = rightY * sinX + rightZ * cosX;
        double tempY = rightY * cosX - rightZ * sinX;
        rightY = tempY;
        rightZ = tempZ;

        // Das Gleiche nun für den up-Vektor

        // 1. Z-Rotation auf den up-Vektor anwenden
        double upX = upVector[0] * cosZ - upVector[1] * sinZ;
        double upY = upVector[0] * sinZ + upVector[1] * cosZ;
        double upZ = upVector[2];

        // 2. Y-Rotation auf den resultierenden Vektor anwenden
        tempX = upX * cosY + upZ * sinY;
        tempZ = -upX * sinY + upZ * cosY;
        upX = tempX;
        upZ = tempZ;

        // 3. X-Rotation auf den resultierenden Vektor anwenden
        tempZ = upY * sinX + upZ * cosX;
        tempY = upY * cosX - upZ * sinX;
        upY = tempY;
        upZ = tempZ;

        // Berechne die Weltverschiebung durch Projektion der Bildschirmbewegung
        // auf die transformierten Einheitsvektoren
        double worldDx = screenDx * rightX + screenDy * upX;
        double worldDy = screenDx * rightY + screenDy * upY;

        // Grenzen des Graphen aktualisieren
        double newXMin = viewController.getCurrentXMin() + worldDx;
        double newXMax = viewController.getCurrentXMax() + worldDx;
        double newYMin = viewController.getCurrentYMin() + worldDy;
        double newYMax = viewController.getCurrentYMax() + worldDy;

        // Ansicht aktualisieren
        viewController.setCurrentXMin(newXMin);
        viewController.setCurrentXMax(newXMax);
        viewController.setCurrentYMin(newYMin);
        viewController.setCurrentYMax(newYMax);

        // Neu zeichnen
        mainPanel.renderPlot();

        debug("Panning: Screen (dx=" + dx + ", dy=" + dy + ") -> World (" +
                formatDouble(worldDx) + ", " + formatDouble(worldDy) + ")");
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