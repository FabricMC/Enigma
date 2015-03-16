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
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.ParameterizedType;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.Type;

public class ClassRenamer {
	
	public static void renameClasses(CtClass c, final Translator translator) {
		renameClasses(c, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				ClassEntry entry = translator.translateEntry(new ClassEntry(className));
				if (entry != null) {
					return entry.getName();
				}
				return null;
			}
		});
	}
	
	public static void moveAllClassesOutOfDefaultPackage(CtClass c, final String newPackageName) {
		renameClasses(c, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				ClassEntry entry = new ClassEntry(className);
				if (entry.isInDefaultPackage()) {
					return newPackageName + "/" + entry.getName();
				}
				return null;
			}
		});
	}
	
	public static void moveAllClassesIntoDefaultPackage(CtClass c, final String oldPackageName) {
		renameClasses(c, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				ClassEntry entry = new ClassEntry(className);
				if (entry.getPackageName().equals(oldPackageName)) {
					return entry.getSimpleName();
				}
				return null;
			}
		});
	}
	
	public static void renameClasses(CtClass c, ClassNameReplacer replacer) {
		Map<ParameterizedType,ParameterizedType> map = Maps.newHashMap();
		for (ParameterizedType type : ClassRenamer.getAllClassTypes(c)) {
			ParameterizedType renamedType = new ParameterizedType(type, replacer);
			if (!type.equals(renamedType)) {
				map.put(type, renamedType);
			}
		}
		renameTypes(c, map);
	}

	public static Set<ParameterizedType> getAllClassTypes(final CtClass c) {
		
		// TODO: might have to scan SignatureAttributes directly because javassist is buggy
		
		// get the class types that javassist knows about
		final Set<ParameterizedType> types = Sets.newHashSet();
		ClassMap map = new ClassMap() {
			@Override
			public Object get(Object obj) {
				if (obj instanceof String) {
					String str = (String)obj;
					
					// sometimes javasist gives us dot-separated classes... whadda hell?
					str = str.replace('.', '/');
					
					// skip weird types
					boolean hasNestedParams = str.indexOf('<') >= 0 && str.indexOf('<', str.indexOf('<')+1) >= 0;
					boolean hasWeirdChars = str.indexOf('*') >= 0 || str.indexOf('-') >= 0 || str.indexOf('+') >= 0;
					if (hasNestedParams || hasWeirdChars) {
						// TEMP
						System.out.println("Skipped translating: " + str);
						return null;
					}
					
					ParameterizedType type = new ParameterizedType(new Type("L" + str + ";"));
					assert(type.isClass());
					// TEMP
					try {
						type.getClassEntry();
					} catch (Throwable t) {
						// bad type
						// TEMP
						System.out.println("Skipped translating: " + str);
						return null;
					}
					
					types.add(type);
				}
				return null;
			}
			
			private static final long serialVersionUID = -202160293602070641L;
		};
		c.replaceClassName(map);
		
		return types;
	}

	public static void renameTypes(CtClass c, Map<ParameterizedType,ParameterizedType> map) {
		
		// convert the type map to a javassist class map
		ClassMap nameMap = new ClassMap();
		for (Map.Entry<ParameterizedType,ParameterizedType> entry : map.entrySet()) {
			String source = entry.getKey().toString();
			String dest = entry.getValue().toString();
			
			// don't forget to chop off the L ... ;
			// javassist doesn't want it there
			source = source.substring(1, source.length() - 1);
			dest = dest.substring(1, dest.length() - 1);
			
			nameMap.put(source, dest);
		}
		
		// replace!!
		c.replaceClassName(nameMap);

		// replace simple names in the InnerClasses attribute too
		ConstPool constants = c.getClassFile().getConstPool();
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i = 0; i < attr.tableLength(); i++) {
				
				// get the inner class full name (which has already been translated)
				ClassEntry classEntry = new ClassEntry(Descriptor.toJvmName(attr.innerClass(i)));
				
				if (attr.innerNameIndex(i) != 0) {
					// update the inner name
					attr.setInnerNameIndex(i, constants.addUtf8Info(classEntry.getInnermostClassName()));
				}
				
				/* DEBUG
				System.out.println(String.format("\tDEOBF: %s-> ATTR: %s,%s,%s", classEntry, attr.outerClass(i), attr.innerClass(i), attr.innerName(i)));
				*/
			}
		}
	}
}
