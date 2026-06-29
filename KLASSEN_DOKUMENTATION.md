# UML to Java Converter — Klassen- & Funktionsdokumentation

> Detaillierte Erklärung **jeder Klasse** und **jeder Methode** — nicht nur *was* sie
> tut, sondern *wie* die Logik tatsächlich funktioniert. Grundlage für Präsentation,
> Anleitung und mündliche Erklärung.

---

## Überblick: Die Pipeline

Das Programm wandelt ein UMLet-Klassendiagramm (`.uxf`-Datei) in echten Java-Quellcode
um. Es ist bewusst in vier Verantwortlichkeiten getrennt (Single Responsibility):

```
.uxf-Datei
   │
   ▼
UMLReader      ──►  liest die XML-Datei, holt jeden <panel_attributes>-Block als Rohtext
   │
   ▼
UMLParser      ──►  zerlegt jeden Block in: Name, Art, Attribute, Methoden, Enum-Konstanten
   │
   ▼
DataSave       ──►  hält diese Daten je Klasse (reiner Container, noch rohe Strings)
   │
   ▼
ClassGenerator ──►  erzeugt aus den Daten echten Java-Quellcode und schreibt .java-Dateien
```

`Main` steuert diese Kette; `UmlLauncherApp` ist die grafische Oberfläche, die das
fertige JAR als externen Prozess startet.

**Warum diese Trennung?** Jede Stufe ist einzeln testbar. „Daten lesen", „Daten parsen"
und „Code erzeugen" hängen nicht voneinander ab — man kann den Parser testen, ohne
eine Datei zu lesen, und den Generator testen, ohne zu parsen.

---

## 1. `Main` — Einstiegspunkt & Pipeline-Steuerung

**Paket:** `progProjekt` · **Datei:** `ADV-main/Main.java`

Die Steuerklasse. Sie ist eine *Utility-Klasse* (privater Konstruktor → nicht
instanziierbar); der Einstieg erfolgt nur über `main`.

### `private Main()`
Privater Konstruktor. Verhindert, dass jemand `new Main()` aufruft. Das ist die
übliche Konvention für Klassen, die nur statische Methoden bündeln.

### `public static void main(String[] args)`
Das Herzstück. Ablauf Schritt für Schritt:

1. **Pfade ermitteln.** Werden **zwei Argumente** übergeben (`args[0]`, `args[1]`),
   läuft das Programm *nicht-interaktiv* — so startet die GUI das JAR.
   Andernfalls werden Eingabepfad und Ausgabeordner *interaktiv* über die Tastatur
   (`Scanner` auf `System.in`) abgefragt.
2. **Lesen.** Ein `UMLReader` wird mit dem Eingabepfad erzeugt; `readPanelAttributes()`
   liefert eine Liste roher Blöcke (ein Eintrag pro UML-Element).
3. **Parsen (Schleife über alle Blöcke).** Für jeden Block:
   - `parser.className(block)` ermittelt den Namen. Ist er leer (z. B. ein
     Beziehungspfeil ohne Namen), wird der Block mit `continue` **übersprungen**.
   - `parser.classKind(block)` bestimmt die Art (Klasse / abstrakt / Interface / Enum).
   - Ein neues `DataSave`-Objekt wird angelegt und befüllt: alle Attribute, alle
     Methoden, und — **nur falls es ein Enum ist** — zusätzlich die Enum-Konstanten.
   - Das gefüllte `DataSave` wandert in die Liste `klassen`.
4. **Erzeugen.** Ein `ClassGenerator` mit dem Ausgabeordner wird erzeugt;
   `generateJavaFiles(klassen)` schreibt pro Klasse eine `.java`-Datei.
5. **Rückmeldung.** Es wird ausgegeben, wie viele Klassen erkannt und wohin sie
   geschrieben wurden.

**Fehlerbehandlung:** Der gesamte Ablauf steht in einem `try/catch`. Jede Exception
wird abgefangen, eine Meldung „Fehler: …" ausgegeben und der Stacktrace gedruckt —
das Programm stürzt also nicht hart ab.

---

## 2. `UMLReader` — XML-Leser für `.uxf`-Dateien

**Paket:** `progProjekt` · **Datei:** `ADV-main/UMLReader.java`

Eine `.uxf`-Datei ist eine XML-Datei. Jedes UML-Element (Klasse, Interface, Enum,
Beziehung) steckt darin in einem `<panel_attributes>`-Tag als reiner Text. Diese
Klasse holt genau diese Texte heraus.

