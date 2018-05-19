package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.mapping.*;
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
		if (translatedEntry != null) {
			super.visitFieldInsn(opcode, translatedEntry.getClassName(), translatedEntry.getName(), translatedEntry.getDesc().toString());
		} else {
			super.visitFieldInsn(opcode, owner, name, desc);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		MethodEntry entry = new MethodEntry(new ClassEntry(owner), name, new MethodDescriptor(desc));
		MethodEntry translatedEntry = translator.getTranslatedMethod(entry);
		if (translatedEntry != null) {
			super.visitMethodInsn(opcode, translatedEntry.getClassName(), translatedEntry.getName(), translatedEntry.getDesc().toString(), itf);
		} else {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}

	@Override
	public void visitAttribute(Attribute attr) {
		// TODO: Implement
		super.visitAttribute(attr);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// TODO: Implement
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		LocalVariableDefEntry entry = new LocalVariableDefEntry(methodEntry, index, name, new TypeDescriptor(desc));
		LocalVariableDefEntry translatedEntry = translator.getTranslatedVariableDef(entry);
		String translatedName = translatedEntry.getName();
		// TODO: Better name inference
		if (translatedName.equals(entry.getName())) {
			TypeDescriptor argDesc = translatedEntry.getDesc();
			int nameIndex = translatedEntry.getNamedIndex() + 1;
			String prefix = translatedEntry.getNamedIndex() < methodEntry.getDesc().getArgumentDescs().size() ? "a" : "v";
			StringBuilder nameBuilder = new StringBuilder(prefix);
			// Unfortunately each of these have different name getters, so they have different code paths
			if (argDesc.isPrimitive()) {
				TypeDescriptor.Primitive argCls = argDesc.getPrimitive();
				nameBuilder.append(argCls.name());
			} else if (argDesc.isArray()) {
				// List types would require this whole block again, so just go with aListx
				nameBuilder.append(nameIndex);
			} else if (argDesc.isType()) {
				String typeName = argDesc.getOwnerEntry().getSimpleName().replace("$", "");
				typeName = typeName.substring(0, 1).toUpperCase(Locale.ROOT) + typeName.substring(1);
				nameBuilder.append(typeName);
			}
			if (methodEntry.getDesc().getArgumentDescs().size() > 1) {
				nameBuilder.append(nameIndex);
			}
			translatedName = nameBuilder.toString();
		}
		super.visitLocalVariable(translatedName, translatedEntry.getDesc().toString(), signature, start, end, index);
	}
}
