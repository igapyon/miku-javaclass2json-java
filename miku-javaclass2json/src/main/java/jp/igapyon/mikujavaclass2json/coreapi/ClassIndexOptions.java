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
    private List<String> excludePackages = new ArrayList<String>();
    private List<String> excludeCallPackages = new ArrayList<String>();
    private ClassIndexProgressListener progressListener;

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

    public List<String> getExcludePackages() {
        return new ArrayList<String>(excludePackages);
    }

    public void setExcludePackages(List<String> excludePackages) {
        this.excludePackages = excludePackages == null ? new ArrayList<String>() : new ArrayList<String>(excludePackages);
    }

    public List<String> getExcludeCallPackages() {
        return new ArrayList<String>(excludeCallPackages);
    }

    public void setExcludeCallPackages(List<String> excludeCallPackages) {
        this.excludeCallPackages = excludeCallPackages == null ? new ArrayList<String>() : new ArrayList<String>(excludeCallPackages);
    }

    public ClassIndexProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(ClassIndexProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void reportProgress(String step, String message) {
        if (progressListener != null) {
            progressListener.onProgress(step, message);
        }
    }

    public boolean isExcludedPackage(String binaryName) {
        return matchesPackage(binaryName, excludePackages);
    }

    private static boolean matchesPackage(String binaryName, List<String> patterns) {
        for (String pattern : patterns) {
            String prefix = pattern.endsWith(".*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            if (binaryName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
