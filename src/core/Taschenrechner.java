
package core;

import javax.swing.*;

import util.debug.DebugManager;
import util.history.HistoryManager;

import java.awt.*;
import java.awt.event.*;

/**
 * Main calculator class with UI components
 */
public class Taschenrechner extends JFrame {
    // UI components
    private JTextField displayField;

    // Core managers
    private HistoryManager historyManager;
    private DebugManager debugManager;
    private CalculationEngine calculationEngine;
    private TaschenrechnerKeypad keypad;
    private InputHandler inputHandler;

    // Status flags
    private boolean neueZahlBegonnen = true;

    public Taschenrechner() {
        // Window title and basic settings
        super("Taschenrechner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(400, 300));

        // Initialize managers
        debugManager = new DebugManager(this);
        historyManager = new HistoryManager(this);
        calculationEngine = new CalculationEngine(this);
        inputHandler = new InputHandler(this, calculationEngine);
        keypad = new TaschenrechnerKeypad(this, inputHandler);

        // Create input field
        displayField = new JTextField("0");
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setFont(new Font("Arial", Font.PLAIN, 24));
        displayField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    calculationEngine.berechneFormel();
                }
            }
        });

        // Layout assembly
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(displayField, BorderLayout.NORTH);
        mainPanel.add(keypad.createKeypadPanel(), BorderLayout.CENTER);

        // Main layout
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        debug("Taschenrechner gestartet");
    }

    /**
     * Toggles the history panel visibility
     */
    public void toggleHistory() {
        if (historyManager.isVisible()) {
            historyManager.hideHistoryDialog();
            debug("History-Dialog ausgeblendet");
        } else {
            historyManager.showHistoryDialog();
            debug("History-Dialog angezeigt");
        }
    }

    /**
     * Toggles the debug panel visibility
     */
    public void toggleDebug() {
        if (debugManager.isVisible()) {
            debugManager.hideDebugDialog();
            debug("Debug-Dialog ausgeblendet");
        } else {
            debugManager.showDebugDialog();
            debug("Debug-Dialog angezeigt");
        }
    }

    /**
     * Adds a debug message
     */
    public void debug(String message) {
        debugManager.debug(message);
    }

    /**
     * Adds a calculation to the history
     */
    public void addToHistory(String calculation, String result) {
        historyManager.addToHistory(calculation, result);
    }

    /**
     * Gets the display text
     */
    public String getDisplayText() {
        return displayField.getText();
    }

    /**
     * Sets the display text
     */
    public void setDisplayText(String text) {
        displayField.setText(text);
    }

    /**
     * Checks if a new number has been started
     */
    public boolean isNeueZahlBegonnen() {
        return neueZahlBegonnen;
    }

    /**
     * Sets the flag for starting a new number
     */
    public void setNeueZahlBegonnen(boolean neueZahlBegonnen) {
        this.neueZahlBegonnen = neueZahlBegonnen;
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        // Start the GUI in the Event-Dispatch-Thread
        SwingUtilities.invokeLater(() -> {
            Taschenrechner taschenrechner = new Taschenrechner();
            taschenrechner.setVisible(true);
        });
    }
}