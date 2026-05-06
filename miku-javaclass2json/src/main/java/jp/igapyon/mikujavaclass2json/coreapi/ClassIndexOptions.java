package jp.igapyon.mikujavaclass2json.coreapi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassIndexOptions {
    private Path input;
    private Path outputDirectory;
    private boolean failOnMissingClasses = true;
    private List<String> noDescendPackages = new ArrayList<String>(Arrays.asList(
            "java.*", "javax.*", "jakarta.*", "jdk.*", "sun.*", "com.sun.*", "org.w3c.*", "org.xml.*"));

    public Path getInput() {
        return input;
    }

    public void setInput(Path input) {
        this.input = input;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isFailOnMissingClasses() {
        return failOnMissingClasses;
    }

    public void setFailOnMissingClasses(boolean failOnMissingClasses) {
        this.failOnMissingClasses = failOnMissingClasses;
    }

    public List<String> getNoDescendPackages() {
        return new ArrayList<String>(noDescendPackages);
    }

    public void setNoDescendPackages(List<String> noDescendPackages) {
        this.noDescendPackages = noDescendPackages == null ? new ArrayList<String>() : new ArrayList<String>(noDescendPackages);
    }
}
