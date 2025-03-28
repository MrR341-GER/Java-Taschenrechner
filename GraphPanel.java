import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphPanel - Ein Panel zum Zeichnen von Funktionsgraphen in einem
 * Koordinatensystem
 */
public class GraphPanel extends JPanel {
    // Konstanten für die Darstellung
    private static final int AXIS_MARGIN = 40; // Abstand der Achsen vom Rand
    private static final int TICK_LENGTH = 5; // Länge der Achsenmarkierungen
    private static final int GRID_SPACING = 50; // Abstand der Gitterlinien in Pixeln
    private static final float ZOOM_FACTOR = 1.2f; // Faktor für Zoom-Operationen

    // Darstellungsparameter
    private double xMin = -10; // Minimaler X-Wert im sichtbaren Bereich
    private double xMax = 10; // Maximaler X-Wert im sichtbaren Bereich
    private double yMin = -10; // Minimaler Y-Wert im sichtbaren Bereich
    private double yMax = 10; // Maximaler Y-Wert im sichtbaren Bereich
    private double xScale; // Skalierungsfaktor für X-Werte (Pixel pro Einheit)
    private double yScale; // Skalierungsfaktor für Y-Werte (Pixel pro Einheit)

    // Maus-Interaktion
    private Point lastMousePos; // Letzte Mausposition (für Pan)
    private boolean isDragging = false; // Wird gerade gezogen?

    // Funktionen, die gezeichnet werden sollen
    private List<FunctionInfo> functions = new ArrayList<>();

    // Formatter für die Achsenbeschriftung
    private final DecimalFormat axisFormat = new DecimalFormat("0.##");

