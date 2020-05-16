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

package cuchaz.enigma.source.procyon.typeloader;

import com.google.common.collect.Lists;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import cuchaz.enigma.ClassProvider;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class CompiledSourceTypeLoader extends CachingTypeLoader {
	//Store one instance as the classpath shouldn't change during load
	private static final ITypeLoader CLASSPATH_TYPE_LOADER = new CachingClasspathTypeLoader();

	private final ClassProvider compiledSource;
	private final LinkedList<Function<ClassVisitor, ClassVisitor>> visitors = new LinkedList<>();

	public CompiledSourceTypeLoader(ClassProvider compiledSource) {
		this.compiledSource = compiledSource;
	}

	public void addVisitor(Function<ClassVisitor, ClassVisitor> visitor) {
		this.visitors.addFirst(visitor);
	}

	@Override
	protected byte[] doLoad(String className) {
		byte[] data = loadType(className);
		if (data == null) {
			return loadClasspath(className);
		}

		return data;
	}

	private byte[] loadClasspath(String name) {
		Buffer parentBuf = new Buffer();
		if (CLASSPATH_TYPE_LOADER.tryLoadType(name, parentBuf)) {
			return parentBuf.array();
		}
		return EMPTY_ARRAY;
	}

	private byte[] loadType(String className) {
		ClassEntry entry = new ClassEntry(className);

		// find the class in the jar
		ClassNode node = findClassNode(entry);
		if (node == null) {
			// couldn't find it
			return null;
		}

		removeRedundantClassCalls(node);

		ClassWriter writer = new ClassWriter(0);

		ClassVisitor visitor = writer;
		for (Function<ClassVisitor, ClassVisitor> visitorFunction : this.visitors) {
			visitor = visitorFunction.apply(visitor);
		}

		node.accept(visitor);

		// we have a transformed class!
		return writer.toByteArray();
	}

	private void removeRedundantClassCalls(ClassNode node) {
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
	}

	private ClassNode findClassNode(ClassEntry entry) {
		// try to find the class in the jar
		for (String className : getClassNamesToTry(entry)) {
			ClassNode node = compiledSource.getClassNode(className);
			if (node != null) {
				return node;
			}
		}

		// didn't find it  ;_;
		return null;
	}

	private Collection<String> getClassNamesToTry(ClassEntry entry) {
		List<String> classNamesToTry = Lists.newArrayList();
		classNamesToTry.add(entry.getFullName());

		ClassEntry outerClass = entry.getOuterClass();
		if (outerClass != null) {
			classNamesToTry.addAll(getClassNamesToTry(outerClass));
		}

		return classNamesToTry;
	}
}
