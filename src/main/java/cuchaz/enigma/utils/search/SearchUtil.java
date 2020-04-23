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
				float baseScore = 0;
				float scorePerChar = 1f / Arrays.stream(name).mapToInt(String::length).sum();
				String remaining = term;
				for (String component : name) {
					component = component.toUpperCase(Locale.ROOT);
					int l = compareEqualLength(remaining, component);
					remaining = remaining.substring(l);
					baseScore += scorePerChar * l;
				}
				if (!remaining.isEmpty()) {
					baseScore = 0;
				}
				maxScore = Math.max(maxScore, baseScore);
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
