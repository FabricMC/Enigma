package cuchaz.enigma.translation;

public enum VoidTranslator implements Translator {
	INSTANCE;

	@Override
	public <T extends Translatable> TranslateResult<T> extendedTranslate(T translatable) {
		return TranslateResult.obfuscated(translatable);
	}

}
