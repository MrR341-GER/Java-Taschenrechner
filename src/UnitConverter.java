import java.util.HashMap;
import java.util.Map;

/**
 * Klasse zur Umrechnung verschiedener Einheiten
 */
public class UnitConverter {

    // Kategorien von Einheiten
    public enum UnitCategory {
        LENGTH, AREA, VOLUME, MASS, TEMPERATURE, TIME, SPEED, ANGLE, DATA, ENERGY, POWER, PRESSURE, FORCE
    }

    // Karten zur Umrechnung in SI-Basiseinheiten
    private static final Map<String, Double> lengthFactors = new HashMap<>();
    private static final Map<String, Double> areaFactors = new HashMap<>();
    private static final Map<String, Double> volumeFactors = new HashMap<>();
    private static final Map<String, Double> massFactors = new HashMap<>();
    private static final Map<String, Double> timeFactors = new HashMap<>();
    private static final Map<String, Double> speedFactors = new HashMap<>();
    private static final Map<String, Double> angleFactors = new HashMap<>();
    private static final Map<String, Double> dataFactors = new HashMap<>();
    private static final Map<String, Double> energyFactors = new HashMap<>();
    private static final Map<String, Double> powerFactors = new HashMap<>();
    private static final Map<String, Double> pressureFactors = new HashMap<>();
    private static final Map<String, Double> forceFactors = new HashMap<>();

