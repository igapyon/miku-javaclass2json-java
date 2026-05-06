package jp.igapyon.mikujavaclass2json.coreapi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikujavaclass2json.internal.ClassFileInfo;
import jp.igapyon.mikujavaclass2json.internal.ClassFileInputReader;
import jp.igapyon.mikujavaclass2json.internal.ClassIndexWriters;
import jp.igapyon.mikujavaclass2json.internal.JsonFiles;

public class ClassIndexGenerator {
    public ClassIndexResult scanBinaryNames(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());

        ClassIndexResult result = new ClassIndexResult();
        Path file = options.getOutputDirectory().resolve("binary-names.jsonl");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
                if (options.isExcludedPackage(classInfo.binaryName)) {
                    return;
                }
                writer.write("{\"binaryName\":\"" + classInfo.binaryName + "\"}");
                writer.newLine();
                result.incrementClassCount();
            });
        }
        result.addGeneratedPath(file);
        return result;
    }

    public ClassIndexResult generate(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve("classes"));

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
        });

        ClassIndexResult result = new ClassIndexResult();
        try (ClassIndexWriters writers = new ClassIndexWriters(options.getOutputDirectory())) {
            for (IndexedClass indexedClass : classes) {
                writeClass(indexedClass.classInfo, indexedClass.artifact, indexedClass.entryName, options, result, writers, indexedBinaryNames);
            }
        }
        result.addGeneratedPath(JsonFiles.writeMethodCallReverseSummaryJsonl(options.getOutputDirectory()));
        result.addGeneratedPath(JsonFiles.writeIndexJson(options.getOutputDirectory(), result.getClassCount(), result.getWarningCount(), true));
        return result;
    }

    public ClassIndexResult generateClassJson(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve("classes"));

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
        });

        ClassIndexResult result = new ClassIndexResult();
        for (IndexedClass indexedClass : classes) {
            Path classJson = JsonFiles.classJsonPath(options.getOutputDirectory(), indexedClass.classInfo);
            classJson = JsonFiles.writeClassJson(options.getOutputDirectory(), indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames,
                    options.getExcludePackages(), options.getExcludeCallPackages());
            result.addGeneratedPath(classJson);
            result.incrementClassCount();
        }
        return result;
    }

    public ClassIndexResult generateIndexFiles(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
        });

        ClassIndexResult result = new ClassIndexResult();
        try (ClassIndexWriters writers = new ClassIndexWriters(options.getOutputDirectory())) {
            for (IndexedClass indexedClass : classes) {
                writers.writeSource(indexedClass.classInfo, indexedClass.artifact, indexedClass.entryName);
                writers.writeClassIndex(indexedClass.classInfo);
                writers.writeSymbols(indexedClass.classInfo);
                writers.writeDependencies(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages());
                writers.writeMethodCalls(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages(),
                        options.getExcludeCallPackages());
                result.incrementClassCount();
            }
        }
        result.addGeneratedPath(JsonFiles.writeIndexJson(options.getOutputDirectory(), result.getClassCount(), result.getWarningCount(), false));
        return result;
    }

    public ClassIndexResult generateReverseIndexFiles(ClassIndexOptions options) throws IOException {
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }
        if (options.getOutputDirectory() == null) {
            throw new IllegalArgumentException("outputDirectory is required");
        }
        Path outputDirectory = options.getOutputDirectory();
        Path summary = outputDirectory.resolve("method-call-summary.jsonl");
        if (!Files.exists(summary)) {
            throw new IllegalArgumentException("method-call-summary.jsonl is required in output directory");
        }

        ClassIndexResult result = new ClassIndexResult();
        result.addGeneratedPath(JsonFiles.writeMethodCallReverseSummaryJsonl(outputDirectory));
        Path indexJson = outputDirectory.resolve("index.json");
        int classCount = readJsonInt(indexJson, "classCount");
        int warningCount = readJsonInt(indexJson, "warningCount");
        result.setClassCount(classCount);
        result.addGeneratedPath(JsonFiles.writeIndexJson(outputDirectory, classCount, warningCount, true));
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

    private static int readJsonInt(Path file, String name) throws IOException {
        if (!Files.exists(file)) {
            return 0;
        }
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String marker = "\"" + name + "\":";
        int start = text.indexOf(marker);
        if (start < 0) {
            return 0;
        }
        start += marker.length();
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (end == start) {
            return 0;
        }
        return Integer.parseInt(text.substring(start, end));
    }

    private static void writeClass(ClassFileInfo classInfo, String artifact, String entryName, ClassIndexOptions options,
            ClassIndexResult result, ClassIndexWriters writers, Set<String> indexedBinaryNames) throws IOException {
        Path classJson = JsonFiles.classJsonPath(options.getOutputDirectory(), classInfo);
        if (Files.exists(classJson)) {
            writers.writeWarning(classInfo, artifact, entryName, "overwriting existing class JSON for binaryName");
            result.incrementWarningCount();
        }
        writers.writeSource(classInfo, artifact, entryName);
        classJson = JsonFiles.writeClassJson(options.getOutputDirectory(), classInfo, options.getNoDescendPackages(), indexedBinaryNames,
                options.getExcludePackages(), options.getExcludeCallPackages());
        result.addGeneratedPath(classJson);
        writers.writeClassIndex(classInfo);
        writers.writeSymbols(classInfo);
        writers.writeDependencies(classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages());
        writers.writeMethodCalls(classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages(), options.getExcludeCallPackages());
        result.incrementClassCount();
    }

    private static final class IndexedClass {
        private final ClassFileInfo classInfo;
        private final String artifact;
        private final String entryName;

        private IndexedClass(ClassFileInfo classInfo, String artifact, String entryName) {
            this.classInfo = classInfo;
            this.artifact = artifact;
            this.entryName = entryName;
        }
    }
}
