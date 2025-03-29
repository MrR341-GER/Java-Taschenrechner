import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Verbesserter Renderer für 3D-Funktionsgraphen
 * Implementiert ein richtiges Koordinatensystem mit Achsen, Teilstrichen und
 * Beschriftungen
 * Passt den Wertebereich beim Zoomen automatisch an
 */
public class Plot3DRenderer {
    private Function3DParser function;
    private double xMin, xMax, yMin, yMax;
    private double originalXMin, originalXMax, originalYMin, originalYMax; // Ursprüngliche Grenzen für Reset
    private int resolution; // Anzahl der Punkte pro Achse
    private double rotationX; // Rotation um die X-Achse in Grad
    private double rotationY; // Rotation um die Y-Achse in Grad
    private double rotationZ; // Rotation um die Z-Achse in Grad
    private double scale; // Skalierungsfaktor
    private double zMin, zMax; // Automatisch berechnete Z-Grenzen
    private Point3D[][][] gridPoints; // [x][y][3] - Speichert originale, transformierte und projizierte Koordinaten

    // Eigenschaften des Koordinatensystems
    private boolean showCoordinateSystem = true;
    private boolean showGrid = true;
    private static final double AXIS_EXTENSION = 1.3; // Achsen über Datengrenzen hinaus verlängern
    private static final int NUM_TICKS = 5; // Anzahl der Teilstriche pro Achse

    // Farbschema
    private Color gridColor = new Color(200, 200, 200, 100); // Halbtransparentes Gitter
    private Color axisColor = Color.BLACK; // Einheitliche Farbe für alle Achsen
    private Color tickColor = Color.BLACK;
    private Color labelColor = Color.BLACK;
    private Color gridLineColor = new Color(220, 220, 220, 80); // Noch transparentere Gitterlinien
    private GradientColorScheme colorScheme;

    // Konstanten
    private static final int ORIGINAL = 0;
    private static final int TRANSFORMED = 1;
    private static final int PROJECTED = 2;
    private static final int TICK_LENGTH = 5; // Länge der Teilstriche in Pixeln

    /**
     * Konstruktor für den 3D-Renderer
     * 
     * @param functionExpression Funktionsausdruck (z = f(x,y))
     * @param xMin               Minimaler X-Wert
     * @param xMax               Maximaler X-Wert
     * @param yMin               Minimaler Y-Wert
     * @param yMax               Maximaler Y-Wert
     * @param resolution         Anzahl der Punkte pro Achse
     */
    public Plot3DRenderer(String functionExpression, double xMin, double xMax,
            double yMin, double yMax, int resolution) {
        this.function = new Function3DParser(functionExpression);
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        // Originalgrenzen für Reset speichern
        this.originalXMin = xMin;
        this.originalXMax = xMax;
        this.originalYMin = yMin;
        this.originalYMax = yMax;
        this.resolution = Math.max(10, Math.min(100, resolution)); // Min 10, Max 100
        this.rotationX = 30; // Standardrotation
        this.rotationY = 0;
        this.rotationZ = 30;
        this.scale = 1.0;

        // Standardfarbschema erstellen
        this.colorScheme = new GradientColorScheme(
                new Color(0, 0, 255), // Blau (niedrige Werte)
                new Color(0, 255, 255), // Cyan
                new Color(0, 255, 0), // Grün
                new Color(255, 255, 0), // Gelb
                new Color(255, 0, 0) // Rot (hohe Werte)
        );

        // Gitterpunkte initialisieren und Funktionswerte berechnen
        calculateFunctionValues();
    }

