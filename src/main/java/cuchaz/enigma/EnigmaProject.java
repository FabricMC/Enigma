package cuchaz.enigma;

import com.google.common.base.Functions;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.bytecode.translators.SourceFixVisitor;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.MappingsChecker;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class EnigmaProject {
	private final Enigma enigma;

	private final ClassCache classCache;
	private final JarIndex jarIndex;

	private EntryRemapper mapper;

	public EnigmaProject(Enigma enigma, ClassCache classCache, JarIndex jarIndex) {
		this.enigma = enigma;
		this.classCache = classCache;
		this.jarIndex = jarIndex;

		this.mapper = EntryRemapper.empty(jarIndex);
	}

	public void setMappings(EntryTree<EntryMapping> mappings) {
		if (mappings != null) {
			mapper = EntryRemapper.mapped(jarIndex, mappings);
		} else {
			mapper = EntryRemapper.empty(jarIndex);
		}
	}

	public Enigma getEnigma() {
		return enigma;
	}

	public ClassCache getClassCache() {
		return classCache;
	}

	public JarIndex getJarIndex() {
		return jarIndex;
	}

	public EntryRemapper getMapper() {
		return mapper;
	}

	public void dropMappings(ProgressListener progress) {
		DeltaTrackingTree<EntryMapping> mappings = mapper.getObfToDeobf();

		Collection<Entry<?>> dropped = dropMappings(mappings, progress);
		for (Entry<?> entry : dropped) {
			mappings.trackChange(entry);
		}
	}

	private Collection<Entry<?>> dropMappings(EntryTree<EntryMapping> mappings, ProgressListener progress) {
		// drop mappings that don't match the jar
		MappingsChecker checker = new MappingsChecker(jarIndex, mappings);
		MappingsChecker.Dropped dropped = checker.dropBrokenMappings(progress);

		Map<Entry<?>, String> droppedMappings = dropped.getDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedMappings.entrySet()) {
			System.out.println("WARNING: Couldn't find " + mapping.getKey() + " (" + mapping.getValue() + ") in jar. Mapping was dropped.");
		}

		return droppedMappings.keySet();
	}

	public boolean isRenamable(Entry<?> obfEntry) {
		if (obfEntry instanceof MethodEntry) {
			// HACKHACK: Object methods are not obfuscated identifiers
			MethodEntry obfMethodEntry = (MethodEntry) obfEntry;
			String name = obfMethodEntry.getName();
			String sig = obfMethodEntry.getDesc().toString();
			if (name.equals("clone") && sig.equals("()Ljava/lang/Object;")) {
				return false;
			} else if (name.equals("equals") && sig.equals("(Ljava/lang/Object;)Z")) {
				return false;
			} else if (name.equals("finalize") && sig.equals("()V")) {
				return false;
			} else if (name.equals("getClass") && sig.equals("()Ljava/lang/Class;")) {
				return false;
			} else if (name.equals("hashCode") && sig.equals("()I")) {
				return false;
			} else if (name.equals("notify") && sig.equals("()V")) {
				return false;
			} else if (name.equals("notifyAll") && sig.equals("()V")) {
				return false;
			} else if (name.equals("toString") && sig.equals("()Ljava/lang/String;")) {
				return false;
			} else if (name.equals("wait") && sig.equals("()V")) {
				return false;
			} else if (name.equals("wait") && sig.equals("(J)V")) {
				return false;
			} else if (name.equals("wait") && sig.equals("(JI)V")) {
				return false;
			}
		} else if (obfEntry instanceof LocalVariableEntry && !((LocalVariableEntry) obfEntry).isArgument()) {
			return false;
		}

		return this.jarIndex.getEntryIndex().hasEntry(obfEntry);
	}

	public boolean isRenamable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && isRenamable(obfReference.getNameableEntry());
	}

	public JarExport exportRemappedJar(ProgressListener progress) {
		Collection<ClassEntry> classEntries = jarIndex.getEntryIndex().getClasses();
		Translator deobfuscator = mapper.getDeobfuscator();

		AtomicInteger count = new AtomicInteger();
		progress.init(classEntries.size(), "Deobfuscating classes...");

		Map<String, ClassNode> compiled = classEntries.parallelStream()
				.map(entry -> {
					ClassEntry translatedEntry = deobfuscator.translate(entry);
					progress.step(count.getAndIncrement(), translatedEntry.toString());

					ClassNode node = classCache.getClassNode(entry.getFullName());
					if (node != null) {
						ClassNode translatedNode = new ClassNode();
						node.accept(new TranslationClassVisitor(deobfuscator, Opcodes.ASM5, translatedNode));
						return translatedNode;
					}

					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(n -> n.name, Functions.identity()));

		return new JarExport(jarIndex, compiled);
	}

	public static final class JarExport {
		private final JarIndex jarIndex;
		private final Map<String, ClassNode> compiled;

		JarExport(JarIndex jarIndex, Map<String, ClassNode> compiled) {
			this.jarIndex = jarIndex;
			this.compiled = compiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(this.compiled.size(), "Writing jar...");

			try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
				AtomicInteger count = new AtomicInteger();

				for (ClassNode node : this.compiled.values()) {
					progress.step(count.getAndIncrement(), node.name);

					String entryName = node.name.replace('.', '/') + ".class";

					ClassWriter writer = new ClassWriter(0);
					node.accept(writer);

					out.putNextEntry(new JarEntry(entryName));
					out.write(writer.toByteArray());
					out.closeEntry();
				}
			}
		}

		public SourceExport decompile(ProgressListener progress) {
			Collection<ClassNode> classes = this.compiled.values().stream()
					.filter(classNode -> classNode.name.indexOf('$') == -1)
					.collect(Collectors.toList());

			progress.init(classes.size(), "Decompiling classes...");

			//create a common instance outside the loop as mappings shouldn't be changing while this is happening
			CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(this.compiled::get);
			typeLoader.addVisitor(visitor -> new SourceFixVisitor(Opcodes.ASM5, visitor, jarIndex));

			//synchronized to make sure the parallelStream doesn't CME with the cache
			ITypeLoader synchronizedTypeLoader = new SynchronizedTypeLoader(typeLoader);

			MetadataSystem metadataSystem = new NoRetryMetadataSystem(synchronizedTypeLoader);

			//ensures methods are loaded on classload and prevents race conditions
			metadataSystem.setEagerMethodLoadingEnabled(true);

			DecompilerSettings settings = SourceProvider.createSettings();
			SourceProvider sourceProvider = new SourceProvider(settings, synchronizedTypeLoader, metadataSystem);

			AtomicInteger count = new AtomicInteger();

			Collection<ClassSource> decompiled = classes.parallelStream()
					.map(translatedNode -> {
						progress.step(count.getAndIncrement(), translatedNode.name);

						String source = decompileClass(translatedNode, sourceProvider);
						return new ClassSource(translatedNode.name, source);
					})
					.collect(Collectors.toList());

			return new SourceExport(decompiled);
		}

		private String decompileClass(ClassNode translatedNode, SourceProvider sourceProvider) {
			CompilationUnit sourceTree = sourceProvider.getSources(translatedNode.name);

			StringWriter writer = new StringWriter();
			sourceProvider.writeSource(writer, sourceTree);

			return writer.toString();
		}
	}

	public static final class SourceExport {
		private final Collection<ClassSource> decompiled;

		SourceExport(Collection<ClassSource> decompiled) {
			this.decompiled = decompiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(decompiled.size(), "Writing sources...");

			int count = 0;
			for (ClassSource source : decompiled) {
				progress.step(count++, source.name);

				Path sourcePath = source.resolvePath(path);
				source.writeTo(sourcePath);
			}
		}
	}

	private static class ClassSource {
		private final String name;
		private final String source;

		ClassSource(String name, String source) {
			this.name = name;
			this.source = source;
		}

		void writeTo(Path path) throws IOException {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write(source);
			}
		}

		Path resolvePath(Path root) {
			return root.resolve(name.replace('.', '/') + ".java");
		}
	}
}
