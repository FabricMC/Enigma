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

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MappingsChecker {
	private final JarIndex index;
	private final BidirectionalMapper mapper;

	public MappingsChecker(JarIndex index, BidirectionalMapper mapper) {
		this.index = index;
		this.mapper = mapper;
	}

	public Dropped dropBrokenMappings() {
		Dropped dropped = new Dropped();
		for (Entry entry : mapper.getObfEntries()) {
			if (entry instanceof ClassEntry) {
				tryDropClass(dropped, (ClassEntry) entry);
			} else if (entry instanceof FieldEntry) {
				tryDropField(dropped, (FieldEntry) entry);
			} else if (entry instanceof MethodEntry) {
				tryDropMethod(dropped, (MethodEntry) entry);
			}
		}
		return dropped;
	}

	private void tryDropClass(Dropped dropped, ClassEntry entry) {
		if (!index.containsObfClass(entry)) {
			EntryMapping mapping = dropMapping(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private void tryDropField(Dropped dropped, FieldEntry entry) {
		if (!index.containsObfField(entry)) {
			EntryMapping mapping = dropMapping(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	private void tryDropMethod(Dropped dropped, MethodEntry entry) {
		if (!index.containsObfMethod(entry)) {
			EntryMapping mapping = dropMapping(entry);
			if (mapping != null) {
				dropped.drop(entry, mapping);
			}
		}
	}

	@Nullable
	private EntryMapping dropMapping(Entry entry) {
		EntryMapping mapping = mapper.getDeobfMapping(entry);
		if (mapping != null) {
			mapper.removeByObf(entry);
		}
		return mapping;
	}

	public static class Dropped {
		private final Map<ClassEntry, String> droppedClassMappings = new HashMap<>();
		private final Map<FieldEntry, String> droppedFieldMappings = new HashMap<>();
		private final Map<MethodEntry, String> droppedMethodMappings = new HashMap<>();

		public void drop(ClassEntry entry, EntryMapping mapping) {
			droppedClassMappings.put(entry, mapping.getTargetName());
		}

		public void drop(FieldEntry entry, EntryMapping mapping) {
			droppedFieldMappings.put(entry, mapping.getTargetName());
		}

		public void drop(MethodEntry entry, EntryMapping mapping) {
			droppedMethodMappings.put(entry, mapping.getTargetName());
		}

		public Map<ClassEntry, String> getDroppedClassMappings() {
			return droppedClassMappings;
		}

		public Map<FieldEntry, String> getDroppedFieldMappings() {
			return droppedFieldMappings;
		}

		public Map<MethodEntry, String> getDroppedMethodMappings() {
			return droppedMethodMappings;
		}
	}
}
