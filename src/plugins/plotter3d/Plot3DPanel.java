
package plugins.plotter3d;

import javax.swing.*;

import common.ColorChooser;
import core.GrafischerTaschenrechner;
import plugins.plotter3d.interaction.Plot3DInteractionHandler;
import plugins.plotter3d.renderer.Plot3DRenderer;
import plugins.plotter3d.ui.Plot3DFunctionManager;
import plugins.plotter3d.view.Plot3DUIBuilder;
import plugins.plotter3d.view.Plot3DViewController;
import util.debug.DebugManager;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.ParseException;

/**
 * Panel für 3D-Funktionsplots mit Unterstützung für mehrere Funktionen
 * (Refactored version with components split into smaller classes)
 */
public class Plot3DPanel extends JPanel {
    private final GrafischerTaschenrechner calculator;
    private final Plot3DRenderer renderer;
    private JPanel plotPanel;

    // UI-Komponenten Manager
    private final Plot3DUIBuilder uiBuilder;

    // Funktions-Manager
    private final Plot3DFunctionManager functionManager;

    // Interaktions-Handler
    private final Plot3DInteractionHandler interactionHandler;

    // View-Controller
    private final Plot3DViewController viewController;

    // Debug-Referenz
    private DebugManager debugManager;

    /**
     * Konstruktor für das 3D-Plot-Panel
     */
    public Plot3DPanel(GrafischerTaschenrechner calculator) {
        this.calculator = calculator;

        // Initialisiere den Renderer mit Standardwerten
        renderer = new Plot3DRenderer(
                Plot3DViewController.DEFAULT_MIN,
                Plot3DViewController.DEFAULT_MAX,
                Plot3DViewController.DEFAULT_MIN,
                Plot3DViewController.DEFAULT_MAX,
                Plot3DViewController.DEFAULT_RESOLUTION);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Manager initialisieren
        viewController = new Plot3DViewController(renderer);
        functionManager = new Plot3DFunctionManager(renderer, viewController);

        // UI-Builder erstellen
        uiBuilder = new Plot3DUIBuilder(this, functionManager, viewController);

        // UI-Komponenten erstellen
        createUI();

        // Wichtig: Referenzen auf dieses Panel setzen
        viewController.setParentPanel(this);
        functionManager.setParentPanel(this);

        // Textfelder aus dem UI-Builder in den ViewController übertragen
        viewController.setXMinField(uiBuilder.getXMinField());
        viewController.setXMaxField(uiBuilder.getXMaxField());
        viewController.setYMinField(uiBuilder.getYMinField());
        viewController.setYMaxField(uiBuilder.getYMaxField());

        // Interaktions-Handler initialisieren (nach UI-Erstellung)
        interactionHandler = new Plot3DInteractionHandler(plotPanel, viewController, this);

        // Jetzt erst initialen Plot rendern, wenn alles initialisiert ist
        SwingUtilities.invokeLater(() -> renderPlot());
    }

    /**
     * Erstellt die Benutzeroberfläche
     */
    private void createUI() {
        // Hauptbereich: Plot und Steuerungsbereich
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7); // Plot erhält 70% des Platzes

        // Plot-Panel mit Canvas für 3D-Darstellung
        plotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (renderer != null) {
                    renderer.render((Graphics2D) g, getWidth(), getHeight());
                }
            }
        };
        plotPanel.setBackground(Color.WHITE);

        // Mindestgröße für den Plot festlegen
        plotPanel.setMinimumSize(new Dimension(400, 300));

        // Steuerungsbereich erstellen
        JPanel controlPanel = uiBuilder.createControlPanel();
        controlPanel.setMinimumSize(new Dimension(250, 300));

        // Panes zusammenfügen
        splitPane.setLeftComponent(plotPanel);
        splitPane.setRightComponent(controlPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    // Zusätzliche Hilfsmethode, um das Funktionsfeld aus dem UI-Builder abzurufen
    public JTextField getFunctionField() {
        return uiBuilder.getFunctionField();
    }

    // In der renderPlot() Methode, um die Standard-Funktion zu verbessern:
    public void renderPlot() {
        try {
            // Überprüfe auf leere Funktionen
            if (functionManager.getFunctionListModel().isEmpty()) {
                // Optional: Füge eine Standard-Funktion hinzu
                if (renderer.getFunctions().isEmpty()) {
                    String defaultFunction = Plot3DViewController.DEFAULT_FUNCTION;
                    Color defaultColor = ColorChooser.generateRandomColor();
                    String colorName = ColorChooser.getColorName(defaultColor);

                    // Testweise eine Funktion hinzufügen (nur beim ersten Start)
                    renderer.addFunction(defaultFunction, defaultColor);
                    functionManager.getFunctionListModel()
                            .addElement("f(x,y) = " + defaultFunction + " [" + colorName + "]");

                    // Wenn es ein Funktionsfeld gibt, aktualisiere es auch
                    JTextField functionField = getFunctionField();
                    if (functionField != null) {
                        functionField.setText(defaultFunction);
                    }
                }
            }

            // Bereichsangaben parsen und Renderer aktualisieren
            renderer.setBounds(
                    viewController.getCurrentXMin(),
                    viewController.getCurrentXMax(),
                    viewController.getCurrentYMin(),
                    viewController.getCurrentYMax());

            // Plot neu zeichnen - wichtig: beide aufrufen!
            if (plotPanel != null) {
                plotPanel.repaint();
                plotPanel.revalidate();
            }

            debug("3D-Plot neu gezeichnet");

        } catch (Exception e) {
            debug("Fehler beim Rendern: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Fehler beim Rendern der Funktion: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Setzt den DebugManager für Logging
     */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
        viewController.setDebugManager(debugManager);
        functionManager.setDebugManager(debugManager);
        interactionHandler.setDebugManager(debugManager);
    }

    /**
     * Schreibt Debug-Informationen
     */
    public void debug(String message) {
        if (debugManager != null) {
            debugManager.debug("[3D-Plotter] " + message);
        } else {
            System.out.println("[3D-Plotter] " + message);
        }
    }

    /**
     * Gibt den Plot3DRenderer zurück
     */
    public Plot3DRenderer getRenderer() {
        return renderer;
    }

    /**
     * Gibt den Plot3DFunctionManager zurück
     */
    public Plot3DFunctionManager getFunctionManager() {
        return functionManager;
    }

    /**
     * Gibt den Plot3DViewController zurück
     */
    public Plot3DViewController getViewController() {
        return viewController;
    }

    /**
     * Gibt das Plot-Panel zurück
     */
    public JPanel getPlotPanel() {
        return plotPanel;
    }
}