### Feld
- `private File inputFile` — die zu lesende `.uxf`-Datei.

### `public UMLReader(String file)`
Speichert den übergebenen Pfad als `File`-Objekt. Liest noch nichts.

### `public ArrayList<String> readPanelAttributes()`
Der eigentliche Lesevorgang:

1. Die Datei wird **als `InputStream`** geöffnet (`Files.newInputStream`). Das ist der
   entscheidende Punkt für **UTF-8**: Öffnet man die Datei als Stream, liest der
   XML-Parser die Kodierungs-Deklaration aus dem XML-Header (`encoding="UTF-8"`)
   selbst aus. So bleiben Umlaute (ä, ö, ü) in Klassen- und Membernamen korrekt.
   (Würde man stattdessen einen `Reader` mit falscher Kodierung übergeben, wären die
   Umlaute kaputt.)
2. Ein `DocumentBuilder` parst den Stream zu einem DOM-`Document`.
3. `getElementsByTagName("panel_attributes")` liefert **alle** Panel-Tags.
4. Eine Schleife geht über die Liste und sammelt von jedem den `getTextContent()`
   (den reinen Textinhalt) in eine `ArrayList<String>`.

**Fehlerfall:** Schlägt das Lesen/Parsen fehl, wird der Stacktrace gedruckt und eine
(ggf. leere) Liste zurückgegeben — der Aufrufer bekommt nie `null`.

---

## 3. `UMLParser` — Zerlegung der Panel-Blöcke

**Paket:** `progProjekt` · **Datei:** `ADV-main/UMLParser.java`

Die „intelligente" Klasse. Sie versteht die UMLet-Textnotation und zerlegt einen
Rohblock in seine Bestandteile. Sie ist **zustandslos** — jede Methode arbeitet nur
auf dem übergebenen Block-String.

> **UMLet-Blockaufbau (wichtig fürs Verständnis):** Ein Block besteht aus einem
> *Kopfbereich* (Klassenname, Stereotyp), dann einem Trenner `--`, dann *Attributen*,
> optional noch einem `--` und *Methoden*. Beispiel:
> ```
> «enum»
> Wochentag
> --        ← erster Trenner
> MONTAG       (bei Enums: Konstanten)
> --        ← zweiter Trenner
> - aufgabe: String
> + Wochentag(aufgabe: String)
> ```

### Öffentliche Methoden

#### `public UMLParser()`
Leerer Konstruktor (kein Zustand). Existiert nur, um dokumentiert zu sein.

#### `public String className(String block)`
Findet den Klassennamen. Geht die Zeilen von oben durch und nimmt die **erste
„echte" Kopfzeile**. Dabei wird der Reihe nach gefiltert:
- leere Zeilen überspringen;
- bei einem Trenner (`--`) abbrechen (Kopfbereich ist zu Ende);
- UMLet-Styling-Zeilen (`bg=yellow` …) überspringen;
- Unterstreichungs-Formatierung `_text_` entfernen;
- `{abstract}`-artige Zeilen überspringen;
- Stereotype `<<…>>` / `«…»` entfernen;
- Zeilen, die mit einem Sichtbarkeitszeichen (`+ - # ~`) beginnen, oder die `:` bzw.
  `(` enthalten, sind **keine** Namen (das sind Attribute/Methoden) → überspringen;
- bei Schrägstrich-Notation `/Name/` (abstrakte Klasse) die Schrägstriche abstreifen.

Die übrig bleibende Zeile wird durch `sanitizeIdentifier` zu einem gültigen
Java-Bezeichner gemacht und zurückgegeben. Findet sich nichts, kommt `""` zurück.

#### `public DataSave.ClassKind classKind(String block)`
Bestimmt die **Art**. Geht ebenfalls die Kopfzeilen durch und prüft in dieser
Reihenfolge:
1. Interface-Stereotyp → `INTERFACE`
2. Enum-Stereotyp → `ENUM`
3. Zeile `{abstract}` → `ABSTRACT_CLASS`
4. `/Name/`-Notation → `ABSTRACT_CLASS`

Trifft nichts zu, ist es eine normale `CLASS` (Default).

#### `public List<String> attribute(String block)`
Sammelt die **Attributzeilen**. Es wird erst gelesen, **nachdem** der erste Trenner
`--` passiert wurde (`passedHeader`). Eine Zeile gilt als Attribut, wenn sie:
- **kein** `(` enthält (sonst wäre es eine Methode) **und** ein `:` enthält
  (Typ-Trennzeichen `name: Typ`).

