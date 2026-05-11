# ADV

Java executable for translating a simple UML document into a Java project.

## UML to Java translator

Compile:

```bash
javac src/UmlToJavaTranslator.java src/UmlToJavaTranslatorTest.java
```

Run translator:

```bash
java -cp src UmlToJavaTranslator /path/to/model.uml /path/to/output-dir
```

Supported UML text format:

```text
package com.example.model
class Person {
  - id: int
  + name: String
  + getName(): String
  + setName(value: String): void
}
```

Member lines may optionally end with `;`.

Run focused test:

```bash
java -cp src UmlToJavaTranslatorTest
```
