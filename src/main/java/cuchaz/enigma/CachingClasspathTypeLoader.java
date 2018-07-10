package cuchaz.enigma;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;

/**
 * Caching version of {@link ClasspathTypeLoader}
 */
class CachingClasspathTypeLoader extends CachingTypeLoader {
	private final ITypeLoader classpathLoader = new ClasspathTypeLoader();

	protected byte[] doLoad(String className) {
		Buffer parentBuf = new Buffer();
		if (classpathLoader.tryLoadType(className, parentBuf)) {
			return parentBuf.array();
		}
		return EMPTY_ARRAY;//need to return *something* as null means no store
	}
}
