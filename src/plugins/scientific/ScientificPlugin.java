
package plugins.scientific;

import javax.swing.JPanel;
import plugins.CalculatorPlugin;
import core.GrafischerTaschenrechner;
import core.Taschenrechner;

/**
 * Plugin für den wissenschaftlichen Taschenrechner
 */
public class ScientificPlugin implements CalculatorPlugin {

    @Override
    public String getName() {
        return "Wissenschaftlich";
    }

    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        // Typumwandlung erforderlich, da das Plugin den GrafischerTaschenrechner
        // benötigt
        if (calculator instanceof GrafischerTaschenrechner) {
            return new ScientificPanel((GrafischerTaschenrechner) calculator);
        }
        throw new IllegalArgumentException("Der wissenschaftliche Rechner benötigt einen GrafischerTaschenrechner");
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