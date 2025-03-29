import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Creates and manages the keypad of the calculator
 */
public class TaschenrechnerKeypad {
    // Constants for text buffer (additional space around text)
    private static final int TEXT_PADDING_HORIZONTAL = 16;
    private static final int TEXT_PADDING_VERTICAL = 10;
    private static final int MINIMUM_BUTTON_WIDTH = 40;
    private static final int MINIMUM_BUTTON_HEIGHT = 35;

    private final Taschenrechner calculator;
    private final ActionListener inputHandler;

    public TaschenrechnerKeypad(Taschenrechner calculator, ActionListener inputHandler) {
        this.calculator = calculator;
        this.inputHandler = inputHandler;
    }

    /**
     * Creates the complete button panel
     */
    public JPanel createKeypadPanel() {
        // Main button panel with BorderLayout
        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));

        // Control buttons
        JButton debugButton = new JButton("Debug");
        debugButton.setFont(new Font("Arial", Font.PLAIN, 16));
        debugButton.setBackground(new Color(255, 200, 200)); // Light red
        adjustButtonSize(debugButton);
        debugButton.addActionListener(e -> calculator.toggleDebug());

        JButton historyButton = new JButton("History");
        historyButton.setFont(new Font("Arial", Font.PLAIN, 16));
        historyButton.setBackground(new Color(200, 230, 255)); // Light blue
        adjustButtonSize(historyButton);
        historyButton.addActionListener(e -> calculator.toggleHistory());

        // Panel for control buttons
        JPanel controlButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlButtonPanel.add(debugButton);
        controlButtonPanel.add(historyButton);

        // Panels for different button groups with fixed spacing
        JPanel ziffernPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        JPanel operatorenPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        JPanel funktionsPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JPanel wissenschaftlichPanel = new JPanel(new GridLayout(1, 6, 5, 5));

        // Number buttons (0-9 and .)
        String[] ziffern = { "7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "+/-" };
        for (String ziffer : ziffern) {
            JButton button = new JButton(ziffer);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            ziffernPanel.add(button);
        }

        // Operators
        String[] operatoren = { "/", "*", "-", "+", "=" };
        for (String operator : operatoren) {
            JButton button = new JButton(operator);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Light lilac
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            operatorenPanel.add(button);
        }

        // Function buttons
        String[] funktionen = { "C", "(", ")", "←" };
        for (String funktion : funktionen) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            adjustButtonSize(button);

            // Special colors for function buttons
            if (funktion.equals("C")) {
                button.setBackground(new Color(255, 200, 200)); // Light red
            } else if (funktion.equals("←")) {
                button.setBackground(new Color(255, 240, 200)); // Light yellow
            } else {
                button.setBackground(new Color(200, 230, 255)); // Light blue for parentheses
            }

            button.addActionListener(inputHandler);
            funktionsPanel.add(button);
        }

        // Scientific functions
        String[] wissenschaftlich = { "x²", "x³", "x^y", "√x", "³√x", "y√x" };
        for (String funktion : wissenschaftlich) {
            JButton button = new JButton(funktion);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.setBackground(new Color(230, 230, 250)); // Light lilac
            adjustButtonSize(button);
            button.addActionListener(inputHandler);
            wissenschaftlichPanel.add(button);
        }

        // Assemble the button layout
        JPanel allButtonsPanel = new JPanel(new BorderLayout(5, 5));
        allButtonsPanel.add(funktionsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(ziffernPanel, BorderLayout.CENTER);
        centerPanel.add(operatorenPanel, BorderLayout.EAST);

        // Scientific panel with GridLayout
        JPanel wissenschaftlichesGridPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        wissenschaftlichesGridPanel.add(wissenschaftlichPanel);

        allButtonsPanel.add(centerPanel, BorderLayout.CENTER);
        allButtonsPanel.add(wissenschaftlichesGridPanel, BorderLayout.SOUTH);

        // Combine UI components
        buttonPanel.add(allButtonsPanel, BorderLayout.CENTER);
        buttonPanel.add(controlButtonPanel, BorderLayout.SOUTH);

        return buttonPanel;
    }

    /**
     * Helper method to dynamically adjust button size based on text
     */
    public void adjustButtonSize(JButton button) {
        // Calculate text size
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(button.getText());
        int textHeight = metrics.getHeight();

        // Set minimum button size based on text size plus buffer
        int width = Math.max(textWidth + TEXT_PADDING_HORIZONTAL, MINIMUM_BUTTON_WIDTH);
        int height = Math.max(textHeight + TEXT_PADDING_VERTICAL, MINIMUM_BUTTON_HEIGHT);

        button.setMinimumSize(new Dimension(width, height));
        button.setPreferredSize(new Dimension(width, height));

        // Small margins for better appearance
        button.setMargin(new Insets(4, 4, 4, 4));

        // Debug output for checking
        calculator.debug("Button '" + button.getText() + "' Größe angepasst: " + width + "x" + height +
                " (Text: " + textWidth + "x" + textHeight + ")");
    }
}
