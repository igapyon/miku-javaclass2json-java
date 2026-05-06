package jp.igapyon.mikujavaclass2json.coreapi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassIndexOptions {
    private Path input;
    private Path outputDirectory;
    private boolean failOnMissingClasses = true;
    private Set<String> knownBinaryNames = new HashSet<String>();
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

    public Set<String> getKnownBinaryNames() {
        return new HashSet<String>(knownBinaryNames);
    }

    public void setKnownBinaryNames(Set<String> knownBinaryNames) {
        this.knownBinaryNames = knownBinaryNames == null ? new HashSet<String>() : new HashSet<String>(knownBinaryNames);
    }

    public List<String> getNoDescendPackages() {
        return new ArrayList<String>(noDescendPackages);
    }

    public void setNoDescendPackages(List<String> noDescendPackages) {
        this.noDescendPackages = noDescendPackages == null ? new ArrayList<String>() : new ArrayList<String>(noDescendPackages);
    }
}
