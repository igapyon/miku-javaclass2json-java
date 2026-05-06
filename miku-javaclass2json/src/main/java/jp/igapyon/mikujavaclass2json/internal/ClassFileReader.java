package jp.igapyon.mikujavaclass2json.internal;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class ClassFileReader {
    private ClassFileReader() {
    }

    public static ClassFileInfo read(byte[] bytes, String sourceArtifact) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        int magic = input.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Invalid class file: " + sourceArtifact);
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        Object[] constantPool = readConstantPool(input);
        ClassFileInfo info = new ClassFileInfo();
        info.accessFlags = input.readUnsignedShort();
        info.binaryName = className(constantPool, input.readUnsignedShort());
        info.canonicalName = info.binaryName.replace('$', '.');
        info.packageName = packageName(info.binaryName);
        info.simpleName = simpleName(info.canonicalName);
        info.kind = kind(info.accessFlags);
        int superIndex = input.readUnsignedShort();
        info.superClass = superIndex == 0 ? null : className(constantPool, superIndex);
        int interfaceCount = input.readUnsignedShort();
        for (int index = 0; index < interfaceCount; index++) {
            info.interfaces.add(className(constantPool, input.readUnsignedShort()));
        }
        readMembers(input, constantPool, info.fields);
        readMembers(input, constantPool, info.methods);
        info.sourceArtifact = sourceArtifact;
        return info;
    }

    private static Object[] readConstantPool(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        Object[] pool = new Object[count];
        for (int index = 1; index < count; index++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
            case 1:
                pool[index] = input.readUTF();
                break;
            case 3:
            case 4:
                input.readInt();
                break;
            case 5:
            case 6:
                input.readLong();
                index++;
                break;
            case 7:
            case 8:
            case 16:
            case 19:
            case 20:
                pool[index] = Integer.valueOf(input.readUnsignedShort());
                break;
            case 9:
            case 10:
            case 11:
            case 12:
            case 18:
                input.readUnsignedShort();
                input.readUnsignedShort();
                break;
            case 15:
                input.readUnsignedByte();
                input.readUnsignedShort();
                break;
            default:
                throw new IOException("Unsupported constant pool tag: " + tag);
            }
        }
        return pool;
    }

    private static void readMembers(DataInputStream input, Object[] constantPool, java.util.List<MemberInfo> members) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            int accessFlags = input.readUnsignedShort();
            String name = utf8(constantPool, input.readUnsignedShort());
            String descriptor = utf8(constantPool, input.readUnsignedShort());
            members.add(new MemberInfo(name, descriptor, accessFlags));
            skipAttributes(input);
        }
    }

    private static void skipAttributes(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            input.readUnsignedShort();
            int length = input.readInt();
            int skipped = 0;
            while (skipped < length) {
                skipped += input.skipBytes(length - skipped);
            }
        }
    }

    private static String className(Object[] constantPool, int classIndex) {
        int nameIndex = ((Integer) constantPool[classIndex]).intValue();
        return utf8(constantPool, nameIndex).replace('/', '.');
    }

    private static String utf8(Object[] constantPool, int index) {
        return (String) constantPool[index];
    }

    private static String packageName(String binaryName) {
        int index = binaryName.lastIndexOf('.');
        return index < 0 ? "" : binaryName.substring(0, index);
    }

    private static String simpleName(String canonicalName) {
        int index = canonicalName.lastIndexOf('.');
        return index < 0 ? canonicalName : canonicalName.substring(index + 1);
    }

    private static String kind(int accessFlags) {
        if ((accessFlags & 0x2000) != 0) {
            return "annotation";
        }
        if ((accessFlags & 0x0200) != 0) {
            return "interface";
        }
        if ((accessFlags & 0x4000) != 0) {
            return "enum";
        }
        return "class";
    }
}
