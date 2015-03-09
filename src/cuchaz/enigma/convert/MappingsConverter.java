/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.convert;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.FieldMapping;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MethodMapping;

public class MappingsConverter {
	
	public static Matches computeMatches(JarFile sourceJar, JarFile destJar, Mappings mappings) {
		
		// index jars
		System.out.println("Indexing source jar...");
		JarIndex sourceIndex = new JarIndex();
		sourceIndex.indexJar(sourceJar, false);
		System.out.println("Indexing dest jar...");
		JarIndex destIndex = new JarIndex();
		destIndex.indexJar(destJar, false);
		
		// compute the matching
		ClassMatching matching = computeMatching(sourceJar, sourceIndex, destJar, destIndex, null);
		return new Matches(matching.matches());
	}
	
	public static ClassMatching computeMatching(JarFile sourceJar, JarIndex sourceIndex, JarFile destJar, JarIndex destIndex, BiMap<ClassEntry,ClassEntry> knownMatches) {
		
		System.out.println("Iteratively matching classes");
		
		ClassMatching lastMatching = null;
		int round = 0;
		SidedClassNamer sourceNamer = null;
		SidedClassNamer destNamer = null;
		for (boolean useReferences : Arrays.asList(false, true)) {
			
			int numUniqueMatchesLastTime = 0;
			if (lastMatching != null) {
				numUniqueMatchesLastTime = lastMatching.uniqueMatches().size();
			}
			
			while (true) {
				
				System.out.println("Round " + (++round) + "...");
				
				// init the matching with identity settings
				ClassMatching matching = new ClassMatching(
					new ClassIdentifier(sourceJar, sourceIndex, sourceNamer, useReferences),
					new ClassIdentifier(destJar, destIndex, destNamer, useReferences)
				);
				
				if (knownMatches != null) {
					matching.addKnownMatches(knownMatches);
				}
				
				if (lastMatching == null) {
					// search all classes
					matching.match(sourceIndex.getObfClassEntries(), destIndex.getObfClassEntries());
				} else {
					// we already know about these matches from last time
					matching.addKnownMatches(lastMatching.uniqueMatches());
					
					// search unmatched and ambiguously-matched classes
					matching.match(lastMatching.unmatchedSourceClasses(), lastMatching.unmatchedDestClasses());
					for (ClassMatch match : lastMatching.ambiguousMatches()) {
						matching.match(match.sourceClasses, match.destClasses);
					}
				}
				System.out.println(matching);
				BiMap<ClassEntry,ClassEntry> uniqueMatches = matching.uniqueMatches();
				
				// did we match anything new this time?
				if (uniqueMatches.size() > numUniqueMatchesLastTime) {
					numUniqueMatchesLastTime = uniqueMatches.size();
					lastMatching = matching;
				} else {
					break;
				}
				
				// update the namers
				ClassNamer namer = new ClassNamer(uniqueMatches);
				sourceNamer = namer.getSourceNamer();
				destNamer = namer.getDestNamer();
			}
		}
		
		return lastMatching;
	}
	
	public static Mappings newMappings(Matches matches, Mappings oldMappings, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		// sort the unique matches by size of inner class chain
		Multimap<Integer,Entry<ClassEntry,ClassEntry>> matchesByDestChainSize = HashMultimap.create();
		for (Entry<ClassEntry,ClassEntry> match : matches.getUniqueMatches().entrySet()) {
			int chainSize = destDeobfuscator.getJarIndex().getObfClassChain(match.getValue()).size();
			matchesByDestChainSize.put(chainSize, match);
		}
		
		// build the mappings (in order of small-to-large inner chains)
		Mappings newMappings = new Mappings();
		List<Integer> chainSizes = Lists.newArrayList(matchesByDestChainSize.keySet());
		Collections.sort(chainSizes);
		for (int chainSize : chainSizes) {
			for (Entry<ClassEntry,ClassEntry> match : matchesByDestChainSize.get(chainSize)) {
				
				// get class info
				ClassEntry obfSourceClassEntry = match.getKey();
				ClassEntry obfDestClassEntry = match.getValue();
				List<ClassEntry> destClassChain = destDeobfuscator.getJarIndex().getObfClassChain(obfDestClassEntry);
				
				ClassMapping sourceMapping = sourceDeobfuscator.getMappings().getClassByObf(obfSourceClassEntry);
				if (sourceMapping == null) {
					// if this class was never deobfuscated, don't try to match it
					continue;
				}
				
				// find out where to make the dest class mapping
				if (destClassChain.size() == 1) {
					// not an inner class, add directly to mappings
					newMappings.addClassMapping(migrateClassMapping(obfDestClassEntry, sourceMapping, matches, false));
				} else {
					// inner class, find the outer class mapping
					ClassMapping destMapping = null;
					for (int i=0; i<destClassChain.size()-1; i++) {
						ClassEntry destChainClassEntry = destClassChain.get(i);
						if (destMapping == null) {
							destMapping = newMappings.getClassByObf(destChainClassEntry);
							if (destMapping == null) {
								destMapping = new ClassMapping(destChainClassEntry.getName());
								newMappings.addClassMapping(destMapping);
							}
						} else {
							destMapping = destMapping.getInnerClassByObf(destChainClassEntry.getInnerClassName());
							if (destMapping == null) {
								destMapping = new ClassMapping(destChainClassEntry.getName());
								destMapping.addInnerClassMapping(destMapping);
							}
						}
					}
					destMapping.addInnerClassMapping(migrateClassMapping(obfDestClassEntry, sourceMapping, matches, true));
				}
			}
		}
		return newMappings;
	}
	
