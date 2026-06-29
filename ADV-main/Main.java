package progProjekt;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Einstiegspunkt des Programms. Steuert die Pipeline:
 *
 * <ol>
 *   <li>{@link UMLReader} &ndash; liest die .uxf-Datei und liefert rohe Panel-Bloecke</li>
 *   <li>{@link UMLParser} &ndash; zerlegt jeden Block in Name, Art, Attribute, Methoden</li>
 *   <li>{@link DataSave} &ndash; haelt die Daten je Klasse</li>
 *   <li>{@link ClassGenerator} &ndash; schreibt jede Klasse in eine eigene .java-Datei</li>
 * </ol>
 *
 * <p>Der Aufruf ist sowohl interaktiv (Stdin) als auch nicht-interaktiv moeglich:
 * <pre>
 *   java -jar UmlToJava.jar &lt;eingabe.uxf&gt; &lt;ausgabeordner&gt;
 * </pre>
 */
public class Main {

    /**
     * Utility-Klasse &ndash; nicht instanziierbar. Der Einstieg erfolgt
     * ausschliesslich ueber {@link #main(String[])}.
     */
    private Main() {
    }

    /**
     * Startet den UML-zu-Java-Konverter.
     *
     * <p>Werden zwei Kommandozeilenargumente uebergeben, werden diese als
     * Eingabepfad und Ausgabeordner verwendet. Andernfalls werden die Pfade
     * interaktiv von der Standardeingabe gelesen.
     *
     * @param args optionale Argumente: {@code args[0]} = Eingabedatei,
     *             {@code args[1]} = Ausgabeordner
     */
    public static void main(String[] args) {
        try {
            String eingabePfad;
            String ausgabeOrdner;

            if (args.length >= 2) {
                // Nicht-interaktiver Modus (z. B. gestartet aus der GUI)
                eingabePfad  = args[0];
                ausgabeOrdner = args[1];
            } else {
                // Interaktiver Modus ueber Stdin
                Scanner sc = new Scanner(System.in);
                System.out.print("Geben Sie bitte den Pfad der Datei ein: ");
                eingabePfad = sc.nextLine();
                System.out.print("Geben Sie bitte den Pfad des Ausgabeordners ein: ");
                ausgabeOrdner = sc.nextLine();
                sc.close();
            }

            // 1. Datei einlesen und rohe Panel-Bloecke holen.
            UMLReader reader = new UMLReader(eingabePfad);
            ArrayList<String> bloecke = reader.readPanelAttributes();

            // 2. Jeden Block parsen und in ein DataSave-Objekt ueberfuehren.
            UMLParser parser = new UMLParser();
            List<DataSave> klassen = new ArrayList<>();

            for (String block : bloecke) {
                String name = parser.className(block);
                if (name.isEmpty()) {
                    continue; // Block ohne Namen (z. B. Relation-Elemente) ueberspringen
                }

                DataSave.ClassKind kind = parser.classKind(block);
                DataSave daten = new DataSave(name);
                daten.setKind(kind);

                for (String attr : parser.attribute(block)) {
                    daten.addAttribute(attr);
                }
                for (String meth : parser.methode(block)) {
                    daten.addMethod(meth);
                }
                if (kind == DataSave.ClassKind.ENUM) {
                    for (String c : parser.enumConstants(block)) {
                        daten.addEnumConstant(c);
                    }
                }

                klassen.add(daten);
            }

            // 3. Jede Klasse in ihre eigene .java-Datei schreiben.
            ClassGenerator generator = new ClassGenerator(ausgabeOrdner);
            generator.generateJavaFiles(klassen);

            System.out.println(klassen.size() + " Klasse(n) erkannt.");
            System.out.println("Geschrieben nach: " + ausgabeOrdner
                    + "/ (" + klassen.size() + " Datei(en))");

        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
