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

package cuchaz.enigma.analysis;

import com.google.common.collect.*;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.mapping.entry.*;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class JarIndex {

	private final ReferencedEntryPool entryPool;

	private Set<ClassEntry> obfClassEntries;
	private TranslationIndex translationIndex;
	private Map<Entry, Access> access;
	private Multimap<ClassEntry, FieldDefEntry> fields;
	private Multimap<ClassEntry, MethodDefEntry> methods;
	private Multimap<String, MethodDefEntry> methodImplementations;
	private Multimap<MethodEntry, EntryReference<MethodEntry, MethodDefEntry>> methodsReferencing;
	private Multimap<MethodEntry, MethodEntry> methodReferences;
	private Multimap<FieldEntry, EntryReference<FieldEntry, MethodDefEntry>> fieldReferences;
	private Multimap<ClassEntry, ClassEntry> innerClassesByOuter;
	private Map<ClassEntry, ClassEntry> outerClassesByInner;
	private Map<MethodEntry, MethodEntry> bridgedMethods;
	private Set<MethodEntry> syntheticMethods;

	public JarIndex(ReferencedEntryPool entryPool) {
		this.entryPool = entryPool;
		this.obfClassEntries = Sets.newHashSet();
		this.translationIndex = new TranslationIndex(entryPool);
		this.access = Maps.newHashMap();
		this.fields = HashMultimap.create();
		this.methods = HashMultimap.create();
		this.methodImplementations = HashMultimap.create();
		this.methodsReferencing = HashMultimap.create();
		this.methodReferences = HashMultimap.create();
		this.fieldReferences = HashMultimap.create();
		this.innerClassesByOuter = HashMultimap.create();
		this.outerClassesByInner = Maps.newHashMap();
		this.bridgedMethods = Maps.newHashMap();
		this.syntheticMethods = Sets.newHashSet();
	}

	public void indexJar(ParsedJar jar, boolean buildInnerClasses) {

		// step 1: read the class names
		obfClassEntries.addAll(jar.getClassEntries());

		// step 2: index classes, fields, methods, interfaces
		jar.visit(node -> node.accept(new IndexClassVisitor(this, Opcodes.ASM5)));

		// step 3: index field, method, constructor references
		jar.visit(node -> node.accept(new IndexReferenceVisitor(this, Opcodes.ASM5)));

		// step 4: index access and bridged methods
		for (MethodDefEntry methodEntry : methods.values()) {
			// look for access and bridged methods
			MethodEntry accessedMethod = findAccessMethod(methodEntry);
			if (accessedMethod != null) {
				if (isBridgedMethod(accessedMethod, methodEntry)) {
					this.bridgedMethods.put(methodEntry, accessedMethod);
				}
			}
		}

		if (buildInnerClasses) {
			// step 5: index inner classes and anonymous classes
			jar.visit(node -> node.accept(new IndexInnerClassVisitor(this, Opcodes.ASM5)));

			// step 6: update other indices with inner class info
			Map<String, String> renames = Maps.newHashMap();
			for (ClassEntry innerClassEntry : this.innerClassesByOuter.values()) {
				String newName = innerClassEntry.buildClassEntry(getObfClassChain(innerClassEntry)).getName();
				if (!innerClassEntry.getName().equals(newName)) {
					// DEBUG
					//System.out.println("REPLACE: " + innerClassEntry.getName() + " WITH " + newName);
					renames.put(innerClassEntry.getName(), newName);
				}
			}
			EntryRenamer.renameClassesInSet(renames, this.obfClassEntries);
			this.translationIndex.renameClasses(renames);
			EntryRenamer.renameClassesInMultimap(renames, this.methodImplementations);
			EntryRenamer.renameClassesInMultimap(renames, this.methodsReferencing);
			EntryRenamer.renameClassesInMultimap(renames, this.methodReferences);
			EntryRenamer.renameClassesInMultimap(renames, this.fieldReferences);
			EntryRenamer.renameClassesInMap(renames, this.access);
		}
	}

	protected ClassDefEntry indexClass(int access, String name, String signature, String superName, String[] interfaces) {
		for (String interfaceName : interfaces) {
			if (name.equals(interfaceName)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + name);
			}
		}
		return this.translationIndex.indexClass(access, name, signature, superName, interfaces);
	}

	protected void indexField(ClassDefEntry owner, int access, String name, String desc, String signature) {
		FieldDefEntry fieldEntry = new FieldDefEntry(owner, name, new TypeDescriptor(desc), Signature.createTypedSignature(signature), new AccessFlags(access));
		this.translationIndex.indexField(fieldEntry);
		this.access.put(fieldEntry, Access.get(access));
		this.fields.put(fieldEntry.getOwnerClassEntry(), fieldEntry);
	}

	protected void indexMethod(ClassDefEntry owner, int access, String name, String desc, String signature) {
		MethodDefEntry methodEntry = new MethodDefEntry(owner, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		this.translationIndex.indexMethod(methodEntry);
		this.access.put(methodEntry, Access.get(access));
		this.methods.put(methodEntry.getOwnerClassEntry(), methodEntry);

		if (new AccessFlags(access).isSynthetic()) {
			syntheticMethods.add(methodEntry);
		}

		// we don't care about constructors here
		if (!methodEntry.isConstructor()) {
			// index implementation
			this.methodImplementations.put(methodEntry.getClassName(), methodEntry);
		}
	}

	protected void indexMethodCall(MethodDefEntry callerEntry, String owner, String name, String desc) {
		MethodEntry referencedMethod = new MethodEntry(entryPool.getClass(owner), name, new MethodDescriptor(desc));
		ClassEntry resolvedClassEntry = translationIndex.resolveEntryOwner(referencedMethod);
		if (resolvedClassEntry != null && !resolvedClassEntry.equals(referencedMethod.getOwnerClassEntry())) {
			referencedMethod = referencedMethod.updateOwnership(resolvedClassEntry);
		}
		methodsReferencing.put(referencedMethod, new EntryReference<>(referencedMethod, referencedMethod.getName(), callerEntry));
		methodReferences.put(callerEntry, referencedMethod);
	}

	protected void indexFieldAccess(MethodDefEntry callerEntry, String owner, String name, String desc) {
		FieldEntry referencedField = new FieldEntry(entryPool.getClass(owner), name, new TypeDescriptor(desc));
		ClassEntry resolvedClassEntry = translationIndex.resolveEntryOwner(referencedField);
		if (resolvedClassEntry != null && !resolvedClassEntry.equals(referencedField.getOwnerClassEntry())) {
			referencedField = referencedField.updateOwnership(resolvedClassEntry);
		}
		fieldReferences.put(referencedField, new EntryReference<>(referencedField, referencedField.getName(), callerEntry));
	}

	public void indexInnerClass(ClassEntry innerEntry, ClassEntry outerEntry) {
		this.innerClassesByOuter.put(outerEntry, innerEntry);
		this.outerClassesByInner.putIfAbsent(innerEntry, outerEntry);
	}

	private MethodEntry findAccessMethod(MethodDefEntry method) {

		// we want to find all compiler-added methods that directly call another with no processing

		// skip non-synthetic methods
		if (!method.getAccess().isSynthetic()) {
			return null;
		}

		// get all the methods that we call
		final Collection<MethodEntry> referencedMethods = methodReferences.get(method);

		// is there just one?
		if (referencedMethods.size() != 1) {
			return null;
		}

		return referencedMethods.stream().findFirst().orElse(null);
	}

	private boolean isBridgedMethod(MethodEntry called, MethodEntry access) {
		// Bridged methods will always have the same name as the method they are calling
		// They will also have the same amount of parameters (though equal descriptors cannot be guaranteed)
		if (!called.getName().equals(access.getName()) || called.getDesc().getArgumentDescs().size() != access.getDesc().getArgumentDescs().size()) {
			return false;
		}

		TypeDescriptor accessReturn = access.getDesc().getReturnDesc();
		TypeDescriptor calledReturn = called.getDesc().getReturnDesc();
		if (calledReturn.isVoid() || calledReturn.isPrimitive() || accessReturn.isVoid() || accessReturn.isPrimitive()) {
			return false;
		}

		// Bridged methods will never have the same type as what they are calling
		if (accessReturn.equals(calledReturn)) {
			return false;
		}

		String accessType = accessReturn.toString();

		// If we're casting down from generic type to type-erased Object we're a bridge method
		if (accessType.equals("Ljava/lang/Object;")) {
			return true;
		}

		// Now we need to detect cases where we are being casted down to a higher type bound
		List<ClassEntry> calledAncestry = translationIndex.getAncestry(calledReturn.getTypeEntry());
		return calledAncestry.contains(accessReturn.getTypeEntry());
	}

	public Set<ClassEntry> getObfClassEntries() {
		return this.obfClassEntries;
	}

	public Collection<FieldDefEntry> getObfFieldEntries() {
		return this.fields.values();
	}

	public Collection<FieldDefEntry> getObfFieldEntries(ClassEntry classEntry) {
		return this.fields.get(classEntry);
	}

	public Collection<MethodDefEntry> getObfBehaviorEntries() {
		return this.methods.values();
	}

	public Collection<MethodDefEntry> getObfBehaviorEntries(ClassEntry classEntry) {
		return this.methods.get(classEntry);
	}

	public TranslationIndex getTranslationIndex() {
		return this.translationIndex;
	}

	public Access getAccess(Entry entry) {
		return this.access.get(entry);
	}

	public ClassInheritanceTreeNode getClassInheritance(Translator deobfuscatingTranslator, ClassEntry obfClassEntry) {

		// get the root node
		List<String> ancestry = Lists.newArrayList();
		ancestry.add(obfClassEntry.getName());
		for (ClassEntry classEntry : this.translationIndex.getAncestry(obfClassEntry)) {
			if (containsObfClass(classEntry)) {
				ancestry.add(classEntry.getName());
			}
		}
		ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode(
				deobfuscatingTranslator,
				ancestry.get(ancestry.size() - 1)
		);

		// expand all children recursively
		rootNode.load(this.translationIndex, true);

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
		LinkedList<ClassEntry> entries = new LinkedList<>();
		entries.add(obfMethodEntry.getOwnerClassEntry());

		// TODO: This could be optimized to not go through interfaces repeatedly...

		ClassEntry baseImplementationClassEntry = obfMethodEntry.getOwnerClassEntry();

		for (ClassEntry itf : getInterfaces(obfMethodEntry.getOwnerClassEntry().getClassName())) {
			MethodEntry itfMethodEntry = entryPool.getMethod(itf, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
			if (itfMethodEntry != null && containsObfMethod(itfMethodEntry)) {
				baseImplementationClassEntry = itf;
			}
		}

		for (ClassEntry ancestorClassEntry : this.translationIndex.getAncestry(entries.remove())) {
			MethodEntry ancestorMethodEntry = entryPool.getMethod(ancestorClassEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
			if (ancestorMethodEntry != null) {
				if (containsObfMethod(ancestorMethodEntry)) {
					baseImplementationClassEntry = ancestorClassEntry;
				}

				for (ClassEntry itf : getInterfaces(ancestorClassEntry.getClassName())) {
					MethodEntry itfMethodEntry = entryPool.getMethod(itf, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
					if (itfMethodEntry != null && containsObfMethod(itfMethodEntry)) {
						baseImplementationClassEntry = itf;
					}
				}
			}
		}

		// make a root node at the base
		MethodEntry methodEntry = entryPool.getMethod(baseImplementationClassEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
				deobfuscatingTranslator,
				methodEntry,
				containsObfMethod(methodEntry)
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
				MethodEntry methodInterface = entryPool.getMethod(interfaceEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
				if (methodInterface != null && containsObfMethod(methodInterface)) {
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
		getRelatedMethodImplementations(methodEntries, getMethodInheritance(new DirectionalTranslator(entryPool), obfMethodEntry));
		return methodEntries;
	}

	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (methodEntries.contains(methodEntry)) {
			return;
		}

		if (containsObfMethod(methodEntry)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(getRelatedMethodImplementations(bridgedMethod));
			bridgedMethod = getBridgedMethod(bridgedMethod);
		}

		// look at interface methods too
		for (MethodImplementationsTreeNode implementationsNode : getMethodImplementations(new DirectionalTranslator(entryPool), methodEntry)) {
			getRelatedMethodImplementations(methodEntries, implementationsNode);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodInheritanceTreeNode) node.getChildAt(i));
		}
	}

	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (containsObfMethod(methodEntry)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(getRelatedMethodImplementations(bridgedMethod));
			bridgedMethod = getBridgedMethod(bridgedMethod);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodImplementationsTreeNode) node.getChildAt(i));
		}
	}

	public Collection<EntryReference<FieldEntry, MethodDefEntry>> getFieldReferences(FieldEntry fieldEntry) {
		return this.fieldReferences.get(fieldEntry);
	}

	public Collection<FieldEntry> getReferencedFields(MethodDefEntry methodEntry) {
		// linear search is fast enough for now
		Set<FieldEntry> fieldEntries = Sets.newHashSet();
		for (EntryReference<FieldEntry, MethodDefEntry> reference : this.fieldReferences.values()) {
			if (reference.context == methodEntry) {
				fieldEntries.add(reference.entry);
			}
		}
		return fieldEntries;
	}

	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getMethodsReferencing(MethodEntry methodEntry) {
		return this.methodsReferencing.get(methodEntry);
	}

	public Collection<MethodEntry> getReferencedMethods(MethodDefEntry methodEntry) {
		return this.methodReferences.get(methodEntry);
	}

	public Collection<ClassEntry> getInnerClasses(ClassEntry obfOuterClassEntry) {
		return this.innerClassesByOuter.get(obfOuterClassEntry);
	}

	public ClassEntry getOuterClass(ClassEntry obfInnerClassEntry) {
		return this.outerClassesByInner.get(obfInnerClassEntry);
	}

	public boolean isSyntheticMethod(MethodEntry methodEntry) {
		return this.syntheticMethods.contains(methodEntry);
	}

	public Set<ClassEntry> getInterfaces(String className) {
		ClassEntry classEntry = entryPool.getClass(className);
		Set<ClassEntry> interfaces = new HashSet<>(this.translationIndex.getInterfaces(classEntry));
		for (ClassEntry ancestor : this.translationIndex.getAncestry(classEntry)) {
			interfaces.addAll(this.translationIndex.getInterfaces(ancestor));
		}
		return interfaces;
	}

	public Set<String> getImplementingClasses(String targetInterfaceName) {

		// linear search is fast enough for now
		Set<String> classNames = Sets.newHashSet();
		for (Map.Entry<ClassEntry, ClassEntry> entry : this.translationIndex.getClassInterfaces()) {
			ClassEntry classEntry = entry.getKey();
			ClassEntry interfaceEntry = entry.getValue();
			if (interfaceEntry.getName().equals(targetInterfaceName)) {
				String className = classEntry.getClassName();
				classNames.add(className);
				if (isInterface(className)) {
					classNames.addAll(getImplementingClasses(className));
				}

				this.translationIndex.getSubclassNamesRecursively(classNames, classEntry);
			}
		}
		return classNames;
	}

	public boolean isInterface(String className) {
		return this.translationIndex.isInterface(entryPool.getClass(className));
	}

	public boolean containsObfClass(ClassEntry obfClassEntry) {
		return this.obfClassEntries.contains(obfClassEntry);
	}

	public boolean containsObfField(FieldEntry obfFieldEntry) {
		return this.access.containsKey(obfFieldEntry);
	}

	public boolean containsObfMethod(MethodEntry obfMethodEntry) {
		return this.access.containsKey(obfMethodEntry);
	}

	public boolean containsEntryWithSameName(Entry entry) {
		for (Entry target : this.access.keySet())
			if (target.getName().equals(entry.getName()) && entry.getClass().isInstance(target.getClass()))
				return true;
		return false;
	}

	public boolean containsObfVariable(LocalVariableEntry obfVariableEntry) {
		// check the behavior
		if (!containsObfMethod(obfVariableEntry.getOwnerEntry())) {
			return false;
		}

		return true;
	}

	public boolean containsObfEntry(Entry obfEntry) {
		if (obfEntry instanceof ClassEntry) {
			return containsObfClass((ClassEntry) obfEntry);
		} else if (obfEntry instanceof FieldEntry) {
			return containsObfField((FieldEntry) obfEntry);
		} else if (obfEntry instanceof MethodEntry) {
			return containsObfMethod((MethodEntry) obfEntry);
		} else if (obfEntry instanceof LocalVariableEntry) {
			return containsObfVariable((LocalVariableEntry) obfEntry);
		} else {
			throw new Error("Entry desc not supported: " + obfEntry.getClass().getName());
		}
	}

	public MethodEntry getBridgedMethod(MethodEntry bridgeMethodEntry) {
		return this.bridgedMethods.get(bridgeMethodEntry);
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
