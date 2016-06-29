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
package cuchaz.enigma;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

import com.google.common.collect.Lists;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.mapping.Entry;

public class TokenChecker {
	
	private Deobfuscator m_deobfuscator;
	
	protected TokenChecker(JarFile jarFile)
	throws IOException {
		m_deobfuscator = new Deobfuscator(jarFile);
	}
	
	protected String getDeclarationToken(Entry entry) {
		// decompile the class
		CompilationUnit tree = m_deobfuscator.getSourceTree(entry.getClassName());
		// DEBUG
		// tree.acceptVisitor( new TreeDumpVisitor( new File( "tree." + entry.getClassName().replace( '/', '.' ) + ".txt" ) ), null );
		String source = m_deobfuscator.getSource(tree);
		SourceIndex index = m_deobfuscator.getSourceIndex(tree, source);
		
		// get the token value
		Token token = index.getDeclarationToken(entry);
		if (token == null) {
			return null;
		}
		return source.substring(token.start, token.end);
	}
	
	@SuppressWarnings("unchecked")
	protected Collection<String> getReferenceTokens(EntryReference<? extends Entry,? extends Entry> reference) {
		// decompile the class
		CompilationUnit tree = m_deobfuscator.getSourceTree(reference.context.getClassName());
		String source = m_deobfuscator.getSource(tree);
		SourceIndex index = m_deobfuscator.getSourceIndex(tree, source);
		
		// get the token values
		List<String> values = Lists.newArrayList();
		for (Token token : index.getReferenceTokens((EntryReference<Entry,Entry>)reference)) {
			values.add(source.substring(token.start, token.end));
		}
		return values;
	}
}
