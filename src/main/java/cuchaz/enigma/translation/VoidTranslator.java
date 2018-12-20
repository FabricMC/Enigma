package cuchaz.enigma.translation;

public enum VoidTranslator implements Translator {
	INSTANCE;

	@Override
	public <T extends Translatable> T translate(T translatable) {
		return translatable;
	}
}
