import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Eine Klasse zum Auswählen und Verwalten von Farben für Funktionen im
 * Funktionsplotter
 */
public class ColorChooser {
    // Map zum Speichern von Farbzuordnungen und -namen
    private static Map<String, Color> colorMap = new HashMap<>();
    private static Map<Color, String> colorNameMap = new HashMap<>();

    // Konstante für die Zufallsoption
    public static final String RANDOM_COLOR_OPTION = "Zufällig";

    // Random-Generator für zufällige Farben
    private static final Random random = new Random();

    // Statischer Initialisierer für Standardfarben mit Namen
    static {
        addColorWithName(Color.RED, "Rot");
        addColorWithName(Color.BLUE, "Blau");
        addColorWithName(Color.GREEN, "Grün");
        addColorWithName(Color.MAGENTA, "Magenta");
        addColorWithName(Color.ORANGE, "Orange");
        addColorWithName(Color.CYAN, "Cyan");
        addColorWithName(new Color(128, 0, 128), "Lila");
        addColorWithName(new Color(165, 42, 42), "Braun");

        // Spezielle Option für zufällige Farben (ohne tatsächliche Farbe)
        colorMap.put(RANDOM_COLOR_OPTION, null);
    }

    /**
     * Fügt eine Farbe mit Namen hinzu
     */
    private static void addColorWithName(Color color, String name) {
        colorMap.put(name, color);
        colorNameMap.put(color, name);
    }

    /**
     * Zeigt den Farbauswahldialog an
     * 
     * @param parent       Das Elternfenster
     * @param title        Der Titel des Dialogs
     * @param initialColor Die Ausgangsfarbe
     * @return Die ausgewählte Farbe oder null, wenn abgebrochen wurde
     */
    public static Color showColorChooser(Component parent, String title, Color initialColor) {
        return JColorChooser.showDialog(parent, title, initialColor);
    }

    /**
     * Generiert eine komplett zufällige Farbe
     * 
     * @return Eine neue, zufällige Farbe
     */
    public static Color generateRandomColor() {
        return new Color(
                random.nextInt(256), // Rot (0-255)
                random.nextInt(256), // Grün (0-255)
                random.nextInt(256) // Blau (0-255)
        );
    }

    /**
     * Generiert eine deterministische "zufällige" Farbe basierend auf einem Seed
     * 
     * @param seed Der Seed für den Zufallsgenerator (z.B. die Funktionsformel)
     * @return Eine "zufällige" Farbe, die für denselben Seed immer gleich ist
     */
    public static Color generateDeterministicRandomColor(String seed) {
        // Einfache Hash-Funktion für den Seed
        int hash = seed.hashCode();
        // Verwende den Hash als Seed für einen neuen Random-Generator
        Random seededRandom = new Random(hash);

        return new Color(
                seededRandom.nextInt(256), // Rot (0-255)
                seededRandom.nextInt(256), // Grün (0-255)
                seededRandom.nextInt(256) // Blau (0-255)
        );
    }

    /**
     * Generiert dynamisch eine neue Farbe
     * 
     * @return Eine neue Farbe, die sich von den bisherigen unterscheidet
     */
    public static Color generateColor() {
        // HSB-Farbraum verwenden für gleichmäßige Verteilung
        // Basis auf der Anzahl der vorhandenen Farben
        float hue = (colorMap.size() * 0.618033988749895f) % 1.0f; // Goldener Schnitt für gute Verteilung
        float saturation = 0.8f; // Hohe Sättigung für kräftige Farben
        float brightness = 0.95f; // Hohe Helligkeit für gute Sichtbarkeit

        Color newColor = Color.getHSBColor(hue, saturation, brightness);

        // Farbname zuweisen
        String colorName = "Farbe " + (colorMap.size() + 1);
        addColorWithName(newColor, colorName);

        return newColor;
    }

    /**
     * Gibt den Namen einer Farbe zurück
     *
     * @param color Die Farbe
     * @return Der Name der Farbe oder "Unbekannt" wenn kein Name zugeordnet ist
     */
    public static String getColorName(Color color) {
        if (color == null)
            return "Keine";

        // Exakte Übereinstimmung
        if (colorNameMap.containsKey(color)) {
            return colorNameMap.get(color);
        }

        // Suche nach ähnlicher Farbe
        String closestName = "Benutzerdefiniert";
        int minDistance = Integer.MAX_VALUE;

        for (Map.Entry<Color, String> entry : colorNameMap.entrySet()) {
            Color existingColor = entry.getKey();
            int distance = colorDistance(color, existingColor);

            if (distance < minDistance) {
                minDistance = distance;
                closestName = entry.getValue();
            }
        }

        // Wenn die Farbe zu ähnlich ist, gleichen Namen verwenden
        if (minDistance < 50) {
            return closestName;
        }

        // Neue Farbe mit Namen registrieren
        String newName = "Farbe " + (colorMap.size() + 1);
        addColorWithName(color, newName);
        return newName;
    }

    /**
     * Berechnet den Abstand zwischen zwei Farben im RGB-Raum
     */
    private static int colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return (rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff);
    }

    /**
     * Gibt eine Farbe anhand des Namens zurück
     *
     * @param name Der Farbname
     * @return Die zugehörige Farbe oder null für "Zufällig"
     */
    public static Color getColorByName(String name) {
        // Für die Option "Zufällig" sofort eine zufällige Farbe erzeugen
        if (RANDOM_COLOR_OPTION.equals(name)) {
            return generateRandomColor();
        }

        return colorMap.getOrDefault(name, Color.BLACK);
    }

    /**
     * Gibt eine Farbe anhand des Namens zurück und benutzt für "Zufällig"
     * eine deterministische Farbe basierend auf dem Seed
     *
     * @param name Der Farbname
     * @param seed Der Seed für die deterministische Zufallsfarbe
     * @return Die zugehörige Farbe
     */
    public static Color getColorByName(String name, String seed) {
        // Für die Option "Zufällig" eine deterministische Farbe erzeugen
        if (RANDOM_COLOR_OPTION.equals(name)) {
            return generateDeterministicRandomColor(seed);
        }

        return colorMap.getOrDefault(name, Color.BLACK);
    }

    /**
     * Gibt alle verfügbaren Farbnamen zurück (inklusive "Zufällig")
     */
    public static String[] getColorNames() {
        return colorMap.keySet().toArray(new String[0]);
    }

    /**
     * Gibt alle verfügbaren Farben zurück (ohne null für "Zufällig")
     */
    public static Color[] getColors() {
        // Wir filtern null-Einträge heraus (die "Zufällig"-Option)
        return colorMap.values().stream()
                .filter(color -> color != null)
                .toArray(Color[]::new);
    }
}