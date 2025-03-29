import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * PlotterPanel is a container panel that contains GraphPanel and control
 * elements
 */
public class PlotterPanel extends JPanel {
    private final GraphPanel graphPanel;
    private final FunctionInputPanel functionInputPanel;
    private final ViewControlPanel viewControlPanel;
    private final IntersectionPanel intersectionPanel;
    private final ExamplePanel examplePanel;

    // Formatter for coordinates
    private final DecimalFormat coordinateFormat = new DecimalFormat("0.##");

    /**
     * Creates a new PlotterPanel
     */
    public PlotterPanel() {
        setLayout(new BorderLayout(5, 5));

        // Create graph panel
        graphPanel = new GraphPanel();

        // Create helper panels
        functionInputPanel = new FunctionInputPanel(this);
        viewControlPanel = new ViewControlPanel(this);
        intersectionPanel = new IntersectionPanel(this);

        // Add listener for dynamic update of the intersection list
        graphPanel.addPropertyChangeListener("intersectionsUpdated", evt -> {
            if (intersectionPanel.isShowingIntersections()) {
                updateIntersectionList();
            }
        });

        // Add listener for updating the centering fields when the view changes
        graphPanel.addPropertyChangeListener("viewChanged", evt -> {
            viewControlPanel.updateCenteringFields();
        });

        // Example panel needs reference to function field
        examplePanel = new ExamplePanel(this, getTextField());

        // Control area setup
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // Create a box for organizing the control panels
        Box controlBox = Box.createVerticalBox();
        controlBox.add(functionInputPanel.createFunctionInputPanel());
        controlBox.add(functionInputPanel.createActionButtonPanel());
        controlBox.add(functionInputPanel.createFunctionListPanel());
        controlBox.add(Box.createVerticalStrut(5));
        controlBox.add(viewControlPanel.createViewControlPanel());
        controlBox.add(Box.createVerticalStrut(5));
        controlBox.add(intersectionPanel.createOptionsPanel());

        // Add control box to control panel
        controlPanel.add(controlBox, BorderLayout.NORTH);
        controlPanel.add(examplePanel.createExamplesPanel(), BorderLayout.SOUTH);

        // Configure a split pane for controls and intersections
        JSplitPane controlSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                controlPanel,
                intersectionPanel.getIntersectionPanel());
        controlSplitPane.setResizeWeight(0.7);
        controlSplitPane.setContinuousLayout(true);
        controlSplitPane.setOneTouchExpandable(true);

        // Main window with adjustable size for control panel
        JSplitPane mainSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                graphPanel,
                controlSplitPane);
        mainSplitPane.setResizeWeight(1.0); // The graph panel gets most of the space when resizing
        mainSplitPane.setOneTouchExpandable(true); // One-click resizing
        mainSplitPane.setContinuousLayout(true);

        // At startup, set a reasonable position (70% for graph, 30% for controls)
        mainSplitPane.setDividerLocation(0.7);

        // Main layout
        add(mainSplitPane, BorderLayout.CENTER);

        // Initialize the centering fields with the current view
        viewControlPanel.updateCenteringFields();
    }

    /**
     * Returns the graph panel
     */
    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    /**
     * Returns the coordinate format
     */
    public DecimalFormat getCoordinateFormat() {
        return coordinateFormat;
    }

    /**
     * Returns the function field from the function input panel
     */
    private JTextField getTextField() {
        // This is a workaround since we don't have direct access to the function field
        // In a real refactoring, we would modify the FunctionInputPanel class to expose
        // this
        Component[] components = functionInputPanel.createFunctionInputPanel().getComponents();
        for (Component component : components) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            }
        }
        return new JTextField(); // Fallback, should never happen
    }

    /**
     * Updates the graph from the function list
     */
    public void updateGraphFromList() {
        graphPanel.clearFunctions();

        DefaultListModel<String> listModel = functionInputPanel.getFunctionListModel();
        Color[] availableColors = functionInputPanel.getAvailableColors();
        String[] colorNames = functionInputPanel.getColorNames();

        for (int i = 0; i < listModel.size(); i++) {
            String entry = listModel.get(i);

            // Extract the actual function from the list entry
            String funcPart = entry.substring(entry.indexOf('=') + 1, entry.lastIndexOf('[')).trim();

            // Extract the color
            String colorPart = entry.substring(entry.lastIndexOf('[') + 1, entry.lastIndexOf(']')).trim();
            int colorIndex = -1;

            for (int j = 0; j < colorNames.length; j++) {
                if (colorNames[j].equals(colorPart)) {
                    colorIndex = j;
                    break;
                }
            }

            Color color = (colorIndex >= 0)
                    ? availableColors[colorIndex]
                    : availableColors[i % availableColors.length];

            // Add function
            graphPanel.addFunction(funcPart, color);
        }

        // Update intersections if enabled
        if (intersectionPanel.isShowingIntersections()) {
            graphPanel.toggleIntersections(true);
            updateIntersectionList();
        }
    }

    /**
     * Returns if intersections are being shown
     */
    public boolean isShowingIntersections() {
        return intersectionPanel.isShowingIntersections();
    }

    /**
     * Updates the intersection list with the current intersection points
     */
    public void updateIntersectionList() {
        // Clear the list
        DefaultListModel<String> listModel = intersectionPanel.getIntersectionListModel();
        listModel.clear();

        // Get intersection points from the GraphPanel
        List<IntersectionPoint> intersectionPoints = graphPanel.getIntersectionPoints();

        if (intersectionPoints.isEmpty()) {
            listModel.addElement("Keine Schnittpunkte gefunden");
            return;
        }

        // For each intersection point, add to the list
        for (int i = 0; i < intersectionPoints.size(); i++) {
            IntersectionPoint point = intersectionPoints.get(i);

            // Extract function expressions from the function list for better display
            String func1 = getFunctionExpressionByIndex(point.getFunctionIndex1());
            String func2 = getFunctionExpressionByIndex(point.getFunctionIndex2());

            // Create an informative list entry
            String entry = "S" + (i + 1) + ": (" + coordinateFormat.format(point.x) + ", " +
                    coordinateFormat.format(point.y) + ") ";
            entry += "zwischen " + func1 + " und " + func2;

            listModel.addElement(entry);
        }
    }

    /**
     * Helper method to get the function expression by index
     */
    private String getFunctionExpressionByIndex(int index) {
        DefaultListModel<String> listModel = functionInputPanel.getFunctionListModel();

        if (index < 0 || index >= listModel.size()) {
            return "f" + (index + 1);
        }

        String entry = listModel.get(index);

        // Extract the function expression (between "=" and "[")
        int equalsPos = entry.indexOf('=');
        int bracketPos = entry.lastIndexOf('[');

        if (equalsPos >= 0 && bracketPos > equalsPos) {
            return entry.substring(equalsPos + 1, bracketPos).trim();
        } else {
            return "f" + (index + 1);
        }
    }
}