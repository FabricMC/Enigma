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
package cuchaz.enigma.convert;

import java.util.Map;
import java.util.jar.JarFile;

import com.google.common.collect.Maps;

import javassist.CtClass;
import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;


public class ClassIdentifier {
	
	private JarIndex m_index;
	private SidedClassNamer m_namer;
	private boolean m_useReferences;
	private TranslatingTypeLoader m_loader;
	private Map<ClassEntry,ClassIdentity> m_cache;
	
	public ClassIdentifier(JarFile jar, JarIndex index, SidedClassNamer namer, boolean useReferences) {
		m_index = index;
		m_namer = namer;
		m_useReferences = useReferences;
		m_loader = new TranslatingTypeLoader(jar, index);
		m_cache = Maps.newHashMap();
	}
	
	public ClassIdentity identify(ClassEntry classEntry)
	throws ClassNotFoundException {
		ClassIdentity identity = m_cache.get(classEntry);
		if (identity == null) {
			CtClass c = m_loader.loadClass(classEntry.getName());
			if (c == null) {
				throw new ClassNotFoundException(classEntry.getName());
			}
			identity = new ClassIdentity(c, m_namer, m_index, m_useReferences);
			m_cache.put(classEntry, identity);
		}
		return identity;
	}
}
