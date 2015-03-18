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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ByteArray;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InnerClassesAttribute;
import javassist.bytecode.LocalVariableTypeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SignatureAttribute.ClassSignature;
import javassist.bytecode.SignatureAttribute.MethodSignature;
import javassist.bytecode.SignatureAttribute.ObjectType;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.Type;

public class ClassRenamer {
	
	private static enum SignatureType {
		Class {
			
			@Override
			public void rename(SignatureAttribute attribute, ReplacerClassMap map) {
				renameClassSignatureAttribute(attribute, map);
			}
		},
		Field {
			
			@Override
			public void rename(SignatureAttribute attribute, ReplacerClassMap map) {
				renameFieldSignatureAttribute(attribute, map);
			}
		},
		Method {
			
			@Override
			public void rename(SignatureAttribute attribute, ReplacerClassMap map) {
				renameMethodSignatureAttribute(attribute, map);
			}
		};
		
		public abstract void rename(SignatureAttribute attribute, ReplacerClassMap map);
	}
	
	private static class ReplacerClassMap extends HashMap<String,String> {
		
		private static final long serialVersionUID = 317915213205066168L;
		
		private ClassNameReplacer m_replacer;
		
		public ReplacerClassMap(ClassNameReplacer replacer) {
			m_replacer = replacer;
		}
		
		@Override
		public String get(Object obj) {
			if (obj instanceof String) {
				return get((String)obj);
			} else if (obj instanceof ObjectType) {
				return get((ObjectType)obj);
			}
			return null;
		}
		
		public String get(String typeName) {
			
			// javassist doesn't give us the class framing, add it
			typeName = "L" + typeName + ";";
			
			String out = getFramed(typeName);
			if (out == null) {
				return null;
			}

			// javassist doesn't want the class framing, so remove it
			out = out.substring(1, out.length() - 1);
			
			return out;
		}
		
		public String getFramed(String typeName) {
			Type type = new Type(typeName);
			Type renamedType = new Type(type, m_replacer);
			if (!type.equals(renamedType)) {
				return renamedType.toString();
			}
			return null;
		}
		
		public String get(ObjectType type) {
			
			// we can deal with the ones that start with a class
			String signature = type.encode();
			/*
			if (signature.startsWith("L") || signature.startsWith("[")) {
				
				// TEMP: skip special characters for now
				if (signature.indexOf('*') >= 0 || signature.indexOf('+') >= 0 || signature.indexOf('-') >= 0) {
					System.out.println("Skipping translating: " + signature);
					return null;
				}
				
				// replace inner class / with $
				int pos = signature.indexOf("$");
				if (pos >= 0) {
					signature = signature.substring(0, pos + 1) + signature.substring(pos, signature.length()).replace('/', '$');
				}
				
				return getFramed(signature);
			} else if (signature.startsWith("T")) {
				// don't need to care about template names
				return null;
			} else {
			*/
				// TEMP
				System.out.println("Skipping translating: " + signature);
				return null;
			//}
		}
	}
	
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
	
