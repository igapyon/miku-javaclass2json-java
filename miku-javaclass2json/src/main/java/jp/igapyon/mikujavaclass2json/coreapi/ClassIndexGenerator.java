package jp.igapyon.mikujavaclass2json.coreapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jp.igapyon.mikujavaclass2json.internal.ClassFileInfo;
import jp.igapyon.mikujavaclass2json.internal.ClassFileInputReader;
import jp.igapyon.mikujavaclass2json.internal.ClassIndexWriters;
import jp.igapyon.mikujavaclass2json.internal.JsonFiles;

public class ClassIndexGenerator {
    public ClassIndexResult generate(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve("classes"));

        ClassIndexResult result = new ClassIndexResult();
        try (ClassIndexWriters writers = new ClassIndexWriters(options.getOutputDirectory())) {
            ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> writeClass(classInfo, artifact, entryName, options,
                    result, writers));
        }
        result.addGeneratedPath(JsonFiles.writeIndexJson(options.getOutputDirectory(), result.getClassCount(), result.getWarningCount()));
        return result;
    }

    private static void validateOptions(ClassIndexOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }
        if (options.getInput() == null) {
            throw new IllegalArgumentException("input is required");
        }
        if (options.getOutputDirectory() == null) {
            throw new IllegalArgumentException("outputDirectory is required");
        }
    }

    private static void writeClass(ClassFileInfo classInfo, String artifact, String entryName, ClassIndexOptions options,
            ClassIndexResult result, ClassIndexWriters writers) throws IOException {
        Path classJson = JsonFiles.classJsonPath(options.getOutputDirectory(), classInfo);
        if (Files.exists(classJson)) {
            writers.writeWarning(classInfo, artifact, entryName, "overwriting existing class JSON for binaryName");
            result.incrementWarningCount();
        }
        writers.writeSource(classInfo, artifact, entryName);
        classJson = JsonFiles.writeClassJson(options.getOutputDirectory(), classInfo, options.getNoDescendPackages());
        result.addGeneratedPath(classJson);
        writers.writeClassIndex(classInfo);
        writers.writeSymbols(classInfo);
        writers.writeDependencies(classInfo, options.getNoDescendPackages());
        writers.writeMethodCalls(classInfo);
        result.incrementClassCount();
    }
}
