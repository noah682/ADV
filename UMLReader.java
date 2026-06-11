package umlReader;

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
        //ArrayList für die Coordinaten erstellen

        try {
         
            // Lädt die XML-Datei und baut daraus eine Baumstruktur (DOM) im Arbeitsspeicher auf
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
            
            // Alle "panel_attributes" Tags holen und in die NodeList Packen
            NodeList panelAttributesList = doc.getElementsByTagName("panel_attributes");
            
            // Alle "coordinates" Tags holen und in die NodeList Packen
            //NodeList coordinatesList = doc.getElementsByTagName("coordinates");

            // Inhalt von "panel_attributes" in ArrayList packen
            for (int i = 0; i < panelAttributesList.getLength(); i++) {
                String inhalt = panelAttributesList.item(i).getTextContent();
                klassenInhalte.add(inhalt);
            } 
            
            // Die Inhalte der Coordinates in die ArrayList von Coordinates einfügen

		} catch(Exception e){
			e.printStackTrace();	
		}
		
        return klassenInhalte;
	}
	
}
