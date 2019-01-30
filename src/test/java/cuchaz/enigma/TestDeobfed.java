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

import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.jar.JarFile;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TestDeobfed {

	private static ParsedJar jar;
	private static JarIndex index;

	@BeforeClass
	public static void beforeClass()
		throws Exception {
		jar = new ParsedJar(new JarFile("build/test-deobf/translation.jar"));
		index = JarIndex.empty();
		index.indexJar(jar, s -> {});
	}

	@Test
	public void obfEntries() {
		assertThat(index.getEntryIndex().getClasses(), containsInAnyOrder(
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
		Deobfuscator deobfuscator = new Deobfuscator(jar);
		SourceProvider sourceProvider = deobfuscator.getObfSourceProvider();
		sourceProvider.getSources("a");
		sourceProvider.getSources("b");
		sourceProvider.getSources("c");
		sourceProvider.getSources("d");
		sourceProvider.getSources("d$1");
		sourceProvider.getSources("e");
		sourceProvider.getSources("f");
		sourceProvider.getSources("g");
		sourceProvider.getSources("g$a");
		sourceProvider.getSources("g$a$a");
		sourceProvider.getSources("g$b");
		sourceProvider.getSources("g$b$a");
		sourceProvider.getSources("h");
		sourceProvider.getSources("h$a");
		sourceProvider.getSources("h$a$a");
		sourceProvider.getSources("h$b");
		sourceProvider.getSources("h$b$a");
		sourceProvider.getSources("h$b$a$a");
		sourceProvider.getSources("h$b$a$b");
		sourceProvider.getSources("i");
		sourceProvider.getSources("i$a");
		sourceProvider.getSources("i$b");
	}
}
