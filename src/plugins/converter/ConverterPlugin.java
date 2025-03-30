
package plugins.converter;

import javax.swing.JPanel;
import plugins.CalculatorPlugin;
import core.GrafischerTaschenrechner;
import core.Taschenrechner;

/**
 * Plugin für den Einheitenumrechner
 */
public class ConverterPlugin implements CalculatorPlugin {

    @Override
    public String getName() {
        return "Umrechner";
    }

    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        // Typumwandlung erforderlich, da das Plugin den GrafischerTaschenrechner
        // benötigt
        if (calculator instanceof GrafischerTaschenrechner) {
            return new ConverterPanel((GrafischerTaschenrechner) calculator);
        }
        throw new IllegalArgumentException("Der Umrechner benötigt einen GrafischerTaschenrechner");
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