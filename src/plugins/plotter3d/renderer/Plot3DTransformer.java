
package plugins.plotter3d.renderer;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;

/**
 * Handles transformations from 3D world coordinates to 2D screen coordinates
 * Applies rotation, scale, and panning
 */
public class Plot3DTransformer {
    private static final int ORIGINAL = 0;
    private static final int TRANSFORMED = 1;
    private static final int PROJECTED = 2;

    /**
     * Transforms and projects all grid points for all functions
     * 
     * @param model   The data model containing functions
     * @param view    The view parameters
     * @param zCenter The center z-coordinate for normalization
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
     * Calculates the z-value range for a function
     */
    private double calculateZRange(Plot3DModel.Function3DInfo functionInfo) {
        double zMin = Double.POSITIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        if (functionInfo.getGridPoints() == null) {
            return 1.0; // Default range if no points
        }

        // Find min/max Z values across all grid points
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

        // Ensure range is valid
        if (zMin == Double.POSITIVE_INFINITY || zMax == Double.NEGATIVE_INFINITY || Math.abs(zMax - zMin) < 1e-10) {
            return 1.0; // Default range if no valid points or too small range
        }

        return zMax - zMin;
    }

    /**
     * Transforms and projects the grid points for a single function
     */
    private void transformAndProjectPoints(Plot3DModel.Function3DInfo functionInfo,
            Plot3DView view,
            double xCenter, double yCenter, double zCenter) {
        // Convert rotation angles to radians
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Pre-calculate sine and cosine values for efficiency
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Determine the normalization factor for coordinates
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        // We don't have access to model here, so we'll use the grid points themselves
        // to calculate a zRange
        double zRange = calculateZRange(functionInfo);
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Scale for the current transformation
        double scale = view.getScale();

        // Adjusted pan values
        double adjustedPanX = view.getPanX() * scale;
        double adjustedPanY = view.getPanY() * scale;

        // Get grid points array for this function
        Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
        int resolution = gridPoints.length;

        // Process each point in the grid
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                // Get the original point
                Plot3DPoint original = gridPoints[i][j][ORIGINAL];

                // Center and normalize the coordinates
                double x = (original.getX() - xCenter) * factor;
                double y = (original.getY() - yCenter) * factor;
                double z = (original.getZ() - zCenter) * factor;

                // Apply scaling
                x *= scale;
                y *= scale;
                z *= scale;

                // Apply rotation around Z axis
                double tempX = x * cosZ - y * sinZ;
                double tempY = x * sinZ + y * cosZ;
                x = tempX;
                y = tempY;

                // Apply rotation around Y axis
                tempX = x * cosY + z * sinY;
                double tempZ = -x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // Apply rotation around X axis
                tempY = y * cosX - z * sinX;
                tempZ = y * sinX + z * cosX;
                y = tempY;
                z = tempZ;

                // Apply panning
                x += adjustedPanX;
                y += adjustedPanY;

                // Store the transformed point
                gridPoints[i][j][TRANSFORMED].setX(x);
                gridPoints[i][j][TRANSFORMED].setY(y);
                gridPoints[i][j][TRANSFORMED].setZ(z);

                // Project to 2D (simple parallel projection)
                gridPoints[i][j][PROJECTED].setX(x);
                gridPoints[i][j][PROJECTED].setY(y);
                gridPoints[i][j][PROJECTED].setZ(0);
            }
        }
    }

    /**
     * Transforms a single 3D point for drawing lines
     * 
     * @return The 2D projected point
     */
    public Plot3DPoint transformPoint(double x, double y, double z,
            double xCenter, double yCenter, double zCenter,
            double factor, double scale,
            double sinX, double cosX,
            double sinY, double cosY,
            double sinZ, double cosZ,
            double panX, double panY) {
        // Center and normalize
        double nx = (x - xCenter) * factor * scale;
        double ny = (y - yCenter) * factor * scale;
        double nz = (z - zCenter) * factor * scale;

        // Rotation around Z axis
        double tx = nx * cosZ - ny * sinZ;
        double ty = nx * sinZ + ny * cosZ;

        // Rotation around Y axis
        double tempX = tx * cosY + nz * sinY;
        double tempZ = -tx * sinY + nz * cosY;
        tx = tempX;

        // Rotation around X axis
        double tempY = ty * cosX - tempZ * sinX;
        double tz = ty * sinX + tempZ * cosX;
        ty = tempY;

        // Apply panning
        tx += panX * scale;
        ty += panY * scale;

        // Return the transformed point
        return new Plot3DPoint(tx, ty, tz);
    }

    /**
     * Converts a transformed point to screen coordinates
     */
    public int[] projectToScreen(Plot3DPoint point, double displayScale, int xOffset, int yOffset) {
        int screenX = xOffset + (int) (point.getX() * displayScale);
        int screenY = yOffset - (int) (point.getY() * displayScale); // Y is inverted on screen

        return new int[] { screenX, screenY };
    }
}