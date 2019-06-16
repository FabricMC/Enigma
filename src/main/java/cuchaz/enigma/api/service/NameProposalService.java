package cuchaz.enigma.api.service;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Optional;

public interface NameProposalService extends EnigmaService {
	EnigmaServiceType<NameProposalService> TYPE = EnigmaServiceType.create("name_proposal");

	Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper);
}
