package jp.igapyon.mikujavaclass2json.internal;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class ClassFileReader {
    private ClassFileReader() {
    }

    public static ClassFileInfo read(byte[] bytes, String sourceArtifact) throws IOException {
        try {
            final ClassFileInfo info = new ClassFileInfo();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    info.accessFlags = access;
                    info.binaryName = toBinaryName(name);
                    info.canonicalName = info.binaryName.replace('$', '.');
                    info.packageName = packageName(info.binaryName);
                    info.simpleName = simpleName(info.canonicalName);
                    info.kind = kind(access);
                    info.superClass = superName == null ? null : toBinaryName(superName);
                    if (interfaces != null) {
                        for (String interfaceName : interfaces) {
                            info.interfaces.add(toBinaryName(interfaceName));
                        }
                    }
                    info.sourceArtifact = sourceArtifact;
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    info.fields.add(new MemberInfo(name, descriptor, access));
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, final String sourceMethodName, final String sourceMethodDescriptor, String signature,
                        String[] exceptions) {
                    info.methods.add(new MemberInfo(sourceMethodName, sourceMethodDescriptor, access));
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String targetMethodName, String targetMethodDescriptor,
                                boolean isInterface) {
                            info.methodCalls.add(new MethodCallInfo(info.binaryName, sourceMethodName, sourceMethodDescriptor, toBinaryName(owner),
                                    targetMethodName, targetMethodDescriptor, opcodeName(opcode), isInterface));
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String dynamicName, String dynamicDescriptor,
                                org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                            info.methodCalls.add(new MethodCallInfo(info.binaryName, sourceMethodName, sourceMethodDescriptor, "<invokedynamic>",
                                    dynamicName, dynamicDescriptor, "invokedynamic", false));
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return info;
        } catch (RuntimeException ex) {
            throw new IOException("Invalid class file: " + sourceArtifact, ex);
        }
    }

    private static String toBinaryName(String internalName) {
        return internalName == null ? null : internalName.replace('/', '.');
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
        if ((accessFlags & Opcodes.ACC_ANNOTATION) != 0) {
            return "annotation";
        }
        if ((accessFlags & Opcodes.ACC_INTERFACE) != 0) {
            return "interface";
        }
        if ((accessFlags & Opcodes.ACC_ENUM) != 0) {
            return "enum";
        }
        return "class";
    }

    private static String opcodeName(int opcode) {
        switch (opcode) {
        case Opcodes.INVOKEVIRTUAL:
            return "invokevirtual";
        case Opcodes.INVOKESPECIAL:
            return "invokespecial";
        case Opcodes.INVOKESTATIC:
            return "invokestatic";
        case Opcodes.INVOKEINTERFACE:
            return "invokeinterface";
        default:
            return "opcode-" + opcode;
        }
    }
}
