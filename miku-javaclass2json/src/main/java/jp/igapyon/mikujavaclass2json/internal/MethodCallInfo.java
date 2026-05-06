package jp.igapyon.mikujavaclass2json.internal;

public class MethodCallInfo {
    public final String fromClass;
    public final String fromMethod;
    public final String fromDescriptor;
    public final String toClass;
    public final String toMethod;
    public final String toDescriptor;
    public final String opcode;
    public final boolean interfaceCall;

    public MethodCallInfo(String fromClass, String fromMethod, String fromDescriptor, String toClass, String toMethod, String toDescriptor,
            String opcode, boolean interfaceCall) {
        this.fromClass = fromClass;
        this.fromMethod = fromMethod;
        this.fromDescriptor = fromDescriptor;
        this.toClass = toClass;
        this.toMethod = toMethod;
        this.toDescriptor = toDescriptor;
        this.opcode = opcode;
        this.interfaceCall = interfaceCall;
    }
}
