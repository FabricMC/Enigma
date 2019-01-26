package cuchaz.enigma.translation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

public class CachingTranslator implements Translator, AutoCloseable {
	private static final int DEFAULT_CACHE_SIZE = 1024;

	private final LoadingCache<Translatable, Translatable> cache;

	public CachingTranslator(Translator inner, int cacheSize) {
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(cacheSize)
				.build(new CacheLoader<Translatable, Translatable>() {
					@Override
					public Translatable load(Translatable translatable) {
						return inner.translate(translatable);
					}
				});
	}

	public CachingTranslator(Translator inner) {
		this(inner, DEFAULT_CACHE_SIZE);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Translatable> T translate(T translatable) {
		try {
			return (T) cache.get(translatable);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		cache.invalidateAll();
		cache.cleanUp();
	}
}
