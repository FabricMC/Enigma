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
			newClass("none/a"),
			newClass("none/b"),
			newClass("none/c"),
			newClass("none/d"),
			newClass("none/d$1"),
			newClass("none/e"),
			newClass("none/f"),
			newClass("none/g"),
			newClass("none/g$a"),
			newClass("none/g$a$a"),
			newClass("none/g$b"),
			newClass("none/g$b$a"),
			newClass("none/h"),
			newClass("none/h$a"),
			newClass("none/h$a$a"),
			newClass("none/h$b"),
			newClass("none/h$b$a"),
			newClass("none/h$b$a$a"),
			newClass("none/h$b$a$b"),
			newClass("none/i"),
			newClass("none/i$a"),
			newClass("none/i$b")
		));
	}
	
	@Test
	public void decompile()
	throws Exception {
		Deobfuscator deobfuscator = new Deobfuscator(m_jar);
		deobfuscator.getSourceTree("none/a");
		deobfuscator.getSourceTree("none/b");
		deobfuscator.getSourceTree("none/c");
		deobfuscator.getSourceTree("none/d");
		deobfuscator.getSourceTree("none/d$1");
		deobfuscator.getSourceTree("none/e");
		deobfuscator.getSourceTree("none/f");
		deobfuscator.getSourceTree("none/g");
		deobfuscator.getSourceTree("none/g$a");
		deobfuscator.getSourceTree("none/g$a$a");
		deobfuscator.getSourceTree("none/g$b");
		deobfuscator.getSourceTree("none/g$b$a");
		deobfuscator.getSourceTree("none/h");
		deobfuscator.getSourceTree("none/h$a");
		deobfuscator.getSourceTree("none/h$a$a");
		deobfuscator.getSourceTree("none/h$b");
		deobfuscator.getSourceTree("none/h$b$a");
		deobfuscator.getSourceTree("none/h$b$a$a");
		deobfuscator.getSourceTree("none/h$b$a$b");
		deobfuscator.getSourceTree("none/i");
		deobfuscator.getSourceTree("none/i$a");
		deobfuscator.getSourceTree("none/i$b");
	}
}
