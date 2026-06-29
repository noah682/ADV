package progProjekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Zerlegt einen rohen UMLet-Panel-Block in Klassenname, Art (Klasse / abstrakte
 * Klasse / Interface / Enum), Attribute, Methoden und Enum-Konstanten.
 *
 * <p>Unterstuetzte UML-Notationen:
 * <ul>
 *   <li>Stereotype: {@code <<interface>>}, {@code «interface»}, {@code <<enum>>},
 *       {@code «enum»}, {@code <<enumeration>>} usw.</li>
 *   <li>Abstrakte Klassen: {@code {abstract}} als eigene Zeile oder {@code /Name/}
 *       (Schraegstrich-Notation).</li>
 *   <li>Statische Member: {@code _text_} (Unterstrich-Notation).</li>
 *   <li>Abgeleitete Member: {@code /text/} oder fuehrendes {@code /}.</li>
 *   <li>Sichtbarkeit: {@code +} public, {@code -} private, {@code #} protected,
 *       {@code ~} paketsichtbar.</li>
 *   <li>Umlet-Styling-Zeilen ({@code bg=yellow}, {@code lt=<<-} usw.) werden
 *       vollstaendig ignoriert.</li>
 * </ul>
 */
public class UMLParser {

    /**
     * Erzeugt einen Parser ohne Zustand. Alle Methoden arbeiten ausschliesslich
     * auf dem jeweils uebergebenen Panel-Block.
     */
    public UMLParser() {
    }

    /**
     * Ermittelt den bereinigten Klassennamen aus einem Panel-Block.
     *
     * <p>Die erste nicht-leere Kopfzeile ohne Sichtbarkeitszeichen, ohne
     * Doppelpunkt und ohne Klammer wird als Klassenname interpretiert.
     * Stereotype ({@code <<...>>}, {@code «...»}) werden entfernt,
     * Schraegstriche ({@code /Name/}) fuer abstrakte Klassen werden gestripped,
     * und ungueltige Java-Identifier-Zeichen (z.&nbsp;B. Bindestriche) werden
     * entfernt.
     *
     * @param block roher Panel-Inhalt eines UML-Elements
     * @return bereinigter Java-Bezeichner oder {@code ""} wenn kein Name erkennbar
     */
    public String className(String block) {
        if (block == null) return "";
        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) break;           // Kopfbereich endet bei --
            if (isUmletProperty(line)) continue;    // Styling-Zeilen ignorieren
            line = stripFormatting(line);           // _..._ Unterstreichung entfernen
            if (line.isEmpty()) continue;
            if (line.startsWith("{") && line.endsWith("}")) continue; // {abstract} etc.
            line = removeStereotype(line);          // <<...>> / «...» entfernen
            if (line.isEmpty()) continue;
            if (isVisibility(line.charAt(0))) continue;
            if (line.contains(":") || line.contains("(")) continue;
            // /Name/ -> Schraegstriche entfernen (abstrakte Klasse)
            if (line.startsWith("/") && line.endsWith("/") && line.length() > 2) {
                line = line.substring(1, line.length() - 1).trim();
            } else if (line.startsWith("/") && line.length() > 1) {
                line = line.substring(1).trim();
            }
            return sanitizeIdentifier(line);
        }
        return "";
    }

    /**
     * Ermittelt die Art ({@link DataSave.ClassKind}) des UML-Typs aus dem Block.
     *
     * <p>Reihenfolge der Pruefung: Interface-Stereotyp, Enum-Stereotyp,
     * {@code {abstract}}-Zeile, {@code /Name/}-Notation.
     *
     * @param block roher Panel-Inhalt eines UML-Elements
     * @return erkannte {@link DataSave.ClassKind}, Standard ist {@code CLASS}
     */
    public DataSave.ClassKind classKind(String block) {
        if (block == null) return DataSave.ClassKind.CLASS;
        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) break;
            if (isUmletProperty(line)) continue;
            line = stripFormatting(line);
            if (line.isEmpty()) continue;
            if (isInterfaceStereotype(line)) return DataSave.ClassKind.INTERFACE;
            if (isEnumStereotype(line))      return DataSave.ClassKind.ENUM;
            if (line.equals("{abstract}"))   return DataSave.ClassKind.ABSTRACT_CLASS;
            // /Name/ Notation zeigt abstrakte Klasse an
            if (line.startsWith("/") && line.endsWith("/") && line.length() > 2) {
                return DataSave.ClassKind.ABSTRACT_CLASS;
            }
        }
        return DataSave.ClassKind.CLASS;
    }

    /**
     * Extrahiert alle Attributzeilen (ohne Methoden, ohne Kopfzeile) aus dem Block.
     *
     * <p>Umlet-Styling-Zeilen, abstrakte Modifier und Unterstrich-/Schraegstrich-
     * Formatierungen werden bereinigt. Default-Sichtbarkeit ist {@code -} (private).
     *
     * @param block roher Panel-Inhalt eines UML-Elements
     * @return Liste der normalisierten Attributzeilen
     */
    public List<String> attribute(String block) {
        List<String> result = new ArrayList<>();
        if (block == null) return result;
        String header = className(block);
        String[] lines = block.replace("\r\n", "\n").split("\n");
        boolean passedHeader = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) { passedHeader = true; continue; }
            if (!passedHeader) continue;
            if (isUmletProperty(line)) continue;
            line = stripFormatting(line);
            if (line.isEmpty()) continue;
            if (line.equals(header)) continue;
            if (line.startsWith("{") && line.endsWith("}")) continue;
            if (line.contains("(")) continue;  // Methoden ausschliessen
            if (line.contains(":")) {
                // Fuehrendes / (abgeleitetes Attribut) entfernen
                if (line.startsWith("/")) line = line.substring(1).trim();
                // {readonly}, {abstract} etc. am Zeilenende entfernen
                line = stripBraceModifiers(line);
                if (line.isEmpty()) continue;
                if (!isVisibility(line.charAt(0))) {
                    line = "- " + line;  // Default: private
                }
                result.add(line);
            }
        }
        return result;
    }

    /**
     * Extrahiert alle Methodenzeilen (ohne Attribute, ohne Kopfzeile) aus dem Block.
     *
     * <p>Erkennungsmerkmal: Zeile enthaelt {@code (} und {@code )}.
     * Umlet-Formatting und {@code {abstract}}-Modifier werden entfernt.
     * Default-Sichtbarkeit ist {@code +} (public).
     *
     * @param block roher Panel-Inhalt eines UML-Elements
     * @return Liste der normalisierten Methodenzeilen
     */
    public List<String> methode(String block) {
        List<String> result = new ArrayList<>();
        if (block == null) return result;
        String header = className(block);
        String[] lines = block.replace("\r\n", "\n").split("\n");
        boolean passedHeader = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) { passedHeader = true; continue; }
            if (!passedHeader) continue;
            if (isUmletProperty(line)) continue;
            line = stripFormatting(line);
            if (line.isEmpty()) continue;
            if (line.equals(header)) continue;
            if (line.startsWith("{") && line.endsWith("}")) continue;
            if (line.contains("(") && line.contains(")")) {
                // Fuehrendes / (abgeleitete Methode) entfernen
                if (line.startsWith("/")) line = line.substring(1).trim();
                // Abschliessendes / entfernen
                if (line.endsWith("/")) line = line.substring(0, line.length() - 1).trim();
                // {abstract}, {static} etc. entfernen
                line = stripBraceModifiers(line);
                if (line.isEmpty()) continue;
                if (!isVisibility(line.charAt(0))) {
                    line = "+ " + line;  // Default: public
                }
                result.add(line);
            }
        }
        return result;
    }

    /**
     * Extrahiert die Enum-Konstanten aus dem ersten Abschnitt nach {@code --}.
     *
     * <p>Konstantenzeilen haben weder {@code :} (keinen Typ) noch {@code (}
     * (keine Parameter). Ein optionales Sichtbarkeitszeichen ({@code +}, {@code -}
     * usw.) wird entfernt.
     *
     * @param block roher Panel-Inhalt eines UML-Enum-Elements
     * @return Liste der Konstantennamen, z.&nbsp;B. {@code ["MONTAG", "DIENSTAG"]}
     */
    public List<String> enumConstants(String block) {
        List<String> result = new ArrayList<>();
        if (block == null) return result;
        String[] lines = block.replace("\r\n", "\n").split("\n");
        boolean inConstants = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) {
                if (!inConstants) {
                    inConstants = true;   // Erster -- : Konstantenabschnitt beginnt
                } else {
                    break;                // Zweiter -- : Konstantenabschnitt endet
                }
                continue;
            }
            if (!inConstants) continue;
            if (isUmletProperty(line)) continue;
            line = stripFormatting(line);
            if (line.isEmpty()) continue;
            // Nur echte Konstanten: kein : und kein (
            if (!line.contains(":") && !line.contains("(")) {
                if (isVisibility(line.charAt(0))) {
                    line = line.substring(1).trim();
                }
                String name = sanitizeIdentifier(line);
                if (!name.isEmpty()) result.add(name);
            }
        }
        return result;
    }

    // ── private Hilfsmethoden ──────────────────────────────────────────────────

    /**
     * Prueft ob eine Zeile ein UMLet-Abschnittstrennzeichen ist ({@code --},
     * {@code ----}, {@code ====}, {@code attributes} oder {@code methods}).
     *
     * @param line bereinigter Zeileninhalt
     * @return {@code true} wenn die Zeile ein Trenner ist
     */
    private boolean isSeparator(String line) {
        if (line.equals("--")) return true;
        if (line.startsWith("----") || line.startsWith("====")) return true;
        String low = line.toLowerCase();
        return low.equals("attributes") || low.equals("methods");
    }

    /**
     * Prueft ob eine Zeile eine UMLet-Styling-Property ist (Muster {@code key=...}).
     * Solche Zeilen enthalten keine sinnvollen UML-Daten und werden ignoriert.
     *
     * @param line bereinigter Zeileninhalt
     * @return {@code true} wenn die Zeile eine Property ist
     */
    private boolean isUmletProperty(String line) {
        return line.matches("^[A-Za-z_][A-Za-z0-9_]*=.*");
    }

    /**
     * Entfernt Stereotype der Form {@code <<text>>} oder {@code «text»} aus der Zeile.
     *
     * @param line Zeileninhalt moeglicherweise mit Stereotyp
     * @return Zeile ohne Stereotyp, oder unveraenderter Input wenn keins gefunden
     */
    private String removeStereotype(String line) {
        // ASCII-Doppelpfeile <<...>>
        int start = line.indexOf("<<");
        int end   = line.indexOf(">>");
        if (start >= 0 && end > start) {
            return (line.substring(0, start) + line.substring(end + 2)).trim();
        }
        // Guillemets «...»
        int gStart = line.indexOf('«');
        int gEnd   = line.indexOf('»');
        if (gStart >= 0 && gEnd > gStart) {
            return (line.substring(0, gStart) + line.substring(gEnd + 1)).trim();
        }
        return line;
    }

    /**
     * Entfernt UMLet-Formatierungszeichen am Zeilenanfang und -ende:
     * {@code _text_} (Unterstreichung = statisch).
     *
     * @param line Zeileninhalt moeglicherweise mit Formatierung
     * @return Zeile ohne Formatierungszeichen
     */
    private String stripFormatting(String line) {
        if (line.startsWith("_") && line.endsWith("_") && line.length() > 2) {
            line = line.substring(1, line.length() - 1).trim();
        }
        return line;
    }

    /**
     * Entfernt geschweifte Klammerausdruecke wie {@code {abstract}} oder
     * {@code {readonly}} aus einer Attribut- oder Methodenzeile.
     *
     * @param line Zeileninhalt moeglicherweise mit Modifier
     * @return Zeile ohne geschweifte Klammerausdruecke
     */
    private String stripBraceModifiers(String line) {
        int brace = line.indexOf('{');
        if (brace >= 0) {
            line = line.substring(0, brace).trim();
        }
        return line;
    }

    /**
     * Prueft ob eine Zeile ein Interface-Stereotyp traegt
     * ({@code <<interface>>} oder {@code «interface»}).
     *
     * @param line Zeileninhalt (Gross-/Kleinschreibung ignoriert)
     * @return {@code true} wenn die Zeile ein Interface-Stereotyp ist
     */
    private boolean isInterfaceStereotype(String line) {
        String low = line.toLowerCase();
        return low.equals("<<interface>>") || low.equals("«interface»");
    }

    /**
     * Prueft ob eine Zeile ein Enum-Stereotyp traegt ({@code <<enum>>},
     * {@code «enum»}, {@code <<enumeration>>}, {@code «enumeration»},
     * {@code <<enumerations>>} oder {@code «enumerations»}).
     *
     * @param line Zeileninhalt (Gross-/Kleinschreibung ignoriert)
     * @return {@code true} wenn die Zeile ein Enum-Stereotyp ist
     */
    private boolean isEnumStereotype(String line) {
        String low = line.toLowerCase();
        return low.equals("<<enum>>")           || low.equals("«enum»")
            || low.equals("<<enumeration>>")    || low.equals("«enumeration»")
            || low.equals("<<enumerations>>")   || low.equals("«enumerations»");
    }

    /**
     * Bereinigt einen String zu einem gueltigen Java-Bezeichner.
     *
     * <p>Es werden nur Zeichen behalten, die {@link Character#isJavaIdentifierStart}
     * (fuer das erste Zeichen) bzw. {@link Character#isJavaIdentifierPart}
     * (fuer alle weiteren) zulaesst. Dazu gehoeren Unicode-Buchstaben (inkl.
     * Umlaute wie oe, ae, ue), Ziffern, {@code _} und {@code $}.
     * Ungueltige Zeichen wie Bindestriche werden einfach weggelassen.
     *
     * @param name zu bereinigender Bezeichner
     * @return gueltiger Java-Bezeichner oder {@code ""} wenn leer
     */
    private String sanitizeIdentifier(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = sb.isEmpty()
                    ? Character.isJavaIdentifierStart(c)
                    : Character.isJavaIdentifierPart(c);
            if (ok) sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Prueft ob ein Zeichen ein UML-Sichtbarkeitszeichen ist.
     *
     * @param c zu pruefendes Zeichen
     * @return {@code true} fuer {@code +}, {@code -}, {@code #} oder {@code ~}
     */
    private boolean isVisibility(char c) {
        return c == '+' || c == '-' || c == '#' || c == '~';
    }
}
