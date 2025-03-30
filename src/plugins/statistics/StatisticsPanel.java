import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel für statistische Berechnungen
 */
public class StatisticsPanel extends JPanel {
    private final GrafischerTaschenrechner calculator;
    private final StatisticCalculator statisticCalculator;
    private final JTextField inputField;
    private final JTextArea resultArea;
    private final JTable dataTable;
    private final DefaultTableModel tableModel;
    private final DecimalFormat decimalFormat;
    
    // Debug-Referenz
    private DebugManager debugManager;

    public StatisticsPanel(GrafischerTaschenrechner calculator) {
        this.calculator = calculator;
        this.statisticCalculator = new StatisticCalculator();
        this.decimalFormat = new DecimalFormat("0.######");
        
        // Panel-Layout
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Komponenten erstellen
        inputField = new JTextField(20);
        inputField.addActionListener(e -> addValue());
        
        // Tabelle für die Daten
        String[] columnNames = {"Nr.", "Wert"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.setPreferredSize(new Dimension(200, 400));
        
        // Ergebnisbereich
        resultArea = new JTextArea(10, 30);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        
        // Button-Panel
        JPanel buttonPanel = createButtonPanel();
        
        // Eingabebereich
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel("Datenwert:"), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        JButton addButton = new JButton("Hinzufügen");
        addButton.addActionListener(e -> addValue());
        inputPanel.add(addButton, BorderLayout.EAST);
        
        // Daten-Panel (links)
        JPanel dataPanel = new JPanel(new BorderLayout(5, 5));
        dataPanel.setBorder(BorderFactory.createTitledBorder("Daten"));
        dataPanel.add(inputPanel, BorderLayout.NORTH);
        dataPanel.add(tableScrollPane, BorderLayout.CENTER);
        dataPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Ergebnis-Panel (rechts)
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Statistische Ergebnisse"));
        
        JPanel resultButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton calculateButton = new JButton("Berechnen");
        calculateButton.addActionListener(e -> calculateStatistics());
        
        resultButtonPanel.add(calculateButton);
        
        resultPanel.add(resultButtonPanel, BorderLayout.NORTH);
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);
        
        // Split-Pane für Daten- und Ergebnisbereich
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            dataPanel,
            resultPanel
        );
        splitPane.setResizeWeight(0.4); // 40% für Daten, 60% für Ergebnisse
        
        // Hauptpanel zusammensetzen
        add(splitPane, BorderLayout.CENTER);
        
