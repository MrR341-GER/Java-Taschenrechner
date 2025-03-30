
package plugins.statistics;

import javax.swing.JPanel;
import plugins.CalculatorPlugin;
import core.GrafischerTaschenrechner;
import core.Taschenrechner;

/**
 * Plugin für die statistische Berechnungen
 */
public class StatisticsPlugin implements CalculatorPlugin {

    @Override
    public String getName() {
        return "Statistik";
    }

    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        // Typumwandlung erforderlich, da das Plugin den GrafischerTaschenrechner
        // benötigt
        if (calculator instanceof GrafischerTaschenrechner) {
            return new StatisticsPanel((GrafischerTaschenrechner) calculator);
        }
        throw new IllegalArgumentException("Das Statistik-Modul benötigt einen GrafischerTaschenrechner");
    }

    @Override
    public void initialize() {
        // Initialisierungslogik, falls erforderlich
    }

    @Override
    public void shutdown() {
        // Ressourcen freigeben, falls erforderlich
    }
}