package jp.igapyon.mikujavaclass2json.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
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
            if ("--version".equals(args[0]) || "-v".equals(args[0])) {
                out.println("miku-javaclass2json " + version());
                return 0;
            }
            if (!"index".equals(args[0])) {
                err.println("Unknown command: " + args[0]);
                printHelp(err);
                return 2;
            }
            String phase = parsePhase(args);
            ClassIndexOptions options = parseIndexOptions(args, phase);
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
            } else if ("step4".equals(phase)) {
                result = new ClassIndexGenerator().generateReverseIndexFiles(options);
                out.println("step4 reverse index files: " + result.getGeneratedPaths().size());
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

    private static ClassIndexOptions parseIndexOptions(String[] args, String phase) {
        ClassIndexOptions options = new ClassIndexOptions();
        options.setOutputDirectory(Paths.get(".java-class-index"));
        List<String> excludePackages = new ArrayList<String>();
        List<String> excludeCallPackages = new ArrayList<String>();
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if ("--input".equals(arg)) {
                options.setInput(Paths.get(requireValue(args, ++index, arg)));
            } else if ("--output".equals(arg)) {
                options.setOutputDirectory(Paths.get(requireValue(args, ++index, arg)));
            } else if ("--exclude-package".equals(arg)) {
                excludePackages.add(requireValue(args, ++index, arg));
            } else if ("--exclude-call-package".equals(arg)) {
                excludeCallPackages.add(requireValue(args, ++index, arg));
            } else if ("--phase".equals(arg) || "--step1-output".equals(arg)) {
                index++;
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        options.setExcludePackages(excludePackages);
        options.setExcludeCallPackages(excludeCallPackages);
        if (options.getInput() == null && !"step4".equals(phase)) {
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
            } else if ("--input".equals(arg) || "--output".equals(arg) || "--step1-output".equals(arg) || "--exclude-package".equals(arg)
                    || "--exclude-call-package".equals(arg)) {
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
            } else if ("--input".equals(arg) || "--output".equals(arg) || "--phase".equals(arg) || "--exclude-package".equals(arg)
                    || "--exclude-call-package".equals(arg)) {
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
        out.println("miku-javaclass2json indexes Java .class files, class directories, and .jar files");
        out.println("into JSON / JSONL artifacts for agent and search workflows.");
        out.println();
        out.println("Usage:");
        out.println("  miku-javaclass2json --version");
        out.println("  miku-javaclass2json index --input <classes-dir|class-file|jar-file> [--output <index-dir>]");
        out.println("  miku-javaclass2json index --phase step1 --input <input> --output <step1-dir> [filters]");
        out.println("  miku-javaclass2json index --phase step2 --input <input> --step1-output <step1-dir|binary-names.jsonl> --output <index-dir> [filters]");
        out.println("  miku-javaclass2json index --phase step3 --input <input> --step1-output <step1-dir|binary-names.jsonl> --output <index-dir> [filters]");
        out.println("  miku-javaclass2json index --phase step4 --output <index-dir>");
        out.println();
        out.println("Default behavior:");
        out.println("  Without --phase, the index command runs the full single-process pipeline:");
        out.println("  collect class names, write per-class JSON, write JSONL indexes, then write");
        out.println("  method-call-reverse-summary.jsonl. If --output is omitted, output goes to");
        out.println("  .java-class-index.");
        out.println();
        out.println("Generated files:");
        out.println("  index.json                         Manifest for generated index files.");
        out.println("  classes.jsonl                      One line per indexed class.");
        out.println("  symbols.jsonl                      Class, field, and method symbols.");
        out.println("  dependencies.jsonl                 Class dependency edges.");
        out.println("  method-calls.jsonl                 Raw bytecode method-call edges.");
        out.println("  method-call-summary.jsonl          Aggregated forward call edges.");
        out.println("  method-call-reverse-summary.jsonl  Aggregated reverse call edges, target first.");
        out.println("  sources.jsonl                      Class to source artifact/entry mapping.");
        out.println("  warnings.log                       Non-fatal indexing warnings.");
        out.println("  classes/                           Per-class JSON details.");
        out.println();
        out.println("Split phases:");
        out.println("  step1  Reads input and writes binary-names.jsonl.");
        out.println("  step2  Reads input and step1 names, then writes classes/<binaryName>.json.");
        out.println("  step3  Reads input and step1 names, then writes line-oriented JSONL indexes");
        out.println("         and index.json, but not reverse summaries.");
        out.println("  step4  Reads method-call-summary.jsonl from --output and writes");
        out.println("         method-call-reverse-summary.jsonl. It does not read .class files.");
        out.println();
        out.println("Options:");
        out.println("  --input <path>");
        out.println("      Input class directory, single .class file, or .jar file. Required except");
        out.println("      for --phase step4.");
        out.println("  --output <dir>");
        out.println("      Output index directory. Default: .java-class-index.");
        out.println("  --step1-output <dir|binary-names.jsonl>");
        out.println("      Binary-name set produced by step1. Required for step2 and step3. May be");
        out.println("      specified multiple times for split inputs.");
        out.println("  --exclude-package <binary.package.*>");
        out.println("      Exclude matching classes from generated indexes. This affects classes,");
        out.println("      sources, symbols, dependencies, method-calls, method-call summaries, and");
        out.println("      per-class JSON dependencies/calls. Use this for shaded libraries or");
        out.println("      generated packages that should not participate in the index.");
        out.println("  --exclude-call-package <binary.package.*>");
        out.println("      Keep matching classes/symbols/dependencies, but remove method-call edges");
        out.println("      where fromClass or toClass matches the package.");
        out.println();
        out.println("Large-system guidance:");
        out.println("  Line-oriented files grow with classes, members, dependencies, and bytecode");
        out.println("  calls. For large systems, decide package exclusions before indexing and pass");
        out.println("  the same --exclude-package values to step1, step2, and step3. Quote wildcard");
        out.println("  patterns in shells, for example: --exclude-package 'org.objectweb.*'.");
        out.println();
        out.println("Examples:");
        out.println("  miku-javaclass2json index --input target/classes --output .java-class-index");
        out.println("  miku-javaclass2json index --input app.jar --output .java-class-index --exclude-package 'org.objectweb.*'");
        out.println("  miku-javaclass2json index --phase step4 --output .java-class-index");
    }

    private static String version() {
        String packageVersion = MikuJavaclass2jsonCli.class.getPackage().getImplementationVersion();
        if (packageVersion != null && !packageVersion.trim().isEmpty()) {
            return packageVersion;
        }
        Properties properties = new Properties();
        InputStream in = MikuJavaclass2jsonCli.class.getResourceAsStream("version.properties");
        if (in != null) {
            try {
                properties.load(in);
                String resourceVersion = properties.getProperty("version");
                if (resourceVersion != null && !resourceVersion.trim().isEmpty()
                        && !resourceVersion.startsWith("${")) {
                    return resourceVersion;
                }
            } catch (IOException ex) {
                return "development";
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    // Ignore close failure while reporting a best-effort version.
                }
            }
        }
        return "development";
    }
}
