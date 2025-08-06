package cuchaz.enigma.api.service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

	/**
	 * Creates multiple thread-local {@code ClassVisitor}s, runs each on a subset of classes on their own thread, and
	 * combines them at the end.
	 */
	static <V extends ClassVisitor> JarIndexerService fromVisitorsInParallel(Supplier<V> visitorCreator, Consumer<Collection<V>> combiner) {
		return (scope, classProvider, jarIndex) -> {
			CopyOnWriteArrayList<V> allVisitors = new CopyOnWriteArrayList<>();
			ThreadLocal<V> visitors = ThreadLocal.withInitial(() -> {
				V visitor = visitorCreator.get();
				allVisitors.add(visitor);
				return visitor;
			});
			scope.parallelStream().forEach(className -> classProvider.get(className).accept(visitors.get()));
			combiner.accept(allVisitors);
		};
	}
}
