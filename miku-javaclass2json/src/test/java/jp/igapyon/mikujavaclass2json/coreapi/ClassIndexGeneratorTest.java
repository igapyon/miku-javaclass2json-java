package jp.igapyon.mikujavaclass2json.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClassIndexGeneratorTest {
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
        Path classJson = output.resolve("classes/jp/igapyon/mikujavaclass2json/coreapi/ClassIndexGeneratorTest.json");
        String classJsonText = new String(Files.readAllBytes(classJson), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(classJsonText.contains("\"calls\""));
        assertTrue(classJsonText.contains("\"toMethod\""));
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

    private static Path findTestClassesDirectory() {
        return java.nio.file.Paths.get("target/test-classes");
    }
}
