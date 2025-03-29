wmic process where "commandline like '%%GrafischerTaschenrechner%%'" delete
javac -sourcepath src -d bin src\GrafischerTaschenrechner.java
jar cvfe GrafischerTaschenrechner.jar GrafischerTaschenrechner -C bin .
java -jar GrafischerTaschenrechner.jar