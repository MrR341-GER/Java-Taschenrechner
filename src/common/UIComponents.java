
package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Klasse für einheitliche UI-Komponenten
 * Stellt Factory-Methoden für häufig verwendete Komponenten bereit
 */
public class UIComponents {
    // Konstantendefinitionen für gemeinsame Designelemente
    private static final int TEXT_PADDING_HORIZONTAL = 16;
    private static final int TEXT_PADDING_VERTICAL = 10;
    private static final int MINIMUM_BUTTON_WIDTH = 40;
    private static final int MINIMUM_BUTTON_HEIGHT = 35;
    private static final Font DEFAULT_BUTTON_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Font DEFAULT_LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font MONOSPACE_FONT = new Font("Monospaced", Font.PLAIN, 14);

    /**
     * Erstellt einen Button mit optimierter Größe
     * 
     * @param label    Text des Buttons
     * @param listener ActionListener für den Button
     * @param color    Hintergrundfarbe (kann null sein)
     * @return Der erstellte Button
     */
    public static JButton createButton(String label, ActionListener listener, Color color) {
        JButton button = new JButton(label);
        button.setFont(DEFAULT_BUTTON_FONT);

        if (color != null) {
            button.setBackground(color);
        }

        // Optimale Größe basierend auf Text
        adjustButtonSize(button);

        if (listener != null) {
            button.addActionListener(listener);
        }

        return button;
    }

    /**
     * Erstellt einen Button mit Standard-Hintergrundfarbe
     */
    public static JButton createButton(String label, ActionListener listener) {
        return createButton(label, listener, null);
    }

    /**
     * Erstellt einen benannten Button für mathematische Funktionen
     */
    public static JButton createFunctionButton(String label, ActionListener listener) {
        return createButton(label, listener, new Color(230, 230, 250)); // Helles Lila
    }

    /**
     * Erstellt einen benannten Button für Operationen
     */
    public static JButton createOperationButton(String label, ActionListener listener) {
        return createButton(label, listener, new Color(230, 230, 250)); // Helles Lila
    }

    /**
     * Erstellt einen benannten Button für Ziffern
     */
    public static JButton createDigitButton(String label, ActionListener listener) {
        return createButton(label, listener, Color.WHITE);
    }

    /**
     * Erstellt einen benannten Button für Lösch-Operationen
     */
    public static JButton createClearButton(String label, ActionListener listener) {
        return createButton(label, listener, new Color(255, 200, 200)); // Helles Rot
    }

    /**
     * Passt die Größe eines Buttons basierend auf seinem Text an
     */
    public static void adjustButtonSize(JButton button) {
        // Textgröße berechnen
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(button.getText());
        int textHeight = metrics.getHeight();

        // Mindestgröße basierend auf Textgröße plus Puffer
        int width = Math.max(textWidth + TEXT_PADDING_HORIZONTAL, MINIMUM_BUTTON_WIDTH);
        int height = Math.max(textHeight + TEXT_PADDING_VERTICAL, MINIMUM_BUTTON_HEIGHT);

        button.setMinimumSize(new Dimension(width, height));
        button.setPreferredSize(new Dimension(width, height));

        // Kleine Abstände für besseres Aussehen
        button.setMargin(new Insets(4, 4, 4, 4));
    }

    /**
     * Erstellt ein Panel mit Titel-Umrandung
     */
    public static JPanel createTitledPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Erstellt ein Eingabefeld mit Beschriftung
     */
    public static JPanel createLabeledTextField(String label, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Erstellt ein Textfeld mit Monospace-Schrift
     */
    public static JTextField createMonospaceTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(MONOSPACE_FONT);
        return textField;
    }

    /**
     * Erstellt ein Textfeld für numerische Eingaben
     */
    public static JTextField createNumericTextField(int columns) {
        JTextField textField = createMonospaceTextField(columns);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        return textField;
    }

    /**
     * Erstellt ein mehrzeiliges Textfeld mit Scrollbalken
     */
    public static JScrollPane createScrollableTextArea(JTextArea textArea) {
        textArea.setFont(MONOSPACE_FONT);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    /**
     * Erstellt ein Panel mit Abstand an allen Seiten
     */
    public static JPanel createPaddedPanel(JComponent content, int padding) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Erstellt ein Panel mit Standard-Abstand (10px)
     */
    public static JPanel createPaddedPanel(JComponent content) {
        return createPaddedPanel(content, 10);
    }

    /**
     * Erstellt ein Button-Panel mit Buttons in einer Reihe
     */
    public static JPanel createButtonPanel(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    /**
     * Erstellt ein Panel mit Grid-Layout und gleich großen Zellen
     */
    public static JPanel createGridPanel(int rows, int cols, int hgap, int vgap) {
        return new JPanel(new GridLayout(rows, cols, hgap, vgap));
    }
}
