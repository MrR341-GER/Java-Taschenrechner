
package plugins.plotter3d;

import javax.swing.JPanel;
import plugins.CalculatorPlugin;
import core.GrafischerTaschenrechner;
import core.Taschenrechner;

/**
 * Plugin für den 3D-Funktionsplotter
 */
public class Plot3DPlugin implements CalculatorPlugin {

    @Override
    public String getName() {
        return "3D-Funktionsplotter";
    }

    @Override
    public JPanel createPanel(Taschenrechner calculator) {
        // Typumwandlung erforderlich, da das Plugin den GrafischerTaschenrechner
        // benötigt
        if (calculator instanceof GrafischerTaschenrechner) {
            return new Plot3DPanel((GrafischerTaschenrechner) calculator);
        }
        throw new IllegalArgumentException("Der 3D-Plotter benötigt einen GrafischerTaschenrechner");
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