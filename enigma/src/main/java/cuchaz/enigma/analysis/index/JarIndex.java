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

package cuchaz.enigma.analysis.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import cuchaz.enigma.Enigma;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;

import java.util.*;

public class JarIndex implements JarIndexer {
	private final Set<String> indexedClasses = new HashSet<>();
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;
	private final PackageVisibilityIndex packageVisibilityIndex;
	private final EntryResolver entryResolver;

	private final Collection<JarIndexer> indexers;

	private final Multimap<String, MethodDefEntry> methodImplementations = HashMultimap.create();
	private final Map<ClassEntry, List<ParentedEntry>> childrenByClass;

	public JarIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex, BridgeMethodIndex bridgeMethodIndex, PackageVisibilityIndex packageVisibilityIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
		this.packageVisibilityIndex = packageVisibilityIndex;
		this.indexers = Arrays.asList(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
		this.entryResolver = new IndexEntryResolver(this);
		this.childrenByClass = new HashMap<>();
	}

	public static JarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex);
		PackageVisibilityIndex packageVisibilityIndex = new PackageVisibilityIndex();
		return new JarIndex(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
	}

	public void indexJar(Set<String> classNames, ClassProvider classProvider, ProgressListener progress) {
		indexedClasses.addAll(classNames);
		progress.init(4, I18n.translate("progress.jar.indexing"));

		progress.step(1, I18n.translate("progress.jar.indexing.entries"));

		for (String className : classNames) {
			classProvider.get(className).accept(new IndexClassVisitor(this, Enigma.ASM_VERSION));
		}

		progress.step(2, I18n.translate("progress.jar.indexing.references"));

		for (String className : classNames) {
			classProvider.get(className).accept(new IndexReferenceVisitor(this, entryIndex, inheritanceIndex, Enigma.ASM_VERSION));
		}

		progress.step(3, I18n.translate("progress.jar.indexing.methods"));
		bridgeMethodIndex.findBridgeMethods();

		progress.step(4, I18n.translate("progress.jar.indexing.process"));
		processIndex(this);
	}

	@Override
	public void processIndex(JarIndex index) {
		indexers.forEach(indexer -> indexer.processIndex(index));
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		if (classEntry.isJre()) {
			return;
		}

		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			if (classEntry.equals(interfaceEntry)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + classEntry);
			}
		}

		indexers.forEach(indexer -> indexer.indexClass(classEntry));
		childrenByClass.putIfAbsent(classEntry, new ArrayList<>());
		if (classEntry.isInnerClass() && !classEntry.getAccess().isSynthetic()) {
			childrenByClass.putIfAbsent(classEntry.getParent(), new ArrayList<>());
			childrenByClass.get(classEntry.getParent()).add(classEntry);
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		if (fieldEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexField(fieldEntry));
		if (!fieldEntry.getAccess().isSynthetic()) {
			childrenByClass.putIfAbsent(fieldEntry.getParent(), new ArrayList<>());
			childrenByClass.get(fieldEntry.getParent()).add(fieldEntry);
		}
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		if (methodEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethod(methodEntry));
		if (!methodEntry.getAccess().isSynthetic() && !methodEntry.getName().equals("<clinit>")) {
			childrenByClass.putIfAbsent(methodEntry.getParent(), new ArrayList<>());
			childrenByClass.get(methodEntry.getParent()).add(methodEntry);
		}

		if (!methodEntry.isConstructor()) {
			methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethodReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexFieldReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexLambda(callerEntry, lambda, targetType));
	}

	public EntryIndex getEntryIndex() {
		return entryIndex;
	}

	public InheritanceIndex getInheritanceIndex() {
		return this.inheritanceIndex;
	}

	public ReferenceIndex getReferenceIndex() {
		return referenceIndex;
	}

	public BridgeMethodIndex getBridgeMethodIndex() {
		return bridgeMethodIndex;
	}

	public PackageVisibilityIndex getPackageVisibilityIndex() {
		return packageVisibilityIndex;
	}

	public EntryResolver getEntryResolver() {
		return entryResolver;
	}

	public Map<ClassEntry, List<ParentedEntry>> getChildrenByClass() {
		return this.childrenByClass;
	}

	public boolean isIndexed(String internalName) {
		return indexedClasses.contains(internalName);
	}
}
