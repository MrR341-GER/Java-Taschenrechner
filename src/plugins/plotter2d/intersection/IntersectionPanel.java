package plugins.plotter2d.intersection;

import javax.swing.*;

import plugins.plotter2d.PlotterPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel zur Anzeige von Schnittpunkten
 */
public class IntersectionPanel {
    private final PlotterPanel plotter;
    private final DefaultListModel<String> intersectionListModel;
    private final JList<String> intersectionList;
    private JDialog intersectionDialog;
    private final JCheckBox showIntersectionsCheckbox;

    /**
     * Erstellt ein neues Schnittpunkt-Panel
     */
    public IntersectionPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Erstelle das Modell für die Schnittpunktliste und die Liste
        intersectionListModel = new DefaultListModel<>();
        intersectionList = new JList<>(intersectionListModel);
        intersectionList.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Erstelle ein Kontrollkästchen zum Anzeigen der Schnittpunkte
        showIntersectionsCheckbox = new JCheckBox("Schnittpunkte anzeigen");
        showIntersectionsCheckbox.setSelected(false);
        showIntersectionsCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = showIntersectionsCheckbox.isSelected();
                plotter.getGraphPanel().toggleIntersections(selected);

                if (selected) {
                    // Aktualisiere die Schnittpunktliste
                    plotter.updateIntersectionList();
                    // Zeige den Dialog mit der Liste
                    showIntersectionDialog();
                } else {
                    // Verberge den Dialog und leere die Liste
                    hideIntersectionDialog();
                    intersectionListModel.clear();
                }
            }
        });
    }

    /**
     * Erstellt und zeigt den Schnittpunkt-Dialog
     */
    private void showIntersectionDialog() {
        if (intersectionDialog == null) {
            // Erstelle den Dialog, falls er noch nicht existiert
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(plotter);
            intersectionDialog = new JDialog(parentFrame, "Gefundene Schnittpunkte", false);

            // Erstelle ein Scrollpane für die Schnittpunktliste
            JScrollPane intersectionScrollPane = new JScrollPane(intersectionList);
            intersectionScrollPane.setPreferredSize(new Dimension(500, 200));

            // Erstelle ein Panel für die Liste mit einer Überschrift
            JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            contentPanel.add(new JLabel("Schnittpunkte:"), BorderLayout.NORTH);
            contentPanel.add(intersectionScrollPane, BorderLayout.CENTER);

            // Füge einen Button zum Schließen des Dialogs hinzu
            JButton closeButton = new JButton("Schließen");
            closeButton.addActionListener(e -> intersectionDialog.setVisible(false));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            intersectionDialog.setContentPane(contentPanel);
            intersectionDialog.setSize(550, 300);
            intersectionDialog.setLocationRelativeTo(parentFrame);

            // Stelle sicher, dass der Dialog beim Schließen die Anwendung nicht beendet
            intersectionDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }

        // Aktualisiere die Liste und zeige den Dialog
        plotter.updateIntersectionList();
        intersectionDialog.setVisible(true);
    }

    /**
     * Verbirgt den Schnittpunkt-Dialog
     */
    private void hideIntersectionDialog() {
        if (intersectionDialog != null && intersectionDialog.isVisible()) {
            intersectionDialog.setVisible(false);
        }
    }

    /**
     * Gibt das Kontrollkästchen zum Anzeigen der Schnittpunkte zurück
     * (Zur Verwendung im Options-Panel)
     */
    public JCheckBox getShowIntersectionsCheckbox() {
        return showIntersectionsCheckbox;
    }

    /**
     * Gibt das Modell der Schnittpunktliste zurück
     */
    public DefaultListModel<String> getIntersectionListModel() {
        return intersectionListModel;
    }

    /**
     * Gibt zurück, ob Schnittpunkte angezeigt werden
     */
    public boolean isShowingIntersections() {
        return showIntersectionsCheckbox.isSelected();
    }
}
