package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingSet;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

// TODO: name?
public class MappingTranslator implements Translator {
	private final MappingSet<EntryMapping> mappings;

	public MappingTranslator(MappingSet<EntryMapping> mappings) {
		this.mappings = mappings;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Translatable> T translate(T translatable) {
		if (translatable == null) {
			return null;
		}
		return (T) translatable.translate(this, mappings);
	}

	@Override
	public Type translateType(Type type) {
		String descString = type.getDescriptor();
		switch (type.getSort()) {
			case Type.OBJECT: {
				ClassEntry classEntry = new ClassEntry(type.getInternalName());
				return Type.getObjectType(translate(classEntry).getName());
			}
			case Type.ARRAY: {
				TypeDescriptor descriptor = new TypeDescriptor(descString);
				return Type.getType(translate(descriptor).toString());
			}
			case Type.METHOD: {
				MethodDescriptor descriptor = new MethodDescriptor(descString);
				return Type.getMethodType(translate(descriptor).toString());
			}
		}
		return type;
	}

	@Override
	public Handle translateHandle(Handle handle) {
		MethodEntry entry = new MethodEntry(new ClassEntry(handle.getOwner()), handle.getName(), new MethodDescriptor(handle.getDesc()));
		MethodEntry translatedMethod = translate(entry);
		ClassEntry ownerClass = translatedMethod.getParent();
		return new Handle(handle.getTag(), ownerClass.getName(), translatedMethod.getName(), translatedMethod.getDesc().toString(), handle.isInterface());
	}

	@Override
	public Object translateValue(Object value) {
		if (value instanceof Type) {
			return this.translateType((Type) value);
		} else if (value instanceof Handle) {
			return translateHandle((Handle) value);
		}
		return value;
	}
}
