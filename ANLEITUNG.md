## UML to Java Converter

Dieses Programm wandelt ein UML Diagramm im Format uxf, wie es das Programm UMLet speichert, in fertigen Java Quellcode um. Für jede Klasse im Diagramm wird eine eigene Datei mit der Endung java erzeugt. Es gibt zwei Wege, das Programm zu starten. Der erste Weg über das Fenster ist der einfachere. Der zweite Weg über das Terminal ist für alle gedacht, die lieber mit der Kommandozeile arbeiten.

Auf dem Rechner muss Java installiert sein, und zwar in der Version 21 oder neuer. JavaFX muss nicht gesondert installiert werden, da die passenden Bibliotheken bereits im Projektordner im Ordner app und lib enthalten sind.


## Weg 1, über das Fenster

1. Öffnen Sie den Projektordner und starten Sie die Datei UML_Converter.bat mit einem Doppelklick. Nach kurzer Zeit erscheint das Fenster mit dem Namen UML to Java Converter.

2. Wählen Sie die gewünschte uxf Datei aus. Dazu ziehen Sie die Datei entweder mit der Maus in das große Feld in der Mitte, oder Sie klicken auf das Feld und suchen die Datei im Dateidialog. Es werden nur Dateien mit der Endung uxf angenommen.

3. Klicken Sie auf die Schaltfläche Start. Der Fortschritt wird im unteren Ausgabebereich angezeigt.

4. Nach dem Durchlauf finden Sie das Ergebnis direkt neben der ausgewählten uxf Datei. Dort entsteht ein neuer Ordner, dessen Name aus dem Namen der Datei und dem aktuellen Datum mit Uhrzeit besteht. In diesem Ordner liegen die erzeugten Java Dateien.

5. Möchten Sie eine weitere Datei umwandeln, klicken Sie auf Zurücksetzen und beginnen wieder bei Schritt 2. Zum Beenden schließen Sie das Fenster.


## Weg 2, über das Terminal

1. Öffnen Sie im Projektordner eine Eingabeaufforderung.

2. Geben Sie den Startbefehl ein. Er besteht aus dem Aufruf von Java, dem Konverterprogramm, dem Pfad zur uxf Datei und dem gewünschten Ausgabeordner. Ein vollständiges Beispiel sieht so aus:

   java -jar app\UmlToJava-new.jar C:\Diagramme\MeinDiagramm.uxf C:\Diagramme\ausgabe

3. Bestätigen Sie mit der Eingabetaste. Das Programm liest die Datei ein und schreibt die erzeugten Java Dateien in den angegebenen Ausgabeordner. Zum Schluss wird angezeigt, wie viele Klassen erkannt und wie viele Dateien geschrieben wurden.

4. Wird der Ausgabeordner beim Startbefehl weggelassen, fragt das Programm die beiden Pfade nacheinander im Terminal ab.
