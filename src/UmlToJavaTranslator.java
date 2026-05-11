import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UmlToJavaTranslator {
    private static final Pattern CLASS_PATTERN = Pattern.compile("^class\\s+(\\w+)(\\s*\\{)?$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^([+\\-#])?\\s*(\\w+)\\s*:\\s*([\\w<>\\[\\], ?]+);?$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^([+\\-#])?\\s*(\\w+)\\s*\\(([^)]*)\\)\\s*:\\s*([\\w<>\\[\\], ?]+);?$");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java UmlToJavaTranslator <uml-file> <output-directory>");
            System.exit(1);
        }

        Path umlFile = Path.of(args[0]);
        Path outputDirectory = Path.of(args[1]);
        translate(umlFile, outputDirectory);
    }

    public static void translate(Path umlFile, Path outputDirectory) throws IOException {
        List<String> lines = Files.readAllLines(umlFile, StandardCharsets.UTF_8);
        UmlModel model = parse(lines);
        writeJavaFiles(model, outputDirectory);
    }

    private static UmlModel parse(List<String> lines) {
        UmlModel model = new UmlModel();
        UmlClass currentClass = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("package ")) {
                model.packageName = line.substring("package ".length()).trim().replace(";", "");
                continue;
            }

            if ("}".equals(line)) {
                currentClass = null;
                continue;
            }

            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.matches()) {
                currentClass = new UmlClass(classMatcher.group(1));
                model.classes.add(currentClass);
                continue;
            }

            if (currentClass == null) {
                throw new IllegalArgumentException("Unexpected UML line outside class: " + line);
            }

            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.matches()) {
                currentClass.methods.add(new UmlMethod(
                        visibility(methodMatcher.group(1)),
                        methodMatcher.group(2),
                        parseParameters(methodMatcher.group(3)),
                        methodMatcher.group(4).trim()));
                continue;
            }

            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            if (fieldMatcher.matches()) {
                currentClass.fields.add(new UmlField(
                        visibility(fieldMatcher.group(1)),
                        fieldMatcher.group(2),
                        fieldMatcher.group(3).trim()));
                continue;
            }

            throw new IllegalArgumentException("Unrecognized UML member: " + line);
        }

        return model;
    }

    private static List<MethodParameter> parseParameters(String parameterText) {
        List<MethodParameter> parameters = new ArrayList<>();
        if (parameterText == null || parameterText.trim().isEmpty()) {
            return parameters;
        }

        String[] parts = parameterText.split(",");
        for (String part : parts) {
            String[] pair = part.trim().split(":");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Invalid method parameter: " + part);
            }
            String name = pair[0].trim();
            String type = pair[1].trim();
            parameters.add(new MethodParameter(name, type));
        }

        return parameters;
    }

    private static String visibility(String symbol) {
        if ("+".equals(symbol)) {
            return "public";
        }
        if ("#".equals(symbol)) {
            return "protected";
        }
        return "private";
    }

    private static void writeJavaFiles(UmlModel model, Path outputDirectory) throws IOException {
        for (UmlClass umlClass : model.classes) {
            StringBuilder javaContent = new StringBuilder();

            if (model.packageName != null && !model.packageName.isBlank()) {
                javaContent.append("package ").append(model.packageName).append(";\n\n");
            }

            javaContent.append("public class ").append(umlClass.name).append(" {\n");

            for (UmlField field : umlClass.fields) {
                javaContent.append("    ")
                        .append(field.visibility)
                        .append(" ")
                        .append(field.type)
                        .append(" ")
                        .append(field.name)
                        .append(";\n");
            }

            if (!umlClass.fields.isEmpty() && !umlClass.methods.isEmpty()) {
                javaContent.append("\n");
            }

            for (UmlMethod method : umlClass.methods) {
                javaContent.append("    ")
                        .append(method.visibility)
                        .append(" ")
                        .append(method.returnType)
                        .append(" ")
                        .append(method.name)
                        .append("(")
                        .append(methodParameterList(method.parameters))
                        .append(") {\n")
                        .append("        throw new UnsupportedOperationException(\"Not implemented yet.\");\n")
                        .append("    }\n\n");
            }

            if (!umlClass.methods.isEmpty()) {
                javaContent.setLength(javaContent.length() - 1);
            }

            javaContent.append("}\n");

            Path targetRoot = outputDirectory;
            if (model.packageName != null && !model.packageName.isBlank()) {
                targetRoot = outputDirectory.resolve(model.packageName.replace('.', '/'));
            }
            Files.createDirectories(targetRoot);
            Files.writeString(targetRoot.resolve(umlClass.name + ".java"), javaContent.toString(), StandardCharsets.UTF_8);
        }
    }

    private static String methodParameterList(List<MethodParameter> parameters) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            MethodParameter parameter = parameters.get(i);
            out.append(parameter.type).append(" ").append(parameter.name);
            if (i < parameters.size() - 1) {
                out.append(", ");
            }
        }
        return out.toString();
    }

    private static class UmlModel {
        private String packageName;
        private final List<UmlClass> classes = new ArrayList<>();
    }

    private static class UmlClass {
        private final String name;
        private final List<UmlField> fields = new ArrayList<>();
        private final List<UmlMethod> methods = new ArrayList<>();

        private UmlClass(String name) {
            this.name = name;
        }
    }

    private static class UmlField {
        private final String visibility;
        private final String name;
        private final String type;

        private UmlField(String visibility, String name, String type) {
            this.visibility = visibility;
            this.name = name;
            this.type = type;
        }
    }

    private static class UmlMethod {
        private final String visibility;
        private final String name;
        private final List<MethodParameter> parameters;
        private final String returnType;

        private UmlMethod(String visibility, String name, List<MethodParameter> parameters, String returnType) {
            this.visibility = visibility;
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
        }
    }

    private static class MethodParameter {
        private final String name;
        private final String type;

        private MethodParameter(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