    // Statischer Initialisierer für Umrechnungsfaktoren
    static {
        // Längenfaktoren (Meter ist Basis)
        lengthFactors.put("m", 1.0);         // Meter
        lengthFactors.put("km", 1000.0);     // Kilometer
        lengthFactors.put("cm", 0.01);       // Zentimeter
        lengthFactors.put("mm", 0.001);      // Millimeter
        lengthFactors.put("μm", 1e-6);       // Mikrometer
        lengthFactors.put("nm", 1e-9);       // Nanometer
        lengthFactors.put("in", 0.0254);     // Zoll (Inch)
        lengthFactors.put("ft", 0.3048);     // Fuß (Feet)
        lengthFactors.put("yd", 0.9144);     // Yard
        lengthFactors.put("mi", 1609.344);   // Meile
        lengthFactors.put("nmi", 1852.0);    // Seemeile

        // Flächenfaktoren (Quadratmeter ist Basis)
        areaFactors.put("m²", 1.0);          // Quadratmeter
        areaFactors.put("km²", 1e6);         // Quadratkilometer
        areaFactors.put("cm²", 1e-4);        // Quadratzentimeter
        areaFactors.put("mm²", 1e-6);        // Quadratmillimeter
        areaFactors.put("ha", 10000.0);      // Hektar
        areaFactors.put("in²", 0.00064516);  // Quadratzoll
        areaFactors.put("ft²", 0.09290304);  // Quadratfuß
        areaFactors.put("yd²", 0.83612736);  // Quadratyard
        areaFactors.put("ac", 4046.8564224); // Acre
        areaFactors.put("mi²", 2589988.110336); // Quadratmeile

        // Volumenfaktoren (Kubikmeter ist Basis)
        volumeFactors.put("m³", 1.0);        // Kubikmeter
        volumeFactors.put("km³", 1e9);       // Kubikkilometer
        volumeFactors.put("cm³", 1e-6);      // Kubikzentimeter
        volumeFactors.put("mm³", 1e-9);      // Kubikmillimeter
        volumeFactors.put("l", 0.001);       // Liter
        volumeFactors.put("ml", 1e-6);       // Milliliter
        volumeFactors.put("in³", 1.6387064e-5); // Kubikzoll
        volumeFactors.put("ft³", 0.028316846592); // Kubikfuß
        volumeFactors.put("gal", 0.00378541); // US-Gallone
        volumeFactors.put("qt", 0.000946353); // US-Quart
        volumeFactors.put("pt", 0.000473176); // US-Pint
        volumeFactors.put("oz", 2.95735e-5);  // US-Flüssigunze

        // Massenfaktoren (Kilogramm ist Basis)
        massFactors.put("kg", 1.0);           // Kilogramm
        massFactors.put("g", 0.001);          // Gramm
        massFactors.put("mg", 1e-6);          // Milligramm
        massFactors.put("μg", 1e-9);          // Mikrogramm
        massFactors.put("t", 1000.0);         // Tonne
        massFactors.put("oz", 0.0283495);     // Unze (Avoirdupois)
        massFactors.put("lb", 0.45359237);    // Pfund (Pound)
        massFactors.put("st", 6.35029);       // Stone
        massFactors.put("ton", 907.18474);    // US-Tonne (Short ton)
        massFactors.put("tonUK", 1016.0469088); // UK-Tonne (Long ton)

        // Zeitfaktoren (Sekunde ist Basis)
        timeFactors.put("s", 1.0);            // Sekunde
        timeFactors.put("ms", 0.001);         // Millisekunde
        timeFactors.put("μs", 1e-6);          // Mikrosekunde
        timeFactors.put("ns", 1e-9);          // Nanosekunde
        timeFactors.put("min", 60.0);         // Minute
        timeFactors.put("h", 3600.0);         // Stunde
        timeFactors.put("d", 86400.0);        // Tag
        timeFactors.put("wk", 604800.0);      // Woche
        timeFactors.put("mo", 2592000.0);     // Monat (30 Tage)
        timeFactors.put("yr", 31536000.0);    // Jahr (365 Tage)

        // Geschwindigkeitsfaktoren (m/s ist Basis)
        speedFactors.put("m/s", 1.0);         // Meter pro Sekunde
        speedFactors.put("km/h", 0.277778);   // Kilometer pro Stunde
        speedFactors.put("mph", 0.44704);     // Meilen pro Stunde
        speedFactors.put("ft/s", 0.3048);     // Fuß pro Sekunde
        speedFactors.put("kn", 0.514444);     // Knoten

        // Winkelfaktoren (Radiant ist Basis)
        angleFactors.put("rad", 1.0);         // Radiant
        angleFactors.put("deg", Math.PI/180); // Grad
        angleFactors.put("grad", Math.PI/200); // Gradienten
        angleFactors.put("arcmin", Math.PI/10800); // Bogenminuten
        angleFactors.put("arcsec", Math.PI/648000); // Bogensekunden
        angleFactors.put("rev", 2*Math.PI);   // Umdrehung

        // Datenfaktoren (Byte ist Basis)
        dataFactors.put("B", 1.0);            // Byte
        dataFactors.put("KB", 1024.0);        // Kilobyte
        dataFactors.put("MB", 1048576.0);     // Megabyte
        dataFactors.put("GB", 1073741824.0);  // Gigabyte
        dataFactors.put("TB", 1099511627776.0); // Terabyte
        dataFactors.put("bit", 0.125);        // Bit
        dataFactors.put("Kbit", 128.0);       // Kilobit
        dataFactors.put("Mbit", 131072.0);    // Megabit
        dataFactors.put("Gbit", 134217728.0); // Gigabit

        // Energiefaktoren (Joule ist Basis)
        energyFactors.put("J", 1.0);           // Joule
        energyFactors.put("kJ", 1000.0);       // Kilojoule
        energyFactors.put("cal", 4.184);       // Kalorie
        energyFactors.put("kcal", 4184.0);     // Kilokalorie
        energyFactors.put("Wh", 3600.0);       // Wattstunde
        energyFactors.put("kWh", 3600000.0);   // Kilowattstunde
        energyFactors.put("eV", 1.602176565e-19); // Elektronenvolt
        energyFactors.put("BTU", 1055.06);     // British Thermal Unit

        // Leistungsfaktoren (Watt ist Basis)
        powerFactors.put("W", 1.0);            // Watt
        powerFactors.put("kW", 1000.0);        // Kilowatt
        powerFactors.put("MW", 1000000.0);     // Megawatt
        powerFactors.put("hp", 745.7);         // Pferdestärke (PS)
        powerFactors.put("BTU/h", 0.2930711);  // BTU pro Stunde

        // Druckfaktoren (Pascal ist Basis)
        pressureFactors.put("Pa", 1.0);         // Pascal
        pressureFactors.put("kPa", 1000.0);     // Kilopascal
        pressureFactors.put("MPa", 1000000.0);  // Megapascal
        pressureFactors.put("bar", 100000.0);   // Bar
        pressureFactors.put("atm", 101325.0);   // Atmosphäre
        pressureFactors.put("mmHg", 133.322);   // Millimeter Quecksilbersäule
        pressureFactors.put("inHg", 3386.389);  // Zoll Quecksilbersäule
        pressureFactors.put("psi", 6894.757);   // Pound per Square Inch

        // Kraftfaktoren (Newton ist Basis)
        forceFactors.put("N", 1.0);            // Newton
        forceFactors.put("kN", 1000.0);        // Kilonewton
        forceFactors.put("kgf", 9.80665);      // Kilogramm-Kraft
        forceFactors.put("lbf", 4.44822);      // Pfund-Kraft
    }

