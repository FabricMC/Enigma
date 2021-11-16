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

package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MappingsChecker {
	private final JarIndex index;
	private final EntryTree<EntryMapping> mappings;

	public MappingsChecker(JarIndex index, EntryTree<EntryMapping> mappings) {
		this.index = index;
		this.mappings = mappings;
	}

	public Dropped dropBrokenMappings(ProgressListener progress) {
		Dropped dropped = new Dropped();

		Collection<Entry<?>> obfEntries = mappings.getAllEntries()
				.filter(e -> e instanceof ClassEntry || e instanceof MethodEntry || e instanceof FieldEntry || e instanceof LocalVariableEntry)
				.toList();

		progress.init(obfEntries.size(), "Checking for dropped mappings");

		int steps = 0;
		for (Entry<?> entry : obfEntries) {
			progress.step(steps++, entry.toString());
			tryDropEntry(dropped, entry);
		}

		dropped.apply(mappings);

		return dropped;
	}

	private void tryDropEntry(Dropped dropped, Entry<?> entry) {
		if (shouldDropEntry(entry)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private boolean shouldDropEntry(Entry<?> entry) {
		if (!index.getEntryIndex().hasEntry(entry)) {
			return true;
		}

		if (entry instanceof LocalVariableEntry localVariableEntry) {
			// Drop local variables only if the method entry is to be dropped
			return shouldDropEntry(localVariableEntry.getParent());
		}

		Collection<Entry<?>> resolvedEntries = index.getEntryResolver().resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT);

		if (resolvedEntries.isEmpty()) {
			// Entry doesn't exist at all, drop it.
			return true;
		} else if (resolvedEntries.contains(entry)) {
			// Entry is the root, don't drop it.
			return false;
		}

		if (entry instanceof MethodEntry && mappings.getChildren(entry).size() > 0) {
			// Method entry has parameter names, keep it even though it's not the root.
			return false;
		}

		// Entry is not the root, and is not a method with params
		return true;
	}

	public static class Dropped {
		private final Map<Entry<?>, String> droppedMappings = new HashMap<>();

		public void drop(Entry<?> entry, EntryMapping mapping) {
			droppedMappings.put(entry, mapping.targetName() != null ? mapping.targetName() : entry.getName());
		}

		void apply(EntryTree<EntryMapping> mappings) {
			for (Entry<?> entry : droppedMappings.keySet()) {
				EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
				if (node == null) {
					continue;
				}

				for (Entry<?> childEntry : node.getChildrenRecursively()) {
					mappings.remove(childEntry);
				}
			}
		}

		public Map<Entry<?>, String> getDroppedMappings() {
			return droppedMappings;
		}
	}
}
