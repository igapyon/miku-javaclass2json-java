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

    @Test
    public void splitPhaseCommandsGenerateOutput() {
        Path names = tempDir.resolve("names");
        ByteArrayOutputStream collectOut = new ByteArrayOutputStream();
        ByteArrayOutputStream collectErr = new ByteArrayOutputStream();

        int collectExitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--phase", "step1", "--input", "target/test-classes", "--output", names.toString() },
                new PrintStream(collectOut), new PrintStream(collectErr));

        assertEquals(0, collectExitCode);
        assertTrue(collectOut.toString().contains("step1 class names:"));
        assertTrue(Files.exists(names.resolve("binary-names.jsonl")));

        Path output = tempDir.resolve("split-index");
        ByteArrayOutputStream writeOut = new ByteArrayOutputStream();
        ByteArrayOutputStream writeErr = new ByteArrayOutputStream();

        int writeExitCode = new MikuJavaclass2jsonCli().run(new String[] { "index", "--phase", "step2", "--input", "target/test-classes",
                "--step1-output", names.toString(), "--output", output.toString() }, new PrintStream(writeOut), new PrintStream(writeErr));

        assertEquals(0, writeExitCode);
        assertTrue(writeOut.toString().contains("step2 class JSON files:"));
        assertTrue(Files.exists(output.resolve("classes")));

        ByteArrayOutputStream indexOut = new ByteArrayOutputStream();
        ByteArrayOutputStream indexErr = new ByteArrayOutputStream();

        int indexExitCode = new MikuJavaclass2jsonCli().run(new String[] { "index", "--phase", "step3", "--input", "target/test-classes",
                "--step1-output", names.toString(), "--output", output.toString() }, new PrintStream(indexOut), new PrintStream(indexErr));

        assertEquals(0, indexExitCode);
        assertTrue(indexOut.toString().contains("step3 index files:"));
        assertTrue(Files.exists(output.resolve("index.json")));
        assertTrue(Files.exists(output.resolve("dependencies.jsonl")));
        assertTrue(Files.exists(output.resolve("method-call-summary.jsonl")));
    }
}
