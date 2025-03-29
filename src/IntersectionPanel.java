import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel for displaying intersection points
 */
public class IntersectionPanel {
    private final PlotterPanel plotter;
    private final DefaultListModel<String> intersectionListModel;
    private final JList<String> intersectionList;
    private final JPanel intersectionPanel;
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

        // Create scroll pane for intersection list
        JScrollPane intersectionScrollPane = new JScrollPane(intersectionList);
        intersectionScrollPane.setPreferredSize(new Dimension(300, 100));
        intersectionScrollPane.setMinimumSize(new Dimension(100, 50));

        // Create intersection panel
        intersectionPanel = new JPanel(new BorderLayout(5, 5));
        intersectionPanel.add(new JLabel("Gefundene Schnittpunkte:"), BorderLayout.NORTH);
        intersectionPanel.add(intersectionScrollPane, BorderLayout.CENTER);
        intersectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        intersectionPanel.setVisible(false); // Hidden by default

        // Create checkbox for showing intersections
        showIntersectionsCheckbox = new JCheckBox("Schnittpunkte anzeigen");
        showIntersectionsCheckbox.setSelected(false);
        showIntersectionsCheckbox.addActionListener(e -> {
            boolean selected = showIntersectionsCheckbox.isSelected();
            plotter.getGraphPanel().toggleIntersections(selected);
            intersectionPanel.setVisible(selected);

            if (selected) {
                // Update the intersection list
                plotter.updateIntersectionList();
            } else {
                // Clear the intersection list
                intersectionListModel.clear();
            }
        });
    }

    /**
     * Creates the options panel with intersection checkbox
     */
    public JPanel createOptionsPanel() {
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        optionsPanel.add(showIntersectionsCheckbox);
        return optionsPanel;
    }

    /**
     * Returns the intersection panel
     */
    public JPanel getIntersectionPanel() {
        return intersectionPanel;
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