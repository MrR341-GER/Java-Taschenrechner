package plugins;

import javax.swing.*;

import plugins.plotter2d.PlotterPanel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Panel for view control and centering
 */
public class ViewControlPanel {
    private final PlotterPanel plotter;
    private final JTextField xCenterField;
    private final JTextField yCenterField;

    /**
     * Creates a new view control panel
     */
    public ViewControlPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Center coordinate input fields
        xCenterField = new JTextField(5);
        yCenterField = new JTextField(5);
    }

    /**
     * Creates the view centering panel
     */
    public JPanel createViewControlPanel() {
        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.setBorder(BorderFactory.createTitledBorder("Ansicht zentrieren"));

        // GridBagConstraints für ein responsiveres Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // X-Koordinate Label und Feld
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        centeringPanel.add(new JLabel("x:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        centeringPanel.add(xCenterField, gbc);

        // Y-Koordinate Label und Feld
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        centeringPanel.add(new JLabel("y:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.4;
        centeringPanel.add(yCenterField, gbc);

        // Buttons in einer zweiten Zeile
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));

        JButton centerButton = new JButton("Zentrieren");
        centerButton.addActionListener(e -> centerGraphView());

        JButton resetViewButton = new JButton("Ansicht zurücksetzen");
        resetViewButton.addActionListener(e -> {
            plotter.getGraphPanel().resetView();
            // The centering fields will be updated by the property change listener
        });

        buttonPanel.add(centerButton);
        buttonPanel.add(resetViewButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        centeringPanel.add(buttonPanel, gbc);

        return centeringPanel;
    }

    /**
     * Updates the centering fields with the current view center
     */
    public void updateCenteringFields() {
        Point2D.Double center = plotter.getGraphPanel().getViewCenter();
        xCenterField.setText(plotter.getCoordinateFormat().format(center.x));
        yCenterField.setText(plotter.getCoordinateFormat().format(center.y));
    }

    /**
     * Centers the graph view on the entered X and Y coordinates
     */
    private void centerGraphView() {
        try {
            // Extract the entered values
            String xText = xCenterField.getText().trim();
            String yText = yCenterField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty()) {
                JOptionPane.showMessageDialog(plotter,
                        "Bitte geben Sie gültige X- und Y-Werte ein.",
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Try to parse the values (supports both dot and comma)
            double xCenter = parseDecimal(xText);
            double yCenter = parseDecimal(yText);

            // Call the method to center the view in the GraphPanel
            plotter.getGraphPanel().centerViewAt(xCenter, yCenter);

            // Also update the intersection list if intersections are shown
            if (plotter.isShowingIntersections()) {
                plotter.updateIntersectionList();
            }

        } catch (NumberFormatException | ParseException e) {
            JOptionPane.showMessageDialog(plotter,
                    "Ungültige Zahlenformate. Bitte geben Sie gültige Zahlen ein." +
                            "\n(Hinweis: Sowohl Punkt als auch Komma als Dezimaltrennzeichen werden akzeptiert)",
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Parses a decimal value from a string, supports both dot and comma
     * as decimal separators
     */
    private double parseDecimal(String text) throws NumberFormatException, ParseException {
        // First method: Direct parsing (for dot as decimal separator)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Second method: Replace comma with dot and try again
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Third method: Use the current locale
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }
}