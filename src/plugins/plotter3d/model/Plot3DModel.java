package plugins.plotter3d.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import parser.Function3DParser;

/**
 * Verwaltet das Datenmodell für die 3D-Darstellung, einschließlich Funktionen,
 * Rasterpunkten und Wertebereichen
 */
public class Plot3DModel {
    // Liste der Funktionen mit ihren Eigenschaften
    private List<Function3DInfo> functions = new ArrayList<>();

    // Z-Wertebereich (wird automatisch basierend auf den Funktionen berechnet)
    private double zMin = 0;
    private double zMax = 1;

    /**
     * Fügt dem Modell eine neue Funktion hinzu
     * 
     * @param functionExpression Der Funktionenausdruck
     * @param color              Die Farbe für diese Funktion
     * @return Der Index der hinzugefügten Funktion
     */
    public int addFunction(String functionExpression, Color color) {
        Function3DParser parser = new Function3DParser(functionExpression);
        Function3DInfo functionInfo = new Function3DInfo(parser, color, functionExpression);
        functions.add(functionInfo);
        return functions.size() - 1;
    }

    /**
     * Entfernt alle Funktionen aus dem Modell
     */
    public void clearFunctions() {
        functions.clear();

        // Setze den Z-Wertebereich zurück
        zMin = 0;
        zMax = 1;
    }

    /**
     * Entfernt eine Funktion an dem angegebenen Index
     * 
     * @param index Der Index der zu entfernenden Funktion
     */
    public void removeFunction(int index) {
        if (index >= 0 && index < functions.size()) {
            functions.remove(index);

            // Berechne den Z-Wertebereich neu, wenn noch Funktionen vorhanden sind
            if (!functions.isEmpty()) {
                recalculateZRange();
            } else {
                // Falls keine Funktionen mehr vorhanden sind, setze den Z-Wertebereich zurück
                zMin = 0;
                zMax = 1;
            }
        }
    }

    /**
     * Berechnet die Funktionswerte für alle Funktionen im Modell
     * 
     * @param xMin       Minimaler x-Wert
     * @param xMax       Maximaler x-Wert
     * @param yMin       Minimaler y-Wert
     * @param yMax       Maximaler y-Wert
     * @param resolution Auflösung (Anzahl der Punkte pro Achse)
     */
    public void calculateAllFunctionValues(double xMin, double xMax, double yMin, double yMax, int resolution) {
        // Setze den Z-Wertebereich für die Neuberechnung zurück
        zMin = Double.POSITIVE_INFINITY;
        zMax = Double.NEGATIVE_INFINITY;

        // Berechne die Werte für jede Funktion
        for (Function3DInfo functionInfo : functions) {
            calculateFunctionValues(functionInfo, xMin, xMax, yMin, yMax, resolution);
        }

        // Wenn keine Funktionen vorhanden sind oder keine validen Z-Werte berechnet
        // wurden,
        // setze Standard-Z-Bereich
        if (functions.isEmpty() || zMin == Double.POSITIVE_INFINITY || zMax == Double.NEGATIVE_INFINITY) {
            // Setze auf einen sinnvollen Standardbereich (analog zum x,y-Bereich)
            double xRange = Math.abs(xMax - xMin);
            double yRange = Math.abs(yMax - yMin);
            double defaultRange = Math.max(xRange, yRange);

            // Standardbereich symmetrisch um 0, mit ähnlicher Ausdehnung wie x,y-Bereich
            zMin = -defaultRange / 2;
            zMax = defaultRange / 2;
        }

        // Stelle sicher, dass der Z-Wertebereich gültig ist (verhindere zu kleine
        // Bereiche)
        if (Math.abs(zMax - zMin) < 1e-10) {
            zMax = zMin + 1.0;
        }
    }

