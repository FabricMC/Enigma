package cuchaz.enigma.convert;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.mapping.ClassEntry;


public class ClassForest {
	
	private ClassIdentifier m_identifier;
	private Multimap<ClassIdentity,ClassEntry> m_forest;
	
	public ClassForest(ClassIdentifier identifier) {
		m_identifier = identifier;
		m_forest = HashMultimap.create();
	}

	public void addAll(Iterable<ClassEntry> entries) {
		for (ClassEntry entry : entries) {
			add(entry);
		}
	}

	public void add(ClassEntry entry) {
		try {
			m_forest.put(m_identifier.identify(entry), entry);
		} catch (ClassNotFoundException ex) {
			throw new Error("Unable to find class " + entry.getName());
		}
	}

	public Collection<ClassIdentity> identities() {
		return m_forest.keySet();
	}
	
	public Collection<ClassEntry> classes() {
		return m_forest.values();
	}

	public Collection<ClassEntry> getClasses(ClassIdentity identity) {
		return m_forest.get(identity);
	}

	public boolean containsIdentity(ClassIdentity identity) {
		return m_forest.containsKey(identity);
	}
}
