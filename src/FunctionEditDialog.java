import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

/**
 * Dialog zum Bearbeiten einer Funktion und ihrer Farbe
 */
public class FunctionEditDialog extends JDialog {
    private JTextField functionField;
    private JButton colorButton;
    private JCheckBox randomColorCheck;
    private JButton okButton;
    private JButton cancelButton;
    private Color selectedColor;
    private String functionText;
    private boolean useRandomColor = false;
    private boolean confirmed = false;

    /**
     * Erstellt einen neuen Dialog zum Bearbeiten einer Funktion
     * 
     * @param parent       Das Elternfenster
     * @param title        Der Titel des Dialogs
     * @param function     Die zu bearbeitende Funktion
     * @param initialColor Die anfängliche Farbe
     */
    public FunctionEditDialog(Frame parent, String title, String function, Color initialColor) {
        super(parent, title, true); // Modal-Dialog

        this.functionText = function;
        this.selectedColor = initialColor;

        initComponents();
        layoutComponents();

        // Fenstergröße und Position
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Initialisiert die Dialog-Komponenten
     */
    private void initComponents() {
        // Funktionsfeld
        functionField = new JTextField(functionText, 25);
        functionField.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Checkbox für Zufallsfarbe
        randomColorCheck = new JCheckBox("Zufällige Farbe verwenden");
        randomColorCheck.addActionListener(e -> {
            useRandomColor = randomColorCheck.isSelected();
            // Wenn zufällige Farbe aktiviert wird, deaktiviere Farbauswahl
            colorButton.setEnabled(!useRandomColor);
            if (useRandomColor) {
                // Neue Zufallsfarbe generieren und als Vorschau anzeigen
                selectedColor = ColorChooser.generateRandomColor();
                updateColorButton();
            }
        });

        // Farbauswahl-Button mit Farbvorschau
        colorButton = new JButton("Farbe wählen");
        updateColorButton(); // Initiale Farbe setzen
        colorButton.addActionListener(e -> chooseColor());

        // OK-Button
        okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            // Nutzereingaben übernehmen
            functionText = functionField.getText().trim();
            confirmed = true;
            dispose();
        });

        // Abbrechen-Button
        cancelButton = new JButton("Abbrechen");
        cancelButton.addActionListener(e -> dispose());

        // ESC-Taste zum Schließen
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // ENTER-Taste zum Bestätigen
        getRootPane().registerKeyboardAction(
                e -> okButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Standard-Button festlegen
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Aktualisiert das Aussehen des Farbauswahl-Buttons
     */
    private void updateColorButton() {
        colorButton.setBackground(selectedColor);
        colorButton.setForeground(getContrastColor(selectedColor));
    }

    /**
     * Organisiert das Layout der Komponenten
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Oberer Bereich: Funktionseingabe
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.add(new JLabel("f(x) = "), BorderLayout.WEST);
        functionPanel.add(functionField, BorderLayout.CENTER);

        // Mittlerer Bereich: Farbauswahl
        JPanel colorPanel = new JPanel(new BorderLayout(5, 5));
        colorPanel.add(colorButton, BorderLayout.WEST);

        // Panel für Zufallsfarboption
        JPanel randomPanel = new JPanel(new BorderLayout());
        randomPanel.add(randomColorCheck, BorderLayout.WEST);

        // Farbvorschau-Panel
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Vorschau"));
        JPanel colorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();

                // Farbfläche zeichnen
                g.setColor(selectedColor);
                g.fillRect(0, 0, w, h);

                // Rahmen zeichnen
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, w - 1, h - 1);
            }
        };
        colorPreview.setPreferredSize(new Dimension(50, 30));
        previewPanel.add(colorPreview, BorderLayout.CENTER);

        // Farb-Panel mit Vorschau und Button
        JPanel fullColorPanel = new JPanel(new BorderLayout(10, 10));
        fullColorPanel.add(colorButton, BorderLayout.WEST);
        fullColorPanel.add(previewPanel, BorderLayout.EAST);
        fullColorPanel.add(randomPanel, BorderLayout.SOUTH);

        // Unterer Bereich: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Alles zusammenfügen
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(functionPanel, BorderLayout.NORTH);
        centerPanel.add(fullColorPanel, BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    /**
     * Öffnet den Farbauswahldialog
     */
    private void chooseColor() {
        Color newColor = ColorChooser.showColorChooser(this, "Funktionsfarbe wählen", selectedColor);
        if (newColor != null) {
            selectedColor = newColor;
            updateColorButton();
            repaint(); // Neuzeichnen für Farbvorschau
        }
    }

    /**
     * Berechnet eine Kontrastfarbe für die Textanzeige
     */
    private Color getContrastColor(Color color) {
        // Berechne Helligkeit (0.0 - 1.0)
        double brightness = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;

        // Heller Hintergrund -> dunkler Text, dunkler Hintergrund -> heller Text
        return brightness > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Gibt zurück, ob der Dialog bestätigt wurde
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Gibt die gewählte Funktion zurück
     */
    public String getFunction() {
        return functionText;
    }

    /**
     * Gibt die gewählte Farbe zurück
     */
    public Color getColor() {
        return selectedColor;
    }

    /**
     * Gibt zurück, ob eine zufällige Farbe verwendet werden soll
     */
    public boolean isUsingRandomColor() {
        return useRandomColor;
    }

    /**
     * Statische Hilfsmethode zum Anzeigen des Dialogs
     */
    public static FunctionEditResult showDialog(Frame parent, String function, Color initialColor) {
        FunctionEditDialog dialog = new FunctionEditDialog(
                parent,
                "Funktion bearbeiten",
                function,
                initialColor);
        dialog.setVisible(true);

        // Ergebnis zurückgeben
        if (dialog.isConfirmed()) {
            return new FunctionEditResult(
                    dialog.getFunction(),
                    dialog.getColor(),
                    dialog.isUsingRandomColor());
        } else {
            return null; // Bearbeitung abgebrochen
        }
    }

    /**
     * Klasse zum Speichern des Bearbeitungsergebnisses
     */
    public static class FunctionEditResult {
        private final String function;
        private final Color color;
        private final boolean useRandomColor;

        public FunctionEditResult(String function, Color color, boolean useRandomColor) {
            this.function = function;
            this.color = color;
            this.useRandomColor = useRandomColor;
        }

        public String getFunction() {
            return function;
        }

        public Color getColor() {
            return color;
        }

        public boolean isUsingRandomColor() {
            return useRandomColor;
        }
    }
}