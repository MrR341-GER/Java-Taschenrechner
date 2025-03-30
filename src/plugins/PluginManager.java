package plugins;

import java.util.HashMap;
import java.util.Map;
import core.Taschenrechner;

public class PluginManager {
    private final Map<String, CalculatorPlugin> plugins = new HashMap<>();
    private final Taschenrechner calculator;

    public PluginManager(Taschenrechner calculator) {
        this.calculator = calculator;
    }

    public void registerPlugin(CalculatorPlugin plugin) {
        plugins.put(plugin.getName(), plugin);
        plugin.initialize();
    }

    public CalculatorPlugin getPlugin(String name) {
        return plugins.get(name);
    }

    public void createAllPluginTabs() {
        // Erstellt Tabs für alle registrierten Plugins
        for (CalculatorPlugin plugin : plugins.values()) {
            calculator.addTab(plugin.getName(), plugin.createPanel(calculator));
        }
    }
}
