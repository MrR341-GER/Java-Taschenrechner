import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel mit Beispielfunktionen für den 3D-Plotter
 */
public class Example3DPanel {
    private final Plot3DPanel plotterPanel;
    private final JTextField functionField;
    private DefaultListModel<String> exampleListModel;
    private JList<String> exampleList;

    // Standard-3D-Funktionsbeispiele
    private final String[] defaultExamples = {
            "sin(sqrt(x^2+y^2))",             // Kreisförmige Welle (Mexican Hat)
            "cos(x) * sin(y)",                // Gewellte Oberfläche
            "x^2 + y^2",                      // Paraboloid
            "sin(x) + cos(y)",                // Periodische Oberfläche
            "exp(-(x^2+y^2)/4)",              // Glockenkurve (Normalverteilung)
            "sin(x*y)",                       // Komplexes Rippelmuster
            "sqrt(abs(x) + abs(y))",          // Kegelförmige Oberfläche
            "1/(1+x^2+y^2)",                  // Umgekehrter Kegel
            "sin(3*x) * cos(3*y) / 3",        // Komplexe trigonometrische Oberfläche
            "cos(sqrt(x^2+y^2)) * exp(-0.1*(x^2+y^2))", // Gedämpfte Wellenringe
            "sin(x) * sin(y)",                // Schachbrettmuster
            "abs(sin(x) * cos(y))"            // Gefaltete Oberfläche
    };

    // Datei zum Speichern der Beispiele
    private static final String EXAMPLES_FILE = "function3d_examples.txt";

    /**
     * Erstellt ein neues Beispiel-Panel für 3D-Funktionen
     */
    public Example3DPanel(Plot3DPanel plotterPanel, JTextField functionField) {
        this.plotterPanel = plotterPanel;
        this.functionField = functionField;
    }

    /**
     * Erstellt das Beispiel-Panel mit einer Liste von 3D-Funktionen und Buttons zum Verwalten
     */
    public JPanel createExamplesPanel() {
        // Panel mit BorderLayout erstellen
        JPanel examplesPanel = new JPanel(new BorderLayout());
        examplesPanel.setBorder(BorderFactory.createTitledBorder("3D-Beispiele"));

        // Liste für Beispiele erstellen
        exampleListModel = new DefaultListModel<>();
        exampleList = new JList<>(exampleListModel);
        exampleList.setFont(new Font("Arial", Font.PLAIN, 12));
        exampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Gespeicherte Beispiele laden oder Standardbeispiele verwenden
        loadExamples();

        // Doppelklick-Listener für die Liste
        exampleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = exampleList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String selectedExample = exampleList.getModel().getElementAt(index);
                        functionField.setText(selectedExample);
                        debug("3D-Beispiel ausgewählt: " + selectedExample);
                    }
                }
            }
        });

        // Scrollpane für die Liste
        JScrollPane scrollPane = new JScrollPane(exampleList);
        scrollPane.setPreferredSize(new Dimension(150, 150));

        // Button-Panel für Aktionen
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));

        // Button zum Hinzufügen des aktuellen Ausdrucks zu den Beispielen
        JButton addButton = new JButton("Hinzufügen");
        addButton.addActionListener(e -> {
            String currentFunction = functionField.getText().trim();
            if (!currentFunction.isEmpty() && !exampleListModel.contains(currentFunction)) {
                exampleListModel.addElement(currentFunction);
                saveExamples();
                debug("3D-Beispiel hinzugefügt: " + currentFunction);
            }
        });

        // Button zum Entfernen eines ausgewählten Beispiels
        JButton removeButton = new JButton("Entfernen");
        removeButton.addActionListener(e -> {
            int selectedIndex = exampleList.getSelectedIndex();
            if (selectedIndex >= 0) {
                String removedExample = exampleListModel.get(selectedIndex);
                exampleListModel.remove(selectedIndex);
                saveExamples();
                debug("3D-Beispiel entfernt: " + removedExample);
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        // Alles zusammenfügen
        examplesPanel.add(scrollPane, BorderLayout.CENTER);
        examplesPanel.add(buttonPanel, BorderLayout.SOUTH);

        return examplesPanel;
    }

    /**
     * Lädt gespeicherte Beispiele aus einer Datei oder verwendet die Standardbeispiele
     */
    private void loadExamples() {
        // Erst alle Standardbeispiele laden
        for (String example : defaultExamples) {
            if (!exampleListModel.contains(example)) {
                exampleListModel.addElement(example);
            }
        }

        try {
            File file = new File(EXAMPLES_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !exampleListModel.contains(line)) {
                        exampleListModel.addElement(line);
                    }
                }

                reader.close();
                debug("3D-Beispiele aus Datei geladen");
            }
        } catch (IOException e) {
            debug("Fehler beim Laden der 3D-Beispiele: " + e.getMessage());
        }
    }

    /**
     * Speichert alle Beispiele in einer Datei
     */
    private void saveExamples() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(EXAMPLES_FILE));

            for (int i = 0; i < exampleListModel.getSize(); i++) {
                writer.write(exampleListModel.getElementAt(i));
                writer.newLine();
            }

            writer.close();
            debug("3D-Beispiele in Datei gespeichert");
        } catch (IOException e) {
            debug("Fehler beim Speichern der 3D-Beispiele: " + e.getMessage());
        }
    }

    /**
     * Debug-Hilfsmethod
     */
    private void debug(String message) {
        if (plotterPanel != null) {
            plotterPanel.debug("[3D-Beispiele] " + message);
        } else {
            System.out.println("[3D-Beispiele] " + message);
        }
    }

    /**
     * Gibt das Listenmodell der Beispiele zurück
     */
    public DefaultListModel<String> getExampleListModel() {
        return exampleListModel;
    }
}