	@SuppressWarnings("unchecked")
	public static void renameClasses(CtClass c, ClassNameReplacer replacer) {
		
		// sadly, we can't use CtClass.renameClass() because SignatureAttribute.renameClass() is extremely buggy =(
		
		ReplacerClassMap map = new ReplacerClassMap(replacer);
		ClassFile classFile = c.getClassFile();
		
		// rename the constant pool (covers ClassInfo, MethodTypeInfo, and NameAndTypeInfo)
		ConstPool constPool = c.getClassFile().getConstPool();
		constPool.renameClass(map);
		
		// rename class attributes
		renameAttributes(classFile.getAttributes(), map, SignatureType.Class);
		
		// rename methods
		for (MethodInfo methodInfo : (List<MethodInfo>)classFile.getMethods()) {
			methodInfo.setDescriptor(Descriptor.rename(methodInfo.getDescriptor(), map));
			renameAttributes(methodInfo.getAttributes(), map, SignatureType.Method);
		}
		
		// rename fields
		for (FieldInfo fieldInfo : (List<FieldInfo>)classFile.getFields()) {
			fieldInfo.setDescriptor(Descriptor.rename(fieldInfo.getDescriptor(), map));
			renameAttributes(fieldInfo.getAttributes(), map, SignatureType.Field);
		}
		
		// rename the class name itself last
		// NOTE: don't use the map here, because setName() calls the buggy SignatureAttribute.renameClass()
		// we only want to replace exactly this class name
		String newName = replacer.replace(Descriptor.toJvmName(c.getName()));
		if (newName != null) {
			c.setName(Descriptor.toJavaName(newName));
		}
		
		// replace simple names in the InnerClasses attribute too
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i = 0; i < attr.tableLength(); i++) {
				
				// get the inner class full name (which has already been translated)
				ClassEntry classEntry = new ClassEntry(Descriptor.toJvmName(attr.innerClass(i)));
				
				if (attr.innerNameIndex(i) != 0) {
					// update the inner name
					attr.setInnerNameIndex(i, constPool.addUtf8Info(classEntry.getInnermostClassName()));
				}
				
				/* DEBUG
				System.out.println(String.format("\tDEOBF: %s-> ATTR: %s,%s,%s", classEntry, attr.outerClass(i), attr.innerClass(i), attr.innerName(i)));
				*/
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void renameAttributes(List<AttributeInfo> attributes, ReplacerClassMap map, SignatureType type) {
		try {
			
			// make the rename class method accessible
			Method renameClassMethod = AttributeInfo.class.getDeclaredMethod("renameClass", Map.class);
			renameClassMethod.setAccessible(true);
			
			for (AttributeInfo attribute : attributes) {
				if (attribute instanceof SignatureAttribute) {
					// this has to be handled specially because SignatureAttribute.renameClass() is buggy as hell
					SignatureAttribute signatureAttribute = (SignatureAttribute)attribute;
					type.rename(signatureAttribute, map);
				} else if (attribute instanceof CodeAttribute) {
					// code attributes have signature attributes too (indirectly)
					CodeAttribute codeAttribute = (CodeAttribute)attribute;
					renameAttributes(codeAttribute.getAttributes(), map, type);
				} else if (attribute instanceof LocalVariableTypeAttribute) {
					// lvt attributes have signature attributes too
					LocalVariableTypeAttribute localVariableAttribute = (LocalVariableTypeAttribute)attribute;
					renameLocalVariableTypeAttribute(localVariableAttribute, map);
				} else {
					renameClassMethod.invoke(attribute, map);
				}
			}
			
		} catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new Error("Unable to call javassist methods by reflection!", ex);
		}
	}

	private static void renameClassSignatureAttribute(SignatureAttribute attribute, ReplacerClassMap map) {
		try {
			ClassSignature classSignature = SignatureAttribute.toClassSignature(attribute.getSignature());
			// TODO: do class signatures
		} catch (BadBytecode ex) {
			throw new Error("Unable to parse class signature: " + attribute.getSignature(), ex);
		}
	}
	
	private static void renameFieldSignatureAttribute(SignatureAttribute attribute, ReplacerClassMap map) {
		try {
			ObjectType fieldSignature = SignatureAttribute.toFieldSignature(attribute.getSignature());
			String newSignature = map.get(fieldSignature);
			if (newSignature != null) {
				attribute.setSignature(newSignature);
			}
		} catch (BadBytecode ex) {
			throw new Error("Unable to parse field signature: " + attribute.getSignature(), ex);
		}
	}
	
	private static void renameMethodSignatureAttribute(SignatureAttribute attribute, ReplacerClassMap map) {
		try {
			MethodSignature methodSignature = SignatureAttribute.toMethodSignature(attribute.getSignature());
			// TODO: do method signatures
		} catch (BadBytecode ex) {
			throw new Error("Unable to parse method signature: " + attribute.getSignature(), ex);
		}
	}
	
	private static void renameLocalVariableTypeAttribute(LocalVariableTypeAttribute attribute, ReplacerClassMap map) {
		// adapted from LocalVariableAttribute.renameClass()
		ConstPool cp = attribute.getConstPool();
		int n = attribute.tableLength();
		byte[] info = attribute.get();
		for (int i = 0; i < n; ++i) {
			int pos = i * 10 + 2;
			int index = ByteArray.readU16bit(info, pos + 6);
			if (index != 0) {
				String desc = cp.getUtf8Info(index);
				try {
					ObjectType fieldSignature = SignatureAttribute.toFieldSignature(desc);
					String newDesc = map.get(fieldSignature);
					if (newDesc != null) {
						ByteArray.write16bit(cp.addUtf8Info(newDesc), info, pos + 6);
					}
				} catch (BadBytecode ex) {
					throw new Error("Unable to parse field signature: " + desc, ex);
				}
			}
		}
	}
}
