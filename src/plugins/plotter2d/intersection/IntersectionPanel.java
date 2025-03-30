
package plugins.plotter2d.intersection;

import javax.swing.*;

import plugins.plotter2d.PlotterPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel for displaying intersection points
 */
public class IntersectionPanel {
    private final PlotterPanel plotter;
    private final DefaultListModel<String> intersectionListModel;
    private final JList<String> intersectionList;
    private JDialog intersectionDialog;
    private final JCheckBox showIntersectionsCheckbox;

    /**
     * Creates a new intersection panel
     */
    public IntersectionPanel(PlotterPanel plotter) {
        this.plotter = plotter;

        // Create intersection list model and list
        intersectionListModel = new DefaultListModel<>();
        intersectionList = new JList<>(intersectionListModel);
        intersectionList.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Create checkbox for showing intersections
        showIntersectionsCheckbox = new JCheckBox("Schnittpunkte anzeigen");
        showIntersectionsCheckbox.setSelected(false);
        showIntersectionsCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = showIntersectionsCheckbox.isSelected();
                plotter.getGraphPanel().toggleIntersections(selected);

                if (selected) {
                    // Update the intersection list
                    plotter.updateIntersectionList();
                    // Show dialog with the list
                    showIntersectionDialog();
                } else {
                    // Hide the dialog and clear the list
                    hideIntersectionDialog();
                    intersectionListModel.clear();
                }
            }
        });
    }

    /**
     * Creates and shows the intersection dialog
     */
    private void showIntersectionDialog() {
        if (intersectionDialog == null) {
            // Create the dialog if it doesn't exist
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(plotter);
            intersectionDialog = new JDialog(parentFrame, "Gefundene Schnittpunkte", false);

            // Create scroll pane for intersection list
            JScrollPane intersectionScrollPane = new JScrollPane(intersectionList);
            intersectionScrollPane.setPreferredSize(new Dimension(500, 200));

            // Create panel for the list with a title
            JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            contentPanel.add(new JLabel("Schnittpunkte:"), BorderLayout.NORTH);
            contentPanel.add(intersectionScrollPane, BorderLayout.CENTER);

            // Add button to close the dialog
            JButton closeButton = new JButton("Schließen");
            closeButton.addActionListener(e -> intersectionDialog.setVisible(false));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            intersectionDialog.setContentPane(contentPanel);
            intersectionDialog.setSize(550, 300);
            intersectionDialog.setLocationRelativeTo(parentFrame);

            // Make sure dialog doesn't close the app when closed
            intersectionDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }

        // Update the list and show the dialog
        plotter.updateIntersectionList();
        intersectionDialog.setVisible(true);
    }

    /**
     * Hides the intersection dialog
     */
    private void hideIntersectionDialog() {
        if (intersectionDialog != null && intersectionDialog.isVisible()) {
            intersectionDialog.setVisible(false);
        }
    }

    /**
     * Returns the checkbox for showing intersections
     * (For use in the display options panel)
     */
    public JCheckBox getShowIntersectionsCheckbox() {
        return showIntersectionsCheckbox;
    }

    /**
     * Returns the intersection list model
     */
    public DefaultListModel<String> getIntersectionListModel() {
        return intersectionListModel;
    }

    /**
     * Returns whether intersections are being shown
     */
    public boolean isShowingIntersections() {
        return showIntersectionsCheckbox.isSelected();
    }
}