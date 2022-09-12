package cuchaz.enigma.bytecode.translators;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class AsmObjectTranslator {
	public static Type translateType(Translator translator, Type type) {
		String descString = type.getDescriptor();
		switch (type.getSort()) {
		case Type.OBJECT: {
			ClassEntry classEntry = new ClassEntry(type.getInternalName());
			return Type.getObjectType(translator.translate(classEntry).getFullName());
		}
		case Type.ARRAY: {
			TypeDescriptor descriptor = new TypeDescriptor(descString);
			return Type.getType(translator.translate(descriptor).toString());
		}
		case Type.METHOD: {
			MethodDescriptor descriptor = new MethodDescriptor(descString);
			return Type.getMethodType(translator.translate(descriptor).toString());
		}
		}

		return type;
	}

	public static Handle translateHandle(Translator translator, Handle handle) {
		final boolean isFieldHandle = handle.getTag() <= Opcodes.H_PUTSTATIC;
		return isFieldHandle ? translateFieldHandle(translator, handle) : translateMethodHandle(translator, handle);
	}

	private static Handle translateMethodHandle(Translator translator, Handle handle) {
		MethodEntry entry = new MethodEntry(new ClassEntry(handle.getOwner()), handle.getName(), new MethodDescriptor(handle.getDesc()));
		MethodEntry translatedMethod = translator.translate(entry);
		ClassEntry ownerClass = translatedMethod.getParent();
		return new Handle(handle.getTag(), ownerClass.getFullName(), translatedMethod.getName(), translatedMethod.getDesc().toString(), handle.isInterface());
	}

	private static Handle translateFieldHandle(Translator translator, Handle handle) {
		FieldEntry entry = new FieldEntry(new ClassEntry(handle.getOwner()), handle.getName(), new TypeDescriptor(handle.getDesc()));
		FieldEntry translatedMethod = translator.translate(entry);
		ClassEntry ownerClass = translatedMethod.getParent();
		return new Handle(handle.getTag(), ownerClass.getFullName(), translatedMethod.getName(), translatedMethod.getDesc().toString(), handle.isInterface());
	}

	public static Object translateValue(Translator translator, Object value) {
		if (value instanceof Type) {
			return translateType(translator, (Type) value);
		} else if (value instanceof Handle) {
			return translateHandle(translator, (Handle) value);
		}

		return value;
	}
}
