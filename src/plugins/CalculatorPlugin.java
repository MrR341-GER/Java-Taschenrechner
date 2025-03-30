
package plugins;

import javax.swing.JPanel;
import core.Taschenrechner;

public interface CalculatorPlugin {
    String getName(); // Name des Plugins

    JPanel createPanel(Taschenrechner calculator); // Panel-Erstellung

    void initialize(); // Initialisierung

    void shutdown(); // Aufräumen bei Beendigung
}
