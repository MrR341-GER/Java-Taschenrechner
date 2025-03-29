import javax.swing.*;
import java.awt.*;

/**
 * Manages the debug functionality
 */
public class DebugManager {
    private JTextArea debugTextArea;
    private JDialog debugDialog;
    private final Taschenrechner calculator;

    public DebugManager(Taschenrechner calculator) {
        this.calculator = calculator;
        initializeDebugPanel();
    }

    /**
     * Initializes the debug panel
     */
    private void initializeDebugPanel() {
        // Create debug text area
        debugTextArea = new JTextArea(10, 40);
        debugTextArea.setEditable(false);
        debugTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Create dialog for debug information
        createDebugDialog();
    }

    /**
     * Creates the dialog for displaying debug information
     */
    private void createDebugDialog() {
        // Erstelle den Dialog
        debugDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(calculator), "Debug-Informationen");
        debugDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        // Erstelle das Panel mit allen Komponenten für den Dialog
        JPanel dialogPanel = new JPanel(new BorderLayout(5, 5));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Überschrift
        JLabel titleLabel = new JLabel("Debug-Ausgaben:");
        dialogPanel.add(titleLabel, BorderLayout.NORTH);

        // Scrollbare Textfläche für Debug-Informationen
        JScrollPane debugScrollPane = new JScrollPane(debugTextArea);
        debugScrollPane.setPreferredSize(new Dimension(500, 400));
        dialogPanel.add(debugScrollPane, BorderLayout.CENTER);

        // Panel für Buttons am unteren Rand
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Lösch-Button für Debug-Ausgaben
        JButton clearButton = new JButton("Ausgaben löschen");
        clearButton.addActionListener(e -> {
            debugTextArea.setText("");
        });

        // Schließen-Button
        JButton closeButton = new JButton("Schließen");
        closeButton.addActionListener(e -> hideDebugDialog());

        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Dialog konfigurieren
        debugDialog.setContentPane(dialogPanel);
        debugDialog.setSize(550, 500);

        // Dialog mittig zum Hauptfenster positionieren
        debugDialog.setLocationRelativeTo(calculator);
    }

    /**
     * Adds a debug message
     */
    public void debug(String message) {
        System.out.println("[DEBUG] " + message);
        debugTextArea.append(message + "\n");
        // Scroll to the end
        debugTextArea.setCaretPosition(debugTextArea.getDocument().getLength());
    }

    /**
     * Shows the debug dialog
     */
    public void showDebugDialog() {
        if (debugDialog != null) {
            debugDialog.setVisible(true);
        }
    }

    /**
     * Hides the debug dialog
     */
    public void hideDebugDialog() {
        if (debugDialog != null) {
            debugDialog.setVisible(false);
        }
    }

    /**
     * Returns if the debug dialog is visible
     */
    public boolean isVisible() {
        return debugDialog != null && debugDialog.isVisible();
    }

    /**
     * Updates the dialog position relative to the parent window
     */
    public void updateDialogPosition() {
        if (debugDialog != null && calculator != null) {
            debugDialog.setLocationRelativeTo(calculator);
        }
    }
}