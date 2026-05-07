package jp.igapyon.mikujavaclass2json.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(out.toString().contains("miku-javaclass2json --version"));
        assertTrue(out.toString().contains("miku-javaclass2json index"));
        assertTrue(out.toString().contains("Generated files:"));
        assertTrue(out.toString().contains("method-call-reverse-summary.jsonl"));
        assertTrue(out.toString().contains("--exclude-package"));
        assertTrue(out.toString().contains("--verbose"));
        assertTrue(out.toString().contains("Large-system guidance:"));
    }

    @Test
    public void versionReturnsZero() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new MikuJavaclass2jsonCli().run(new String[] { "--version" }, new PrintStream(out), new PrintStream(err));
        assertEquals(0, exitCode);
        assertEquals("miku-javaclass2json 0.5.4\n", out.toString());
        assertEquals("", err.toString());
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
        assertTrue(Files.exists(output.resolve("method-call-reverse-summary.jsonl")));
    }

    @Test
    public void verboseIndexCommandPrintsProgressToErr() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Path output = tempDir.resolve("verbose-index");

        int exitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--verbose", "--input", "target/test-classes", "--output", output.toString() }, new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("indexed classes:"));
        assertTrue(err.toString().contains("[verbose] all: starting index command"));
        assertTrue(err.toString().contains("[verbose] all: step1 reading class input:"));
        assertTrue(err.toString().contains("[verbose] all: step4 writing reverse method-call summary"));
        assertTrue(err.toString().contains("[verbose] all: completed index command"));
    }

    @Test
    public void verboseCanBeUsedBeforeIndexCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Path output = tempDir.resolve("verbose-global-index");

        int exitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "--verbose", "index", "--input", "target/test-classes", "--output", output.toString() }, new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("indexed classes:"));
        assertTrue(err.toString().contains("[verbose] all: starting index command"));
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
        assertTrue(Files.exists(output.resolve("cls")));

        ByteArrayOutputStream indexOut = new ByteArrayOutputStream();
        ByteArrayOutputStream indexErr = new ByteArrayOutputStream();

        int indexExitCode = new MikuJavaclass2jsonCli().run(new String[] { "index", "--phase", "step3", "--input", "target/test-classes",
                "--step1-output", names.toString(), "--output", output.toString() }, new PrintStream(indexOut), new PrintStream(indexErr));

        assertEquals(0, indexExitCode);
        assertTrue(indexOut.toString().contains("step3 index files:"));
        assertTrue(Files.exists(output.resolve("index.json")));
        assertTrue(Files.exists(output.resolve("dependencies.jsonl")));
        assertTrue(Files.exists(output.resolve("method-call-summary.jsonl")));
        assertFalse(Files.exists(output.resolve("method-call-reverse-summary.jsonl")));

        ByteArrayOutputStream reverseOut = new ByteArrayOutputStream();
        ByteArrayOutputStream reverseErr = new ByteArrayOutputStream();

        int reverseExitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--phase", "step4", "--output", output.toString() }, new PrintStream(reverseOut),
                new PrintStream(reverseErr));

        assertEquals(0, reverseExitCode);
        assertTrue(reverseOut.toString().contains("step4 reverse index files:"));
        assertTrue(Files.exists(output.resolve("method-call-reverse-summary.jsonl")));
    }

    @Test
    public void splitPhaseCommandsUseOutputAsDefaultStep1Output() {
        Path output = tempDir.resolve("same-output-split-index");
        ByteArrayOutputStream collectOut = new ByteArrayOutputStream();
        ByteArrayOutputStream collectErr = new ByteArrayOutputStream();

        int collectExitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--phase", "step1", "--input", "target/test-classes", "--output", output.toString() },
                new PrintStream(collectOut), new PrintStream(collectErr));

        assertEquals(0, collectExitCode);
        assertTrue(Files.exists(output.resolve("binary-names.jsonl")));

        ByteArrayOutputStream writeOut = new ByteArrayOutputStream();
        ByteArrayOutputStream writeErr = new ByteArrayOutputStream();

        int writeExitCode = new MikuJavaclass2jsonCli().run(
                new String[] { "index", "--phase", "step2", "--input", "target/test-classes", "--output", output.toString() },
                new PrintStream(writeOut), new PrintStream(writeErr));

        assertEquals(0, writeExitCode);
        assertTrue(writeOut.toString().contains("step2 class JSON files:"));
        assertTrue(Files.exists(output.resolve("cls")));
    }
}
