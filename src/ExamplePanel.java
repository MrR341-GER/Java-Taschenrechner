import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel with function examples that can be selected and saved
 */
public class ExamplePanel {
    private final PlotterPanel plotter;
    private final JTextField functionField;
    private DefaultListModel<String> exampleListModel;
    private JList<String> exampleList;

    // Default function examples
    private final String[] defaultExamples = {
            "x^2", "sin(x)", "cos(x)", "tan(x)", "x^3-3*x", "sqrt(x)",
            "log(x)", "ln(x)", "abs(x)", "exp(x)", "sin(x)+cos(x)", "e^(0.05*x)*sin(x)"
    };

    // Datei zum Speichern der Beispiele
    private static final String EXAMPLES_FILE = "function_examples.txt";

    /**
     * Creates a new example panel
     */
    public ExamplePanel(PlotterPanel plotter, JTextField functionField) {
        this.plotter = plotter;
        this.functionField = functionField;
    }

    /**
     * Creates the example panel with a list of examples and buttons to manage them
     */
    public JPanel createExamplesPanel() {
        // Erstelle ein Panel mit BorderLayout
        JPanel examplesPanel = new JPanel(new BorderLayout());
        examplesPanel.setBorder(BorderFactory.createTitledBorder("Beispiele"));

        // Erstelle die Liste für Beispiele
        exampleListModel = new DefaultListModel<>();
        exampleList = new JList<>(exampleListModel);
        exampleList.setFont(new Font("Arial", Font.PLAIN, 12));
        exampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Lade gespeicherte Beispiele oder verwende Standardbeispiele
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
            }
        });

        // Button zum Entfernen eines ausgewählten Beispiels
        JButton removeButton = new JButton("Entfernen");
        removeButton.addActionListener(e -> {
            int selectedIndex = exampleList.getSelectedIndex();
            if (selectedIndex >= 0) {
                exampleListModel.remove(selectedIndex);
                saveExamples();
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
     * Lädt gespeicherte Beispiele aus einer Datei oder verwendet die
     * Standardbeispiele
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
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Beispiele: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der Beispiele: " + e.getMessage());
        }
    }

    /**
     * Gibt das Modell der Beispielliste zurück
     */
    public DefaultListModel<String> getExampleListModel() {
        return exampleListModel;
    }
}