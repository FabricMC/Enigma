package cuchaz.enigma.convert;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;


public class FieldMatches {

	private BiMap<FieldEntry,FieldEntry> m_matches;
	private Multimap<ClassEntry,FieldEntry> m_unmatchedSourceFields;
	private Multimap<ClassEntry,FieldEntry> m_unmatchedDestFields;
	
	public FieldMatches() {
		m_matches = HashBiMap.create();
		m_unmatchedSourceFields = HashMultimap.create();
		m_unmatchedDestFields = HashMultimap.create();
	}
	
	public void addMatch(FieldEntry srcField, FieldEntry destField) {
		m_matches.put(srcField, destField);
	}
	
	public void addUnmatchedSourceField(FieldEntry fieldEntry) {
		m_unmatchedSourceFields.put(fieldEntry.getClassEntry(), fieldEntry);
	}
	
	public void addUnmatchedSourceFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedSourceField(fieldEntry);
		}
	}
	
	public void addUnmatchedDestField(FieldEntry fieldEntry) {
		m_unmatchedDestFields.put(fieldEntry.getClassEntry(), fieldEntry);
	}
	
	public void addUnmatchedDestFields(Iterable<FieldEntry> fieldEntries) {
		for (FieldEntry fieldEntry : fieldEntries) {
			addUnmatchedDestField(fieldEntry);
		}
	}
	
	public Set<ClassEntry> getSourceClassesWithUnmatchedFields() {
		return m_unmatchedSourceFields.keySet();
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

	public Collection<FieldEntry> getUnmatchedDestFields(ClassEntry sourceClass) {
		return m_unmatchedDestFields.get(sourceClass);
	}

	public BiMap<FieldEntry,FieldEntry> matches() {
		return m_matches;
	}

	public boolean isDestMatched(FieldEntry destFieldEntry) {
		return m_matches.containsValue(destFieldEntry);
	}

	public void makeMatch(FieldEntry sourceField, FieldEntry destField) {
		m_unmatchedSourceFields.remove(sourceField.getClassEntry(), sourceField);
		m_unmatchedDestFields.remove(destField.getClassEntry(), destField);
		m_matches.put(sourceField, destField);
	}
}
