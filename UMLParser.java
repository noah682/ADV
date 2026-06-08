import java.util.ArrayList;
import java.util.List;

/**
 * Zerlegt einen rohen Klassenblock (wie ihn der UMLReader liefert) in seine
 * drei logischen Teile: Klassenname, Attribute und Methoden.
 *
 * Lernidee: Ein Umlet-Klassenblock ist durch Zeilen, die nur aus "--"
 * bestehen, in drei Abschnitte geteilt:
 *
 *   ClassName              <- Abschnitt 0: der Name
 *   --
 *   - attr: Type           <- Abschnitt 1: die Attribute
 *   --
 *   + method(p: T): R      <- Abschnitt 2: die Methoden
 *
 * Diese Klasse interpretiert die Zeilen NOCH NICHT als Java-Code. Sie liefert
 * nur saubere Listen roher Zeilen. Das eigentliche Uebersetzen in Java
 * passiert spaeter im ClassGenerator.
 */
public class UMLParser {

    /**
     * Liefert den Klassennamen aus einem Block.
     * Das ist die erste nicht-leere Zeile des ersten Abschnitts.
     *
     * @param xmlInhalt roher Klassenblock
     * @return der Klassenname, z. B. "UMLReader" (oder "" wenn keiner da ist)
     */
    public String className(String xmlInhalt) {
        List<List<String>> abschnitte = splitAbschnitte(xmlInhalt);
        if (abschnitte.isEmpty() || abschnitte.get(0).isEmpty()) {
            return "";
        }
        return abschnitte.get(0).get(0);
    }

    /**
     * Liefert die rohen Attributzeilen (Abschnitt 1) eines Blocks.
     *
     * @param xmlInhalt roher Klassenblock
     * @return Liste der Attributzeilen, leer falls keine vorhanden
     */
    public List<String> attribute(String xmlInhalt) {
        List<List<String>> abschnitte = splitAbschnitte(xmlInhalt);
        if (abschnitte.size() > 1) {
            return abschnitte.get(1);
        }
        return new ArrayList<>();
    }

    /**
     * Liefert die rohen Methodenzeilen (Abschnitt 2) eines Blocks.
     *
     * @param xmlInhalt roher Klassenblock
     * @return Liste der Methodenzeilen, leer falls keine vorhanden
     */
    public List<String> methode(String xmlInhalt) {
        List<List<String>> abschnitte = splitAbschnitte(xmlInhalt);
        if (abschnitte.size() > 2) {
            return abschnitte.get(2);
        }
        return new ArrayList<>();
    }

    /**
     * Hilfsfunktion: zerlegt den ganzen Block an den "--"-Trennzeilen in
     * Abschnitte. Jeder Abschnitt ist eine Liste seiner nicht-leeren,
     * getrimmten Zeilen.
     *
     * Beispiel: Aus "Main\n--\n\n--\n+ main(...)" wird
     *   [ ["Main"], [], ["+ main(...)"] ]
     *
     * @param block roher Klassenblock
     * @return Liste der Abschnitte
     */
    private List<List<String>> splitAbschnitte(String block) {
        List<List<String>> abschnitte = new ArrayList<>();
        List<String> aktuell = new ArrayList<>();

        if (block == null) {
            abschnitte.add(aktuell);
            return abschnitte;
        }

        // splitlines-aehnlich: an jedem Zeilenumbruch trennen.
        String[] zeilen = block.split("\\R");
        for (String roh : zeilen) {
            String zeile = roh.trim();

            if (zeile.equals("--")) {
                // Trennlinie: aktuellen Abschnitt abschliessen, neuen beginnen.
                abschnitte.add(aktuell);
                aktuell = new ArrayList<>();
            } else if (!zeile.isEmpty()) {
                // Echte Inhaltszeile dem aktuellen Abschnitt hinzufuegen.
                aktuell.add(zeile);
            }
            // Leere Zeilen werden bewusst ignoriert.
        }
        abschnitte.add(aktuell);

        return abschnitte;
    }
}
