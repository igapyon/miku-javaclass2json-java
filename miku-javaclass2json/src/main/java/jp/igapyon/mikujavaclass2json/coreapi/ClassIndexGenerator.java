package jp.igapyon.mikujavaclass2json.coreapi;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

import jp.igapyon.mikujavaclass2json.internal.ClassFileInfo;
import jp.igapyon.mikujavaclass2json.internal.ClassFileReader;
import jp.igapyon.mikujavaclass2json.internal.JsonFiles;

public class ClassIndexGenerator {
    public ClassIndexResult generate(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve("classes"));

        ClassIndexResult result = new ClassIndexResult();
        IndexWriters writers = new IndexWriters(options.getOutputDirectory());
        try {
            readClasses(options.getInput(), options, result, writers);
        } finally {
            writers.close();
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

    private static void readClasses(Path input, ClassIndexOptions options, ClassIndexResult result, IndexWriters writers) throws IOException {
        if (!Files.exists(input)) {
            throw new IOException("Input does not exist: " + input);
        }
        if (Files.isDirectory(input)) {
            readDirectory(input, options, result, writers);
        } else if (input.getFileName().toString().endsWith(".jar")) {
            readJar(input, options, result, writers);
        } else if (input.getFileName().toString().endsWith(".class")) {
            writeClass(ClassFileReader.read(Files.readAllBytes(input), input.toString()), input.toString(), input.getFileName().toString(),
                    options, result, writers);
        } else {
            throw new IOException("Input must be a classes directory, .class file, or .jar file: " + input);
        }
    }

    private static void readDirectory(Path root, ClassIndexOptions options, ClassIndexResult result, IndexWriters writers) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    if (file.getFileName().toString().endsWith(".class")) {
                        String entryName = root.relativize(file).toString();
                        writeClass(ClassFileReader.read(Files.readAllBytes(file), entryName), root.toString(), entryName, options, result,
                                writers);
                    } else if (file.getFileName().toString().endsWith(".jar")) {
                        readJar(file, options, result, writers);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }
            throw ex;
        }
    }

    private static void readJar(Path jar, ClassIndexOptions options, ClassIndexResult result, IndexWriters writers) throws IOException {
        JarInputStream input = new JarInputStream(Files.newInputStream(jar));
        try {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    writeClass(ClassFileReader.read(readAll(input), jar.toString() + "!" + entry.getName()), jar.toString(), entry.getName(),
                            options, result, writers);
                }
            }
        } finally {
            input.close();
        }
    }

    private static void writeClass(ClassFileInfo classInfo, String artifact, String entryName, ClassIndexOptions options,
            ClassIndexResult result, IndexWriters writers) throws IOException {
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

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = input.read(buffer)) >= 0) {
            output.write(buffer, 0, length);
        }
        return output.toByteArray();
    }

    private static final class IndexWriters {
        private final BufferedWriter classes;
        private final BufferedWriter symbols;
        private final BufferedWriter dependencies;
        private final BufferedWriter methodCalls;
        private final BufferedWriter sources;
        private final BufferedWriter warnings;

        private IndexWriters(Path outputDirectory) throws IOException {
            classes = Files.newBufferedWriter(outputDirectory.resolve("classes.jsonl"), StandardCharsets.UTF_8);
            symbols = Files.newBufferedWriter(outputDirectory.resolve("symbols.jsonl"), StandardCharsets.UTF_8);
            dependencies = Files.newBufferedWriter(outputDirectory.resolve("dependencies.jsonl"), StandardCharsets.UTF_8);
            methodCalls = Files.newBufferedWriter(outputDirectory.resolve("method-calls.jsonl"), StandardCharsets.UTF_8);
            sources = Files.newBufferedWriter(outputDirectory.resolve("sources.jsonl"), StandardCharsets.UTF_8);
            warnings = Files.newBufferedWriter(outputDirectory.resolve("warnings.log"), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        private void writeClassIndex(ClassFileInfo info) throws IOException {
            classes.write(JsonFiles.classIndexLine(info));
            classes.newLine();
        }

        private void writeSymbols(ClassFileInfo info) throws IOException {
            symbols.write(JsonFiles.symbolLines(info));
        }

        private void writeDependencies(ClassFileInfo info, java.util.List<String> noDescendPackages) throws IOException {
            dependencies.write(JsonFiles.dependencyLines(info, noDescendPackages));
        }

        private void writeMethodCalls(ClassFileInfo info) throws IOException {
            methodCalls.write(JsonFiles.methodCallLines(info));
        }

        private void writeSource(ClassFileInfo info, String artifact, String entryName) throws IOException {
            sources.write(JsonFiles.sourceLine(info, artifact, entryName));
            sources.newLine();
        }

        private void writeWarning(ClassFileInfo info, String artifact, String entryName, String message) throws IOException {
            warnings.write(JsonFiles.warningLine(info, artifact, entryName, message));
            warnings.newLine();
        }

        private void close() throws IOException {
            IOException thrown = null;
            thrown = close(classes, thrown);
            thrown = close(symbols, thrown);
            thrown = close(dependencies, thrown);
            thrown = close(methodCalls, thrown);
            thrown = close(sources, thrown);
            thrown = close(warnings, thrown);
            if (thrown != null) {
                throw thrown;
            }
        }

        private static IOException close(BufferedWriter writer, IOException thrown) {
            try {
                writer.close();
            } catch (IOException ex) {
                if (thrown == null) {
                    return ex;
                }
                thrown.addSuppressed(ex);
            }
            return thrown;
        }
    }
}
