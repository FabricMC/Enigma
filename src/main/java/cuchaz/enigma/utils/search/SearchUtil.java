package cuchaz.enigma.utils.search;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cuchaz.enigma.utils.Pair;

public class SearchUtil<T extends SearchEntry> {

	private final Map<T, Entry<T>> entries = new HashMap<>();
	private final Map<String, Integer> hitCount = new HashMap<>();
	private final Executor searchExecutor = Executors.newWorkStealingPool();

	public void add(T entry) {
		Entry<T> e = Entry.from(entry);
		entries.put(entry, e);
	}

	public void add(Entry<T> entry) {
		entries.put(entry.searchEntry, entry);
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

	public void clearHits() {
		hitCount.clear();
	}

	public Stream<T> search(String term) {
		return entries.values().parallelStream()
				.map(e -> new Pair<>(e, e.getScore(term, hitCount.getOrDefault(e.searchEntry.getIdentifier(), 0))))
				.filter(e -> e.b > 0)
				.sorted(Comparator.comparingDouble(o -> -o.b))
				.map(e -> e.a.searchEntry)
				.sequential();
	}

	public SearchControl asyncSearch(String term, SearchResultConsumer<T> consumer) {
		Map<String, Integer> hitCount = new HashMap<>(this.hitCount);
		Map<T, Entry<T>> entries = new HashMap<>(this.entries);
		float[] scores = new float[entries.size()];
		Lock scoresLock = new ReentrantLock();
		AtomicInteger size = new AtomicInteger();
		AtomicBoolean control = new AtomicBoolean(false);
		AtomicInteger elapsed = new AtomicInteger();
		for (Entry<T> value : entries.values()) {
			searchExecutor.execute(() -> {
				try {
					if (control.get()) return;
					float score = value.getScore(term, hitCount.getOrDefault(value.searchEntry.getIdentifier(), 0));
					if (score <= 0) return;
					score = -score; // sort descending
					try {
						scoresLock.lock();
						if (control.get()) return;
						int dataSize = size.getAndIncrement();
						int index = Arrays.binarySearch(scores, 0, dataSize, score);
						if (index < 0) {
							index = ~index;
						}
						System.arraycopy(scores, index, scores, index + 1, dataSize - index);
						scores[index] = score;
						consumer.add(index, value.searchEntry);
					} finally {
						scoresLock.unlock();
					}
				} finally {
					elapsed.incrementAndGet();
				}
			});
		}

		return new SearchControl() {
			@Override
			public void stop() {
				control.set(true);
			}

			@Override
			public boolean isFinished() {
				return entries.size() == elapsed.get();
			}

			@Override
			public float getProgress() {
				return (float) elapsed.get() / entries.size();
			}
		};
	}

	public void hit(T entry) {
		if (entries.containsKey(entry)) {
			hitCount.compute(entry.getIdentifier(), (_id, i) -> i == null ? 1 : i + 1);
		}
	}

	public static final class Entry<T extends SearchEntry> {

		public final T searchEntry;
		private final String[][] components;

		private Entry(T searchEntry, String[][] components) {
			this.searchEntry = searchEntry;
			this.components = components;
		}

		public float getScore(String term, int hits) {
			String ucTerm = term.toUpperCase(Locale.ROOT);
			float maxScore = (float) Arrays.stream(components)
					.mapToDouble(name -> getScoreFor(ucTerm, name))
					.max().orElse(0.0);
			return maxScore * (hits + 1);
		}

		/**
		 * Computes the score for the given <code>name</code> against the given search term.
		 *
		 * @param term the search term (expected to be upper-case)
		 * @param name the entry name, split at word boundaries (see {@link Entry#wordwiseSplit(String)})
		 * @return the computed score for the entry
		 */
		private static float getScoreFor(String term, String[] name) {
			int totalLength = Arrays.stream(name).mapToInt(String::length).sum();
			float scorePerChar = 1f / totalLength;

			// This map contains a snapshot of all the states the search has
			// been in. The keys are the remaining characters of the search
			// term, the values are the maximum scores for that remaining
			// search term part.
			Map<String, Float> snapshots = new HashMap<>();
			snapshots.put(term, 0f);

			// For each component, start at each existing snapshot, searching
			// for the next longest match, and calculate the new score for each
			// match length until the maximum. Then the new scores are put back
			// into the snapshot map.
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
						merge(newSnapshots, Collections.singletonMap(remaining.substring(i), score + baseScore * posMultiplier + chainBonus), Math::max);
					}
				}
				merge(snapshots, newSnapshots, Math::max);
			}

