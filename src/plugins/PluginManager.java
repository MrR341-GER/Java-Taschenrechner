package plugins;

import java.util.HashMap;
import java.util.Map;
import core.Taschenrechner;
import core.GrafischerTaschenrechner;
import javax.swing.JPanel;

/**
 * Verwaltet die Plugins des Taschenrechners
 * Ermöglicht das Registrieren, Initialisieren und Erstellen von Plugin-Tabs
 */
public class PluginManager {
    private final Map<String, CalculatorPlugin> plugins = new HashMap<>();
    private final Taschenrechner calculator;

    /**
     * Erstellt einen neuen PluginManager
     * 
     * @param calculator Der Taschenrechner, zu dem Plugins hinzugefügt werden
     *                   sollen
     */
    public PluginManager(Taschenrechner calculator) {
        this.calculator = calculator;
    }

    /**
     * Registriert ein neues Plugin
     * 
     * @param plugin Das zu registrierende Plugin
     */
    public void registerPlugin(CalculatorPlugin plugin) {
        plugins.put(plugin.getName(), plugin);
        plugin.initialize();
    }

    /**
     * Gibt ein Plugin anhand seines Namens zurück
     * 
     * @param name Der Name des Plugins
     * @return Das Plugin oder null, wenn kein Plugin mit diesem Namen existiert
     */
    public CalculatorPlugin getPlugin(String name) {
        return plugins.get(name);
    }

    /**
     * Erstellt Tabs für alle registrierten Plugins
     * Fügt die Tabs zum Taschenrechner hinzu, falls dieser ein
     * GrafischerTaschenrechner ist
     */
    public void createAllPluginTabs() {
        // Prüfe, ob der Taschenrechner ein GrafischerTaschenrechner ist
        if (calculator instanceof GrafischerTaschenrechner) {
            GrafischerTaschenrechner graphCalc = (GrafischerTaschenrechner) calculator;

            // Erstellt Tabs für alle registrierten Plugins
            for (CalculatorPlugin plugin : plugins.values()) {
                try {
                    JPanel pluginPanel = plugin.createPanel(calculator);
                    graphCalc.addTab(plugin.getName(), pluginPanel);
                    System.out.println("Plugin-Tab hinzugefügt: " + plugin.getName());
                } catch (Exception e) {
                    System.err.println("Fehler beim Erstellen des Tabs für Plugin " +
                            plugin.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Fehler: Tabs können nur zu einem GrafischerTaschenrechner hinzugefügt werden");
        }
    }

    /**
     * Gibt alle registrierten Plugins zurück
     * 
     * @return Die Map mit allen Plugins (Name -> Plugin)
     */
    public Map<String, CalculatorPlugin> getAllPlugins() {
        return new HashMap<>(plugins); // Defensive Kopie
    }

    /**
     * Deaktiviert alle Plugins und gibt Ressourcen frei
     * Wird beim Beenden der Anwendung aufgerufen
     */
    public void shutdownAllPlugins() {
        for (CalculatorPlugin plugin : plugins.values()) {
            try {
                plugin.shutdown();
                System.out.println("Plugin heruntergefahren: " + plugin.getName());
            } catch (Exception e) {
                System.err.println("Fehler beim Herunterfahren des Plugins " +
                        plugin.getName() + ": " + e.getMessage());
            }
        }
        plugins.clear();
    }
}