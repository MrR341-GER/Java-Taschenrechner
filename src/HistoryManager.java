import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Manages the calculation history
 */
public class HistoryManager {
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private JDialog historyDialog;
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

        // Create dialog for the history
        createHistoryDialog();
    }

    /**
     * Creates the dialog for displaying history
     */
    private void createHistoryDialog() {
        // Erstelle den Dialog, wenn er noch nicht existiert
        historyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(calculator), "Berechnungsverlauf");
        historyDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        // Erstelle das Panel mit allen Komponenten für den Dialog
        JPanel dialogPanel = new JPanel(new BorderLayout(5, 5));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Überschrift
        JLabel titleLabel = new JLabel("Berechnungsverlauf:");
        dialogPanel.add(titleLabel, BorderLayout.NORTH);

        // Scrollbare Liste mit allen Berechnungen
        JScrollPane historyScrollPane = new JScrollPane(historyList);
        historyScrollPane.setPreferredSize(new Dimension(400, 300));
        dialogPanel.add(historyScrollPane, BorderLayout.CENTER);

        // Panel für Buttons am unteren Rand
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Lösch-Button für den Verlauf
        JButton clearHistoryButton = new JButton("Verlauf löschen");
        clearHistoryButton.addActionListener(e -> {
            historyModel.clear();
            calculationHistory.clear();
            calculator.debug("Berechnungsverlauf gelöscht");
        });

        // Schließen-Button
        JButton closeButton = new JButton("Schließen");
        closeButton.addActionListener(e -> hideHistoryDialog());

        buttonPanel.add(clearHistoryButton);
        buttonPanel.add(closeButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Dialog konfigurieren
        historyDialog.setContentPane(dialogPanel);
        historyDialog.setSize(450, 400);

        // Dialog mittig zum Hauptfenster positionieren
        historyDialog.setLocationRelativeTo(calculator);
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
     * Shows the history dialog
     */
    public void showHistoryDialog() {
        if (historyDialog != null) {
            historyDialog.setVisible(true);
        }
    }

    /**
     * Hides the history dialog
     */
    public void hideHistoryDialog() {
        if (historyDialog != null) {
            historyDialog.setVisible(false);
        }
    }

    /**
     * Returns if the history dialog is visible
     */
    public boolean isVisible() {
        return historyDialog != null && historyDialog.isVisible();
    }

    /**
     * Updates the dialog position relative to the parent window
     */
    public void updateDialogPosition() {
        if (historyDialog != null && calculator != null) {
            historyDialog.setLocationRelativeTo(calculator);
        }
    }
}