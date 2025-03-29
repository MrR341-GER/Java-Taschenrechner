import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel für die Umrechnung verschiedener Einheiten
 */
public class ConverterPanel extends JPanel {
    private final GrafischerTaschenrechner calculator;
    private final JTextField inputField;
    private final JTextField resultField;
    private final JComboBox<String> categoryComboBox;
    private final JComboBox<String> fromUnitComboBox;
    private final JComboBox<String> toUnitComboBox;
    private final DecimalFormat decimalFormat;
    
    // Debug-Referenz
    private DebugManager debugManager;
    
    // Kategorien und Einheiten
    private final Map<String, String[]> unitCategories = new HashMap<>();
    
    // Umrechnungsfaktoren
    private final Map<String, Map<String, Double>> conversionFactors = new HashMap<>();

    public ConverterPanel(GrafischerTaschenrechner calculator) {
        this.calculator = calculator;
        this.decimalFormat = new DecimalFormat("0.############");
        
        // Panel-Layout
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Einheiten-Kategorien initialisieren
        initializeUnitCategories();
        
        // Umrechnungsfaktoren initialisieren
        initializeConversionFactors();
        
        // Komponenten erstellen
        categoryComboBox = new JComboBox<>(unitCategories.keySet().toArray(new String[0]));
        categoryComboBox.addActionListener(e -> updateUnitCombos());
        
        fromUnitComboBox = new JComboBox<>();
        toUnitComboBox = new JComboBox<>();
        
        inputField = new JTextField(15);
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                convert();
            }
        });
        
        resultField = new JTextField(15);
        resultField.setEditable(false);
        resultField.setBackground(new Color(240, 240, 240));
        
        // Initialen Inhalt der Einheiten-Combos setzen
        updateUnitCombos();
        
        // UI zusammensetzen
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // Umrechnung direkt zu Beginn durchführen
        convert();
    }
    
    /**
     * Initialisiert die verfügbaren Einheiten-Kategorien
     */
    private void initializeUnitCategories() {
        // Längeneinheiten
        unitCategories.put("Länge", new String[] {
            "Kilometer (km)", "Meter (m)", "Dezimeter (dm)", "Zentimeter (cm)", "Millimeter (mm)", 
            "Meile (mi)", "Yard (yd)", "Fuß (ft)", "Zoll (in)", "Seemeile (nmi)"
        });
        
        // Gewichtseinheiten
        unitCategories.put("Gewicht", new String[] {
            "Tonne (t)", "Kilogramm (kg)", "Gramm (g)", "Milligramm (mg)",
            "Pfund (lb)", "Unze (oz)", "Stein (st)"
        });
        
        // Volumeneinheiten
        unitCategories.put("Volumen", new String[] {
            "Kubikmeter (m³)", "Liter (l)", "Milliliter (ml)",
            "Gallone (US) (gal)", "Quart (US) (qt)", "Pint (US) (pt)", "Flüssigunze (US) (fl oz)",
            "Gallone (UK) (gal)", "Quart (UK) (qt)", "Pint (UK) (pt)", "Flüssigunze (UK) (fl oz)"
        });
        
        // Flächeneinheiten
        unitCategories.put("Fläche", new String[] {
            "Quadratkilometer (km²)", "Hektar (ha)", "Ar (a)", "Quadratmeter (m²)", "Quadratzentimeter (cm²)",
            "Quadratmeile (mi²)", "Acre (ac)", "Quadratyard (yd²)", "Quadratfuß (ft²)", "Quadratzoll (in²)"
        });
        
        // Temperatureinheiten
        unitCategories.put("Temperatur", new String[] {
            "Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"
        });
        
        // Zeiteinheiten
        unitCategories.put("Zeit", new String[] {
            "Jahr (a)", "Monat (mon)", "Woche (w)", "Tag (d)", "Stunde (h)", "Minute (min)", "Sekunde (s)", "Millisekunde (ms)"
        });
        
        // Winkeleinheiten
        unitCategories.put("Winkel", new String[] {
            "Grad (°)", "Radiant (rad)", "Gradminute (')", "Gradsekunde (\")", "Gon", "Neugrad", "Vollwinkel"
        });
        
        // Dateneinheiten
        unitCategories.put("Daten", new String[] {
            "Byte (B)", "Kilobyte (KB)", "Megabyte (MB)", "Gigabyte (GB)", "Terabyte (TB)",
            "Kibibyte (KiB)", "Mebibyte (MiB)", "Gibibyte (GiB)", "Tebibyte (TiB)"
        });
        
        // Geschwindigkeitseinheiten
        unitCategories.put("Geschwindigkeit", new String[] {
            "Meter pro Sekunde (m/s)", "Kilometer pro Stunde (km/h)", "Meilen pro Stunde (mph)", "Knoten (kn)", "Fuß pro Sekunde (ft/s)"
        });
    }
    
    /**
     * Initialisiert die Umrechnungsfaktoren zwischen verschiedenen Einheiten
     */
    private void initializeConversionFactors() {
        // Längeneinheiten (Basis: Meter)
        Map<String, Double> lengthFactors = new HashMap<>();
        lengthFactors.put("Kilometer (km)", 1000.0);
        lengthFactors.put("Meter (m)", 1.0);
        lengthFactors.put("Dezimeter (dm)", 0.1);
        lengthFactors.put("Zentimeter (cm)", 0.01);
        lengthFactors.put("Millimeter (mm)", 0.001);
        lengthFactors.put("Meile (mi)", 1609.344);
        lengthFactors.put("Yard (yd)", 0.9144);
        lengthFactors.put("Fuß (ft)", 0.3048);
        lengthFactors.put("Zoll (in)", 0.0254);
        lengthFactors.put("Seemeile (nmi)", 1852.0);
        conversionFactors.put("Länge", lengthFactors);
        
        // Gewichtseinheiten (Basis: Kilogramm)
        Map<String, Double> weightFactors = new HashMap<>();
        weightFactors.put("Tonne (t)", 1000.0);
        weightFactors.put("Kilogramm (kg)", 1.0);
        weightFactors.put("Gramm (g)", 0.001);
        weightFactors.put("Milligramm (mg)", 0.000001);
        weightFactors.put("Pfund (lb)", 0.45359237);
        weightFactors.put("Unze (oz)", 0.028349523125);
        weightFactors.put("Stein (st)", 6.35029318);
        conversionFactors.put("Gewicht", weightFactors);
        
        // Volumeneinheiten (Basis: Liter)
        Map<String, Double> volumeFactors = new HashMap<>();
        volumeFactors.put("Kubikmeter (m³)", 1000.0);
        volumeFactors.put("Liter (l)", 1.0);
        volumeFactors.put("Milliliter (ml)", 0.001);
        volumeFactors.put("Gallone (US) (gal)", 3.78541178);
        volumeFactors.put("Quart (US) (qt)", 0.946352946);
        volumeFactors.put("Pint (US) (pt)", 0.473176473);
        volumeFactors.put("Flüssigunze (US) (fl oz)", 0.0295735296);
        volumeFactors.put("Gallone (UK) (gal)", 4.54609);
        volumeFactors.put("Quart (UK) (qt)", 1.1365225);
        volumeFactors.put("Pint (UK) (pt)", 0.56826125);
        volumeFactors.put("Flüssigunze (UK) (fl oz)", 0.0284130625);
        conversionFactors.put("Volumen", volumeFactors);
        
        // Flächeneinheiten (Basis: Quadratmeter)
        Map<String, Double> areaFactors = new HashMap<>();
        areaFactors.put("Quadratkilometer (km²)", 1000000.0);
        areaFactors.put("Hektar (ha)", 10000.0);
        areaFactors.put("Ar (a)", 100.0);
        areaFactors.put("Quadratmeter (m²)", 1.0);
        areaFactors.put("Quadratzentimeter (cm²)", 0.0001);
        areaFactors.put("Quadratmeile (mi²)", 2589988.110336);
        areaFactors.put("Acre (ac)", 4046.8564224);
        areaFactors.put("Quadratyard (yd²)", 0.83612736);
        areaFactors.put("Quadratfuß (ft²)", 0.09290304);
        areaFactors.put("Quadratzoll (in²)", 0.00064516);
        conversionFactors.put("Fläche", areaFactors);
        
        // Temperatureinheiten werden speziell behandelt (nicht linearer Zusammenhang)
        Map<String, Double> tempFactors = new HashMap<>();
        tempFactors.put("Celsius (°C)", 1.0);
        tempFactors.put("Fahrenheit (°F)", 1.0);
        tempFactors.put("Kelvin (K)", 1.0);
        conversionFactors.put("Temperatur", tempFactors);
        
        // Zeiteinheiten (Basis: Sekunde)
        Map<String, Double> timeFactors = new HashMap<>();
        timeFactors.put("Jahr (a)", 31536000.0); // 365 Tage
        timeFactors.put("Monat (mon)", 2592000.0); // 30 Tage
        timeFactors.put("Woche (w)", 604800.0);
        timeFactors.put("Tag (d)", 86400.0);
        timeFactors.put("Stunde (h)", 3600.0);
        timeFactors.put("Minute (min)", 60.0);
        timeFactors.put("Sekunde (s)", 1.0);
        timeFactors.put("Millisekunde (ms)", 0.001);
        conversionFactors.put("Zeit", timeFactors);
        
        // Winkeleinheiten (Basis: Grad)
        Map<String, Double> angleFactors = new HashMap<>();
        angleFactors.put("Grad (°)", 1.0);
        angleFactors.put("Radiant (rad)", 57.29577951308232); // 180/π
        angleFactors.put("Gradminute (')", 1.0/60.0);
        angleFactors.put("Gradsekunde (\")", 1.0/3600.0);
        angleFactors.put("Gon", 0.9);
        angleFactors.put("Neugrad", 0.9);
        angleFactors.put("Vollwinkel", 360.0);
        conversionFactors.put("Winkel", angleFactors);
        
        // Dateneinheiten (Basis: Byte)
        Map<String, Double> dataFactors = new HashMap<>();
        dataFactors.put("Byte (B)", 1.0);
        dataFactors.put("Kilobyte (KB)", 1000.0);
        dataFactors.put("Megabyte (MB)", 1000000.0);
        dataFactors.put("Gigabyte (GB)", 1000000000.0);
        dataFactors.put("Terabyte (TB)", 1000000000000.0);
        dataFactors.put("Kibibyte (KiB)", 1024.0);
        dataFactors.put("Mebibyte (MiB)", 1048576.0);
        dataFactors.put("Gibibyte (GiB)", 1073741824.0);
        dataFactors.put("Tebibyte (TiB)", 1099511627776.0);
        conversionFactors.put("Daten", dataFactors);
        
        // Geschwindigkeitseinheiten (Basis: Meter pro Sekunde)
        Map<String, Double> speedFactors = new HashMap<>();
        speedFactors.put("Meter pro Sekunde (m/s)", 1.0);
        speedFactors.put("Kilometer pro Stunde (km/h)", 0.277777778);
        speedFactors.put("Meilen pro Stunde (mph)", 0.44704);
        speedFactors.put("Knoten (kn)", 0.514444444);
        speedFactors.put("Fuß pro Sekunde (ft/s)", 0.3048);
        conversionFactors.put("Geschwindigkeit", speedFactors);
    }
    
    /**
     * Aktualisiert die Einheiten-Auswahlboxen basierend auf der gewählten Kategorie
     */
    private void updateUnitCombos() {
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        if (selectedCategory == null) return;
        
        // Einheiten für die gewählte Kategorie
        String[] units = unitCategories.get(selectedCategory);
        
        // Alte Auswahlen speichern
        String oldFromUnit = (String) fromUnitComboBox.getSelectedItem();
        String oldToUnit = (String) toUnitComboBox.getSelectedItem();
        
        // Listener temporär entfernen
        ActionListener[] fromListeners = fromUnitComboBox.getActionListeners();
        ActionListener[] toListeners = toUnitComboBox.getActionListeners();
        
        for (ActionListener listener : fromListeners) {
            fromUnitComboBox.removeActionListener(listener);
        }
        
        for (ActionListener listener : toListeners) {
            toUnitComboBox.removeActionListener(listener);
        }
        
        // Inhalte aktualisieren
        fromUnitComboBox.removeAllItems();
        toUnitComboBox.removeAllItems();
        
        for (String unit : units) {
            fromUnitComboBox.addItem(unit);
            toUnitComboBox.addItem(unit);
        }
        
        // Standard-Auswahl setzen oder alte Auswahl wiederherstellen
        if (oldFromUnit != null && fromUnitComboBox.getItemCount() > 0) {
            fromUnitComboBox.setSelectedItem(oldFromUnit);
        } else if (fromUnitComboBox.getItemCount() > 0) {
            fromUnitComboBox.setSelectedIndex(0);
        }
        
        if (oldToUnit != null && toUnitComboBox.getItemCount() > 0) {
            toUnitComboBox.setSelectedItem(oldToUnit);
        } else if (toUnitComboBox.getItemCount() > 1) {
            toUnitComboBox.setSelectedIndex(1);
        } else if (toUnitComboBox.getItemCount() > 0) {
            toUnitComboBox.setSelectedIndex(0);
        }
        
        // Listener wieder hinzufügen
        for (ActionListener listener : fromListeners) {
            fromUnitComboBox.addActionListener(listener);
        }
        
        for (ActionListener listener : toListeners) {
            toUnitComboBox.addActionListener(listener);
        }
        
        // Nachdem die Einheiten aktualisiert wurden, Umrechnung durchführen
        convert();
    }
    
    /**
     * Erstellt das Haupt-Panel mit den Eingabefeldern und Auswahllisten
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        // Oberes Panel mit Kategorie-Auswahl
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        topPanel.add(new JLabel("Kategorie:"), BorderLayout.WEST);
        topPanel.add(categoryComboBox, BorderLayout.CENTER);
        
        // Panel für Ein- und Ausgabe
        JPanel conversionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        
        // Von-Einheit
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        conversionPanel.add(new JLabel("Von:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        conversionPanel.add(fromUnitComboBox, c);
        
        // Input-Feld
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        conversionPanel.add(new JLabel("Wert:"), c);
        
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        conversionPanel.add(inputField, c);
        
        // Zu-Einheit
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        conversionPanel.add(new JLabel("Nach:"), c);
        
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1.0;
        conversionPanel.add(toUnitComboBox, c);
        
        // Ergebnis-Feld
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.weightx = 0.0;
        conversionPanel.add(new JLabel("Ergebnis:"), c);
        
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        conversionPanel.add(resultField, c);
        
        // Hauptpanel zusammensetzen
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(conversionPanel, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton swapButton = new JButton("Einheiten tauschen");
        swapButton.addActionListener(e -> swapUnits());
        
        JButton convertButton = new JButton("Umrechnen");
        convertButton.addActionListener(e -> convert());
        
        buttonPanel.add(swapButton);
        buttonPanel.add(convertButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // ActionListener für die Einheiten-Combos
        ActionListener unitChangeListener = e -> convert();
        fromUnitComboBox.addActionListener(unitChangeListener);
        toUnitComboBox.addActionListener(unitChangeListener);
        
        return panel;
    }
    
    /**
     * Tauscht die Von- und Zu-Einheiten
     */
    private void swapUnits() {
        Object fromUnit = fromUnitComboBox.getSelectedItem();
        Object toUnit = toUnitComboBox.getSelectedItem();
        
        fromUnitComboBox.setSelectedItem(toUnit);
        toUnitComboBox.setSelectedItem(fromUnit);
        
        // Nach dem Tausch das Ergebnis in das Eingabefeld übernehmen und neu umrechnen
        inputField.setText(resultField.getText());
        convert();
    }
    
    /**
     * Führt die Umrechnung durch
     */
    private void convert() {
        String inputText = inputField.getText().trim();
        if (inputText.isEmpty()) {
            resultField.setText("");
            return;
        }
        
        try {
            double inputValue = Double.parseDouble(inputText);
            String selectedCategory = (String) categoryComboBox.getSelectedItem();
            String fromUnit = (String) fromUnitComboBox.getSelectedItem();
            String toUnit = (String) toUnitComboBox.getSelectedItem();
            
            if (selectedCategory == null || fromUnit == null || toUnit == null) {
                return;
            }
            
            double result;
            
            // Spezialfall: Temperatur-Umrechnung
            if (selectedCategory.equals("Temperatur")) {
                result = convertTemperature(inputValue, fromUnit, toUnit);
            } else {
                // Normale lineare Umrechnung
                Map<String, Double> factors = conversionFactors.get(selectedCategory);
                
                if (factors != null) {
                    double fromFactor = factors.get(fromUnit);
                    double toFactor = factors.get(toUnit);
                    
                    // Umrechnung: Eingabe -> Basiseinheit -> Zieleinheit
                    result = inputValue * fromFactor / toFactor;
                } else {
                    resultField.setText("Fehler: Keine Faktoren für Kategorie");
                    return;
                }
            }
            
            // Ergebnis formatieren und anzeigen
            resultField.setText(formatResult(result));
            debug("Umrechnung: " + inputValue + " " + fromUnit + " = " + result + " " + toUnit);
            
        } catch (NumberFormatException e) {
            resultField.setText("Ungültige Eingabe");
            debug("Fehler bei Umrechnung: " + e.getMessage());
        } catch (Exception e) {
            resultField.setText("Fehler: " + e.getMessage());
            debug("Allgemeiner Fehler bei Umrechnung: " + e.getMessage());
        }
    }
    
    /**
     * Spezialisierte Methode für die Umrechnung von Temperaturen
     */
    private double convertTemperature(double value, String fromUnit, String toUnit) {
        // Umrechnung: Eingang -> Celsius -> Ausgang
        double celsius;
        
        // Umrechnung in Celsius
        if (fromUnit.equals("Celsius (°C)")) {
            celsius = value;
        } else if (fromUnit.equals("Fahrenheit (°F)")) {
            celsius = (value - 32) * 5 / 9;
        } else if (fromUnit.equals("Kelvin (K)")) {
            celsius = value - 273.15;
        } else {
            throw new IllegalArgumentException("Unbekannte Temperatureinheit: " + fromUnit);
        }
        
        // Umrechnung von Celsius in Zieleinheit
        if (toUnit.equals("Celsius (°C)")) {
            return celsius;
        } else if (toUnit.equals("Fahrenheit (°F)")) {
            return celsius * 9 / 5 + 32;
        } else if (toUnit.equals("Kelvin (K)")) {
            return celsius + 273.15;
        } else {
            throw new IllegalArgumentException("Unbekannte Temperatureinheit: " + toUnit);
        }
    }
    
    /**
     * Formatiert das Ergebnis (entfernt unnötige Nachkommastellen)
     */
    private String formatResult(double result) {
        // Für kleine oder große Zahlen wissenschaftliche Notation verwenden
        if (Math.abs(result) < 0.0001 || Math.abs(result) > 10000000) {
            return String.format("%e", result);
        }
        
        // Sonst schöne Dezimalzahlen
        return decimalFormat.format(result);
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
            debugManager.debug("[Umrechner] " + message);
        } else {
            System.out.println("[Umrechner] " + message);
        }
    }
    
    /**
     * Aktualisiert die Anzeige des Umrechner-Panels
     */
    public void refresh() {
        // Kann verwendet werden, um das Panel bei Tab-Wechsel zu aktualisieren
    }
    
    /**
     * Leert die Eingabe- und Ergebnisfelder
     */
    public void clear() {
        inputField.setText("");
        resultField.setText("");
    }
}