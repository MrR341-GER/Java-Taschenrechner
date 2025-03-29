wmic process where "commandline like '%GrafischerTaschenrechner%'" delete
javac -d bin GrafischerTaschenrechner.java
jar cvfe GrafischerTaschenrechner.jar GrafischerTaschenrechner -C bin .
java -jar GrafischerTaschenrechner.jar
