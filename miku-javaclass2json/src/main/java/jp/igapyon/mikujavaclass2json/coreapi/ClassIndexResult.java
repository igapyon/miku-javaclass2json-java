package jp.igapyon.mikujavaclass2json.coreapi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClassIndexResult {
    private int classCount;
    private int warningCount;
    private final List<Path> generatedPaths = new ArrayList<Path>();

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public void incrementClassCount() {
        this.classCount++;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void incrementWarningCount() {
        this.warningCount++;
    }

    public List<Path> getGeneratedPaths() {
        return new ArrayList<Path>(generatedPaths);
    }

    public void addGeneratedPath(Path generatedPath) {
        generatedPaths.add(generatedPath);
    }
}
