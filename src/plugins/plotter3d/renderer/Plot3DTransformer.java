package plugins.plotter3d.renderer;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Behandelt Transformationen von 3D-Weltkoordinaten zu 2D-Bildschirmkoordinaten
 * Wendet Rotation, Skalierung und Verschiebung an
 */
public class Plot3DTransformer {
    private static final int ORIGINAL = 0;
    private static final int TRANSFORMED = 1;
    private static final int PROJECTED = 2;

    /**
     * Transformiert und projiziert alle Gitterpunkte für alle Funktionen
     * 
     * @param model   Das Datenmodell, das die Funktionen enthält
     * @param view    Die Ansichtsparameter
     * @param zCenter Die zentrale z-Koordinate für die Normalisierung
     */
    public void transformAndProjectAllPoints(Plot3DModel model, Plot3DView view, double zCenter) {
        for (Plot3DModel.Function3DInfo functionInfo : model.getFunctions()) {
            if (functionInfo.getGridPoints() != null) {
                transformAndProjectPoints(functionInfo, view,
                        (view.getXMax() + view.getXMin()) / 2,
                        (view.getYMax() + view.getYMin()) / 2,
                        zCenter);
            }
        }
    }

    /**
     * Berechnet den z-Wertebereich für eine Funktion
     */
    private double calculateZRange(Plot3DModel.Function3DInfo functionInfo) {
        double zMin = Double.POSITIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        if (functionInfo.getGridPoints() == null) {
            return 1.0; // Standardbereich, wenn keine Punkte vorhanden sind
        }

        // Ermittle min/max Z-Werte über alle Gitterpunkte
        for (int i = 0; i < functionInfo.getGridPoints().length; i++) {
            for (int j = 0; j < functionInfo.getGridPoints()[i].length; j++) {
                Plot3DPoint original = functionInfo.getGridPoints()[i][j][0];
                if (original != null) {
                    double z = original.getZ();
                    if (z < zMin)
                        zMin = z;
                    if (z > zMax)
                        zMax = z;
                }
            }
        }

        // Stelle sicher, dass der Bereich gültig ist
        if (zMin == Double.POSITIVE_INFINITY || zMax == Double.NEGATIVE_INFINITY || Math.abs(zMax - zMin) < 1e-10) {
            return 1.0; // Standardbereich, wenn keine gültigen Punkte vorhanden sind oder der Bereich
                        // zu klein ist
        }

        return zMax - zMin;
    }

    /**
     * Transformiert und projiziert die Gitterpunkte für eine einzelne Funktion
     */
    private void transformAndProjectPoints(Plot3DModel.Function3DInfo functionInfo,
            Plot3DView view,
            double xCenter, double yCenter, double zCenter) {
        // Wandelt Rotationswinkel in Bogenmaß um
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Berechne Sinus- und Kosinuswerte vorab für mehr Effizienz
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Bestimme den Normalisierungsfaktor für die Koordinaten
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        // Da wir hier keinen Zugriff auf das Modell haben, verwenden wir die
        // Gitterpunkte selbst,
        // um einen z-Bereich zu berechnen
        double zRange = calculateZRange(functionInfo);
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Skalierung für die aktuelle Transformation
        double scale = view.getScale();

        // Angepasste Verschiebungswerte
        double adjustedPanX = view.getPanX() * scale;
        double adjustedPanY = view.getPanY() * scale;

        // Hole das Gitterpunkt-Array für diese Funktion
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        int resolution = gridPoints.length;

        // Verarbeite jeden Punkt im Gitter
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                // Hole den ursprünglichen Punkt
                Plot3DPoint original = gridPoints[i][j][ORIGINAL];

                // Zentriere und normalisiere die Koordinaten
                double x = (original.getX() - xCenter) * factor;
                double y = (original.getY() - yCenter) * factor;
                double z = (original.getZ() - zCenter) * factor;

                // Wende Skalierung an
                x *= scale;
                y *= scale;
                z *= scale;

                // Wende Rotation um die Z-Achse an
                double tempX = x * cosZ - y * sinZ;
                double tempY = x * sinZ + y * cosZ;
                x = tempX;
                y = tempY;

                // Wende Rotation um die Y-Achse an
                tempX = x * cosY + z * sinY;
                double tempZ = -x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // Wende Rotation um die X-Achse an
                tempY = y * cosX - z * sinX;
                tempZ = y * sinX + z * cosX;
                y = tempY;
                z = tempZ;

                // Wende Verschiebung an
                x += adjustedPanX;
                y += adjustedPanY;

                // Speichere den transformierten Punkt
                gridPoints[i][j][TRANSFORMED].setX(x);
                gridPoints[i][j][TRANSFORMED].setY(y);
                gridPoints[i][j][TRANSFORMED].setZ(z);

                // Projiziere auf 2D (einfache Parallelprojektion)
                gridPoints[i][j][PROJECTED].setX(x);
                gridPoints[i][j][PROJECTED].setY(y);
                gridPoints[i][j][PROJECTED].setZ(0);
            }
        }
    }

    /**
     * Transformiert einen einzelnen 3D-Punkt zum Zeichnen von Linien
     * 
     * @return Der 2D-projizierte Punkt
     */
    public Plot3DPoint transformPoint(double x, double y, double z,
            double xCenter, double yCenter, double zCenter,
            double factor, double scale,
            double sinX, double cosX,
            double sinY, double cosY,
            double sinZ, double cosZ,
            double panX, double panY) {
        // Zentriere und normalisiere
        double nx = (x - xCenter) * factor * scale;
        double ny = (y - yCenter) * factor * scale;
        double nz = (z - zCenter) * factor * scale;

        // Rotation um die Z-Achse
        double tx = nx * cosZ - ny * sinZ;
        double ty = nx * sinZ + ny * cosZ;

        // Rotation um die Y-Achse
        double tempX = tx * cosY + nz * sinY;
        double tempZ = -tx * sinY + nz * cosY;
        tx = tempX;

        // Rotation um die X-Achse
        double tempY = ty * cosX - tempZ * sinX;
        double tz = ty * sinX + tempZ * cosX;
        ty = tempY;

        // Wende Verschiebung an
        tx += panX * scale;
        ty += panY * scale;

        // Gib den transformierten Punkt zurück
        return new Plot3DPoint(tx, ty, tz);
    }

    /**
     * Konvertiert einen transformierten Punkt in Bildschirmkoordinaten
     */
    public int[] projectToScreen(Plot3DPoint point, double displayScale, int xOffset, int yOffset) {
        int screenX = xOffset + (int) (point.getX() * displayScale);
        int screenY = yOffset - (int) (point.getY() * displayScale); // Y ist auf dem Bildschirm invertiert

        return new int[] { screenX, screenY };
    }
}
