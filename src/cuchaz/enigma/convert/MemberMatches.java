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
import cuchaz.enigma.mapping.Entry;


public class MemberMatches<T extends Entry> {

	private BiMap<T,T> m_matches;
	private Multimap<ClassEntry,T> m_matchedSourceEntries;
	private Multimap<ClassEntry,T> m_unmatchedSourceEntries;
	private Multimap<ClassEntry,T> m_unmatchedDestEntries;
	private Multimap<ClassEntry,T> m_unmatchableSourceEntries;
	
	public MemberMatches() {
		m_matches = HashBiMap.create();
		m_matchedSourceEntries = HashMultimap.create();
		m_unmatchedSourceEntries = HashMultimap.create();
		m_unmatchedDestEntries = HashMultimap.create();
		m_unmatchableSourceEntries = HashMultimap.create();
	}
	
	public void addMatch(T srcEntry, T destEntry) {
		boolean wasAdded = m_matches.put(srcEntry, destEntry) == null;
		assert (wasAdded);
		wasAdded = m_matchedSourceEntries.put(srcEntry.getClassEntry(), srcEntry);
		assert (wasAdded);
	}
	
	public void addUnmatchedSourceEntry(T sourceEntry) {
		boolean wasAdded = m_unmatchedSourceEntries.put(sourceEntry.getClassEntry(), sourceEntry);
		assert (wasAdded);
	}
	
	public void addUnmatchedSourceEntries(Iterable<T> sourceEntries) {
		for (T sourceEntry : sourceEntries) {
			addUnmatchedSourceEntry(sourceEntry);
		}
	}
	
	public void addUnmatchedDestEntry(T destEntry) {
		boolean wasAdded = m_unmatchedDestEntries.put(destEntry.getClassEntry(), destEntry);
		assert (wasAdded);
	}
	
	public void addUnmatchedDestEntries(Iterable<T> destEntriesntries) {
		for (T entry : destEntriesntries) {
			addUnmatchedDestEntry(entry);
		}
	}
	
	public void addUnmatchableSourceEntry(T sourceEntry) {
		boolean wasAdded = m_unmatchableSourceEntries.put(sourceEntry.getClassEntry(), sourceEntry);
		assert (wasAdded);
	}
	
	public Set<ClassEntry> getSourceClassesWithUnmatchedEntries() {
		return m_unmatchedSourceEntries.keySet();
	}
	
	public Collection<ClassEntry> getSourceClassesWithoutUnmatchedEntries() {
		Set<ClassEntry> out = Sets.newHashSet();
		out.addAll(m_matchedSourceEntries.keySet());
		out.removeAll(m_unmatchedSourceEntries.keySet());
		return out;
	}

	public Collection<T> getUnmatchedSourceEntries() {
		return m_unmatchedSourceEntries.values();
	}

	public Collection<T> getUnmatchedSourceEntries(ClassEntry sourceClass) {
		return m_unmatchedSourceEntries.get(sourceClass);
	}

	public Collection<T> getUnmatchedDestEntries() {
		return m_unmatchedDestEntries.values();
	}

	public Collection<T> getUnmatchedDestEntries(ClassEntry destClass) {
		return m_unmatchedDestEntries.get(destClass);
	}
	
	public Collection<T> getUnmatchableSourceEntries() {
		return m_unmatchableSourceEntries.values();
	}

	public boolean hasSource(T sourceEntry) {
		return m_matches.containsKey(sourceEntry) || m_unmatchedSourceEntries.containsValue(sourceEntry);
	}
	
	public boolean hasDest(T destEntry) {
		return m_matches.containsValue(destEntry) || m_unmatchedDestEntries.containsValue(destEntry);
	}

	public BiMap<T,T> matches() {
		return m_matches;
	}
	
	public boolean isMatchedSourceEntry(T sourceEntry) {
		return m_matches.containsKey(sourceEntry);
	}

	public boolean isMatchedDestEntry(T destEntry) {
		return m_matches.containsValue(destEntry);
	}
	
	public boolean isUnmatchableSourceEntry(T sourceEntry) {
		return m_unmatchableSourceEntries.containsEntry(sourceEntry.getClassEntry(), sourceEntry);
	}

	public void makeMatch(T sourceEntry, T destEntry) {
		boolean wasRemoved = m_unmatchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
		assert (wasRemoved);
		wasRemoved = m_unmatchedDestEntries.remove(destEntry.getClassEntry(), destEntry);
		assert (wasRemoved);
		addMatch(sourceEntry, destEntry);
	}

	public boolean isMatched(T sourceEntry, T destEntry) {
		T match = m_matches.get(sourceEntry);
		return match != null && match.equals(destEntry);
	}

	public void unmakeMatch(T sourceEntry, T destEntry) {
		boolean wasRemoved = m_matches.remove(sourceEntry) != null;
		assert (wasRemoved);
		wasRemoved = m_matchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
		assert (wasRemoved);
		addUnmatchedSourceEntry(sourceEntry);
		addUnmatchedDestEntry(destEntry);
	}
	
	public void makeSourceUnmatchable(T sourceEntry) {
		assert(!isMatchedSourceEntry(sourceEntry));
		boolean wasRemoved = m_unmatchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
		assert (wasRemoved);
		addUnmatchableSourceEntry(sourceEntry);
	}
}