	private static ClassMapping migrateClassMapping(ClassEntry newObfClass, ClassMapping mapping, final Matches matches, boolean useSimpleName) {
		
		ClassNameReplacer replacer = new ClassNameReplacer() {
			@Override
			public String replace(String className) {
				ClassEntry newClassEntry = matches.getUniqueMatches().get(new ClassEntry(className));
				if (newClassEntry != null) {
					return newClassEntry.getName();
				}
				return null;
			}
		};
		
		ClassMapping newMapping;
		String deobfName = mapping.getDeobfName();
		if (deobfName != null) {
			if (useSimpleName) {
				deobfName = new ClassEntry(deobfName).getSimpleName();
			}
			newMapping = new ClassMapping(newObfClass.getName(), deobfName);
		} else {
			newMapping = new ClassMapping(newObfClass.getName());
		}
		
		// copy fields
		for (FieldMapping fieldMapping : mapping.fields()) {
			// TODO: map field obf names too...
			newMapping.addFieldMapping(new FieldMapping(fieldMapping, replacer));
		}
		
		// copy methods
		for (MethodMapping methodMapping : mapping.methods()) {
			// TODO: map method obf names too...
			newMapping.addMethodMapping(new MethodMapping(methodMapping, replacer));
		}
		
		return newMapping;
	}

	public static void convertMappings(Mappings mappings, BiMap<ClassEntry,ClassEntry> changes) {
		
		// sort the changes so classes are renamed in the correct order
		// ie. if we have the mappings a->b, b->c, we have to apply b->c before a->b
		LinkedHashMap<ClassEntry,ClassEntry> sortedChanges = Maps.newLinkedHashMap();
		int numChangesLeft = changes.size();
		while (!changes.isEmpty()) {
			Iterator<Map.Entry<ClassEntry,ClassEntry>> iter = changes.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ClassEntry,ClassEntry> change = iter.next();
				if (changes.containsKey(change.getValue())) {
					sortedChanges.put(change.getKey(), change.getValue());
					iter.remove();
				}
			}
			
			// did we remove any changes?
			if (numChangesLeft - changes.size() > 0) {
				// keep going
				numChangesLeft = changes.size();
			} else {
				// can't sort anymore. There must be a loop
				break;
			}
		}
		if (!changes.isEmpty()) {
			throw new Error("Unable to sort class changes! There must be a cycle.");
		}
		
		// convert the mappings in the correct class order
		for (Map.Entry<ClassEntry,ClassEntry> entry : sortedChanges.entrySet()) {
			mappings.renameObfClass(entry.getKey().getName(), entry.getValue().getName());
		}
	}
	
	/* TODO: after we get a mapping, check to see that the other entries match
	public static void checkMethods() {
		
		// check the method matches
		System.out.println("Checking methods...");
		for (ClassMapping classMapping : mappings.classes()) {
			ClassEntry classEntry = new ClassEntry(classMapping.getObfFullName());
			for (MethodMapping methodMapping : classMapping.methods()) {
				
				// skip constructors
				if (methodMapping.getObfName().equals("<init>")) {
					continue;
				}
				
				MethodEntry methodEntry = new MethodEntry(
					classEntry,
					methodMapping.getObfName(),
					methodMapping.getObfSignature()
				);
				if (!destIndex.containsObfBehavior(methodEntry)) {
					System.err.println("WARNING: method doesn't match: " + methodEntry);
					
					// TODO: show methods if needed
					// show the available methods
					System.err.println("\tAvailable dest methods:");
					CtClass c = destLoader.loadClass(classMapping.getObfFullName());
					for (CtBehavior behavior : c.getDeclaredBehaviors()) {
						System.err.println("\t\t" + EntryFactory.getBehaviorEntry(behavior));
					}
					
					System.err.println("\tAvailable source methods:");
					c = sourceLoader.loadClass(matchedClassNames.inverse().get(classMapping.getObfFullName()));
					for (CtBehavior behavior : c.getDeclaredBehaviors()) {
						System.err.println("\t\t" + EntryFactory.getBehaviorEntry(behavior));
					}
				}
			}
		}
		
		System.out.println("Done!");
	}
	*/
}
