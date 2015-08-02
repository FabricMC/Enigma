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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Util;
import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.ConstPoolEditor;
import cuchaz.enigma.bytecode.InfoType;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Type;

public class ClassIdentity {
	
	private ClassEntry m_classEntry;
	private SidedClassNamer m_namer;
	private Multiset<String> m_fields;
	private Multiset<String> m_methods;
	private Multiset<String> m_constructors;
	private String m_staticInitializer;
	private String m_extends;
	private Multiset<String> m_implements;
	private Set<String> m_stringLiterals;
	private Multiset<String> m_implementations;
	private Multiset<String> m_references;
	private String m_outer;

	private final ClassNameReplacer m_classNameReplacer = new ClassNameReplacer() {
		
		private Map<String,String> m_classNames = Maps.newHashMap();
		
		@Override
		public String replace(String className) {
			
			// classes not in the none package can be passed through
			ClassEntry classEntry = new ClassEntry(className);
			if (!classEntry.getPackageName().equals(Constants.NonePackage)) {
				return className;
			}
			
			// is this class ourself?
			if (className.equals(m_classEntry.getName())) {
				return "CSelf";
			}
			
			// try the namer
			if (m_namer != null) {
				String newName = m_namer.getName(className);
				if (newName != null) {
					return newName;
				}
			}
			
			// otherwise, use local naming
			if (!m_classNames.containsKey(className)) {
				m_classNames.put(className, getNewClassName());
			}
			return m_classNames.get(className);
		}
		
		private String getNewClassName() {
			return String.format("C%03d", m_classNames.size());
		}
	};

	public ClassIdentity(CtClass c, SidedClassNamer namer, JarIndex index, boolean useReferences) {
		m_namer = namer;
		
		// stuff from the bytecode
		
		m_classEntry = EntryFactory.getClassEntry(c);
		m_fields = HashMultiset.create();
		for (CtField field : c.getDeclaredFields()) {
			m_fields.add(scrubType(field.getSignature()));
		}
		m_methods = HashMultiset.create();
		for (CtMethod method : c.getDeclaredMethods()) {
			m_methods.add(scrubSignature(method.getSignature()) + "0x" + getBehaviorSignature(method));
		}
		m_constructors = HashMultiset.create();
		for (CtConstructor constructor : c.getDeclaredConstructors()) {
			m_constructors.add(scrubSignature(constructor.getSignature()) + "0x" + getBehaviorSignature(constructor));
		}
		m_staticInitializer = "";
		if (c.getClassInitializer() != null) {
			m_staticInitializer = getBehaviorSignature(c.getClassInitializer());
		}
		m_extends = "";
		if (c.getClassFile().getSuperclass() != null) {
			m_extends = scrubClassName(Descriptor.toJvmName(c.getClassFile().getSuperclass()));
		}
		m_implements = HashMultiset.create();
		for (String interfaceName : c.getClassFile().getInterfaces()) {
			m_implements.add(scrubClassName(Descriptor.toJvmName(interfaceName)));
		}
		
		m_stringLiterals = Sets.newHashSet();
		ConstPool constants = c.getClassFile().getConstPool();
		for (int i=1; i<constants.getSize(); i++) {
			if (constants.getTag(i) == ConstPool.CONST_String) {
				m_stringLiterals.add(constants.getStringInfo(i));
			}
		}
		
		// stuff from the jar index
		
		m_implementations = HashMultiset.create();
		ClassImplementationsTreeNode implementationsNode = index.getClassImplementations(null, m_classEntry);
		if (implementationsNode != null) {
			@SuppressWarnings("unchecked")
			Enumeration<ClassImplementationsTreeNode> implementations = implementationsNode.children();
			while (implementations.hasMoreElements()) {
				ClassImplementationsTreeNode node = implementations.nextElement();
				m_implementations.add(scrubClassName(node.getClassEntry().getName()));
			}
		}
		
		m_references = HashMultiset.create();
		if (useReferences) {
			for (CtField field : c.getDeclaredFields()) {
				FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
				for (EntryReference<FieldEntry,BehaviorEntry> reference : index.getFieldReferences(fieldEntry)) {
					addReference(reference);
				}
			}
			for (CtBehavior behavior : c.getDeclaredBehaviors()) {
				BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
				for (EntryReference<BehaviorEntry,BehaviorEntry> reference : index.getBehaviorReferences(behaviorEntry)) {
					addReference(reference);
				}
			}
		}
		
		m_outer = null;
		if (m_classEntry.isInnerClass()) {
			m_outer = m_classEntry.getOuterClassName();
		}
	}
	
