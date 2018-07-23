package cuchaz.enigma;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;

/**
 * Caching version of {@link ClasspathTypeLoader}
 */
public class CachingClasspathTypeLoader extends CachingTypeLoader {
	private static ITypeLoader extraClassPathLoader = null;

	public static void setExtraClassPathLoader(ITypeLoader loader){
		extraClassPathLoader = loader;
	}

	private final ITypeLoader classpathLoader = new ClasspathTypeLoader();

	protected byte[] doLoad(String className) {
		Buffer parentBuf = new Buffer();
		if (classpathLoader.tryLoadType(className, parentBuf)) {
			return parentBuf.array();
		}
		if (extraClassPathLoader != null){
			parentBuf.reset();
			if (extraClassPathLoader.tryLoadType(className, parentBuf)){
				return parentBuf.array();
			}
		}
		return EMPTY_ARRAY;//need to return *something* as null means no store
	}
}
