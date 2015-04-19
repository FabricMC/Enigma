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

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;


public class FieldMatches {

	private BiMap<FieldEntry,FieldEntry> m_matches;
	private Multimap<ClassEntry,FieldEntry> m_matchedSourceFields;
	private Multimap<ClassEntry,FieldEntry> m_unmatchedSourceFields;
	private Multimap<ClassEntry,FieldEntry> m_unmatchedDestFields;
	private Multimap<ClassEntry,FieldEntry> m_unmatchableSourceFields;
	
	public FieldMatches() {
		m_matches = HashBiMap.create();
		m_matchedSourceFields = HashMultimap.create();
		m_unmatchedSourceFields = HashMultimap.create();
		m_unmatchedDestFields = HashMultimap.create();
		m_unmatchableSourceFields = HashMultimap.create();
	}
	
	public void addMatch(FieldEntry srcField, FieldEntry destField) {
		boolean wasAdded = m_matches.put(srcField, destField) == null;
		assert (wasAdded);
		wasAdded = m_matchedSourceFields.put(srcField.getClassEntry(), srcField);
		assert (wasAdded);
	}
	
	public void addUnmatchedSourceField(FieldEntry fieldEntry) {
		boolean wasAdded = m_unmatchedSourceFields.put(fieldEntry.getClassEntry(), fieldEntry);
		assert (wasAdded);
	}
	
	public void addUnmatchedSourceFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedSourceField(fieldEntry);
		}
	}
	
	public void addUnmatchedDestField(FieldEntry fieldEntry) {
		boolean wasAdded = m_unmatchedDestFields.put(fieldEntry.getClassEntry(), fieldEntry);
		assert (wasAdded);
	}
	
	public void addUnmatchedDestFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedDestField(fieldEntry);
		}
	}
	
	public void addUnmatchableSourceField(FieldEntry sourceField) {
		boolean wasAdded = m_unmatchableSourceFields.put(sourceField.getClassEntry(), sourceField);
		assert (wasAdded);
	}
	
	public Set<ClassEntry> getSourceClassesWithUnmatchedFields() {
		return m_unmatchedSourceFields.keySet();
	}
	
	public Collection<ClassEntry> getSourceClassesWithoutUnmatchedFields() {
		Set<ClassEntry> out = Sets.newHashSet();
		out.addAll(m_matchedSourceFields.keySet());
		out.removeAll(m_unmatchedSourceFields.keySet());
		return out;
	}

	public Collection<FieldEntry> getUnmatchedSourceFields() {
		return m_unmatchedSourceFields.values();
	}

	public Collection<FieldEntry> getUnmatchedSourceFields(ClassEntry sourceClass) {
		return m_unmatchedSourceFields.get(sourceClass);
	}

	public Collection<FieldEntry> getUnmatchedDestFields() {
		return m_unmatchedDestFields.values();
	}

	public Collection<FieldEntry> getUnmatchedDestFields(ClassEntry destClass) {
		return m_unmatchedDestFields.get(destClass);
	}
	
	public Collection<FieldEntry> getUnmatchableSourceFields() {
		return m_unmatchableSourceFields.values();
	}

	public boolean hasSource(FieldEntry fieldEntry) {
		return m_matches.containsKey(fieldEntry) || m_unmatchedSourceFields.containsValue(fieldEntry);
	}
	
	public boolean hasDest(FieldEntry fieldEntry) {
		return m_matches.containsValue(fieldEntry) || m_unmatchedDestFields.containsValue(fieldEntry);
	}

	public BiMap<FieldEntry,FieldEntry> matches() {
		return m_matches;
	}
	
	public boolean isMatchedSourceField(FieldEntry sourceField) {
		return m_matches.containsKey(sourceField);
	}

	public boolean isMatchedDestField(FieldEntry destField) {
		return m_matches.containsValue(destField);
	}

	public void makeMatch(FieldEntry sourceField, FieldEntry destField) {
		boolean wasRemoved = m_unmatchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		wasRemoved = m_unmatchedDestFields.remove(destField.getClassEntry(), destField);
		assert (wasRemoved);
		addMatch(sourceField, destField);
	}

	public boolean isMatched(FieldEntry sourceField, FieldEntry destField) {
		FieldEntry match = m_matches.get(sourceField);
		return match != null && match.equals(destField);
	}

	public void unmakeMatch(FieldEntry sourceField, FieldEntry destField) {
		boolean wasRemoved = m_matches.remove(sourceField) != null;
		assert (wasRemoved);
		wasRemoved = m_matchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		addUnmatchedSourceField(sourceField);
		addUnmatchedDestField(destField);
	}
	
	public void makeSourceUnmatchable(FieldEntry sourceField) {
		assert(!isMatchedSourceField(sourceField));
		boolean wasRemoved = m_unmatchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		assert (wasRemoved);
		addUnmatchableSourceField(sourceField);
	}
}