Zusätzlich: führendes `/` (abgeleitetes Attribut) und `{readonly}`-artige Modifier am
Ende werden entfernt. Fehlt ein Sichtbarkeitszeichen, wird `- ` (private) als Default
vorangestellt. Ergebnis sind normalisierte Zeilen wie `- inputFile: File`.

#### `public List<String> methode(String block)`
Sammelt die **Methodenzeilen**. Erkennungsmerkmal: die Zeile enthält **`(` und `)`**.
Auch hier wird erst nach dem ersten `--` gelesen. Führende/abschließende Schrägstriche
(abgeleitete Methode) und `{abstract}`-Modifier werden entfernt. Fehlt das
Sichtbarkeitszeichen, wird `+ ` (public) als Default gesetzt.

#### `public List<String> enumConstants(String block)`
Sammelt die **Enum-Konstanten**. Diese stehen im Abschnitt **zwischen dem ersten und
dem zweiten `--`**. Die Methode merkt sich mit `inConstants`, ob sie bereits im
Konstanten-Abschnitt ist: erster `--` schaltet ihn ein, zweiter `--` bricht ab.
Eine Konstante ist eine Zeile **ohne `:` und ohne `(`**. Ein optionales führendes
Sichtbarkeitszeichen wird abgeschnitten, der Rest durch `sanitizeIdentifier`
bereinigt — so wird aus `- LIGHT` sauber `LIGHT`.

### Private Hilfsmethoden (das „Wie" im Detail)

- **`isSeparator(line)`** — erkennt Trenner: `--`, `----`, `====`, oder die Wörter
  `attributes` / `methods`.
- **`isUmletProperty(line)`** — erkennt UMLet-Styling per Regex `^[A-Za-z_]\w*=.*`,
  also „Schlüssel=Wert"-Zeilen wie `bg=yellow`, `lt=<<-`. Diese enthalten keine
  UML-Daten und werden überall ignoriert. *(Das verhinderte früher Junk-Klassen mit
  `=` im Namen.)*
- **`removeStereotype(line)`** — schneidet `<<…>>` (ASCII-Doppelpfeile) **oder**
  `«…»` (Guillemets) aus der Zeile heraus.
