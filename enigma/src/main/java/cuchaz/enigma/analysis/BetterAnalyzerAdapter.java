package cuchaz.enigma.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

/**
 * An {@link AnalyzerAdapter} that works even if the class wasn't read with {@link ClassReader#EXPAND_FRAMES}.
 */
public class BetterAnalyzerAdapter extends AnalyzerAdapter {
	private final List<Object> lastFrameLocals = new ArrayList<>();
	private final List<Object> lastFrameStack = new ArrayList<>();

	protected BetterAnalyzerAdapter(int api, String owner, int access, String name, String descriptor, @Nullable MethodVisitor methodVisitor) {
		super(api, owner, access, name, descriptor, methodVisitor);

		for (Object local : this.locals) {
			if (!local.equals(Opcodes.TOP)) {
				lastFrameLocals.add(local);
			}
		}
	}

	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		switch (type) {
		case Opcodes.F_NEW -> {
			super.visitFrame(type, numLocal, local, numStack, stack);
			return;
		}
		case Opcodes.F_SAME -> {
			lastFrameStack.clear();
		}
		case Opcodes.F_SAME1 -> {
			lastFrameStack.clear();
			lastFrameStack.add(stack[0]);
		}
		case Opcodes.F_APPEND -> {
			Collections.addAll(lastFrameLocals, local);
			lastFrameStack.clear();
		}
		case Opcodes.F_CHOP -> {
			lastFrameLocals.subList(lastFrameLocals.size() - numLocal, lastFrameLocals.size()).clear();
			lastFrameStack.clear();
		}
		case Opcodes.F_FULL -> {
			lastFrameLocals.clear();
			Collections.addAll(lastFrameLocals, local);
			lastFrameStack.clear();
			Collections.addAll(lastFrameStack, stack);
		}
		default -> {
			throw new AssertionError("Illegal frame type: " + type);
		}
		}

		super.visitFrame(Opcodes.F_NEW, lastFrameLocals.size(), lastFrameLocals.toArray(), lastFrameStack.size(), lastFrameStack.toArray());
	}
}
