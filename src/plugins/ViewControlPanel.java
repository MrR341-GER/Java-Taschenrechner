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
 * Panel zur Steuerung und Zentrierung der Ansicht
 */
public class ViewControlPanel {
    private final PlotterPanel plotter;
    private final JTextField xCenterField;
    private final JTextField yCenterField;

    /**
     * Erstellt ein neues Panel zur Steuerung der Ansicht
     */
    public ViewControlPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Eingabefelder für die Zentrumkoordinaten
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

        // X-Koordinate: Label und Feld
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        centeringPanel.add(new JLabel("x:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        centeringPanel.add(xCenterField, gbc);

        // Y-Koordinate: Label und Feld
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

        // UI-Komponenten zusammenfügen
        return centeringPanel;
    }

    /**
     * Aktualisiert die Zentrierungsfelder mit dem aktuellen Ansichts-Zentrum
     */
    public void updateCenteringFields() {
        Point2D.Double center = plotter.getGraphPanel().getViewCenter();
        xCenterField.setText(plotter.getCoordinateFormat().format(center.x));
        yCenterField.setText(plotter.getCoordinateFormat().format(center.y));
    }

    /**
     * Zentriert die Graph-Ansicht auf die eingegebenen X- und Y-Koordinaten
     */
    private void centerGraphView() {
        try {
            // Extrahiere die eingegebenen Werte
            String xText = xCenterField.getText().trim();
            String yText = yCenterField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty()) {
                JOptionPane.showMessageDialog(plotter,
                        "Bitte geben Sie gültige X- und Y-Werte ein.",
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Versuche, die Werte zu parsen (unterstützt sowohl Punkt als auch Komma als
            // Dezimaltrennzeichen)
            double xCenter = parseDecimal(xText);
            double yCenter = parseDecimal(yText);

            // Rufe die Methode auf, die die Ansicht im GraphPanel zentriert
            plotter.getGraphPanel().centerViewAt(xCenter, yCenter);

            // Aktualisiere auch die Schnittpunktliste, falls Schnittpunkte angezeigt werden
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
        // Erste Methode: Direkte Umwandlung (für Punkt als Dezimaltrennzeichen)
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // Zweite Methode: Ersetze Komma durch Punkt und versuche es erneut
            String replacedText = text.replace(',', '.');
            try {
                return Double.parseDouble(replacedText);
            } catch (NumberFormatException ex) {
                // Dritte Methode: Verwende die aktuelle Locale
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                return nf.parse(text).doubleValue();
            }
        }
    }
}
