package jp.igapyon.mikujavaclass2json.cli;

import java.io.PrintStream;
import java.nio.file.Paths;

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
            ClassIndexResult result = new ClassIndexGenerator().generate(options);
            out.println("indexed classes: " + result.getClassCount());
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
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        if (options.getInput() == null) {
            throw new IllegalArgumentException("--input is required");
        }
        return options;
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
    }
}
