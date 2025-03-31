package core;

import javax.swing.*;

import util.debug.DebugManager;
import util.history.HistoryManager;

import java.awt.*;
import java.awt.event.*;

/**
 * Haupt-Taschenrechner-Klasse mit UI-Komponenten
 */
public class Taschenrechner extends JFrame {
    // UI-Komponenten
    private JTextField displayField;

    // Zentrale Manager
    private HistoryManager historyManager;
    private DebugManager debugManager;
    private CalculationEngine calculationEngine;
    private TaschenrechnerKeypad keypad;
    private InputHandler inputHandler;

    // Status-Flag
    private boolean neueZahlBegonnen = true;

    public Taschenrechner() {
        // Fenstertitel und Grundeinstellungen
        super("Taschenrechner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(400, 300));

        // Manager initialisieren
        debugManager = new DebugManager(this);
        historyManager = new HistoryManager(this);
        calculationEngine = new CalculationEngine(this);
        inputHandler = new InputHandler(this, calculationEngine);
        keypad = new TaschenrechnerKeypad(this, inputHandler);

        // Eingabefeld erstellen
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

        // Layout zusammenstellen
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(displayField, BorderLayout.NORTH);
        mainPanel.add(keypad.createKeypadPanel(), BorderLayout.CENTER);

        // Hauptlayout
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        debug("Taschenrechner gestartet");
    }

    /**
     * Wechselt die Sichtbarkeit des Verlaufspanels
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
     * Wechselt die Sichtbarkeit des Debug-Panels
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
     * Fügt eine Debug-Nachricht hinzu
     */
    public void debug(String message) {
        debugManager.debug(message);
    }

    /**
     * Fügt eine Berechnung zum Verlauf hinzu
     */
    public void addToHistory(String calculation, String result) {
        historyManager.addToHistory(calculation, result);
    }

    /**
     * Gibt den Anzeigetext zurück
     */
    public String getDisplayText() {
        return displayField.getText();
    }

    /**
     * Setzt den Anzeigetext
     */
    public void setDisplayText(String text) {
        displayField.setText(text);
    }

    /**
     * Überprüft, ob eine neue Zahl begonnen wurde
     */
    public boolean isNeueZahlBegonnen() {
        return neueZahlBegonnen;
    }

    /**
     * Setzt das Flag, dass eine neue Zahl begonnen wurde
     */
    public void setNeueZahlBegonnen(boolean neueZahlBegonnen) {
        this.neueZahlBegonnen = neueZahlBegonnen;
    }

    /**
     * Hauptmethode
     */
    public static void main(String[] args) {
        // Starte die GUI im Event-Dispatch-Thread
        SwingUtilities.invokeLater(() -> {
            Taschenrechner taschenrechner = new Taschenrechner();
            taschenrechner.setVisible(true);
        });
    }
}
