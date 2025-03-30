
package plugins.plotter2d;

import javax.swing.JPanel;
import plugins.CalculatorPlugin;
import core.GrafischerTaschenrechner;
import core.Taschenrechner;

/**
 * Plugin für den 2D-Funktionsplotter
 */
public class PlotterPlugin implements CalculatorPlugin {

    @Override
    public String getName() {
        return "Funktionsplotter";
    }

    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        // Typumwandlung erforderlich, da das Plugin den GrafischerTaschenrechner
        // benötigt
        if (calculator instanceof GrafischerTaschenrechner) {
            return new PlotterPanel((GrafischerTaschenrechner) calculator);
        }
        throw new IllegalArgumentException("Der 2D-Plotter benötigt einen GrafischerTaschenrechner");
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