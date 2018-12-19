package cuchaz.enigma.translation;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

public enum VoidTranslator implements Translator{
	INSTANCE;

	@Override
	public <T extends Translatable> T translate(T translatable) {
		return translatable;
	}

	@Override
	public Type translateType(Type type) {
		return type;
	}

	@Override
	public Handle translateHandle(Handle handle) {
		return handle;
	}

	@Override
	public Object translateValue(Object value) {
		return value;
	}
}
