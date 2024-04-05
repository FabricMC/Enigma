package cuchaz.enigma.source.vineflower;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.utils.AsmUtil;

class EnigmaContextSource implements IContextSource {
	private final IContextSource classpathSource = new ClasspathSource();
	private final ClassProvider classProvider;
	private final String className;
	private Entries entries;

	EnigmaContextSource(ClassProvider classProvider, String className) {
		this.classProvider = classProvider;
		this.className = className;
	}

	public IContextSource getClasspath() {
		return classpathSource;
	}

	@Override
	public String getName() {
		return "Enigma-provided context for class " + className;
	}

	@Override
	public Entries getEntries() {
		computeEntriesIfNecessary();
		return entries;
	}

	private void computeEntriesIfNecessary() {
		if (entries != null) {
			return;
		}

		synchronized (this) {
			if (entries != null) return;

			List<String> classNames = new ArrayList<>();
			classNames.add(className);

			int dollarIndex = className.indexOf('$');
			String outermostClass = dollarIndex == -1 ? className : className.substring(0, className.indexOf('$'));
			String outermostClassSuffixed = outermostClass + "$";

			for (String currentClass : classProvider.getClassNames()) {
				if (currentClass.startsWith(outermostClassSuffixed) && !currentClass.equals(className)) {
					classNames.add(currentClass);
				}
			}

			List<Entry> classes = classNames.stream()
					.map(Entry::atBase)
					.toList();

			entries = new Entries(classes, Collections.emptyList(), Collections.emptyList());
		}
	}

	@Override
	public InputStream getInputStream(String resource) {
		ClassNode node = classProvider.get(resource.substring(0, resource.lastIndexOf(".class")));

		if (node == null) {
			return null;
		}

		return new ByteArrayInputStream(AsmUtil.nodeToBytes(node));
	}

	@Override
	public IOutputSink createOutputSink(IResultSaver saver) {
		return new IOutputSink() {
			@Override
			public void begin() { }

			@Override
			public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
				if (qualifiedName.equals(EnigmaContextSource.this.className)) {
					saver.saveClassFile(null, qualifiedName, fileName, content, mapping);
				}
			}

			@Override
			public void acceptDirectory(String directory) { }

			@Override
			public void acceptOther(String path) { }

			@Override
			public void close() { }
		};
	}

	public class ClasspathSource implements IContextSource {
		@Override
		public String getName() {
			return "Enigma-provided classpath context for " + EnigmaContextSource.this.className;
		}

		@Override
		public Entries getEntries() {
			return Entries.EMPTY;
		}

		@Override
		public boolean isLazy() {
			return true;
		}

		@Override
		public InputStream getInputStream(String resource) {
			return EnigmaContextSource.this.getInputStream(resource);
		}
	}
}
