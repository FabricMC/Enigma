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

package cuchaz.enigma.convert;

import com.google.common.collect.*;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;

import java.util.Collection;
import java.util.Set;

public class FieldMatches {

	private BiMap<FieldEntry, FieldEntry> matches;
	private Multimap<ClassEntry, FieldEntry> matchedSourceFields;
	private Multimap<ClassEntry, FieldEntry> unmatchedSourceFields;
	private Multimap<ClassEntry, FieldEntry> unmatchedDestFields;
	private Multimap<ClassEntry, FieldEntry> unmatchableSourceFields;

	public FieldMatches() {
		matches = HashBiMap.create();
		matchedSourceFields = HashMultimap.create();
		unmatchedSourceFields = HashMultimap.create();
		unmatchedDestFields = HashMultimap.create();
		unmatchableSourceFields = HashMultimap.create();
	}

	public void addMatch(FieldEntry srcField, FieldEntry destField) {
		boolean wasAdded = matches.put(srcField, destField) == null;
		assert (wasAdded);
		wasAdded = matchedSourceFields.put(srcField.getClassEntry(), srcField);
		assert (wasAdded);
	}

	public void addUnmatchedSourceField(FieldEntry fieldEntry) {
		boolean wasAdded = unmatchedSourceFields.put(fieldEntry.getClassEntry(), fieldEntry);
		assert (wasAdded);
	}

	public void addUnmatchedSourceFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedSourceField(fieldEntry);
		}
	}

	public void addUnmatchedDestField(FieldEntry fieldEntry) {
		boolean wasAdded = unmatchedDestFields.put(fieldEntry.getClassEntry(), fieldEntry);
		assert (wasAdded);
	}

	public void addUnmatchedDestFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedDestField(fieldEntry);
		}
	}

	public void addUnmatchableSourceField(FieldEntry sourceField) {
		boolean wasAdded = unmatchableSourceFields.put(sourceField.getClassEntry(), sourceField);
		assert (wasAdded);
	}

	public Set<ClassEntry> getSourceClassesWithUnmatchedFields() {
		return unmatchedSourceFields.keySet();
	}

	public Collection<ClassEntry> getSourceClassesWithoutUnmatchedFields() {
		Set<ClassEntry> out = Sets.newHashSet();
		out.addAll(matchedSourceFields.keySet());
		out.removeAll(unmatchedSourceFields.keySet());
		return out;
	}

	public Collection<FieldEntry> getUnmatchedSourceFields() {
		return unmatchedSourceFields.values();
	}

	public Collection<FieldEntry> getUnmatchedSourceFields(ClassEntry sourceClass) {
		return unmatchedSourceFields.get(sourceClass);
	}

	public Collection<FieldEntry> getUnmatchedDestFields() {
		return unmatchedDestFields.values();
	}

	public Collection<FieldEntry> getUnmatchedDestFields(ClassEntry destClass) {
		return unmatchedDestFields.get(destClass);
	}

	public Collection<FieldEntry> getUnmatchableSourceFields() {
		return unmatchableSourceFields.values();
	}

	public boolean hasSource(FieldEntry fieldEntry) {
		return matches.containsKey(fieldEntry) || unmatchedSourceFields.containsValue(fieldEntry);
	}

	public boolean hasDest(FieldEntry fieldEntry) {
		return matches.containsValue(fieldEntry) || unmatchedDestFields.containsValue(fieldEntry);
	}

	public BiMap<FieldEntry, FieldEntry> matches() {
		return matches;
	}

	public boolean isMatchedSourceField(FieldEntry sourceField) {
		return matches.containsKey(sourceField);
	}

	public boolean isMatchedDestField(FieldEntry destField) {
		return matches.containsValue(destField);
	}

	public void makeMatch(FieldEntry sourceField, FieldEntry destField) {
		boolean wasRemoved = unmatchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		wasRemoved = unmatchedDestFields.remove(destField.getClassEntry(), destField);
		assert (wasRemoved);
		addMatch(sourceField, destField);
	}

	public boolean isMatched(FieldEntry sourceField, FieldEntry destField) {
		FieldEntry match = matches.get(sourceField);
		return match != null && match.equals(destField);
	}

	public void unmakeMatch(FieldEntry sourceField, FieldEntry destField) {
		boolean wasRemoved = matches.remove(sourceField) != null;
		assert (wasRemoved);
		wasRemoved = matchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		addUnmatchedSourceField(sourceField);
		addUnmatchedDestField(destField);
	}

	public void makeSourceUnmatchable(FieldEntry sourceField) {
		assert (!isMatchedSourceField(sourceField));
		boolean wasRemoved = unmatchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		addUnmatchableSourceField(sourceField);
	}
}
