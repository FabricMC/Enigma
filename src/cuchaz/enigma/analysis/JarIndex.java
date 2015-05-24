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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InnerClassesAttribute;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.Constants;
import cuchaz.enigma.bytecode.ClassRenamer;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class JarIndex {
	
	private Set<ClassEntry> m_obfClassEntries;
	private TranslationIndex m_translationIndex;
	private Map<Entry,Access> m_access;
	private Multimap<ClassEntry,FieldEntry> m_fields;
	private Multimap<ClassEntry,BehaviorEntry> m_behaviors;
	private Multimap<String,MethodEntry> m_methodImplementations;
	private Multimap<BehaviorEntry,EntryReference<BehaviorEntry,BehaviorEntry>> m_behaviorReferences;
	private Multimap<FieldEntry,EntryReference<FieldEntry,BehaviorEntry>> m_fieldReferences;
	private Multimap<ClassEntry,ClassEntry> m_innerClassesByOuter;
	private Map<ClassEntry,ClassEntry> m_outerClassesByInner;
	private Map<ClassEntry,BehaviorEntry> m_anonymousClasses;
	private Map<MethodEntry,MethodEntry> m_bridgedMethods;
	
	public JarIndex() {
		m_obfClassEntries = Sets.newHashSet();
		m_translationIndex = new TranslationIndex();
		m_access = Maps.newHashMap();
		m_fields = HashMultimap.create();
		m_behaviors = HashMultimap.create();
		m_methodImplementations = HashMultimap.create();
		m_behaviorReferences = HashMultimap.create();
		m_fieldReferences = HashMultimap.create();
		m_innerClassesByOuter = HashMultimap.create();
		m_outerClassesByInner = Maps.newHashMap();
		m_anonymousClasses = Maps.newHashMap();
		m_bridgedMethods = Maps.newHashMap();
	}
	
	public void indexJar(JarFile jar, boolean buildInnerClasses) {
		
		// step 1: read the class names
		for (ClassEntry classEntry : JarClassIterator.getClassEntries(jar)) {
			if (classEntry.isInDefaultPackage()) {
				// move out of default package
				classEntry = new ClassEntry(Constants.NonePackage + "/" + classEntry.getName());
			}
			m_obfClassEntries.add(classEntry);
		}
		
		// step 2: index field/method/constructor access
		for (CtClass c : JarClassIterator.classes(jar)) {
			ClassRenamer.moveAllClassesOutOfDefaultPackage(c, Constants.NonePackage);
			for (CtField field : c.getDeclaredFields()) {
				FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
				m_access.put(fieldEntry, Access.get(field));
				m_fields.put(fieldEntry.getClassEntry(), fieldEntry);
			}
			for (CtBehavior behavior : c.getDeclaredBehaviors()) {
				BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
				m_access.put(behaviorEntry, Access.get(behavior));
				m_behaviors.put(behaviorEntry.getClassEntry(), behaviorEntry);
			}
		}
		
		// step 3: index extends, implements, fields, and methods
		for (CtClass c : JarClassIterator.classes(jar)) {
			ClassRenamer.moveAllClassesOutOfDefaultPackage(c, Constants.NonePackage);
			m_translationIndex.indexClass(c);
			String className = Descriptor.toJvmName(c.getName());
			for (String interfaceName : c.getClassFile().getInterfaces()) {
				className = Descriptor.toJvmName(className);
				interfaceName = Descriptor.toJvmName(interfaceName);
				if (className.equals(interfaceName)) {
					throw new IllegalArgumentException("Class cannot be its own interface! " + className);
				}
			}
			for (CtBehavior behavior : c.getDeclaredBehaviors()) {
				indexBehavior(behavior);
			}
		}
		
		// step 4: index field, method, constructor references
		for (CtClass c : JarClassIterator.classes(jar)) {
			ClassRenamer.moveAllClassesOutOfDefaultPackage(c, Constants.NonePackage);
			for (CtBehavior behavior : c.getDeclaredBehaviors()) {
				indexBehaviorReferences(behavior);
			}
		}
		
		if (buildInnerClasses) {
			
			// step 5: index inner classes and anonymous classes
			for (CtClass c : JarClassIterator.classes(jar)) {
				ClassRenamer.moveAllClassesOutOfDefaultPackage(c, Constants.NonePackage);
				ClassEntry innerClassEntry = EntryFactory.getClassEntry(c);
				ClassEntry outerClassEntry = findOuterClass(c);
				if (outerClassEntry != null) {
					m_innerClassesByOuter.put(outerClassEntry, innerClassEntry);
					boolean innerWasAdded = m_outerClassesByInner.put(innerClassEntry, outerClassEntry) == null;
					assert (innerWasAdded);
					
					BehaviorEntry enclosingBehavior = isAnonymousClass(c, outerClassEntry);
					if (enclosingBehavior != null) {
						m_anonymousClasses.put(innerClassEntry, enclosingBehavior);
						
						// DEBUG
						//System.out.println("ANONYMOUS: " + outerClassEntry.getName() + "$" + innerClassEntry.getSimpleName());
					} else {
						// DEBUG
						//System.out.println("INNER: " + outerClassEntry.getName() + "$" + innerClassEntry.getSimpleName());
					}
				}
			}
			
			// step 6: update other indices with inner class info
			Map<String,String> renames = Maps.newHashMap();
			for (ClassEntry innerClassEntry : m_innerClassesByOuter.values()) {
				String newName = innerClassEntry.buildClassEntry(getObfClassChain(innerClassEntry)).getName();
				if (!innerClassEntry.getName().equals(newName)) {
					// DEBUG
					//System.out.println("REPLACE: " + innerClassEntry.getName() + " WITH " + newName);
					renames.put(innerClassEntry.getName(), newName);
				}
			}
			EntryRenamer.renameClassesInSet(renames, m_obfClassEntries);
			m_translationIndex.renameClasses(renames);
			EntryRenamer.renameClassesInMultimap(renames, m_methodImplementations);
			EntryRenamer.renameClassesInMultimap(renames, m_behaviorReferences);
			EntryRenamer.renameClassesInMultimap(renames, m_fieldReferences);
			EntryRenamer.renameClassesInMap(renames, m_access);
		}
	}
	
	private void indexBehavior(CtBehavior behavior) {
		// get the behavior entry
		final BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
		if (behaviorEntry instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry)behaviorEntry;
			
			// index implementation
			m_methodImplementations.put(behaviorEntry.getClassName(), methodEntry);
			
			// look for bridge and bridged methods
			CtMethod bridgedMethod = getBridgedMethod((CtMethod)behavior);
			if (bridgedMethod != null) {
				m_bridgedMethods.put(methodEntry, EntryFactory.getMethodEntry(bridgedMethod));
			}
		}
		// looks like we don't care about constructors here
	}
	
	private void indexBehaviorReferences(CtBehavior behavior) {
		// index method calls
		final BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
		try {
			behavior.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) {
					MethodEntry calledMethodEntry = EntryFactory.getMethodEntry(call);
					ClassEntry resolvedClassEntry = m_translationIndex.resolveEntryClass(calledMethodEntry);
					if (resolvedClassEntry != null && !resolvedClassEntry.equals(calledMethodEntry.getClassEntry())) {
						calledMethodEntry = new MethodEntry(
							resolvedClassEntry,
							calledMethodEntry.getName(),
							calledMethodEntry.getSignature()
						);
					}
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledMethodEntry,
						call.getMethodName(),
						behaviorEntry
					);
					m_behaviorReferences.put(calledMethodEntry, reference);
				}
				
				@Override
				public void edit(FieldAccess call) {
					FieldEntry calledFieldEntry = EntryFactory.getFieldEntry(call);
					ClassEntry resolvedClassEntry = m_translationIndex.resolveEntryClass(calledFieldEntry);
					if (resolvedClassEntry != null && !resolvedClassEntry.equals(calledFieldEntry.getClassEntry())) {
						calledFieldEntry = new FieldEntry(calledFieldEntry, resolvedClassEntry);
					}
					EntryReference<FieldEntry,BehaviorEntry> reference = new EntryReference<FieldEntry,BehaviorEntry>(
						calledFieldEntry,
						call.getFieldName(),
						behaviorEntry
					);
					m_fieldReferences.put(calledFieldEntry, reference);
				}
				
				@Override
				public void edit(ConstructorCall call) {
					ConstructorEntry calledConstructorEntry = EntryFactory.getConstructorEntry(call);
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						call.getMethodName(),
						behaviorEntry
					);
					m_behaviorReferences.put(calledConstructorEntry, reference);
				}
				
				@Override
				public void edit(NewExpr call) {
					ConstructorEntry calledConstructorEntry = EntryFactory.getConstructorEntry(call);
					EntryReference<BehaviorEntry,BehaviorEntry> reference = new EntryReference<BehaviorEntry,BehaviorEntry>(
						calledConstructorEntry,
						call.getClassName(),
						behaviorEntry
					);
					m_behaviorReferences.put(calledConstructorEntry, reference);
				}
			});
		} catch (CannotCompileException ex) {
			throw new Error(ex);
		}
	}
	
	private CtMethod getBridgedMethod(CtMethod method) {
		
		// bridge methods just call another method, cast it to the return type, and return the result
		// let's see if we can detect this scenario
		
		// skip non-synthetic methods
		if ((method.getModifiers() & AccessFlag.SYNTHETIC) == 0) {
			return null;
		}
		
		// get all the called methods
		final List<MethodCall> methodCalls = Lists.newArrayList();
		try {
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) {
					methodCalls.add(call);
				}
			});
		} catch (CannotCompileException ex) {
			// this is stupid... we're not even compiling anything
			throw new Error(ex);
		}
		
		// is there just one?
		if (methodCalls.size() != 1) {
			return null;
		}
		MethodCall call = methodCalls.get(0);
		
		try {
			// we have a bridge method!
			return call.getMethod();
		} catch (NotFoundException ex) {
			// can't find the type? not a bridge method
			return null;
		}
	}
	
	private ClassEntry findOuterClass(CtClass c) {
		
		ClassEntry classEntry = EntryFactory.getClassEntry(c);
		
		// does this class already have an outer class?
		if (classEntry.isInnerClass()) {
			return classEntry.getOuterClassEntry();
		}
		
		// inner classes:
		// have constructors that can (illegally) set synthetic fields
		// the outer class is the only class that calls constructors
		
		// use the synthetic fields to find the synthetic constructors
		for (CtConstructor constructor : c.getDeclaredConstructors()) {
			Set<String> syntheticFieldTypes = Sets.newHashSet();
			if (!isIllegalConstructor(syntheticFieldTypes, constructor)) {
				continue;
			}
			
			ConstructorEntry constructorEntry = EntryFactory.getConstructorEntry(constructor);
			
			// gather the classes from the illegally-set synthetic fields
			Set<ClassEntry> illegallySetClasses = Sets.newHashSet();
			for (String type : syntheticFieldTypes) {
				if (type.startsWith("L")) {
					ClassEntry outerClassEntry = new ClassEntry(type.substring(1, type.length() - 1));
					if (isSaneOuterClass(outerClassEntry, classEntry)) {
						illegallySetClasses.add(outerClassEntry);
					}
				}
			}
			
			// who calls this constructor?
			Set<ClassEntry> callerClasses = Sets.newHashSet();
			for (EntryReference<BehaviorEntry,BehaviorEntry> reference : getBehaviorReferences(constructorEntry)) {
				
				// make sure it's not a call to super
				if (reference.entry instanceof ConstructorEntry && reference.context instanceof ConstructorEntry) {
					
					// is the entry a superclass of the context?
					ClassEntry calledClassEntry = reference.entry.getClassEntry();
					ClassEntry superclassEntry = m_translationIndex.getSuperclass(reference.context.getClassEntry());
					if (superclassEntry != null && superclassEntry.equals(calledClassEntry)) {
						// it's a super call, skip
						continue;
					}
				}
				
				if (isSaneOuterClass(reference.context.getClassEntry(), classEntry)) {
					callerClasses.add(reference.context.getClassEntry());
				}
			}
			
			// do we have an answer yet?
			if (callerClasses.isEmpty()) {
				if (illegallySetClasses.size() == 1) {
					return illegallySetClasses.iterator().next();
				} else {
					System.out.println(String.format("WARNING: Unable to find outer class for %s. No caller and no illegally set field classes.", classEntry));
				}
			} else {
				if (callerClasses.size() == 1) {
					return callerClasses.iterator().next();
				} else {
					// multiple callers, do the illegally set classes narrow it down?
					Set<ClassEntry> intersection = Sets.newHashSet(callerClasses);
					intersection.retainAll(illegallySetClasses);
					if (intersection.size() == 1) {
						return intersection.iterator().next();
					} else {
						System.out.println(String.format("WARNING: Unable to choose outer class for %s among options: %s", classEntry, callerClasses));
					}
				}
			}
		}
		
		return null;
	}
	
	private boolean isSaneOuterClass(ClassEntry outerClassEntry, ClassEntry innerClassEntry) {
		
		// clearly this would be silly
		if (outerClassEntry.equals(innerClassEntry)) {
			return false;
		}
		
		// is the outer class in the jar?
		if (!m_obfClassEntries.contains(outerClassEntry)) {
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private boolean isIllegalConstructor(Set<String> syntheticFieldTypes, CtConstructor constructor) {
		
		// illegal constructors only set synthetic member fields, then call super()
		String className = constructor.getDeclaringClass().getName();
		
		// collect all the field accesses, constructor calls, and method calls
		final List<FieldAccess> illegalFieldWrites = Lists.newArrayList();
		final List<ConstructorCall> constructorCalls = Lists.newArrayList();
		try {
			constructor.instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess fieldAccess) {
					if (fieldAccess.isWriter() && constructorCalls.isEmpty()) {
						illegalFieldWrites.add(fieldAccess);
					}
				}
				
				@Override
				public void edit(ConstructorCall constructorCall) {
					constructorCalls.add(constructorCall);
				}
			});
		} catch (CannotCompileException ex) {
			// we're not compiling anything... this is stupid
			throw new Error(ex);
		}
		
		// are there any illegal field writes?
		if (illegalFieldWrites.isEmpty()) {
			return false;
		}
		
		// are all the writes to synthetic fields?
		for (FieldAccess fieldWrite : illegalFieldWrites) {
			
			// all illegal writes have to be to the local class
			if (!fieldWrite.getClassName().equals(className)) {
				System.err.println(String.format("WARNING: illegal write to non-member field %s.%s", fieldWrite.getClassName(), fieldWrite.getFieldName()));
				return false;
			}
			
			// find the field
			FieldInfo fieldInfo = null;
			for (FieldInfo info : (List<FieldInfo>)constructor.getDeclaringClass().getClassFile().getFields()) {
				if (info.getName().equals(fieldWrite.getFieldName()) && info.getDescriptor().equals(fieldWrite.getSignature())) {
					fieldInfo = info;
					break;
				}
			}
			if (fieldInfo == null) {
				// field is in a superclass or something, can't be a local synthetic member
				return false;
			}
			
			// is this field synthetic?
			boolean isSynthetic = (fieldInfo.getAccessFlags() & AccessFlag.SYNTHETIC) != 0;
			if (isSynthetic) {
				syntheticFieldTypes.add(fieldInfo.getDescriptor());
			} else {
				System.err.println(String.format("WARNING: illegal write to non synthetic field %s %s.%s", fieldInfo.getDescriptor(), className, fieldInfo.getName()));
				return false;
			}
		}
		
		// we passed all the tests!
		return true;
	}
	
	private BehaviorEntry isAnonymousClass(CtClass c, ClassEntry outerClassEntry) {
		
		// is this class already marked anonymous?
		EnclosingMethodAttribute enclosingMethodAttribute = (EnclosingMethodAttribute)c.getClassFile().getAttribute(EnclosingMethodAttribute.tag);
		if (enclosingMethodAttribute != null) {
			if (enclosingMethodAttribute.methodIndex() > 0) {
				return EntryFactory.getBehaviorEntry(
					Descriptor.toJvmName(enclosingMethodAttribute.className()),
					enclosingMethodAttribute.methodName(),
					enclosingMethodAttribute.methodDescriptor()
				);
			} else {
				// an attribute but no method? assume not anonymous
				return null;
			}
		}
		
		// if there's an inner class attribute, but not an enclosing method attribute, then it's not anonymous
		InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute)c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (innerClassesAttribute != null) {
			return null;
		}
		
		ClassEntry innerClassEntry = new ClassEntry(Descriptor.toJvmName(c.getName()));
		
		// anonymous classes:
		// can't be abstract
		// have only one constructor
		// it's called exactly once by the outer class
		// the type the instance is assigned to can't be this type
		
		// is abstract?
		if (Modifier.isAbstract(c.getModifiers())) {
			return null;
		}
		
		// is there exactly one constructor?
		if (c.getDeclaredConstructors().length != 1) {
			return null;
		}
		CtConstructor constructor = c.getDeclaredConstructors()[0];
		
		// is this constructor called exactly once?
		ConstructorEntry constructorEntry = EntryFactory.getConstructorEntry(constructor);
		Collection<EntryReference<BehaviorEntry,BehaviorEntry>> references = getBehaviorReferences(constructorEntry);
		if (references.size() != 1) {
			return null;
		}
		
		// does the caller use this type?
		BehaviorEntry caller = references.iterator().next().context;
		for (FieldEntry fieldEntry : getReferencedFields(caller)) {
			if (fieldEntry.getType().hasClass() && fieldEntry.getType().getClassEntry().equals(innerClassEntry)) {
				// caller references this type, so it can't be anonymous
				return null;
			}
		}
		for (BehaviorEntry behaviorEntry : getReferencedBehaviors(caller)) {
			if (behaviorEntry.getSignature().hasClass(innerClassEntry)) {
				return null;
			}
		}
		
		return caller;
	}
	
	public Set<ClassEntry> getObfClassEntries() {
		return m_obfClassEntries;
	}
	
	public Collection<FieldEntry> getObfFieldEntries() {
		return m_fields.values();
	}
	
	public Collection<FieldEntry> getObfFieldEntries(ClassEntry classEntry) {
		return m_fields.get(classEntry);
	}
	
	public Collection<BehaviorEntry> getObfBehaviorEntries() {
		return m_behaviors.values();
	}
	
	public Collection<BehaviorEntry> getObfBehaviorEntries(ClassEntry classEntry) {
		return m_behaviors.get(classEntry);
	}
	
	public TranslationIndex getTranslationIndex() {
		return m_translationIndex;
	}
	
	public Access getAccess(Entry entry) {
		return m_access.get(entry);
	}
	
	public ClassInheritanceTreeNode getClassInheritance(Translator deobfuscatingTranslator, ClassEntry obfClassEntry) {
		
		// get the root node
		List<String> ancestry = Lists.newArrayList();
		ancestry.add(obfClassEntry.getName());
		for (ClassEntry classEntry : m_translationIndex.getAncestry(obfClassEntry)) {
			if (containsObfClass(classEntry)) {
				ancestry.add(classEntry.getName());
			}
		}
		ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode(
			deobfuscatingTranslator,
			ancestry.get(ancestry.size() - 1)
		);
		
		// expand all children recursively
		rootNode.load(m_translationIndex, true);
		
		return rootNode;
	}
	
	public ClassImplementationsTreeNode getClassImplementations(Translator deobfuscatingTranslator, ClassEntry obfClassEntry) {
		
		// is this even an interface?
		if (isInterface(obfClassEntry.getClassName())) {
			ClassImplementationsTreeNode node = new ClassImplementationsTreeNode(deobfuscatingTranslator, obfClassEntry);
			node.load(this);
			return node;
		}
		return null;
	}
	
	public MethodInheritanceTreeNode getMethodInheritance(Translator deobfuscatingTranslator, MethodEntry obfMethodEntry) {
		
		// travel to the ancestor implementation
		ClassEntry baseImplementationClassEntry = obfMethodEntry.getClassEntry();
		for (ClassEntry ancestorClassEntry : m_translationIndex.getAncestry(obfMethodEntry.getClassEntry())) {
			MethodEntry ancestorMethodEntry = new MethodEntry(
				new ClassEntry(ancestorClassEntry),
				obfMethodEntry.getName(),
				obfMethodEntry.getSignature()
			);
			if (containsObfBehavior(ancestorMethodEntry)) {
				baseImplementationClassEntry = ancestorClassEntry;
			}
		}
		
		// make a root node at the base
		MethodEntry methodEntry = new MethodEntry(
			baseImplementationClassEntry,
			obfMethodEntry.getName(),
			obfMethodEntry.getSignature()
		);
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
			deobfuscatingTranslator,
			methodEntry,
			containsObfBehavior(methodEntry)
		);
		
		// expand the full tree
		rootNode.load(this, true);
		
		return rootNode;
	}
	
	public List<MethodImplementationsTreeNode> getMethodImplementations(Translator deobfuscatingTranslator, MethodEntry obfMethodEntry) {
		
		List<MethodEntry> interfaceMethodEntries = Lists.newArrayList();
		
		// is this method on an interface?
		if (isInterface(obfMethodEntry.getClassName())) {
			interfaceMethodEntries.add(obfMethodEntry);
		} else {
			// get the interface class
			for (ClassEntry interfaceEntry : getInterfaces(obfMethodEntry.getClassName())) {
				
				// is this method defined in this interface?
				MethodEntry methodInterface = new MethodEntry(
					interfaceEntry,
					obfMethodEntry.getName(),
					obfMethodEntry.getSignature()
				);
				if (containsObfBehavior(methodInterface)) {
					interfaceMethodEntries.add(methodInterface);
				}
			}
		}
		
		List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
		if (!interfaceMethodEntries.isEmpty()) {
			for (MethodEntry interfaceMethodEntry : interfaceMethodEntries) {
				MethodImplementationsTreeNode node = new MethodImplementationsTreeNode(deobfuscatingTranslator, interfaceMethodEntry);
				node.load(this);
				nodes.add(node);
			}
		}
		return nodes;
	}
	
	public Set<MethodEntry> getRelatedMethodImplementations(MethodEntry obfMethodEntry) {
		Set<MethodEntry> methodEntries = Sets.newHashSet();
		getRelatedMethodImplementations(methodEntries, getMethodInheritance(null, obfMethodEntry));
		return methodEntries;
	}
	
	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (containsObfBehavior(methodEntry)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}
		
		// look at interface methods too
		for (MethodImplementationsTreeNode implementationsNode : getMethodImplementations(null, methodEntry)) {
			getRelatedMethodImplementations(methodEntries, implementationsNode);
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodInheritanceTreeNode)node.getChildAt(i));
		}
	}
	
	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (containsObfBehavior(methodEntry)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}
		
		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodImplementationsTreeNode)node.getChildAt(i));
		}
	}
	
	public Collection<EntryReference<FieldEntry,BehaviorEntry>> getFieldReferences(FieldEntry fieldEntry) {
		return m_fieldReferences.get(fieldEntry);
	}
	
	public Collection<FieldEntry> getReferencedFields(BehaviorEntry behaviorEntry) {
		// linear search is fast enough for now
		Set<FieldEntry> fieldEntries = Sets.newHashSet();
		for (EntryReference<FieldEntry,BehaviorEntry> reference : m_fieldReferences.values()) {
			if (reference.context == behaviorEntry) {
				fieldEntries.add(reference.entry);
			}
		}
		return fieldEntries;
	}
	
	public Collection<EntryReference<BehaviorEntry,BehaviorEntry>> getBehaviorReferences(BehaviorEntry behaviorEntry) {
		return m_behaviorReferences.get(behaviorEntry);
	}
	
	public Collection<BehaviorEntry> getReferencedBehaviors(BehaviorEntry behaviorEntry) {
		// linear search is fast enough for now
		Set<BehaviorEntry> behaviorEntries = Sets.newHashSet();
		for (EntryReference<BehaviorEntry,BehaviorEntry> reference : m_behaviorReferences.values()) {
			if (reference.context == behaviorEntry) {
				behaviorEntries.add(reference.entry);
			}
		}
		return behaviorEntries;
	}
	
	public Collection<ClassEntry> getInnerClasses(ClassEntry obfOuterClassEntry) {
		return m_innerClassesByOuter.get(obfOuterClassEntry);
	}
	
	public ClassEntry getOuterClass(ClassEntry obfInnerClassEntry) {
		return m_outerClassesByInner.get(obfInnerClassEntry);
	}
	
	public boolean isAnonymousClass(ClassEntry obfInnerClassEntry) {
		return m_anonymousClasses.containsKey(obfInnerClassEntry);
	}
	
	public BehaviorEntry getAnonymousClassCaller(ClassEntry obfInnerClassName) {
		return m_anonymousClasses.get(obfInnerClassName);
	}
	
	public Set<ClassEntry> getInterfaces(String className) {
		ClassEntry classEntry = new ClassEntry(className);
		Set<ClassEntry> interfaces = new HashSet<ClassEntry>();
		interfaces.addAll(m_translationIndex.getInterfaces(classEntry));
		for (ClassEntry ancestor : m_translationIndex.getAncestry(classEntry)) {
			interfaces.addAll(m_translationIndex.getInterfaces(ancestor));
		}
		return interfaces;
	}
	
	public Set<String> getImplementingClasses(String targetInterfaceName) {
		
		// linear search is fast enough for now
		Set<String> classNames = Sets.newHashSet();
		for (Map.Entry<ClassEntry,ClassEntry> entry : m_translationIndex.getClassInterfaces()) {
			ClassEntry classEntry = entry.getKey();
			ClassEntry interfaceEntry = entry.getValue();
			if (interfaceEntry.getName().equals(targetInterfaceName)) {
				classNames.add(classEntry.getClassName());
				m_translationIndex.getSubclassNamesRecursively(classNames, classEntry);
			}
		}
		return classNames;
	}
	
	public boolean isInterface(String className) {
		return m_translationIndex.isInterface(new ClassEntry(className));
	}
	
	public boolean containsObfClass(ClassEntry obfClassEntry) {
		return m_obfClassEntries.contains(obfClassEntry);
	}
	
	public boolean containsObfField(FieldEntry obfFieldEntry) {
		return m_access.containsKey(obfFieldEntry);
	}
	
	public boolean containsObfBehavior(BehaviorEntry obfBehaviorEntry) {
		return m_access.containsKey(obfBehaviorEntry);
	}
	
	public boolean containsObfArgument(ArgumentEntry obfArgumentEntry) {
		// check the behavior
		if (!containsObfBehavior(obfArgumentEntry.getBehaviorEntry())) {
			return false;
		}
		
		// check the argument
		if (obfArgumentEntry.getIndex() >= obfArgumentEntry.getBehaviorEntry().getSignature().getArgumentTypes().size()) {
			return false;
		}
		
		return true;
	}
	
	public boolean containsObfEntry(Entry obfEntry) {
		if (obfEntry instanceof ClassEntry) {
			return containsObfClass((ClassEntry)obfEntry);
		} else if (obfEntry instanceof FieldEntry) {
			return containsObfField((FieldEntry)obfEntry);
		} else if (obfEntry instanceof BehaviorEntry) {
			return containsObfBehavior((BehaviorEntry)obfEntry);
		} else if (obfEntry instanceof ArgumentEntry) {
			return containsObfArgument((ArgumentEntry)obfEntry);
		} else {
			throw new Error("Entry type not supported: " + obfEntry.getClass().getName());
		}
	}
	
	public MethodEntry getBridgedMethod(MethodEntry bridgeMethodEntry) {
		return m_bridgedMethods.get(bridgeMethodEntry);
	}

	public List<ClassEntry> getObfClassChain(ClassEntry obfClassEntry) {
		
		// build class chain in inner-to-outer order
		List<ClassEntry> obfClassChain = Lists.newArrayList(obfClassEntry);
		ClassEntry checkClassEntry = obfClassEntry;
		while (true) {
			ClassEntry obfOuterClassEntry = getOuterClass(checkClassEntry);
			if (obfOuterClassEntry != null) {
				obfClassChain.add(obfOuterClassEntry);
				checkClassEntry = obfOuterClassEntry;
			} else {
				break;
			}
		}
		
		// switch to outer-to-inner order
		Collections.reverse(obfClassChain);
		
		return obfClassChain;
	}
}
