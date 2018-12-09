/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import cuchaz.enigma.mapping.entry.Entry;

import java.util.*;
import java.util.regex.Pattern;

public class SourceIndex {
	private static Pattern ANONYMOUS_INNER = Pattern.compile("\\$\\d+$");

	private String source;
	private TreeMap<Token, EntryReference<Entry, Entry>> tokenToReference;
	private Multimap<EntryReference<Entry, Entry>, Token> referenceToTokens;
	private Map<Entry, Token> declarationToToken;
	private List<Integer> lineOffsets;
	private boolean ignoreBadTokens;

	public SourceIndex(String source) {
		this(source, true);
	}

	public SourceIndex(String source, boolean ignoreBadTokens) {
		this.source = source;
		this.ignoreBadTokens = ignoreBadTokens;
		this.tokenToReference = Maps.newTreeMap();
		this.referenceToTokens = HashMultimap.create();
		this.declarationToToken = Maps.newHashMap();
		calculateLineOffsets();
	}

	private void calculateLineOffsets() {
		// count the lines
		this.lineOffsets = Lists.newArrayList();
		this.lineOffsets.add(0);
		for (int i = 0; i < source.length(); i++) {
			if (source.charAt(i) == '\n') {
				this.lineOffsets.add(i + 1);
			}
		}
	}

	public void remap(String source, Map<Token, Token> tokenMap) {
		this.source = source;
		calculateLineOffsets();

		for (Entry entry : Lists.newArrayList(declarationToToken.keySet())) {
			Token token = declarationToToken.get(entry);
			declarationToToken.put(entry, tokenMap.getOrDefault(token, token));
		}

		for (EntryReference<Entry, Entry> ref : referenceToTokens.keySet()) {
			Collection<Token> oldTokens = referenceToTokens.get(ref);
			List<Token> newTokens = new ArrayList<>(oldTokens.size());

			for (Token token : oldTokens) {
				newTokens.add(tokenMap.getOrDefault(token, token));
			}

			referenceToTokens.replaceValues(ref, newTokens);
		}

		Map<Token, EntryReference<Entry, Entry>> tokenToReferenceCopy = Maps.newHashMap(tokenToReference);
		tokenToReference.clear();
		for (Token token : tokenToReferenceCopy.keySet()) {
			tokenToReference.put(tokenMap.getOrDefault(token, token), tokenToReferenceCopy.get(token));
		}
	}

	public String getSource() {
		return this.source;
	}

	public Token getToken(AstNode node) {

		// get the text of the node
		String name = "";
		if (node instanceof Identifier) {
			name = ((Identifier) node).getName();
		}

		// get a token for this node's region
		Region region = node.getRegion();
		if (region.getBeginLine() == 0 || region.getEndLine() == 0) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" has invalid region: %s", node.getNodeType(), name, region));
			return null;
		}
		Token token = new Token(toPos(region.getBeginLine(), region.getBeginColumn()), toPos(region.getEndLine(), region.getEndColumn()), this.source);
		if (token.start == 0) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" has invalid start: %s", node.getNodeType(), name, region));
			return null;
		}

		if (node instanceof Identifier && name.indexOf('$') >=0 && node.getParent() instanceof ConstructorDeclaration && name.lastIndexOf('$') >= 0 && !ANONYMOUS_INNER.matcher(name).matches()){
			TypeDeclaration type = node.getParent().getParent() instanceof TypeDeclaration ? (TypeDeclaration) node.getParent().getParent() : null;
			if (type != null){
				name = type.getName();
				token.end = token.start + name.length();
			}
		}

		// DEBUG
		// System.out.println( String.format( "%s \"%s\" region: %s", node.getNodeType(), name, region ) );

		// if the token has a $ in it, something's wrong. Ignore this token
		if (name.lastIndexOf('$') >= 0 && this.ignoreBadTokens) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" is probably a bad token. It was ignored", node.getNodeType(), name));
			return null;
		}

		return token;
	}

	public void addReference(AstNode node, Entry deobfEntry, Entry deobfContext) {
		Token token = getToken(node);
		if (token != null) {
			EntryReference<Entry, Entry> deobfReference = new EntryReference<>(deobfEntry, token.text, deobfContext);
			this.tokenToReference.put(token, deobfReference);
			this.referenceToTokens.put(deobfReference, token);
		}
	}

	public void addDeclaration(AstNode node, Entry deobfEntry) {
		Token token = getToken(node);
		if (token != null) {
			EntryReference<Entry, Entry> reference = new EntryReference<>(deobfEntry, token.text);
			this.tokenToReference.put(token, reference);
			this.referenceToTokens.put(reference, token);
			this.declarationToToken.put(deobfEntry, token);
		}
	}

	public Token getReferenceToken(int pos) {
		Token token = this.tokenToReference.floorKey(new Token(pos, pos, null));
		if (token != null && token.contains(pos)) {
			return token;
		}
		return null;
	}

	public Collection<Token> getReferenceTokens(EntryReference<Entry, Entry> deobfReference) {
		return this.referenceToTokens.get(deobfReference);
	}

	public EntryReference<Entry, Entry> getDeobfReference(Token token) {
		if (token == null) {
			return null;
		}
		return this.tokenToReference.get(token);
	}

	public void replaceDeobfReference(Token token, EntryReference<Entry, Entry> newDeobfReference) {
		EntryReference<Entry, Entry> oldDeobfReference = this.tokenToReference.get(token);
		this.tokenToReference.put(token, newDeobfReference);
		Collection<Token> tokens = this.referenceToTokens.get(oldDeobfReference);
		this.referenceToTokens.removeAll(oldDeobfReference);
		this.referenceToTokens.putAll(newDeobfReference, tokens);
	}

	public Iterable<Token> referenceTokens() {
		return this.tokenToReference.keySet();
	}

	public Iterable<Token> declarationTokens() {
		return this.declarationToToken.values();
	}

	public Iterable<Entry> declarations() {
		return this.declarationToToken.keySet();
	}

	public Token getDeclarationToken(Entry deobfEntry) {
		return this.declarationToToken.get(deobfEntry);
	}

	public int getLineNumber(int pos) {
		// line number is 1-based
		int line = 0;
		for (Integer offset : this.lineOffsets) {
			if (offset > pos) {
				break;
			}
			line++;
		}
		return line;
	}

	public int getColumnNumber(int pos) {
		// column number is 1-based
		return pos - this.lineOffsets.get(getLineNumber(pos) - 1) + 1;
	}

	private int toPos(int line, int col) {
		// line and col are 1-based
		return this.lineOffsets.get(line - 1) + col - 1;
	}
}
