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
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

public class JarIndex implements JarIndexer {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;
	private final PackageVisibilityIndex packageVisibilityIndex;
	private final EntryResolver entryResolver;

	private final Collection<JarIndexer> indexers;

	private final Multimap<String, MethodDefEntry> methodImplementations = HashMultimap.create();

	public JarIndex(EntryIndex entryIndex, InheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex, BridgeMethodIndex bridgeMethodIndex, PackageVisibilityIndex packageVisibilityIndex) {
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
		this.packageVisibilityIndex = packageVisibilityIndex;
		this.indexers = Arrays.asList(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
		this.entryResolver = new IndexEntryResolver(this);
	}

	public static JarIndex empty() {
		EntryIndex entryIndex = new EntryIndex();
		InheritanceIndex inheritanceIndex = new InheritanceIndex(entryIndex);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex);
		PackageVisibilityIndex packageVisibilityIndex = new PackageVisibilityIndex();
		return new JarIndex(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
	}

	public void indexJar(ClassCache classCache, ProgressListener progress) {
		progress.init(4, "Indexing jar");

		progress.step(1, "Entries");
		classCache.visit(() -> new IndexClassVisitor(this, Opcodes.ASM5), ClassReader.SKIP_CODE);

		progress.step(2, "Entry references");
		classCache.visit(() -> new IndexReferenceVisitor(this, Opcodes.ASM5), ClassReader.SKIP_FRAMES);

		progress.step(3, "Bridge methods");
		bridgeMethodIndex.findBridgeMethods();

		progress.step(4, "Processing");
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
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		if (fieldEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexField(fieldEntry));
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		if (methodEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethod(methodEntry));

		if (!methodEntry.isConstructor()) {
			methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethodReference(callerEntry, referencedEntry));
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexFieldReference(callerEntry, referencedEntry));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexLambda(callerEntry, lambda));
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
}
