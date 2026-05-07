package jp.igapyon.mikujavaclass2json.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonFiles {
    public static final String CLASS_JSON_DIRECTORY = "cls";
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter WARNING_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");

    private JsonFiles() {
    }

    public static Path writeClassJson(Path outputDirectory, ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames)
            throws IOException {
        return writeClassJson(outputDirectory, info, noDescendPackages, indexedBinaryNames, java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList());
    }

    public static Path writeClassJson(Path outputDirectory, ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, List<String> excludeCallPackages) throws IOException {
        return writeClassJson(outputDirectory, java.util.Collections.singletonList(info), noDescendPackages, indexedBinaryNames, excludePackages,
                excludeCallPackages);
    }

    public static Path writeClassJson(Path outputDirectory, List<ClassFileInfo> classGroup, List<String> noDescendPackages,
            Set<String> indexedBinaryNames, List<String> excludePackages, List<String> excludeCallPackages) throws IOException {
        if (classGroup == null || classGroup.isEmpty()) {
            throw new IllegalArgumentException("classGroup is required");
        }
        ClassFileInfo primary = primaryClass(classGroup);
        Path file = classJsonPath(outputDirectory, primary);
        Files.createDirectories(file.getParent());
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            writer.write("{\n");
            property(writer, "schemaVersion", "java-class-index-class-v1", 1, true);
            classFields(writer, primary, noDescendPackages, indexedBinaryNames, excludePackages, excludeCallPackages, 1, true);
            nestedClasses(writer, primary, classGroup, noDescendPackages, indexedBinaryNames, excludePackages, excludeCallPackages, 1);
            writer.write("}\n");
        } finally {
            writer.close();
        }
        return file;
    }

    public static Path classJsonPath(Path outputDirectory, ClassFileInfo info) {
        return outputDirectory.resolve(CLASS_JSON_DIRECTORY).resolve(topLevelBinaryName(info.binaryName).replace('.', '/') + ".json");
    }

    public static Path writeIndexJson(Path outputDirectory, int classCount, int warningCount) throws IOException {
        return writeIndexJson(outputDirectory, classCount, warningCount, true);
    }

    public static Path writeIndexJson(Path outputDirectory, int classCount, int warningCount, boolean includeReverseIndexes) throws IOException {
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
            property(writer, "methodCallSummaryIndex", "method-call-summary.jsonl", 1, true);
            if (includeReverseIndexes) {
                property(writer, "methodCallReverseSummaryIndex", "method-call-reverse-summary.jsonl", 1, true);
            }
            property(writer, "sourcesIndex", "sources.jsonl", 1, true);
            property(writer, "warningsLog", "warnings.log", 1, false);
            writer.write("}\n");
        } finally {
            writer.close();
        }
        return file;
    }

    public static String classIndexLine(ClassFileInfo info) {
        return "{\"binaryName\":\"" + escape(info.binaryName) + "\",\"path\":\"" + CLASS_JSON_DIRECTORY + "/"
                + escape(topLevelBinaryName(info.binaryName).replace('.', '/')) + ".json\"}";
    }

    public static String topLevelBinaryName(String binaryName) {
        int index = binaryName.indexOf('$');
        return index < 0 ? binaryName : binaryName.substring(0, index);
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

    public static String dependencyLines(ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages) {
        StringBuilder builder = new StringBuilder();
        for (String dependency : info.dependencies()) {
            if (matchesPackage(dependency, excludePackages)) {
                continue;
            }
            builder.append("{\"from\":\"").append(escape(info.binaryName)).append("\",\"to\":\"").append(escape(dependency))
                    .append("\",\"targetKind\":\"").append(targetKind(dependency, noDescendPackages, indexedBinaryNames)).append("\"}\n");
        }
        return builder.toString();
    }

    public static String methodCallLines(ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, List<String> excludeCallPackages) {
        StringBuilder builder = new StringBuilder();
        for (MethodCallInfo call : info.methodCalls) {
            if (isExcludedCall(call, excludePackages, excludeCallPackages)) {
                continue;
            }
            String targetKind = targetKind(call.toClass, noDescendPackages, indexedBinaryNames);
            builder.append("{\"fromClass\":\"").append(escape(call.fromClass)).append("\",\"fromMethod\":\"")
                    .append(escape(call.fromMethod)).append("\",\"fromDescriptor\":\"").append(escape(call.fromDescriptor))
                    .append("\",\"toClass\":\"").append(escape(call.toClass)).append("\",\"toMethod\":\"")
                    .append(escape(call.toMethod)).append("\",\"toDescriptor\":\"").append(escape(call.toDescriptor))
                    .append("\",\"targetKind\":\"").append(escape(targetKind)).append("\",\"opcode\":\"").append(escape(call.opcode))
                    .append("\",\"interfaceCall\":").append(call.interfaceCall).append("}\n");
        }
        return builder.toString();
    }

    public static String methodCallSummaryLines(ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, List<String> excludeCallPackages) {
        StringBuilder builder = new StringBuilder();
        for (SummaryCall summary : summaryCalls(info, noDescendPackages, indexedBinaryNames, excludePackages, excludeCallPackages).values()) {
            MethodCallInfo call = summary.call;
            builder.append("{\"fromClass\":\"").append(escape(call.fromClass)).append("\",\"fromMethod\":\"")
                    .append(escape(call.fromMethod)).append("\",\"fromDescriptor\":\"").append(escape(call.fromDescriptor))
                    .append("\",\"toClass\":\"").append(escape(call.toClass)).append("\",\"toMethod\":\"")
                    .append(escape(call.toMethod)).append("\",\"toDescriptor\":\"").append(escape(call.toDescriptor))
                    .append("\",\"targetKind\":\"").append(escape(summary.targetKind)).append("\",\"opcode\":\"").append(escape(call.opcode))
                    .append("\",\"interfaceCall\":").append(call.interfaceCall).append(",\"count\":").append(summary.count).append("}\n");
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

    public static Path writeMethodCallReverseSummaryJsonl(Path outputDirectory) throws IOException {
        Path input = outputDirectory.resolve("method-call-summary.jsonl");
        Path output = outputDirectory.resolve("method-call-reverse-summary.jsonl");
        BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
        BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                writer.write(methodCallReverseSummaryLine(line));
                writer.newLine();
            }
        } finally {
            try {
                writer.close();
            } finally {
                reader.close();
            }
        }
        return output;
    }

    public static Path writeDependenciesJsonl(Path outputDirectory, List<ClassFileInfo> classes, List<String> noDescendPackages) throws IOException {
        Path file = outputDirectory.resolve("dependencies.jsonl");
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            for (ClassFileInfo info : classes) {
                for (String dependency : info.dependencies()) {
                    writer.write("{\"from\":\"" + escape(info.binaryName) + "\",\"to\":\"" + escape(dependency) + "\",\"targetKind\":\""
                            + targetKind(dependency, noDescendPackages, null) + "\"}\n");
                }
            }
        } finally {
            writer.close();
        }
        return file;
    }

    private static void classFields(BufferedWriter writer, ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, List<String> excludeCallPackages, int indent, boolean commaAfterDependencies) throws IOException {
        spaces(writer, indent);
        writer.write("\"identity\": {\n");
        property(writer, "binaryName", info.binaryName, indent + 1, true);
        property(writer, "canonicalName", info.canonicalName, indent + 1, true);
        property(writer, "packageName", info.packageName, indent + 1, true);
        property(writer, "simpleName", info.simpleName, indent + 1, true);
        property(writer, "kind", info.kind, indent + 1, true);
        property(writer, "sourceArtifact", info.sourceArtifact, indent + 1, false);
        spaces(writer, indent);
        writer.write("},\n");
        spaces(writer, indent);
        writer.write("\"inheritance\": {\n");
        property(writer, "superClass", info.superClass, indent + 1, true);
        stringArray(writer, "interfaces", info.interfaces, indent + 1, false);
        spaces(writer, indent);
        writer.write("},\n");
        members(writer, "fields", info.fields, indent);
        writer.write(",\n");
        methods(writer, info, excludePackages, excludeCallPackages, indent);
        writer.write(",\n");
        dependencies(writer, info, noDescendPackages, indexedBinaryNames, excludePackages, indent);
        writer.write(commaAfterDependencies ? ",\n" : "\n");
    }

    private static void nestedClasses(BufferedWriter writer, ClassFileInfo primary, List<ClassFileInfo> classGroup, List<String> noDescendPackages,
            Set<String> indexedBinaryNames, List<String> excludePackages, List<String> excludeCallPackages, int indent) throws IOException {
        spaces(writer, indent);
        writer.write("\"nestedClasses\": [\n");
        boolean first = true;
        for (ClassFileInfo nested : classGroup) {
            if (nested.binaryName.equals(primary.binaryName)) {
                continue;
            }
            if (!first) {
                writer.write(",\n");
            }
            spaces(writer, indent + 1);
            writer.write("{\n");
            classFields(writer, nested, noDescendPackages, indexedBinaryNames, excludePackages, excludeCallPackages, indent + 2, false);
            spaces(writer, indent + 1);
            writer.write("}");
            first = false;
        }
        writer.write(first ? "" : "\n");
        spaces(writer, indent);
        writer.write("]\n");
    }

    private static void dependencies(BufferedWriter writer, ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, int indent) throws IOException {
        spaces(writer, indent);
        writer.write("\"dependencies\": [\n");
        List<String> dependencies = filteredDependencies(info, excludePackages);
        for (int index = 0; index < dependencies.size(); index++) {
            String dependency = dependencies.get(index);
            spaces(writer, indent + 1);
            writer.write("{\"to\":\"" + escape(dependency) + "\",\"targetKind\":\"" + targetKind(dependency, noDescendPackages, indexedBinaryNames)
                    + "\"}");
            writer.write(index + 1 < dependencies.size() ? ",\n" : "\n");
        }
        spaces(writer, indent);
        writer.write("]");
    }

    private static void members(BufferedWriter writer, String name, List<MemberInfo> members, int indent) throws IOException {
        spaces(writer, indent);
        writer.write("\"" + name + "\": [\n");
        for (int index = 0; index < members.size(); index++) {
            MemberInfo member = members.get(index);
            spaces(writer, indent + 1);
            writer.write("{\"name\":\"" + escape(member.name) + "\",\"descriptor\":\"" + escape(member.descriptor) + "\"}");
            writer.write(index + 1 < members.size() ? ",\n" : "\n");
        }
        spaces(writer, indent);
        writer.write("]");
    }

    private static void methods(BufferedWriter writer, ClassFileInfo info, List<String> excludePackages, List<String> excludeCallPackages)
            throws IOException {
        methods(writer, info, excludePackages, excludeCallPackages, 1);
    }

    private static void methods(BufferedWriter writer, ClassFileInfo info, List<String> excludePackages, List<String> excludeCallPackages, int indent)
            throws IOException {
        spaces(writer, indent);
        writer.write("\"methods\": [\n");
        for (int index = 0; index < info.methods.size(); index++) {
            MemberInfo method = info.methods.get(index);
            spaces(writer, indent + 1);
            writer.write("{\"name\":\"" + escape(method.name) + "\",\"descriptor\":\"" + escape(method.descriptor) + "\",\"calls\":[");
            boolean firstCall = true;
            for (MethodCallInfo call : info.methodCalls) {
                if (isExcludedCall(call, excludePackages, excludeCallPackages)) {
                    continue;
                }
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
        spaces(writer, indent);
        writer.write("]");
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

    private static String targetKind(String binaryName, List<String> noDescendPackages, Set<String> indexedBinaryNames) {
        if (indexedBinaryNames != null && indexedBinaryNames.contains(binaryName)) {
            return "internal";
        }
        for (String pattern : noDescendPackages) {
            String prefix = pattern.endsWith(".*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            if (binaryName.startsWith(prefix)) {
                return binaryName.startsWith("java.") || binaryName.startsWith("jdk.") || binaryName.startsWith("sun.") || binaryName.startsWith("com.sun.")
                        ? "external-platform" : "external-api";
            }
        }
        return "external-library";
    }

    private static Map<String, SummaryCall> summaryCalls(ClassFileInfo info, List<String> noDescendPackages, Set<String> indexedBinaryNames,
            List<String> excludePackages, List<String> excludeCallPackages) {
        Map<String, SummaryCall> calls = new LinkedHashMap<String, SummaryCall>();
        for (MethodCallInfo call : info.methodCalls) {
            if (isExcludedCall(call, excludePackages, excludeCallPackages)) {
                continue;
            }
            String targetKind = targetKind(call.toClass, noDescendPackages, indexedBinaryNames);
            if ("external-platform".equals(targetKind) || "external-api".equals(targetKind)) {
                continue;
            }
            String key = call.fromClass + "\n" + call.fromMethod + "\n" + call.fromDescriptor + "\n" + call.toClass + "\n" + call.toMethod + "\n"
                    + call.toDescriptor + "\n" + targetKind + "\n" + call.opcode + "\n" + call.interfaceCall;
            SummaryCall summary = calls.get(key);
            if (summary == null) {
                summary = new SummaryCall(call, targetKind);
                calls.put(key, summary);
            }
            summary.count++;
        }
        return calls;
    }

    public static boolean matchesPackage(String binaryName, List<String> patterns) {
        for (String pattern : patterns) {
            String prefix = pattern.endsWith(".*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            if (binaryName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExcludedCall(MethodCallInfo call, List<String> excludePackages, List<String> excludeCallPackages) {
        return matchesPackage(call.fromClass, excludePackages) || matchesPackage(call.toClass, excludePackages)
                || matchesPackage(call.fromClass, excludeCallPackages) || matchesPackage(call.toClass, excludeCallPackages);
    }

    private static List<String> filteredDependencies(ClassFileInfo info, List<String> excludePackages) {
        java.util.ArrayList<String> dependencies = new java.util.ArrayList<String>();
        for (String dependency : info.dependencies()) {
            if (!matchesPackage(dependency, excludePackages)) {
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    private static ClassFileInfo primaryClass(List<ClassFileInfo> classGroup) {
        String topLevelBinaryName = topLevelBinaryName(classGroup.get(0).binaryName);
        for (ClassFileInfo info : classGroup) {
            if (topLevelBinaryName.equals(info.binaryName)) {
                return info;
            }
        }
        return classGroup.get(0);
    }

    private static String methodCallReverseSummaryLine(String line) {
        String toClass = jsonString(line, "toClass");
        String toMethod = jsonString(line, "toMethod");
        String toDescriptor = jsonString(line, "toDescriptor");
        String fromClass = jsonString(line, "fromClass");
        String fromMethod = jsonString(line, "fromMethod");
        String fromDescriptor = jsonString(line, "fromDescriptor");
        String targetKind = jsonString(line, "targetKind");
        String opcode = jsonString(line, "opcode");
        String interfaceCall = jsonLiteral(line, "interfaceCall");
        String count = jsonLiteral(line, "count");
        return "{\"toClass\":\"" + escape(toClass) + "\",\"toMethod\":\"" + escape(toMethod) + "\",\"toDescriptor\":\""
                + escape(toDescriptor) + "\",\"fromClass\":\"" + escape(fromClass) + "\",\"fromMethod\":\"" + escape(fromMethod)
                + "\",\"fromDescriptor\":\"" + escape(fromDescriptor) + "\",\"targetKind\":\"" + escape(targetKind)
                + "\",\"opcode\":\"" + escape(opcode) + "\",\"interfaceCall\":" + interfaceCall + ",\"count\":" + count + "}";
    }

    private static String jsonString(String line, String name) {
        String marker = "\"" + name + "\":\"";
        int start = line.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int index = start; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (escaped) {
                switch (ch) {
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                default:
                    builder.append(ch);
                    break;
                }
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return builder.toString();
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String jsonLiteral(String line, String name) {
        String marker = "\"" + name + "\":";
        int start = line.indexOf(marker);
        if (start < 0) {
            return "interfaceCall".equals(name) ? "false" : "0";
        }
        start += marker.length();
        int end = start;
        while (end < line.length() && line.charAt(end) != ',' && line.charAt(end) != '}') {
            end++;
        }
        return line.substring(start, end);
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

    private static final class SummaryCall {
        private final MethodCallInfo call;
        private final String targetKind;
        private int count;

        private SummaryCall(MethodCallInfo call, String targetKind) {
            this.call = call;
            this.targetKind = targetKind;
        }
    }
}
