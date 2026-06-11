package progProjekt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Wandelt die geparsten Klassendaten (DataSave) in echten Java-Quellcode um
 * und schreibt jede Klasse in ihre eigene .java-Datei (Dateiname = Klassenname).
 *
 * Lernidee: Hier passiert die Uebersetzung von UML-Schreibweise
 * ("- name: Type") in Java-Schreibweise ("private Type name;"). Der Parser
 * davor liefert nur rohe Zeilen, der Generator interpretiert sie.
 */
public class ClassGenerator {

    /** Zielordner, in den die erzeugte .java-Datei geschrieben wird. */
    private String outputFolder;

    /**
     * @param targetFolder Ordner fuer die Ausgabedatei, z. B. "output"
     */
    public ClassGenerator(String targetFolder) {
        this.outputFolder = targetFolder;
    }

    /**
     * Erzeugt fuer jede Klasse eine eigene .java-Datei und schreibt sie.
     *
     * @param klassen alle im Diagramm gefundenen Klassen
     * @throws IOException falls das Schreiben fehlschlaegt
     */
    public void generateJavaFiles(List<DataSave> klassen) throws IOException {
        for (DataSave klasse : klassen) {
            StringBuilder datei = new StringBuilder();
            datei.append("import java.util.*;\n");
            datei.append("import java.io.*;\n\n");
            datei.append(buildKlasse(klasse));

            Path ziel = Paths.get(outputFolder, klasse.getClassName() + ".java");
            Files.createDirectories(ziel.getParent());
            Files.writeString(ziel, datei.toString());
        }
    }

