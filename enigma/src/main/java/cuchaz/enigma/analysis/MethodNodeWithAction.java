package cuchaz.enigma.analysis;

import java.util.function.Consumer;

import org.objectweb.asm.tree.MethodNode;

public class MethodNodeWithAction extends MethodNode {
	private final Consumer<MethodNode> action;

	public MethodNodeWithAction(int api, int access, String name, String descriptor, String signature, String[] exceptions, Consumer<MethodNode> action) {
		super(api, access, name, descriptor, signature, exceptions);
		this.action = action;
	}

	@Override
	public void visitEnd() {
		action.accept(this);
	}
}
