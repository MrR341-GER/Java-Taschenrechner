
package common;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;

/**
 * Zentrale Klasse für Konstanten und Standardwerte des Taschenrechners
 * Vereinheitlicht die Werte über alle Komponenten hinweg
 */
public class CalculatorConstants {
    // Allgemeine Designkonstanten
    public static final String APPLICATION_NAME = "Wissenschaftlicher Taschenrechner";
    public static final String APPLICATION_VERSION = "3.0";
    public static final String COPYRIGHT = "© 2025";

    // Standard-Schriftarten
    public static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font DISPLAY_FONT = new Font("Arial", Font.PLAIN, 24);
    public static final Font MONOSPACE_FONT = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font BUTTON_FONT = new Font("Arial", Font.PLAIN, 16);

    // Standardfarben
    public static final Color NORMAL_BUTTON_COLOR = new Color(240, 240, 240);
    public static final Color DIGIT_BUTTON_COLOR = Color.WHITE;
    public static final Color FUNCTION_BUTTON_COLOR = new Color(230, 230, 250); // Helles Lila
    public static final Color CLEAR_BUTTON_COLOR = new Color(255, 200, 200); // Helles Rot
    public static final Color EQUALS_BUTTON_COLOR = new Color(220, 250, 220); // Helles Grün

    // Formatierung für Zahlen
    public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("0.##########");
    public static final DecimalFormat COMPACT_DECIMAL_FORMAT = new DecimalFormat("0.##");
    public static final DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat("0.######E0");

    // Standard-Wertebereiche für Plotter
    public static final double DEFAULT_PLOT_MIN = -10.0;
    public static final double DEFAULT_PLOT_MAX = 10.0;

    // Standard-Auflösung für 3D-Plotter
    public static final int DEFAULT_3D_RESOLUTION = 30;
    public static final String DEFAULT_3D_FUNCTION = "sin(sqrt(x^2+y^2))";

    // Dateien und Pfade
    public static final String EXAMPLES_FILE = "function_examples.txt";

    // UI-Konstanten
    public static final int AXIS_MARGIN = 40;
    public static final int MIN_BUTTON_WIDTH = 40;
    public static final int MIN_BUTTON_HEIGHT = 35;
    public static final int PADDING = 10;

    // Mathematische Konstanten
    public static final double EPSILON = 1e-10; // Für Vergleiche mit Näherungswerten

    // Standard-Beispielfunktionen
    public static final String[] DEFAULT_FUNCTION_EXAMPLES = {
            "x^2", "sin(x)", "cos(x)", "tan(x)", "x^3-3*x", "sqrt(x)",
            "log(x)", "ln(x)", "abs(x)", "exp(x)", "sin(x)+cos(x)", "e^(0.05*x)*sin(x)"
    };

    /**
     * Private Konstruktor verhindert Instanziierung
     */
    private CalculatorConstants() {
        // Verhindert Instanziierung dieser Utility-Klasse
    }
}
