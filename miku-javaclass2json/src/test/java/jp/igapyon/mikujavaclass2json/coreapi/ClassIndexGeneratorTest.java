package jp.igapyon.mikujavaclass2json.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClassIndexGeneratorTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    public Path tempDir;

    @Test
    public void indexesCompiledTestClass() throws Exception {
        Path output = tempDir.resolve("index");
        ClassIndexOptions options = new ClassIndexOptions();
        options.setInput(findTestClassesDirectory());
        options.setOutputDirectory(output);

        ClassIndexResult result = new ClassIndexGenerator().generate(options);

        assertTrue(result.getClassCount() > 0);
        assertTrue(Files.exists(output.resolve("index.json")));
        assertTrue(Files.exists(output.resolve("classes.jsonl")));
        assertTrue(Files.exists(output.resolve("symbols.jsonl")));
        assertTrue(Files.exists(output.resolve("dependencies.jsonl")));
        assertTrue(Files.exists(output.resolve("method-calls.jsonl")));
        assertTrue(Files.exists(output.resolve("sources.jsonl")));
        assertTrue(Files.exists(output.resolve("warnings.log")));
        assertEquals(true, Files.walk(output.resolve("classes")).anyMatch(path -> path.toString().endsWith("ClassIndexGeneratorTest.json")));
        String methodCalls = new String(Files.readAllBytes(output.resolve("method-calls.jsonl")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(methodCalls.contains("\"opcode\":\"invoke"));
        assertTrue(methodCalls.contains("\"fromClass\":\"jp.igapyon.mikujavaclass2json.coreapi.ClassIndexGeneratorTest\""));
        Path classJson = output.resolve("classes/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.json");
        String classJsonText = new String(Files.readAllBytes(classJson), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(classJsonText.contains("\"calls\""));
        assertTrue(classJsonText.contains("\"toMethod\""));
        JsonNode indexJson = JSON.readTree(output.resolve("index.json").toFile());
        assertEquals("java-class-index-v1", indexJson.get("schemaVersion").asText());
        JsonNode parsedClassJson = JSON.readTree(classJson.toFile());
        assertEquals("java-class-index-class-v1", parsedClassJson.get("schemaVersion").asText());
        assertTrue(parsedClassJson.get("methods").isArray());
    }

    @Test
    public void duplicateBinaryNameOverwritesAndWritesWarningLog() throws Exception {
        Path input = tempDir.resolve("input");
        Path first = input.resolve("a/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class");
        Path second = input.resolve("b/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Path compiledClass = findTestClassesDirectory().resolve("jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class");
        Files.copy(compiledClass, first, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(compiledClass, second, StandardCopyOption.REPLACE_EXISTING);

        Path output = tempDir.resolve("duplicate-index");
        ClassIndexOptions options = new ClassIndexOptions();
        options.setInput(input);
        options.setOutputDirectory(output);

        ClassIndexResult result = new ClassIndexGenerator().generate(options);

        assertEquals(2, result.getClassCount());
        assertEquals(1, result.getWarningCount());
        String warnings = new String(Files.readAllBytes(output.resolve("warnings.log")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(warnings.contains("warning: overwriting existing class JSON"));
        assertTrue(warnings.contains("[Asia/Tokyo]"));
    }

    @Test
    public void indexesSingleClassFile() throws Exception {
        Path output = tempDir.resolve("single-class-index");
        ClassIndexOptions options = new ClassIndexOptions();
        options.setInput(compiledTestClass());
        options.setOutputDirectory(output);

        ClassIndexResult result = new ClassIndexGenerator().generate(options);

        assertEquals(1, result.getClassCount());
        assertTrue(Files.exists(output.resolve("classes/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.json")));
        assertTrue(new String(Files.readAllBytes(output.resolve("sources.jsonl")), java.nio.charset.StandardCharsets.UTF_8)
                .contains("ClassIndexGeneratorTest.class"));
    }

    @Test
    public void indexesSingleJarFile() throws Exception {
        Path jar = tempDir.resolve("sample.jar");
        writeJar(jar, "jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class", compiledTestClass());

        Path output = tempDir.resolve("single-jar-index");
        ClassIndexOptions options = new ClassIndexOptions();
        options.setInput(jar);
        options.setOutputDirectory(output);

        ClassIndexResult result = new ClassIndexGenerator().generate(options);

        assertEquals(1, result.getClassCount());
        assertTrue(Files.exists(output.resolve("method-calls.jsonl")));
        assertTrue(new String(Files.readAllBytes(output.resolve("sources.jsonl")), java.nio.charset.StandardCharsets.UTF_8)
                .contains("sample.jar"));
    }

    @Test
    public void indexesJarInsideDirectory() throws Exception {
        Path input = tempDir.resolve("jar-dir");
        Files.createDirectories(input);
        Path jar = input.resolve("nested.jar");
        writeJar(jar, "jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class", compiledTestClass());

        Path output = tempDir.resolve("jar-dir-index");
        ClassIndexOptions options = new ClassIndexOptions();
        options.setInput(input);
        options.setOutputDirectory(output);

        ClassIndexResult result = new ClassIndexGenerator().generate(options);

        assertEquals(1, result.getClassCount());
        assertTrue(Files.exists(output.resolve("classes/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.json")));
        assertTrue(new String(Files.readAllBytes(output.resolve("sources.jsonl")), java.nio.charset.StandardCharsets.UTF_8)
                .contains("nested.jar"));
    }

    private static Path findTestClassesDirectory() {
        return java.nio.file.Paths.get("target/test-classes");
    }

    private static Path compiledTestClass() {
        return findTestClassesDirectory().resolve("jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.class");
    }

    private static void writeJar(Path jar, String entryName, Path classFile) throws Exception {
        JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
        try {
            output.putNextEntry(new JarEntry(entryName));
            Files.copy(classFile, output);
            output.closeEntry();
        } finally {
            output.close();
        }
    }
}
