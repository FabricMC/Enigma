package cuchaz.enigma.translation;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nullable;

import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.Entry;

public class ProposingTranslator implements Translator {
	private final EntryRemapper mapper;
	private final NameProposalService[] nameProposalServices;

	public ProposingTranslator(EntryRemapper mapper, NameProposalService[] nameProposalServices) {
		this.mapper = mapper;
		this.nameProposalServices = nameProposalServices;
	}

	@Nullable
	@Override
	public <T extends Translatable> TranslateResult<T> extendedTranslate(T translatable) {
		if (translatable == null) {
			return null;
		}

		TranslateResult<T> deobfuscated = mapper.extendedDeobfuscate(translatable);

		if (translatable instanceof Entry && ((Entry) deobfuscated.getValue()).getName().equals(((Entry<?>) translatable).getName())) {
			return mapper.getObfResolver().resolveEntry((Entry<?>) translatable, ResolutionStrategy.RESOLVE_ROOT).stream().map(this::proposeName).filter(Optional::isPresent).map(Optional::get).findFirst().map(
					newName -> TranslateResult.proposed((T) ((Entry) deobfuscated.getValue()).withName(newName))).orElse(deobfuscated);
		}

		return deobfuscated;
	}

	private Optional<String> proposeName(Entry<?> entry) {
		return Arrays.stream(nameProposalServices).map(service -> service.proposeName(entry, mapper)).filter(Optional::isPresent).map(Optional::get).findFirst();
	}
}
