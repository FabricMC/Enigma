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

import cuchaz.enigma.mapping.ClassEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ParsedJar {
	private final Map<String, ClassNode> nodes = new LinkedHashMap<>();

	public ParsedJar(JarFile jar) throws IOException {
		try {
			// get the jar entries that correspond to classes
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				// is this a class file?
				if (entry.getName().endsWith(".class")) {
					try (InputStream input = jar.getInputStream(entry)) {
						// read the ClassNode from the jar
						ClassReader reader = new ClassReader(input);
						ClassNode node = new ClassNode();
						reader.accept(node, 0);
						String path = entry.getName().substring(0, entry.getName().length() - ".class".length());
						nodes.put(path, node);
					}
				}
			}
		} finally {
			jar.close();
		}
	}

	public void visit(Consumer<ClassNode> visitor) {
		for (ClassNode node : nodes.values()) {
			visitor.accept(node);
		}
	}

	public int getClassCount() {
		return nodes.size();
	}

	public List<ClassEntry> getClassEntries() {
		List<ClassEntry> entries = new ArrayList<>(nodes.size());
		for (ClassNode node : nodes.values()) {
			entries.add(new ClassEntry(node.name));
		}
		return entries;
	}

	public ClassNode getClassNode(String name) {
		return nodes.get(name);
	}
}
