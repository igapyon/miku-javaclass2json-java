package jp.igapyon.mikujavaclass2json.mavenplugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MikuJavaclass2jsonMojoTest {
    @TempDir
    public Path tempDir;

    @Test
    public void canInstantiateMojo() {
        assertNotNull(new MikuJavaclass2jsonMojo());
    }

    @Test
    public void executeGeneratesIndexFromConfiguredDirectories() throws Exception {
        MikuJavaclass2jsonMojo mojo = new MikuJavaclass2jsonMojo();
        mojo.setClassesDirectory(new File("target/test-classes"));
        mojo.setOutputDirectory(tempDir.resolve("plugin-index").toFile());

        mojo.execute();

        assertTrue(Files.exists(tempDir.resolve("plugin-index/index.json")));
        assertTrue(Files.exists(tempDir.resolve("plugin-index/classes.jsonl")));
    }

    @Test
    public void skipDoesNotGenerateOutput() throws Exception {
        MikuJavaclass2jsonMojo mojo = new MikuJavaclass2jsonMojo();
        Path output = tempDir.resolve("skipped-index");
        mojo.setClassesDirectory(new File("target/test-classes"));
        mojo.setOutputDirectory(output.toFile());
        mojo.setSkip(true);

        mojo.execute();

        assertTrue(!Files.exists(output));
    }
}