    /**
     * Berechnet alle Funktionswerte für das Gitter
     */
    private void calculateFunctionValues() {
        gridPoints = new Point3D[resolution][resolution][3];

        double xStep = (xMax - xMin) / (resolution - 1);
        double yStep = (yMax - yMin) / (resolution - 1);

        zMin = Double.POSITIVE_INFINITY;
        zMax = Double.NEGATIVE_INFINITY;

        // Berechne alle Punkte und finde zMin/zMax
        for (int i = 0; i < resolution; i++) {
            double x = xMin + i * xStep;

            for (int j = 0; j < resolution; j++) {
                double y = yMin + j * yStep;

                // Sichere Funktionsauswertung mit Fehlerbehandlung
                double z;
                try {
                    z = function.evaluateAt(x, y);
                    if (Double.isNaN(z) || Double.isInfinite(z)) {
                        z = 0; // Problematische Werte auf 0 setzen
                    }
                } catch (Exception e) {
                    z = 0; // Bei Fehlern auf 0 setzen
                }

                gridPoints[i][j][ORIGINAL] = new Point3D(x, y, z);

                // Min/Max Z-Werte aktualisieren
                if (z < zMin)
                    zMin = z;
                if (z > zMax)
                    zMax = z;
            }
        }

        // Verhindere zu kleine Z-Bereiche
        if (Math.abs(zMax - zMin) < 1e-10) {
            zMax = zMin + 1.0;
        }

        // Transformiere und projiziere alle Punkte
        transformAndProjectPoints();
    }