    /**
     * Konstruktor - initialisiert das Panel und fügt Maus-Listener hinzu
     */
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));

        // Doppelpuffer für flüssiges Zeichnen aktivieren
        setDoubleBuffered(true);

        // Maus-Listener für Zoom und Pan
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Berechne die Verschiebung in Weltkoordinaten
                    double dx = (e.getX() - lastMousePos.x) / xScale;
                    double dy = (e.getY() - lastMousePos.y) / yScale * -1; // Y-Achse invertiert

                    // Verschiebe die Ansicht
                    xMin -= dx;
                    xMax -= dx;
                    yMin -= dy;
                    yMax -= dy;

                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });

        // Mouse-Wheel Listener für Zoom
        addMouseWheelListener(e -> {
            // Mausposition in Weltkoordinaten umrechnen
            double mouseX = screenToWorldX(e.getX());
            double mouseY = screenToWorldY(e.getY());

            // Zoom-Faktor basierend auf Mausrad-Richtung
            double factor = (e.getWheelRotation() < 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Zoom um die Mausposition herum
            double newWidth = (xMax - xMin) * factor;
            double newHeight = (yMax - yMin) * factor;

            // Berechne relative Position der Maus im sichtbaren Bereich (0-1)
            double relX = (mouseX - xMin) / (xMax - xMin);
            double relY = (mouseY - yMin) / (yMax - yMin);

            // Zoom-Punkt als relatives Zentrum verwenden
            xMin = mouseX - relX * newWidth;
            xMax = xMin + newWidth;
            yMin = mouseY - relY * newHeight;
            yMax = yMin + newHeight;

            repaint();
        });
    }

    /**
     * Fügt eine neue Funktion zum Plotter hinzu
     */
    public void addFunction(String expression, Color color) {
        FunctionParser parser = new FunctionParser(expression);
        functions.add(new FunctionInfo(parser, color));
        repaint();
    }

    /**
     * Entfernt alle Funktionen
     */
    public void clearFunctions() {
        functions.clear();
        repaint();
    }

    /**
     * Setzt die Ansicht auf Standardwerte zurück
     */
    public void resetView() {
        xMin = -10;
        xMax = 10;
        yMin = -10;
        yMax = 10;
        repaint();
    }

    /**
     * Konvertiert eine X-Bildschirmkoordinate in eine X-Weltkoordinate
     */
    private double screenToWorldX(int screenX) {
        return xMin + (screenX - AXIS_MARGIN) / xScale;
    }

    /**
     * Konvertiert eine Y-Bildschirmkoordinate in eine Y-Weltkoordinate
     */
    private double screenToWorldY(int screenY) {
        return yMax - (screenY - AXIS_MARGIN) / yScale;
    }

    /**
     * Konvertiert eine X-Weltkoordinate in eine X-Bildschirmkoordinate
     */
    private int worldToScreenX(double worldX) {
        return (int) (AXIS_MARGIN + (worldX - xMin) * xScale);
    }

    /**
     * Konvertiert eine Y-Weltkoordinate in eine Y-Bildschirmkoordinate
     */
    private int worldToScreenY(double worldY) {
        return (int) (AXIS_MARGIN + (yMax - worldY) * yScale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Anti-Aliasing für glattere Linien aktivieren
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Berechne die Skalierungsfaktoren
        int width = getWidth() - 2 * AXIS_MARGIN;
        int height = getHeight() - 2 * AXIS_MARGIN;
        xScale = width / (xMax - xMin);
        yScale = height / (yMax - yMin);

        // Koordinatengitter zeichnen
        drawGrid(g2d);

        // Achsen zeichnen
        drawAxes(g2d);

        // Funktionen zeichnen
        for (FunctionInfo function : functions) {
            drawFunction(g2d, function);
        }

        // Info-Text
        g2d.setColor(Color.BLACK);
        g2d.drawString("Zoom: Mausrad, Verschieben: Maus ziehen", 10, getHeight() - 10);
    }

    /**
     * Zeichnet das Koordinatengitter
     */
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // Hellgrau
        g2d.setStroke(new BasicStroke(0.5f));

        // Berechne Gitterlinienabstände in Weltkoordinaten
        double xGridSpacing = calculateGridSpacing(xMax - xMin);
        double yGridSpacing = calculateGridSpacing(yMax - yMin);

        // X-Gitterlinien
        double x = Math.ceil(xMin / xGridSpacing) * xGridSpacing;
        while (x <= xMax) {
            int screenX = worldToScreenX(x);
            g2d.draw(new Line2D.Double(screenX, AXIS_MARGIN, screenX, getHeight() - AXIS_MARGIN));
            x += xGridSpacing;
        }

        // Y-Gitterlinien
        double y = Math.ceil(yMin / yGridSpacing) * yGridSpacing;
        while (y <= yMax) {
            int screenY = worldToScreenY(y);
            g2d.draw(new Line2D.Double(AXIS_MARGIN, screenY, getWidth() - AXIS_MARGIN, screenY));
            y += yGridSpacing;
        }
    }

    /**
     * Berechnet einen geeigneten Abstand für Gitterlinien
     */
    private double calculateGridSpacing(double range) {
        // Ziel: Etwa 10 Gitterlinien im sichtbaren Bereich
        double rawSpacing = range / 10;

        // Normalisiere auf Zehnerpotenzen
        double exponent = Math.floor(Math.log10(rawSpacing));
        double mantissa = rawSpacing / Math.pow(10, exponent);

        // Runde auf "schöne" Werte: 1, 2, 5, 10
        if (mantissa < 1.5)
            return Math.pow(10, exponent);
        else if (mantissa < 3.5)
            return 2 * Math.pow(10, exponent);
        else if (mantissa < 7.5)
            return 5 * Math.pow(10, exponent);
        else
            return 10 * Math.pow(10, exponent);
    }

    /**
     * Zeichnet die X- und Y-Achsen mit Beschriftung
     */
    private void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));

        // X-Achse
        int yAxisPos = worldToScreenY(0);
        if (yAxisPos < AXIS_MARGIN)
            yAxisPos = AXIS_MARGIN;
        if (yAxisPos > getHeight() - AXIS_MARGIN)
            yAxisPos = getHeight() - AXIS_MARGIN;

        g2d.draw(new Line2D.Double(AXIS_MARGIN, yAxisPos, getWidth() - AXIS_MARGIN, yAxisPos));

        // Y-Achse
        int xAxisPos = worldToScreenX(0);
        if (xAxisPos < AXIS_MARGIN)
            xAxisPos = AXIS_MARGIN;
        if (xAxisPos > getWidth() - AXIS_MARGIN)
            xAxisPos = getWidth() - AXIS_MARGIN;

        g2d.draw(new Line2D.Double(xAxisPos, AXIS_MARGIN, xAxisPos, getHeight() - AXIS_MARGIN));

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // X-Achse Markierungen und Beschriftungen
        double xGridSpacing = calculateGridSpacing(xMax - xMin);
        double x = Math.ceil(xMin / xGridSpacing) * xGridSpacing;

        while (x <= xMax) {
            int screenX = worldToScreenX(x);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenX >= AXIS_MARGIN && screenX <= getWidth() - AXIS_MARGIN) {
                // Markierung
                g2d.draw(new Line2D.Double(screenX, yAxisPos - TICK_LENGTH, screenX, yAxisPos + TICK_LENGTH));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(x) > 1e-10) { // Kleine Werte als Null betrachten
                    String label = axisFormat.format(x);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, screenX - labelWidth / 2, yAxisPos + TICK_LENGTH + 12);
                }
            }
            x += xGridSpacing;
        }

        // Y-Achse Markierungen und Beschriftungen
        double yGridSpacing = calculateGridSpacing(yMax - yMin);
        double y = Math.ceil(yMin / yGridSpacing) * yGridSpacing;

        while (y <= yMax) {
            int screenY = worldToScreenY(y);

            // Nur zeichnen, wenn innerhalb der Grenzen
            if (screenY >= AXIS_MARGIN && screenY <= getHeight() - AXIS_MARGIN) {
                // Markierung
                g2d.draw(new Line2D.Double(xAxisPos - TICK_LENGTH, screenY, xAxisPos + TICK_LENGTH, screenY));

                // Beschriftung (nicht bei 0, da dort die Achsenbeschriftung ist)
                if (Math.abs(y) > 1e-10) { // Kleine Werte als Null betrachten
                    String label = axisFormat.format(y);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(label);
                    g2d.drawString(label, xAxisPos - labelWidth - 5, screenY + 4);
                }
            }
            y += yGridSpacing;
        }

        // Ursprungsbeschriftung
        if (xAxisPos >= AXIS_MARGIN && xAxisPos <= getWidth() - AXIS_MARGIN &&
                yAxisPos >= AXIS_MARGIN && yAxisPos <= getHeight() - AXIS_MARGIN) {
            g2d.drawString("0", xAxisPos + 4, yAxisPos + 12);
        }

        // Achsenbeschriftungen
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("x", getWidth() - AXIS_MARGIN + 10, yAxisPos + 4);
        g2d.drawString("y", xAxisPos - 4, AXIS_MARGIN - 10);
    }

    /**
     * Zeichnet eine Funktion
     */
    private void drawFunction(Graphics2D g2d, FunctionInfo functionInfo) {
        g2d.setColor(functionInfo.color);
        g2d.setStroke(new BasicStroke(2f));

        Path2D path = new Path2D.Double();
        boolean pathStarted = false;

        // Anzahl der Punkte basierend auf der Bildschirmbreite
        int numPoints = getWidth() - 2 * AXIS_MARGIN;
        double step = (xMax - xMin) / numPoints;

        Double lastY = null;

        for (int i = 0; i <= numPoints; i++) {
            double x = xMin + i * step;

            try {
                double y = functionInfo.function.evaluateAt(x);

                // Prüfen ob der Wert gültig ist
                if (Double.isNaN(y) || Double.isInfinite(y)) {
                    // Ungültiger Punkt, neuen Pfad starten
                    pathStarted = false;
                    lastY = null;
                    continue;
                }

                // Sprunghafte Änderungen erkennen und Linien unterbrechen
                if (lastY != null) {
                    double deltaY = Math.abs(y - lastY);
                    double deltaYRelative = deltaY / (yMax - yMin);

                    // Wenn die relative Änderung zu groß ist, neuen Pfad starten
                    if (deltaYRelative > 0.2) {
                        pathStarted = false;
                    }
                }
                lastY = y;

                // Nur zeichnen, wenn im sichtbaren Bereich
                if (y >= yMin && y <= yMax) {
                    int screenX = worldToScreenX(x);
                    int screenY = worldToScreenY(y);

                    if (!pathStarted) {
                        path.moveTo(screenX, screenY);
                        pathStarted = true;
                    } else {
                        path.lineTo(screenX, screenY);
                    }
                } else {
                    // Punkt außerhalb des sichtbaren Bereichs
                    pathStarted = false;
                }
            } catch (Exception e) {
                // Bei Fehlern in der Auswertung den Pfad unterbrechen
                pathStarted = false;
            }
        }

        g2d.draw(path);
    }

    /**
     * Klasse zur Speicherung von Funktionsinformationen
     */
    private static class FunctionInfo {
        final FunctionParser function;
        final Color color;

        FunctionInfo(FunctionParser function, Color color) {
            this.function = function;
            this.color = color;
        }
    }

    /**
     * Einfacher Parser für mathematische Funktionen
     * Diese Klasse verwendet Rekursiven Abstieg zum Parsen von Ausdrücken
     */
    private static class FunctionParser {
        private final String expression;
        private int pos;
        private char ch;

        public FunctionParser(String expression) {
            this.expression = expression.toLowerCase().replaceAll("\\s+", "");
        }

        /**
         * Wertet die Funktion an einer bestimmten Stelle x aus
         */
        public double evaluateAt(double x) {
            pos = 0;
            nextChar();
            double result = parseExpression(x);

            if (pos < expression.length()) {
                throw new RuntimeException("Unexpected character: " + ch);
            }

            return result;
        }

        private void nextChar() {
            ch = (pos < expression.length()) ? expression.charAt(pos++) : '\0';
        }

        private boolean eat(char charToEat) {
            while (ch == ' ')
                nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression(double x) {
            double result = parseTerm(x);

            while (true) {
                if (eat('+'))
                    result += parseTerm(x);
                else if (eat('-'))
                    result -= parseTerm(x);
                else
                    return result;
            }
        }

        private double parseTerm(double x) {
            double result = parseFactor(x);

            while (true) {
                if (eat('*'))
                    result *= parseFactor(x);
                else if (eat('/')) {
                    double divisor = parseFactor(x);
                    if (Math.abs(divisor) < 1e-10) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result /= divisor;
                } else
                    return result;
            }
        }

        private double parseFactor(double x) {
            if (eat('+'))
                return parseFactor(x);
            if (eat('-'))
                return -parseFactor(x);

            double result;

            // Klammern
            if (eat('(')) {
                result = parseExpression(x);
                eat(')');
            }
            // Zahlen
            else if ((ch >= '0' && ch <= '9') || ch == '.') {
                StringBuilder sb = new StringBuilder();
                while ((ch >= '0' && ch <= '9') || ch == '.') {
                    sb.append(ch);
                    nextChar();
                }
                result = Double.parseDouble(sb.toString());
            }
            // Die Variable x
            else if (ch == 'x') {
                nextChar();
                result = x;
            }
            // Funktionen wie sin, cos, etc.
            else if (ch >= 'a' && ch <= 'z') {
                StringBuilder funcName = new StringBuilder();
                while (ch >= 'a' && ch <= 'z') {
                    funcName.append(ch);
                    nextChar();
                }

                String name = funcName.toString();
                if (eat('(')) {
                    result = parseExpression(x);
                    eat(')');

                    // Bekannte Funktionen auswerten
                    switch (name) {
                        case "sin":
                            result = Math.sin(result);
                            break;
                        case "cos":
                            result = Math.cos(result);
                            break;
                        case "tan":
                            result = Math.tan(result);
                            break;
                        case "sqrt":
                            if (result < 0)
                                throw new ArithmeticException("Square root of negative number");
                            result = Math.sqrt(result);
                            break;
                        case "log":
                            if (result <= 0)
                                throw new ArithmeticException("Logarithm of non-positive number");
                            result = Math.log10(result);
                            break;
                        case "ln":
                            if (result <= 0)
                                throw new ArithmeticException("Natural logarithm of non-positive number");
                            result = Math.log(result);
                            break;
                        case "abs":
                            result = Math.abs(result);
                            break;
                        case "exp":
                            result = Math.exp(result);
                            break;
                        default:
                            throw new RuntimeException("Unknown function: " + name);
                    }
                } else {
                    // Mathematische Konstanten
                    switch (name) {
                        case "pi":
                            result = Math.PI;
                            break;
                        case "e":
                            result = Math.E;
                            break;
                        case "x":
                            result = x;
                            break;
                        default:
                            throw new RuntimeException("Unknown identifier: " + name);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected: " + ch);
            }

            // Exponentation (Potenzen)
            if (eat('^')) {
                result = Math.pow(result, parseFactor(x));
            }

            return result;
        }
    }
}