    /**
     * Berechnet die Funktionswerte für eine spezifische Funktion
     */
    private void calculateFunctionValues(Function3DInfo functionInfo, double xMin, double xMax,
            double yMin, double yMax, int resolution) {
        double xStep = (xMax - xMin) / (resolution - 1);
        double yStep = (yMax - yMin) / (resolution - 1);

        // Initialisiere das Rasterpunkt-Array, falls noch nicht vorhanden
        if (functionInfo.getGridPoints() == null ||
                functionInfo.getGridPoints().length != resolution ||
                functionInfo.getGridPoints()[0].length != resolution) {

            functionInfo.setGridPoints(new Plot3DPoint[resolution][resolution][3]);
        }

        // Verfolge lokale Min-/Max-Z-Werte für diese Funktion
        double localZMin = Double.POSITIVE_INFINITY;
        double localZMax = Double.NEGATIVE_INFINITY;

        // Berechne alle Punkte für diese Funktion
        for (int i = 0; i < resolution; i++) {
            double x = xMin + i * xStep;

            for (int j = 0; j < resolution; j++) {
                double y = yMin + j * yStep;

                // Werte die Funktion mit Fehlerbehandlung aus
                double z;
                try {
                    z = functionInfo.function.evaluateAt(x, y);
                    if (Double.isNaN(z) || Double.isInfinite(z)) {
                        z = 0; // Behandle problematische Werte
                    }
                } catch (Exception e) {
                    z = 0; // Setze bei einem Fehler auf 0
                }

                // Erstelle den ursprünglichen Punkt
                Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
                gridPoints[i][j][0] = new Plot3DPoint(x, y, z);

                // Initialisiere auch transformierte und projizierte Punkte als Kopien
                // Diese werden später vom Transformer aktualisiert
                gridPoints[i][j][1] = new Plot3DPoint(x, y, z);
                gridPoints[i][j][2] = new Plot3DPoint(x, y, 0); // Z=0 für die Projektion

                // Aktualisiere lokale Z-Min/Max-Werte
                if (z < localZMin)
                    localZMin = z;
                if (z > localZMax)
                    localZMax = z;
            }
        }

        // Aktualisiere globale Z-Min/Max-Werte
        if (localZMin < zMin)
            zMin = localZMin;
        if (localZMax > zMax)
            zMax = localZMax;
    }

    /**
     * Berechnet den Z-Bereich über alle Funktionen neu
     */
    private void recalculateZRange() {
        zMin = Double.POSITIVE_INFINITY;
        zMax = Double.NEGATIVE_INFINITY;

        // Finde die Min-/Max-Z-Werte über alle Funktionen
        for (Function3DInfo functionInfo : functions) {
            Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
            if (gridPoints != null) {
                int resolution = gridPoints.length;

                for (int i = 0; i < resolution; i++) {
                    for (int j = 0; j < resolution; j++) {
                        if (gridPoints[i][j][0] != null) {
                            double z = gridPoints[i][j][0].getZ();

                            if (z < zMin)
                                zMin = z;
                            if (z > zMax)
                                zMax = z;
                        }
                    }
                }
            }
        }

        // Stelle sicher, dass der Z-Bereich gültig ist
        if (Math.abs(zMax - zMin) < 1e-10) {
            zMax = zMin + 1.0;
        }
    }

    // Getter

    public List<Function3DInfo> getFunctions() {
        return functions;
    }

    public double getZMin() {
        return zMin;
    }

    public double getZMax() {
        return zMax;
    }

    /**
     * Klasse zur Speicherung von Funktionsinformationen einschließlich
     * Rasterpunkten
     */
    public static class Function3DInfo {
        public final Function3DParser function;
        public final Color color;
        public final String expression;
        private Plot3DPoint[][][] gridPoints; // [x][y][Phase], wobei Phase 0 = original, 1 = transformiert, 2 =
                                              // projiziert
        private boolean visible = true; // Standardmäßig sichtbar

        public Function3DInfo(Function3DParser function, Color color, String expression) {
            this.function = function;
            this.color = color;
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        public Color getColor() {
            return color;
        }

        // Getter und Setter für gridPoints
        public Plot3DPoint[][][] getGridPoints() {
            return gridPoints;
        }

        public void setGridPoints(Plot3DPoint[][][] gridPoints) {
            this.gridPoints = gridPoints;
        }

        /**
         * Gibt an, ob die Funktion sichtbar ist
         */
        public boolean isVisible() {
            return visible;
        }

        /**
         * Setzt die Sichtbarkeit der Funktion
         */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /**
         * Schaltet die Sichtbarkeit der Funktion um
         */
        public void toggleVisibility() {
            visible = !visible;
        }
    }
}
