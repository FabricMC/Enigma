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

import java.io.File;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;

import cuchaz.enigma.mapping.ClassEntry;

public class TestSourceIndex {
	
	@Test
	public void indexEverything()
	throws Exception {
		
		File home = new File(System.getProperty("user.home"));
		File jarFile = new File(home, "/.minecraft/versions/1.8.3/1.8.3.jar");
		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(jarFile));
		
		// get all classes that aren't inner classes
		Set<ClassEntry> classEntries = Sets.newHashSet();
		for (ClassEntry obfClassEntry : deobfuscator.getJarIndex().getObfClassEntries()) {
			if (!obfClassEntry.isInnerClass()) {
				classEntries.add(obfClassEntry);
			}
		}
		
		for (ClassEntry obfClassEntry : classEntries) {
			try {
				CompilationUnit tree = deobfuscator.getSourceTree(obfClassEntry.getName());
				String source = deobfuscator.getSource(tree);
				deobfuscator.getSourceIndex(tree, source);
			} catch (Throwable t) {
				throw new Error("Unable to index " + obfClassEntry, t);
			}
		}
	}
}
