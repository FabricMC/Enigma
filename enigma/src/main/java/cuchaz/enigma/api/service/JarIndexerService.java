package cuchaz.enigma.api.service;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.ClassProvider;

public interface JarIndexerService extends EnigmaService {
	EnigmaServiceType<JarIndexerService> TYPE = EnigmaServiceType.create("jar_indexer");

	void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex);

	static JarIndexerService fromVisitor(ClassVisitor visitor) {
		return (scope, classProvider, jarIndex) -> {
			for (String className : scope) {
				classProvider.get(className).accept(visitor);
			}
		};
	}
}
