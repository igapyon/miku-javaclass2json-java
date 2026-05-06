package jp.igapyon.mikujavaclass2json.internal;

public class MemberInfo {
    public String name;
    public String descriptor;
    public int accessFlags;

    public MemberInfo(String name, String descriptor, int accessFlags) {
        this.name = name;
        this.descriptor = descriptor;
        this.accessFlags = accessFlags;
    }
}
