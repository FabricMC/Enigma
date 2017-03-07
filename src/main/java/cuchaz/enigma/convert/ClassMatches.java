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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

import cuchaz.enigma.mapping.ClassEntry;


public class ClassMatches implements Iterable<ClassMatch> {

    private Collection<ClassMatch>        matches;
    private Map<ClassEntry, ClassMatch>   matchesBySource;
    private Map<ClassEntry, ClassMatch>   matchesByDest;
    private BiMap<ClassEntry, ClassEntry> uniqueMatches;
    private Map<ClassEntry, ClassMatch>   ambiguousMatchesBySource;
    private Map<ClassEntry, ClassMatch>   ambiguousMatchesByDest;
    private Set<ClassEntry>               unmatchedSourceClasses;
    private Set<ClassEntry>               unmatchedDestClasses;

    public ClassMatches() {
        this(new ArrayList<>());
    }

    public ClassMatches(Collection<ClassMatch> matches) {
        this.matches = matches;
        matchesBySource = Maps.newHashMap();
        matchesByDest = Maps.newHashMap();
        uniqueMatches = HashBiMap.create();
        ambiguousMatchesBySource = Maps.newHashMap();
        ambiguousMatchesByDest = Maps.newHashMap();
        unmatchedSourceClasses = Sets.newHashSet();
        unmatchedDestClasses = Sets.newHashSet();

        for (ClassMatch match : matches) {
            indexMatch(match);
        }
    }

    public void add(ClassMatch match) {
        matches.add(match);
        indexMatch(match);
    }

    public void remove(ClassMatch match) {
        for (ClassEntry sourceClass : match.sourceClasses) {
            matchesBySource.remove(sourceClass);
            uniqueMatches.remove(sourceClass);
            ambiguousMatchesBySource.remove(sourceClass);
            unmatchedSourceClasses.remove(sourceClass);
        }
        for (ClassEntry destClass : match.destClasses) {
            matchesByDest.remove(destClass);
            uniqueMatches.inverse().remove(destClass);
            ambiguousMatchesByDest.remove(destClass);
            unmatchedDestClasses.remove(destClass);
        }
        matches.remove(match);
    }

    public int size() {
        return matches.size();
    }

    @Override
    public Iterator<ClassMatch> iterator() {
        return matches.iterator();
    }

    private void indexMatch(ClassMatch match) {
        if (!match.isMatched()) {
            // unmatched
            unmatchedSourceClasses.addAll(match.sourceClasses);
            unmatchedDestClasses.addAll(match.destClasses);
        } else {
            if (match.isAmbiguous()) {
                // ambiguously matched
                for (ClassEntry entry : match.sourceClasses) {
                    ambiguousMatchesBySource.put(entry, match);
                }
                for (ClassEntry entry : match.destClasses) {
                    ambiguousMatchesByDest.put(entry, match);
                }
            } else {
                // uniquely matched
                uniqueMatches.put(match.getUniqueSource(), match.getUniqueDest());
            }
        }
        for (ClassEntry entry : match.sourceClasses) {
            matchesBySource.put(entry, match);
        }
        for (ClassEntry entry : match.destClasses) {
            matchesByDest.put(entry, match);
        }
    }

    public BiMap<ClassEntry, ClassEntry> getUniqueMatches() {
        return uniqueMatches;
    }

    public Set<ClassEntry> getUnmatchedSourceClasses() {
        return unmatchedSourceClasses;
    }

    public Set<ClassEntry> getUnmatchedDestClasses() {
        return unmatchedDestClasses;
    }

    public Set<ClassEntry> getAmbiguouslyMatchedSourceClasses() {
        return ambiguousMatchesBySource.keySet();
    }

    public ClassMatch getAmbiguousMatchBySource(ClassEntry sourceClass) {
        return ambiguousMatchesBySource.get(sourceClass);
    }

    public ClassMatch getMatchBySource(ClassEntry sourceClass) {
        return matchesBySource.get(sourceClass);
    }

    public ClassMatch getMatchByDest(ClassEntry destClass) {
        return matchesByDest.get(destClass);
    }

    public void removeSource(ClassEntry sourceClass) {
        ClassMatch match = matchesBySource.get(sourceClass);
        if (match != null) {
            remove(match);
            match.sourceClasses.remove(sourceClass);
            if (!match.sourceClasses.isEmpty() || !match.destClasses.isEmpty()) {
                add(match);
            }
        }
    }

    public void removeDest(ClassEntry destClass) {
        ClassMatch match = matchesByDest.get(destClass);
        if (match != null) {
            remove(match);
            match.destClasses.remove(destClass);
            if (!match.sourceClasses.isEmpty() || !match.destClasses.isEmpty()) {
                add(match);
            }
        }
    }
}
