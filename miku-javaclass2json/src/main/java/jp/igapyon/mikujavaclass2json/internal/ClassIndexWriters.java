package jp.igapyon.mikujavaclass2json.internal;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class ClassIndexWriters implements Closeable {
    private final BufferedWriter classes;
    private final BufferedWriter symbols;
    private final BufferedWriter dependencies;
    private final BufferedWriter methodCalls;
    private final BufferedWriter sources;
    private final BufferedWriter warnings;

    public ClassIndexWriters(Path outputDirectory) throws IOException {
        classes = Files.newBufferedWriter(outputDirectory.resolve("classes.jsonl"), StandardCharsets.UTF_8);
        symbols = Files.newBufferedWriter(outputDirectory.resolve("symbols.jsonl"), StandardCharsets.UTF_8);
        dependencies = Files.newBufferedWriter(outputDirectory.resolve("dependencies.jsonl"), StandardCharsets.UTF_8);
        methodCalls = Files.newBufferedWriter(outputDirectory.resolve("method-calls.jsonl"), StandardCharsets.UTF_8);
        sources = Files.newBufferedWriter(outputDirectory.resolve("sources.jsonl"), StandardCharsets.UTF_8);
        warnings = Files.newBufferedWriter(outputDirectory.resolve("warnings.log"), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    public void writeClassIndex(ClassFileInfo info) throws IOException {
        classes.write(JsonFiles.classIndexLine(info));
        classes.newLine();
    }

    public void writeSymbols(ClassFileInfo info) throws IOException {
        symbols.write(JsonFiles.symbolLines(info));
    }

    public void writeDependencies(ClassFileInfo info, List<String> noDescendPackages) throws IOException {
        dependencies.write(JsonFiles.dependencyLines(info, noDescendPackages));
    }

    public void writeMethodCalls(ClassFileInfo info) throws IOException {
        methodCalls.write(JsonFiles.methodCallLines(info));
    }

    public void writeSource(ClassFileInfo info, String artifact, String entryName) throws IOException {
        sources.write(JsonFiles.sourceLine(info, artifact, entryName));
        sources.newLine();
    }

    public void writeWarning(ClassFileInfo info, String artifact, String entryName, String message) throws IOException {
        warnings.write(JsonFiles.warningLine(info, artifact, entryName, message));
        warnings.newLine();
    }

    @Override
    public void close() throws IOException {
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
