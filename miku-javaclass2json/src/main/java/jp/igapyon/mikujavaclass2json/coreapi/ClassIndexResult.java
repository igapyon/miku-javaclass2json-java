package jp.igapyon.mikujavaclass2json.coreapi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClassIndexResult {
    private int classCount;
    private final List<Path> generatedPaths = new ArrayList<Path>();

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public List<Path> getGeneratedPaths() {
        return new ArrayList<Path>(generatedPaths);
    }

    public void addGeneratedPath(Path generatedPath) {
        generatedPaths.add(generatedPath);
    }
}
