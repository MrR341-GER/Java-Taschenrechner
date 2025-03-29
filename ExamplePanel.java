import javax.swing.*;
import java.awt.*;

/**
 * Panel with function example buttons
 */
public class ExamplePanel {
    private final PlotterPanel plotter;
    private final JTextField functionField;

    // Function examples
    private final String[] examples = {
            "x^2", "sin(x)", "cos(x)", "tan(x)", "x^3-3*x", "sqrt(x)",
            "log(x)", "ln(x)", "abs(x)", "exp(x)", "sin(x)+cos(x)", "e^(0.05*x)*sin(x)"
    };

    /**
     * Creates a new example panel
     */
    public ExamplePanel(PlotterPanel plotter, JTextField functionField) {
        this.plotter = plotter;
        this.functionField = functionField;
    }

    /**
     * Creates the example panel with function buttons
     */
    public JPanel createExamplesPanel() {
        JPanel examplesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        examplesPanel.setBorder(BorderFactory.createTitledBorder("Beispiele"));

        // Create a button for each example
        for (String example : examples) {
            JButton exampleButton = new JButton(example);
            exampleButton.setFont(new Font("Arial", Font.PLAIN, 11));
            exampleButton.addActionListener(e -> functionField.setText(example));
            examplesPanel.add(exampleButton);
        }

        return examplesPanel;
    }
}
