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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassMatching {
	
	private ClassForest m_sourceClasses;
	private ClassForest m_destClasses;
	private BiMap<ClassEntry,ClassEntry> m_knownMatches;
	
	public ClassMatching(ClassIdentifier sourceIdentifier, ClassIdentifier destIdentifier) {
		m_sourceClasses = new ClassForest(sourceIdentifier);
		m_destClasses = new ClassForest(destIdentifier);
		m_knownMatches = HashBiMap.create();
	}
	
	public void addKnownMatches(BiMap<ClassEntry,ClassEntry> knownMatches) {
		m_knownMatches.putAll(knownMatches);
	}
	
	public void match(Iterable<ClassEntry> sourceClasses, Iterable<ClassEntry> destClasses) {
		for (ClassEntry sourceClass : sourceClasses) {
			if (!m_knownMatches.containsKey(sourceClass)) {
				m_sourceClasses.add(sourceClass);
			}
		}
		for (ClassEntry destClass : destClasses) {
			if (!m_knownMatches.containsValue(destClass)) {
				m_destClasses.add(destClass);
			}
		}
	}
	
	public Collection<ClassMatch> matches() {
		List<ClassMatch> matches = Lists.newArrayList();
		for (Entry<ClassEntry,ClassEntry> entry : m_knownMatches.entrySet()) {
			matches.add(new ClassMatch(
				entry.getKey(),
				entry.getValue()
			));
		}
		for (ClassIdentity identity : m_sourceClasses.identities()) {
			matches.add(new ClassMatch(
				m_sourceClasses.getClasses(identity),
				m_destClasses.getClasses(identity)
			));
		}
		for (ClassIdentity identity : m_destClasses.identities()) {
			if (!m_sourceClasses.containsIdentity(identity)) {
				matches.add(new ClassMatch(
					new ArrayList<ClassEntry>(),
					m_destClasses.getClasses(identity)
				));
			}
		}
		return matches;
	}
	
	public Collection<ClassEntry> sourceClasses() {
		Set<ClassEntry> classes = Sets.newHashSet();
		for (ClassMatch match : matches()) {
			classes.addAll(match.sourceClasses);
		}
		return classes;
	}
	
	public Collection<ClassEntry> destClasses() {
		Set<ClassEntry> classes = Sets.newHashSet();
		for (ClassMatch match : matches()) {
			classes.addAll(match.destClasses);
		}
		return classes;
	}
	
	public BiMap<ClassEntry,ClassEntry> uniqueMatches() {
		BiMap<ClassEntry,ClassEntry> uniqueMatches = HashBiMap.create();
		for (ClassMatch match : matches()) {
			if (match.isMatched() && !match.isAmbiguous()) {
				uniqueMatches.put(match.getUniqueSource(), match.getUniqueDest());
			}
		}
		return uniqueMatches;
	}
	
	public Collection<ClassMatch> ambiguousMatches() {
		List<ClassMatch> ambiguousMatches = Lists.newArrayList();
		for (ClassMatch match : matches()) {
			if (match.isMatched() && match.isAmbiguous()) {
				ambiguousMatches.add(match);
			}
		}
		return ambiguousMatches;
	}
	
	public Collection<ClassEntry> unmatchedSourceClasses() {
		List<ClassEntry> classes = Lists.newArrayList();
		for (ClassMatch match : matches()) {
			if (!match.isMatched() && !match.sourceClasses.isEmpty()) {
				classes.addAll(match.sourceClasses);
			}
		}
		return classes;
	}
	
	public Collection<ClassEntry> unmatchedDestClasses() {
		List<ClassEntry> classes = Lists.newArrayList();
		for (ClassMatch match : matches()) {
			if (!match.isMatched() && !match.destClasses.isEmpty()) {
				classes.addAll(match.destClasses);
			}
		}
		return classes;
	}
	
	@Override
	public String toString() {
		
		// count the ambiguous classes
		int numAmbiguousSource = 0;
		int numAmbiguousDest = 0;
		for (ClassMatch match : ambiguousMatches()) {
			numAmbiguousSource += match.sourceClasses.size();
			numAmbiguousDest += match.destClasses.size();
		}
		
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("%20s%8s%8s\n", "", "Source", "Dest"));
		buf.append(String.format("%20s%8d%8d\n", "Classes", sourceClasses().size(), destClasses().size()));
		buf.append(String.format("%20s%8d%8d\n", "Uniquely matched", uniqueMatches().size(), uniqueMatches().size()));
		buf.append(String.format("%20s%8d%8d\n", "Ambiguously matched", numAmbiguousSource, numAmbiguousDest));
		buf.append(String.format("%20s%8d%8d\n", "Unmatched", unmatchedSourceClasses().size(), unmatchedDestClasses().size()));
		return buf.toString();
	}
}
