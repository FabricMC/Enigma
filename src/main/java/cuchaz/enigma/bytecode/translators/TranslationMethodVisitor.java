package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.TypeDescriptor;
import cuchaz.enigma.mapping.entry.*;
import org.objectweb.asm.*;

import java.util.Locale;

public class TranslationMethodVisitor extends MethodVisitor {
	private final MethodDefEntry methodEntry;
	private final Translator translator;

	public TranslationMethodVisitor(Translator translator, MethodDefEntry methodEntry, int api, MethodVisitor mv) {
		super(api, mv);
		this.translator = translator;
		this.methodEntry = methodEntry;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		FieldEntry entry = new FieldEntry(new ClassEntry(owner), name, new TypeDescriptor(desc));
		FieldEntry translatedEntry = translator.getTranslatedField(entry);
		super.visitFieldInsn(opcode, translatedEntry.getClassName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		MethodEntry entry = new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc));
		MethodEntry translatedEntry = translator.getTranslatedMethod(entry);
		super.visitMethodInsn(opcode, translatedEntry.getClassName(), translatedEntry.getName(), translatedEntry.getDesc().toString(), itf);
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
				ClassEntry entry = new ClassEntry((String) object);
				array[i] = translator.getTranslatedClass(entry).getName();
			}
		}
		return array;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		String translatedSignature = translator.getTranslatedSignature(Signature.createTypedSignature(signature)).toString();

		// If we're not static, "this" is bound to index 0
		int offsetIndex = index - (methodEntry.getAccess().isStatic() ? 0 : 1);
		if (offsetIndex >= 0) {
			LocalVariableDefEntry entry = new LocalVariableDefEntry(methodEntry, offsetIndex, name, new TypeDescriptor(desc));
			LocalVariableDefEntry translatedEntry = translator.getTranslatedVariableDef(entry);
			String translatedName = translatedEntry.getName();

			// TODO: Better name inference
			if (translatedName.equals(entry.getName())) {
				TypeDescriptor argDesc = translatedEntry.getDesc();
				int nameIndex = offsetIndex + 1;
				String prefix = offsetIndex < methodEntry.getDesc().getArgumentDescs().size() ? "a" : "v";
				StringBuilder nameBuilder = new StringBuilder(prefix);
				// Unfortunately each of these have different name getters, so they have different code paths
				if (argDesc.isPrimitive()) {
					TypeDescriptor.Primitive argCls = argDesc.getPrimitive();
					nameBuilder.append(argCls.name());
				} else if (argDesc.isArray()) {
					// List types would require this whole block again, so just go with aListx
					nameBuilder.append(nameIndex);
				} else if (argDesc.isType()) {
					String typeName = argDesc.getTypeEntry().getSimpleName().replace("$", "");
					typeName = typeName.substring(0, 1).toUpperCase(Locale.ROOT) + typeName.substring(1);
					nameBuilder.append(typeName);
				}
				if (methodEntry.getDesc().getArgumentDescs().size() > 1) {
					nameBuilder.append(nameIndex);
				}
				translatedName = nameBuilder.toString();
			} else {
				System.out.println();
			}

			super.visitLocalVariable(translatedName, translatedEntry.getDesc().toString(), translatedSignature, start, end, index);
		} else {
			// Handle "this" variable
			TypeDescriptor translatedDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
			super.visitLocalVariable(name, translatedDesc.toString(), translatedSignature, start, end, index);
		}
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		ClassEntry translatedEntry = translator.getTranslatedClass(new ClassEntry(type));
		super.visitTypeInsn(opcode, translatedEntry.getName());
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		MethodDescriptor translatedMethodDesc = translator.getTranslatedMethodDesc(new MethodDescriptor(desc));
		Object[] translatedBsmArgs = new Object[bsmArgs.length];
		for (int i = 0; i < bsmArgs.length; i++) {
			translatedBsmArgs[i] = translator.getTranslatedValue(bsmArgs[i]);
		}
		super.visitInvokeDynamicInsn(name, translatedMethodDesc.toString(), translator.getTranslatedHandle(bsm), translatedBsmArgs);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(translator.getTranslatedValue(cst));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		super.visitMultiANewArrayInsn(translator.getTranslatedTypeDesc(new TypeDescriptor(desc)).toString(), dims);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (type != null) {
			ClassEntry translatedEntry = translator.getTranslatedClass(new ClassEntry(type));
			super.visitTryCatchBlock(start, end, handler, translatedEntry.getName());
		} else {
			super.visitTryCatchBlock(start, end, handler, type);
		}
	}
}
