package cuchaz.enigma.source;

import java.util.*;

import javax.annotation.Nullable;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.EnigmaServices;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.LocalNameGenerator;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableDefEntry;

public class DecompiledClassSource {
	private final ClassEntry classEntry;

	private final SourceIndex obfuscatedIndex;
	private SourceIndex remappedIndex;

	private final Map<RenamableTokenType, Collection<Token>> highlightedTokens = new EnumMap<>(RenamableTokenType.class);

	public DecompiledClassSource(ClassEntry classEntry, SourceIndex index) {
		this.classEntry = classEntry;
		this.obfuscatedIndex = index;
		this.remappedIndex = index;
	}

	public static DecompiledClassSource text(ClassEntry classEntry, String text) {
		return new DecompiledClassSource(classEntry, new SourceIndex(text));
	}

	public void remapSource(EnigmaProject project, Translator translator) {
		highlightedTokens.clear();

		SourceRemapper remapper = new SourceRemapper(obfuscatedIndex.getSource(), obfuscatedIndex.referenceTokens());

		SourceRemapper.Result remapResult = remapper.remap((token, movedToken) -> remapToken(project, token, movedToken, translator));
		remappedIndex = obfuscatedIndex.remapTo(remapResult);
	}

	private String remapToken(EnigmaProject project, Token token, Token movedToken, Translator translator) {
		EntryReference<Entry<?>, Entry<?>> reference = obfuscatedIndex.getReference(token);

		Entry<?> entry = reference.getNameableEntry();
		TranslateResult<Entry<?>> translatedEntry = translator.extendedTranslate(entry);

		if (project.isRenamable(reference)) {
			if (!translatedEntry.isObfuscated()) {
				highlightToken(movedToken, translatedEntry.getType());
				return translatedEntry.getValue().getSourceRemapName();
			} else {
				Optional<String> proposedName = proposeName(project, entry);
				if (proposedName.isPresent()) {
					highlightToken(movedToken, RenamableTokenType.PROPOSED);
					return proposedName.get();
				}

				highlightToken(movedToken, RenamableTokenType.OBFUSCATED);
			}
		}

		String defaultName = generateDefaultName(translatedEntry.getValue());
		if (defaultName != null) {
			return defaultName;
		}

		return null;
	}

	private Optional<String> proposeName(EnigmaProject project, Entry<?> entry) {
		EnigmaServices services = project.getEnigma().getServices();

		return services.get(NameProposalService.TYPE).stream().flatMap(nameProposalService -> {
			EntryRemapper mapper = project.getMapper();
			Collection<Entry<?>> resolved = mapper.getObfResolver().resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT);

			return resolved.stream()
					.map(e -> nameProposalService.proposeName(e, mapper))
					.filter(Optional::isPresent)
					.map(Optional::get);
		}).findFirst();
	}

	@Nullable
	private String generateDefaultName(Entry<?> entry) {
		if (entry instanceof LocalVariableDefEntry) {
			LocalVariableDefEntry localVariable = (LocalVariableDefEntry) entry;

			int index = localVariable.getIndex();
			if (localVariable.isArgument()) {
				List<TypeDescriptor> arguments = localVariable.getParent().getDesc().getArgumentDescs();
				return LocalNameGenerator.generateArgumentName(index, localVariable.getDesc(), arguments);
			} else {
				return LocalNameGenerator.generateLocalVariableName(index, localVariable.getDesc());
			}
		}

		return null;
	}

	public ClassEntry getEntry() {
		return classEntry;
	}

	public SourceIndex getIndex() {
		return remappedIndex;
	}

	public Map<RenamableTokenType, Collection<Token>> getHighlightedTokens() {
		return highlightedTokens;
	}

	private void highlightToken(Token token, RenamableTokenType highlightType) {
		highlightedTokens.computeIfAbsent(highlightType, t -> new ArrayList<>()).add(token);
	}

	public int getObfuscatedOffset(int deobfOffset) {
		return getOffset(remappedIndex, obfuscatedIndex, deobfOffset);
	}

	public int getDeobfuscatedOffset(int obfOffset) {
		return getOffset(obfuscatedIndex, remappedIndex, obfOffset);
	}

	private static int getOffset(SourceIndex fromIndex, SourceIndex toIndex, int fromOffset) {
		int relativeOffset = 0;

		Iterator<Token> fromTokenItr = fromIndex.referenceTokens().iterator();
		Iterator<Token> toTokenItr = toIndex.referenceTokens().iterator();
		while (fromTokenItr.hasNext() && toTokenItr.hasNext()) {
			Token fromToken = fromTokenItr.next();
			Token toToken = toTokenItr.next();
			if (fromToken.end > fromOffset) {
				break;
			}

			relativeOffset = toToken.end - fromToken.end;
		}

		return fromOffset + relativeOffset;
	}

	@Override
	public String toString() {
		return remappedIndex.getSource();
	}
}
