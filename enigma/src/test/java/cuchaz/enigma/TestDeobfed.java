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

import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.source.SourceSettings;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TestDeobfed {
	private static Enigma enigma;
	private static ClassCache classCache;
	private static JarIndex index;

	@BeforeClass
	public static void beforeClass() throws Exception {
		enigma = Enigma.create();

		Path obf = Paths.get("build/test-obf/translation.jar");
		Path deobf = Paths.get("build/test-deobf/translation.jar");
		Files.createDirectories(deobf.getParent());
		EnigmaProject project = enigma.openJar(obf, ProgressListener.none());
		project.exportRemappedJar(ProgressListener.none()).write(deobf, ProgressListener.none());

		classCache = ClassCache.of(deobf);
		index = classCache.index(ProgressListener.none());
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
	public void decompile() {
		EnigmaProject project = new EnigmaProject(enigma, classCache, index, new byte[20]);
		Decompiler decompiler = Decompilers.PROCYON.create(project.getClassCache(), new SourceSettings(false, false));

		decompiler.getSource("a");
		decompiler.getSource("b");
		decompiler.getSource("c");
		decompiler.getSource("d");
		decompiler.getSource("d$1");
		decompiler.getSource("e");
		decompiler.getSource("f");
		decompiler.getSource("g");
		decompiler.getSource("g$a");
		decompiler.getSource("g$a$a");
		decompiler.getSource("g$b");
		decompiler.getSource("g$b$a");
		decompiler.getSource("h");
		decompiler.getSource("h$a");
		decompiler.getSource("h$a$a");
		decompiler.getSource("h$b");
		decompiler.getSource("h$b$a");
		decompiler.getSource("h$b$a$a");
		decompiler.getSource("h$b$a$b");
		decompiler.getSource("i");
		decompiler.getSource("i$a");
		decompiler.getSource("i$b");
	}
}
