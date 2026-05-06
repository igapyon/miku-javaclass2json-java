package jp.igapyon.mikujavaclass2json.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

public final class ClassFileInputReader {
    private ClassFileInputReader() {
    }

    public static void read(Path input, ClassFileVisitor visitor) throws IOException {
        if (!Files.exists(input)) {
            throw new IOException("Input does not exist: " + input);
        }
        if (Files.isDirectory(input)) {
            readDirectory(input, visitor);
        } else if (isJar(input)) {
            readJar(input, visitor);
        } else if (isClassFile(input)) {
            visitor.visit(ClassFileReader.read(Files.readAllBytes(input), input.toString()), input.toString(), input.getFileName().toString());
        } else {
            throw new IOException("Input must be a classes directory, .class file, or .jar file: " + input);
        }
    }

    private static void readDirectory(Path root, ClassFileVisitor visitor) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    if (isClassFile(file)) {
                        String entryName = root.relativize(file).toString();
                        visitor.visit(ClassFileReader.read(Files.readAllBytes(file), entryName), root.toString(), entryName);
                    } else if (isJar(file)) {
                        readJar(file, visitor);
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

    private static void readJar(Path jar, ClassFileVisitor visitor) throws IOException {
        try (JarInputStream input = new JarInputStream(Files.newInputStream(jar))) {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    visitor.visit(ClassFileReader.read(readAll(input), jar.toString() + "!" + entry.getName()), jar.toString(), entry.getName());
                }
            }
        }
    }

    private static boolean isClassFile(Path path) {
        return path.getFileName().toString().endsWith(".class");
    }

    private static boolean isJar(Path path) {
        return path.getFileName().toString().endsWith(".jar");
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
