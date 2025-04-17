# Klassendiagramm des Java-Taschenrechners

Dieses Dokument beschreibt die Hauptklassen des Taschenrechners und deren Beziehungen zueinander.

```
+-------------------------+     +------------------------+
|     Taschenrechner      |<----| GrafischerTaschenrechner|
+-------------------------+     +------------------------+
| - displayField          |     | + transferFunctionToPlotter()
| - historyManager        |     | + transferFunctionTo3DPlotter()
| - debugManager          |     +------------------------+
| - calculationEngine     |            ^
| - inputHandler          |            |
| - keypad                |     +------+-------+
+-------------------------+     |  PluginManager |
| + getDisplayText()      |     +------+-------+
| + setDisplayText()      |            |
| + addToHistory()        |            v
| + debug()               |     +------+-------+
| + toggleHistory()       |     | CalculatorPlugin |
| + toggleDebug()         |     +----------------+
+-------------------------+     | + getName()    |
           ^                    | + createPanel()|
           |                    | + initialize() |
           |                    | + shutdown()   |
+----------+----------+         +----------------+
|   CalculationEngine  |              ^
+---------------------+               |
| + berechneFormel()  |        +------+-------+------+
| + berechneAusdruck()|        |      |       |      |
| + checkIfFunction() |   +----+--+ +-+-----+ | +----+---+
+---------------------+   |Plotter| |Stats  | | |Converter|
                          +-------+ +-------+ | +--------+
+---------------------+                       |
|    InputHandler     |                +------+------+
+---------------------+                |ScientificCalc|
| + handleInput()     |                +-------------+
| + handleNumericInput() |
| + handleOperatorInput()|
| + handleSpecialOperation() |
+---------------------+
           |
           v
+---------------------+     +------------------+
|  TaschenrechnerKeypad|     |  Parser         |
+---------------------+     +------------------+
| + createKeypadPanel()|     | + parse()       |
+---------------------+     | + validate()    |
                            +------------------+
                                    ^
                                    |
                      +-------------+------------+
                      |                          |
           +----------+--------+    +-----------+--------+
           | FunctionParser    |    | Function3DParser   |
           +-------------------+    +--------------------+
           | + parseFunction() |    | + parseFunction()  |
           | + evaluateAt()    |    | + evaluateAt()     |
           +-------------------+    +--------------------+

+-------------------+     +-------------------+
| HistoryManager    |     | DebugManager      |
+-------------------+     +-------------------+
| + addToHistory()  |     | + debug()         |
| + getHistory()    |     | + showDebugDialog()|
| + clearHistory()  |     | + hideDebugDialog()|
| + showHistoryDialog() | +-------------------+
+-------------------+
```

## Beschreibung der Hauptbeziehungen

1. **Taschenrechner** ist die zentrale Klasse, die Instanzen von `HistoryManager`, `DebugManager`, `CalculationEngine`, `InputHandler` und `TaschenrechnerKeypad` besitzt.

2. **GrafischerTaschenrechner** erweitert den Basistaschenrechner und fügt grafische Funktionalitäten hinzu.

3. **PluginManager** verwaltet die Plugins und ist mit dem `GrafischerTaschenrechner` verbunden.

4. **CalculatorPlugin** ist ein Interface, das von allen Plugin-Implementierungen implementiert wird.

5. Die **Parser**-Hierarchie besteht aus:
   - `AbstractExpressionParser` als Basisklasse
   - `FunctionParser` zum Parsen von 2D-Funktionen
   - `Function3DParser` zum Parsen von 3D-Funktionen

6. Spezielle **Plugins** wie `Plotter`, `ScientificCalc`, `Stats` und `Converter` erweitern die Grundfunktionalität des Taschenrechners.

7. **InputHandler** verarbeitet Benutzereingaben und leitet sie an die entsprechenden Komponenten weiter.

8. **CalculationEngine** führt die eigentlichen mathematischen Berechnungen durch.

9. **HistoryManager** und **DebugManager** verwalten den Berechnungsverlauf bzw. Debugging-Informationen. 