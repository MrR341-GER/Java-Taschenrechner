package plugins.plotter3d.renderer;

import java.awt.Color;

/**
 * Verwaltet Farbschemata für die 3D-Darstellung
 * Ordnet z-Werte Farbwerten mittels Gradientenschemata zu
 */
public class Plot3DColorScheme {
    // Array von Farbstopps für den Farbverlauf
    private Color[] colors;

    /**
     * Erzeugt ein Farbschema mit den angegebenen Farbstopps
     *
     * @param colors Ein Array von Farben, das den Farbverlauf definiert
     */
    public Plot3DColorScheme(Color... colors) {
        if (colors == null || colors.length < 2) {
            // Standard-Farbschema, falls keins angegeben wurde
            this.colors = new Color[] {
                    new Color(0, 0, 255), // Blau (niedrige Werte)
                    new Color(0, 255, 255), // Cyan
                    new Color(0, 255, 0), // Grün
                    new Color(255, 255, 0), // Gelb
                    new Color(255, 0, 0) // Rot (hohe Werte)
            };
        } else {
            this.colors = colors;
        }
    }

    /**
     * Erzeugt das Standard-Farbschema
     */
    public static Plot3DColorScheme createDefault() {
        return new Plot3DColorScheme(
                new Color(0, 0, 255), // Blau (niedrige Werte)
                new Color(0, 255, 255), // Cyan
                new Color(0, 255, 0), // Grün
                new Color(255, 255, 0), // Gelb
                new Color(255, 0, 0) // Rot (hohe Werte)
        );
    }

    /**
     * Ermittelt eine Farbe für einen normierten Wert (0,0 bis 1,0)
     *
     * @param normalizedValue Wert zwischen 0,0 und 1,0
     * @return Die interpolierte Farbe aus dem Schema
     */
    public Color getColorForValue(double normalizedValue) {
        if (normalizedValue <= 0.0) {
            return colors[0];
        }

        if (normalizedValue >= 1.0) {
            return colors[colors.length - 1];
        }

        // Finde das Segment im Farbverlauf
        double segment = 1.0 / (colors.length - 1);
        int index = (int) (normalizedValue / segment);
        double remainder = (normalizedValue - index * segment) / segment;

        // Hole die beiden Farben, zwischen denen interpoliert werden soll
        Color c1 = colors[index];
        Color c2 = colors[index + 1];

        // Lineare Interpolation zwischen den beiden Farben
        int r = (int) (c1.getRed() + remainder * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + remainder * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + remainder * (c2.getBlue() - c1.getBlue()));

        return new Color(r, g, b);
    }

    /**
     * Ermittelt eine Farbe basierend auf einem z-Wert und dem aktuellen z-Bereich
     *
     * @param z    Der z-Wert, der einer Farbe zugeordnet werden soll
     * @param zMin Der minimale z-Wert im aktuellen Bereich
     * @param zMax Der maximale z-Wert im aktuellen Bereich
     * @return Die Farbe, die dem z-Wert entspricht
     */
    public Color getColorForZ(double z, double zMin, double zMax) {
        // Normalisiere den z-Wert in den Bereich 0,0 bis 1,0
        double normalizedZ = (z - zMin) / (zMax - zMin);
        return getColorForValue(normalizedZ);
    }
}
