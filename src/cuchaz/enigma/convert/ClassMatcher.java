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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javassist.CtBehavior;
import javassist.CtClass;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.MethodMapping;

public class ClassMatcher {
	
	public static void main(String[] args) throws IOException, MappingParseException {
		// TEMP
		JarFile sourceJar = new JarFile(new File("input/1.8-pre3.jar"));
		JarFile destJar = new JarFile(new File("input/1.8.jar"));
		File inMappingsFile = new File("../Enigma Mappings/1.8-pre3.mappings");
		File outMappingsFile = new File("../Enigma Mappings/1.8.mappings");
		
		// define a matching to use when the automated system cannot find a match
		Map<String,String> fallbackMatching = Maps.newHashMap();
		fallbackMatching.put("none/ayb", "none/ayf");
		fallbackMatching.put("none/ayd", "none/ayd");
		fallbackMatching.put("none/bgk", "unknown/bgk");
		
		// do the conversion
		Mappings mappings = new MappingsReader().read(new FileReader(inMappingsFile));
		convertMappings(sourceJar, destJar, mappings, fallbackMatching);
		
		// write out the converted mappings
		FileWriter writer = new FileWriter(outMappingsFile);
		new MappingsWriter().write(writer, mappings);
		writer.close();
		System.out.println("Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath());
	}
	
	private static void convertMappings(JarFile sourceJar, JarFile destJar, Mappings mappings, Map<String,String> fallbackMatching) {
		// index jars
		System.out.println("Indexing source jar...");
		JarIndex sourceIndex = new JarIndex();
		sourceIndex.indexJar(sourceJar, false);
		System.out.println("Indexing dest jar...");
		JarIndex destIndex = new JarIndex();
		destIndex.indexJar(destJar, false);
		TranslatingTypeLoader sourceLoader = new TranslatingTypeLoader(sourceJar, sourceIndex);
		TranslatingTypeLoader destLoader = new TranslatingTypeLoader(destJar, destIndex);
		
		// compute the matching
		ClassMatching matching = computeMatching(sourceIndex, sourceLoader, destIndex, destLoader);
		Map<String,Map.Entry<ClassIdentity,List<ClassIdentity>>> matchingIndex = matching.getIndex();
		
		// get all the obf class names used in the mappings
		Set<String> usedClassNames = mappings.getAllObfClassNames();
		Set<String> allClassNames = Sets.newHashSet();
		for (ClassEntry classEntry : sourceIndex.getObfClassEntries()) {
			allClassNames.add(classEntry.getName());
		}
		usedClassNames.retainAll(allClassNames);
		System.out.println("Used " + usedClassNames.size() + " classes in the mappings");
		
		// probabilistically match the non-uniquely-matched source classes
		for (Map.Entry<ClassIdentity,List<ClassIdentity>> entry : matchingIndex.values()) {
			ClassIdentity sourceClass = entry.getKey();
			List<ClassIdentity> destClasses = entry.getValue();
			
			// skip classes that are uniquely matched
			if (destClasses.size() == 1) {
				continue;
			}
			
			// skip classes that aren't used in the mappings
			if (!usedClassNames.contains(sourceClass.getClassEntry().getName())) {
				continue;
			}
			
			System.out.println("No exact match for source class " + sourceClass.getClassEntry());
			
			// find the closest classes
			Multimap<Integer,ClassIdentity> scoredMatches = ArrayListMultimap.create();
			for (ClassIdentity c : destClasses) {
				scoredMatches.put(sourceClass.getMatchScore(c), c);
			}
			List<Integer> scores = new ArrayList<Integer>(scoredMatches.keySet());
			Collections.sort(scores, Collections.reverseOrder());
			printScoredMatches(sourceClass.getMaxMatchScore(), scores, scoredMatches);
			
			// does the best match have a non-zero score and the same name?
			int bestScore = scores.get(0);
			Collection<ClassIdentity> bestMatches = scoredMatches.get(bestScore);
			if (bestScore > 0 && bestMatches.size() == 1) {
				ClassIdentity bestMatch = bestMatches.iterator().next();
				if (bestMatch.getClassEntry().equals(sourceClass.getClassEntry())) {
					// use it
					System.out.println("\tAutomatically choosing likely match: " + bestMatch.getClassEntry().getName());
					destClasses.clear();
					destClasses.add(bestMatch);
				}
			}
		}
		
		// group the matching into unique and non-unique matches
		BiMap<String,String> matchedClassNames = HashBiMap.create();
		Set<String> unmatchedSourceClassNames = Sets.newHashSet();
		for (String className : usedClassNames) {
			// is there a match for this class?
			Map.Entry<ClassIdentity,List<ClassIdentity>> entry = matchingIndex.get(className);
			ClassIdentity sourceClass = entry.getKey();
			List<ClassIdentity> matches = entry.getValue();
			
			if (matches.size() == 1) {
				// unique match! We're good to go!
				matchedClassNames.put(sourceClass.getClassEntry().getName(), matches.get(0).getClassEntry().getName());
			} else {
				// no match, check the fallback matching
				String fallbackMatch = fallbackMatching.get(className);
				if (fallbackMatch != null) {
					matchedClassNames.put(sourceClass.getClassEntry().getName(), fallbackMatch);
				} else {
					unmatchedSourceClassNames.add(className);
				}
			}
		}
		
		// report unmatched classes
		if (!unmatchedSourceClassNames.isEmpty()) {
			System.err.println("ERROR: there were unmatched classes!");
			for (String className : unmatchedSourceClassNames) {
				System.err.println("\t" + className);
			}
			return;
		}
		
		// get the class name changes from the matched class names
		Map<String,String> classChanges = Maps.newHashMap();
		for (Map.Entry<String,String> entry : matchedClassNames.entrySet()) {
			if (!entry.getKey().equals(entry.getValue())) {
				classChanges.put(entry.getKey(), entry.getValue());
				System.out.println(String.format("Class change: %s -> %s", entry.getKey(), entry.getValue()));
				/* DEBUG
				System.out.println(String.format("\n%s\n%s",
					new ClassIdentity(sourceLoader.loadClass(entry.getKey()), null, sourceIndex, false, false),
					new ClassIdentity( destLoader.loadClass(entry.getValue()), null, destIndex, false, false)
				));
				*/
			}
		}
		
		// sort the changes so classes are renamed in the correct order
		// ie. if we have the mappings a->b, b->c, we have to apply b->c before a->b
		LinkedHashMap<String,String> orderedClassChanges = Maps.newLinkedHashMap();
		int numChangesLeft = classChanges.size();
		while (!classChanges.isEmpty()) {
			Iterator<Map.Entry<String,String>> iter = classChanges.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String,String> entry = iter.next();
				if (classChanges.get(entry.getValue()) == null) {
					orderedClassChanges.put(entry.getKey(), entry.getValue());
					iter.remove();
				}
			}
			
			// did we remove any changes?
			if (numChangesLeft - classChanges.size() > 0) {
				// keep going
				numChangesLeft = classChanges.size();
			} else {
				// can't sort anymore. There must be a loop
				break;
			}
		}
		if (classChanges.size() > 0) {
			throw new Error(String.format("Unable to sort %d/%d class changes!", classChanges.size(), matchedClassNames.size()));
		}
		
		// convert the mappings in the correct class order
		for (Map.Entry<String,String> entry : orderedClassChanges.entrySet()) {
			mappings.renameObfClass(entry.getKey(), entry.getValue());
		}
		
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
	
	public static ClassMatching computeMatching(JarIndex sourceIndex, TranslatingTypeLoader sourceLoader, JarIndex destIndex, TranslatingTypeLoader destLoader) {
		
		System.out.println("Matching classes...");
		
		ClassMatching matching = null;
		for (boolean useReferences : Arrays.asList(false, true)) {
			int numMatches = 0;
			do {
				SidedClassNamer sourceNamer = null;
				SidedClassNamer destNamer = null;
				if (matching != null) {
					// build a class namer
					ClassNamer namer = new ClassNamer(matching.getUniqueMatches());
					sourceNamer = namer.getSourceNamer();
					destNamer = namer.getDestNamer();
					
					// note the number of matches
					numMatches = matching.getUniqueMatches().size();
				}
				
				// get the entries left to match
				Set<ClassEntry> sourceClassEntries = Sets.newHashSet();
				Set<ClassEntry> destClassEntries = Sets.newHashSet();
				if (matching == null) {
					sourceClassEntries.addAll(sourceIndex.getObfClassEntries());
					destClassEntries.addAll(destIndex.getObfClassEntries());
					matching = new ClassMatching();
				} else {
					for (Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : matching.getAmbiguousMatches().entrySet()) {
						for (ClassIdentity c : entry.getKey()) {
							sourceClassEntries.add(c.getClassEntry());
							matching.removeSource(c);
						}
						for (ClassIdentity c : entry.getValue()) {
							destClassEntries.add(c.getClassEntry());
							matching.removeDest(c);
						}
					}
					for (ClassIdentity c : matching.getUnmatchedSourceClasses()) {
						sourceClassEntries.add(c.getClassEntry());
						matching.removeSource(c);
					}
					for (ClassIdentity c : matching.getUnmatchedDestClasses()) {
						destClassEntries.add(c.getClassEntry());
						matching.removeDest(c);
					}
				}
				
				// compute a matching for the classes
				for (ClassEntry classEntry : sourceClassEntries) {
					CtClass c = sourceLoader.loadClass(classEntry.getName());
					ClassIdentity sourceClass = new ClassIdentity(c, sourceNamer, sourceIndex, useReferences);
					matching.addSource(sourceClass);
				}
				for (ClassEntry classEntry : destClassEntries) {
					CtClass c = destLoader.loadClass(classEntry.getName());
					ClassIdentity destClass = new ClassIdentity(c, destNamer, destIndex, useReferences);
					matching.matchDestClass(destClass);
				}
				
				// TEMP
				System.out.println(matching);
			} while (matching.getUniqueMatches().size() - numMatches > 0);
		}
		
		// check the class matches
		System.out.println("Checking class matches...");
		ClassNamer namer = new ClassNamer(matching.getUniqueMatches());
		SidedClassNamer sourceNamer = namer.getSourceNamer();
		SidedClassNamer destNamer = namer.getDestNamer();
		for (Map.Entry<ClassIdentity,ClassIdentity> entry : matching.getUniqueMatches().entrySet()) {
			
			// check source
			ClassIdentity sourceClass = entry.getKey();
			CtClass sourceC = sourceLoader.loadClass(sourceClass.getClassEntry().getName());
			assert (sourceC != null) : "Unable to load source class " + sourceClass.getClassEntry();
			assert (sourceClass.matches(sourceC)) : "Source " + sourceClass + " doesn't match " + new ClassIdentity(sourceC, sourceNamer, sourceIndex, false);
			
			// check dest
			ClassIdentity destClass = entry.getValue();
			CtClass destC = destLoader.loadClass(destClass.getClassEntry().getName());
			assert (destC != null) : "Unable to load dest class " + destClass.getClassEntry();
			assert (destClass.matches(destC)) : "Dest " + destClass + " doesn't match " + new ClassIdentity(destC, destNamer, destIndex, false);
		}
		
		// warn about the ambiguous matchings
		List<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>> ambiguousMatches = new ArrayList<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>>(matching.getAmbiguousMatches().entrySet());
		Collections.sort(ambiguousMatches, new Comparator<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>>() {
			@Override
			public int compare(Map.Entry<List<ClassIdentity>,List<ClassIdentity>> a, Map.Entry<List<ClassIdentity>,List<ClassIdentity>> b) {
				String aName = a.getKey().get(0).getClassEntry().getName();
				String bName = b.getKey().get(0).getClassEntry().getName();
				return aName.compareTo(bName);
			}
		});
		for (Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : ambiguousMatches) {
			System.out.println("Ambiguous matching:");
			System.out.println("\tSource: " + getClassNames(entry.getKey()));
			System.out.println("\tDest:   " + getClassNames(entry.getValue()));
		}
		
		/* DEBUG
		Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry = ambiguousMatches.get( 7 );
		for (ClassIdentity c : entry.getKey()) {
			System.out.println(c);
		}
		for(ClassIdentity c : entry.getKey()) {
			System.out.println(decompile(sourceLoader, c.getClassEntry())); 
		}
		*/
		
		return matching;
	}
	
	private static void printScoredMatches(int maxScore, List<Integer> scores, Multimap<Integer,ClassIdentity> scoredMatches) {
		int numScoredMatchesShown = 0;
		for (int score : scores) {
			for (ClassIdentity scoredMatch : scoredMatches.get(score)) {
				System.out.println(String.format("\tScore: %3d %3.0f%%   %s", score, 100.0 * score / maxScore, scoredMatch.getClassEntry().getName()));
				if (numScoredMatchesShown++ > 10) {
					return;
				}
			}
		}
	}
	
	private static List<String> getClassNames(Collection<ClassIdentity> classes) {
		List<String> out = Lists.newArrayList();
		for (ClassIdentity c : classes) {
			out.add(c.getClassEntry().getName());
		}
		Collections.sort(out);
		return out;
	}
	
	/* DEBUG
	private static String decompile(TranslatingTypeLoader loader, ClassEntry classEntry) {
		PlainTextOutput output = new PlainTextOutput();
		DecompilerSettings settings = DecompilerSettings.javaDefaults();
		settings.setForceExplicitImports(true);
		settings.setShowSyntheticMembers(true);
		settings.setTypeLoader(loader);
		Decompiler.decompile(classEntry.getName(), output, settings);
		return output.toString();
	}
	*/
}