    /**
     * Transformiert und projiziert alle Punkte basierend auf Rotation und
     * Skalierung
     */
    private void transformAndProjectPoints() {
        // Winkel in Radianten umrechnen
        double angleX = Math.toRadians(rotationX);
        double angleY = Math.toRadians(rotationY);
        double angleZ = Math.toRadians(rotationZ);

        // Sinus und Kosinus für Effizienz vorberechnen
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Mittelpunkte für die Zentrierung berechnen
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;
        double zCenter = (zMax + zMin) / 2;

        // Normalisierungsfaktor, um Koordinaten in einen relativen Bereich zu bringen
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        double zRange = zMax - zMin;
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Jeden Punkt transformieren und projizieren
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                Point3D original = gridPoints[i][j][ORIGINAL];

                // Punkt um den Ursprung zentrieren und normalisieren
                double x = (original.x - xCenter) * factor;
                double y = (original.y - yCenter) * factor;
                double z = (original.z - zCenter) * factor;

                // Skalieren
                x *= scale;
                y *= scale;
                z *= scale;

                // Rotation um Z-Achse
                double tempX = x * cosZ - y * sinZ;
                double tempY = x * sinZ + y * cosZ;
                x = tempX;
                y = tempY;

                // Rotation um Y-Achse
                tempX = x * cosY + z * sinY;
                double tempZ = -x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // Rotation um X-Achse
                tempY = y * cosX - z * sinX;
                tempZ = y * sinX + z * cosX;
                y = tempY;
                z = tempZ;

                // Transformierten Punkt speichern
                gridPoints[i][j][TRANSFORMED] = new Point3D(x, y, z);

                // Punkt auf 2D-Ebene projizieren (einfache Parallelprojektion)
                gridPoints[i][j][PROJECTED] = new Point3D(x, y, 0);
            }
        }
    }

    /**
     * Rendert den 3D-Plot auf der übergebenen Graphics2D-Instanz
     */
    public void render(Graphics2D g2d, int width, int height) {
        // Anti-Aliasing für glattere Linien aktivieren
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Skalierungsfaktoren für die Anzeige berechnen
        double displayScale = Math.min(width, height) * 0.4;
        int xOffset = width / 2;
        int yOffset = height / 2;

        // Zuerst das Koordinatensystem zeichnen (falls aktiviert)
        if (showCoordinateSystem) {
            if (showGrid) {
                drawCoordinateGrid(g2d, displayScale, xOffset, yOffset);
            }
            drawAxes(g2d, displayScale, xOffset, yOffset);
            drawTicksAndLabels(g2d, displayScale, xOffset, yOffset);
        }

        // Das Funktionsgitter zeichnen
        drawGrid(g2d, displayScale, xOffset, yOffset);

        // Beschriftungen zeichnen
        drawLabels(g2d, width, height);
    }

    /**
     * Zeichnet die Koordinatengitter-Ebenen (XY, YZ, XZ)
     */
    private void drawCoordinateGrid(Graphics2D g2d, double displayScale, int xOffset, int yOffset) {
        g2d.setColor(gridLineColor);
        g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] { 2.0f, 2.0f }, 0.0f));

        // Teilstrichabstände berechnen
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        double zRange = zMax - zMin;

        double xStep = xRange / (NUM_TICKS - 1);
        double yStep = yRange / (NUM_TICKS - 1);
        double zStep = zRange / (NUM_TICKS - 1);

        // Transformationsmatrizen erhalten (vereinfacht als einzelne Transformationen)
        double angleX = Math.toRadians(rotationX);
        double angleY = Math.toRadians(rotationY);
        double angleZ = Math.toRadians(rotationZ);

        // Sinus- und Kosinuswerte vorberechnen
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Normalisierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Mittelpunkte für die Zentrierung berechnen
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;
        double zCenter = (zMax + zMin) / 2;

        // Gitterlinien auf der XY-Ebene zeichnen (Z = zMin)
        for (int i = 0; i < NUM_TICKS; i++) {
            // X-Gitterlinien
            double x1 = xMin;
            double x2 = xMax;
            double y = yMin + i * yStep;
            double z = zMin;

            // X-Linie an dieser Y-Position zeichnen
            drawTransformedLine(g2d, x1, y, z, x2, y, z,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);

            // Y-Gitterlinien
            double y1 = yMin;
            double y2 = yMax;
            double x = xMin + i * xStep;
            z = zMin;

            // Y-Linie an dieser X-Position zeichnen
            drawTransformedLine(g2d, x, y1, z, x, y2, z,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);
        }

        // Gitterlinien auf der XZ-Ebene zeichnen (Y = yMin)
        for (int i = 0; i < NUM_TICKS; i++) {
            // X-Gitterlinien
            double x1 = xMin;
            double x2 = xMax;
            double y = yMin;
            double z = zMin + i * zStep;

            // X-Linie an dieser Z-Position zeichnen
            drawTransformedLine(g2d, x1, y, z, x2, y, z,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);

            // Z-Gitterlinien
            double z1 = zMin;
            double z2 = zMax;
            double x = xMin + i * xStep;
            y = yMin;

            // Z-Linie an dieser X-Position zeichnen
            drawTransformedLine(g2d, x, y, z1, x, y, z2,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);
        }

        // Gitterlinien auf der YZ-Ebene zeichnen (X = xMin)
        for (int i = 0; i < NUM_TICKS; i++) {
            // Y-Gitterlinien
            double y1 = yMin;
            double y2 = yMax;
            double x = xMin;
            double z = zMin + i * zStep;

            // Y-Linie an dieser Z-Position zeichnen
            drawTransformedLine(g2d, x, y1, z, x, y2, z,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);

            // Z-Gitterlinien
            double z1 = zMin;
            double z2 = zMax;
            x = xMin;
            double y = yMin + i * yStep;

            // Z-Linie an dieser Y-Position zeichnen
            drawTransformedLine(g2d, x, y, z1, x, y, z2,
                    xCenter, yCenter, zCenter,
                    factor, scale,
                    sinX, cosX, sinY, cosY, sinZ, cosZ,
                    displayScale, xOffset, yOffset);
        }
    }

    /**
     * Hilfsmethode zum Zeichnen einer transformierten 3D-Linie
     */
    private void drawTransformedLine(Graphics2D g2d,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double xCenter, double yCenter, double zCenter,
            double factor, double scale,
            double sinX, double cosX,
            double sinY, double cosY,
            double sinZ, double cosZ,
            double displayScale, int xOffset, int yOffset) {
        // Punkt 1 transformieren
        double tx1 = (x1 - xCenter) * factor * scale;
        double ty1 = (y1 - yCenter) * factor * scale;
        double tz1 = (z1 - zCenter) * factor * scale;

        // Rotationen auf Punkt 1 anwenden
        double tempX = tx1 * cosZ - ty1 * sinZ;
        double tempY = tx1 * sinZ + ty1 * cosZ;
        tx1 = tempX;
        ty1 = tempY;

        tempX = tx1 * cosY + tz1 * sinY;
        double tempZ = -tx1 * sinY + tz1 * cosY;
        tx1 = tempX;
        tz1 = tempZ;

        tempY = ty1 * cosX - tz1 * sinX;
        tempZ = ty1 * sinX + tz1 * cosX;
        ty1 = tempY;
        tz1 = tempZ;

        // Punkt 2 transformieren
        double tx2 = (x2 - xCenter) * factor * scale;
        double ty2 = (y2 - yCenter) * factor * scale;
        double tz2 = (z2 - zCenter) * factor * scale;

        // Rotationen auf Punkt 2 anwenden
        tempX = tx2 * cosZ - ty2 * sinZ;
        tempY = tx2 * sinZ + ty2 * cosZ;
        tx2 = tempX;
        ty2 = tempY;

        tempX = tx2 * cosY + tz2 * sinY;
        tempZ = -tx2 * sinY + tz2 * cosY;
        tx2 = tempX;
        tz2 = tempZ;

        tempY = ty2 * cosX - tz2 * sinX;
        tempZ = ty2 * sinX + tz2 * cosX;
        ty2 = tempY;
        tz2 = tempZ;

        // Auf Bildschirmkoordinaten projizieren
        int screenX1 = xOffset + (int) (tx1 * displayScale);
        int screenY1 = yOffset - (int) (ty1 * displayScale);
        int screenX2 = xOffset + (int) (tx2 * displayScale);
        int screenY2 = yOffset - (int) (ty2 * displayScale);

        // Linie zeichnen
        g2d.drawLine(screenX1, screenY1, screenX2, screenY2);
    }

    /**
     * Zeichnet das 3D-Gitter als Drahtgittermodell
     */
    private void drawGrid(Graphics2D g2d, double displayScale, int xOffset, int yOffset) {
        // Linienstärke für das Gitter
        g2d.setStroke(new BasicStroke(1.0f));

        // Horizontale Linien zeichnen (entlang der X-Achse)
        for (int j = 0; j < resolution; j++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int i = 0; i < resolution; i++) {
                Point3D projected = gridPoints[i][j][PROJECTED];

                // Bildschirmkoordinaten berechnen
                int screenX = xOffset + (int) (projected.x * displayScale);
                int screenY = yOffset - (int) (projected.y * displayScale); // Y-Achse ist auf dem Bildschirm umgekehrt

                if (!started) {
                    path.moveTo(screenX, screenY);
                    started = true;
                } else {
                    path.lineTo(screenX, screenY);
                }

                // Farbe basierend auf Z-Wert bestimmen
                double normalizedZ = (gridPoints[i][j][ORIGINAL].z - zMin) / (zMax - zMin);
                Color color = colorScheme.getColorForValue(normalizedZ);
                g2d.setColor(color);
            }

            g2d.draw(path);
        }

        // Vertikale Linien zeichnen (entlang der Y-Achse)
        for (int i = 0; i < resolution; i++) {
            Path2D path = new Path2D.Double();
            boolean started = false;

            for (int j = 0; j < resolution; j++) {
                Point3D projected = gridPoints[i][j][PROJECTED];

                // Bildschirmkoordinaten berechnen
                int screenX = xOffset + (int) (projected.x * displayScale);
                int screenY = yOffset - (int) (projected.y * displayScale);

                if (!started) {
                    path.moveTo(screenX, screenY);
                    started = true;
                } else {
                    path.lineTo(screenX, screenY);
                }

                // Farbe basierend auf Z-Wert bestimmen
                double normalizedZ = (gridPoints[i][j][ORIGINAL].z - zMin) / (zMax - zMin);
                Color color = colorScheme.getColorForValue(normalizedZ);
                g2d.setColor(color);
            }

            g2d.draw(path);
        }
    }

    /**
     * Zeichnet die Koordinatenachsen durch den gesamten sichtbaren Bereich
     */
    private void drawAxes(Graphics2D g2d, double displayScale, int xOffset, int yOffset) {
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(axisColor);

        // Mittelpunkte für Zentrierung
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;
        double zCenter = (zMax + zMin) / 2;

        // Wertebereiche
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        double zRange = zMax - zMin;

        // Normalisierungsfaktor
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Transformationsparameter
        double angleX = Math.toRadians(rotationX);
        double angleY = Math.toRadians(rotationY);
        double angleZ = Math.toRadians(rotationZ);

        // Sinus/Kosinus vorberechnen
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // X-Achse durch den gesamten sichtbaren Bereich zeichnen
        double x1 = xMin;
        double y1 = 0; // X-Achse verläuft bei y=0
        double z1 = 0; // X-Achse verläuft bei z=0
        double x2 = xMax;
        double y2 = 0;
        double z2 = 0;

        // X-Achse transformieren und zeichnen
        drawTransformedLine(g2d, x1, y1, z1, x2, y2, z2,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                displayScale, xOffset, yOffset);

        // Y-Achse durch den gesamten sichtbaren Bereich zeichnen
        x1 = 0; // Y-Achse verläuft bei x=0
        y1 = yMin;
        z1 = 0; // Y-Achse verläuft bei z=0
        x2 = 0;
        y2 = yMax;
        z2 = 0;

        // Y-Achse transformieren und zeichnen
        drawTransformedLine(g2d, x1, y1, z1, x2, y2, z2,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                displayScale, xOffset, yOffset);

        // Z-Achse durch den gesamten sichtbaren Bereich zeichnen
        x1 = 0; // Z-Achse verläuft bei x=0
        y1 = 0; // Z-Achse verläuft bei y=0
        z1 = zMin;
        x2 = 0;
        y2 = 0;
        z2 = zMax;

        // Z-Achse transformieren und zeichnen
        drawTransformedLine(g2d, x1, y1, z1, x2, y2, z2,
                xCenter, yCenter, zCenter,
                factor, scale,
                sinX, cosX, sinY, cosY, sinZ, cosZ,
                displayScale, xOffset, yOffset);

        // Achsenbeschriftungen zeichnen
        Font originalFont = g2d.getFont();
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));

        // Transformiere Endpunkte der Achsen für Beschriftungen
        // X-Achse Endpunkt
        double tx = (xMax - xCenter) * factor * scale;
        double ty = (0 - yCenter) * factor * scale;
        double tz = (0 - zCenter) * factor * scale;

        double tempX = tx * cosZ - ty * sinZ;
        double tempY = tx * sinZ + ty * cosZ;
        tx = tempX;
        ty = tempY;

        tempX = tx * cosY + tz * sinY;
        double tempZ = -tx * sinY + tz * cosY;
        tx = tempX;
        tz = tempZ;

        tempY = ty * cosX - tz * sinX;
        tempZ = ty * sinX + tz * cosX;
        ty = tempY;

        int xLabelX = xOffset + (int) (tx * displayScale) + 10;
        int xLabelY = yOffset - (int) (ty * displayScale);
        g2d.drawString("X", xLabelX, xLabelY);

        // Y-Achse Endpunkt
        tx = (0 - xCenter) * factor * scale;
        ty = (yMax - yCenter) * factor * scale;
        tz = (0 - zCenter) * factor * scale;

        tempX = tx * cosZ - ty * sinZ;
        tempY = tx * sinZ + ty * cosZ;
        tx = tempX;
        ty = tempY;

        tempX = tx * cosY + tz * sinY;
        tempZ = -tx * sinY + tz * cosY;
        tx = tempX;
        tz = tempZ;

        tempY = ty * cosX - tz * sinX;
        tempZ = ty * sinX + tz * cosX;
        ty = tempY;

        int yLabelX = xOffset + (int) (tx * displayScale);
        int yLabelY = yOffset - (int) (ty * displayScale) - 10;
        g2d.drawString("Y", yLabelX, yLabelY);

        // Z-Achse Endpunkt
        tx = (0 - xCenter) * factor * scale;
        ty = (0 - yCenter) * factor * scale;
        tz = (zMax - zCenter) * factor * scale;

        tempX = tx * cosZ - ty * sinZ;
        tempY = tx * sinZ + ty * cosZ;
        tx = tempX;
        ty = tempY;

        tempX = tx * cosY + tz * sinY;
        tempZ = -tx * sinY + tz * cosY;
        tx = tempX;
        tz = tempZ;

        tempY = ty * cosX - tz * sinX;
        tempZ = ty * sinX + tz * cosX;
        ty = tempY;

        int zLabelX = xOffset + (int) (tx * displayScale);
        int zLabelY = yOffset - (int) (ty * displayScale);
        g2d.drawString("Z", zLabelX, zLabelY);

        // Ursprungsfarnung wiederherstellen
        g2d.setFont(originalFont);
    }

    /**
     * Zeichnet Teilstriche und Beschriftungen auf den Achsen
     */
    private void drawTicksAndLabels(Graphics2D g2d, double displayScale, int xOffset, int yOffset) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.setStroke(new BasicStroke(1.0f));

        // Format für Teilstrichbeschriftungen
        DecimalFormat df = new DecimalFormat("0.##");

        // Anzahl der Teilstriche pro Achse
        int numTicks = NUM_TICKS;

        // Teilstrichabstände berechnen
        double xStep = (xMax - xMin) / (numTicks - 1);
        double yStep = (yMax - yMin) / (numTicks - 1);
        double zStep = (zMax - zMin) / (numTicks - 1);

        // Transformationsparameter (wie bei den Achsen)
        double angleX = Math.toRadians(rotationX);
        double angleY = Math.toRadians(rotationY);
        double angleZ = Math.toRadians(rotationZ);

        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Mittelpunkte für die Zentrierung berechnen
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;
        double zCenter = (zMax + zMin) / 2;

        // Normalisierungsfaktor
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        double zRange = zMax - zMin;
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Teilstriche und Beschriftungen auf der X-Achse zeichnen
        for (int i = 0; i < numTicks; i++) {
            double tickValue = xMin + i * xStep;

            // Normalisierte und skalierte Position berechnen
            double x = (tickValue - xCenter) * factor * scale;
            double y = 0;
            double z = 0;

            // Rotationen anwenden
            double tx = x * cosZ - y * sinZ;
            double ty = x * sinZ + y * cosZ;

            double tempX = tx * cosY + z * sinY;
            double tempZ = -tx * sinY + z * cosY;
            tx = tempX;

            double tempY = ty * cosX - tempZ * sinX;
            ty = tempY;

            // In Bildschirmkoordinaten umrechnen
            int screenX = xOffset + (int) (tx * displayScale);
            int screenY = yOffset - (int) (ty * displayScale);

            // Teilstrich senkrecht zur Achse zeichnen
            g2d.setColor(tickColor);

            // Senkrechte Richtung berechnen (vereinfacht)
            int perpX = -TICK_LENGTH / 2;
            int perpY = -TICK_LENGTH / 2;

            g2d.drawLine(screenX - perpX, screenY - perpY, screenX + perpX, screenY + perpY);

            // Beschriftung zeichnen
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);

            g2d.drawString(label, screenX - labelWidth / 2, screenY + TICK_LENGTH + fm.getAscent());
        }

        // Teilstriche und Beschriftungen auf der Y-Achse zeichnen
        for (int i = 0; i < numTicks; i++) {
            double tickValue = yMin + i * yStep;

            // Normalisierte und skalierte Position berechnen
            double x = 0;
            double y = (tickValue - yCenter) * factor * scale;
            double z = 0;

            // Rotationen anwenden
            double tx = x * cosZ - y * sinZ;
            double ty = x * sinZ + y * cosZ;

            double tempX = tx * cosY + z * sinY;
            double tempZ = -tx * sinY + z * cosY;
            tx = tempX;

            double tempY = ty * cosX - tempZ * sinX;
            ty = tempY;

            // In Bildschirmkoordinaten umrechnen
            int screenX = xOffset + (int) (tx * displayScale);
            int screenY = yOffset - (int) (ty * displayScale);

            // Teilstrich zeichnen
            g2d.setColor(tickColor);

            // Senkrechte Richtung berechnen (vereinfacht)
            int perpX = TICK_LENGTH / 2;
            int perpY = -TICK_LENGTH / 2;

            g2d.drawLine(screenX - perpX, screenY - perpY, screenX + perpX, screenY + perpY);

            // Beschriftung zeichnen
            g2d.setColor(labelColor);
            String label = df.format(tickValue);

            g2d.drawString(label, screenX + TICK_LENGTH, screenY);
        }

        // Teilstriche und Beschriftungen auf der Z-Achse zeichnen
        for (int i = 0; i < numTicks; i++) {
            double tickValue = zMin + i * zStep;

            // Normalisierte und skalierte Position berechnen
            double x = 0;
            double y = 0;
            double z = (tickValue - zCenter) * factor * scale;

            // Rotationen anwenden
            double tx = x * cosZ - y * sinZ;
            double ty = x * sinZ + y * cosZ;

            double tempX = tx * cosY + z * sinY;
            double tempZ = -tx * sinY + z * cosY;
            tx = tempX;
            z = tempZ;

            double tempY = ty * cosX - z * sinX;
            z = ty * sinX + z * cosX;
            ty = tempY;

            // In Bildschirmkoordinaten umrechnen
            int screenX = xOffset + (int) (tx * displayScale);
            int screenY = yOffset - (int) (ty * displayScale);

            // Teilstrich zeichnen
            g2d.setColor(tickColor);

            // Senkrechte Richtung berechnen (vereinfacht)
            int perpX = TICK_LENGTH / 2;
            int perpY = TICK_LENGTH / 2;

            g2d.drawLine(screenX - perpX, screenY - perpY, screenX + perpX, screenY + perpY);

            // Beschriftung zeichnen
            g2d.setColor(labelColor);
            String label = df.format(tickValue);
            FontMetrics fm = g2d.getFontMetrics();

            g2d.drawString(label, screenX + TICK_LENGTH, screenY - TICK_LENGTH);
        }
    }

    /**
     * Zeichnet Beschriftungen für Achsen und Farbskala
     */
    private void drawLabels(Graphics2D g2d, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Legende für Achsen in der unteren rechten Ecke
        g2d.drawString("Achsen:", width - 80, height - 30);
        g2d.drawString("X - horizontal", width - 80, height - 20);
        g2d.drawString("Y - vertikal", width - 80, height - 10);
        g2d.drawString("Z - tiefe", width - 80, height);

        // Wertebereiche anzeigen
        g2d.drawString(String.format("X: [%.2f, %.2f]", xMin, xMax), 10, height - 40);
        g2d.drawString(String.format("Y: [%.2f, %.2f]", yMin, yMax), 10, height - 25);
        g2d.drawString(String.format("Z: [%.2f, %.2f]", zMin, zMax), 10, height - 10);

        // Farbskala zeichnen
        drawColorScale(g2d, width - 30, 50, 20, height - 100);
    }

    /**
     * Zeichnet eine Farbskala zur Anzeige der Z-Werte
     */
    private void drawColorScale(Graphics2D g2d, int x, int y, int width, int height) {
        // Farbskala zeichnen
        for (int i = 0; i < height; i++) {
            double normalizedValue = 1.0 - (double) i / height;
            g2d.setColor(colorScheme.getColorForValue(normalizedValue));
            g2d.fillRect(x, y + i, width, 1);
        }

        // Rahmen um die Skala
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height);

        // Beschriftungen für Min und Max
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        DecimalFormat df = new DecimalFormat("0.##");
        g2d.drawString(df.format(zMax), x + width + 2, y + 10);
        g2d.drawString(df.format(zMin), x + width + 2, y + height);
    }

    /**
     * Erstellt ein BufferedImage des 3D-Plots
     */
    public BufferedImage createImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Weißen Hintergrund setzen
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Plot rendern
        render(g2d, width, height);

        g2d.dispose();
        return image;
    }

    /**
     * Setzt die Rotation des Plots
     */
    public void setRotation(double rotationX, double rotationY, double rotationZ) {
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        transformAndProjectPoints();
    }

    /**
     * Setzt den Skalierungsfaktor
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.1, Math.min(10.0, scale)); // Skalierung begrenzen
        transformAndProjectPoints();
    }

    /**
     * Passt die Wertebereiche beim Zoomen an
     */
    private void adjustValueRangesForZoom() {
        // Berechnen, wie stark die Bereiche angepasst werden sollen
        // Beim Hineinzoomen (scale > 1) werden die Bereiche kleiner
        // Beim Herauszoomen (scale < 1) werden die Bereiche größer

        // Der Mittelpunkt der Ansicht soll stabil bleiben
        double xCenter = (xMax + xMin) / 2;
        double yCenter = (yMax + yMin) / 2;

        // Bereich basierend auf Skalierung anpassen
        // 1/scale gibt uns die inverse Beziehung, die wir wollen:
        // Größere Skalierung = kleinerer sichtbarer Bereich
        double adjustmentFactor = 1 / scale;

        // Originalbereiche verwenden, um korrekt zu skalieren
        double originalXRange = originalXMax - originalXMin;
        double originalYRange = originalYMax - originalYMin;

        // Neue Bereiche berechnen
        double newXRange = originalXRange * adjustmentFactor;
        double newYRange = originalYRange * adjustmentFactor;

        // Neue Minimum- und Maximumwerte um xCenter und yCenter zentriert setzen
        xMin = xCenter - newXRange / 2;
        xMax = xCenter + newXRange / 2;
        yMin = yCenter - newYRange / 2;
        yMax = yCenter + newYRange / 2;

        // Funktionswerte mit neuen Bereichen neu berechnen
        calculateFunctionValues();
    }

    /**
     * Ändert den Skalierungsfaktor (Zoom)
     */
    public void zoom(double factor) {
        setScale(scale * factor);
    }

    /**
     * Setzt eine neue Funktion
     */
    public void setFunction(String functionExpression) {
        try {
            this.function = new Function3DParser(functionExpression);
            calculateFunctionValues();
        } catch (Exception e) {
            // Behalte die alte Funktion bei, wenn die neue ungültig ist
            System.err.println("Fehler beim Setzen der Funktion: " + e.getMessage());
        }
    }

    /**
     * Setzt die Grenzen des Wertebereichs
     */
    public void setBounds(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        // Auch die originalen Grenzen aktualisieren
        this.originalXMin = xMin;
        this.originalXMax = xMax;
        this.originalYMin = yMin;
        this.originalYMax = yMax;

        calculateFunctionValues();
    }

    /**
     * Setzt die Auflösung des Gitters
     */
    public void setResolution(int resolution) {
        this.resolution = Math.max(10, Math.min(100, resolution));
        calculateFunctionValues();
    }

    /**
     * Setzt das Farbschema
     */
    public void setColorScheme(GradientColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    /**
     * Schaltet die Anzeige des Koordinatensystems ein/aus
     */
    public void setShowCoordinateSystem(boolean show) {
        this.showCoordinateSystem = show;
    }

    /**
     * Schaltet die Anzeige des Gitters ein/aus
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }

    /**
     * Getter-Methode für xMin
     */
    public double getXMin() {
        return xMin;
    }

    /**
     * Getter-Methode für xMax
     */
    public double getXMax() {
        return xMax;
    }

    /**
     * Getter-Methode für yMin
     */
    public double getYMin() {
        return yMin;
    }

    /**
     * Getter-Methode für yMax
     */
    public double getYMax() {
        return yMax;
    }

    /**
     * Hilfsklasse zum Speichern von 3D-Punkten
     */
    private static class Point3D {
        double x, y, z;

        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * Klasse für Farbverläufe basierend auf Z-Werten
     */
    public static class GradientColorScheme {
        private Color[] colors;

        public GradientColorScheme(Color... colors) {
            this.colors = colors;
        }

        public Color getColorForValue(double normalizedValue) {
            if (normalizedValue <= 0.0)
                return colors[0];
            if (normalizedValue >= 1.0)
                return colors[colors.length - 1];

            double segment = 1.0 / (colors.length - 1);
            int index = (int) (normalizedValue / segment);
            double remainder = (normalizedValue - index * segment) / segment;

            Color c1 = colors[index];
            Color c2 = colors[index + 1];

            // Linearen Farbübergang berechnen
            int r = (int) (c1.getRed() + remainder * (c2.getRed() - c1.getRed()));
            int g = (int) (c1.getGreen() + remainder * (c2.getGreen() - c1.getGreen()));
            int b = (int) (c1.getBlue() + remainder * (c2.getBlue() - c1.getBlue()));

            return new Color(r, g, b);
        }
    }
}