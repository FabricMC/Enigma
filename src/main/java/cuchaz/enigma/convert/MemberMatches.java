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
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;

import java.util.Collection;
import java.util.Set;


public class MemberMatches<T extends Entry> {

    private BiMap<T, T>             matches;
    private Multimap<ClassEntry, T> matchedSourceEntries;
    private Multimap<ClassEntry, T> unmatchedSourceEntries;
    private Multimap<ClassEntry, T> unmatchedDestEntries;
    private Multimap<ClassEntry, T> unmatchableSourceEntries;

    public MemberMatches() {
        matches = HashBiMap.create();
        matchedSourceEntries = HashMultimap.create();
        unmatchedSourceEntries = HashMultimap.create();
        unmatchedDestEntries = HashMultimap.create();
        unmatchableSourceEntries = HashMultimap.create();
    }

    public void addMatch(T srcEntry, T destEntry) {
        boolean wasAdded = matches.put(srcEntry, destEntry) == null;
        assert (wasAdded);
        wasAdded = matchedSourceEntries.put(srcEntry.getClassEntry(), srcEntry);
        assert (wasAdded);
    }

    public void addUnmatchedSourceEntry(T sourceEntry) {
        boolean wasAdded = unmatchedSourceEntries.put(sourceEntry.getClassEntry(), sourceEntry);
        assert (wasAdded);
    }

    public void addUnmatchedSourceEntries(Iterable<T> sourceEntries) {
        for (T sourceEntry : sourceEntries) {
            addUnmatchedSourceEntry(sourceEntry);
        }
    }

    public void addUnmatchedDestEntry(T destEntry) {
        if (destEntry.getName().equals("<clinit>") || destEntry.getName().equals("<init>"))
            return;
        boolean wasAdded = unmatchedDestEntries.put(destEntry.getClassEntry(), destEntry);
        assert (wasAdded);
    }

    public void addUnmatchedDestEntries(Iterable<T> destEntriesntries) {
        for (T entry : destEntriesntries) {
            addUnmatchedDestEntry(entry);
        }
    }

    public void addUnmatchableSourceEntry(T sourceEntry) {
        boolean wasAdded = unmatchableSourceEntries.put(sourceEntry.getClassEntry(), sourceEntry);
        assert (wasAdded);
    }

    public Set<ClassEntry> getSourceClassesWithUnmatchedEntries() {
        return unmatchedSourceEntries.keySet();
    }

    public Collection<ClassEntry> getSourceClassesWithoutUnmatchedEntries() {
        Set<ClassEntry> out = Sets.newHashSet();
        out.addAll(matchedSourceEntries.keySet());
        out.removeAll(unmatchedSourceEntries.keySet());
        return out;
    }

    public Collection<T> getUnmatchedSourceEntries() {
        return unmatchedSourceEntries.values();
    }

    public Collection<T> getUnmatchedSourceEntries(ClassEntry sourceClass) {
        return unmatchedSourceEntries.get(sourceClass);
    }

    public Collection<T> getUnmatchedDestEntries() {
        return unmatchedDestEntries.values();
    }

    public Collection<T> getUnmatchedDestEntries(ClassEntry destClass) {
        return unmatchedDestEntries.get(destClass);
    }

    public Collection<T> getUnmatchableSourceEntries() {
        return unmatchableSourceEntries.values();
    }

    public boolean hasSource(T sourceEntry) {
        return matches.containsKey(sourceEntry) || unmatchedSourceEntries.containsValue(sourceEntry);
    }

    public boolean hasDest(T destEntry) {
        return matches.containsValue(destEntry) || unmatchedDestEntries.containsValue(destEntry);
    }

    public BiMap<T, T> matches() {
        return matches;
    }

    public boolean isMatchedSourceEntry(T sourceEntry) {
        return matches.containsKey(sourceEntry);
    }

    public boolean isMatchedDestEntry(T destEntry) {
        return matches.containsValue(destEntry);
    }

    public boolean isUnmatchableSourceEntry(T sourceEntry) {
        return unmatchableSourceEntries.containsEntry(sourceEntry.getClassEntry(), sourceEntry);
    }
    public void makeMatch(T sourceEntry, T destEntry) {
        makeMatch(sourceEntry, destEntry, null, null);
    }

    public void makeMatch(T sourceEntry, T destEntry, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
        if (sourceDeobfuscator != null && destDeobfuscator != null)
        {
            makeMatch(sourceEntry, destEntry);
            sourceEntry = (T) sourceEntry.cloneToNewClass(sourceDeobfuscator.getJarIndex().getTranslationIndex().resolveEntryClass(sourceEntry, true));
            destEntry = (T) destEntry.cloneToNewClass(destDeobfuscator.getJarIndex().getTranslationIndex().resolveEntryClass(destEntry, true));
        }
        boolean wasRemoved = unmatchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
        assert (wasRemoved);
        wasRemoved = unmatchedDestEntries.remove(destEntry.getClassEntry(), destEntry);
        assert (wasRemoved);
        addMatch(sourceEntry, destEntry);
    }

    public boolean isMatched(T sourceEntry, T destEntry) {
        T match = matches.get(sourceEntry);
        return match != null && match.equals(destEntry);
    }

    public void unmakeMatch(T sourceEntry, T destEntry, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator)
    {
        if (sourceDeobfuscator != null && destDeobfuscator != null)
        {
            unmakeMatch(sourceEntry, destEntry, null, null);
            sourceEntry = (T) sourceEntry.cloneToNewClass(
                    sourceDeobfuscator.getJarIndex().getTranslationIndex().resolveEntryClass(sourceEntry, true));
            destEntry = (T) destEntry.cloneToNewClass(
                    destDeobfuscator.getJarIndex().getTranslationIndex().resolveEntryClass(destEntry, true));
        }

        boolean wasRemoved = matches.remove(sourceEntry) != null;
        assert (wasRemoved);
        wasRemoved = matchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
        assert (wasRemoved);
        addUnmatchedSourceEntry(sourceEntry);
        addUnmatchedDestEntry(destEntry);
    }

    public void makeSourceUnmatchable(T sourceEntry, Deobfuscator sourceDeobfuscator) {
        if (sourceDeobfuscator != null)
        {
            makeSourceUnmatchable(sourceEntry, null);
            sourceEntry = (T) sourceEntry.cloneToNewClass(
                    sourceDeobfuscator.getJarIndex().getTranslationIndex().resolveEntryClass(sourceEntry, true));
        }
        assert (!isMatchedSourceEntry(sourceEntry));
        boolean wasRemoved = unmatchedSourceEntries.remove(sourceEntry.getClassEntry(), sourceEntry);
        assert (wasRemoved);
        addUnmatchableSourceEntry(sourceEntry);
    }
}
