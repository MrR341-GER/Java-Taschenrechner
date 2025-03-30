import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Klasse zur Durchführung statistischer Berechnungen
 */
public class StatisticCalculator {
    private List<Double> data = new ArrayList<>();

    /**
     * Fügt einen Wert zur Datenreihe hinzu
     */
    public void addValue(double value) {
        data.add(value);
    }

    /**
     * Setzt die Datenreihe (ersetzt bisherige Werte)
     */
    public void setData(List<Double> newData) {
        this.data = new ArrayList<>(newData);
    }

    /**
     * Löscht alle Daten
     */
    public void clearData() {
        data.clear();
    }

    /**
     * Gibt die aktuelle Datenreihe zurück
     */
    public List<Double> getData() {
        return new ArrayList<>(data);
    }

    /**
     * Berechnet die Anzahl der Datenpunkte
     */
    public int count() {
        return data.size();
    }

    /**
     * Berechnet die Summe aller Werte
     */
    public double sum() {
        double sum = 0;
        for (Double value : data) {
            sum += value;
        }
        return sum;
    }

    /**
     * Berechnet den Durchschnitt (arithmetisches Mittel) der Werte
     */
    public double mean() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }
        return sum() / data.size();
    }

    /**
     * Berechnet den Median der Werte
     */
    public double median() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }

        List<Double> sortedData = new ArrayList<>(data);
        Collections.sort(sortedData);

        int size = sortedData.size();
        if (size % 2 == 0) {
            // Durchschnitt der beiden mittleren Werte
            return (sortedData.get(size / 2 - 1) + sortedData.get(size / 2)) / 2.0;
        } else {
            // Mittlerer Wert
            return sortedData.get(size / 2);
        }
    }

    /**
     * Berechnet das Minimum der Werte
     */
    public double min() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }
        return Collections.min(data);
    }

    /**
     * Berechnet das Maximum der Werte
     */
    public double max() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }
        return Collections.max(data);
    }

    /**
     * Berechnet die Spannweite (Range) der Werte
     */
    public double range() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }
        return max() - min();
    }

    /**
     * Berechnet die Varianz der Werte (Stichprobenvarianz)
     */
    public double variance() {
        if (data.size() <= 1) {
            throw new IllegalStateException("Mindestens zwei Datenpunkte erforderlich");
        }

        double mean = mean();
        double sum = 0;

        for (Double value : data) {
            double diff = value - mean;
            sum += diff * diff;
        }

        // Verwende n-1 für Stichprobenvarianz
        return sum / (data.size() - 1);
    }

    /**
     * Berechnet die Standardabweichung der Werte (Stichproben-Standardabweichung)
     */
    public double standardDeviation() {
        return Math.sqrt(variance());
    }

    /**
     * Berechnet die Populationsvarianz (mit n statt n-1 im Nenner)
     */
    public double populationVariance() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }

        double mean = mean();
        double sum = 0;

        for (Double value : data) {
            double diff = value - mean;
            sum += diff * diff;
        }

        return sum / data.size();
    }

    /**
     * Berechnet die Populations-Standardabweichung
     */
    public double populationStandardDeviation() {
        return Math.sqrt(populationVariance());
    }

    /**
     * Berechnet den geometrischen Mittelwert
     */
    public double geometricMean() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }

        // Prüfe, ob alle Werte positiv sind
        for (Double value : data) {
            if (value <= 0) {
                throw new IllegalStateException("Geometrisches Mittel erfordert positive Werte");
            }
        }

        double product = 1.0;
        for (Double value : data) {
            product *= value;
        }

        return Math.pow(product, 1.0 / data.size());
    }

    /**
     * Berechnet den harmonischen Mittelwert
     */
    public double harmonicMean() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Keine Daten vorhanden");
        }

        // Prüfe, ob alle Werte positiv sind
        double sumOfReciprocals = 0.0;
        for (Double value : data) {
            if (value <= 0) {
                throw new IllegalStateException("Harmonisches Mittel erfordert positive Werte");
            }
            sumOfReciprocals += 1.0 / value;
        }

        return data.size() / sumOfReciprocals;
    }

    /**
     * Berechnet die Summe der Quadrate
     */
    public double sumOfSquares() {
        double sum = 0;
        for (Double value : data) {
            sum += value * value;
        }
        return sum;
    }

    /**
     * Berechnet den Quartilsabstand (IQR)
     */
    public double interquartileRange() {
        if (data.size() < 4) {
            throw new IllegalStateException("Mindestens vier Datenpunkte für IQR erforderlich");
        }

        List<Double> sortedData = new ArrayList<>(data);
        Collections.sort(sortedData);

        int size = sortedData.size();
        
        // Berechne Q1 (unteres Quartil)
        double q1;
        if (size % 4 == 0) {
            int index = size / 4;
            q1 = (sortedData.get(index - 1) + sortedData.get(index)) / 2.0;
        } else {
            q1 = sortedData.get(size / 4);
        }
        
        // Berechne Q3 (oberes Quartil)
        double q3;
        if (size % 4 == 0) {
            int index = 3 * size / 4;
            q3 = (sortedData.get(index - 1) + sortedData.get(index)) / 2.0;
        } else {
            q3 = sortedData.get(3 * size / 4);
        }
        
        return q3 - q1;
    }
}