package jp.igapyon.mikujavaclass2json.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class JsonFiles {
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter WARNING_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");

    private JsonFiles() {
    }

    public static Path writeClassJson(Path outputDirectory, ClassFileInfo info, List<String> noDescendPackages) throws IOException {
        Path file = classJsonPath(outputDirectory, info);
        Files.createDirectories(file.getParent());
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            writer.write("{\n");
            property(writer, "schemaVersion", "java-class-index-class-v1", 1, true);
            writer.write("  \"identity\": {\n");
            property(writer, "binaryName", info.binaryName, 2, true);
            property(writer, "canonicalName", info.canonicalName, 2, true);
            property(writer, "packageName", info.packageName, 2, true);
            property(writer, "simpleName", info.simpleName, 2, true);
            property(writer, "kind", info.kind, 2, true);
            property(writer, "sourceArtifact", info.sourceArtifact, 2, false);
            writer.write("  },\n");
            writer.write("  \"inheritance\": {\n");
            property(writer, "superClass", info.superClass, 2, true);
            stringArray(writer, "interfaces", info.interfaces, 2, false);
            writer.write("  },\n");
            members(writer, "fields", info.fields);
            writer.write(",\n");
            methods(writer, info);
            writer.write(",\n");
            writer.write("  \"dependencies\": [\n");
            List<String> dependencies = info.dependencies();
            for (int index = 0; index < dependencies.size(); index++) {
                String dependency = dependencies.get(index);
                writer.write("    {\"to\":\"" + escape(dependency) + "\",\"targetKind\":\"" + targetKind(dependency, noDescendPackages) + "\"}");
                writer.write(index + 1 < dependencies.size() ? ",\n" : "\n");
            }
            writer.write("  ]\n");
            writer.write("}\n");
        } finally {
            writer.close();
        }
        return file;
    }

    public static Path classJsonPath(Path outputDirectory, ClassFileInfo info) {
        return outputDirectory.resolve("classes").resolve(info.binaryName.replace('.', '/') + ".json");
    }

    public static Path writeIndexJson(Path outputDirectory, int classCount, int warningCount) throws IOException {
        Path file = outputDirectory.resolve("index.json");
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            writer.write("{\n");
            property(writer, "schemaVersion", "java-class-index-v1", 1, true);
            writer.write("  \"classCount\": " + classCount + ",\n");
            writer.write("  \"warningCount\": " + warningCount + ",\n");
            property(writer, "classesIndex", "classes.jsonl", 1, true);
            property(writer, "symbolsIndex", "symbols.jsonl", 1, true);
            property(writer, "dependenciesIndex", "dependencies.jsonl", 1, true);
            property(writer, "methodCallsIndex", "method-calls.jsonl", 1, true);
            property(writer, "sourcesIndex", "sources.jsonl", 1, true);
            property(writer, "warningsLog", "warnings.log", 1, false);
            writer.write("}\n");
        } finally {
            writer.close();
        }
        return file;
    }

    public static String classIndexLine(ClassFileInfo info) {
        return "{\"binaryName\":\"" + escape(info.binaryName) + "\",\"path\":\"classes/" + escape(info.binaryName.replace('.', '/')) + ".json\"}";
    }

    public static String symbolLines(ClassFileInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"kind\":\"class\",\"binaryName\":\"").append(escape(info.binaryName)).append("\"}\n");
        for (MemberInfo field : info.fields) {
            builder.append("{\"kind\":\"field\",\"class\":\"").append(escape(info.binaryName)).append("\",\"name\":\"")
                    .append(escape(field.name)).append("\"}\n");
        }
        for (MemberInfo method : info.methods) {
            builder.append("{\"kind\":\"method\",\"class\":\"").append(escape(info.binaryName)).append("\",\"name\":\"")
                    .append(escape(method.name)).append("\",\"descriptor\":\"").append(escape(method.descriptor)).append("\"}\n");
        }
        return builder.toString();
    }

    public static String dependencyLines(ClassFileInfo info, List<String> noDescendPackages) {
        StringBuilder builder = new StringBuilder();
        for (String dependency : info.dependencies()) {
            builder.append("{\"from\":\"").append(escape(info.binaryName)).append("\",\"to\":\"").append(escape(dependency))
                    .append("\",\"targetKind\":\"").append(targetKind(dependency, noDescendPackages)).append("\"}\n");
        }
        return builder.toString();
    }

    public static String methodCallLines(ClassFileInfo info) {
        StringBuilder builder = new StringBuilder();
        for (MethodCallInfo call : info.methodCalls) {
            builder.append("{\"fromClass\":\"").append(escape(call.fromClass)).append("\",\"fromMethod\":\"")
                    .append(escape(call.fromMethod)).append("\",\"fromDescriptor\":\"").append(escape(call.fromDescriptor))
                    .append("\",\"toClass\":\"").append(escape(call.toClass)).append("\",\"toMethod\":\"")
                    .append(escape(call.toMethod)).append("\",\"toDescriptor\":\"").append(escape(call.toDescriptor))
                    .append("\",\"opcode\":\"").append(escape(call.opcode)).append("\",\"interfaceCall\":")
                    .append(call.interfaceCall).append("}\n");
        }
        return builder.toString();
    }

    public static String sourceLine(ClassFileInfo info, String artifact, String entryName) {
        return "{\"binaryName\":\"" + escape(info.binaryName) + "\",\"artifact\":\"" + escape(artifact) + "\",\"entryName\":\""
                + escape(entryName) + "\"}";
    }

    public static String warningLine(ClassFileInfo info, String artifact, String entryName, String message) {
        String timestamp = ZonedDateTime.now(JST).format(WARNING_TIME_FORMAT);
        return timestamp + " warning: " + message + ": binaryName=" + info.binaryName + " artifact=" + artifact + " entryName=" + entryName;
    }

    public static Path writeSymbolsJsonl(Path outputDirectory, List<ClassFileInfo> classes) throws IOException {
        Path file = outputDirectory.resolve("symbols.jsonl");
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            for (ClassFileInfo info : classes) {
                writer.write("{\"kind\":\"class\",\"binaryName\":\"" + escape(info.binaryName) + "\"}\n");
                for (MemberInfo field : info.fields) {
                    writer.write("{\"kind\":\"field\",\"class\":\"" + escape(info.binaryName) + "\",\"name\":\"" + escape(field.name) + "\"}\n");
                }
                for (MemberInfo method : info.methods) {
                    writer.write("{\"kind\":\"method\",\"class\":\"" + escape(info.binaryName) + "\",\"name\":\"" + escape(method.name) + "\",\"descriptor\":\"" + escape(method.descriptor) + "\"}\n");
                }
            }
        } finally {
            writer.close();
        }
        return file;
    }

    public static Path writeDependenciesJsonl(Path outputDirectory, List<ClassFileInfo> classes, List<String> noDescendPackages) throws IOException {
        Path file = outputDirectory.resolve("dependencies.jsonl");
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            for (ClassFileInfo info : classes) {
                for (String dependency : info.dependencies()) {
                    writer.write("{\"from\":\"" + escape(info.binaryName) + "\",\"to\":\"" + escape(dependency) + "\",\"targetKind\":\"" + targetKind(dependency, noDescendPackages) + "\"}\n");
                }
            }
        } finally {
            writer.close();
        }
        return file;
    }

    private static void members(BufferedWriter writer, String name, List<MemberInfo> members) throws IOException {
        writer.write("  \"" + name + "\": [\n");
        for (int index = 0; index < members.size(); index++) {
            MemberInfo member = members.get(index);
            writer.write("    {\"name\":\"" + escape(member.name) + "\",\"descriptor\":\"" + escape(member.descriptor) + "\"}");
            writer.write(index + 1 < members.size() ? ",\n" : "\n");
        }
        writer.write("  ]");
    }

    private static void methods(BufferedWriter writer, ClassFileInfo info) throws IOException {
        writer.write("  \"methods\": [\n");
        for (int index = 0; index < info.methods.size(); index++) {
            MemberInfo method = info.methods.get(index);
            writer.write("    {\"name\":\"" + escape(method.name) + "\",\"descriptor\":\"" + escape(method.descriptor) + "\",\"calls\":[");
            boolean firstCall = true;
            for (MethodCallInfo call : info.methodCalls) {
                if (method.name.equals(call.fromMethod) && method.descriptor.equals(call.fromDescriptor)) {
                    if (!firstCall) {
                        writer.write(",");
                    }
                    writer.write("{\"toClass\":\"" + escape(call.toClass) + "\",\"toMethod\":\"" + escape(call.toMethod)
                            + "\",\"toDescriptor\":\"" + escape(call.toDescriptor) + "\",\"opcode\":\"" + escape(call.opcode)
                            + "\",\"interfaceCall\":" + call.interfaceCall + "}");
                    firstCall = false;
                }
            }
            writer.write("]}");
            writer.write(index + 1 < info.methods.size() ? ",\n" : "\n");
        }
        writer.write("  ]");
    }

    private static void property(BufferedWriter writer, String name, String value, int indent, boolean comma) throws IOException {
        spaces(writer, indent);
        writer.write("\"" + name + "\": ");
        writer.write(value == null ? "null" : "\"" + escape(value) + "\"");
        writer.write(comma ? ",\n" : "\n");
    }

    private static void stringArray(BufferedWriter writer, String name, List<String> values, int indent, boolean comma) throws IOException {
        spaces(writer, indent);
        writer.write("\"" + name + "\": [");
        for (int index = 0; index < values.size(); index++) {
            writer.write("\"" + escape(values.get(index)) + "\"");
            if (index + 1 < values.size()) {
                writer.write(", ");
            }
        }
        writer.write("]");
        writer.write(comma ? ",\n" : "\n");
    }

    private static void spaces(BufferedWriter writer, int indent) throws IOException {
        for (int index = 0; index < indent; index++) {
            writer.write("  ");
        }
    }

    private static String targetKind(String binaryName, List<String> noDescendPackages) {
        for (String pattern : noDescendPackages) {
            String prefix = pattern.endsWith(".*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            if (binaryName.startsWith(prefix)) {
                return binaryName.startsWith("java.") || binaryName.startsWith("jdk.") || binaryName.startsWith("sun.") || binaryName.startsWith("com.sun.")
                        ? "external-platform" : "external-api";
            }
        }
        return "internal";
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
            case '\\':
                builder.append("\\\\");
                break;
            case '"':
                builder.append("\\\"");
                break;
            case '\n':
                builder.append("\\n");
                break;
            case '\r':
                builder.append("\\r");
                break;
            case '\t':
                builder.append("\\t");
                break;
            default:
                builder.append(ch);
                break;
            }
        }
        return builder.toString();
    }
}
