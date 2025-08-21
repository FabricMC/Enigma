package cuchaz.enigma.classprovider;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

/**
 * Wraps a ClassProvider to provide caching and synchronization.
 */
public class CachingClassProvider implements ClassProvider {
	private static final long EXPIRE_AFTER = 1 * 60 * 1000; // one minute
	private static final long MAX_SIZE = 128;
	private final ClassProvider classProvider;
	private long lastPruneTime = 0;
	private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

	public CachingClassProvider(ClassProvider classProvider) {
		this.classProvider = classProvider;
	}

	@Override
	public Collection<String> getClassNames() {
		return classProvider.getClassNames();
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		final long time = System.currentTimeMillis();
		boolean prune = false;

		if (lastPruneTime + 2 * EXPIRE_AFTER < time) {
			synchronized (cache) {
				if (lastPruneTime + 2 * EXPIRE_AFTER < time) {
					lastPruneTime = time;
					prune = true;
				}
			}
		}

		if (prune) {
			cache.values().removeIf(value -> value.addTime + EXPIRE_AFTER < time);
		}

		if (cache.size() > MAX_SIZE) {
			synchronized (cache) {
				if (cache.size() > MAX_SIZE) {
					Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
					iterator.next();
					iterator.remove();
				}
			}
		}

		CacheEntry entry = cache.computeIfAbsent(name, key -> new CacheEntry(time, classProvider.get(key)));
		entry.addTime = time;
		return entry.classNode;
	}

	private static final class CacheEntry {
		private long addTime;
		private final @Nullable ClassNode classNode;

		private CacheEntry(long addTime, @Nullable ClassNode classNode) {
			this.addTime = addTime;
			this.classNode = classNode;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof CacheEntry ce && Objects.equals(classNode, ce.classNode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(addTime, classNode);
		}
	}
}
