package progProjekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Einstiegspunkt des Programms. Steuert die Pipeline:
 *
 *   UMLReader  -> liest die .uxf und liefert rohe Klassenbloecke
 *   UMLParser  -> zerlegt jeden Block in Name, Attribute, Methoden
 *   DataSave   -> haelt die Daten je Klasse
 *   ClassGenerator -> schreibt jede Klasse in eine eigene .java-Datei
 *
 * Aufruf:
 *   java Main <pfad-zur-uxf> [ausgabe-ordner]
 *
 * Beispiel:
 *   java Main testdata/Inf_Projekt_UML_to_Java.uxf output
 */
public class Main {

    /**
     * Liest Argumente, prueft sie und durchlaeuft die Pipeline.
     *
     * @param args args[0] = Pfad zur .uxf (Pflicht),
     *             args[1] = Ausgabeordner (optional, Standard "output")
     */
    public static void main(String[] args) {
        // 1. Argumente pruefen und Hilfetext anzeigen, wenn etwas fehlt.
        if (args.length < 1) {
            System.out.println("Aufruf: java Main <pfad-zur-uxf> [ausgabe-ordner]");
            return;
        }

        String eingabePfad = args[0];
        String ausgabeOrdner = (args.length >= 2) ? args[1] : "output";

        try {
            // 2. Datei einlesen und rohe Klassenbloecke holen.
            UMLReader reader = new UMLReader(eingabePfad);
            ArrayList<String> bloecke = reader.readPanelAttributes();

            // 3. Jeden Block parsen und in ein DataSave-Objekt ueberfuehren.
            UMLParser parser = new UMLParser();
            List<DataSave> klassen = new ArrayList<>();

            for (String block : bloecke) {
                String name = parser.className(block);
                if (name.isEmpty()) {
                    continue; // Block ohne Namen ueberspringen
                }

                DataSave daten = new DataSave(name);
                for (String attr : parser.attribute(block)) {
                    daten.addAttribute(attr);
                }
                for (String meth : parser.methode(block)) {
                    daten.addMethod(meth);
                }
                klassen.add(daten);
            }

            // 4. Jede Klasse in ihre eigene .java-Datei schreiben.
            ClassGenerator generator = new ClassGenerator(ausgabeOrdner);
            generator.generateJavaFiles(klassen);

            System.out.println(klassen.size() + " Klasse(n) erkannt.");
            System.out.println("Geschrieben nach: " + ausgabeOrdner + "/ (" + klassen.size() + " Datei(en))");

        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }
}
