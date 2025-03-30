/**
 * Interface für das Logging-System
 * Ermöglicht einheitliches Logging in allen Komponenten
 */
public interface Logger {
    /**
     * Protokolliert eine Debug-Nachricht
     * @param message Die Nachricht
     */
    void debug(String message);
    
    /**
     * Protokolliert eine Info-Nachricht
     * @param message Die Nachricht
     */
    void info(String message);
    
    /**
     * Protokolliert eine Warnung
     * @param message Die Nachricht
     */
    void warning(String message);
    
    /**
     * Protokolliert einen Fehler
     * @param message Die Nachricht
     */
    void error(String message);
    
    /**
     * Protokolliert einen Fehler mit Exception
     * @param message Die Nachricht
     * @param e Die Exception
     */
    void error(String message, Exception e);
}
