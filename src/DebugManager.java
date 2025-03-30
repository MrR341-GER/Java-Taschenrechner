import javax.swing.*;
import java.awt.*;

/**
 * Verwaltet die Debug-Funktionalität
 * Implementiert das Logger-Interface für einheitliche Protokollierung
 */
public class DebugManager implements Logger {
    private JTextArea debugTextArea;
    private JDialog debugDialog;
    private final Taschenrechner calculator;

    /**
     * Erstellt einen neuen DebugManager
     * 
     * @param calculator Der zugehörige Taschenrechner
     */
    public DebugManager(Taschenrechner calculator) {
        this.calculator = calculator;
        initializeDebugPanel();
    }

    /**
     * Initialisiert das Debug-Panel
     */
    private void initializeDebugPanel() {
        // Create debug text area
        debugTextArea = new JTextArea(10, 40);
        debugTextArea.setEditable(false);
        debugTextArea.setFont(CalculatorConstants.MONOSPACE_FONT);

        // Create dialog for debug information
        createDebugDialog();
    }

    /**
     * Erstellt den Dialog für die Anzeige von Debug-Informationen
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
     * Protokolliert eine Debug-Nachricht
     */
    @Override
    public void debug(String message) {
        log("[DEBUG] " + message);
    }

    /**
     * Protokolliert eine Info-Nachricht
     */
    @Override
    public void info(String message) {
        log("[INFO] " + message);
    }

    /**
     * Protokolliert eine Warnung
     */
    @Override
    public void warning(String message) {
        log("[WARNUNG] " + message);
    }

    /**
     * Protokolliert einen Fehler
     */
    @Override
    public void error(String message) {
        log("[FEHLER] " + message);
    }

    /**
     * Protokolliert einen Fehler mit Exception
     */
    @Override
    public void error(String message, Exception e) {
        log("[FEHLER] " + message);
        if (e != null) {
            log("  Exception: " + e.getClass().getName() + ": " + e.getMessage());
            
            // Stacktrace (begrenzt)
            StackTraceElement[] stack = e.getStackTrace();
            for (int i = 0; i < Math.min(3, stack.length); i++) {
                log("    at " + stack[i].toString());
            }
        }
    }

    /**
     * Interne Methode zum Protokollieren einer Nachricht
     */
    private void log(String message) {
        System.out.println(message);
        debugTextArea.append(message + "\n");
        // Zum Ende scrollen
        debugTextArea.setCaretPosition(debugTextArea.getDocument().getLength());
    }

    /**
     * Zeigt den Debug-Dialog an
     */
    public void showDebugDialog() {
        if (debugDialog != null) {
            debugDialog.setVisible(true);
        }
    }

    /**
     * Verbirgt den Debug-Dialog
     */
    public void hideDebugDialog() {
        if (debugDialog != null) {
            debugDialog.setVisible(false);
        }
    }

    /**
     * Gibt zurück, ob der Debug-Dialog sichtbar ist
     */
    public boolean isVisible() {
        return debugDialog != null && debugDialog.isVisible();
    }

    /**
     * Aktualisiert die Dialog-Position relativ zum Elternfenster
     */
    public void updateDialogPosition() {
        if (debugDialog != null && calculator != null) {
            debugDialog.setLocationRelativeTo(calculator);
        }
    }

    /**
     * Gibt das Debug-TextArea zurück
     */
    public JTextArea getDebugTextArea() {
        return debugTextArea;
    }
}
