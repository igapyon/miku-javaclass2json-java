package jp.igapyon.mikujavaclass2json.mavenplugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MikuJavaclass2jsonMojoTest {
    @Test
    public void canInstantiateMojo() {
        assertNotNull(new MikuJavaclass2jsonMojo());
    }
}
