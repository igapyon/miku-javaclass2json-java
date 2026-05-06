package jp.igapyon.mikujavaclass2json.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class MikuJavaclass2jsonCliTest {
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
}
