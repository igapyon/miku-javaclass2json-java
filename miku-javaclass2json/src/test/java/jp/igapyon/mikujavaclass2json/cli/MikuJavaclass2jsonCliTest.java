package jp.igapyon.mikujavaclass2json.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MikuJavaclass2jsonCliTest {
    @TempDir
    public Path tempDir;

    @Test
    public void helpReturnsZero() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new MikuJavaclass2jsonCli().run(new String[] { "--help" }, new PrintStream(out), new PrintStream(err));
        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("miku-javaclass2json index"));
    }

    @Test
    public void missingInputReturnsUsageError() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new MikuJavaclass2jsonCli().run(new String[] { "index" }, new PrintStream(out), new PrintStream(err));
        assertEquals(2, exitCode);
        assertTrue(err.toString().contains("--input is required"));
    }

    @Test
    public void indexCommandGeneratesOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Path output = tempDir.resolve("cli-index");

        int exitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--input", "target/test-classes", "--output", output.toString() }, new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("indexed classes:"));
        assertTrue(Files.exists(output.resolve("index.json")));
        assertTrue(Files.exists(output.resolve("method-calls.jsonl")));
    }
}
