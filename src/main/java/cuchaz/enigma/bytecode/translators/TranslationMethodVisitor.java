package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.LocalNameGenerator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;
import org.objectweb.asm.*;

import java.util.List;
import java.util.stream.Collectors;

public class TranslationMethodVisitor extends MethodVisitor {
	private final ClassDefEntry ownerEntry;
	private final MethodDefEntry methodEntry;
	private final Translator translator;

	private boolean hasParameterMeta;

	public TranslationMethodVisitor(Translator translator, ClassDefEntry ownerEntry, MethodDefEntry methodEntry, int api, MethodVisitor mv) {
		super(api, mv);
		this.translator = translator;
		this.ownerEntry = ownerEntry;
		this.methodEntry = methodEntry;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		FieldEntry entry = new FieldEntry(new ClassEntry(owner), name, new TypeDescriptor(desc));
		FieldEntry translatedEntry = translator.translate(entry);
		super.visitFieldInsn(opcode, translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		MethodEntry entry = new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc));
		MethodEntry translatedEntry = translator.translate(entry);
		super.visitMethodInsn(opcode, translatedEntry.getParent().getFullName(), translatedEntry.getName(), translatedEntry.getDesc().toString(), itf);
	}

	@Override
	public void visitFrame(int type, int localCount, Object[] locals, int stackCount, Object[] stack) {
		Object[] translatedLocals = this.getTranslatedFrame(locals, localCount);
		Object[] translatedStack = this.getTranslatedFrame(stack, stackCount);
		super.visitFrame(type, localCount, translatedLocals, stackCount, translatedStack);
	}

	private Object[] getTranslatedFrame(Object[] array, int count) {
		if (array == null) {
			return null;
		}
		for (int i = 0; i < count; i++) {
			Object object = array[i];
			if (object instanceof String) {
				String type = (String) object;
				array[i] = translator.translate(new ClassEntry(type)).getFullName();
			}
		}
		return array;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitParameterAnnotation(parameter, typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.translate(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		ClassEntry translatedEntry = translator.translate(new ClassEntry(type));
		super.visitTypeInsn(opcode, translatedEntry.getFullName());
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		MethodDescriptor translatedMethodDesc = translator.translate(new MethodDescriptor(desc));
		Object[] translatedBsmArgs = new Object[bsmArgs.length];
		for (int i = 0; i < bsmArgs.length; i++) {
			translatedBsmArgs[i] = AsmObjectTranslator.translateValue(translator, bsmArgs[i]);
		}
		super.visitInvokeDynamicInsn(name, translatedMethodDesc.toString(), AsmObjectTranslator.translateHandle(translator, bsm), translatedBsmArgs);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(AsmObjectTranslator.translateValue(translator, cst));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		super.visitMultiANewArrayInsn(translator.translate(new TypeDescriptor(desc)).toString(), dims);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (type != null) {
			ClassEntry translatedEntry = translator.translate(new ClassEntry(type));
			super.visitTryCatchBlock(start, end, handler, translatedEntry.getFullName());
		} else {
			super.visitTryCatchBlock(start, end, handler, type);
		}
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		hasParameterMeta = true;

		String translatedSignature = translator.translate(Signature.createTypedSignature(signature)).toString();
		int argumentIndex = methodEntry.getArgumentIndex(ownerEntry, index);

		if (argumentIndex >= 0) {
			LocalVariableDefEntry entry = new LocalVariableDefEntry(methodEntry, index, name, true, new TypeDescriptor(desc));
			LocalVariableDefEntry translatedEntry = translator.translate(entry);
			String translatedName = translatedEntry.getName();

			if (translatedName.equals(entry.getName())) {
				List<TypeDescriptor> arguments = methodEntry.getDesc().getArgumentDescs();
				List<TypeDescriptor> translatedArguments = arguments.stream()
						.map(translator::translate)
						.collect(Collectors.toList());

				boolean argument = argumentIndex < arguments.size();
				if (argument) {
					translatedName = LocalNameGenerator.generateArgumentName(argumentIndex, translatedEntry.getDesc(), translatedArguments);
				} else {
					translatedName = LocalNameGenerator.generateLocalVariableName(argumentIndex, translatedEntry.getDesc());
				}
			}

			super.visitLocalVariable(translatedName, translatedEntry.getDesc().toString(), translatedSignature, start, end, index);
		} else {
			// Handle "this" variable
			TypeDescriptor translatedDesc = translator.translate(new TypeDescriptor(desc));
			super.visitLocalVariable(name, translatedDesc.toString(), translatedSignature, start, end, index);
		}
	}

	@Override
	public void visitEnd() {
		// If we didn't receive any parameter metadata, generate it
		if (!hasParameterMeta) {
			List<TypeDescriptor> arguments = translator.translate(methodEntry.getDesc()).getArgumentDescs();
			int offset = ((methodEntry.getAccess().getFlags() & Opcodes.ACC_ABSTRACT) != 0) ? 1 : 0;

			for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
				LocalVariableEntry entry = new LocalVariableEntry(methodEntry, offset, "", true);
				LocalVariableEntry translatedEntry = translator.translate(entry);
				String translatedName = translatedEntry.getName();
				if (translatedName.equals(entry.getName())) {
					super.visitParameter(LocalNameGenerator.generateArgumentName(argumentIndex, arguments.get(argumentIndex), arguments), 0);
				} else {
					super.visitParameter(translatedName, 0);
				}

				offset += arguments.get(argumentIndex).getSize();
			}
		}
		super.visitEnd();
	}
}
