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

import com.google.common.collect.Sets;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class TestSourceIndex {
	@Test
	public void indexEverything()
		throws Exception {
		// Figure out where Minecraft is...
		final String mcDir = System.getProperty("enigma.test.minecraftdir");
		Path mcJar = null;
		if (mcDir == null) {
			String osname = System.getProperty("os.name").toLowerCase();
			if (osname.contains("nix") || osname.contains("nux") || osname.contains("solaris")) {
				mcJar = Paths.get(System.getProperty("user.home"), ".minecraft/versions/1.8.3/1.8.3.jar");
			} else if (osname.contains("mac") || osname.contains("darwin")) {
				mcJar = Paths.get(System.getProperty("user.home"), "Library/Application Support/minecraft/versions/1.8.3/1.8.3.jar");
			} else if (osname.contains("win")) {
				mcJar = Paths.get(System.getenv("AppData"), ".minecraft/versions/1.8.3/1.8.3.jar");
			}
		} else {
			mcJar = Paths.get(mcDir, "versions/1.8.3/1.8.3.jar");
		}

		if (mcJar == null) {
			throw new NullPointerException("Couldn't find jar");
		}

		Enigma enigma = Enigma.create();
		EnigmaProject project = enigma.openJar(mcJar, ProgressListener.none());

		ClassCache classCache = project.getClassCache();
		JarIndex index = project.getJarIndex();

		CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(classCache);
		SourceProvider sourceProvider = new SourceProvider(SourceProvider.createSettings(), typeLoader);

		// get all classes that aren't inner classes
		Set<ClassEntry> classEntries = Sets.newHashSet();
		for (ClassEntry obfClassEntry : index.getEntryIndex().getClasses()) {
			if (!obfClassEntry.isInnerClass()) {
				classEntries.add(obfClassEntry);
			}
		}

		for (ClassEntry obfClassEntry : classEntries) {
			try {
				CompilationUnit tree = sourceProvider.getSources(obfClassEntry.getName());
				String source = sourceProvider.writeSourceToString(tree);

				SourceIndex.buildIndex(source, tree, true);
			} catch (Throwable t) {
				throw new Error("Unable to index " + obfClassEntry, t);
			}
		}
	}
}
