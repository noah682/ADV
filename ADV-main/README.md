# UML to Java Converter

Automatische Generierung von Java-Quellcode aus UMLet-Klassendiagrammen (`.uxf`).

---

## Voraussetzungen

- **JDK 21** (getestet mit Oracle JDK 21.0.8)
- Zum Starten der GUI: JavaFX 21 (liegt als `.jar`-Dateien im Ordner `app/lib/`)

---

## Start

### GUI (empfohlen)

Doppelklick auf `UML_Converter.bat` – oder manuell:

```
javaw --module-path app\lib --add-modules javafx.controls,javafx.graphics,javafx.base -cp app\classes UmlLauncherApp
```

### Kommandozeile

```
java -jar app\UmlToJava-new.jar <eingabe.uxf> <ausgabeordner>
```

Beispiel:
```
java -jar app\UmlToJava-new.jar "H:\prog\Test_Klassendiagramme\TestCase4_Enum_Wochentage.uxf" "H:\prog\output"
```

---

## Bedienung (GUI)

1. `.uxf`-Datei per **Drag & Drop** auf die Drop-Zone ziehen  
   oder die Drop-Zone anklicken und Datei im Dateidialog auswaehlen.
2. **▶ Start** druecken – das Konverter-JAR wird gestartet.
3. Fortschritt und generierte Dateien erscheinen im schwarzen Log-Bereich.
4. Die `.java`-Dateien landen in einem automatisch benannten Unterordner  
   neben der `.uxf`-Datei (Schema: `<Dateiname>_<Zeitstempel>/`).
5. **Zuruecksetzen** leert die Auswahl und das Log.

---

## Unterstuetzte UML-Notation

### Klassen

```
MeineKlasse
--
- feld : String
+ anderesFeld : int
--
+ MeineKlasse(feld : String)
+ methode() : void
- privateMethode(x : int) : boolean
```

### Abstrakte Klassen

Variante 1 – Modifier als eigene Zeile:
```
Fahrzeug
{abstract}
--
...
```

Variante 2 – Schraegstrich-Notation:
```
/Fahrzeug/
--
...
```

### Interfaces

Stereotyp `<<interface>>` oder `«interface»`:
```
«interface»
Fahrbar
--
--
+ fahre() : void
```

Erzeugt: `public interface Fahrbar { void fahre(); }`

### Enumerationen

Stereotyp `<<enum>>`, `«enum»`, `<<enumeration>>` oder `<<enumerations>>`:
```
«enum»
Wochentag
--
MONTAG
DIENSTAG
...
SONNTAG
--
- aufgabe : String
--
+ Wochentag(aufgabe : String)
```

Erzeugt: Enum mit Konstanten, Feldern und privatem Konstruktor.

### Sichtbarkeit

| Zeichen | Java-Keyword |
|---------|-------------|
| `+`     | `public`    |
| `-`     | `private`   |
| `#`     | `protected` |
| `~`     | (package)   |

### Generics

Parameter und Felder mit Generics werden unveraendert uebernommen:
`- liste : ArrayList<String>` erzeugt `private ArrayList<String> liste;`

### Sonderzeichen in Bezeichnern

Umlaute (ae, oe, ue) und andere Unicode-Buchstaben in Klassen- und Membernamen sind in Java
erlaubt und werden korrekt weitergegeben.

Zeichen, die keine gueltigen Java-Identifier-Zeichen sind (z. B. Bindestrich `-`),
werden automatisch entfernt: `Online-Fortbildung` wird zu `OnlineFortbildung`.

---

## Automatisch generierter Code

Fuer jede Klasse im Diagramm entsteht eine eigene `.java`-Datei mit:

- Klassendeklaration (`public class`, `public abstract class`, `public interface`, `public enum`)
- Felder (aus Attributzeilen)
- Getter und Setter fuer jedes Feld (nur wenn nicht bereits als Methode im Diagramm vorhanden)
- Konstruktoren und Methoden mit TODO-Rumpf
- Enum-Konstantenliste
- Leere Platzhalter-Klassen fuer Typen, die im Diagramm referenziert,
  aber nicht definiert sind (z. B. `Waehrung`) &ndash; so kompiliert die
  gesamte Ausgabe ohne manuelle Nacharbeit

---

## Bekannte Grenzen

- **Vererbung / Implements**: Pfeile zwischen Klassen werden nicht ausgewertet.
  `extends` und `implements` muessen manuell im generierten Code ergaenzt werden.
- **Undefinierte Typen**: Fuer Typen, die im Diagramm referenziert, aber nicht
  definiert sind (z. B. `Waehrung`, `Teilnehmer`), wird eine leere
  Platzhalter-Klasse mit TODO-Kommentar erzeugt. Der Code kompiliert damit
  vollstaendig; die Platzhalter muessen aber manuell ausimplementiert werden.
- **Statisch / final**: Die UMLet-Unterstreichungsnotation (`_text_`) wird gelesen,
  aber das `static`-Schluesselwort wird nicht automatisch hinzugefuegt.
- **Default-Werte**: Attribut-Standardwerte (z. B. `= 0`) werden aus der Typ-Angabe
  entfernt und nicht als Initialisierer uebernommen.

---

## Projektstruktur

```
ADV-main/          Java-Quellcode der Konverter-Bibliothek (Paket progProjekt)
  Main.java          Einstiegspunkt + Pipeline-Steuerung
  UMLReader.java     XML-Parser fuer .uxf-Dateien
  UMLParser.java     Zerlegung der Panel-Bloecke
  DataSave.java      Datencontainer je Klasse
  ClassGenerator.java  Java-Quellcode-Erzeugung

app/
  src/UmlLauncherApp.java   JavaFX-GUI
  lib/                      JavaFX-Bibliotheken
  UmlToJava-new.jar         Ausfuehrbares JAR
```
