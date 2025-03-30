
package util.debug;

import javax.swing.JTextArea;

/**
 * Implementierung des Logger-Interfaces für den Taschenrechner
 * Verwendet JTextArea für die GUI-Ausgabe und System.out für Konsolenausgaben
 */
public class CalculatorLogger implements Logger {
    private JTextArea debugTextArea;
    private boolean consoleOutput;

    /**
     * Erstellt einen neuen Logger
     * 
     * @param debugTextArea Das Textfeld für Debug-Ausgaben (kann null sein)
     * @param consoleOutput Ob Ausgaben auch auf der Konsole erfolgen sollen
     */
    public CalculatorLogger(JTextArea debugTextArea, boolean consoleOutput) {
        this.debugTextArea = debugTextArea;
        this.consoleOutput = consoleOutput;
    }

    /**
     * Erstellt einen neuen Logger mit Konsolenausgabe
     * 
     * @param debugTextArea Das Textfeld für Debug-Ausgaben (kann null sein)
     */
    public CalculatorLogger(JTextArea debugTextArea) {
        this(debugTextArea, true);
    }

    @Override
    public void debug(String message) {
        log("[DEBUG] " + message);
    }

    @Override
    public void info(String message) {
        log("[INFO] " + message);
    }

    @Override
    public void warning(String message) {
        log("[WARNUNG] " + message);
    }

    @Override
    public void error(String message) {
        log("[FEHLER] " + message);
    }

    @Override
    public void error(String message, Exception e) {
        log("[FEHLER] " + message);
        log("  Exception: " + e.getClass().getName() + ": " + e.getMessage());

        // Stacktrace (begrenzt)
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; i < Math.min(3, stack.length); i++) {
            log("    at " + stack[i].toString());
        }
    }

    /**
     * Interne Methode zum Protokollieren einer Nachricht
     */
    private void log(String message) {
        if (consoleOutput) {
            System.out.println(message);
        }

        if (debugTextArea != null) {
            debugTextArea.append(message + "\n");
            // Zum Ende scrollen
            debugTextArea.setCaretPosition(debugTextArea.getDocument().getLength());
        }
    }

    /**
     * Setzt das TextArea für Debug-Ausgaben
     */
    public void setDebugTextArea(JTextArea debugTextArea) {
        this.debugTextArea = debugTextArea;
    }

    /**
     * Aktiviert oder deaktiviert die Konsolenausgabe
     */
    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
    }
}