        // Standardtext im Ergebnisbereich
        resultArea.setText("Fügen Sie Datenwerte hinzu und klicken Sie auf 'Berechnen'.\n\n" +
                           "Das Programm berechnet dann automatisch statistische\n" +
                           "Kennzahlen wie Mittelwert, Median, Standardabweichung usw.");
    }
    
    /**
     * Erstellt das Panel mit den Steuerungsbuttons
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        
        JButton deleteButton = new JButton("Ausgewählten Wert löschen");
        deleteButton.addActionListener(e -> deleteSelectedValue());
        
        JButton clearButton = new JButton("Alle Daten löschen");
        clearButton.addActionListener(e -> clearData());
        
        JButton importButton = new JButton("Daten importieren");
        importButton.addActionListener(e -> importData());
        
        JButton exportButton = new JButton("Daten exportieren");
        exportButton.addActionListener(e -> exportData());
        
        panel.add(deleteButton);
        panel.add(clearButton);
        panel.add(importButton);
        panel.add(exportButton);
        
        return panel;
    }
    
    /**
     * Fügt einen Wert zur Datenliste hinzu
     */
    private void addValue() {
        String input = inputField.getText().trim();
        if (!input.isEmpty()) {
            try {
                double value = Double.parseDouble(input);
                
                // Füge Wert zum StatisticCalculator hinzu
                statisticCalculator.addValue(value);
                
                // Aktualisiere die Tabelle
                int rowCount = tableModel.getRowCount();
                tableModel.addRow(new Object[]{rowCount + 1, decimalFormat.format(value)});
                
                // Leere das Eingabefeld
                inputField.setText("");
                
                debug("Wert hinzugefügt: " + value);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Bitte geben Sie eine gültige Zahl ein.",
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE);
                debug("Fehler bei der Werteingabe: " + e.getMessage());
            }
        }
    }
    
    /**
     * Löscht den ausgewählten Wert aus der Liste
     */
    private void deleteSelectedValue() {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Hole die aktuellen Daten
            List<Double> data = statisticCalculator.getData();
            
            // Lösche den Wert aus der Liste
            data.remove(selectedRow);
            
            // Setze die aktualisierten Daten
            statisticCalculator.clearData();
            for (Double value : data) {
                statisticCalculator.addValue(value);
            }
            
            // Aktualisiere die Tabelle
            tableModel.removeRow(selectedRow);
            
            // Aktualisiere die Zeilennummern
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(i + 1, i, 0);
            }
            
            debug("Wert an Position " + (selectedRow + 1) + " gelöscht");
        } else {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Wert zum Löschen aus.",
                "Hinweis",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Löscht alle Daten
     */
    public void clearData() {
        // Bestätigung anfordern
        int option = JOptionPane.showConfirmDialog(this,
            "Möchten Sie wirklich alle Daten löschen?",
            "Bestätigung",
            JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            // Daten im Calculator löschen
            statisticCalculator.clearData();
            
            // Tabelle leeren
            while (tableModel.getRowCount() > 0) {
                tableModel.removeRow(0);
            }
            
            // Ergebnisbereich leeren
            resultArea.setText("Fügen Sie Datenwerte hinzu und klicken Sie auf 'Berechnen'.\n\n" +
                               "Das Programm berechnet dann automatisch statistische\n" +
                               "Kennzahlen wie Mittelwert, Median, Standardabweichung usw.");
            
            debug("Alle Daten gelöscht");
        }
    }
    
    /**
     * Importiert Daten aus einer Datei oder Zwischenablage
     */
    private void importData() {
        // Erstelle ein mehrzeiliges Textfeld für die Dateneingabe
        JTextArea importArea = new JTextArea(10, 30);
        importArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(importArea);
        
        // Erklärender Text
        JLabel infoLabel = new JLabel(
            "<html>Geben Sie Zahlen ein (eine pro Zeile oder durch Kommas getrennt).<br>" +
            "Sie können auch Daten aus Excel oder anderen Programmen einfügen.</html>"
        );
        
        // Panel für das Dialogfenster
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Dialog anzeigen
        int result = JOptionPane.showConfirmDialog(
            this, panel, "Daten importieren", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        
        // Wenn OK gedrückt wurde
        if (result == JOptionPane.OK_OPTION) {
            String input = importArea.getText().trim();
            if (!input.isEmpty()) {
                // Bestehende Daten löschen oder behalten?
                int clearOption = JOptionPane.showConfirmDialog(
                    this,
                    "Möchten Sie die bestehenden Daten löschen bevor die neuen importiert werden?",
                    "Bestehende Daten",
                    JOptionPane.YES_NO_CANCEL_OPTION
                );
                
                if (clearOption == JOptionPane.CANCEL_OPTION) {
                    return; // Abbrechen
                }
                
                if (clearOption == JOptionPane.YES_OPTION) {
                    // Bestehende Daten löschen
                    statisticCalculator.clearData();
                    while (tableModel.getRowCount() > 0) {
                        tableModel.removeRow(0);
                    }
                }
                
                // Daten verarbeiten - nach Zeilen und Kommas aufteilen
                String[] lines = input.split("\\n");
                int importedCount = 0;
                
                for (String line : lines) {
                    // Zeile nach Kommas aufteilen
                    String[] values = line.split("[,;\\s]+");
                    
                    for (String value : values) {
                        value = value.trim();
                        if (!value.isEmpty()) {
                            try {
                                double dValue = Double.parseDouble(value);
                                
                                // Wert zum Calculator hinzufügen
                                statisticCalculator.addValue(dValue);
                                
                                // Tabelle aktualisieren
                                int rowCount = tableModel.getRowCount();
                                tableModel.addRow(new Object[]{rowCount + 1, decimalFormat.format(dValue)});
                                
                                importedCount++;
                            } catch (NumberFormatException e) {
                                // Ungültige Werte überspringen
                                debug("Ungültiger Wert beim Import übersprungen: " + value);
                            }
                        }
                    }
                }
                
                debug(importedCount + " Werte importiert");
                
                if (importedCount > 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        importedCount + " Werte wurden erfolgreich importiert.",
                        "Import abgeschlossen",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Es konnten keine gültigen Zahlenwerte gefunden werden.",
                        "Import fehlgeschlagen",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }
    }
    
    /**
     * Exportiert die Daten in die Zwischenablage
     */
    private void exportData() {
        if (statisticCalculator.count() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "Es sind keine Daten zum Exportieren vorhanden.",
                "Export",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        // Erstelle ein Textareafeld mit den Daten
        JTextArea exportArea = new JTextArea(15, 30);
        exportArea.setEditable(true);
        exportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Füge Daten ein
        List<Double> data = statisticCalculator.getData();
        StringBuilder sb = new StringBuilder();
        
        // Füge Werte hinzu
        for (Double value : data) {
            sb.append(decimalFormat.format(value)).append("\n");
        }
        
        // Füge Statistiken hinzu
        sb.append("\n--- Statistische Kennzahlen ---\n");
        sb.append("Anzahl: ").append(statisticCalculator.count()).append("\n");
        sb.append("Summe: ").append(decimalFormat.format(statisticCalculator.sum())).append("\n");
        sb.append("Mittelwert: ").append(decimalFormat.format(statisticCalculator.mean())).append("\n");
        sb.append("Median: ").append(decimalFormat.format(statisticCalculator.median())).append("\n");
        
        try {
            sb.append("Minimum: ").append(decimalFormat.format(statisticCalculator.min())).append("\n");
            sb.append("Maximum: ").append(decimalFormat.format(statisticCalculator.max())).append("\n");
            sb.append("Spannweite: ").append(decimalFormat.format(statisticCalculator.range())).append("\n");
        } catch (IllegalStateException e) {
            // Kann ignoriert werden, wenn keine Daten vorhanden sind
        }
        
        try {
            sb.append("Varianz: ").append(decimalFormat.format(statisticCalculator.variance())).append("\n");
            sb.append("Standardabweichung: ").append(decimalFormat.format(statisticCalculator.standardDeviation())).append("\n");
        } catch (IllegalStateException e) {
            // Kann ignoriert werden, wenn nicht genug Daten vorhanden sind
        }
        
        exportArea.setText(sb.toString());
        
        // Scrollbares Panel
        JScrollPane scrollPane = new JScrollPane(exportArea);
        
        // Panel für das Dialogfenster
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Die Daten können kopiert und z.B. in Excel eingefügt werden:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton copyButton = new JButton("In Zwischenablage kopieren");
        copyButton.addActionListener(e -> {
            exportArea.selectAll();
            exportArea.copy();
            JOptionPane.showMessageDialog(
                panel,
                "Daten wurden in die Zwischenablage kopiert.",
                "Kopieren",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        
        buttonPanel.add(copyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog anzeigen
        JOptionPane.showMessageDialog(
            this, panel, "Daten exportieren", 
            JOptionPane.PLAIN_MESSAGE
        );
        
        debug("Daten exportiert");
    }
    
    /**
     * Berechnet statistische Kennzahlen für die vorhandenen Daten
     */
    private void calculateStatistics() {
        int count = statisticCalculator.count();
        
        if (count == 0) {
            resultArea.setText("Keine Daten vorhanden.");
            return;
        }
        
        StringBuilder result = new StringBuilder();
        
        // Grundlegende Statistiken
        result.append("=== Grundlegende statistische Kennzahlen ===\n\n");
        result.append("Anzahl der Datenpunkte: ").append(count).append("\n");
        result.append("Summe: ").append(decimalFormat.format(statisticCalculator.sum())).append("\n");
        result.append("Mittelwert: ").append(decimalFormat.format(statisticCalculator.mean())).append("\n");
        result.append("Median: ").append(decimalFormat.format(statisticCalculator.median())).append("\n\n");
        
        // Minimum, Maximum, Spannweite
        try {
            result.append("Minimum: ").append(decimalFormat.format(statisticCalculator.min())).append("\n");
            result.append("Maximum: ").append(decimalFormat.format(statisticCalculator.max())).append("\n");
            result.append("Spannweite: ").append(decimalFormat.format(statisticCalculator.range())).append("\n\n");
        } catch (IllegalStateException e) {
            result.append("Fehler bei Berechnung von Min/Max: ").append(e.getMessage()).append("\n\n");
        }
        
        // Varianz und Standardabweichung
        if (count > 1) {
            try {
                result.append("=== Streuungsmaße ===\n\n");
                result.append("Stichprobenvarianz: ").append(decimalFormat.format(statisticCalculator.variance())).append("\n");
                result.append("Stichproben-Standardabweichung: ").append(decimalFormat.format(statisticCalculator.standardDeviation())).append("\n");
                result.append("Populationsvarianz: ").append(decimalFormat.format(statisticCalculator.populationVariance())).append("\n");
                result.append("Populations-Standardabweichung: ").append(decimalFormat.format(statisticCalculator.populationStandardDeviation())).append("\n\n");
            } catch (IllegalStateException e) {
                result.append("Fehler bei Berechnung der Varianz: ").append(e.getMessage()).append("\n\n");
            }
        } else {
            result.append("Varianz und Standardabweichung erfordern mindestens 2 Datenpunkte.\n\n");
        }
        
        // Alternative Mittelwerte
        try {
            result.append("=== Alternative Mittelwerte ===\n\n");
            result.append("Geometrisches Mittel: ").append(decimalFormat.format(statisticCalculator.geometricMean())).append("\n");
            result.append("Harmonisches Mittel: ").append(decimalFormat.format(statisticCalculator.harmonicMean())).append("\n\n");
        } catch (IllegalStateException e) {
            result.append("Fehler bei Berechnung alternativer Mittelwerte: ").append(e.getMessage()).append("\n\n");
        }
        
        // Quartilabstand (IQR)
        if (count >= 4) {
            try {
                result.append("=== Quartile ===\n\n");
                result.append("Interquartilsabstand (IQR): ").append(decimalFormat.format(statisticCalculator.interquartileRange())).append("\n");
            } catch (IllegalStateException e) {
                result.append("Fehler bei Berechnung des Quartilsabstands: ").append(e.getMessage()).append("\n");
            }
        } else {
            result.append("Quartilsberechnung erfordert mindestens 4 Datenpunkte.\n");
        }
        
        resultArea.setText(result.toString());
        debug("Statistische Kennzahlen berechnet");
    }
    
    /**
     * Setzt den DebugManager für Logging
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
    }
    
    /**
     * Schreibt Debug-Informationen in das Debug-Fenster
     */
    private void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[Statistik] " + message);
        } else {
            System.out.println("[Statistik] " + message);
        }
    }
    
    /**
     * Aktualisiert die Anzeige des Statistik-Panels
     */
    public void refresh() {
        // Kann verwendet werden, um das Panel bei Tab-Wechsel zu aktualisieren
    }
}