	private void addReference(EntryReference<? extends Entry,BehaviorEntry> reference) {
		if (reference.context.getSignature() != null) {
			m_references.add(String.format("%s_%s",
				scrubClassName(reference.context.getClassName()),
				scrubSignature(reference.context.getSignature())
			));
		} else {
			m_references.add(String.format("%s_<clinit>",
				scrubClassName(reference.context.getClassName())
			));
		}
	}
	
	public ClassEntry getClassEntry() {
		return m_classEntry;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("class: ");
		buf.append(m_classEntry.getName());
		buf.append(" ");
		buf.append(hashCode());
		buf.append("\n");
		for (String field : m_fields) {
			buf.append("\tfield ");
			buf.append(field);
			buf.append("\n");
		}
		for (String method : m_methods) {
			buf.append("\tmethod ");
			buf.append(method);
			buf.append("\n");
		}
		for (String constructor : m_constructors) {
			buf.append("\tconstructor ");
			buf.append(constructor);
			buf.append("\n");
		}
		if (m_staticInitializer.length() > 0) {
			buf.append("\tinitializer ");
			buf.append(m_staticInitializer);
			buf.append("\n");
		}
		if (m_extends.length() > 0) {
			buf.append("\textends ");
			buf.append(m_extends);
			buf.append("\n");
		}
		for (String interfaceName : m_implements) {
			buf.append("\timplements ");
			buf.append(interfaceName);
			buf.append("\n");
		}
		for (String implementation : m_implementations) {
			buf.append("\timplemented by ");
			buf.append(implementation);
			buf.append("\n");
		}
		for (String reference : m_references) {
			buf.append("\treference ");
			buf.append(reference);
			buf.append("\n");
		}
		buf.append("\touter ");
		buf.append(m_outer);
		buf.append("\n");
		return buf.toString();
	}
	
	private String scrubClassName(String className) {
		return m_classNameReplacer.replace(className);
	}
	
	private String scrubType(String typeName) {
		return scrubType(new Type(typeName)).toString();
	}
	
	private Type scrubType(Type type) {
		if (type.hasClass()) {
			return new Type(type, m_classNameReplacer);
		} else {
			return type;
		}
	}
	
	private String scrubSignature(String signature) {
		return scrubSignature(new Signature(signature)).toString();
	}
	
	private Signature scrubSignature(Signature signature) {
		return new Signature(signature, m_classNameReplacer);
	}
	
	private boolean isClassMatchedUniquely(String className) {
		return m_namer != null && m_namer.getName(Descriptor.toJvmName(className)) != null;
	}
	
