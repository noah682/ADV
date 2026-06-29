package progProjekt;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Liest eine UMLet-Datei (.uxf) ein und extrahiert die Inhalte aller
 * {@code panel_attributes}-Elemente als rohe Textbloecke.
 *
 * <p>Jeder Block entspricht einem UML-Element (Klasse, Interface, Enum oder
 * Relation) im Diagramm und wird spaeter vom {@link UMLParser} weiterverarbeitet.
 */
public class UMLReader {

    /** Die einzulesende .uxf-Quelldatei. */
    private File inputFile;

    /**
     * Erzeugt einen Reader fuer die angegebene UXF-Datei.
     *
     * @param file Pfad zur .uxf-Eingabedatei
     */
    public UMLReader(String file) {
        this.inputFile = new File(file);
    }

    /**
     * Liest alle {@code panel_attributes}-Inhalte aus der UXF-Datei und gibt
     * sie als Liste von Strings zurueck.
     *
     * <p>Die Datei wird explizit als {@link java.io.InputStream} geoeffnet,
     * damit der XML-Parser die im XML-Header deklarierte Kodierung (UTF-8)
     * korrekt auswertet. So werden Sonderzeichen und Umlaute in Klassen- und
     * Membernamen fehlerfrei eingelesen.
     *
     * @return Liste der rohen Panel-Inhalte; je ein Eintrag pro UML-Element
     */
    public ArrayList<String> readPanelAttributes() {

        ArrayList<String> klassenInhalte = new ArrayList<>();

        try (InputStream is = Files.newInputStream(inputFile.toPath())) {
            // XML-Parser liest Encoding-Deklaration aus dem Stream selbst.
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(is);

            NodeList panelAttributesList = doc.getElementsByTagName("panel_attributes");

            for (int i = 0; i < panelAttributesList.getLength(); i++) {
                String inhalt = panelAttributesList.item(i).getTextContent();
                klassenInhalte.add(inhalt);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return klassenInhalte;
    }
}
