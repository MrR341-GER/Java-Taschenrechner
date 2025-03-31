package plugins.plotter2d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * Behandelt das Zeichnen des Gitters und der Achsen für das GraphPanel
 */
public class GridRenderer {
    private final GraphPanel panel;
    private final CoordinateTransformer transformer;

    // Konstanten
    private static final int TICK_LENGTH = 5; // Länge der Achsenstriche

    public GridRenderer(GraphPanel panel, CoordinateTransformer transformer) {
        this.panel = panel;
        this.transformer = transformer;
    }

    /**
     * Zeichnet das Koordinatengitter
     */
    public void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // Hellgrau
        g2d.setStroke(new BasicStroke(0.5f));

        // Berechnet den Abstand der Gitterlinien in Weltkoordinaten – verwendet den
        // Y-Bereich für beide Achsen
        double gridSpacing = calculateGridSpacing(transformer.getYMax() - transformer.getYMin());

        // Verfügbare Zeichenfläche
        int drawingWidth = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int drawingHeight = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        // X-Gitterlinien
        double x = Math.ceil(transformer.getXMin() / gridSpacing) * gridSpacing;
        while (x <= transformer.getXMax()) {
            int screenX = transformer.worldToScreenX(x);
            g2d.draw(new Line2D.Double(screenX, transformer.getYOffset(),
                    screenX, transformer.getYOffset() + drawingHeight));
            x += gridSpacing;
        }

        // Y-Gitterlinien
        double y = Math.ceil(transformer.getYMin() / gridSpacing) * gridSpacing;
        while (y <= transformer.getYMax()) {
            int screenY = transformer.worldToScreenY(y);
            g2d.draw(new Line2D.Double(transformer.getXOffset(), screenY,
                    transformer.getXOffset() + drawingWidth, screenY));
            y += gridSpacing;
        }
    }

    /**
     * Zeichnet die X- und Y-Achsen mit Beschriftungen
     */
    public void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));

        // Verfügbare Zeichenfläche
        int drawingWidth = panel.getWidth() - 2 * GraphPanel.AXIS_MARGIN;
        int drawingHeight = panel.getHeight() - 2 * GraphPanel.AXIS_MARGIN;

        // X-Achse
        int yAxisPos = transformer.worldToScreenY(0);
        if (yAxisPos < transformer.getYOffset())
            yAxisPos = transformer.getYOffset();
        if (yAxisPos > transformer.getYOffset() + drawingHeight)
            yAxisPos = transformer.getYOffset() + drawingHeight;

        g2d.draw(new Line2D.Double(transformer.getXOffset(), yAxisPos,
                transformer.getXOffset() + drawingWidth, yAxisPos));

        // Y-Achse
        int xAxisPos = transformer.worldToScreenX(0);
        if (xAxisPos < transformer.getXOffset())
            xAxisPos = transformer.getXOffset();
        if (xAxisPos > transformer.getXOffset() + drawingWidth)
            xAxisPos = transformer.getXOffset() + drawingWidth;

        g2d.draw(new Line2D.Double(xAxisPos, transformer.getYOffset(),
                xAxisPos, transformer.getYOffset() + drawingHeight));

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // Gemeinsamer Abstand für beide Achsen
        double gridSpacing = calculateGridSpacing(transformer.getYMax() - transformer.getYMin());

        // X-Achsenstriche und Beschriftungen
        double x = Math.ceil(transformer.getXMin() / gridSpacing) * gridSpacing;

        while (x <= transformer.getXMax()) {
            int screenX = transformer.worldToScreenX(x);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenX >= transformer.getXOffset() && screenX <= transformer.getXOffset() + drawingWidth) {
                // Strich
                g2d.draw(new Line2D.Double(screenX, yAxisPos - TICK_LENGTH, screenX, yAxisPos + TICK_LENGTH));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(x) > 1e-10) { // Kleine Werte als null betrachten
                    String label = transformer.getAxisFormat().format(x);
                    FontMetrics fm = g2d.getFontMetrics();

                    // Speichere den aktuellen Transformationszustand
                    AffineTransform originalTransform = g2d.getTransform();

                    // Konstanter Abstand für alle Beschriftungen
                    int yOffset = 5; // Abstand vom Strich

                    // Positioniere den Text so, dass er UNTER der Achse beginnt
                    g2d.translate(screenX, yAxisPos + TICK_LENGTH + yOffset);
                    g2d.rotate(Math.PI / 2); // 90 Grad im Uhrzeigersinn

                    // Zeichne den Text zentriert an der Linie
                    g2d.drawString(label, 0, 0);

                    // Stelle den ursprünglichen Transformationszustand wieder her
                    g2d.setTransform(originalTransform);
                }
            }
            x += gridSpacing;
        }

        // Y-Achsenstriche und Beschriftungen
        double y = Math.ceil(transformer.getYMin() / gridSpacing) * gridSpacing;

        while (y <= transformer.getYMax()) {
            int screenY = transformer.worldToScreenY(y);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenY >= transformer.getYOffset() && screenY <= transformer.getYOffset() + drawingHeight) {
                // Strich
                g2d.draw(new Line2D.Double(xAxisPos - TICK_LENGTH, screenY, xAxisPos + TICK_LENGTH, screenY));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(y) > 1e-10) { // Kleine Werte als null betrachten
                    String label = transformer.getAxisFormat().format(y);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, xAxisPos - labelWidth - 5, screenY + 4);
                }
            }
            y += gridSpacing;
        }

        // Ursprungsbeschriftung
        if (xAxisPos >= transformer.getXOffset() && xAxisPos <= transformer.getXOffset() + drawingWidth &&
                yAxisPos >= transformer.getYOffset() && yAxisPos <= transformer.getYOffset() + drawingHeight) {
            g2d.drawString("0", xAxisPos + 4, yAxisPos + 12);
        }

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("x", transformer.getXOffset() + drawingWidth + 10, yAxisPos + 4);
        g2d.drawString("y", xAxisPos - 4, transformer.getYOffset() - 10);
    }

    /**
     * Berechnet einen passenden Abstand für Gitterlinien
     */
    public double calculateGridSpacing(double range) {
        // Ziel: Etwa 10 Gitterlinien im sichtbaren Bereich
        double rawSpacing = range / 10;

        // Normalisiere auf Zehnerpotenzen
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Runde auf „schöne“ Werte: 1, 2, 5, 10
        if (mantissa < 1.5)
            return Math.pow(10, exponent);
        else if (mantissa < 3.5)
            return 2 * Math.pow(10, exponent);
        else if (mantissa < 7.5)
            return 5 * Math.pow(10, exponent);
        else
            return 10 * Math.pow(10, exponent);
    }
}
