package cuchaz.enigma;

import com.google.common.collect.Maps;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import java.util.Map;

/**
 * Common cache functions
 */
public abstract class CachingTypeLoader implements ITypeLoader {
	protected static final byte[] EMPTY_ARRAY = {};

	private final Map<String, byte[]> cache = Maps.newHashMap();

	protected abstract byte[] doLoad(String className);

	@Override
	public boolean tryLoadType(String className, Buffer out) {

		// check the cache
		byte[] data = this.cache.computeIfAbsent(className, this::doLoad);

		if (data == EMPTY_ARRAY) {
			return false;
		}

		out.reset(data.length);
		System.arraycopy(data, 0, out.array(), out.position(), data.length);
		out.position(0);
		return true;
	}

	public void clearCache() {
		this.cache.clear();
	}
}
