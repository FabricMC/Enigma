package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

public class AsmObjectTranslator {
	public static Type translateType(Translator translator, Type type) {
		String descString = type.getDescriptor();
		switch (type.getSort()) {
			case Type.OBJECT: {
				ClassEntry classEntry = new ClassEntry(type.getInternalName());
				return Type.getObjectType(translator.translate(classEntry).getName());
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
		MethodEntry entry = new MethodEntry(new ClassEntry(handle.getOwner()), handle.getName(), new MethodDescriptor(handle.getDesc()));
		MethodEntry translatedMethod = translator.translate(entry);
		ClassEntry ownerClass = translatedMethod.getParent();
		return new Handle(handle.getTag(), ownerClass.getName(), translatedMethod.getName(), translatedMethod.getDesc().toString(), handle.isInterface());
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
