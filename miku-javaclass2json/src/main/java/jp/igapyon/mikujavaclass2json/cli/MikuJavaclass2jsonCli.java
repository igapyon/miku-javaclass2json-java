package jp.igapyon.mikujavaclass2json.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexGenerator;
import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexOptions;
import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexResult;

public class MikuJavaclass2jsonCli {
    public static void main(String[] args) {
        int exitCode = new MikuJavaclass2jsonCli().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
                printHelp(out);
                return 0;
            }
            if (!"index".equals(args[0])) {
                err.println("Unknown command: " + args[0]);
                printHelp(err);
                return 2;
            }
            ClassIndexOptions options = parseIndexOptions(args);
            String phase = parsePhase(args);
            ClassIndexResult result;
            if ("step1".equals(phase)) {
                result = new ClassIndexGenerator().scanBinaryNames(options);
                out.println("step1 class names: " + result.getClassCount());
            } else if ("step2".equals(phase)) {
                options.setKnownBinaryNames(readKnownNames(args));
                result = new ClassIndexGenerator().generateClassJson(options);
                out.println("step2 class JSON files: " + result.getClassCount());
            } else if ("step3".equals(phase)) {
                options.setKnownBinaryNames(readKnownNames(args));
                result = new ClassIndexGenerator().generateIndexFiles(options);
                out.println("step3 index files: " + result.getClassCount());
            } else if ("all".equals(phase)) {
                result = new ClassIndexGenerator().generate(options);
                out.println("indexed classes: " + result.getClassCount());
            } else {
                throw new IllegalArgumentException("Unknown phase: " + phase);
            }
            out.println("output: " + options.getOutputDirectory());
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return 2;
        } catch (Exception ex) {
            err.println("Failed to create class index: " + ex.getMessage());
            return 1;
        }
    }

    private static ClassIndexOptions parseIndexOptions(String[] args) {
        ClassIndexOptions options = new ClassIndexOptions();
        options.setOutputDirectory(Paths.get(".java-class-index"));
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if ("--input".equals(arg)) {
                options.setInput(Paths.get(requireValue(args, ++index, arg)));
            } else if ("--output".equals(arg)) {
                options.setOutputDirectory(Paths.get(requireValue(args, ++index, arg)));
            } else if ("--phase".equals(arg) || "--step1-output".equals(arg)) {
                index++;
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        if (options.getInput() == null) {
            throw new IllegalArgumentException("--input is required");
        }
        return options;
    }

    private static String parsePhase(String[] args) {
        String phase = "all";
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if ("--phase".equals(arg)) {
                phase = requireValue(args, ++index, arg);
            } else if ("--input".equals(arg) || "--output".equals(arg) || "--step1-output".equals(arg)) {
                index++;
            }
        }
        return phase;
    }

    private static Set<String> readKnownNames(String[] args) throws IOException {
        Set<String> names = new HashSet<String>();
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if ("--step1-output".equals(arg)) {
                readKnownNames(Paths.get(requireValue(args, ++index, arg)), names);
            } else if ("--input".equals(arg) || "--output".equals(arg) || "--phase".equals(arg)) {
                index++;
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("--step1-output is required for --phase step2 or step3");
        }
        return names;
    }

    private static void readKnownNames(Path path, Set<String> names) throws IOException {
        Path file = Files.isDirectory(path) ? path.resolve("binary-names.jsonl") : path;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String name = parseBinaryName(line);
            if (name != null) {
                names.add(name);
            }
        }
    }

    private static String parseBinaryName(String line) {
        String marker = "\"binaryName\":\"";
        int start = line.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = line.indexOf('"', start);
        return end < 0 ? null : line.substring(start, end);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index];
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage:");
        out.println("  miku-javaclass2json index --input <classes-dir|class-file|jar-file> [--output .java-class-index]");
        out.println("  miku-javaclass2json index --phase step1 --input <input> --output <step1-dir>");
        out.println("  miku-javaclass2json index --phase step2 --input <input> --step1-output <step1-dir|binary-names.jsonl> --output <index-dir>");
        out.println("  miku-javaclass2json index --phase step3 --input <input> --step1-output <step1-dir|binary-names.jsonl> --output <index-dir>");
    }
}
