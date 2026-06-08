import java.util.ArrayList;
import java.util.List;

/**
 * Reiner Datencontainer fuer EINE im UML-Diagramm gefundene Klasse.
 *
 * Lernidee: Wir trennen "Daten halten" (diese Klasse) strikt vom
 * "Daten parsen" (UMLParser) und "Code erzeugen" (ClassGenerator).
 * Jede dieser Aufgaben ist dadurch einzeln testbar und austauschbar.
 *
 * Pro Klasse im Diagramm wird genau ein DataSave-Objekt erzeugt.
 * Die Attribute und Methoden werden hier noch als rohe Strings gehalten
 * (z. B. "- inputFile: File"). Die Umwandlung in echten Java-Code
 * passiert erst spaeter im ClassGenerator.
 */
public class DataSave {

    /** Name der UML-Klasse, z. B. "UMLReader". Wird im Konstruktor gesetzt. */
    private String className;

    /** Rohe Attributzeilen aus dem Diagramm, z. B. "- inputFile: File". */
    private List<String> attribute;

    /** Rohe Methodenzeilen aus dem Diagramm, z. B. "+ main(args: String[]): void". */
    private List<String> methode;

    /**
     * Legt einen Container fuer eine konkrete Klasse an.
     * Die Listen starten leer und werden nach und nach befuellt.
     *
     * @param className Name der Klasse aus dem Diagramm
     */
    public DataSave(String className) {
        this.className = className;
        this.attribute = new ArrayList<>();
        this.methode = new ArrayList<>();
    }

    /**
     * Gibt den Klassennamen zurueck.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Haengt eine rohe Attributzeile an die Attributliste an.
     *
     * @param attr eine Zeile wie "- inputFile: File"
     */
    public void addAttribute(String attr) {
        attribute.add(attr);
    }

    /**
     * Haengt eine rohe Methodenzeile an die Methodenliste an.
     *
     * @param method eine Zeile wie "+ readPanelAttributes(): ArrayList<String>"
     */
    public void addMethod(String method) {
        methode.add(method);
    }

    /**
     * Gibt alle rohen Attributzeilen zurueck.
     */
    public List<String> getAttributes() {
        return attribute;
    }

    /**
     * Gibt alle rohen Methodenzeilen zurueck.
     */
    public List<String> getMethods() {
        return methode;
    }
}
