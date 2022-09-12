package cuchaz.enigma.api.service;

import java.util.Optional;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface NameProposalService extends EnigmaService {
	EnigmaServiceType<NameProposalService> TYPE = EnigmaServiceType.create("name_proposal");

	Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper);
}
