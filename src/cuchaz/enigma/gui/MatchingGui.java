package cuchaz.enigma.gui;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.Matches;


public class MatchingGui {

	public MatchingGui(Matches matches, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		// TODO Auto-generated constructor stub
	}
	

	/* TODO: see if we can use any of this here
	public static doTheThings() {
		
		// get all the obf class names used in the mappings
		Set<ClassEntry> usedClasses = Sets.newHashSet();
		for (String className : mappings.getAllObfClassNames()) {
			usedClasses.add(new ClassEntry(className));
		}
		System.out.println(String.format("Mappings reference %d/%d classes",
			usedClasses.size(), sourceIndex.getObfClassEntries().size()
		));
		
		// get the used matches
		Collection<ClassMatch> matches = matching.matches();
		Matches usedMatches = new Matches();
		for (ClassMatch match : matching.matches()) {
			if (!match.intersectSourceClasses(usedClasses).isEmpty()) {
				usedMatches.add(match);
			}
		}
		System.out.println(String.format("Mappings reference %d/%d match groups",
			usedMatches.size(), matches.size()
		));
		
		// see what the used classes map to
		BiMap<ClassEntry,ClassEntry> uniqueUsedMatches = HashBiMap.create();
		Map<ClassEntry,ClassMatch> ambiguousUsedMatches = Maps.newHashMap();
		Set<ClassEntry> unmatchedUsedClasses = Sets.newHashSet();
		for (ClassMatch match : matching.matches()) {
			Set<ClassEntry> matchUsedClasses = match.intersectSourceClasses(usedClasses);
			if (matchUsedClasses.isEmpty()) {
				continue;
			}
			
			usedMatches.add(match);

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
		}
		
		// bail if there were unmatched classes
		if (!unmatchedUsedClasses.isEmpty()) {
			throw new Error("There were " + unmatchedUsedClasses.size() + " unmatched classes!");
		}
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
	*/
}