- **`stripFormatting(line)`** — entfernt umschließende Unterstriche `_text_`
  (UMLet-Notation für „statisch").
- **`stripBraceModifiers(line)`** — schneidet alles ab dem ersten `{` weg, entfernt
  also `{abstract}`, `{readonly}` usw.
- **`isInterfaceStereotype` / `isEnumStereotype`** — vergleichen die Zeile (klein
  geschrieben) gegen die erlaubten Stereotyp-Schreibweisen. Enum akzeptiert
  `<<enum>>`, `«enum»`, `<<enumeration>>`, `<<enumerations>>` und die Guillemet-
  Varianten.
- **`sanitizeIdentifier(name)`** — die Identifier-Bereinigung. Geht Zeichen für
  Zeichen durch: das **erste** Zeichen muss `Character.isJavaIdentifierStart`
  erfüllen, alle weiteren `Character.isJavaIdentifierPart`. Erlaubt sind damit
  Unicode-Buchstaben (inkl. Umlaute), Ziffern, `_`, `$`. Ungültige Zeichen (z. B.
  `-`) werden **weggelassen**. So wird aus `Online-Fortbildung` → `OnlineFortbildung`.
- **`isVisibility(c)`** — `true` für `+`, `-`, `#`, `~`.

---

## 4. `DataSave` — Datencontainer je Klasse

**Paket:** `progProjekt` · **Datei:** `ADV-main/DataSave.java`

Ein reiner Datenbehälter („POJO") für **genau eine** im Diagramm gefundene Klasse.
Enthält keine Logik — er hält nur die vom Parser gelieferten **rohen Strings**, bis
der Generator sie in Java-Code übersetzt.

### Innerer Enum `ClassKind`
Die vier möglichen Arten: `CLASS`, `ABSTRACT_CLASS`, `INTERFACE`, `ENUM`.

### Felder
- `String className` — der Name.
- `ClassKind kind` — die Art (Default `CLASS`).
- `List<String> attribute` — rohe Attributzeilen, z. B. `- inputFile: File`.
- `List<String> methode` — rohe Methodenzeilen.
- `List<String> enumConstants` — Enum-Konstantennamen.

### Methoden
- **`DataSave(String className)`** — legt das Objekt an und initialisiert die drei
  Listen leer.
- **`getClassName()`**, **`getKind()` / `setKind(kind)`** — Name lesen, Art lesen/setzen.
- **`addAttribute(attr)` / `addMethod(method) `/ `addEnumConstant(constant)`** —
  hängen je einen Roh-Eintrag an die passende Liste an.
- **`getAttributes()` / `getMethods()` / `getEnumConstants()`** — geben die Listen
  zurück (vom Generator gelesen).

Die Trennung lohnt sich, weil der Generator dadurch nur mit einem klar definierten
Datenobjekt arbeitet, statt erneut Text parsen zu müssen.

---

## 5. `ClassGenerator` — Java-Quellcode-Erzeugung

**Paket:** `progProjekt` · **Datei:** `ADV-main/ClassGenerator.java`

Die Gegenrichtung zum Parser: aus den `DataSave`-Daten wird echter Java-Quelltext
zusammengebaut und in Dateien geschrieben.

### Feld
- `private String outputFolder` — Zielordner für die `.java`-Dateien.

### `public ClassGenerator(String targetFolder)`
Merkt sich den Ausgabeordner.

### `public void generateJavaFiles(List<DataSave> klassen)`
Die Hauptmethode. Für **jede** Klasse:
1. Leere Klassennamen überspringen.
2. Einen `StringBuilder` mit Standard-Importen anlegen
   (`import java.util.*;` + `import java.io.*;`).
3. Je nach `getKind()` verzweigen: `INTERFACE` → `buildInterface`, `ENUM` →
   `buildEnum`, sonst (`CLASS`/`ABSTRACT_CLASS`) → `buildKlasse`.
4. Die Datei unter `<outputFolder>/<ClassName>.java` schreiben — **explizit als
   `StandardCharsets.UTF_8`** (`Files.writeString`), damit Umlaute erhalten bleiben.
   Fehlende Zielordner werden vorher per `createDirectories` angelegt.

### `private String buildKlasse(DataSave klasse)` — normale/abstrakte Klasse
1. Vorab alle bereits **explizit** definierten Methodennamen sammeln
   (`collectMethodNames`), um später keine doppelten Getter/Setter zu erzeugen.
2. Deklaration: `public class` bzw. `public abstract class` (je nach `ABSTRACT_CLASS`).
3. **Felder** aus den Attributen (`buildFeld`).
4. **Getter/Setter** für jedes Feld (`buildGetterSetter`).
5. **Methoden** — mit **Duplikat-Erkennung**: ein `HashSet` sammelt die Signaturen
   (`methodSignatur`); ist eine Signatur schon enthalten, wird die Methode
   **übersprungen** (`continue`). So entsteht aus zwei identischen `add(int,int)`
   nur **eine** Methode (sonst wäre der Java-Code nicht kompilierbar).
6. Schließende `}`.

### `private String buildInterface(DataSave klasse)`
Erzeugt `public interface X { … }`. Pro Methode wird nur die **Signatur** ausgegeben
(`buildInterfaceSignatur`) — **kein Rumpf, kein Sichtbarkeitsmodifier, keine Felder,
kein Konstruktor**. Methoden, deren Name dem Klassennamen entspricht (also
Konstruktoren), werden im Interface übersprungen.

### `private String buildEnum(DataSave klasse)`
Erzeugt `public enum X { … }`:
1. **Konstantenliste.** Hat das Enum einen **parametrisierten Konstruktor**, liefert
   `buildEnumCtorArgs` die Default-Argumente (z. B. `null` für `String`); jede
   Konstante bekommt sie angehängt → `MONTAG(null), DIENSTAG(null);`. Ohne Parameter:
   `LIGHT, HEATER, CAMERA;`.
2. Felder + Getter/Setter wie bei Klassen.
3. Methoden mit Duplikat-Erkennung; der Konstruktor wird **`private`** ausgegeben
   (`buildMethode(..., forEnum=true)`), wie es für Enums vorgeschrieben ist.

### `private String buildInterfaceSignatur(String method)`
Baut eine einzelne Interface-Zeile: schneidet das Sichtbarkeitszeichen ab, liest
Name + Parameter (zwischen `(` und letztem `)`) und — falls nach `)` ein `:` folgt —
den Rückgabetyp (sonst `void`). Ergebnis: `void fahre();`.

### `private String buildFeld(String attr)`
Aus `- inputFile: File` wird `    private File inputFile;`. Das erste Zeichen wird
über `mapSichtbarkeit` zum Java-Keyword; `nameVon`/`typVon` zerlegen den Rest.

### `private String buildGetterSetter(String attr, Set<String> vorhandeneMethoden)`
Erzeugt **public** Getter und Setter — aber nur, wenn nicht schon eine gleichnamige
Methode (`getX`/`setX`) explizit im Diagramm steht (verhindert Doppeldefinitionen).
`grossAnfang` macht aus `aufgabe` → `getAufgabe`/`setAufgabe`.

### `private String buildMethode(String method, String className, boolean forEnum)`
Der allgemeine Methoden-/Konstruktor-Generator:
- Sichtbarkeit aus dem ersten Zeichen.
- Name + Parameter zerlegen; `buildParameter` dreht `name: Typ` zu `Typ name`.
- **Konstruktor-Erkennung:** Name == Klassenname → Konstruktor (kein Rückgabetyp).
  In Enums wird er auf `private` gezwungen.
- Sonst: Rückgabetyp nach dem `:` (Default `void`).
- Rumpf: immer `// TODO: Implementierung ergaenzen`; bei nicht-`void`-Methoden zusätzlich
  ein `return <Default>;` (über `defaultRueckgabe`).

### Weitere private Helfer
- **`buildParameter(paramRoh)`** — wandelt `path: String, n: int` → `String path, int n`.
  Nutzt `splitOberste`, damit Kommata **innerhalb von Generics** (`Map<String,Integer>`)
  nicht fälschlich als Parametertrenner gelten. Teile ohne `:` werden übersprungen.
- **`bereinigteRueckgabe(raw)`** — entfernt Default-Werte (nach `=`) und `{…}`-Modifier.
- **`mapSichtbarkeit(c)`** — `+`→`public`, `-`→`private`, `#`→`protected`, `~`→`""`.
- **`methodenName(method)`** — reiner Methodenname (vor dem `(`).
- **`collectMethodNames(methods)`** — Menge aller Methodennamen (für die
  Getter/Setter-Dedup-Prüfung).
- **`nameVon(paar)` / `typVon(paar)`** — splitten `name: Typ`; `typVon` entfernt zudem
  Default-Werte und `{…}`, Fallback ist `Object`.
- **`defaultRueckgabe(typ)`** — passender Default-Wert für `return`: `0` für ganze
  Zahlen, `0.0` für Gleitkomma, `false` für `boolean`, `' '` für `char`, sonst
  `null`.
- **`grossAnfang(wort)`** — erster Buchstabe groß (für `getX`/`setX`).
- **`methodSignatur(method)`** — baut die **Vergleichssignatur** `name(Typ1,Typ2)` für
  die Duplikat-Erkennung. **Nur die Parametertypen** zählen (keine Namen), weil Java
  Overloading nicht nach Parameternamen unterscheidet.
- **`buildEnumCtorArgs(klasse)`** — sucht den Enum-Konstruktor und liefert für seine
  Parameter die Default-Argumente (kommasepariert), z. B. `null` für einen
  `String`-Parameter. Ohne Parameter: `""`.
- **`splitOberste(text, trenner)`** — splittet am Trennzeichen, **zählt aber `<`/`>`
  mit** und trennt nur auf Tiefe 0 — so bleiben Generics als ein Stück erhalten.

---

## 6. `UmlLauncherApp` — JavaFX-GUI

**Paket:** (Standardpaket) · **Datei:** `app/src/UmlLauncherApp.java`

Die grafische Oberfläche im Windows-XP/„Luna"-Stil. Sie konvertiert **nicht selbst**,
sondern startet das `UmlToJava-new.jar` als **separaten Prozess** und zeigt dessen
Ausgabe an. Erbt von `javafx.application.Application`.

### Wichtige Felder
- `selectedFile` — die aktuell gewählte `.uxf`-Datei.
- Diverse `Label`/`TextArea`/`Button`-Referenzen für die Oberfläche.
- Eine Reihe `XP_…`-Farbkonstanten für das XP-Aussehen.

### Methoden (Aufbau der Oberfläche)
- **`start(Stage)`** — JavaFX-Einstieg. Baut das Fenster aus vier Bereichen:
  Titelleiste, Menüleiste, Inhalt, Statusleiste; Größe 520×500, nicht skalierbar.
- **`buildTitleBar(stage)`** — die blaue XP-Titelleiste mit Icon, Titel sowie
  Minimieren-/Schließen-Button. Macht das (randlose) Fenster über
  Maus-Press/Drag **verschiebbar**.
- **`buildTitleButton(...)`** — kleiner Titelleisten-Button mit Hover-Farbwechsel.
- **`buildMenuBar()`** — die Menüpunkte „Datei / Optionen / Hilfe" (optisch, mit
  Hover-Highlight).
- **`buildContent()`** — fasst Drop-Bereich, Button-Reihe und Log-Bereich zusammen.
- **`buildDropGroup()`** — die **Drag-&-Drop-Zone**. Kernlogik:
  - `setOnDragOver` — akzeptiert nur, wenn Dateien gezogen werden, und färbt die Zone.
  - `setOnDragDropped` — nimmt die **erste** Datei; endet sie auf `.uxf`, wird sie
    übernommen (`applyFile`), sonst Fehlerstatus.
  - `setOnMouseClicked` — öffnet alternativ einen `FileChooser` (Filter `*.uxf`).
- **`setDropZoneStyle(dragging, hasFile)`** — schaltet das Aussehen der Zone um
  (neutral / „Datei drüber" / „Datei geladen").
- **`applyFile(f)`** — merkt sich die Datei, zeigt Name + Pfad an, aktiviert „Start".
- **`buildButtonRow()`** — „Zurücksetzen" (leert Auswahl + Log) und „▶ Start"
  (ruft `runConversion`).
- **`buildXpButton(text, primary)`** — XP-Button mit Hover-Schatten.
- **`buildLogGroup()`** — die schwarze Log-`TextArea` (Konsolen-Optik).
- **`buildStatusBar()`** / **`buildGroupBox(title)`** — Statuszeile bzw. umrahmter
  Gruppenkasten (Hilfsbausteine fürs Layout).
- **`setStatus(msg, error)` / `log(msg)`** — schreiben Status bzw. Logzeile, jeweils
  über `Platform.runLater` (GUI-Updates müssen im JavaFX-Thread laufen).

### Die Konvertierungs-Logik
- **`runConversion()`** — der eigentliche Start:
  1. Ausgabeordnernamen bauen: `<uxf-Name>_<Zeitstempel yyyyMMdd_HHmmss>`, neben der
     `.uxf`-Datei.
  2. Das JAR über `findJar` suchen; wird keins gefunden → Fehlermeldung.
  3. Einen **`Task<Void>`** (Hintergrund-Thread) anlegen, damit die Oberfläche nicht
     einfriert. Darin: ein `ProcessBuilder` startet
     `java -jar <jar> <uxf> <ausgabe>`. `redirectErrorStream(true)` mischt
     Fehler- und Standardausgabe; jede gelesene Zeile wird live ins Log geschrieben.
  4. Nach `waitFor()`: Exit-Code 0 **und** Ausgabeordner vorhanden → Erfolg samt
     Anzahl `.java`-Dateien; Exit ≠ 0 → Fehler; sonst Warnung.
  5. Der Thread läuft als **Daemon**, blockiert also das Programmende nicht.
- **`findJar(dir)`** — sucht `UmlToJava-new.jar` (oder `UmlToJava.jar`) an mehreren
  plausiblen Orten: neben der `.uxf`-Datei, im `app/`-Unterordner, im
  Arbeitsverzeichnis und in den jeweiligen Elternordnern. Erstes Treffer-Ergebnis
  gewinnt.
- **`main(String[] args)`** — ruft `launch(args)` und startet damit die JavaFX-App.

---

## Bekannte Grenzen (bewusste Entscheidungen, keine Bugs)

- **Vererbung / `implements`:** Beziehungspfeile zwischen Klassen werden **nicht**
  ausgewertet — `extends`/`implements` muss man im erzeugten Code von Hand ergänzen.
  (Eine zuverlässige Zuordnung bräuchte eine koordinatenbasierte Analyse der Pfeile.)
- **`static`/`final`:** Die Unterstreichungs-Notation `_text_` wird gelesen, aber
  `static` wird nicht automatisch gesetzt.
- **Default-Werte:** Attribut-Standardwerte (`= 0`) werden aus dem Typ entfernt und
  nicht als Initialisierer übernommen.
- **Undefinierte Typen:** Eine einzelne erzeugte Klasse, die auf nicht im selben
  Diagramm definierte Typen verweist, kompiliert isoliert nicht — beim gemeinsamen
  Kompilieren aller erzeugten Dateien entfällt das Problem meist.
