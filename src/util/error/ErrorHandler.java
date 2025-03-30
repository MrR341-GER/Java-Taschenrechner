
package util.error;

import javax.swing.JOptionPane;

import util.debug.Logger;

import java.awt.Component;

/**
 * Zentrale Fehlerbehandlung für den Taschenrechner
 * Stellt einheitliche Fehler- und Warnmeldungen bereit
 */
public class ErrorHandler {
    private final Component parent;
    private final Logger logger;

    /**
     * Erstellt einen neuen ErrorHandler
     * 
     * @param parent Das Elternelement für Dialoge
     * @param logger Der Logger für Protokollierung
     */
    public ErrorHandler(Component parent, Logger logger) {
        this.parent = parent;
        this.logger = logger;
    }

    /**
     * Behandelt einen allgemeinen Fehler
     * 
     * @param userMessage Nachricht für den Benutzer
     * @param error       Der aufgetretene Fehler
     */
    public void handleError(String userMessage, Throwable error) {
        logger.error(userMessage, (error instanceof Exception) ? (Exception) error : null);

        JOptionPane.showMessageDialog(
                parent,
                userMessage + "\n" + error.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Behandelt eine Warnung
     * 
     * @param userMessage Nachricht für den Benutzer
     */
    public void handleWarning(String userMessage) {
        logger.warning(userMessage);

        JOptionPane.showMessageDialog(
                parent,
                userMessage,
                "Warnung",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Zeigt eine Informationsmeldung an
     * 
     * @param userMessage Nachricht für den Benutzer
     */
    public void showInfo(String userMessage) {
        logger.info(userMessage);

        JOptionPane.showMessageDialog(
                parent,
                userMessage,
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Fragt eine Bestätigung vom Benutzer ab
     * 
     * @param question Die Frage an den Benutzer
     * @param title    Der Titel des Dialogs
     * @return true wenn der Benutzer bestätigt, false sonst
     */
    public boolean confirmAction(String question, String title) {
        logger.debug("Bestätigung angefordert: " + question);

        int result = JOptionPane.showConfirmDialog(
                parent,
                question,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return result == JOptionPane.YES_OPTION;
    }
}
