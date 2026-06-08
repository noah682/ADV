import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Liest eine Umlet-Datei (.uxf) ein und holt aus ihr die rohen Textbloecke
 * der einzelnen UML-Klassen heraus.
 *
 * Lernidee: Eine .uxf-Datei ist in Wahrheit eine XML-Datei. Statt mit
 * Regex im Text herumzuraten, benutzen wir den eingebauten XML-Parser
 * von Java (DocumentBuilder). Das ist robust und dekodiert nebenbei
 * automatisch Sonderzeichen wie &lt; zu < (wichtig fuer Generics wie
 * List<String>).
 *
 * Aufbau einer .uxf (vereinfacht):
 *   <diagram>
 *     <element>
 *       <id>UMLClass</id>                         <- Typ des Elements
 *       <panel_attributes>...Klassentext...</panel_attributes>
 *     </element>
 *     <element><id>Relation</id>...</element>     <- Pfeile, ignorieren wir
 *   </diagram>
 */
public class UMLReader {

    /** Die einzulesende .uxf-Datei. */
    private File inputFile;

    /**
     * Merkt sich den Pfad zur einzulesenden Datei.
     *
     * @param path Pfad zur .uxf-Datei
     */
    public UMLReader(String path) {
        this.inputFile = new File(path);
    }

    /**
     * Liest die Datei als XML und gibt pro UML-Klasse einen rohen Textblock
     * (den Inhalt von panel_attributes) zurueck.
     *
     * Wichtig: Es werden nur Elemente mit <id>UMLClass</id> beruecksichtigt.
     * Pfeile (Relation) oder Notizen werden bewusst uebersprungen, weil sie
     * keine Klassen sind.
     *
     * @return Liste roher Klassenbloecke, jeweils ein String pro Klasse
     * @throws Exception falls die Datei fehlt oder kein gueltiges XML ist
     */
    public ArrayList<String> readPanelAttributes() throws Exception {
        ArrayList<String> bloecke = new ArrayList<>();

        // XML-Datei in einen durchsuchbaren Baum (Document) einlesen.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputFile);

        // Alle <element>-Knoten holen (jede Klasse, jeder Pfeil ist ein element).
        NodeList elemente = document.getElementsByTagName("element");

        for (int i = 0; i < elemente.getLength(); i++) {
            Node knoten = elemente.item(i);
            if (knoten.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) knoten;

            // Typ des Elements bestimmen (UMLClass, Relation, ...).
            String typ = ersterKindtext(element, "id");

            // Nur echte Klassen interessieren uns.
            if ("UMLClass".equals(typ)) {
                String inhalt = ersterKindtext(element, "panel_attributes");
                if (inhalt != null) {
                    bloecke.add(inhalt);
                }
            }
        }

        return bloecke;
    }

    /**
     * Hilfsfunktion: holt den Textinhalt des ersten Kind-Tags mit dem
     * gegebenen Namen innerhalb eines Elements.
     *
     * Beispiel: ersterKindtext(element, "id") liefert "UMLClass".
     *
     * @param element der zu durchsuchende XML-Knoten
     * @param tagName der gesuchte Tag-Name
     * @return der Textinhalt oder null, wenn das Tag nicht existiert
     */
    private String ersterKindtext(Element element, String tagName) {
        NodeList treffer = element.getElementsByTagName(tagName);
        if (treffer.getLength() == 0) {
            return null;
        }
        return treffer.item(0).getTextContent();
    }
}
