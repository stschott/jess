package de.upb.sse.jess.comparison;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

public class CodeOnlyTextifier extends Textifier {

    public CodeOnlyTextifier() {
        super(Opcodes.ASM9);
    }

    @Override
    public Textifier visitAnnotation(final String descriptor, final boolean visible) {
        // do not print anything
        return createTextifier();
    }

    @Override
    public Textifier visitParameterAnnotation(final int parameter, final String descriptor, final boolean visible) {
        // do not print anything
        return createTextifier();
    }

    @Override
    public Textifier visitAnnotableParameterCount(int parameterCount, boolean visible) {
        // do not print anything
        return createTextifier();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // do not print anything
    }

}
