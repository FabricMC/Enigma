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
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.jar.JarFile;

import javassist.CtClass;
import javassist.CtField;

import com.google.common.collect.Maps;

import cuchaz.enigma.analysis.JarClassIterator;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.FieldMapping;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.Type;

public class MainFormatConverter {
	
	public static void main(String[] args)
	throws Exception {
		
		System.out.println("Getting field types from jar...");
		
		JarFile jar = new JarFile(System.getProperty("user.home") + "/.minecraft/versions/1.8/1.8.jar");
		Map<String,Type> fieldTypes = Maps.newHashMap();
		for (CtClass c : JarClassIterator.classes(jar)) {
			for (CtField field : c.getDeclaredFields()) {
				FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
				fieldTypes.put(getFieldKey(fieldEntry), moveClasssesOutOfDefaultPackage(fieldEntry.getType()));
			}
		}
		
		System.out.println("Reading mappings...");
		
		File fileMappings = new File("../Enigma Mappings/1.8.mappings");
		MappingsReader mappingsReader = new MappingsReader() {
			
			@Override
			protected FieldMapping readField(String[] parts) {
				// assume the void type for now
				return new FieldMapping(parts[1], new Type("V"), parts[2]);
			}
		};
		Mappings mappings = mappingsReader.read(new FileReader(fileMappings));
		
		System.out.println("Updating field types...");
		
		for (ClassMapping classMapping : mappings.classes()) {
			updateFieldsInClass(fieldTypes, classMapping);
		}
		
		System.out.println("Saving mappings...");
		
		try (FileWriter writer = new FileWriter(fileMappings)) {
			new MappingsWriter().write(writer, mappings);
		}
		
		System.out.println("Done!");
	}

	private static Type moveClasssesOutOfDefaultPackage(Type type) {
		return new Type(type, new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				ClassEntry entry = new ClassEntry(className);
				if (entry.isInDefaultPackage()) {
					return Constants.NonePackage + "/" + className;
				}
				return null;
			}
		});
	}

	private static void updateFieldsInClass(Map<String,Type> fieldTypes, ClassMapping classMapping)
	throws Exception {
		
		// update the fields
		for (FieldMapping fieldMapping : classMapping.fields()) {
			setFieldType(fieldTypes, classMapping, fieldMapping);
		}
		
		// recurse
		for (ClassMapping innerClassMapping : classMapping.innerClasses()) {
			updateFieldsInClass(fieldTypes, innerClassMapping);
		}
	}

	private static void setFieldType(Map<String,Type> fieldTypes, ClassMapping classMapping, FieldMapping fieldMapping)
	throws Exception {
		
		// get the new type
		Type newType = fieldTypes.get(getFieldKey(classMapping, fieldMapping));
		if (newType == null) {
			throw new Error("Can't find type for field: " + getFieldKey(classMapping, fieldMapping));
		}
		
		// hack in the new field type
		Field field = fieldMapping.getClass().getDeclaredField("m_obfType");
		field.setAccessible(true);
		field.set(fieldMapping, newType);
	}

	private static Object getFieldKey(ClassMapping classMapping, FieldMapping fieldMapping) {
		return classMapping.getObfSimpleName() + "." + fieldMapping.getObfName();
	}

	private static String getFieldKey(FieldEntry obfFieldEntry) {
		return obfFieldEntry.getClassEntry().getSimpleName() + "." + obfFieldEntry.getName();
	}
}
