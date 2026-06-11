package progProjekt;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.ArrayList;

public class UMLReader {
	
	private File inputFile;

	public UMLReader(String file) {
		this.inputFile = new File(file);
	}

	public ArrayList<String> readPanelAttributes(){

        ArrayList<String> klassenInhalte = new ArrayList<>();

        try {
            // Laedt die XML-Datei und baut daraus eine Baumstruktur (DOM) im Speicher auf
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);

            // Alle "panel_attributes" Tags (ein Tag pro UML-Klasse) holen
            NodeList panelAttributesList = doc.getElementsByTagName("panel_attributes");

            // Textinhalt jedes Tags in die ArrayList packen
            for (int i = 0; i < panelAttributesList.getLength(); i++) {
                String inhalt = panelAttributesList.item(i).getTextContent();
                klassenInhalte.add(inhalt);
            }

		} catch(Exception e){
			e.printStackTrace();	
		}
		
        return klassenInhalte;
	}
	
}
