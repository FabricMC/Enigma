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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.MethodMapping;

public class ClassMatcher {
	
	public static void main(String[] args)
	throws IOException, MappingParseException {
		
		// setup files
		File home = new File(System.getProperty("user.home"));
		JarFile sourceJar = new JarFile(new File(home, ".minecraft/versions/1.8/1.8.jar"));
		JarFile destJar = new JarFile(new File(home, ".minecraft/versions/1.8.3/1.8.3.jar"));
		File inMappingsFile = new File("../Enigma Mappings/1.8.mappings");
		File outMappingsFile = new File("../Enigma Mappings/1.8.3.mappings");
		
		// define a matching to use when the automated system cannot find a match
		Map<String,String> fallbackMatching = Maps.newHashMap();
		/*
		fallbackMatching.put("none/ayb", "none/ayf");
		fallbackMatching.put("none/ayd", "none/ayd");
		fallbackMatching.put("none/bgk", "unknown/bgk");
		*/
		
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
		
		// compute the matching
		ClassMatching matching = computeMatching(sourceJar, sourceIndex, destJar, destIndex);
		
		// get all the obf class names used in the mappings
		Set<ClassEntry> usedClasses = Sets.newHashSet();
		for (String className : mappings.getAllObfClassNames()) {
			usedClasses.add(new ClassEntry(className));
		}
		System.out.println("Mappings reference " + usedClasses.size() + " classes");
		
		// see what the used classes map to
		BiMap<ClassEntry,ClassEntry> uniqueUsedMatches = HashBiMap.create();
		Map<ClassEntry,ClassMatch> ambiguousUsedMatches = Maps.newHashMap();
		Set<ClassEntry> unmatchedUsedClasses = Sets.newHashSet();
		for (ClassMatch match : matching.matches()) {
			Set<ClassEntry> matchUsedClasses = match.intersectSourceClasses(usedClasses);
			if (matchUsedClasses.isEmpty()) {
				continue;
			}

			// classify the match
			if (!match.isMatched()) {
				// unmatched
				unmatchedUsedClasses.addAll(matchUsedClasses);
			} else {
				if (match.isAmbiguous()) {
					// ambiguously matched
					for (ClassEntry matchUsedClass : matchUsedClasses) {
						ambiguousUsedMatches.put(matchUsedClass, match);
					}
				} else {
					// uniquely matched
					uniqueUsedMatches.put(match.getUniqueSource(), match.getUniqueDest());
				}
			}
		}
		
		// get unmatched dest classes
		Set<ClassEntry> unmatchedDestClasses = Sets.newHashSet();
		for (ClassMatch match : matching.matches()) {
			if (!match.isMatched()) {
				unmatchedDestClasses.addAll(match.destClasses);
			}
		}
		
		// warn about the ambiguous used matches
		if (ambiguousUsedMatches.size() > 0) {
			System.out.println(String.format("%d source classes have ambiguous mappings", ambiguousUsedMatches.size()));
			List<ClassMatch> ambiguousMatchesList = Lists.newArrayList(Sets.newHashSet(ambiguousUsedMatches.values()));
			Collections.sort(ambiguousMatchesList, new Comparator<ClassMatch>() {
				@Override
				public int compare(ClassMatch a, ClassMatch b) {
					String aName = a.sourceClasses.iterator().next().getName();
					String bName = b.sourceClasses.iterator().next().getName();
					return aName.compareTo(bName);
				}
			});
			for (ClassMatch match : ambiguousMatchesList) {
				System.out.println("Ambiguous matching:");
				System.out.println("\tSource: " + getClassNames(match.sourceClasses));
				System.out.println("\tDest:   " + getClassNames(match.destClasses));
			}
		}
		
		// warn about unmatched used classes
		for (ClassEntry unmatchedUsedClass : unmatchedUsedClasses) {
			System.out.println("No exact match for source class " + unmatchedUsedClass.getClassEntry());
			
			// rank all the unmatched dest classes against the used class
			ClassIdentity sourceIdentity = matching.getSourceIdentifier().identify(unmatchedUsedClass);
			Multimap<Integer,ClassEntry> scoredDestClasses = ArrayListMultimap.create();
			for (ClassEntry unmatchedDestClass : unmatchedDestClasses) {
				ClassIdentity destIdentity = matching.getDestIdentifier().identify(unmatchedDestClass);	
				scoredDestClasses.put(sourceIdentity.getMatchScore(destIdentity), unmatchedDestClass);
			}
			
			List<Integer> scores = new ArrayList<Integer>(scoredDestClasses.keySet());
			Collections.sort(scores, Collections.reverseOrder());
			printScoredMatches(sourceIdentity.getMaxMatchScore(), scores, scoredDestClasses);
			
			/* TODO: re-enable auto-pick logic
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
			*/
		}
		
		// bail if there were unmatched classes
		if (!unmatchedUsedClasses.isEmpty()) {
			throw new Error("There were " + unmatchedUsedClasses.size() + " unmatched classes!");
		}
		
		// sort the changes so classes are renamed in the correct order
		// ie. if we have the mappings a->b, b->c, we have to apply b->c before a->b
		BiMap<ClassEntry,ClassEntry> unsortedChanges = HashBiMap.create(uniqueUsedMatches);
		LinkedHashMap<ClassEntry,ClassEntry> sortedChanges = Maps.newLinkedHashMap();
		int numChangesLeft = unsortedChanges.size();
		while (!unsortedChanges.isEmpty()) {
			Iterator<Map.Entry<ClassEntry,ClassEntry>> iter = unsortedChanges.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ClassEntry,ClassEntry> change = iter.next();
				if (unsortedChanges.containsKey(change.getValue())) {
					sortedChanges.put(change.getKey(), change.getValue());
					iter.remove();
				}
			}
			
			// did we remove any changes?
			if (numChangesLeft - unsortedChanges.size() > 0) {
				// keep going
				numChangesLeft = unsortedChanges.size();
			} else {
				// can't sort anymore. There must be a loop
				break;
			}
		}
		if (!unsortedChanges.isEmpty()) {
			throw new Error(String.format("Unable to sort %d/%d class changes!", unsortedChanges.size(), uniqueUsedMatches.size()));
		}
		
