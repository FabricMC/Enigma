package cuchaz.enigma.utils.search;

import java.util.*;
import java.util.stream.Collectors;

import cuchaz.enigma.utils.Pair;

public class SearchUtil<T extends SearchEntry> {

	private final Map<T, Entry<T>> entries = new HashMap<>();

	public void add(T entry) {
		Entry<T> e = Entry.from(entry);
		entries.put(entry, e);
	}

	public void addAll(Collection<T> entries) {
		this.entries.putAll(entries.parallelStream().collect(Collectors.toMap(e -> e, Entry::from)));
	}

	public void remove(T entry) {
		entries.remove(entry);
	}

	public void clear() {
		entries.clear();
	}

	public List<T> search(String term, int limit) {
		return entries.values().parallelStream()
				.map(e -> new Pair<>(e, e.getScore(term)))
				.filter(e -> e.b > 0)
				.sorted(Comparator.comparingDouble(o -> -o.b))
				.map(e -> e.a.searchEntry)
				.limit(limit)
				.collect(Collectors.toList());
	}

	public void hit(T entry) {
		Entry<T> e = entries.get(entry);
		if (e != null) e.hit();
	}

	private static final class Entry<T extends SearchEntry> {

		public final T searchEntry;
		public final String[][] components;
		private int hits = 0;

		private Entry(T searchEntry, String[][] components) {
			this.searchEntry = searchEntry;
			this.components = components;
		}

		public void hit() {
			hits += 1;
		}

		public float getScore(String term) {
			term = term.toUpperCase(Locale.ROOT);
			float maxScore = 0;
			for (String[] name : components) {
				float scorePerChar = 1f / Arrays.stream(name).mapToInt(String::length).sum();
				Map<String, Float> snapshots = new HashMap<>();
				snapshots.put(term, 0f);
				for (int componentIndex = 0; componentIndex < name.length; componentIndex++) {
					String component = name[componentIndex];
					float posMultiplier = (name.length - componentIndex) * 0.3f;
					Map<String, Float> newSnapshots = new HashMap<>();
					for (Map.Entry<String, Float> snapshot : snapshots.entrySet()) {
						String remaining = snapshot.getKey();
						float score = snapshot.getValue();
						component = component.toUpperCase(Locale.ROOT);
						int l = compareEqualLength(remaining, component);
						for (int i = 1; i <= l; i++) {
							float baseScore = scorePerChar * i;
							float chainBonus = (i - 1) * 0.5f;
							newSnapshots.put(remaining.substring(i), score + baseScore * posMultiplier + chainBonus);
						}
					}
					newSnapshots.forEach((k, v) -> snapshots.compute(k, (_k, v1) -> v1 == null ? v : Math.max(v, v1)));
				}
				maxScore = Math.max(maxScore, snapshots.getOrDefault("", 0f));
			}
			return maxScore * (hits + 1);
		}

		public static <T extends SearchEntry> Entry<T> from(T e) {
			List<String> searchableNames = e.getSearchableNames();
			String[][] components = new String[searchableNames.size()][];
			for (int i = 0; i < searchableNames.size(); i++) {
				String name = searchableNames.get(i);
				components[i] = wordwiseSplit(name).toArray(new String[0]);
			}
			return new Entry<>(e, components);
		}

	}

	private static int compareEqualLength(String s1, String s2) {
		int len = 0;
		while (len < s1.length() && len < s2.length() && s1.charAt(len) == s2.charAt(len)) {
			len += 1;
		}
		return len;
	}

	private static List<String> wordwiseSplit(String name) {
		List<String> list = new ArrayList<>();
		while (!name.isEmpty()) {
			int take;
			if (Character.isLetter(name.charAt(0))) {
				if (name.length() == 1) {
					take = 1;
				} else {
					boolean nextSegmentIsUppercase = Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1));
					if (nextSegmentIsUppercase) {
						int nextLowercase = 1;
						while (Character.isUpperCase(name.charAt(nextLowercase))) {
							nextLowercase += 1;
							if (nextLowercase == name.length()) {
								nextLowercase += 1;
								break;
							}
						}
						take = nextLowercase - 1;
					} else {
						int nextUppercase = 1;
						while (nextUppercase < name.length() && Character.isLowerCase(name.charAt(nextUppercase))) {
							nextUppercase += 1;
						}
						take = nextUppercase;
					}
				}
			} else if (Character.isDigit(name.charAt(0))) {
				int nextNonNum = 1;
				while (nextNonNum < name.length() && Character.isLetter(name.charAt(nextNonNum)) && !Character.isLowerCase(name.charAt(nextNonNum))) {
					nextNonNum += 1;
				}
				take = nextNonNum;
			} else {
				take = 1;
			}
			list.add(name.substring(0, take));
			name = name.substring(take);
		}
		return list;
	}

}
