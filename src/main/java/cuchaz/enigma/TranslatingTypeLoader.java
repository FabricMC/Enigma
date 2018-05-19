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

package cuchaz.enigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ReferencedEntryPool;
import cuchaz.enigma.mapping.Translator;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Map;

public class TranslatingTypeLoader implements ITypeLoader {

	private final ParsedJar jar;
	private final JarIndex jarIndex;
	private final ReferencedEntryPool entryPool;
	private final Translator obfuscatingTranslator;
	private final Translator deobfuscatingTranslator;
	private final Map<String, byte[]> cache;
	private final ClasspathTypeLoader defaultTypeLoader;

	public TranslatingTypeLoader(ParsedJar jar, JarIndex jarIndex, ReferencedEntryPool entryPool, Translator obfuscatingTranslator, Translator deobfuscatingTranslator) {
		this.jar = jar;
		this.jarIndex = jarIndex;
		this.entryPool = entryPool;
		this.obfuscatingTranslator = obfuscatingTranslator;
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.cache = Maps.newHashMap();
		this.defaultTypeLoader = new ClasspathTypeLoader();

	}

	public void clearCache() {
		this.cache.clear();
	}

	@Override
	public boolean tryLoadType(String className, Buffer out) {

		// check the cache
		byte[] data;
		if (this.cache.containsKey(className)) {
			data = this.cache.get(className);
		} else {
			data = loadType(className);
			this.cache.put(className, data);
		}

		if (data == null) {
			// chain to default desc loader
			return this.defaultTypeLoader.tryLoadType(className, out);
		}

		// send the class to the decompiler
		out.reset(data.length);
		System.arraycopy(data, 0, out.array(), out.position(), data.length);
		out.position(0);
		return true;
	}

	private byte[] loadType(String className) {

		// NOTE: don't know if class name is obf or deobf
		ClassEntry classEntry = new ClassEntry(className);
		ClassEntry obfClassEntry = this.obfuscatingTranslator.getTranslatedClass(classEntry);

		// is this an inner class referenced directly? (ie trying to load b instead of a$b)
		if (!obfClassEntry.isInnerClass()) {
			List<ClassEntry> classChain = this.jarIndex.getObfClassChain(obfClassEntry);
			if (classChain.size() > 1) {
				System.err.println(String.format("WARNING: no class %s after inner class reconstruction. Try %s",
						className, obfClassEntry.buildClassEntry(classChain)
				));
				return null;
			}
		}

		// is this a class we should even know about?
		if (!this.jarIndex.containsObfClass(obfClassEntry)) {
			return null;
		}

		// DEBUG
		//System.out.println(String.format("Looking for %s (obf: %s)", classEntry.getName(), obfClassEntry.getName()));

		// find the class in the jar
		ClassNode node = findClassInJar(obfClassEntry);
		if (node == null) {
			// couldn't find it
			return null;
		}

		ClassWriter writer = new ClassWriter(0);
		createTransformer(node, writer);

		// we have a transformed class!
		return writer.toByteArray();
	}

	private ClassNode findClassInJar(ClassEntry obfClassEntry) {

		// try to find the class in the jar
		for (String className : getClassNamesToTry(obfClassEntry)) {
			ClassNode node = this.jar.getClassNode(className);
			if (node != null) {
				return node;
			}
		}

		// didn't find it  ;_;
		return null;
	}

	public List<String> getClassNamesToTry(String className) {
		return getClassNamesToTry(this.obfuscatingTranslator.getTranslatedClass(new ClassEntry(className)));
	}

	public List<String> getClassNamesToTry(ClassEntry obfClassEntry) {
		List<String> classNamesToTry = Lists.newArrayList();
		classNamesToTry.add(obfClassEntry.getName());
		if (obfClassEntry.isInnerClass()) {
			// try just the inner class name
			classNamesToTry.add(obfClassEntry.getInnermostClassName());
		}
		return classNamesToTry;
	}

	public void createTransformer(ClassNode node, ClassWriter writer) {
		node.accept(new TranslationClassVisitor(deobfuscatingTranslator, jarIndex, entryPool, Opcodes.ASM5, writer));
	}
}