    /**
     * Konvertiert einen Wert von einer Einheit in eine andere
     */
    public static double convert(double value, String fromUnit, String toUnit, UnitCategory category) {
        Map<String, Double> factors = getFactorMap(category);
        
        // Spezialfall: Temperatur
        if (category == UnitCategory.TEMPERATURE) {
            return convertTemperature(value, fromUnit, toUnit);
        }
        
        // Prüfe, ob die Einheiten existieren
        if (!factors.containsKey(fromUnit) || !factors.containsKey(toUnit)) {
            throw new IllegalArgumentException("Unbekannte Einheit: " + 
                                              (factors.containsKey(fromUnit) ? toUnit : fromUnit));
        }
        
        // Berechne den konvertierten Wert
        double fromFactor = factors.get(fromUnit);
        double toFactor = factors.get(toUnit);
        
        return value * fromFactor / toFactor;
    }
    
    /**
     * Konvertiert Temperaturwerte zwischen verschiedenen Einheiten
     */
    private static double convertTemperature(double value, String fromUnit, String toUnit) {
        // Zuerst in Kelvin umrechnen
        double kelvin;
        
        switch (fromUnit) {
            case "K":
                kelvin = value;
                break;
            case "°C":
                kelvin = value + 273.15;
                break;
            case "°F":
                kelvin = (value + 459.67) * 5/9;
                break;
            case "°R":
                kelvin = value * 5/9;
                break;
            default:
                throw new IllegalArgumentException("Unbekannte Temperatureinheit: " + fromUnit);
        }
        
        // Dann von Kelvin in die Zieleinheit umrechnen
        switch (toUnit) {
            case "K":
                return kelvin;
            case "°C":
                return kelvin - 273.15;
            case "°F":
                return kelvin * 9/5 - 459.67;
            case "°R":
                return kelvin * 9/5;
            default:
                throw new IllegalArgumentException("Unbekannte Temperatureinheit: " + toUnit);
        }
    }
    
    /**
     * Gibt die Umrechnungsfaktoren-Map für die angegebene Kategorie zurück
     */
    private static Map<String, Double> getFactorMap(UnitCategory category) {
        switch (category) {
            case LENGTH:
                return lengthFactors;
            case AREA:
                return areaFactors;
            case VOLUME:
                return volumeFactors;
            case MASS:
                return massFactors;
            case TIME:
                return timeFactors;
            case SPEED:
                return speedFactors;
            case ANGLE:
                return angleFactors;
            case DATA:
                return dataFactors;
            case ENERGY:
                return energyFactors;
            case POWER:
                return powerFactors;
            case PRESSURE:
                return pressureFactors;
            case FORCE:
                return forceFactors;
            case TEMPERATURE:
                // Temperatur wird speziell behandelt
                return new HashMap<>();
            default:
                throw new IllegalArgumentException("Unbekannte Einheitenkategorie: " + category);
        }
    }
    
    /**
     * Gibt die verfügbaren Einheiten für eine bestimmte Kategorie zurück
     */
    public static String[] getAvailableUnits(UnitCategory category) {
        Map<String, Double> factors = getFactorMap(category);
        
        // Spezialfall: Temperatur
        if (category == UnitCategory.TEMPERATURE) {
            return new String[]{"K", "°C", "°F", "°R"};
        }
        
        return factors.keySet().toArray(new String[0]);
    }
    
    /**
     * Gibt die verfügbaren Kategorien zurück
     */
    public static UnitCategory[] getAvailableCategories() {
        return UnitCategory.values();
    }
    
