package cuchaz.enigma;

import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Arrays;
import java.util.Optional;

public class ProposingTranslator implements Translator {
	private final EntryRemapper mapper;
	private final NameProposalService[] nameProposalServices;

	public ProposingTranslator(EntryRemapper mapper, NameProposalService[] nameProposalServices) {
		this.mapper = mapper;
		this.nameProposalServices = nameProposalServices;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Translatable> T translate(T translatable) {
		if (translatable == null) {
			return null;
		}

		T deobfuscated = mapper.deobfuscate(translatable);

		if (translatable instanceof Entry && ((Entry) deobfuscated).getName().equals(((Entry<?>) translatable).getName())) {
			return mapper.getObfResolver()
					.resolveEntry((Entry<?>) translatable, ResolutionStrategy.RESOLVE_ROOT)
					.stream()
					.map(this::proposeName)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst()
					.map(newName -> (T) ((Entry) deobfuscated).withName(newName))
					.orElse(deobfuscated);
		}

		return deobfuscated;
	}

	private Optional<String> proposeName(Entry<?> entry) {
		return Arrays.stream(nameProposalServices)
				.map(service -> service.proposeName(entry, mapper))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}
}
