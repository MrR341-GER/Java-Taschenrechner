package plugins.plotter2d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Panel zur Steuerung und Zentrierung der Ansicht
 */
public class ViewControlPanel {
    private final PlotterPanel plotter;
    private final JTextField xCenterField;
    private final JTextField yCenterField;

    /**
     * Erzeugt ein neues Panel zur Ansichtsteuerung
     */
    public ViewControlPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Eingabefelder für die Zentrierung der Koordinaten
        xCenterField = new JTextField(5);
        yCenterField = new JTextField(5);
    }

    /**
     * Erstellt das Panel zur Zentrierung der Ansicht
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
            // Die Zentrierungsfelder werden durch den Property-Change-Listener aktualisiert
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
     * Aktualisiert die Zentrierungsfelder mit dem aktuellen Mittelpunkt der Ansicht
     */
    public void updateCenteringFields() {
        Point2D.Double center = plotter.getGraphPanel().getViewCenter();
        xCenterField.setText(plotter.getCoordinateFormat().format(center.x));
        yCenterField.setText(plotter.getCoordinateFormat().format(center.y));
    }

    /**
     * Zentriert die Graphansicht auf die eingegebenen X- und Y-Koordinaten
     */
    private void centerGraphView() {
        try {
            // Liest die eingegebenen Werte aus
            String xText = xCenterField.getText().trim();
            String yText = yCenterField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty()) {
                JOptionPane.showMessageDialog(plotter,
                        "Bitte geben Sie gültige X- und Y-Werte ein.",
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Versucht, die Werte zu parsen (unterstützt sowohl Punkt als auch Komma)
            double xCenter = parseDecimal(xText);
            double yCenter = parseDecimal(yText);

            // Ruft die Methode zum Zentrieren der Ansicht im GraphPanel auf
            plotter.getGraphPanel().centerViewAt(xCenter, yCenter);

            // Aktualisiert auch die Schnittmengenliste, falls Schnittpunkte angezeigt
            // werden
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
     * Parst einen Dezimalwert aus einem String, unterstützt sowohl Punkt als auch
     * Komma
     * als Dezimaltrennzeichen
     */
    private double parseDecimal(String text) throws NumberFormatException, ParseException {
        // Erste Methode: Direktes Parsen (für Punkt als Dezimaltrennzeichen)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Zweite Methode: Ersetzt Komma durch Punkt und versucht es erneut
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Dritte Methode: Verwendet die aktuelle Locale
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }
}
