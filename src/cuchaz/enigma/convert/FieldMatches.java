package cuchaz.enigma.convert;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;


public class FieldMatches {

	private BiMap<FieldEntry,FieldEntry> m_matches;
	private Set<FieldEntry> m_unmatchedSourceFields;
	
	public FieldMatches() {
		m_matches = HashBiMap.create();
		m_unmatchedSourceFields = Sets.newHashSet();
	}
	
	public void addUnmatchedSourceFields(Set<FieldEntry> fieldEntries) {
		m_unmatchedSourceFields.addAll(fieldEntries);
	}
	
	public Collection<ClassEntry> getSourceClassesWithUnmatchedFields() {
		Set<ClassEntry> classEntries = Sets.newHashSet();
		for (FieldEntry fieldEntry : m_unmatchedSourceFields) {
			classEntries.add(fieldEntry.getClassEntry());
		}
		return classEntries;
	}
}
