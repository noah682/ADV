package progProjekt;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Einstiegspunkt des Programms. Steuert die Pipeline:
 *
 *   UMLReader      -> liest die .uxf und liefert rohe Klassenbloecke
 *   UMLParser      -> zerlegt jeden Block in Name, Attribute, Methoden
 *   DataSave       -> haelt die Daten je Klasse
 *   ClassGenerator -> schreibt jede Klasse in eine eigene .java-Datei
 */
public class Main {

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {

            // 1. Benutzer eingabe der Pfade (Datei & Ausgabe Ordner)
            System.out.print("Geben Sie bitte den Pfad der Datei ein: ");
            String eingabePfad = sc.nextLine();
            System.out.print("Geben Sie bitte den Pfad der Ausgabe Ordner ein: ");
            String ausgabeOrdner = sc.nextLine();


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
