package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.entry.*;
import org.objectweb.asm.*;

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

		private static ParentedEntry<?> getHandleEntry(Handle handle) {
			switch (handle.getTag()) {
				case Opcodes.H_GETFIELD:
				case Opcodes.H_GETSTATIC:
				case Opcodes.H_PUTFIELD:
				case Opcodes.H_PUTSTATIC:
					return FieldEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
				case Opcodes.H_INVOKEINTERFACE:
				case Opcodes.H_INVOKESPECIAL:
				case Opcodes.H_INVOKESTATIC:
				case Opcodes.H_INVOKEVIRTUAL:
				case Opcodes.H_NEWINVOKESPECIAL:
					return MethodEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
			}
			throw new RuntimeException("Invalid handle tag " + handle.getTag());
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			if ("java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner()) && "metafactory".equals(bsm.getName())) {
				Type samMethodType = (Type) bsmArgs[0];
				Handle implMethod = (Handle) bsmArgs[1];
				Type instantiatedMethodType = (Type) bsmArgs[2];

				this.indexer.indexLambda(callerEntry, new Lambda(
					name,
					new MethodDescriptor(desc),
					new MethodDescriptor(samMethodType.getDescriptor()),
					getHandleEntry(implMethod),
					new MethodDescriptor(instantiatedMethodType.getDescriptor())
				));
			}
		}
	}
}