	private String getBehaviorSignature(CtBehavior behavior) {
		try {
			// does this method have an implementation?
			if (behavior.getMethodInfo().getCodeAttribute() == null) {
				return "(none)";
			}
			
			// compute the hash from the opcodes
			ConstPool constants = behavior.getMethodInfo().getConstPool();
			final MessageDigest digest = MessageDigest.getInstance("MD5");
			CodeIterator iter = behavior.getMethodInfo().getCodeAttribute().iterator();
			while (iter.hasNext()) {
				int pos = iter.next();
				
				// update the hash with the opcode
				int opcode = iter.byteAt(pos);
				digest.update((byte)opcode);
				
				switch (opcode) {
					case Opcode.LDC: {
						int constIndex = iter.byteAt(pos + 1);
						updateHashWithConstant(digest, constants, constIndex);
					}
					break;
					
					case Opcode.LDC_W:
					case Opcode.LDC2_W: {
						int constIndex = (iter.byteAt(pos + 1) << 8) | iter.byteAt(pos + 2);
						updateHashWithConstant(digest, constants, constIndex);
					}
					break;
				}
			}
			
			// update hash with method and field accesses
			behavior.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) {
					updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(call.getClassName())));
					updateHashWithString(digest, scrubSignature(call.getSignature()));
					if (isClassMatchedUniquely(call.getClassName())) {
						updateHashWithString(digest, call.getMethodName());
					}
				}
				
				@Override
				public void edit(FieldAccess access) {
					updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(access.getClassName())));
					updateHashWithString(digest, scrubType(access.getSignature()));
					if (isClassMatchedUniquely(access.getClassName())) {
						updateHashWithString(digest, access.getFieldName());
					}
				}
				
				@Override
				public void edit(ConstructorCall call) {
					updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(call.getClassName())));
					updateHashWithString(digest, scrubSignature(call.getSignature()));
				}
				
				@Override
				public void edit(NewExpr expr) {
					updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(expr.getClassName())));
				}
			});
			
			// convert the hash to a hex string
			return toHex(digest.digest());
		} catch (BadBytecode | NoSuchAlgorithmException | CannotCompileException ex) {
			throw new Error(ex);
		}
	}
	
	private void updateHashWithConstant(MessageDigest digest, ConstPool constants, int index) {
		ConstPoolEditor editor = new ConstPoolEditor(constants);
		ConstInfoAccessor item = editor.getItem(index);
		if (item.getType() == InfoType.StringInfo) {
			updateHashWithString(digest, constants.getStringInfo(index));
		}
		// TODO: other constants
	}
	
	private void updateHashWithString(MessageDigest digest, String val) {
		try {
			digest.update(val.getBytes("UTF8"));
		} catch (UnsupportedEncodingException ex) {
			throw new Error(ex);
		}
	}
	
	private String toHex(byte[] bytes) {
		// function taken from:
		// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassIdentity) {
			return equals((ClassIdentity)other);
		}
		return false;
	}
	
	public boolean equals(ClassIdentity other) {
		return m_fields.equals(other.m_fields)
			&& m_methods.equals(other.m_methods)
			&& m_constructors.equals(other.m_constructors)
			&& m_staticInitializer.equals(other.m_staticInitializer)
			&& m_extends.equals(other.m_extends)
			&& m_implements.equals(other.m_implements)
			&& m_implementations.equals(other.m_implementations)
			&& m_references.equals(other.m_references);
	}
	
	@Override
	public int hashCode() {
		List<Object> objs = Lists.newArrayList();
		objs.addAll(m_fields);
		objs.addAll(m_methods);
		objs.addAll(m_constructors);
		objs.add(m_staticInitializer);
		objs.add(m_extends);
		objs.addAll(m_implements);
		objs.addAll(m_implementations);
		objs.addAll(m_references);
		return Util.combineHashesOrdered(objs);
	}
	
	public int getMatchScore(ClassIdentity other) {
		return 2*getNumMatches(m_extends, other.m_extends)
			+ 2*getNumMatches(m_outer, other.m_outer)
			+ 2*getNumMatches(m_implements, other.m_implements)
			 + getNumMatches(m_stringLiterals, other.m_stringLiterals)
			+ getNumMatches(m_fields, other.m_fields)
			+ getNumMatches(m_methods, other.m_methods)
			+ getNumMatches(m_constructors, other.m_constructors);
	}
	
	public int getMaxMatchScore() {
		return 2 + 2 + 2*m_implements.size() + m_stringLiterals.size() + m_fields.size() + m_methods.size() + m_constructors.size();
	}
	
	public boolean matches(CtClass c) {
		// just compare declaration counts
		return m_fields.size() == c.getDeclaredFields().length
			&& m_methods.size() == c.getDeclaredMethods().length
			&& m_constructors.size() == c.getDeclaredConstructors().length;
	}
	
	private int getNumMatches(Set<String> a, Set<String> b) {
		int numMatches = 0;
		for (String val : a) {
			if (b.contains(val)) {
				numMatches++;
			}
		}
		return numMatches;
	}
	
	private int getNumMatches(Multiset<String> a, Multiset<String> b) {
		int numMatches = 0;
		for (String val : a) {
			if (b.contains(val)) {
				numMatches++;
			}
		}
		return numMatches;
	}
	
	private int getNumMatches(String a, String b) {
		if (a == null && b == null) {
			return 1;
		} else if (a != null && b != null && a.equals(b)) {
			return 1;
		}
		return 0;
	}
}
