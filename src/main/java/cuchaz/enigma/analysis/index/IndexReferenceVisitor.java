package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IndexReferenceVisitor extends ClassVisitor {
	private final JarIndexer indexer;
	private ClassEntry classEntry;

	public IndexReferenceVisitor(JarIndexer indexer, int api) {
		super(api);
		this.indexer = indexer;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = new ClassEntry(name);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(classEntry, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		return new Method(this.indexer, entry, this.api);
	}

	private static class Method extends MethodVisitor {
		private final JarIndexer indexer;
		private final MethodDefEntry callerEntry;

		public Method(JarIndexer indexer, MethodDefEntry callerEntry, int api) {
			super(api);
			this.indexer = indexer;
			this.callerEntry = callerEntry;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			FieldEntry fieldEntry = FieldEntry.parse(owner, name, desc);
			this.indexer.indexFieldReference(callerEntry, fieldEntry);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			MethodEntry methodEntry = MethodEntry.parse(owner, name, desc);
			this.indexer.indexMethodReference(callerEntry, methodEntry);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			for (Object bsmArg : bsmArgs) {
				if (bsmArg instanceof Handle) {
					Handle handle = (Handle) bsmArg;
					switch (handle.getTag()) {
						case Opcodes.H_GETFIELD:
						case Opcodes.H_GETSTATIC:
						case Opcodes.H_PUTFIELD:
						case Opcodes.H_PUTSTATIC:
							FieldEntry fieldEntry = FieldEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
							this.indexer.indexFieldReference(callerEntry, fieldEntry);
							break;
						case Opcodes.H_INVOKEINTERFACE:
						case Opcodes.H_INVOKESPECIAL:
						case Opcodes.H_INVOKESTATIC:
						case Opcodes.H_INVOKEVIRTUAL:
						case Opcodes.H_NEWINVOKESPECIAL:
							MethodEntry methodEntry = MethodEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
							this.indexer.indexMethodReference(callerEntry, methodEntry);
							break;
					}
				}
			}
		}
	}
}
