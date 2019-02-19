package cuchaz.enigma.gui;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.gui.highlight.TokenHighlightType;
import cuchaz.enigma.translation.LocalNameGenerator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableDefEntry;

import javax.annotation.Nullable;
import java.util.*;

public class DecompiledClassSource {
	private final ClassEntry classEntry;

	private final SourceIndex obfuscatedIndex;
	private SourceIndex remappedIndex;

	private final Map<TokenHighlightType, Collection<Token>> highlightedTokens = new EnumMap<>(TokenHighlightType.class);

	public DecompiledClassSource(ClassEntry classEntry, SourceIndex index) {
		this.classEntry = classEntry;
		this.obfuscatedIndex = index;
		this.remappedIndex = index;
	}

	public static DecompiledClassSource text(ClassEntry classEntry, String text) {
		return new DecompiledClassSource(classEntry, new SourceIndex(text));
	}

	public void remapSource(Deobfuscator deobfuscator, Translator translator) {
		highlightedTokens.clear();

		SourceRemapper remapper = new SourceRemapper(obfuscatedIndex.getSource(), obfuscatedIndex.referenceTokens());

		SourceRemapper.Result remapResult = remapper.remap((token, movedToken) -> remapToken(deobfuscator, token, movedToken, translator));
		remappedIndex = obfuscatedIndex.remapTo(remapResult);
	}

	private String remapToken(Deobfuscator deobfuscator, Token token, Token movedToken, Translator translator) {
		EntryReference<Entry<?>, Entry<?>> reference = obfuscatedIndex.getReference(token);

		Entry<?> entry = reference.getNameableEntry();
		Entry<?> translatedEntry = translator.translate(entry);

		if (deobfuscator.isRenamable(reference)) {
			if (isDeobfuscated(entry, translatedEntry)) {
				highlightToken(movedToken, TokenHighlightType.DEOBFUSCATED);
				return translatedEntry.getSourceRemapName();
			} else {
				String proposedName = proposeName(deobfuscator, entry);
				if (proposedName != null) {
					highlightToken(movedToken, TokenHighlightType.PROPOSED);
					return proposedName;
				}

				highlightToken(movedToken, TokenHighlightType.OBFUSCATED);
			}
		}

		String defaultName = generateDefaultName(translatedEntry);
		if (defaultName != null) {
			return defaultName;
		}

		return null;
	}

	@Nullable
	private String proposeName(Deobfuscator deobfuscator, Entry<?> entry) {
		if (entry instanceof FieldEntry) {
			for (EnigmaPlugin plugin : deobfuscator.getPlugins()) {
				String owner = entry.getContainingClass().getFullName();
				String proposal = plugin.proposeFieldName(owner, entry.getName(), ((FieldEntry) entry).getDesc().toString());
				if (proposal != null) {
					return proposal;
				}
			}
		}
		return null;
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

	private boolean isDeobfuscated(Entry<?> entry, Entry<?> translatedEntry) {
		return !entry.getName().equals(translatedEntry.getName());
	}

	public ClassEntry getEntry() {
		return classEntry;
	}

	public SourceIndex getIndex() {
		return remappedIndex;
	}

	public Map<TokenHighlightType, Collection<Token>> getHighlightedTokens() {
		return highlightedTokens;
	}

	private void highlightToken(Token token, TokenHighlightType highlightType) {
		highlightedTokens.computeIfAbsent(highlightType, t -> new ArrayList<>()).add(token);
	}

	@Override
	public String toString() {
		return remappedIndex.getSource();
	}
}
