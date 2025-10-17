package cuchaz.enigma.analysis.index;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cuchaz.enigma.analysis.BetterAnalyzerAdapter;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;

public class IndexReferenceVisitor extends ClassVisitor {
	private final JarIndexer indexer;
	private ClassEntry classEntry;
	private String className;

	public IndexReferenceVisitor(JarIndexer indexer, int api) {
		super(api);
		this.indexer = indexer;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		classEntry = new ClassEntry(name);
		className = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(classEntry, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		return new IndexReferenceMethodVisitor(api, className, access, name, desc, entry, indexer);
	}

	private static class IndexReferenceMethodVisitor extends BetterAnalyzerAdapter {
		private final MethodDefEntry callerEntry;
		private final JarIndexer indexer;

		IndexReferenceMethodVisitor(int api, String owner, int access, String name, String descriptor, MethodDefEntry callerEntry, JarIndexer indexer) {
			super(api, owner, access, name, descriptor, null);
			this.callerEntry = callerEntry;
			this.indexer = indexer;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			switch (opcode) {
			case Opcodes.GETSTATIC, Opcodes.PUTSTATIC -> indexer.indexFieldReference(callerEntry, FieldEntry.parse(owner, name, descriptor), ReferenceTargetType.none());
			case Opcodes.GETFIELD -> indexer.indexFieldReference(callerEntry, FieldEntry.parse(owner, name, descriptor), getReferenceTargetType(0));
			case Opcodes.PUTFIELD -> indexer.indexFieldReference(callerEntry, FieldEntry.parse(owner, name, descriptor), getReferenceTargetType(Type.getType(descriptor).getSize()));
			}

			super.visitFieldInsn(opcode, owner, name, descriptor);
		}

		@Override
		public void visitLdcInsn(Object value) {
			if (value instanceof Type type && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
				if (type.getSort() == Type.ARRAY) {
					type = type.getElementType();
				}

				indexer.indexClassReference(callerEntry, ClassEntry.parse(type.getInternalName()), ReferenceTargetType.none());
			}

			super.visitLdcInsn(value);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (opcode == Opcodes.INSTANCEOF || opcode == Opcodes.CHECKCAST) {
				Type classType = Type.getObjectType(type);

				if (classType.getSort() == Type.ARRAY) {
					classType = classType.getElementType();
				}

				indexer.indexClassReference(callerEntry, ClassEntry.parse(classType.getInternalName()), ReferenceTargetType.none());
			}

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			ReferenceTargetType targetType;

			if (opcode == Opcodes.INVOKESTATIC) {
				targetType = ReferenceTargetType.none();
			} else {
				int argSize = (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1;
				targetType = getReferenceTargetType(argSize);
			}

			indexer.indexMethodReference(callerEntry, MethodEntry.parse(owner, name, descriptor), targetType);

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			if ("java/lang/invoke/LambdaMetafactory".equals(bootstrapMethodHandle.getOwner()) && ("metafactory".equals(bootstrapMethodHandle.getName()) || "altMetafactory".equals(bootstrapMethodHandle.getName()))) {
				Type samMethodType = (Type) bootstrapMethodArguments[0];
				Handle implMethod = (Handle) bootstrapMethodArguments[1];
				Type instantiatedMethodType = (Type) bootstrapMethodArguments[2];

				ReferenceTargetType targetType;

				if (implMethod.getTag() != Opcodes.H_GETSTATIC && implMethod.getTag() != Opcodes.H_PUTFIELD && implMethod.getTag() != Opcodes.H_INVOKESTATIC) {
					if (instantiatedMethodType.getArgumentCount() < Type.getArgumentCount(implMethod.getDesc())) {
						if (descriptor.startsWith("(L")) { // is the first parameter of the indy an object type?
							int argSize = (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1;
							targetType = getReferenceTargetType(argSize - 1);
						} else {
							targetType = ReferenceTargetType.none();
						}
					} else {
						targetType = ReferenceTargetType.none(); // no "this" argument
					}
				} else {
					targetType = ReferenceTargetType.none();
				}

				indexer.indexLambda(callerEntry, new Lambda(name, new MethodDescriptor(descriptor), new MethodDescriptor(samMethodType.getDescriptor()), getHandleEntry(implMethod), new MethodDescriptor(instantiatedMethodType.getDescriptor())), targetType);
			}

			super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		}

		private ReferenceTargetType getReferenceTargetType(int stackDepth) {
			if (stack == null) { // Unreachable instruction if ASM is to be trusted
				return ReferenceTargetType.uninitialized();
			}

			if (stackDepth >= stack.size()) {
				throw new IllegalStateException("Stack depth " + stackDepth + " is higher than the stack: " + stackValuesToString(stack) + " in method " + callerEntry);
			}

			Object stackValue = stack.get(stack.size() - 1 - stackDepth);

			if (stackValue.equals(Opcodes.UNINITIALIZED_THIS) || stackValue.equals(Opcodes.NULL) || stackValue instanceof Label) {
				return ReferenceTargetType.uninitialized();
			}

			if (!(stackValue instanceof String type)) {
				throw new IllegalStateException("Illegal stack value in method " + callerEntry + ": " + stackValuesToString(List.of(stackValue)));
			}

			if (type.startsWith("[")) {
				// array type
				return ReferenceTargetType.classType(new ClassEntry("java/lang/Object"));
			} else {
				return ReferenceTargetType.classType(new ClassEntry(type));
			}
		}

		private static String stackValuesToString(List<Object> stack) {
			StringBuilder result = new StringBuilder("[");
			boolean first = true;

			for (Object stackValue : stack) {
				if (first) {
					first = false;
				} else {
					result.append(", ");
				}

				if (stackValue instanceof String str) {
					result.append(str);
				} else if (stackValue instanceof Integer i) {
					result.append("TIFDJNU".charAt(i));
				} else if (stackValue instanceof Label) {
					result.append('U');
				} else {
					throw new AssertionError("Illegal stack value type: " + stackValue.getClass().getName());
				}
			}

			return result.append(']').toString();
		}

		private static ParentedEntry<?> getHandleEntry(Handle handle) {
			return switch (handle.getTag()) {
			case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC ->
					FieldEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
			case Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESPECIAL, Opcodes.H_INVOKESTATIC,
				Opcodes.H_INVOKEVIRTUAL, Opcodes.H_NEWINVOKESPECIAL ->
					MethodEntry.parse(handle.getOwner(), handle.getName(), handle.getDesc());
			default -> throw new RuntimeException("Invalid handle tag " + handle.getTag());
			};
		}
	}
}
