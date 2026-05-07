package jp.igapyon.mikujavaclass2json.coreapi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikujavaclass2json.internal.ClassFileInfo;
import jp.igapyon.mikujavaclass2json.internal.ClassFileInputReader;
import jp.igapyon.mikujavaclass2json.internal.ClassIndexWriters;
import jp.igapyon.mikujavaclass2json.internal.JsonFiles;

public class ClassIndexGenerator {
    public ClassIndexResult scanBinaryNames(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        options.reportProgress("step1", "creating output directory: " + options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory());

        ClassIndexResult result = new ClassIndexResult();
        Path file = options.getOutputDirectory().resolve("binary-names.jsonl");
        options.reportProgress("step1", "reading class input: " + options.getInput());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
                if (options.isExcludedPackage(classInfo.binaryName)) {
                    return;
                }
                writer.write("{\"binaryName\":\"" + classInfo.binaryName + "\"}");
                writer.newLine();
                result.incrementClassCount();
                reportClassProgress(options, "step1", "collected class names", result.getClassCount());
            });
        }
        result.addGeneratedPath(file);
        options.reportProgress("step1", "wrote binary names: " + file);
        return result;
    }

    public ClassIndexResult generate(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        options.reportProgress("all", "creating output directories: " + options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve(JsonFiles.CLASS_JSON_DIRECTORY));

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        options.reportProgress("all", "step1 reading class input: " + options.getInput());
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
            reportClassProgress(options, "all", "step1 collected classes", classes.size());
        });

        ClassIndexResult result = new ClassIndexResult();
        options.reportProgress("all", "step2/step3 writing class JSON and JSONL indexes: " + classes.size() + " classes");
        try (ClassIndexWriters writers = new ClassIndexWriters(options.getOutputDirectory())) {
            Set<String> seenBinaryNames = new HashSet<String>();
            for (IndexedClass indexedClass : classes) {
                if (!seenBinaryNames.add(indexedClass.classInfo.binaryName)) {
                    writers.writeWarning(indexedClass.classInfo, indexedClass.artifact, indexedClass.entryName,
                            "overwriting existing class JSON for binaryName");
                    result.incrementWarningCount();
                }
                writers.writeSource(indexedClass.classInfo, indexedClass.artifact, indexedClass.entryName);
                writers.writeClassIndex(indexedClass.classInfo);
                writers.writeSymbols(indexedClass.classInfo);
                writers.writeDependencies(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages());
                writers.writeMethodCalls(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages(),
                        options.getExcludeCallPackages());
                result.incrementClassCount();
                reportClassProgress(options, "all", "step2/step3 indexed classes", result.getClassCount());
            }
            writeClassJsonGroups(classes, options, result, indexedBinaryNames);
        }
        options.reportProgress("all", "step4 writing reverse method-call summary");
        result.addGeneratedPath(JsonFiles.writeMethodCallReverseSummaryJsonl(options.getOutputDirectory()));
        options.reportProgress("all", "writing index manifest");
        result.addGeneratedPath(JsonFiles.writeIndexJson(options.getOutputDirectory(), result.getClassCount(), result.getWarningCount(), true));
        return result;
    }

    public ClassIndexResult generateClassJson(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        options.reportProgress("step2", "creating output directories: " + options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve(JsonFiles.CLASS_JSON_DIRECTORY));

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        options.reportProgress("step2", "reading class input: " + options.getInput());
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
            reportClassProgress(options, "step2", "read classes", classes.size());
        });

        ClassIndexResult result = new ClassIndexResult();
        options.reportProgress("step2", "writing class JSON files: " + classes.size() + " classes");
        writeClassJsonGroups(classes, options, result, indexedBinaryNames);
        for (int index = 0; index < classes.size(); index++) {
            result.incrementClassCount();
            reportClassProgress(options, "step2", "wrote class JSON files", result.getClassCount());
        }
        return result;
    }

    public ClassIndexResult generateIndexFiles(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        options.reportProgress("step3", "creating output directory: " + options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory());

        List<IndexedClass> classes = new ArrayList<IndexedClass>();
        final Set<String> indexedBinaryNames = options.getKnownBinaryNames();
        options.reportProgress("step3", "reading class input: " + options.getInput());
        ClassFileInputReader.read(options.getInput(), (classInfo, artifact, entryName) -> {
            if (options.isExcludedPackage(classInfo.binaryName)) {
                return;
            }
            classes.add(new IndexedClass(classInfo, artifact, entryName));
            indexedBinaryNames.add(classInfo.binaryName);
            reportClassProgress(options, "step3", "read classes", classes.size());
        });

        ClassIndexResult result = new ClassIndexResult();
        options.reportProgress("step3", "writing JSONL indexes: " + classes.size() + " classes");
        try (ClassIndexWriters writers = new ClassIndexWriters(options.getOutputDirectory())) {
            for (IndexedClass indexedClass : classes) {
                writers.writeSource(indexedClass.classInfo, indexedClass.artifact, indexedClass.entryName);
                writers.writeClassIndex(indexedClass.classInfo);
                writers.writeSymbols(indexedClass.classInfo);
                writers.writeDependencies(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages());
                writers.writeMethodCalls(indexedClass.classInfo, options.getNoDescendPackages(), indexedBinaryNames, options.getExcludePackages(),
                        options.getExcludeCallPackages());
                result.incrementClassCount();
                reportClassProgress(options, "step3", "indexed classes", result.getClassCount());
            }
        }
        options.reportProgress("step3", "writing index manifest");
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
        options.reportProgress("step4", "writing reverse method-call summary from: " + summary);
        result.addGeneratedPath(JsonFiles.writeMethodCallReverseSummaryJsonl(outputDirectory));
        Path indexJson = outputDirectory.resolve("index.json");
        int classCount = readJsonInt(indexJson, "classCount");
        int warningCount = readJsonInt(indexJson, "warningCount");
        result.setClassCount(classCount);
        options.reportProgress("step4", "writing index manifest");
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

    private static void reportClassProgress(ClassIndexOptions options, String step, String action, int classCount) {
        if (classCount <= 10 || classCount % 100 == 0) {
            options.reportProgress(step, action + ": " + classCount);
        }
    }

    private static void writeClassJsonGroups(List<IndexedClass> classes, ClassIndexOptions options, ClassIndexResult result,
            Set<String> indexedBinaryNames) throws IOException {
        for (List<ClassFileInfo> classGroup : groupedClassInfos(classes).values()) {
            Path classJson = JsonFiles.writeClassJson(options.getOutputDirectory(), classGroup, options.getNoDescendPackages(), indexedBinaryNames,
                    options.getExcludePackages(), options.getExcludeCallPackages());
            result.addGeneratedPath(classJson);
        }
    }

    private static Map<String, List<ClassFileInfo>> groupedClassInfos(List<IndexedClass> classes) {
        Map<String, List<ClassFileInfo>> groups = new LinkedHashMap<String, List<ClassFileInfo>>();
        for (IndexedClass indexedClass : classes) {
            String topLevelBinaryName = JsonFiles.topLevelBinaryName(indexedClass.classInfo.binaryName);
            List<ClassFileInfo> group = groups.get(topLevelBinaryName);
            if (group == null) {
                group = new ArrayList<ClassFileInfo>();
                groups.put(topLevelBinaryName, group);
            }
            group.add(indexedClass.classInfo);
        }
        for (List<ClassFileInfo> group : groups.values()) {
            Collections.sort(group, new Comparator<ClassFileInfo>() {
                @Override
                public int compare(ClassFileInfo left, ClassFileInfo right) {
                    String leftTopLevel = JsonFiles.topLevelBinaryName(left.binaryName);
                    String rightTopLevel = JsonFiles.topLevelBinaryName(right.binaryName);
                    if (left.binaryName.equals(leftTopLevel) && !right.binaryName.equals(rightTopLevel)) {
                        return -1;
                    }
                    if (!left.binaryName.equals(leftTopLevel) && right.binaryName.equals(rightTopLevel)) {
                        return 1;
                    }
                    return left.binaryName.compareTo(right.binaryName);
                }
            });
        }
        return groups;
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