    /**
     * Baut den kompletten Quelltext einer einzelnen Klasse als String.
     *
     * @param klasse die zu erzeugende Klasse
     * @return Java-Quelltext der Klasse
     */
    private String buildKlasse(DataSave klasse) {
        StringBuilder sb = new StringBuilder();

        // Namen aller explizit gezeichneten Methoden sammeln. Damit vermeiden wir
        // doppelte Methoden: Hat das Diagramm z. B. getClassName() bereits als
        // eigene Methode, generieren wir dafuer keinen zweiten Getter.
        java.util.Set<String> vorhandeneMethoden = new java.util.HashSet<>();
        for (String method : klasse.getMethods()) {
            vorhandeneMethoden.add(methodenName(method));
        }

        // Klassenrumpf oeffnen (public, da jede Klasse in einer eigenen Datei steht).
        sb.append("public class ").append(klasse.getClassName()).append(" {\n\n");

        // 1. Attribute als Felder.
        for (String attr : klasse.getAttributes()) {
            sb.append(buildFeld(attr));
        }
        if (!klasse.getAttributes().isEmpty()) {
            sb.append("\n");
        }

        // 2. Getter und Setter zu jedem Attribut.
        for (String attr : klasse.getAttributes()) {
            sb.append(buildGetterSetter(attr, vorhandeneMethoden));
        }

        // 3. Methodenruempfe.
        for (String method : klasse.getMethods()) {
            sb.append(buildMethode(method, klasse.getClassName()));
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Erzeugt aus einer Attributzeile ein Java-Feld.
     * Beispiel: "- inputFile: File" -> "    private File inputFile;"
     *
     * @param attr rohe Attributzeile
     * @return Java-Felddeklaration als fertige Zeile
     */
    private String buildFeld(String attr) {
        String sichtbarkeit = mapSichtbarkeit(attr.charAt(0));
        String rest = attr.substring(1).trim(); // ohne Sichtbarkeitszeichen
        String name = nameVon(rest);
        String typ = typVon(rest);

        // Sichtbarkeit kann leer sein (paketweit), dann kein doppeltes Leerzeichen.
        String prefix = sichtbarkeit.isEmpty() ? "" : sichtbarkeit + " ";
        return "    " + prefix + typ + " " + name + ";\n";
    }

    /**
     * Erzeugt zu einem Attribut einen Getter und einen Setter (immer public).
     *
     * @param attr rohe Attributzeile
     * @param vorhandeneMethoden Namen bereits explizit gezeichneter Methoden
     * @return Getter und Setter als Java-Quelltext
     */
    private String buildGetterSetter(String attr, java.util.Set<String> vorhandeneMethoden) {
        String rest = attr.substring(1).trim();
        String name = nameVon(rest);
        String typ = typVon(rest);
        String gross = grossAnfang(name); // fuer getName / setName

        StringBuilder sb = new StringBuilder();

        // Getter nur erzeugen, wenn es keine gleichnamige explizite Methode gibt.
        if (!vorhandeneMethoden.contains("get" + gross)) {
            sb.append("    public ").append(typ).append(" get").append(gross).append("() {\n");
            sb.append("        return ").append(name).append(";\n");
            sb.append("    }\n\n");
        }

        // Setter ebenso nur erzeugen, wenn nicht bereits vorhanden.
        if (vorhandeneMethoden.contains("set" + gross)) {
            return sb.toString();
        }

        sb.append("    public void set").append(gross).append("(").append(typ).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
        sb.append("    }\n\n");

        return sb.toString();
    }

    /**
     * Erzeugt aus einer Methodenzeile einen Methodenrumpf.
     * Stimmt der Name mit className ueberein, wird ein Konstruktor erzeugt.
     *
     * @param method    rohe Methodenzeile
     * @param className Name der umgebenden Klasse (zur Konstruktor-Erkennung)
     * @return Methodenrumpf als Java-Quelltext
     */
    private String buildMethode(String method, String className) {
        String sichtbarkeit = mapSichtbarkeit(method.charAt(0));
        String rest = method.substring(1).trim();

        int klammerAuf = rest.indexOf('(');
        int klammerZu = rest.indexOf(')');
        if (klammerAuf < 0 || klammerZu < 0) {
            return ""; // unverstaendliche Zeile sicherheitshalber ueberspringen
        }

        String name = rest.substring(0, klammerAuf).trim();
        String paramRoh = rest.substring(klammerAuf + 1, klammerZu).trim();
        String params = buildParameter(paramRoh);

        // Rueckgabetyp steht hinter ")" nach einem ":". Fehlt er, ist es ein Konstruktor.
        boolean istKonstruktor = name.equals(className);
        String rueckgabe = "";
        int doppelpunkt = rest.indexOf(':', klammerZu);
        if (doppelpunkt >= 0) {
            rueckgabe = rest.substring(doppelpunkt + 1).trim();
        }

        StringBuilder sb = new StringBuilder();
        String prefix = sichtbarkeit.isEmpty() ? "" : sichtbarkeit + " ";

        if (istKonstruktor) {
            // Konstruktoren haben keinen Rueckgabetyp.
            sb.append("    ").append(prefix).append(name).append("(").append(params).append(") {\n");
            sb.append("        // TODO: Implementierung ergaenzen\n");
            sb.append("    }\n\n");
        } else {
            if (rueckgabe.isEmpty()) {
                rueckgabe = "void"; // Standard, falls im Diagramm kein Typ angegeben ist
            }
            sb.append("    ").append(prefix).append(rueckgabe).append(" ").append(name)
              .append("(").append(params).append(") {\n");
            sb.append("        // TODO: Implementierung ergaenzen\n");
            if (!rueckgabe.equals("void")) {
                sb.append("        return ").append(defaultRueckgabe(rueckgabe)).append(";\n");
            }
            sb.append("    }\n\n");
        }

        return sb.toString();
    }

    /**
     * Wandelt die UML-Parameterliste in Java-Parameter um.
     * Beispiel: "path: String, n: int" wird zu "String path, int n".
     *
     * @param paramRoh Parameterteil zwischen den Klammern (kann leer sein)
     * @return Java-Parameterliste
     */
    private String buildParameter(String paramRoh) {
        if (paramRoh.isEmpty()) {
            return "";
        }

        List<String> teile = splitOberste(paramRoh, ',');
        StringBuilder sb = new StringBuilder();
        for (String roh : teile) {
            String p = roh.trim();

            // Nur echte Parameter im Format "name: Type" uebernehmen. Informelle
            // Platzhalter im Diagramm wie "..." enthalten keinen Doppelpunkt und
            // wuerden ungueltigen Java-Code erzeugen, daher ueberspringen wir sie.
            if (!p.contains(":")) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(typVon(p)).append(" ").append(nameVon(p));
        }
        return sb.toString();
    }

    /**
     * Uebersetzt das UML-Sichtbarkeitszeichen in das Java-Schluesselwort.
     *
     * @param zeichen +, -, # oder ~
     * @return "public", "private", "protected" oder "" (paketweit)
     */
    private String mapSichtbarkeit(char zeichen) {
        switch (zeichen) {
            case '+': return "public";
            case '-': return "private";
            case '#': return "protected";
            case '~': return ""; // paketweit sichtbar, in Java kein Schluesselwort
            default:  return ""; // unbekannt: ohne Modifikator behandeln
        }
    }

    /**
     * Liefert den reinen Methodennamen aus einer rohen Methodenzeile.
     * Beispiel: "+ getClassName(): String" liefert "getClassName".
     *
     * @param method rohe Methodenzeile
     * @return der Methodenname (ohne Sichtbarkeitszeichen und Klammern)
     */
    private String methodenName(String method) {
        String rest = method.substring(1).trim(); // Sichtbarkeitszeichen entfernen
        int klammer = rest.indexOf('(');
        if (klammer < 0) {
            return rest;
        }
        return rest.substring(0, klammer).trim();
    }

    /**
     * Liefert den Namensteil aus "name: Type".
     *
     * @param paar Zeichenkette der Form "name: Type"
     * @return der Name (alles vor dem ersten ":")
     */
    private String nameVon(String paar) {
        int dp = paar.indexOf(':');
        if (dp < 0) {
            return paar.trim();
        }
        return paar.substring(0, dp).trim();
    }

    /**
     * Liefert den Typteil aus "name: Type".
     *
     * @param paar Zeichenkette der Form "name: Type"
     * @return der Typ (alles nach dem ersten ":") oder "Object" als Notfall
     */
    private String typVon(String paar) {
        int dp = paar.indexOf(':');
        if (dp < 0) {
            return "Object";
        }
        return paar.substring(dp + 1).trim();
    }

    /**
     * Liefert einen passenden Default-Rueckgabewert fuer den return-Befehl im
     * generierten Methodenrumpf.
     *
     * @param typ der Rueckgabetyp
     * @return ein gueltiger Default-Wert als String
     */
    private String defaultRueckgabe(String typ) {
        switch (typ) {
            case "int":
            case "long":
            case "short":
            case "byte":   return "0";
            case "double":
            case "float":  return "0.0";
            case "boolean": return "false";
            case "char":   return "'\\u0000'";
            default:       return "null"; // Objekte, Arrays, Generics
        }
    }

    /**
     * Schreibt den ersten Buchstaben gross (fuer getX/setX).
     *
     * @param wort z. B. "inputFile"
     * @return z. B. "InputFile"
     */
    private String grossAnfang(String wort) {
        if (wort.isEmpty()) {
            return wort;
        }
        return Character.toUpperCase(wort.charAt(0)) + wort.substring(1);
    }

    /**
     * Trennt einen String am Trennzeichen, aber nur auf oberster Ebene.
     * Kommata innerhalb von Generics (zwischen < und >) werden ignoriert,
     * z. B. "a: Map<String, Integer>, b: int" -> ["a: Map<String, Integer>", " b: int"].
     *
     * @param text    der zu zerlegende String
     * @param trenner das Trennzeichen, hier ','
     * @return Liste der obersten Teilstuecke
     */
    private List<String> splitOberste(String text, char trenner) {
        List<String> teile = new java.util.ArrayList<>();
        int tiefe = 0;          // Schachtelungstiefe der spitzen Klammern
        StringBuilder aktuell = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') {
                tiefe++;
            } else if (c == '>') {
                tiefe--;
            }

            if (c == trenner && tiefe == 0) {
                teile.add(aktuell.toString());
                aktuell.setLength(0);
            } else {
                aktuell.append(c);
            }
        }
        teile.add(aktuell.toString());
        return teile;
    }
}
