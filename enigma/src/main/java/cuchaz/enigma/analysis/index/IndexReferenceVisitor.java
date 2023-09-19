package cuchaz.enigma.analysis.index;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import cuchaz.enigma.analysis.IndexSimpleVerifier;
import cuchaz.enigma.analysis.InterpreterPair;
import cuchaz.enigma.analysis.MethodNodeWithAction;
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
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private ClassEntry classEntry;
	private String className;

	public IndexReferenceVisitor(JarIndexer indexer, EntryIndex entryIndex, InheritanceIndex inheritanceIndex, int api) {
		super(api);
		this.indexer = indexer;
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		classEntry = new ClassEntry(name);
		className = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = new MethodDefEntry(classEntry, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		return new MethodNodeWithAction(api, access, name, desc, signature, exceptions, methodNode -> {
			try {
				new Analyzer<>(new MethodInterpreter(entry, indexer, entryIndex, inheritanceIndex)).analyze(className, methodNode);
			} catch (AnalyzerException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static class MethodInterpreter extends InterpreterPair<BasicValue, SourceValue> {
		private final MethodDefEntry callerEntry;
		private final JarIndexer indexer;

		MethodInterpreter(MethodDefEntry callerEntry, JarIndexer indexer, EntryIndex entryIndex, InheritanceIndex inheritanceIndex) {
			super(new IndexSimpleVerifier(entryIndex, inheritanceIndex), new SourceInterpreter());
			this.callerEntry = callerEntry;
			this.indexer = indexer;
		}

		@Override
		public PairValue<BasicValue, SourceValue> newOperation(AbstractInsnNode insn) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.GETSTATIC) {
				FieldInsnNode field = (FieldInsnNode) insn;
				indexer.indexFieldReference(callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.LDC) {
				LdcInsnNode ldc = (LdcInsnNode) insn;

				if (ldc.getType() == Type.ARRAY && ldc.cst instanceof Type type) {
					String className = type.getClassName().replace(".", "/");
					indexer.indexClassReference(callerEntry, ClassEntry.parse(className), ReferenceTargetType.none());
				}
			}

			return super.newOperation(insn);
		}

		@Override
		public PairValue<BasicValue, SourceValue> unaryOperation(AbstractInsnNode insn, PairValue<BasicValue, SourceValue> value) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.PUTSTATIC) {
				FieldInsnNode field = (FieldInsnNode) insn;
				indexer.indexFieldReference(callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.GETFIELD) {
				FieldInsnNode field = (FieldInsnNode) insn;
				indexer.indexFieldReference(callerEntry, FieldEntry.parse(field.owner, field.name, field.desc), getReferenceTargetType(value, insn));
			}

			if (insn.getOpcode() == Opcodes.INSTANCEOF) {
				TypeInsnNode type = (TypeInsnNode) insn;
				// Note: type.desc is actually the name
				indexer.indexClassReference(callerEntry, ClassEntry.parse(type.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.CHECKCAST) {
				TypeInsnNode type = (TypeInsnNode) insn;
				indexer.indexClassReference(callerEntry, ClassEntry.parse(type.desc), ReferenceTargetType.none());
			}

			return super.unaryOperation(insn, value);
		}

		@Override
		public PairValue<BasicValue, SourceValue> binaryOperation(AbstractInsnNode insn, PairValue<BasicValue, SourceValue> value1, PairValue<BasicValue, SourceValue> value2) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.PUTFIELD) {
				FieldInsnNode field = (FieldInsnNode) insn;
				FieldEntry fieldEntry = FieldEntry.parse(field.owner, field.name, field.desc);
				indexer.indexFieldReference(callerEntry, fieldEntry, ReferenceTargetType.none());
			}

			return super.binaryOperation(insn, value1, value2);
		}

		@Override
		public PairValue<BasicValue, SourceValue> naryOperation(AbstractInsnNode insn, List<? extends PairValue<BasicValue, SourceValue>> values) throws AnalyzerException {
			if (insn.getOpcode() == Opcodes.INVOKEINTERFACE || insn.getOpcode() == Opcodes.INVOKESPECIAL || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				indexer.indexMethodReference(callerEntry, MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc), getReferenceTargetType(values.get(0), insn));
			}

			if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				indexer.indexMethodReference(callerEntry, MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc), ReferenceTargetType.none());
			}

			if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
				InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;
				List<AbstractInsnNode> args = values.stream().map(v -> v.right.insns.stream().findFirst().orElseThrow(AssertionError::new)).toList();

				if ("java/lang/invoke/LambdaMetafactory".equals(invokeDynamicInsn.bsm.getOwner()) && "metafactory".equals(invokeDynamicInsn.bsm.getName())) {
					Type samMethodType = (Type) invokeDynamicInsn.bsmArgs[0];
					Handle implMethod = (Handle) invokeDynamicInsn.bsmArgs[1];
					Type instantiatedMethodType = (Type) invokeDynamicInsn.bsmArgs[2];

					ReferenceTargetType targetType;

					if (implMethod.getTag() != Opcodes.H_GETSTATIC && implMethod.getTag() != Opcodes.H_PUTFIELD && implMethod.getTag() != Opcodes.H_INVOKESTATIC) {
						if (instantiatedMethodType.getArgumentTypes().length < Type.getArgumentTypes(implMethod.getDesc()).length) {
							targetType = getReferenceTargetType(values.get(0), insn);
						} else {
							targetType = ReferenceTargetType.none(); // no "this" argument
						}
					} else {
						targetType = ReferenceTargetType.none();
					}

					indexer.indexLambda(callerEntry, new Lambda(invokeDynamicInsn.name, new MethodDescriptor(invokeDynamicInsn.desc), new MethodDescriptor(samMethodType.getDescriptor()), getHandleEntry(implMethod), new MethodDescriptor(instantiatedMethodType.getDescriptor())), targetType);
				}
			}

			return super.naryOperation(insn, values);
		}

		private ReferenceTargetType getReferenceTargetType(PairValue<BasicValue, SourceValue> target, AbstractInsnNode insn) throws AnalyzerException {
			if (target.left == BasicValue.UNINITIALIZED_VALUE) {
				return ReferenceTargetType.uninitialized();
			}

			if (target.left.getType().getSort() == Type.OBJECT) {
				return ReferenceTargetType.classType(new ClassEntry(target.left.getType().getInternalName()));
			}

			if (target.left.getType().getSort() == Type.ARRAY) {
				return ReferenceTargetType.classType(new ClassEntry("java/lang/Object"));
			}

			throw new AnalyzerException(insn, "called method on or accessed field of non-object type");
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
	}
}
