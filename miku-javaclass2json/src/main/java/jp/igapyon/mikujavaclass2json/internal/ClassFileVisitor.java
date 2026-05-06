package jp.igapyon.mikujavaclass2json.internal;

import java.io.IOException;

public interface ClassFileVisitor {
    void visit(ClassFileInfo classInfo, String artifact, String entryName) throws IOException;
}
