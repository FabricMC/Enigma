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
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.ReferencedEntryPool;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class TranslatingTypeLoader extends CachingTypeLoader implements ITranslatingTypeLoader {
	//Store one instance as the classpath shouldnt change during load
	private static final ITypeLoader defaultTypeLoader = new CachingClasspathTypeLoader();

	private final ParsedJar jar;
	private final JarIndex jarIndex;
	private final ReferencedEntryPool entryPool;
	private final Translator obfuscatingTranslator;
	private final Translator deobfuscatingTranslator;

	public TranslatingTypeLoader(ParsedJar jar, JarIndex jarIndex, ReferencedEntryPool entryPool, Translator obfuscatingTranslator, Translator deobfuscatingTranslator) {
		this.jar = jar;
		this.jarIndex = jarIndex;
		this.entryPool = entryPool;
		this.obfuscatingTranslator = obfuscatingTranslator;
		this.deobfuscatingTranslator = deobfuscatingTranslator;
	}

	protected byte[] doLoad(String className) {
		byte[] data = loadType(className);
		if (data == null) {
			// chain to default desc loader
			Buffer parentBuf = new Buffer();
			if (defaultTypeLoader.tryLoadType(className, parentBuf)) {
				return parentBuf.array();
			}
			return EMPTY_ARRAY;//need to return *something* as null means no store
		}
		return data;
	}

	private byte[] loadType(String className) {

		// NOTE: don't know if class name is obf or deobf
		ClassEntry classEntry = new ClassEntry(className);
		ClassEntry obfClassEntry = this.obfuscatingTranslator.translate(classEntry);

		// is this a class we should even know about?
		if (!jarIndex.getEntryIndex().hasClass(obfClassEntry)) {
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


		// remove <obj>.getClass() calls that are seemingly injected
		//	DUP
		//	INVOKEVIRTUAL java/lang/Object.getClass ()Ljava/lang/Class;
		//	POP
		for (MethodNode methodNode : node.methods) {
			AbstractInsnNode insnNode = methodNode.instructions.getFirst();
			while (insnNode != null) {
				if (insnNode instanceof MethodInsnNode && insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
					if (methodInsnNode.name.equals("getClass") && methodInsnNode.owner.equals("java/lang/Object") && methodInsnNode.desc.equals("()Ljava/lang/Class;")) {
						AbstractInsnNode previous = methodInsnNode.getPrevious();
						AbstractInsnNode next = methodInsnNode.getNext();
						if (previous.getOpcode() == Opcodes.DUP && next.getOpcode() == Opcodes.POP) {
							insnNode = previous.getPrevious();//reset the iterator so it gets the new next instruction
							methodNode.instructions.remove(previous);
							methodNode.instructions.remove(methodInsnNode);
							methodNode.instructions.remove(next);
						}
					}
				}
				insnNode = insnNode.getNext();
			}
		}

		ClassWriter writer = new ClassWriter(0);
		transformInto(node, writer);

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

	@Override
	public List<String> getClassNamesToTry(String className) {
		return getClassNamesToTry(this.obfuscatingTranslator.translate(new ClassEntry(className)));
	}

	@Override
	public List<String> getClassNamesToTry(ClassEntry obfClassEntry) {
		List<String> classNamesToTry = Lists.newArrayList();
		classNamesToTry.add(obfClassEntry.getFullName());

		ClassEntry outerClass = obfClassEntry.getOuterClass();
		if (outerClass != null) {
			classNamesToTry.addAll(getClassNamesToTry(outerClass));
		}

		return classNamesToTry;
	}

	@Override
	public String transformInto(ClassNode node, ClassWriter writer) {
		node.accept(new TranslationClassVisitor(deobfuscatingTranslator, jarIndex, entryPool, Opcodes.ASM5, writer));
		return deobfuscatingTranslator.translate(new ClassEntry(node.name)).getFullName();
	}

}
