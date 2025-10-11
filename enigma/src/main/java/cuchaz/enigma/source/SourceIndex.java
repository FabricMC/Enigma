package cuchaz.enigma.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.Entry;

public class SourceIndex {
	private String source;
	private List<Integer> lineOffsets;
	private final TreeMap<Token, EntryReference<Entry<?>, Entry<?>>> tokenToReference;
	private final Map<EntryReference<Entry<?>, Entry<?>>, Collection<Token>> referenceToTokens;
	private final TreeMap<Token, Entry<?>> tokenToDeclaration;
	private final Map<Entry<?>, Token> declarationToToken;

	public SourceIndex() {
		tokenToReference = new TreeMap<>();
		referenceToTokens = new HashMap<>();
		tokenToDeclaration = new TreeMap<>();
		declarationToToken = new HashMap<>();
	}

	public SourceIndex(String source) {
		this();
		setSource(source);
	}

	public void setSource(String source) {
		this.source = source;
		lineOffsets = new ArrayList<>();
		lineOffsets.add(0);

		for (int i = 0; i < this.source.length(); i++) {
			if (this.source.charAt(i) == '\n') {
				lineOffsets.add(i + 1);
			}
		}
	}

	public String getSource() {
		return source;
	}

	public int getLineNumber(int position) {
		int line = 0;

		for (int offset : lineOffsets) {
			if (offset > position) {
				break;
			}

			line++;
		}

		return line;
	}

	public int getColumnNumber(int position) {
		return position - lineOffsets.get(getLineNumber(position) - 1) + 1;
	}

	public int getPosition(int line, int column) {
		return lineOffsets.get(line - 1) + column - 1;
	}

	public Iterable<Entry<?>> declarations() {
		return declarationToToken.keySet();
	}

	public Iterable<Token> declarationTokens() {
		return declarationToToken.values();
	}

	public Token getDeclarationToken(Entry<?> entry) {
		return declarationToToken.get(entry);
	}

	public void addDeclaration(Token token, Entry<?> deobfEntry) {
		if (token != null) {
			EntryReference<Entry<?>, Entry<?>> reference = new EntryReference<>(deobfEntry, token.text);
			tokenToReference.put(token, reference);
			referenceToTokens.computeIfAbsent(reference, key -> new ArrayList<>())
					.add(token);
			referenceToTokens.computeIfAbsent(EntryReference.declaration(deobfEntry, token.text), key -> new ArrayList<>())
					.add(token);
			tokenToDeclaration.put(token, deobfEntry);
			declarationToToken.put(deobfEntry, token);
		}
	}

	public Iterable<EntryReference<Entry<?>, Entry<?>>> references() {
		return referenceToTokens.keySet();
	}

	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (token == null) {
			return null;
		}

		return tokenToReference.get(token);
	}

	public Iterable<Token> referenceTokens() {
		return tokenToReference.keySet();
	}

	@Nullable
	public Token getReferenceToken(int pos) {
		Token token = tokenToReference.floorKey(new Token(pos, pos, null));

		if (token != null && token.contains(pos)) {
			return token;
		}

		return null;
	}

	public Collection<Token> getReferenceTokens(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		return referenceToTokens.getOrDefault(deobfReference, List.of());
	}

	@Nullable
	public Token getDeclarationToken(int pos) {
		Token token = tokenToDeclaration.floorKey(new Token(pos, pos, null));

		if (token != null && token.contains(pos)) {
			return token;
		}

		return null;
	}

	public void addReference(Token token, Entry<?> deobfEntry, Entry<?> deobfContext) {
		if (token != null) {
			EntryReference<Entry<?>, Entry<?>> deobfReference = new EntryReference<>(deobfEntry, token.text, deobfContext);
			tokenToReference.put(token, deobfReference);
			referenceToTokens.computeIfAbsent(deobfReference, key -> new ArrayList<>())
					.add(token);
		}
	}

	public void resolveReferences(EntryResolver resolver) {
		// resolve all the classes in the source references
		for (Token token : referenceToTokens.values().stream().flatMap(Collection::stream).toList()) {
			EntryReference<Entry<?>, Entry<?>> reference = tokenToReference.get(token);
			EntryReference<Entry<?>, Entry<?>> resolvedReference = resolver.resolveFirstReference(reference, ResolutionStrategy.RESOLVE_CLOSEST);

			// replace the reference
			tokenToReference.replace(token, resolvedReference);

			Collection<Token> tokens = referenceToTokens.remove(reference);

			if (tokens != null) {
				referenceToTokens.computeIfAbsent(resolvedReference, key -> new ArrayList<>())
						.addAll(tokens);
			}
		}
	}

	public SourceIndex remapTo(SourceRemapper.Result result) {
		SourceIndex remapped = new SourceIndex(result.getSource());

		for (Map.Entry<Entry<?>, Token> entry : declarationToToken.entrySet()) {
			Token remappedToken = result.getRemappedToken(entry.getValue());
			remapped.declarationToToken.put(entry.getKey(), remappedToken);
			remapped.tokenToDeclaration.put(remappedToken, entry.getKey());
		}

		for (Map.Entry<EntryReference<Entry<?>, Entry<?>>, Collection<Token>> entry : referenceToTokens.entrySet()) {
			EntryReference<Entry<?>, Entry<?>> reference = entry.getKey();
			Collection<Token> oldTokens = entry.getValue();

			Collection<Token> newTokens = oldTokens.stream().map(result::getRemappedToken).toList();

			remapped.referenceToTokens.computeIfAbsent(reference, key -> new ArrayList<>())
					.addAll(newTokens);
		}

		for (Map.Entry<Token, EntryReference<Entry<?>, Entry<?>>> entry : tokenToReference.entrySet()) {
			remapped.tokenToReference.put(result.getRemappedToken(entry.getKey()), entry.getValue());
		}

		return remapped;
	}
}
