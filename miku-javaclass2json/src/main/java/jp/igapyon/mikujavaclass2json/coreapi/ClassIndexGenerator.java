package jp.igapyon.mikujavaclass2json.coreapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import jp.igapyon.mikujavaclass2json.internal.ClassFileInfo;
import jp.igapyon.mikujavaclass2json.internal.ClassFileReader;
import jp.igapyon.mikujavaclass2json.internal.JsonFiles;

public class ClassIndexGenerator {
    public ClassIndexResult generate(ClassIndexOptions options) throws IOException {
        validateOptions(options);
        Files.createDirectories(options.getOutputDirectory());
        Files.createDirectories(options.getOutputDirectory().resolve("classes"));

        List<ClassFileInfo> classes = readClasses(options.getInput());
        Collections.sort(classes);

        ClassIndexResult result = new ClassIndexResult();
        for (ClassFileInfo classInfo : classes) {
            Path classJson = JsonFiles.writeClassJson(options.getOutputDirectory(), classInfo, options.getNoDescendPackages());
            result.addGeneratedPath(classJson);
        }
        result.addGeneratedPath(JsonFiles.writeIndexJson(options.getOutputDirectory(), classes));
        result.addGeneratedPath(JsonFiles.writeSymbolsJsonl(options.getOutputDirectory(), classes));
        result.addGeneratedPath(JsonFiles.writeDependenciesJsonl(options.getOutputDirectory(), classes, options.getNoDescendPackages()));
        result.setClassCount(classes.size());
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

    private static List<ClassFileInfo> readClasses(Path input) throws IOException {
        if (!Files.exists(input)) {
            throw new IOException("Input does not exist: " + input);
        }
        List<ClassFileInfo> classes = new ArrayList<ClassFileInfo>();
        if (Files.isDirectory(input)) {
            readDirectory(input, classes);
        } else if (input.getFileName().toString().endsWith(".jar")) {
            readJar(input, classes);
        } else if (input.getFileName().toString().endsWith(".class")) {
            classes.add(ClassFileReader.read(Files.readAllBytes(input), input.toString()));
        } else {
            throw new IOException("Input must be a classes directory, .class file, or .jar file: " + input);
        }
        return classes;
    }

    private static void readDirectory(Path root, List<ClassFileInfo> classes) throws IOException {
        List<Path> files = new ArrayList<Path>();
        Files.walk(root).filter(Files::isRegularFile).forEach(files::add);
        Collections.sort(files);
        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".class")) {
                classes.add(ClassFileReader.read(Files.readAllBytes(file), root.relativize(file).toString()));
            }
        }
    }

    private static void readJar(Path jar, List<ClassFileInfo> classes) throws IOException {
        JarInputStream input = new JarInputStream(Files.newInputStream(jar));
        try {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    classes.add(ClassFileReader.read(readAll(input), jar.toString() + "!" + entry.getName()));
                }
            }
        } finally {
            input.close();
        }
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
}
