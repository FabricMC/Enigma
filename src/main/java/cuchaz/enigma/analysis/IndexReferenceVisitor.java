package cuchaz.enigma.analysis;

import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.entry.ClassEntry;
import cuchaz.enigma.mapping.entry.MethodDefEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class IndexReferenceVisitor extends ClassVisitor {
	private final JarIndex index;
	private ClassEntry classEntry;

	public IndexReferenceVisitor(JarIndex index, int api) {
		super(api);
		this.index = index;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = new ClassEntry(name);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(classEntry, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		return new Method(this.index, entry, this.api);
	}

	private class Method extends MethodVisitor {
		private final JarIndex index;
		private final MethodDefEntry callerEntry;

		public Method(JarIndex index, MethodDefEntry callerEntry, int api) {
			super(api);
			this.index = index;
			this.callerEntry = callerEntry;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			this.index.indexFieldAccess(callerEntry, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			this.index.indexMethodCall(callerEntry, owner, name, desc);
		}
	}
}
