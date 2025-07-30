/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.analysis.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;

public class JarIndex implements JarIndexer {
	private final Set<String> indexedClasses = new HashSet<>();
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;
	private final PackageVisibilityIndex packageVisibilityIndex;
	private final EntryResolver entryResolver;

	private final Collection<JarIndexer> indexers;

	private final ConcurrentMap<ClassEntry, List<ParentedEntry<?>>> childrenByClass;

	public JarIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex, BridgeMethodIndex bridgeMethodIndex, PackageVisibilityIndex packageVisibilityIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
		this.packageVisibilityIndex = packageVisibilityIndex;
		this.indexers = List.of(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
		this.entryResolver = new IndexEntryResolver(this);
		this.childrenByClass = new ConcurrentHashMap<>();
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

		classNames.parallelStream().forEach(className -> {
			classProvider.get(className).accept(new IndexClassVisitor(this, Enigma.ASM_VERSION));
		});

		progress.step(2, I18n.translate("progress.jar.indexing.references"));

		classNames.parallelStream().forEach(className -> {
			try {
				classProvider.get(className).accept(new IndexReferenceVisitor(this, Enigma.ASM_VERSION));
			} catch (Exception e) {
				throw new RuntimeException("Exception while indexing class: " + className, e);
			}
		});

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

		if (classEntry.isInnerClass() && !classEntry.getAccess().isSynthetic()) {
			synchronizedAdd(childrenByClass, classEntry.getParent(), classEntry);
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		if (fieldEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexField(fieldEntry));

		if (!fieldEntry.getAccess().isSynthetic()) {
			synchronizedAdd(childrenByClass, fieldEntry.getParent(), fieldEntry);
		}
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		if (methodEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethod(methodEntry));

		if (!methodEntry.getAccess().isSynthetic() && !methodEntry.getName().equals("<clinit>")) {
			synchronizedAdd(childrenByClass, methodEntry.getParent(), methodEntry);
		}
	}

	@Override
	public void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexClassReference(callerEntry, referencedEntry, targetType));
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

	public Map<ClassEntry, List<ParentedEntry<?>>> getChildrenByClass() {
		return this.childrenByClass;
	}

	public boolean isIndexed(String internalName) {
		return indexedClasses.contains(internalName);
	}

	static <K, V> void synchronizedAdd(ConcurrentMap<K, List<V>> map, K key, V value) {
		List<V> list = map.computeIfAbsent(key, k -> new ArrayList<>());
		synchronized (list) {
			list.add(value);
		}
	}
}
