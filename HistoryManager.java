import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Manages the calculation history
 */
public class HistoryManager {
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private JPanel historyPanel;
    private ArrayList<String> calculationHistory = new ArrayList<>();
    private final Taschenrechner calculator;

    public HistoryManager(Taschenrechner calculator) {
        this.calculator = calculator;
        initializeHistoryPanel();
    }

    /**
     * Initializes the history panel components
     */
    private void initializeHistoryPanel() {
        // Initialize the history model and list
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && historyList.getSelectedIndex() != -1) {
                String selected = historyList.getSelectedValue();
                if (selected != null && !selected.isEmpty()) {
                    // Extract the formula from the history entry (format: "Formula = Result")
                    String formel = selected.split("=")[0].trim();
                    calculator.setDisplayText(formel);
                    calculator.debug("Formel aus History geladen: " + formel);
                }
            }
        });

        // Create scroll pane for the history list
        JScrollPane historyScrollPane = new JScrollPane(historyList);

        // Create the history panel
        historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("Berechnungsverlauf:"), BorderLayout.NORTH);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        // Create clear history button
        JButton clearHistoryButton = new JButton("Verlauf löschen");
        clearHistoryButton.addActionListener(e -> {
            historyModel.clear();
            calculationHistory.clear();
            calculator.debug("Berechnungsverlauf gelöscht");
        });
        historyPanel.add(clearHistoryButton, BorderLayout.SOUTH);
    }

    /**
     * Adds a calculation to the history
     */
    public void addToHistory(String calculation, String result) {
        String historyEntry = calculation + " = " + result;
        calculationHistory.add(historyEntry);
        historyModel.addElement(historyEntry);

        // Scroll to the latest entry
        historyList.ensureIndexIsVisible(historyModel.getSize() - 1);

        calculator.debug("Zur History hinzugefügt: " + historyEntry);
    }

    /**
     * Returns the history panel
     */
    public JPanel getHistoryPanel() {
        return historyPanel;
    }

    /**
     * Sets the visibility of the history panel
     */
    public void setVisible(boolean visible) {
        historyPanel.setVisible(visible);
    }

    /**
     * Returns if the history panel is visible
     */
    public boolean isVisible() {
        return historyPanel.isVisible();
    }
}