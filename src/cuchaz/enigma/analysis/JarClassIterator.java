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
package cuchaz.enigma.analysis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import com.google.common.collect.Lists;

import cuchaz.enigma.Constants;
import cuchaz.enigma.mapping.ClassEntry;

public class JarClassIterator implements Iterator<CtClass> {
	
	private JarFile m_jar;
	private Iterator<JarEntry> m_iter;
	
	public JarClassIterator(JarFile jar) {
		m_jar = jar;
		
		// get the jar entries that correspond to classes
		List<JarEntry> classEntries = Lists.newArrayList();
		Enumeration<JarEntry> entries = m_jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			
			// is this a class file?
			if (entry.getName().endsWith(".class")) {
				classEntries.add(entry);
			}
		}
		m_iter = classEntries.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return m_iter.hasNext();
	}
	
	@Override
	public CtClass next() {
		JarEntry entry = m_iter.next();
		try {
			return getClass(m_jar, entry);
		} catch (IOException | NotFoundException ex) {
			throw new Error("Unable to load class: " + entry.getName());
		}
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public static List<ClassEntry> getClassEntries(JarFile jar) {
		List<ClassEntry> classEntries = Lists.newArrayList();
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			
			// is this a class file?
			if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
				classEntries.add(getClassEntry(entry));
			}
		}
		return classEntries;
	}
	
	public static Iterable<CtClass> classes(final JarFile jar) {
		return new Iterable<CtClass>() {
			@Override
			public Iterator<CtClass> iterator() {
				return new JarClassIterator(jar);
			}
		};
	}
	
	public static CtClass getClass(JarFile jar, ClassEntry classEntry) {
		try {
			return getClass(jar, new JarEntry(classEntry.getName() + ".class"));
		} catch (IOException | NotFoundException ex) {
			throw new Error("Unable to load class: " + classEntry.getName());
		}
	}
	
	private static CtClass getClass(JarFile jar, JarEntry entry) throws IOException, NotFoundException {
		// read the class into a buffer
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[Constants.KiB];
		int totalNumBytesRead = 0;
		InputStream in = jar.getInputStream(entry);
		while (in.available() > 0) {
			int numBytesRead = in.read(buf);
			if (numBytesRead < 0) {
				break;
			}
			bos.write(buf, 0, numBytesRead);
			
			// sanity checking
			totalNumBytesRead += numBytesRead;
			if (totalNumBytesRead > Constants.MiB) {
				throw new Error("Class file " + entry.getName() + " larger than 1 MiB! Something is wrong!");
			}
		}
		
		// get a javassist handle for the class
		String className = Descriptor.toJavaName(getClassEntry(entry).getName());
		ClassPool classPool = new ClassPool();
		classPool.appendSystemPath();
		classPool.insertClassPath(new ByteArrayClassPath(className, bos.toByteArray()));
		return classPool.get(className);
	}
	
	private static ClassEntry getClassEntry(JarEntry entry) {
		return new ClassEntry(entry.getName().substring(0, entry.getName().length() - ".class".length()));
	}
}
