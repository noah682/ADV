package progProjekt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Wandelt die geparsten Klassendaten ({@link DataSave}) in echten Java-Quellcode um
 * und schreibt jede Klasse in ihre eigene {@code .java}-Datei.
 *
 * <p>Alle Dateien werden explizit mit {@link StandardCharsets#UTF_8} geschrieben,
 * damit Umlaute und Sonderzeichen in Bezeichnern korrekt gespeichert werden.
 *
 * <p>Je nach {@link DataSave.ClassKind} wird unterschiedlicher Code erzeugt:
 * <ul>
 *   <li>{@code CLASS} / {@code ABSTRACT_CLASS}: normale oder abstrakte Klasse
 *       mit Feldern, Getter/Setter und Methoden.</li>
 *   <li>{@code INTERFACE}: nur Methodensignaturen ohne Sichtbarkeitsmodifier
 *       und ohne Rumpf.</li>
 *   <li>{@code ENUM}: Konstantenliste, Felder, privater Konstruktor,
 *       Getter/Setter.</li>
 * </ul>
 */
public class ClassGenerator {

    /** Zielordner, in den die erzeugten {@code .java}-Dateien geschrieben werden. */
    private String outputFolder;

    /**
     * Erzeugt einen Generator fuer den angegebenen Ausgabeordner.
     *
     * @param targetFolder Ordner fuer die Ausgabedateien, z.&nbsp;B. {@code "output"}
     */
    public ClassGenerator(String targetFolder) {
        this.outputFolder = targetFolder;
    }

    /**
     * Erzeugt fuer jede Klasse eine eigene {@code .java}-Datei und schreibt sie
     * mit UTF-8-Kodierung in den Ausgabeordner.
     *
     * @param klassen alle im Diagramm gefundenen Typen
     * @return Anzahl der geschriebenen Dateien (inkl. Platzhalter-Klassen)
     * @throws IOException falls das Schreiben einer Datei fehlschlaegt
     */
    public int generateJavaFiles(List<DataSave> klassen) throws IOException {
        int geschrieben = 0;
        for (DataSave klasse : klassen) {
            if (klasse.getClassName().isEmpty()) continue;

            StringBuilder datei = new StringBuilder();
            datei.append("import java.util.*;\n");
            datei.append("import java.io.*;\n\n");

            datei.append(switch (klasse.getKind()) {
                case INTERFACE -> buildInterface(klasse);
                case ENUM -> buildEnum(klasse);
                default -> buildKlasse(klasse); // CLASS und ABSTRACT_CLASS
            });

            Path ziel = Paths.get(outputFolder, klasse.getClassName() + ".java");
            Files.createDirectories(ziel.getParent());
            Files.writeString(ziel, datei.toString(), StandardCharsets.UTF_8);
            geschrieben++;
        }

        // Referenzierte, aber nirgends definierte Typen als leere Platzhalter
        // erzeugen, damit der generierte Code komplett kompilierbar ist.
        List<String> stubs = fehlendeTypen(klassen);
        for (String typ : stubs) {
            String inhalt = "/** Platzhalter: Typ war im Diagramm referenziert, aber nicht definiert. */\n"
                    + "public class " + typ + " {\n"
                    + "    // TODO: durch echte Implementierung ersetzen\n"
                    + "}\n";
            Path ziel = Paths.get(outputFolder, typ + ".java");
            Files.createDirectories(ziel.getParent());
            Files.writeString(ziel, inhalt, StandardCharsets.UTF_8);
            geschrieben++;
        }
        if (!stubs.isEmpty()) {
            System.out.println(stubs.size() + " Platzhalter-Klasse(n) erzeugt: "
                    + String.join(", ", stubs));
        }
        return geschrieben;
    }

    /**
     * Ermittelt alle Typen, die in Feldern, Parametern oder Rueckgabetypen
     * referenziert werden, aber weder im Diagramm definiert noch als
     * Standard-Java-Typ ({@code java.lang}, {@code java.util}, {@code java.io})
     * aufloesbar sind. Fuer diese Typen werden Platzhalter-Klassen erzeugt.
     *
     * @param klassen alle im Diagramm gefundenen Typen
     * @return alphabetisch sortierte Namen der fehlenden Typen
     */
    private List<String> fehlendeTypen(List<DataSave> klassen) {
        Set<String> definiert = new HashSet<>();
        for (DataSave k : klassen) {
            definiert.add(k.getClassName());
        }

        Set<String> referenziert = new TreeSet<>(); // sortiert -> stabile Ausgabe
        for (DataSave k : klassen) {
            for (String attr : k.getAttributes()) {
                sammleBasistypen(typVon(attr.substring(1).trim()), referenziert);
            }
            for (String method : k.getMethods()) {
                String rest = method.substring(1).trim();
                int ka = rest.indexOf('(');
                int kz = rest.lastIndexOf(')');
                if (ka < 0 || kz < 0) continue;
                for (String p : splitOberste(rest.substring(ka + 1, kz).trim(), ',')) {
                    if (p.contains(":")) sammleBasistypen(typVon(p.trim()), referenziert);
                }
                int dp = rest.indexOf(':', kz);
                if (dp >= 0) {
                    sammleBasistypen(bereinigteRueckgabe(rest.substring(dp + 1).trim()), referenziert);
                }
            }
        }

        List<String> fehlt = new ArrayList<>();
        for (String typ : referenziert) {
            if (definiert.contains(typ)) continue;
            if (istBekannterJavaTyp(typ)) continue;
            fehlt.add(typ);
        }
        return fehlt;
    }

    /**
     * Zerlegt eine Typangabe in ihre Basistypnamen und sammelt sie ein.
     * Generics und Arrays werden aufgeloest: {@code "Map<String, Room[]>"}
     * liefert {@code Map}, {@code String} und {@code Room}.
     *
     * @param typ  rohe Typangabe aus dem Diagramm
     * @param ziel Menge, in die die gefundenen Basistypen eingefuegt werden
     */
    private void sammleBasistypen(String typ, Set<String> ziel) {
        for (String teil : typ.split("[<>,\\[\\]]")) {
            String t = teil.trim();
            if (t.isEmpty()) continue;
            // Nur einfache Bezeichner (inkl. Unicode-Buchstaben wie Umlaute)
            if (!t.matches("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*")) continue;
            ziel.add(t);
        }
    }

    /**
     * Prueft ob ein Typname ohne eigene Definition kompilierbar ist: entweder
     * ein primitiver Typ / {@code void} oder eine Klasse aus {@code java.lang},
     * {@code java.util} oder {@code java.io} &ndash; exakt die Pakete, die der
     * generierte Code importiert bzw. implizit sieht.
     *
     * @param typ zu pruefender Typname, z.&nbsp;B. {@code "ArrayList"}
     * @return {@code true} wenn der Typ ohne Platzhalter aufloesbar ist
     */
    private boolean istBekannterJavaTyp(String typ) {
        switch (typ) {
            case "void": case "int": case "long": case "short": case "byte":
            case "double": case "float": case "boolean": case "char":
                return true;
            default:
                break;
        }
        for (String paket : new String[] {"java.lang.", "java.util.", "java.io."}) {
            try {
                Class.forName(paket + typ);
                return true;
            } catch (ClassNotFoundException e) {
                // in diesem Paket nicht vorhanden -> naechstes pruefen
            }
        }
        return false;
    }

    /**
     * Baut den Quelltext einer normalen oder abstrakten Klasse.
     *
     * @param klasse die zu generierende Klasse
     * @return Java-Quelltext der Klasse
     */
    private String buildKlasse(DataSave klasse) {
        StringBuilder sb = new StringBuilder();

        boolean isAbstract = klasse.getKind() == DataSave.ClassKind.ABSTRACT_CLASS;
        String decl = isAbstract ? "public abstract class " : "public class ";
        sb.append(decl).append(klasse.getClassName()).append(" {\n\n");

        appendFelderUndMethoden(sb, klasse, false);

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Haengt Felder, Getter/Setter und Methoden (mit Duplikat-Erkennung) an den
     * uebergebenen {@link StringBuilder} an. Gemeinsamer Rumpf fuer normale/abstrakte
     * Klassen ({@link #buildKlasse}) und Enums ({@link #buildEnum}).
     *
     * @param sb      Ziel-StringBuilder
     * @param klasse  die zu generierende Klasse
     * @param forEnum {@code true} wenn der Rumpf in einer Enum-Deklaration steht
     *                (Konstruktoren werden dann {@code private})
     */
    private void appendFelderUndMethoden(StringBuilder sb, DataSave klasse, boolean forEnum) {
        for (String attr : klasse.getAttributes()) {
            sb.append(buildFeld(attr));
        }
        if (!klasse.getAttributes().isEmpty()) sb.append("\n");

        Set<String> vorhandeneMethoden = collectMethodNames(klasse.getMethods());
        for (String attr : klasse.getAttributes()) {
            sb.append(buildGetterSetter(attr, vorhandeneMethoden));
        }

        // Duplikat-Signaturen erkennen und ueberspringen (z. B. zwei Methoden
        // mit gleichem Namen und gleichen Parametertypen).
        Set<String> generierteSignaturen = new HashSet<>();
        for (String method : klasse.getMethods()) {
            String sig = methodSignatur(method);
            if (!generierteSignaturen.add(sig)) continue; // Duplikat -> ueberspringen
            sb.append(buildMethode(method, klasse.getClassName(), forEnum));
        }
    }

    /**
     * Baut den Quelltext eines Interfaces: nur Methodensignaturen, kein Rumpf,
     * kein Sichtbarkeitsmodifier, keine Felder, kein Konstruktor.
     *
     * @param klasse die zu generierende Interface-Definition
     * @return Java-Quelltext des Interfaces
     */
    private String buildInterface(DataSave klasse) {
        StringBuilder sb = new StringBuilder();
        sb.append("public interface ").append(klasse.getClassName()).append(" {\n\n");

        for (String method : klasse.getMethods()) {
            // Konstruktoren (Name == Klassenname) im Interface ueberspringen
            if (methodenName(method).equals(klasse.getClassName())) continue;
            sb.append(buildInterfaceSignatur(method));
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Baut den Quelltext einer Enum-Deklaration: Konstantenliste, Felder,
     * privater Konstruktor und Getter/Setter.
     *
     * @param klasse die zu generierende Enum-Definition
     * @return Java-Quelltext der Enum-Deklaration
     */
    private String buildEnum(DataSave klasse) {
        StringBuilder sb = new StringBuilder();
        sb.append("public enum ").append(klasse.getClassName()).append(" {\n\n");

        // Konstantenliste – bei parametrisierten Konstruktoren Default-Argumente anhaengen
        List<String> constants = klasse.getEnumConstants();
        if (!constants.isEmpty()) {
            String ctorArgs = buildEnumCtorArgs(klasse);
            List<String> constDecls = new ArrayList<>();
            for (String c : constants) {
                constDecls.add(ctorArgs.isEmpty() ? c : c + "(" + ctorArgs + ")");
            }
            sb.append("    ").append(String.join(", ", constDecls)).append(";\n\n");
        }

        // Felder, Getter/Setter und Methoden; Konstruktor wird private (forEnum).
        appendFelderUndMethoden(sb, klasse, true);

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Erzeugt eine einzelne Interface-Methodensignatur mit Semikolon, ohne Rumpf
     * und ohne Sichtbarkeitsmodifier.
     *
     * @param method rohe Methodenzeile
     * @return Interface-Methodensignatur als Zeile
     */
    private String buildInterfaceSignatur(String method) {
        String rest = method.substring(1).trim(); // Sichtbarkeitszeichen entfernen

        int klammerAuf = rest.indexOf('(');
        int klammerZu  = rest.lastIndexOf(')');
        if (klammerAuf < 0 || klammerZu < 0) return "";

        String name     = rest.substring(0, klammerAuf).trim();
        String paramRoh = rest.substring(klammerAuf + 1, klammerZu).trim();
        String params   = buildParameter(paramRoh);

        String rueckgabe = "void";
        int doppelpunkt  = rest.indexOf(':', klammerZu);
        if (doppelpunkt >= 0) {
            rueckgabe = bereinigteRueckgabe(rest.substring(doppelpunkt + 1).trim());
        }

        return "    " + rueckgabe + " " + name + "(" + params + ");\n\n";
    }

    /**
     * Erzeugt aus einer Attributzeile ein Java-Feld.
     * Beispiel: {@code "- inputFile: File"} &rarr; {@code "    private File inputFile;"}.
     *
     * @param attr rohe Attributzeile
     * @return Java-Felddeklaration
     */
    private String buildFeld(String attr) {
        String sichtbarkeit = mapSichtbarkeit(attr.charAt(0));
        String rest = attr.substring(1).trim();
        String name = nameVon(rest);
        String typ  = typVon(rest);

        String prefix = sichtbarkeit.isEmpty() ? "" : sichtbarkeit + " ";
        return "    " + prefix + typ + " " + name + ";\n";
    }

    /**
     * Erzeugt zu einem Attribut einen Getter und einen Setter (immer public),
     * sofern nicht bereits eine gleichnamige Methode explizit im Diagramm vorhanden.
     *
     * @param attr               rohe Attributzeile
     * @param vorhandeneMethoden Namen der bereits explizit definierten Methoden
     * @return Getter und/oder Setter als Java-Quelltext
     */
    private String buildGetterSetter(String attr, Set<String> vorhandeneMethoden) {
        String rest  = attr.substring(1).trim();
        String name  = nameVon(rest);
        String typ   = typVon(rest);
        String gross = grossAnfang(name);

        StringBuilder sb = new StringBuilder();

        if (!vorhandeneMethoden.contains("get" + gross)) {
            sb.append("    public ").append(typ).append(" get").append(gross).append("() {\n");
            sb.append("        return ").append(name).append(";\n");
            sb.append("    }\n\n");
        }

        if (vorhandeneMethoden.contains("set" + gross)) {
            return sb.toString();
        }

        sb.append("    public void set").append(gross)
          .append("(").append(typ).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
        sb.append("    }\n\n");

        return sb.toString();
    }

    /**
     * Erzeugt aus einer Methodenzeile einen Methodenrumpf.
     * Stimmt der Name mit {@code className} ueberein, wird ein Konstruktor erzeugt.
     * In Enums wird der Konstruktor als {@code private} deklariert.
     *
     * @param method    rohe Methodenzeile
     * @param className Name der umgebenden Klasse (zur Konstruktor-Erkennung)
     * @param forEnum   {@code true} wenn die Methode in einer Enum-Deklaration steht
     * @return Methodenrumpf als Java-Quelltext
     */
    private String buildMethode(String method, String className, boolean forEnum) {
        char visChar = method.charAt(0);
        String sichtbarkeit = mapSichtbarkeit(visChar);
        String rest = method.substring(1).trim();

        int klammerAuf = rest.indexOf('(');
        int klammerZu  = rest.lastIndexOf(')');
        if (klammerAuf < 0 || klammerZu < 0) return "";

        String name     = rest.substring(0, klammerAuf).trim();
        String paramRoh = rest.substring(klammerAuf + 1, klammerZu).trim();
        String params   = buildParameter(paramRoh);

        boolean istKonstruktor = name.equals(className);

        // Enum-Konstruktoren muessen private sein
        if (istKonstruktor && forEnum) {
            sichtbarkeit = "private";
        }

        String rueckgabe = "";
        int doppelpunkt = rest.indexOf(':', klammerZu);
        if (doppelpunkt >= 0) {
            rueckgabe = bereinigteRueckgabe(rest.substring(doppelpunkt + 1).trim());
        }

        StringBuilder sb = new StringBuilder();
        String prefix = sichtbarkeit.isEmpty() ? "" : sichtbarkeit + " ";

        if (istKonstruktor) {
            sb.append("    ").append(prefix).append(name).append("(").append(params).append(") {\n");
            sb.append("        // TODO: Implementierung ergaenzen\n");
            sb.append("    }\n\n");
        } else {
            if (rueckgabe.isEmpty()) rueckgabe = "void";
            sb.append("    ").append(prefix).append(rueckgabe).append(" ")
              .append(name).append("(").append(params).append(") {\n");
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
     * Beispiel: {@code "path: String, n: int"} &rarr; {@code "String path, int n"}.
     * Parameter ohne Doppelpunkt werden uebersprungen.
     *
     * @param paramRoh Parameterteil zwischen den Klammern (kann leer sein)
     * @return Java-Parameterliste
     */
    private String buildParameter(String paramRoh) {
        if (paramRoh.isEmpty()) return "";

        List<String> teile = splitOberste(paramRoh, ',');
        StringBuilder sb = new StringBuilder();
        for (String roh : teile) {
            String p = roh.trim();
            if (!p.contains(":")) continue; // Nur "name: Type"-Format
            if (sb.length() > 0) sb.append(", ");
            sb.append(typVon(p)).append(" ").append(nameVon(p));
        }
        return sb.toString();
    }

    /**
     * Bereinigt einen Rueckgabetyp: entfernt Default-Werte (nach {@code =})
     * und geschweifte Modifier (nach der oeffnenden geschweiften Klammer).
     *
     * @param raw roher Rueckgabetyp-String
     * @return bereinigter Rueckgabetyp oder {@code "void"} wenn leer
     */
    private String bereinigteRueckgabe(String raw) {
        int eq = raw.indexOf('=');
        if (eq >= 0) raw = raw.substring(0, eq).trim();
        int brace = raw.indexOf('{');
        if (brace >= 0) raw = raw.substring(0, brace).trim();
        return raw.isEmpty() ? "void" : raw;
    }

    /**
     * Uebersetzt das UML-Sichtbarkeitszeichen in das Java-Schluesselwort.
     *
     * @param zeichen {@code +}, {@code -}, {@code #} oder {@code ~}
     * @return {@code "public"}, {@code "private"}, {@code "protected"} oder {@code ""}
     */
    private String mapSichtbarkeit(char zeichen) {
        return switch (zeichen) {
            case '+' -> "public";
            case '-' -> "private";
            case '#' -> "protected";
            default  -> ""; // ~ (paketsichtbar) und alles andere
        };
    }

    /**
     * Liefert den reinen Methodennamen aus einer rohen Methodenzeile.
     *
     * @param method rohe Methodenzeile
     * @return Methodenname ohne Sichtbarkeitszeichen und Klammern
     */
    private String methodenName(String method) {
        String rest = method.substring(1).trim();
        int klammer = rest.indexOf('(');
        return klammer < 0 ? rest : rest.substring(0, klammer).trim();
    }

    /**
     * Sammelt alle Methodennamen aus einer Liste roher Methodenzeilen.
     *
     * @param methods Liste roher Methodenzeilen
     * @return Menge der Methodennamen (fuer Dedup-Pruefung)
     */
    private Set<String> collectMethodNames(List<String> methods) {
        Set<String> names = new HashSet<>();
        for (String m : methods) names.add(methodenName(m));
        return names;
    }

    /**
     * Liefert den Namensteil aus {@code "name: Type"}.
     *
     * @param paar Zeichenkette der Form {@code "name: Type"}
     * @return der Name (alles vor dem ersten {@code :})
     */
    private String nameVon(String paar) {
        int dp = paar.indexOf(':');
        return dp < 0 ? paar.trim() : paar.substring(0, dp).trim();
    }

    /**
     * Liefert den bereinigten Typteil aus {@code "name: Type"}.
     * Default-Werte (nach {@code =}) und Modifier (nach der oeffnenden geschweiften Klammer) werden entfernt.
     *
     * @param paar Zeichenkette der Form {@code "name: Type"}
     * @return der bereinigte Typ oder {@code "Object"} als Fallback
     */
    private String typVon(String paar) {
        int dp = paar.indexOf(':');
        if (dp < 0) return "Object";
        String typ = paar.substring(dp + 1).trim();
        // Default-Wert entfernen: "int = 4" -> "int"
        int eq = typ.indexOf('=');
        if (eq >= 0) typ = typ.substring(0, eq).trim();
        // Modifier entfernen: "int {readonly}" -> "int"
        int brace = typ.indexOf('{');
        if (brace >= 0) typ = typ.substring(0, brace).trim();
        return typ.isEmpty() ? "Object" : typ;
    }

    /**
     * Liefert einen passenden Default-Rueckgabewert fuer den {@code return}-Befehl.
     *
     * @param typ der Rueckgabetyp
     * @return gueltiger Default-Wert als String
     */
    private String defaultRueckgabe(String typ) {
        return switch (typ) {
            case "int", "long", "short", "byte" -> "0";
            case "double", "float" -> "0.0";
            case "boolean" -> "false";
            case "char" -> "'\\u0000'";
            default -> "null";
        };
    }

    /**
     * Schreibt den ersten Buchstaben eines Wortes gross (fuer {@code getX}/{@code setX}).
     *
     * @param wort z.&nbsp;B. {@code "inputFile"}
     * @return z.&nbsp;B. {@code "InputFile"}
     */
    private String grossAnfang(String wort) {
        if (wort.isEmpty()) return wort;
        return Character.toUpperCase(wort.charAt(0)) + wort.substring(1);
    }

    /**
     * Erzeugt eine Signatur-Kennung einer Methode fuer die Duplikat-Erkennung.
     * Format: {@code "methodName(Type1,Type2)"}. Parametertypen werden ohne Namen
     * verglichen, da Java kein Overloading auf Parameternamen unterstuetzt.
     *
     * @param method rohe Methodenzeile
     * @return Signatur-String
     */
    private String methodSignatur(String method) {
        String rest = method.substring(1).trim();
        int ka = rest.indexOf('(');
        int kz = rest.lastIndexOf(')');
        if (ka < 0 || kz < 0) return rest;
        String name = rest.substring(0, ka).trim();
        String paramRoh = rest.substring(ka + 1, kz).trim();
        if (paramRoh.isEmpty()) return name + "()";
        List<String> teile = splitOberste(paramRoh, ',');
        StringBuilder sb = new StringBuilder(name).append("(");
        boolean first = true;
        for (String p : teile) {
            if (!p.contains(":")) continue;
            if (!first) sb.append(",");
            sb.append(typVon(p.trim()));
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Ermittelt den Default-Argument-String fuer Enum-Konstanten, wenn ein
     * Konstruktor mit Parametern vorhanden ist.
     * Beispiel: Konstruktor {@code Wochentag(aufgabe: String)} liefert {@code "null"}.
     *
     * @param klasse die Enum-Klasse
     * @return kommaseparierte Default-Argumente oder {@code ""} wenn kein parametrisierter
     *         Konstruktor vorhanden
     */
    private String buildEnumCtorArgs(DataSave klasse) {
        for (String method : klasse.getMethods()) {
            if (!methodenName(method).equals(klasse.getClassName())) continue;
            String rest = method.substring(1).trim();
            int ka = rest.indexOf('(');
            int kz = rest.lastIndexOf(')');
            if (ka < 0 || kz < 0) continue;
            String paramRoh = rest.substring(ka + 1, kz).trim();
            if (paramRoh.isEmpty()) return ""; // parameterloser Konstruktor
            List<String> teile = splitOberste(paramRoh, ',');
            List<String> defaults = new ArrayList<>();
            for (String p : teile) {
                if (!p.contains(":")) continue;
                defaults.add(defaultRueckgabe(typVon(p.trim())));
            }
            return String.join(", ", defaults);
        }
        return "";
    }

    /**
     * Trennt einen String am Trennzeichen, ignoriert aber Kommata innerhalb
     * von Generics ({@code <...>}).
     * Beispiel: {@code "a: Map<String, Integer>, b: int"} &rarr;
     * {@code ["a: Map<String, Integer>", " b: int"]}.
     *
     * @param text    der zu zerlegende String
     * @param trenner das Trennzeichen, hier {@code ','}
     * @return Liste der Teilstuecke auf oberster Ebene
     */
    private List<String> splitOberste(String text, char trenner) {
        List<String> teile = new ArrayList<>();
        int tiefe = 0;
        StringBuilder aktuell = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') tiefe++;
            else if (c == '>') tiefe--;

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
