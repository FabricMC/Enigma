package cuchaz.enigma.translation;

import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;

public interface Translatable {
	TranslateResult<? extends Translatable> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);

	@Deprecated
	default Translatable translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return this.extendedTranslate(translator, resolver, mappings).getValue();
	}
}
