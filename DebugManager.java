import javax.swing.*;
import java.awt.*;

/**
 * Manages the debug functionality
 */
public class DebugManager {
    private JTextArea debugTextArea;
    private JPanel debugPanel;
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
        JScrollPane debugScrollPane = new JScrollPane(debugTextArea);

        // Create debug panel
        debugPanel = new JPanel(new BorderLayout());
        debugPanel.add(new JLabel("Debug-Informationen:"), BorderLayout.NORTH);
        debugPanel.add(debugScrollPane, BorderLayout.CENTER);
        debugPanel.setVisible(false);
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
     * Returns the debug panel
     */
    public JPanel getDebugPanel() {
        return debugPanel;
    }

    /**
     * Sets the visibility of the debug panel
     */
    public void setVisible(boolean visible) {
        debugPanel.setVisible(visible);
    }

    /**
     * Returns if the debug panel is visible
     */
    public boolean isVisible() {
        return debugPanel.isVisible();
    }
}