		// convert the mappings in the correct class order
		for (Map.Entry<ClassEntry,ClassEntry> entry : sortedChanges.entrySet()) {
			mappings.renameObfClass(entry.getKey().getName(), entry.getValue().getName());
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
					
					/* TODO: show methods if needed
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
					*/
				}
			}
		}
		
		System.out.println("Done!");
	}
	
	public static ClassMatching computeMatching(JarFile sourceJar, JarIndex sourceIndex, JarFile destJar, JarIndex destIndex) {
		
		System.out.println("Iteratively matching classes...");
		
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
				
				System.out.println("Round " + (++round) + " ...");
				
				// init the matching with identity settings
				ClassMatching matching = new ClassMatching(
					new ClassIdentifier(sourceJar, sourceIndex, sourceNamer, useReferences),
					new ClassIdentifier(destJar, destIndex, destNamer, useReferences)
				);
				
				if (lastMatching == null) {
					// search all classes
					matching.match(sourceIndex.getObfClassEntries(), destIndex.getObfClassEntries());
				} else {
					// we already know about these matches
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
	
	private static void printScoredMatches(int maxScore, List<Integer> scores, Multimap<Integer,ClassEntry> scoredMatches) {
		int numScoredMatchesShown = 0;
		for (int score : scores) {
			for (ClassEntry classEntry : scoredMatches.get(score)) {
				System.out.println(String.format("\tScore: %3d %3.0f%%   %s",
					score, 100.0 * score / maxScore, classEntry.getName()
				));
				if (numScoredMatchesShown++ > 10) {
					return;
				}
			}
		}
	}
	
	private static List<String> getClassNames(Collection<ClassEntry> classes) {
		List<String> out = Lists.newArrayList();
		for (ClassEntry c : classes) {
			out.add(c.getName());
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
