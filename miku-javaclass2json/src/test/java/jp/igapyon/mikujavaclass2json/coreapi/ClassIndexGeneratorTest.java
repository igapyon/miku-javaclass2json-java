package jp.igapyon.mikujavaclass2json.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

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
        assertTrue(Files.exists(output.resolve("symbols.jsonl")));
        assertTrue(Files.exists(output.resolve("dependencies.jsonl")));
        assertEquals(true, Files.walk(output.resolve("classes")).anyMatch(path -> path.toString().endsWith("ClassIndexGeneratorTest.json")));
    }

    private static Path findTestClassesDirectory() {
        return java.nio.file.Paths.get("target/test-classes");
    }
}
