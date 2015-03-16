/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.bytecode;

import java.util.Map;
import java.util.Set;

import javassist.ClassMap;
import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.InnerClassesAttribute;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassRenamer {
	
	public static void renameClasses(CtClass c, Map<ClassEntry,ClassEntry> map) {
		
		// build the map used by javassist
		ClassMap nameMap = new ClassMap();
		for (Map.Entry<ClassEntry,ClassEntry> entry : map.entrySet()) {
			nameMap.put(entry.getKey().getName(), entry.getValue().getName());
		}
		
		c.replaceClassName(nameMap);
		
		// replace simple names in the InnerClasses attribute too
		ConstPool constants = c.getClassFile().getConstPool();
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i = 0; i < attr.tableLength(); i++) {
				ClassEntry classEntry = new ClassEntry(Descriptor.toJvmName(attr.innerClass(i)));
				if (attr.innerNameIndex(i) != 0) {
					attr.setInnerNameIndex(i, constants.addUtf8Info(classEntry.getInnermostClassName()));
				}
				
				/* DEBUG
				System.out.println(String.format("\tDEOBF: %s-> ATTR: %s,%s,%s", classEntry, attr.outerClass(i), attr.innerClass(i), attr.innerName(i)));
				*/
			}
		}
	}
	
	public static Set<ClassEntry> getAllClassEntries(final CtClass c) {
		
		// get the classes that javassist knows about
		final Set<ClassEntry> entries = Sets.newHashSet();
		ClassMap map = new ClassMap() {
			@Override
			public Object get(Object obj) {
				if (obj instanceof String) {
					String str = (String)obj;
					
					// javassist throws a lot of weird things at this map
					// I either have to implement my on class scanner, or just try to filter out the weirdness
					// I'm opting to filter out the weirdness for now
					
					// skip anything with generic arguments
					if (str.indexOf('<') >= 0 || str.indexOf('>') >= 0 || str.indexOf(';') >= 0) {
						return null;
					}
					
					// convert path/to/class.inner to path/to/class$inner
					str = str.replace('.', '$');
					
					// remember everything else
					entries.add(new ClassEntry(str));
				}
				return null;
			}
			
			private static final long serialVersionUID = -202160293602070641L;
		};
		c.replaceClassName(map);
		
		return entries;
	}
	
	public static void moveAllClassesOutOfDefaultPackage(CtClass c, String newPackageName) {
		Map<ClassEntry,ClassEntry> map = Maps.newHashMap();
		for (ClassEntry classEntry : ClassRenamer.getAllClassEntries(c)) {
			if (classEntry.isInDefaultPackage()) {
				map.put(classEntry, new ClassEntry(newPackageName + "/" + classEntry.getName()));
			}
		}
		ClassRenamer.renameClasses(c, map);
	}
	
	public static void moveAllClassesIntoDefaultPackage(CtClass c, String oldPackageName) {
		Map<ClassEntry,ClassEntry> map = Maps.newHashMap();
		for (ClassEntry classEntry : ClassRenamer.getAllClassEntries(c)) {
			if (classEntry.getPackageName().equals(oldPackageName)) {
				map.put(classEntry, new ClassEntry(classEntry.getSimpleName()));
			}
		}
		ClassRenamer.renameClasses(c, map);
	}
}
