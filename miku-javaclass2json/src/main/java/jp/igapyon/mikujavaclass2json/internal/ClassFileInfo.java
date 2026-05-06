package jp.igapyon.mikujavaclass2json.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassFileInfo implements Comparable<ClassFileInfo> {
    public String binaryName;
    public String canonicalName;
    public String packageName;
    public String simpleName;
    public String kind;
    public int accessFlags;
    public String superClass;
    public List<String> interfaces = new ArrayList<String>();
    public List<MemberInfo> fields = new ArrayList<MemberInfo>();
    public List<MemberInfo> methods = new ArrayList<MemberInfo>();
    public List<MethodCallInfo> methodCalls = new ArrayList<MethodCallInfo>();
    public String sourceArtifact;

    public List<String> dependencies() {
        List<String> values = new ArrayList<String>();
        add(values, superClass);
        values.addAll(interfaces);
        for (MemberInfo field : fields) {
            values.addAll(DescriptorTypes.extract(field.descriptor));
        }
        for (MemberInfo method : methods) {
            values.addAll(DescriptorTypes.extract(method.descriptor));
        }
        Collections.sort(values);
        List<String> unique = new ArrayList<String>();
        for (String value : values) {
            if (!binaryName.equals(value) && !unique.contains(value)) {
                unique.add(value);
            }
        }
        return unique;
    }

    private static void add(List<String> values, String value) {
        if (value != null && value.length() > 0) {
            values.add(value);
        }
    }

    @Override
    public int compareTo(ClassFileInfo other) {
        return binaryName.compareTo(other.binaryName);
    }
}
