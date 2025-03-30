@echo off
REM Kill any running calculator process
wmic process where "commandline like '%%GrafischerTaschenrechner%%'" delete

REM Create bin directory if it doesn't exist
if not exist bin mkdir bin

REM Clear bin directory to avoid class conflicts
del /Q bin\*.* 2>nul

REM Compile the source files with debug information
javac -g -d bin src\core\*.java src\plugins\*.java src\plugins\plotter2d\*.java src\plugins\plotter3d\*.java src\plugins\plotter3d\model\*.java src\plugins\plotter3d\renderer\*.java src\plugins\plotter3d\view\*.java src\plugins\plotter3d\ui\*.java src\plugins\plotter3d\interaction\*.java src\plugins\converter\*.java src\plugins\scientific\*.java src\plugins\statistics\*.java src\plugins\plotter2d\intersection\*.java src\util\debug\*.java src\util\error\*.java src\util\history\*.java src\parser\*.java src\common\*.java

REM Create the JAR file with the core.GrafischerTaschenrechner as the main class
jar cvfe GrafischerTaschenrechner.jar core.GrafischerTaschenrechner -C bin .

REM Run the application directly with the main class to ensure initialization methods are called
java -Xmx4G -cp GrafischerTaschenrechner.jar core.GrafischerTaschenrechner