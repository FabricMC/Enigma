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


import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.BeforeClass;
import org.junit.Test;

import cuchaz.enigma.analysis.JarIndex;


public class TestDeobfed {

	private static JarFile m_jar;
	private static JarIndex m_index;
	
	@BeforeClass
	public static void beforeClass()
	throws Exception {
		m_jar = new JarFile("build/test-deobf/translation.jar");
		m_index = new JarIndex();
		m_index.indexJar(m_jar, true);
	}
	
	@Test
	public void obfEntries() {
		assertThat(m_index.getObfClassEntries(), containsInAnyOrder(
			newClass("cuchaz/enigma/inputs/Keep"),
			newClass("a"),
			newClass("b"),
			newClass("c"),
			newClass("d"),
			newClass("d$1"),
			newClass("e"),
			newClass("f"),
			newClass("g"),
			newClass("g$a"),
			newClass("g$a$a"),
			newClass("g$b"),
			newClass("g$b$a"),
			newClass("h"),
			newClass("h$a"),
			newClass("h$a$a"),
			newClass("h$b"),
			newClass("h$b$a"),
			newClass("h$b$a$a"),
			newClass("h$b$a$b"),
			newClass("i"),
			newClass("i$a"),
			newClass("i$b")
		));
	}
	
	@Test
	public void decompile()
	throws Exception {
		Deobfuscator deobfuscator = new Deobfuscator(m_jar);
		deobfuscator.getSourceTree("a");
		deobfuscator.getSourceTree("b");
		deobfuscator.getSourceTree("c");
		deobfuscator.getSourceTree("d");
		deobfuscator.getSourceTree("d$1");
		deobfuscator.getSourceTree("e");
		deobfuscator.getSourceTree("f");
		deobfuscator.getSourceTree("g");
		deobfuscator.getSourceTree("g$a");
		deobfuscator.getSourceTree("g$a$a");
		deobfuscator.getSourceTree("g$b");
		deobfuscator.getSourceTree("g$b$a");
		deobfuscator.getSourceTree("h");
		deobfuscator.getSourceTree("h$a");
		deobfuscator.getSourceTree("h$a$a");
		deobfuscator.getSourceTree("h$b");
		deobfuscator.getSourceTree("h$b$a");
		deobfuscator.getSourceTree("h$b$a$a");
		deobfuscator.getSourceTree("h$b$a$b");
		deobfuscator.getSourceTree("i");
		deobfuscator.getSourceTree("i$a");
		deobfuscator.getSourceTree("i$b");
	}
}
