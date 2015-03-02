package cuchaz.enigma.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;


public class Matches implements Iterable<ClassMatch> {

	Collection<ClassMatch> m_matches;
	Map<ClassEntry,ClassMatch> m_matchesBySource;
	Map<ClassEntry,ClassMatch> m_matchesByDest;
	BiMap<ClassEntry,ClassEntry> m_uniqueMatches;
	Map<ClassEntry,ClassMatch> m_ambiguousMatchesBySource;
	Map<ClassEntry,ClassMatch> m_ambiguousMatchesByDest;
	Set<ClassEntry> m_unmatchedSourceClasses;
	Set<ClassEntry> m_unmatchedDestClasses;
	
	public Matches() {
		this(new ArrayList<ClassMatch>());
	}
	
	public Matches(Collection<ClassMatch> matches) {
		m_matches = matches;
		m_matchesBySource = Maps.newHashMap();
		m_matchesByDest = Maps.newHashMap();
		m_uniqueMatches = HashBiMap.create();
		m_ambiguousMatchesBySource = Maps.newHashMap();
		m_ambiguousMatchesByDest = Maps.newHashMap();
		m_unmatchedSourceClasses = Sets.newHashSet();
		m_unmatchedDestClasses = Sets.newHashSet();
		
		for (ClassMatch match : matches) {
			indexMatch(match);
		}
	}

	public void add(ClassMatch match) {
		m_matches.add(match);
		indexMatch(match);
	}

	public int size() {
		return m_matches.size();
	}

	@Override
	public Iterator<ClassMatch> iterator() {
		return m_matches.iterator();
	}
	
	private void indexMatch(ClassMatch match) {
		if (!match.isMatched()) {
			// unmatched
			m_unmatchedSourceClasses.addAll(match.sourceClasses);
			m_unmatchedDestClasses.addAll(match.destClasses);
		} else {
			if (match.isAmbiguous()) {
				// ambiguously matched
				for (ClassEntry entry : match.sourceClasses) {
					m_ambiguousMatchesBySource.put(entry, match);
				}
				for (ClassEntry entry : match.destClasses) {
					m_ambiguousMatchesByDest.put(entry, match);
				}
			} else {
				// uniquely matched
				m_uniqueMatches.put(match.getUniqueSource(), match.getUniqueDest());
			}
		}
		for (ClassEntry entry : match.sourceClasses) {
			m_matchesBySource.put(entry, match);
		}
		for (ClassEntry entry : match.destClasses) {
			m_matchesByDest.put(entry, match);
		}
	}
	
	public BiMap<ClassEntry,ClassEntry> getUniqueMatches() {
		return m_uniqueMatches;
	}
	
	public Set<ClassEntry> getUnmatchedSourceClasses() {
		return m_unmatchedSourceClasses;
	}
	
	public Set<ClassEntry> getUnmatchedDestClasses() {
		return m_unmatchedDestClasses;
	}

	public Set<ClassEntry> getAmbiguouslyMatchedSourceClasses() {
		return m_ambiguousMatchesBySource.keySet();
	}
	
	public ClassMatch getAmbiguousMatchBySource(ClassEntry sourceClass) {
		return m_ambiguousMatchesBySource.get(sourceClass);
	}

	public ClassMatch getMatchBySource(ClassEntry sourceClass) {
		return m_matchesBySource.get(sourceClass);
	}
	
	public ClassMatch getMatchByDest(ClassEntry destClass) {
		return m_matchesByDest.get(destClass);
	}
}
