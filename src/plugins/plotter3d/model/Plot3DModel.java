package plugins.plotter3d.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import parser.Function3DParser;

/**
 * Manages the data model for 3D plotting, including functions, grid points, and
 * value ranges
 */
public class Plot3DModel {
    // List of functions with their properties
    private List<Function3DInfo> functions = new ArrayList<>();

    // Z-value range (calculated automatically based on functions)
    private double zMin = 0;
    private double zMax = 1;

    /**
     * Adds a new function to the model
     * 
     * @param functionExpression The function expression
     * @param color              The color for this function
     * @return The index of the added function
     */
    public int addFunction(String functionExpression, Color color) {
        Function3DParser parser = new Function3DParser(functionExpression);
        Function3DInfo functionInfo = new Function3DInfo(parser, color, functionExpression);
        functions.add(functionInfo);
        return functions.size() - 1;
    }

    /**
     * Clears all functions from the model
     */
    public void clearFunctions() {
        functions.clear();

        // Reset Z-range
        zMin = 0;
        zMax = 1;
    }

    /**
     * Removes a function at the specified index
     * 
     * @param index The index of the function to remove
     */
    public void removeFunction(int index) {
        if (index >= 0 && index < functions.size()) {
            functions.remove(index);

            // Recalculate Z-range if there are still functions
            if (!functions.isEmpty()) {
                recalculateZRange();
            } else {
                // If no functions left, reset Z-range
                zMin = 0;
                zMax = 1;
            }
        }
    }

    /**
     * Calculates function values for all functions in the model
     * 
     * @param xMin       Minimum x-value
     * @param xMax       Maximum x-value
     * @param yMin       Minimum y-value
     * @param yMax       Maximum y-value
     * @param resolution Resolution (number of points per axis)
     */
    public void calculateAllFunctionValues(double xMin, double xMax, double yMin, double yMax, int resolution) {
        // Reset Z-range for recalculation
        zMin = Double.POSITIVE_INFINITY;
        zMax = Double.NEGATIVE_INFINITY;

        // Calculate values for each function
        for (Function3DInfo functionInfo : functions) {
            calculateFunctionValues(functionInfo, xMin, xMax, yMin, yMax, resolution);
        }

        // Ensure Z-range is valid (prevent too small ranges)
        if (Math.abs(zMax - zMin) < 1e-10) {
            zMax = zMin + 1.0;
        }
    }

    /**
     * Calculates function values for a specific function
     */
    private void calculateFunctionValues(Function3DInfo functionInfo, double xMin, double xMax,
            double yMin, double yMax, int resolution) {
        double xStep = (xMax - xMin) / (resolution - 1);
        double yStep = (yMax - yMin) / (resolution - 1);

        // Initialize the grid points array if not already done
        if (functionInfo.getGridPoints() == null ||
                functionInfo.getGridPoints().length != resolution ||
                functionInfo.getGridPoints()[0].length != resolution) {

            functionInfo.setGridPoints(new Plot3DPoint[resolution][resolution][3]);
        }

        // Track local min/max Z values for this function
        double localZMin = Double.POSITIVE_INFINITY;
        double localZMax = Double.NEGATIVE_INFINITY;

        // Calculate all points for this function
        for (int i = 0; i < resolution; i++) {
            double x = xMin + i * xStep;

            for (int j = 0; j < resolution; j++) {
                double y = yMin + j * yStep;

                // Evaluate the function with error handling
                double z;
                try {
                    z = functionInfo.function.evaluateAt(x, y);
                    if (Double.isNaN(z) || Double.isInfinite(z)) {
                        z = 0; // Handle problematic values
                    }
                } catch (Exception e) {
                    z = 0; // Set to 0 on any error
                }

                // Create the original point
                Plot3DPoint[][][] gridPoints = functionInfo.getGridPoints();
                gridPoints[i][j][0] = new Plot3DPoint(x, y, z);

                // Also initialize transformed and projected points as copies
                // They'll be updated later by the transformer
                gridPoints[i][j][1] = new Plot3DPoint(x, y, z);
                gridPoints[i][j][2] = new Plot3DPoint(x, y, 0); // Z=0 for projection

                // Update local Z min/max
                if (z < localZMin)
                    localZMin = z;
                if (z > localZMax)
                    localZMax = z;
            }
        }

        // Update global Z min/max
        if (localZMin < zMin)
            zMin = localZMin;
        if (localZMax > zMax)
            zMax = localZMax;
    }

    /**
     * Recalculates the Z-range across all functions
     */
    private void recalculateZRange() {
        zMin = Double.POSITIVE_INFINITY;
        zMax = Double.NEGATIVE_INFINITY;

        // Find min/max Z values across all functions
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

        // Ensure Z-range is valid
        if (Math.abs(zMax - zMin) < 1e-10) {
            zMax = zMin + 1.0;
        }
    }

    // Getters

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
     * Class to store function information including grid points
     */
    public static class Function3DInfo {
        public final Function3DParser function;
        public final Color color;
        public final String expression;
        private Plot3DPoint[][][] gridPoints; // [x][y][phase] where phase is 0=original, 1=transformed, 2=projected

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

        // Getter and setter for gridPoints
        public Plot3DPoint[][][] getGridPoints() {
            return gridPoints;
        }

        public void setGridPoints(Plot3DPoint[][][] gridPoints) {
            this.gridPoints = gridPoints;
        }
    }
}