/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Identifier;

import cuchaz.enigma.mapping.Entry;

public class SourceIndex {
	
	private String m_source;
	private TreeMap<Token,EntryReference<Entry,Entry>> m_tokenToReference;
	private Multimap<EntryReference<Entry,Entry>,Token> m_referenceToTokens;
	private Map<Entry,Token> m_declarationToToken;
	private List<Integer> m_lineOffsets;
	private boolean m_ignoreBadTokens;
	
	public SourceIndex(String source) {
		this(source, true);
	}
	
	public SourceIndex(String source, boolean ignoreBadTokens) {
		m_source = source;
		m_ignoreBadTokens = ignoreBadTokens;
		m_tokenToReference = Maps.newTreeMap();
		m_referenceToTokens = HashMultimap.create();
		m_declarationToToken = Maps.newHashMap();
		m_lineOffsets = Lists.newArrayList();
		
		// count the lines
		m_lineOffsets.add(0);
		for (int i = 0; i < source.length(); i++) {
			if (source.charAt(i) == '\n') {
				m_lineOffsets.add(i + 1);
			}
		}
	}
	
	public String getSource() {
		return m_source;
	}
	
	public Token getToken(AstNode node) {
		
		// get the text of the node
		String name = "";
		if (node instanceof Identifier) {
			name = ((Identifier)node).getName();
		}
		
		// get a token for this node's region
		Region region = node.getRegion();
		if (region.getBeginLine() == 0 || region.getEndLine() == 0) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" has invalid region: %s", node.getNodeType(), name, region));
			return null;
		}
		Token token = new Token(
			toPos(region.getBeginLine(), region.getBeginColumn()),
			toPos(region.getEndLine(), region.getEndColumn()),
			m_source
		);
		if (token.start == 0) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" has invalid start: %s", node.getNodeType(), name, region));
			return null;
		}
		
		// DEBUG
		// System.out.println( String.format( "%s \"%s\" region: %s", node.getNodeType(), name, region ) );
		
		// if the token has a $ in it, something's wrong. Ignore this token
		if (name.lastIndexOf('$') >= 0 && m_ignoreBadTokens) {
			// DEBUG
			System.err.println(String.format("WARNING: %s \"%s\" is probably a bad token. It was ignored", node.getNodeType(), name));
			return null;
		}
		
		return token;
	}
	
	public void addReference(AstNode node, Entry deobfEntry, Entry deobfContext) {
		Token token = getToken(node);
		if (token != null) {
			EntryReference<Entry,Entry> deobfReference = new EntryReference<Entry,Entry>(deobfEntry, token.text, deobfContext);
			m_tokenToReference.put(token, deobfReference);
			m_referenceToTokens.put(deobfReference, token);
		}
	}
	
	public void addDeclaration(AstNode node, Entry deobfEntry) {
		Token token = getToken(node);
		if (token != null) {
			EntryReference<Entry,Entry> reference = new EntryReference<Entry,Entry>(deobfEntry, token.text);
			m_tokenToReference.put(token, reference);
			m_referenceToTokens.put(reference, token);
			m_declarationToToken.put(deobfEntry, token);
		}
	}
	
	public Token getReferenceToken(int pos) {
		Token token = m_tokenToReference.floorKey(new Token(pos, pos, null));
		if (token != null && token.contains(pos)) {
			return token;
		}
		return null;
	}
	
	public Collection<Token> getReferenceTokens(EntryReference<Entry,Entry> deobfReference) {
		return m_referenceToTokens.get(deobfReference);
	}
	
	public EntryReference<Entry,Entry> getDeobfReference(Token token) {
		if (token == null) {
			return null;
		}
		return m_tokenToReference.get(token);
	}
	
	public void replaceDeobfReference(Token token, EntryReference<Entry,Entry> newDeobfReference) {
		EntryReference<Entry,Entry> oldDeobfReference = m_tokenToReference.get(token);
		m_tokenToReference.put(token, newDeobfReference);
		Collection<Token> tokens = m_referenceToTokens.get(oldDeobfReference);
		m_referenceToTokens.removeAll(oldDeobfReference);
		m_referenceToTokens.putAll(newDeobfReference, tokens);
	}
	
	public Iterable<Token> referenceTokens() {
		return m_tokenToReference.keySet();
	}
	
	public Iterable<Token> declarationTokens() {
		return m_declarationToToken.values();
	}
	
	public Iterable<Entry> declarations() {
		return m_declarationToToken.keySet();
	}
	
	public Token getDeclarationToken(Entry deobfEntry) {
		return m_declarationToToken.get(deobfEntry);
	}
	
	public int getLineNumber(int pos) {
		// line number is 1-based
		int line = 0;
		for (Integer offset : m_lineOffsets) {
			if (offset > pos) {
				break;
			}
			line++;
		}
		return line;
	}
	
	public int getColumnNumber(int pos) {
		// column number is 1-based
		return pos - m_lineOffsets.get(getLineNumber(pos) - 1) + 1;
	}
	
	private int toPos(int line, int col) {
		// line and col are 1-based
		return m_lineOffsets.get(line - 1) + col - 1;
	}
}
