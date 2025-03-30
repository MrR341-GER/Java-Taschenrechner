
package plugins.plotter3d.renderer;

import java.awt.Color;

/**
 * Manages color schemes for 3D plotting
 * Maps z-values to colors using gradient schemes
 */
public class Plot3DColorScheme {
    // Array of color stops for the gradient
    private Color[] colors;

    /**
     * Creates a color scheme with the specified color stops
     * 
     * @param colors An array of colors defining the gradient
     */
    public Plot3DColorScheme(Color... colors) {
        if (colors == null || colors.length < 2) {
            // Default color scheme if none provided
            this.colors = new Color[] {
                    new Color(0, 0, 255), // Blue (low values)
                    new Color(0, 255, 255), // Cyan
                    new Color(0, 255, 0), // Green
                    new Color(255, 255, 0), // Yellow
                    new Color(255, 0, 0) // Red (high values)
            };
        } else {
            this.colors = colors;
        }
    }

    /**
     * Creates the default color scheme
     */
    public static Plot3DColorScheme createDefault() {
        return new Plot3DColorScheme(
                new Color(0, 0, 255), // Blue (low values)
                new Color(0, 255, 255), // Cyan
                new Color(0, 255, 0), // Green
                new Color(255, 255, 0), // Yellow
                new Color(255, 0, 0) // Red (high values)
        );
    }

    /**
     * Gets a color for a normalized value (0.0 to 1.0)
     * 
     * @param normalizedValue Value between 0.0 and 1.0
     * @return The interpolated color from the scheme
     */
    public Color getColorForValue(double normalizedValue) {
        if (normalizedValue <= 0.0) {
            return colors[0];
        }

        if (normalizedValue >= 1.0) {
            return colors[colors.length - 1];
        }

        // Find the segment in the gradient
        double segment = 1.0 / (colors.length - 1);
        int index = (int) (normalizedValue / segment);
        double remainder = (normalizedValue - index * segment) / segment;

        // Get the two colors to interpolate between
        Color c1 = colors[index];
        Color c2 = colors[index + 1];

        // Linear interpolation between the two colors
        int r = (int) (c1.getRed() + remainder * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + remainder * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + remainder * (c2.getBlue() - c1.getBlue()));

        return new Color(r, g, b);
    }

    /**
     * Gets a color based on a z-value and the current z-range
     * 
     * @param z    The z-value to map to a color
     * @param zMin The minimum z-value in the current range
     * @param zMax The maximum z-value in the current range
     * @return The color corresponding to the z-value
     */
    public Color getColorForZ(double z, double zMin, double zMax) {
        // Normalize z-value to 0.0-1.0 range
        double normalizedZ = (z - zMin) / (zMax - zMin);
        return getColorForValue(normalizedZ);
    }
}