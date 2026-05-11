import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class UmlToJavaTranslatorTest {
    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("uml-translator-test");
        Path umlFile = tempDir.resolve("model.uml");
        Path outputDir = tempDir.resolve("out");

        String uml = String.join("\n",
                "package demo.model",
                "class Person {",
                "- id: int",
                "+ name: String",
                "+ getName(): String",
                "+ setName(value: String): void",
                "}");

        Files.writeString(umlFile, uml, StandardCharsets.UTF_8);

        UmlToJavaTranslator.translate(umlFile, outputDir);

        Path generated = outputDir.resolve("demo/model/Person.java");
        assertTrue(Files.exists(generated), "Expected generated class file");

        String content = Files.readString(generated, StandardCharsets.UTF_8);
        assertContains(content, "package demo.model;", "Expected package declaration");
        assertContains(content, "private int id;", "Expected private field");
        assertContains(content, "public String name;", "Expected public field");
        assertContains(content, "public String getName()", "Expected getter stub");
        assertContains(content, "public void setName(String value)", "Expected parameterized method stub");

        System.out.println("UmlToJavaTranslatorTest passed");
    }

    private static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new AssertionError(message + ": missing '" + expected + "'\nActual:\n" + content);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
