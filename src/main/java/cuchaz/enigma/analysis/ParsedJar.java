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

import com.google.common.io.ByteStreams;
import cuchaz.enigma.CompiledSource;
import cuchaz.enigma.bytecode.translators.LocalVariableFixVisitor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class ParsedJar implements CompiledSource {
	private final Map<String, byte[]> classBytes;
	private final Map<String, ClassNode> nodeCache = new HashMap<>();

	public ParsedJar(JarFile jar) throws IOException {
		Map<String, byte[]> uClassBytes = new LinkedHashMap<>();
		try {
			// get the jar entries that correspond to classes
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				// is this a class file?
				if (entry.getName().endsWith(".class")) {
					try (InputStream input = new BufferedInputStream(jar.getInputStream(entry))) {
						String path = entry.getName().substring(0, entry.getName().length() - ".class".length());
						uClassBytes.put(path, ByteStreams.toByteArray(input));
					}
				}
			}
		} finally {
			jar.close();
			classBytes = Collections.unmodifiableMap(uClassBytes);
		}
	}

	public ParsedJar(JarInputStream jar) throws IOException {
		Map<String, byte[]> uClassBytes = new LinkedHashMap<>();
		try {
			// get the jar entries that correspond to classes
			JarEntry entry;
			while ((entry = jar.getNextJarEntry()) != null) {
				// is this a class file?
				if (entry.getName().endsWith(".class")) {
					String path = entry.getName().substring(0, entry.getName().length() - ".class".length());
					uClassBytes.put(path, ByteStreams.toByteArray(jar));
					jar.closeEntry();
				}
			}
		} finally {
			jar.close();
			classBytes = Collections.unmodifiableMap(uClassBytes);
		}
	}

	public void visitReader(Function<String, ClassVisitor> visitorFunction, int options) {
		for (String s : classBytes.keySet()) {
			ClassNode nodeCached = nodeCache.get(s);
			if (nodeCached != null) {
				nodeCached.accept(visitorFunction.apply(s));
			} else {
				new ClassReader(classBytes.get(s)).accept(visitorFunction.apply(s), options);
			}
		}
	}

	public void visitNode(Consumer<ClassNode> consumer) {
		for (String s : classBytes.keySet()) {
			consumer.accept(getClassNode(s));
		}
	}

	public int getClassCount() {
		return classBytes.size();
	}

	@Nullable
	@Override
	public ClassNode getClassNode(String name) {
		return nodeCache.computeIfAbsent(name, (n) -> {
			byte[] bytes = classBytes.get(name);
			if (bytes == null) {
				return null;
			}

			ClassReader reader = new ClassReader(bytes);
			ClassNode node = new ClassNode();

			LocalVariableFixVisitor visitor = new LocalVariableFixVisitor(Opcodes.ASM5, node);
			reader.accept(visitor, 0);

			return node;
		});
	}

	public List<ClassEntry> getClassEntries() {
		List<ClassEntry> entries = new ArrayList<>(classBytes.size());
		for (String s : classBytes.keySet()) {
			entries.add(new ClassEntry(s));
		}
		return entries;
	}

	public Map<String, byte[]> getClassDataMap() {
		return classBytes;
	}
}