			// Only return the score for when the search term was completely
			// consumed.
			return snapshots.getOrDefault("", 0f);
		}

		private static <K, V> void merge(Map<K, V> self, Map<K, V> source, BiFunction<V, V, V> combiner) {
			source.forEach((k, v) -> self.compute(k, (_k, v1) -> v1 == null ? v : v == null ? v1 : combiner.apply(v, v1)));
		}

		public static <T extends SearchEntry> Entry<T> from(T e) {
			String[][] components = e.getSearchableNames().parallelStream()
					.map(Entry::wordwiseSplit)
					.toArray(String[][]::new);
			return new Entry<>(e, components);
		}

		private static int compareEqualLength(String s1, String s2) {
			int len = 0;
			while (len < s1.length() && len < s2.length() && s1.charAt(len) == s2.charAt(len)) {
				len += 1;
			}
			return len;
		}

		/**
		 * Splits the given input into components, trying to detect word parts.
		 * <p>
		 * Example of how words get split (using <code>|</code> as seperator):
		 * <p><code>MinecraftClientGame -> Minecraft|Client|Game</code></p>
		 * <p><code>HTTPInputStream -> HTTP|Input|Stream</code></p>
		 * <p><code>class_932 -> class|_|932</code></p>
		 * <p><code>X11FontManager -> X|11|Font|Manager</code></p>
		 * <p><code>openHTTPConnection -> open|HTTP|Connection</code></p>
		 * <p><code>open_http_connection -> open|_|http|_|connection</code></p>
		 *
		 * @param input the input to split
		 * @return the resulting components
		 */
		private static String[] wordwiseSplit(String input) {
			List<String> list = new ArrayList<>();
			while (!input.isEmpty()) {
				int take;
				if (Character.isLetter(input.charAt(0))) {
					if (input.length() == 1) {
						take = 1;
					} else {
						boolean nextSegmentIsUppercase = Character.isUpperCase(input.charAt(0)) && Character.isUpperCase(input.charAt(1));
						if (nextSegmentIsUppercase) {
							int nextLowercase = 1;
							while (Character.isUpperCase(input.charAt(nextLowercase))) {
								nextLowercase += 1;
								if (nextLowercase == input.length()) {
									nextLowercase += 1;
									break;
								}
							}
							take = nextLowercase - 1;
						} else {
							int nextUppercase = 1;
							while (nextUppercase < input.length() && Character.isLowerCase(input.charAt(nextUppercase))) {
								nextUppercase += 1;
							}
							take = nextUppercase;
						}
					}
				} else if (Character.isDigit(input.charAt(0))) {
					int nextNonNum = 1;
					while (nextNonNum < input.length() && Character.isLetter(input.charAt(nextNonNum)) && !Character.isLowerCase(input.charAt(nextNonNum))) {
						nextNonNum += 1;
					}
					take = nextNonNum;
				} else {
					take = 1;
				}
				list.add(input.substring(0, take));
				input = input.substring(take);
			}
			return list.toArray(new String[0]);
		}

	}

	@FunctionalInterface
	public interface SearchResultConsumer<T extends SearchEntry> {
		void add(int index, T entry);
	}

	public interface SearchControl {
		void stop();

		boolean isFinished();

		float getProgress();
	}

}