    /**
     * Gibt einen benutzerfreundlichen Namen für eine Einheit zurück
     */
    public static String getUnitName(String unitSymbol) {
        switch (unitSymbol) {
            // Länge
            case "m": return "Meter";
            case "km": return "Kilometer";
            case "cm": return "Zentimeter";
            case "mm": return "Millimeter";
            case "μm": return "Mikrometer";
            case "nm": return "Nanometer";
            case "in": return "Zoll (Inch)";
            case "ft": return "Fuß (Feet)";
            case "yd": return "Yard";
            case "mi": return "Meile";
            case "nmi": return "Seemeile";
            
            // Fläche
            case "m²": return "Quadratmeter";
            case "km²": return "Quadratkilometer";
            case "cm²": return "Quadratzentimeter";
            case "mm²": return "Quadratmillimeter";
            case "ha": return "Hektar";
            case "in²": return "Quadratzoll";
            case "ft²": return "Quadratfuß";
            case "yd²": return "Quadratyard";
            case "ac": return "Acre";
            case "mi²": return "Quadratmeile";
            
            // Volumen
            case "m³": return "Kubikmeter";
            case "km³": return "Kubikkilometer";
            case "cm³": return "Kubikzentimeter";
            case "mm³": return "Kubikmillimeter";
            case "l": return "Liter";
            case "ml": return "Milliliter";
            case "in³": return "Kubikzoll";
            case "ft³": return "Kubikfuß";
            case "gal": return "US-Gallone";
            case "qt": return "US-Quart";
            case "pt": return "US-Pint";
            case "oz": return "US-Flüssigunze";
            
            // Masse
            case "kg": return "Kilogramm";
            case "g": return "Gramm";
            case "mg": return "Milligramm";
            case "μg": return "Mikrogramm";
            case "t": return "Tonne";
            case "lb": return "Pfund";
            case "st": return "Stone";
            case "ton": return "US-Tonne";
            case "tonUK": return "UK-Tonne";
            
            // Temperatur
            case "K": return "Kelvin";
            case "°C": return "Grad Celsius";
            case "°F": return "Grad Fahrenheit";
            case "°R": return "Grad Rankine";
            
            // Zeit
            case "s": return "Sekunde";
            case "ms": return "Millisekunde";
            case "μs": return "Mikrosekunde";
            case "ns": return "Nanosekunde";
            case "min": return "Minute";
            case "h": return "Stunde";
            case "d": return "Tag";
            case "wk": return "Woche";
            case "mo": return "Monat";
            case "yr": return "Jahr";
            
            // Geschwindigkeit
            case "m/s": return "Meter pro Sekunde";
            case "km/h": return "Kilometer pro Stunde";
            case "mph": return "Meilen pro Stunde";
            case "ft/s": return "Fuß pro Sekunde";
            case "kn": return "Knoten";
            
            // Winkel
            case "rad": return "Radiant";
            case "deg": return "Grad";
            case "grad": return "Gradienten";
            case "arcmin": return "Bogenminute";
            case "arcsec": return "Bogensekunde";
            case "rev": return "Umdrehung";
            
            // Daten
            case "B": return "Byte";
            case "KB": return "Kilobyte";
            case "MB": return "Megabyte";
            case "GB": return "Gigabyte";
            case "TB": return "Terabyte";
            case "bit": return "Bit";
            case "Kbit": return "Kilobit";
            case "Mbit": return "Megabit";
            case "Gbit": return "Gigabit";
            
            // Energie
            case "J": return "Joule";
            case "kJ": return "Kilojoule";
            case "cal": return "Kalorie";
            case "kcal": return "Kilokalorie";
            case "Wh": return "Wattstunde";
            case "kWh": return "Kilowattstunde";
            case "eV": return "Elektronenvolt";
            case "BTU": return "British Thermal Unit";
            
            // Leistung
            case "W": return "Watt";
            case "kW": return "Kilowatt";
            case "MW": return "Megawatt";
            case "hp": return "Pferdestärke";
            case "BTU/h": return "BTU pro Stunde";
            
            // Druck
            case "Pa": return "Pascal";
            case "kPa": return "Kilopascal";
            case "MPa": return "Megapascal";
            case "bar": return "Bar";
            case "atm": return "Atmosphäre";
            case "mmHg": return "Millimeter Quecksilbersäule";
            case "inHg": return "Zoll Quecksilbersäule";
            case "psi": return "Pound per Square Inch";
            
            // Kraft
            case "N": return "Newton";
            case "kN": return "Kilonewton";
            case "kgf": return "Kilogramm-Kraft";
            case "lbf": return "Pfund-Kraft";
            
            default: return unitSymbol;
        }
    }
    
    /**
     * Gibt einen benutzerfreundlichen Namen für eine Kategorie zurück
     */
    public static String getCategoryName(UnitCategory category) {
        switch (category) {
            case LENGTH: return "Länge";
            case AREA: return "Fläche";
            case VOLUME: return "Volumen";
            case MASS: return "Masse";
            case TEMPERATURE: return "Temperatur";
            case TIME: return "Zeit";
            case SPEED: return "Geschwindigkeit";
            case ANGLE: return "Winkel";
            case DATA: return "Daten";
            case ENERGY: return "Energie";
            case POWER: return "Leistung";
            case PRESSURE: return "Druck";
            case FORCE: return "Kraft";
            default: return category.toString();
        }
    }
}