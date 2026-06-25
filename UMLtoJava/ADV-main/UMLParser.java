package progProjekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Einfache Implementierung des UML-Parsers:
 * - className(block) -> Klassenname (erste passende Kopfzeile)
 * - attribute(block) -> Attributzeilen (mit Sichtbarkeitszeichen)
 * - methode(block)   -> Methodenzeilen (mit Sichtbarkeitszeichen)
 *
 * Heuristiken: Zeilen mit '(' sind Methoden, Zeilen mit ':' (ohne '(')
 * sind Attribute, die erste Zeile ohne ':'/'(' und ohne Sichtbarkeitszeichen
 * ist der Klassenname. Fehlt das Sichtbarkeitszeichen, wird '-' (Attribute)
 * bzw. '+' (Methoden) als Standard verwendet.
 */
public class UMLParser {

    /**
     * Ermittelt den Klassennamen aus einem Panel-Block.
     * Die erste nicht-leere, nicht-sichtbare Zeile ohne ':' oder '(' wird als Name
     * interpretiert.
     */
    public String className(String block) {
        if (block == null) return "";
        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) continue;
            // entferne stereotype wie <<...>>
            line = removeStereotype(line);
            if (line.isEmpty()) continue;
            // Wenn Linie mit Sichtbarkeit beginnt, ist es kein Klassenname
            if (line.length() > 0 && isVisibility(line.charAt(0))) continue;
            // Wenn es ':' oder '(' enthält, ist es vermutlich Attribut oder Methode
            if (line.contains(":") || line.contains("(")) continue;
            // ansonsten nehmen wir diese Zeile als Klassenname
            return line;
        }
        return "";
    }

    public List<String> attribute(String block) {
        List<String> result = new ArrayList<>();
        if (block == null) return result;
        String header = className(block);
        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) continue;
            line = removeStereotype(line);
            if (line.isEmpty()) continue;
            if (line.equals(header)) continue; // Kopfzeile auslassen
            // Methode erkennen: hat '(' -> kein Attribut
            if (line.contains("(")) continue;
            // Attribut: sollte ':' enthalten
            if (line.contains(":")) {
                // sicherstellen, dass Zeile mit Sichtbarkeitszeichen beginnt
                if (!isVisibility(line.charAt(0))) {
                    line = "-" + " " + line; // default private
                }
                result.add(line);
            }
        }
        return result;
    }

    public List<String> methode(String block) {
        List<String> result = new ArrayList<>();
        if (block == null) return result;
        String header = className(block);
        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (isSeparator(line)) continue;
            line = removeStereotype(line);
            if (line.isEmpty()) continue;
            if (line.equals(header)) continue; // Kopfzeile auslassen
            // Methode: muss '(' und ')' enthalten (einfacher Check)
            if (line.contains("(") && line.contains(")")) {
                if (!isVisibility(line.charAt(0))) {
                    line = "+" + " " + line; // default public
                }
                result.add(line);
            }
        }
        return result;
    }

    // einfache Trennlinien erkennen (z.B. ---- oder ==== oder "Attributes"/"Methods" headers)
    private boolean isSeparator(String line) {
        String low = line.toLowerCase();
        if (low.startsWith("----") || low.startsWith("====") || low.equals("attributes") || low.equals("methods")) {
            return true;
        }
        return false;
    }

    // Entfernt Stereotype wie <<interface>> an Zeilenanfang / -ende falls vorhanden
    private String removeStereotype(String line) {
        // simpel: entferne alles zwischen << und >>
        int start = line.indexOf("<<");
        int end = line.indexOf(">>");
        if (start >= 0 && end > start) {
            String left = line.substring(0, start).trim();
            String right = line.substring(end + 2).trim();
            String merged = (left + " " + right).trim();
            return merged;
        }
        return line;
    }

    // Sichtbarkeitszeichen erkennen: + public, - private, # protected, ~ package
    private boolean isVisibility(char c) {
        return c == '+' || c == '-' || c == '#' || c == '~';
    }

}
