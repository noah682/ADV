package progProjekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Reiner Datencontainer fuer EINE im UML-Diagramm gefundene Klasse.
 *
 * <p>Lernidee: "Daten halten" (diese Klasse), "Daten parsen" ({@link UMLParser})
 * und "Code erzeugen" ({@link ClassGenerator}) sind getrennt und so einzeln
 * testbar.
 *
 * <p>Attribute und Methoden werden hier noch als rohe Strings gehalten
 * (z.&nbsp;B. {@code "- inputFile: File"}); die Umwandlung in Java-Code passiert
 * erst im {@link ClassGenerator}.
 */
public class DataSave {

    /**
     * Art des UML-Typs, der aus dem Diagramm erkannt wurde.
     */
    public enum ClassKind {
        /** Normale Klasse ({@code public class}). */
        CLASS,
        /** Abstrakte Klasse ({@code public abstract class}). */
        ABSTRACT_CLASS,
        /** Interface ({@code public interface}). */
        INTERFACE,
        /** Aufzaehlung ({@code public enum}). */
        ENUM
    }

    /** Name der UML-Klasse, z.&nbsp;B. {@code "UMLReader"}. */
    private String className;

    /** Art des UML-Typs (Klasse, abstrakt, Interface, Enum). */
    private ClassKind kind = ClassKind.CLASS;

    /** Rohe Attributzeilen aus dem Diagramm, z.&nbsp;B. {@code "- inputFile: File"}. */
    private List<String> attribute;

    /** Rohe Methodenzeilen aus dem Diagramm, z.&nbsp;B. {@code "+ main(args: String[]): void"}. */
    private List<String> methode;

    /** Bezeichner der Enum-Konstanten, z.&nbsp;B. {@code "MONTAG"}. */
    private List<String> enumConstants;

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
        this.enumConstants = new ArrayList<>();
    }

    /**
     * Gibt den Klassennamen zurueck.
     *
     * @return Klassenname als String
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gibt die Art des UML-Typs zurueck.
     *
     * @return {@link ClassKind} des Typs
     */
    public ClassKind getKind() {
        return kind;
    }

    /**
     * Setzt die Art des UML-Typs.
     *
     * @param kind erkannte {@link ClassKind}
     */
    public void setKind(ClassKind kind) {
        this.kind = kind;
    }

    /**
     * Haengt eine rohe Attributzeile an die Attributliste an.
     *
     * @param attr eine Zeile wie {@code "- inputFile: File"}
     */
    public void addAttribute(String attr) {
        attribute.add(attr);
    }

    /**
     * Haengt eine rohe Methodenzeile an die Methodenliste an.
     *
     * @param method eine Zeile wie {@code "+ readPanelAttributes(): ArrayList<String>"}
     */
    public void addMethod(String method) {
        methode.add(method);
    }

    /**
     * Haengt den Bezeichner einer Enum-Konstante an.
     *
     * @param constant Konstantenname, z.&nbsp;B. {@code "MONTAG"}
     */
    public void addEnumConstant(String constant) {
        enumConstants.add(constant);
    }

    /**
     * Gibt alle rohen Attributzeilen zurueck.
     *
     * @return unveraenderliche Sicht auf die Attributliste
     */
    public List<String> getAttributes() {
        return attribute;
    }

    /**
     * Gibt alle rohen Methodenzeilen zurueck.
     *
     * @return unveraenderliche Sicht auf die Methodenliste
     */
    public List<String> getMethods() {
        return methode;
    }

    /**
     * Gibt alle Enum-Konstantennamen zurueck.
     *
     * @return unveraenderliche Sicht auf die Konstantenliste
     */
    public List<String> getEnumConstants() {
        return enumConstants;
    }
}
