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
package cuchaz.enigma.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
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
import javassist.bytecode.SignatureAttribute.ArrayType;
import javassist.bytecode.SignatureAttribute.BaseType;
import javassist.bytecode.SignatureAttribute.ClassSignature;
import javassist.bytecode.SignatureAttribute.ClassType;
import javassist.bytecode.SignatureAttribute.MethodSignature;
import javassist.bytecode.SignatureAttribute.NestedClassType;
import javassist.bytecode.SignatureAttribute.ObjectType;
import javassist.bytecode.SignatureAttribute.Type;
import javassist.bytecode.SignatureAttribute.TypeArgument;
import javassist.bytecode.SignatureAttribute.TypeParameter;
import javassist.bytecode.SignatureAttribute.TypeVariable;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.Translator;

public class ClassRenamer {
	
	private static enum SignatureType {
		Class {
			
			@Override
			public String rename(String signature, ReplacerClassMap map) {
				return renameClassSignature(signature, map);
			}
		},
		Field {
			
			@Override
			public String rename(String signature, ReplacerClassMap map) {
				return renameFieldSignature(signature, map);
			}
		},
		Method {
			
			@Override
			public String rename(String signature, ReplacerClassMap map) {
				return renameMethodSignature(signature, map);
			}
		};
		
		public abstract String rename(String signature, ReplacerClassMap map);
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
			}
			return null;
		}
		
		public String get(String className) {
			return m_replacer.replace(className);
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
		String newName = renameClassName(c.getName(), map);
		if (newName != null) {
			c.setName(newName);
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
					String newSignature = type.rename(signatureAttribute.getSignature(), map);
					if (newSignature != null) {
						signatureAttribute.setSignature(newSignature);
					}
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

	private static void renameLocalVariableTypeAttribute(LocalVariableTypeAttribute attribute, ReplacerClassMap map) {
		
		// adapted from LocalVariableAttribute.renameClass()
		ConstPool cp = attribute.getConstPool();
		int n = attribute.tableLength();
		byte[] info = attribute.get();
		for (int i = 0; i < n; ++i) {
			int pos = i * 10 + 2;
			int index = ByteArray.readU16bit(info, pos + 6);
			if (index != 0) {
				String signature = cp.getUtf8Info(index);
				String newSignature = renameLocalVariableSignature(signature, map);
				if (newSignature != null) {
					ByteArray.write16bit(cp.addUtf8Info(newSignature), info, pos + 6);
				}
			}
		}
	}

	private static String renameLocalVariableSignature(String signature, ReplacerClassMap map) {
		
		// for some reason, signatures with . in them don't count as field signatures
		// looks like anonymous classes delimit with . in stead of $
		// convert the . to $, but keep track of how many we replace
		// we need to put them back after we translate
		int start = signature.lastIndexOf('$') + 1;
		int numConverted = 0;
		StringBuilder buf = new StringBuilder(signature);
		for (int i=buf.length()-1; i>=start; i--) {
			char c = buf.charAt(i);
			if (c == '.') {
				buf.setCharAt(i, '$');
				numConverted++;
			}
		}
		signature = buf.toString();
		
		// translate
		String newSignature = renameFieldSignature(signature, map);
		if (newSignature != null) {
			
			// put the delimiters back
			buf = new StringBuilder(newSignature);
			for (int i=buf.length()-1; i>=0 && numConverted > 0; i--) {
				char c = buf.charAt(i);
				if (c == '$') {
					buf.setCharAt(i, '.');
					numConverted--;
				}
			}
			assert(numConverted == 0);
			newSignature = buf.toString();
			
			return newSignature;
		}
		
		return null;
	}

	private static String renameClassSignature(String signature, ReplacerClassMap map) {
		try {
			ClassSignature type = renameType(SignatureAttribute.toClassSignature(signature), map);
			if (type != null) {
				return type.encode();
			}
			return null;
		} catch (BadBytecode ex) {
			throw new Error("Can't parse field signature: " + signature);
		}
	}
	
	private static String renameFieldSignature(String signature, ReplacerClassMap map) {
		try {
			ObjectType type = renameType(SignatureAttribute.toFieldSignature(signature), map);
			if (type != null) {
				return type.encode();
			}
			return null;
		} catch (BadBytecode ex) {
			throw new Error("Can't parse class signature: " + signature);
		}
	}
	
	private static String renameMethodSignature(String signature, ReplacerClassMap map) {
		try {
			MethodSignature type = renameType(SignatureAttribute.toMethodSignature(signature), map);
			if (type != null) {
				return type.encode();
			}
			return null;
		} catch (BadBytecode ex) {
			throw new Error("Can't parse method signature: " + signature);
		}
	}
	
	private static ClassSignature renameType(ClassSignature type, ReplacerClassMap map) {
		
		TypeParameter[] typeParamTypes = type.getParameters();
		if (typeParamTypes != null) {
			typeParamTypes = Arrays.copyOf(typeParamTypes, typeParamTypes.length);
			for (int i=0; i<typeParamTypes.length; i++) {
				TypeParameter newParamType = renameType(typeParamTypes[i], map);
				if (newParamType != null) {
					typeParamTypes[i] = newParamType;
				}
			}
		}
		
		ClassType superclassType = type.getSuperClass();
		if (superclassType != ClassType.OBJECT) {
			ClassType newSuperclassType = renameType(superclassType, map);
			if (newSuperclassType != null) {
				superclassType = newSuperclassType;
			}
		}
		
		ClassType[] interfaceTypes = type.getInterfaces();
		if (interfaceTypes != null) {
			interfaceTypes = Arrays.copyOf(interfaceTypes, interfaceTypes.length);
			for (int i=0; i<interfaceTypes.length; i++) {
				ClassType newInterfaceType = renameType(interfaceTypes[i], map);
				if (newInterfaceType != null) {
					interfaceTypes[i] = newInterfaceType;
				}
			}
		}
		
		return new ClassSignature(typeParamTypes, superclassType, interfaceTypes);
	}
	
	private static MethodSignature renameType(MethodSignature type, ReplacerClassMap map) {
		
		TypeParameter[] typeParamTypes = type.getTypeParameters();
		if (typeParamTypes != null) {
			typeParamTypes = Arrays.copyOf(typeParamTypes, typeParamTypes.length);
			for (int i=0; i<typeParamTypes.length; i++) {
				TypeParameter newParamType = renameType(typeParamTypes[i], map);
				if (newParamType != null) {
					typeParamTypes[i] = newParamType;
				}
			}
		}
		
		Type[] paramTypes = type.getParameterTypes();
		if (paramTypes != null) {
			paramTypes = Arrays.copyOf(paramTypes, paramTypes.length);
			for (int i=0; i<paramTypes.length; i++) {
				Type newParamType = renameType(paramTypes[i], map);
				if (newParamType != null) {
					paramTypes[i] = newParamType;
				}
			}
		}

		Type returnType = type.getReturnType();
		if (returnType != null) {
			Type newReturnType = renameType(returnType, map);
			if (newReturnType != null) {
				returnType = newReturnType;
			}
		}
		
		ObjectType[] exceptionTypes = type.getExceptionTypes();
		if (exceptionTypes != null) {
			exceptionTypes = Arrays.copyOf(exceptionTypes, exceptionTypes.length);
			for (int i=0; i<exceptionTypes.length; i++) {
				ObjectType newExceptionType = renameType(exceptionTypes[i], map);
				if (newExceptionType != null) {
					exceptionTypes[i] = newExceptionType;
				}
			}
		}
		
		return new MethodSignature(typeParamTypes, paramTypes, returnType, exceptionTypes);
	}

	private static Type renameType(Type type, ReplacerClassMap map) {
		if (type instanceof ObjectType) {
			return renameType((ObjectType)type, map);
		} else if (type instanceof BaseType) {
			return renameType((BaseType)type, map);
		} else {
			throw new Error("Don't know how to rename type " + type.getClass());
		}
	}
	
	private static ObjectType renameType(ObjectType type, ReplacerClassMap map) {
		if (type instanceof ArrayType) {
			return renameType((ArrayType)type, map);
		} else if (type instanceof ClassType) {
			return renameType((ClassType)type, map);
		} else if (type instanceof TypeVariable) {
			return renameType((TypeVariable)type, map);
		} else {
			throw new Error("Don't know how to rename type " + type.getClass());
		}
	}
	
	private static BaseType renameType(BaseType type, ReplacerClassMap map) {
		// don't have to rename primitives
		return null;
	}

	private static TypeVariable renameType(TypeVariable type, ReplacerClassMap map) {
		// don't have to rename template args
		return null;
	}

	private static ClassType renameType(ClassType type, ReplacerClassMap map) {
		
		// translate type args
		TypeArgument[] args = type.getTypeArguments();
		if (args != null) {
			args = Arrays.copyOf(args, args.length);
			for (int i=0; i<args.length; i++) {
				TypeArgument newType = renameType(args[i], map);
				if (newType != null) {
					args[i] = newType;
				}
			}
		}
		
		if (type instanceof NestedClassType) {
			NestedClassType nestedType = (NestedClassType)type;
			
			// translate the name
			String name = getClassName(type);
			String newName = map.get(name);
			if (newName != null) {
				name = new ClassEntry(newName).getInnermostClassName();
			}
			
			// translate the parent class too
			ClassType parent = renameType(nestedType.getDeclaringClass(), map);
			if (parent == null) {
				parent = nestedType.getDeclaringClass();
			}
			
			return new NestedClassType(parent, name, args);
		} else {
			
			// translate the name
			String name = type.getName();
			String newName = renameClassName(name, map);
			if (newName != null) {
				name = newName;
			}
			
			return new ClassType(name, args);
		}
	}

	private static String getClassName(ClassType type) {
		if (type instanceof NestedClassType) {
			NestedClassType nestedType = (NestedClassType)type;
			return getClassName(nestedType.getDeclaringClass()) + "$" + Descriptor.toJvmName(type.getName().replace('.', '$'));
		} else {
			return Descriptor.toJvmName(type.getName());
		}
	}

	private static String renameClassName(String name, ReplacerClassMap map) {
		String newName = map.get(Descriptor.toJvmName(name));
		if (newName != null) {
			return Descriptor.toJavaName(newName);
		}
		return null;
	}

	private static TypeArgument renameType(TypeArgument type, ReplacerClassMap map) {
		ObjectType subType = type.getType();
		if (subType != null) {
			ObjectType newSubType = renameType(subType, map);
			if (newSubType != null) {
				switch (type.getKind()) {
					case ' ': return new TypeArgument(newSubType);
					case '+': return TypeArgument.subclassOf(newSubType);
					case '-': return TypeArgument.superOf(newSubType);
					default:
						throw new Error("Unknown type kind: " + type.getKind());
				}
			}
		}
		return null;
	}

	private static ArrayType renameType(ArrayType type, ReplacerClassMap map) {
		Type newSubType = renameType(type.getComponentType(), map);
		if (newSubType != null) {
			return new ArrayType(type.getDimension(), newSubType);
		}
		return null;
	}
	
	private static TypeParameter renameType(TypeParameter type, ReplacerClassMap map) {
		
		ObjectType superclassType = type.getClassBound();
		if (superclassType != null) {
			ObjectType newSuperclassType = renameType(superclassType, map);
			if (newSuperclassType != null) {
				superclassType = newSuperclassType;
			}
		}
		
		ObjectType[] interfaceTypes = type.getInterfaceBound();
		if (interfaceTypes != null) {
			interfaceTypes = Arrays.copyOf(interfaceTypes, interfaceTypes.length);
			for (int i=0; i<interfaceTypes.length; i++) {
				ObjectType newInterfaceType = renameType(interfaceTypes[i], map);
				if (newInterfaceType != null) {
					interfaceTypes[i] = newInterfaceType;
				}
			}
		}
		
		return new TypeParameter(type.getName(), superclassType, interfaceTypes);
	}
}
