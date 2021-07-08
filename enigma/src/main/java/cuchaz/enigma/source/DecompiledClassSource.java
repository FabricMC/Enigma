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
	private final SourceIndex remappedIndex;

	private final TokenStore highlightedTokens;

	private DecompiledClassSource(ClassEntry classEntry, SourceIndex obfuscatedIndex, SourceIndex remappedIndex, TokenStore highlightedTokens) {
		this.classEntry = classEntry;
		this.obfuscatedIndex = obfuscatedIndex;
		this.remappedIndex = remappedIndex;
		this.highlightedTokens = highlightedTokens;
	}

	public DecompiledClassSource(ClassEntry classEntry, SourceIndex index) {
		this(classEntry, index, index, TokenStore.empty());
	}

	public static DecompiledClassSource text(ClassEntry classEntry, String text) {
		return new DecompiledClassSource(classEntry, new SourceIndex(text));
	}

	public DecompiledClassSource remapSource(EnigmaProject project, Translator translator) {
		SourceRemapper remapper = new SourceRemapper(obfuscatedIndex.getSource(), obfuscatedIndex.referenceTokens());

		TokenStore tokenStore = TokenStore.create(this.obfuscatedIndex);
		SourceRemapper.Result remapResult = remapper.remap((token, movedToken) -> remapToken(tokenStore, project, token, movedToken, translator));
		SourceIndex remappedIndex = obfuscatedIndex.remapTo(remapResult);
		return new DecompiledClassSource(this.classEntry, this.obfuscatedIndex, remappedIndex, tokenStore);
	}

	private String remapToken(TokenStore target, EnigmaProject project, Token token, Token movedToken, Translator translator) {
		EntryReference<Entry<?>, Entry<?>> reference = obfuscatedIndex.getReference(token);

		Entry<?> entry = reference.getNameableEntry();
		TranslateResult<Entry<?>> translatedEntry = translator.extendedTranslate(entry);

		if (project.isRenamable(reference)) {
			if (!translatedEntry.isObfuscated()) {
				target.add(translatedEntry.getType(), movedToken);
				return translatedEntry.getValue().getSourceRemapName();
			} else {
				Optional<String> proposedName = proposeName(project, entry);
				if (proposedName.isPresent()) {
					target.add(RenamableTokenType.PROPOSED, movedToken);
					return proposedName.get();
				}

				target.add(RenamableTokenType.OBFUSCATED, movedToken);
			}
		}

		String defaultName = generateDefaultName(translatedEntry.getValue());
		if (defaultName != null) {
			return defaultName;
		}

		return null;
	}

	public static Optional<String> proposeName(EnigmaProject project, Entry<?> entry) {
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

	public TokenStore getTokenStore() {
		return this.highlightedTokens;
	}

	public Map<RenamableTokenType, ? extends Collection<Token>> getHighlightedTokens() {
		return this.highlightedTokens.getByType();
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
