@echo off
echo Erstelle Ordnerstruktur und verschiebe Dateien fuer den Taschenrechner...

REM Erstelle Hauptordnerstruktur
mkdir src\core
mkdir src\common
mkdir src\util\debug
mkdir src\util\error
mkdir src\util\history
mkdir src\parser
mkdir src\plugins
mkdir src\plugins\plotter2d
mkdir src\plugins\plotter2d\intersection
mkdir src\plugins\plotter3d
mkdir src\plugins\plotter3d\model
mkdir src\plugins\plotter3d\view
mkdir src\plugins\plotter3d\renderer
mkdir src\plugins\plotter3d\interaction
mkdir src\plugins\plotter3d\ui
mkdir src\plugins\scientific
mkdir src\plugins\statistics
mkdir src\plugins\converter

REM Verschiebe Core-Dateien
move src\Taschenrechner.java src\core\
move src\CalculationEngine.java src\core\
move src\InputHandler.java src\core\
move src\TaschenrechnerKeypad.java src\core\
move src\GrafischerTaschenrechner.java src\core\

REM Verschiebe Common-Dateien
move src\CalculatorConstants.java src\common\
move src\ColorChooser.java src\common\
move src\UIComponents.java src\common\

REM Verschiebe Debug & Utility-Dateien
move src\DebugManager.java src\util\debug\
move src\Logger.java src\util\debug\
move src\CalculatorLogger.java src\util\debug\
move src\ErrorHandler.java src\util\error\
move src\HistoryManager.java src\util\history\

REM Verschiebe Parser-Dateien
move src\AbstractExpressionParser.java src\parser\
move src\FunctionParser.java src\parser\
move src\Function3DParser.java src\parser\

REM Verschiebe 2D-Plotter-Dateien
move src\PlotterPanel.java src\plugins\plotter2d\
move src\GraphPanel.java src\plugins\plotter2d\
move src\GridRenderer.java src\plugins\plotter2d\
move src\FunctionRenderer.java src\plugins\plotter2d\
move src\FunctionInputPanel.java src\plugins\plotter2d\
move src\ViewControlPanel.java src\plugins\plotter2d\
move src\CoordinateTransformer.java src\plugins\plotter2d\
move src\ExamplePanel.java src\plugins\plotter2d\
move src\FunctionEditDialog.java src\plugins\plotter2d\

REM Verschiebe Intersection-Dateien
move src\IntersectionPoint.java src\plugins\plotter2d\intersection\
move src\IntersectionCalculator.java src\plugins\plotter2d\intersection\
move src\IntersectionFinder.java src\plugins\plotter2d\intersection\
move src\IntersectionPanel.java src\plugins\plotter2d\intersection\

REM Verschiebe 3D-Plotter-Dateien
move src\Plot3DPanel.java src\plugins\plotter3d\

REM Verschiebe 3D-Model-Dateien
move src\Plot3DModel.java src\plugins\plotter3d\model\
move src\Plot3DPoint.java src\plugins\plotter3d\model\

REM Verschiebe 3D-View-Dateien
move src\Plot3DView.java src\plugins\plotter3d\view\
move src\Plot3DViewController.java src\plugins\plotter3d\view\
move src\Plot3DUIBuilder.java src\plugins\plotter3d\view\

REM Verschiebe 3D-Renderer-Dateien
move src\Plot3DRenderer.java src\plugins\plotter3d\renderer\
move src\Plot3DColorScheme.java src\plugins\plotter3d\renderer\
move src\Plot3DTransformer.java src\plugins\plotter3d\renderer\
move src\Plot3DGridRenderer.java src\plugins\plotter3d\renderer\
move src\Plot3DFunctionRenderer.java src\plugins\plotter3d\renderer\

REM Verschiebe 3D-Interaction-Dateien
move src\Plot3DInteractionHandler.java src\plugins\plotter3d\interaction\

REM Verschiebe 3D-UI-Dateien
move src\Plot3DFunctionManager.java src\plugins\plotter3d\ui\
move src\Example3DPanel.java src\plugins\plotter3d\ui\

REM Verschiebe Scientific Panel-Dateien
move src\ScientificPanel.java src\plugins\scientific\

REM Verschiebe Statistics-Dateien
move src\StatisticCalculator.java src\plugins\statistics\
move src\StatisticsPanel.java src\plugins\statistics\

REM Verschiebe Converter-Dateien
move src\UnitConverter.java src\plugins\converter\
move src\ConverterPanel.java src\plugins\converter\

REM Erstelle die Plugin-Interface-Datei
echo package plugins;> src\plugins\CalculatorPlugin.java
echo.>> src\plugins\CalculatorPlugin.java
echo import javax.swing.JPanel;>> src\plugins\CalculatorPlugin.java
echo import core.Taschenrechner;>> src\plugins\CalculatorPlugin.java
echo.>> src\plugins\CalculatorPlugin.java
echo public interface CalculatorPlugin {>> src\plugins\CalculatorPlugin.java
echo     String getName();           // Name des Plugins>> src\plugins\CalculatorPlugin.java
echo     JPanel createPanel(Taschenrechner calculator); // Panel-Erstellung>> src\plugins\CalculatorPlugin.java
echo     void initialize();          // Initialisierung>> src\plugins\CalculatorPlugin.java
echo     void shutdown();            // Aufräumen bei Beendigung>> src\plugins\CalculatorPlugin.java
echo }>> src\plugins\CalculatorPlugin.java

REM Erstelle den Plugin-Manager
echo package plugins;> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo import java.util.HashMap;>> src\plugins\PluginManager.java
echo import java.util.Map;>> src\plugins\PluginManager.java
echo import core.Taschenrechner;>> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo public class PluginManager {>> src\plugins\PluginManager.java
echo     private final Map^<String, CalculatorPlugin^> plugins = new HashMap^<^>();>> src\plugins\PluginManager.java
echo     private final Taschenrechner calculator;>> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo     public PluginManager(Taschenrechner calculator) {>> src\plugins\PluginManager.java
echo         this.calculator = calculator;>> src\plugins\PluginManager.java
echo     }>> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo     public void registerPlugin(CalculatorPlugin plugin) {>> src\plugins\PluginManager.java
echo         plugins.put(plugin.getName(), plugin);>> src\plugins\PluginManager.java
echo         plugin.initialize();>> src\plugins\PluginManager.java
echo     }>> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo     public CalculatorPlugin getPlugin(String name) {>> src\plugins\PluginManager.java
echo         return plugins.get(name);>> src\plugins\PluginManager.java
echo     }>> src\plugins\PluginManager.java
echo.>> src\plugins\PluginManager.java
echo     public void createAllPluginTabs() {>> src\plugins\PluginManager.java
echo         // Erstellt Tabs für alle registrierten Plugins>> src\plugins\PluginManager.java
echo         for (CalculatorPlugin plugin : plugins.values()) {>> src\plugins\PluginManager.java
echo             calculator.addTab(plugin.getName(), plugin.createPanel(calculator));>> src\plugins\PluginManager.java
echo         }>> src\plugins\PluginManager.java
echo     }>> src\plugins\PluginManager.java
echo }>> src\plugins\PluginManager.java

echo Fertig! Die Dateien wurden in die neue Ordnerstruktur verschoben.
echo Hinweis: Du musst jetzt noch die package-Deklarationen und imports in allen Dateien aktualisieren.