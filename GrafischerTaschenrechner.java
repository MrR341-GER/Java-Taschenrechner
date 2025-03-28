import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Integration des Funktionsplotters in den Taschenrechner
 */
public class GrafischerTaschenrechner extends Taschenrechner {
    private JTabbedPane tabbedPane;
    private PlotterPanel plotterPanel;

    public GrafischerTaschenrechner() {
        super(); // Ruft den Konstruktor des Taschenrechners auf

        // Bestehende Komponenten holen
        Container contentPane = getContentPane();
        Component mainComponent = contentPane.getComponent(0); // Das ist das verticalSplitPane
        contentPane.remove(mainComponent);

        // TabbedPane erstellen
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Taschenrechner", mainComponent);

        // Funktionsplotter erstellen und hinzufügen
        plotterPanel = new PlotterPanel();
        tabbedPane.addTab("Funktionsplotter", plotterPanel);

        // Tab-Wechsel-Event, um alles neu zu zeichnen
        tabbedPane.addChangeListener(e -> {
            tabbedPane.repaint();
            if (tabbedPane.getSelectedIndex() == 1) {
                plotterPanel.repaint();
            }
        });

        // TabbedPane zum Hauptfenster hinzufügen
        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Fenstergröße anpassen
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Schaltfläche für Transfer vom Taschenrechner zum Plotter hinzufügen
        JButton plotButton = new JButton("Plot");
        plotButton.setFont(new Font("Arial", Font.PLAIN, 16));
        plotButton.setBackground(new Color(220, 250, 220)); // Leichtes Grün

        // Minimum-Größe setzen (wie bei anderen Buttons)
        FontMetrics metrics = plotButton.getFontMetrics(plotButton.getFont());
        int textWidth = metrics.stringWidth(plotButton.getText());
        int textHeight = metrics.getHeight();
        int width = Math.max(textWidth + 16, 40);
        int height = Math.max(textHeight + 10, 35);
        plotButton.setMinimumSize(new Dimension(width, height));
        plotButton.setPreferredSize(new Dimension(width, height));

        // Aktion beim Klicken auf Plot-Button
        plotButton.addActionListener(e -> transferToPlotter());

        // Zum Taschenrechner-Control-Panel hinzufügen
        // Wir müssen das richtige Panel finden und den Button hinzufügen
        addButtonToControlPanel(plotButton);
    }

    /**
     * Findet das Control-Panel und fügt den Plot-Button hinzu
     */
    private void addButtonToControlPanel(JButton plotButton) {
        // Hier müssen wir durch die Komponenten-Hierarchie navigieren
        // Dies hängt stark vom genauen Layout des bestehenden Rechners ab
        // Annahme: Das Control-Panel ist ein JPanel in verticalSplitPane mit FlowLayout

        try {
            JSplitPane verticalSplitPane = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
                    .getComponentAt(0);
            JSplitPane mainSplitPane = (JSplitPane) verticalSplitPane.getTopComponent();
            JPanel mainPanel = (JPanel) mainSplitPane.getLeftComponent();

            // Wir suchen nach dem Panel mit den Control-Buttons (History, Debug)
            Component[] components = mainPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    Component[] panelComponents = panel.getComponents();

                    for (Component subComp : panelComponents) {
                        if (subComp instanceof JPanel) {
                            // Das könnte das ButtonPanel sein
                            JPanel buttonPanel = (JPanel) subComp;
                            Component[] buttonComponents = buttonPanel.getComponents();

                            for (Component buttonComp : buttonComponents) {
                                if (buttonComp instanceof JPanel &&
                                        ((JPanel) buttonComp).getLayout() instanceof FlowLayout) {
                                    // Das sollte das Control-Button-Panel sein
                                    ((JPanel) buttonComp).add(plotButton);
                                    ((JPanel) buttonComp).revalidate();
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            // Alternativ: Wenn wir das Panel nicht finden können, fügen wir es einfach
            // am unteren Rand des Hauptpanels hinzu
            JPanel plotButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            plotButtonPanel.add(plotButton);
            mainPanel.add(plotButtonPanel, BorderLayout.SOUTH);
            mainPanel.revalidate();

        } catch (Exception e) {
            // Im Fehlerfall als separates Panel hinzufügen
            JPanel plotButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            plotButtonPanel.add(plotButton);

            Container contentPane = getContentPane();
            contentPane.add(plotButtonPanel, BorderLayout.SOUTH);
            contentPane.revalidate();

            System.err.println("Konnte Button nicht zum Control-Panel hinzufügen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Überträgt den aktuellen Taschenrechner-Ausdruck zum Plotter
     */
    private void transferToPlotter() {
        // Aktuellen Ausdruck vom Taschenrechner holen
        String expression = getDisplayText();

        if (expression != null && !expression.isEmpty() && !expression.equals("0")) {
            // Zum Plotter-Tab wechseln
            tabbedPane.setSelectedIndex(1);

            // Ausdruck in das Funktionsfeld übertragen
            try {
                Component[] components = plotterPanel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        transferExpressionToTextField((JPanel) comp, expression);
                    }
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Übertragen des Ausdrucks: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Bitte zuerst einen Ausdruck im Taschenrechner eingeben.",
                    "Leerer Ausdruck",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Sucht rekursiv nach dem Textfeld im Plotter und setzt den Ausdruck
     */
    private boolean transferExpressionToTextField(JPanel panel, String expression) {
        Component[] components = panel.getComponents();

        for (Component comp : components) {
            if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                // Wir nehmen an, dass das Textfeld für die Funktionseingabe ist
                // Optional: Wir könnten nach einem bestimmten Namen oder einer Eigenschaft
                // suchen
                textField.setText(expression);
                return true;
            } else if (comp instanceof JPanel) {
                if (transferExpressionToTextField((JPanel) comp, expression)) {
                    return true;
                }
            } else if (comp instanceof JSplitPane) {
                JSplitPane splitPane = (JSplitPane) comp;
                Component left = splitPane.getLeftComponent();
                Component right = splitPane.getRightComponent();

                if (left instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) left, expression)) {
                        return true;
                    }
                }
                if (right instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) right, expression)) {
                        return true;
                    }
                }
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JPanel) {
                    if (transferExpressionToTextField((JPanel) view, expression)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Holt den aktuellen Text aus dem Display des Taschenrechners
     */
    private String getDisplayText() {
        try {
            // Wir müssen zur Komponente navigieren, die das Display enthält
            JSplitPane verticalSplitPane = (JSplitPane) ((JTabbedPane) getContentPane().getComponent(0))
                    .getComponentAt(0);
            JSplitPane mainSplitPane = (JSplitPane) verticalSplitPane.getTopComponent();
            JPanel mainPanel = (JPanel) mainSplitPane.getLeftComponent();

            // Das Display ist normalerweise das erste Element im mainPanel
            Component[] components = mainPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    return ((JTextField) comp).getText();
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Lesen des Display-Texts: " + e.getMessage());
        }

        return "";
    }

    /**
     * Hauptmethode
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GrafischerTaschenrechner rechner = new GrafischerTaschenrechner();
            rechner.setVisible(true);
        });
    }
}