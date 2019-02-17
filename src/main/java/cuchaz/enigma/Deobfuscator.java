/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import com.google.common.base.Functions;
import com.google.common.base.Stopwatch;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.IndexTreeBuilder;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.bytecode.translators.SourceFixVisitor;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class Deobfuscator {

	private final ServiceLoader<EnigmaPlugin> plugins = ServiceLoader.load(EnigmaPlugin.class);
	private final ParsedJar parsedJar;
	private final JarIndex jarIndex;
	private final IndexTreeBuilder indexTreeBuilder;

	private final SourceProvider obfSourceProvider;

	private EntryRemapper mapper;

	public Deobfuscator(ParsedJar jar, Consumer<String> listener) {
		this.parsedJar = jar;

		// build the jar index
		this.jarIndex = JarIndex.empty();
		this.jarIndex.indexJar(this.parsedJar, listener);

		listener.accept("Initializing plugins...");
		for (EnigmaPlugin plugin : getPlugins()) {
			plugin.onClassesLoaded(parsedJar.getClassDataMap(), parsedJar::getClassNode);
		}

		this.indexTreeBuilder = new IndexTreeBuilder(jarIndex);

		listener.accept("Preparing...");

		CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(parsedJar);
		typeLoader.addVisitor(visitor -> new SourceFixVisitor(Opcodes.ASM5, visitor, jarIndex));

		this.obfSourceProvider = new SourceProvider(SourceProvider.createSettings(), typeLoader);

		// init mappings
		mapper = new EntryRemapper(jarIndex);
	}

	public Deobfuscator(JarFile jar, Consumer<String> listener) throws IOException {
		this(new ParsedJar(jar), listener);
	}

	public Deobfuscator(ParsedJar jar) {
		this(jar, (msg) -> {
		});
	}

	public Deobfuscator(JarFile jar) throws IOException {
		this(jar, (msg) -> {
		});
	}

	public ServiceLoader<EnigmaPlugin> getPlugins() {
		return plugins;
	}

	public ParsedJar getJar() {
		return this.parsedJar;
	}

	public JarIndex getJarIndex() {
		return this.jarIndex;
	}

	public IndexTreeBuilder getIndexTreeBuilder() {
		return indexTreeBuilder;
	}

	public EntryRemapper getMapper() {
		return this.mapper;
	}

	public void setMappings(EntryTree<EntryMapping> mappings) {
		if (mappings != null) {
			Collection<Entry<?>> dropped = dropMappings(mappings);
			mapper = new EntryRemapper(jarIndex, mappings);

			DeltaTrackingTree<EntryMapping> obfToDeobf = mapper.getObfToDeobf();
			for (Entry<?> entry : dropped) {
				obfToDeobf.trackDeletion(entry);
			}
		} else {
			mapper = new EntryRemapper(jarIndex);
		}
	}

	private Collection<Entry<?>> dropMappings(EntryTree<EntryMapping> mappings) {
		// drop mappings that don't match the jar
		MappingsChecker checker = new MappingsChecker(jarIndex, mappings);
		MappingsChecker.Dropped dropped = checker.dropBrokenMappings();

		Map<Entry<?>, String> droppedMappings = dropped.getDroppedMappings();
		for (Map.Entry<Entry<?>, String> mapping : droppedMappings.entrySet()) {
			System.out.println("WARNING: Couldn't find " + mapping.getKey() + " (" + mapping.getValue() + ") in jar. Mapping was dropped.");
		}

		return droppedMappings.keySet();
	}

	public void getSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		for (ClassEntry obfClassEntry : this.jarIndex.getEntryIndex().getClasses()) {
			// skip inner classes
			if (obfClassEntry.isInnerClass()) {
				continue;
			}

			// separate the classes
			ClassEntry deobfClassEntry = mapper.deobfuscate(obfClassEntry);
			if (!deobfClassEntry.equals(obfClassEntry)) {
				// if the class has a mapping, clearly it's deobfuscated
				deobfClasses.add(obfClassEntry);
			} else if (obfClassEntry.getPackageName() != null) {
				// also call it deobufscated if it's not in the none package
				deobfClasses.add(obfClassEntry);
			} else {
				// otherwise, assume it's still obfuscated
				obfClasses.add(obfClassEntry);
			}
		}
	}

	public SourceProvider getObfSourceProvider() {
		return obfSourceProvider;
	}

	public void writeSources(Path outputDirectory, ProgressListener progress) {
		// get the classes to decompile
		Collection<ClassEntry> classEntries = jarIndex.getEntryIndex().getClasses();

		Stopwatch stopwatch = Stopwatch.createStarted();

		try {
			Translator deobfuscator = mapper.getDeobfuscator();

			// deobfuscate everything first
			Map<String, ClassNode> translatedNodes = deobfuscateClasses(progress, classEntries, deobfuscator);

			decompileClasses(outputDirectory, progress, translatedNodes);
		} finally {
			stopwatch.stop();

			System.out.println("writeSources Done in : " + stopwatch.toString());
		}
	}

	private Map<String, ClassNode> deobfuscateClasses(ProgressListener progress, Collection<ClassEntry> classEntries, Translator translator) {
		AtomicInteger count = new AtomicInteger();
		if (progress != null) {
			progress.init(classEntries.size(), "Deobfuscating classes...");
		}

		return classEntries.parallelStream()
				.map(entry -> {
					ClassEntry translatedEntry = translator.translate(entry);
					if (progress != null) {
						progress.step(count.getAndIncrement(), translatedEntry.toString());
					}

					ClassNode node = parsedJar.getClassNode(entry.getFullName());
					if (node != null) {
						ClassNode translatedNode = new ClassNode();
						node.accept(new TranslationClassVisitor(jarIndex, translator, Opcodes.ASM5, translatedNode));
						return translatedNode;
					}

					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(n -> n.name, Functions.identity()));
	}

	private void decompileClasses(Path outputDirectory, ProgressListener progress, Map<String, ClassNode> translatedClasses) {
		Collection<ClassNode> decompileClasses = translatedClasses.values().stream()
				.filter(classNode -> classNode.name.indexOf('$') == -1)
				.collect(Collectors.toList());

		if (progress != null) {
			progress.init(decompileClasses.size(), "Decompiling classes...");
		}

		//create a common instance outside the loop as mappings shouldn't be changing while this is happening
		CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(translatedClasses::get);
		typeLoader.addVisitor(visitor -> new SourceFixVisitor(Opcodes.ASM5, visitor, jarIndex));

		//synchronized to make sure the parallelStream doesn't CME with the cache
		ITypeLoader synchronizedTypeLoader = new SynchronizedTypeLoader(typeLoader);

		MetadataSystem metadataSystem = new Deobfuscator.NoRetryMetadataSystem(synchronizedTypeLoader);

		//ensures methods are loaded on classload and prevents race conditions
		metadataSystem.setEagerMethodLoadingEnabled(true);

		DecompilerSettings settings = SourceProvider.createSettings();
		SourceProvider sourceProvider = new SourceProvider(settings, synchronizedTypeLoader, metadataSystem);

		AtomicInteger count = new AtomicInteger();

		decompileClasses.parallelStream().forEach(translatedNode -> {
			if (progress != null) {
				progress.step(count.getAndIncrement(), translatedNode.name);
			}

			decompileClass(outputDirectory, translatedNode, sourceProvider);
		});
	}

	private void decompileClass(Path outputDirectory, ClassNode translatedNode, SourceProvider sourceProvider) {
		try {
			// get the source
			CompilationUnit sourceTree = sourceProvider.getSources(translatedNode.name);

			Path path = outputDirectory.resolve(translatedNode.name.replace('.', '/') + ".java");
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path)) {
				sourceProvider.writeSource(writer, sourceTree);
			}
		} catch (Throwable t) {
			// don't crash the whole world here, just log the error and keep going
			// TODO: set up logback via log4j
			System.err.println("Unable to decompile class " + translatedNode.name);
			t.printStackTrace(System.err);
		}
	}

	public void writeTransformedJar(File out, ProgressListener progress) {
		Translator deobfuscator = mapper.getDeobfuscator();
		writeTransformedJar(out, progress, (node, visitor) -> {
			ClassEntry entry = new ClassEntry(node.name);
			node.accept(new TranslationClassVisitor(jarIndex, deobfuscator, Opcodes.ASM5, visitor));
			return deobfuscator.translate(entry).getFullName();
		});
	}

	public void writeTransformedJar(File out, ProgressListener progress, ClassTransformer transformer) {
		try (JarOutputStream outJar = new JarOutputStream(new FileOutputStream(out))) {
			if (progress != null) {
				progress.init(parsedJar.getClassCount(), "Transforming classes...");
			}

			AtomicInteger count = new AtomicInteger();
			parsedJar.visitNode(node -> {
				if (progress != null) {
					progress.step(count.getAndIncrement(), node.name);
				}

				try {
					ClassWriter writer = new ClassWriter(0);
					String transformedName = transformer.transform(node, writer);
					outJar.putNextEntry(new JarEntry(transformedName.replace('.', '/') + ".class"));
					outJar.write(writer.toByteArray());
					outJar.closeEntry();
				} catch (Throwable t) {
					throw new Error("Unable to transform class " + node.name, t);
				}
			});
		} catch (IOException ex) {
			throw new Error("Unable to write to Jar file!");
		}
	}

	public AccessModifier getModifier(Entry<?> entry) {
		EntryMapping mapping = mapper.getDeobfMapping(entry);
		if (mapping == null) {
			return AccessModifier.UNCHANGED;
		}
		return mapping.getAccessModifier();
	}

	public void changeModifier(Entry<?> entry, AccessModifier modifier) {
		EntryMapping mapping = mapper.getDeobfMapping(entry);
		if (mapping != null) {
			mapper.mapFromObf(entry, new EntryMapping(mapping.getTargetName(), modifier));
		} else {
			mapper.mapFromObf(entry, new EntryMapping(entry.getName(), modifier));
		}
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

	public boolean isRemapped(Entry<?> entry) {
		EntryResolver resolver = mapper.getObfResolver();
		DeltaTrackingTree<EntryMapping> mappings = mapper.getObfToDeobf();
		return resolver.resolveEntry(entry, ResolutionStrategy.RESOLVE_ROOT).stream()
				.anyMatch(mappings::contains);
	}

	public void rename(Entry<?> obfEntry, String newName) {
		mapper.mapFromObf(obfEntry, new EntryMapping(newName));
	}

	public void removeMapping(Entry<?> obfEntry) {
		mapper.removeByObf(obfEntry);
	}

	public void markAsDeobfuscated(Entry<?> obfEntry) {
		mapper.mapFromObf(obfEntry, new EntryMapping(mapper.deobfuscate(obfEntry).getName()));
	}

	public <T extends Translatable> T deobfuscate(T translatable) {
		return mapper.deobfuscate(translatable);
	}

	public interface ClassTransformer {
		String transform(ClassNode node, ClassVisitor visitor);
	}

	public static class NoRetryMetadataSystem extends MetadataSystem {
		private final Set<String> _failedTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());

		public NoRetryMetadataSystem(final ITypeLoader typeLoader) {
			super(typeLoader);
		}

		@Override
		protected synchronized TypeDefinition resolveType(final String descriptor, final boolean mightBePrimitive) {
			if (_failedTypes.contains(descriptor)) {
				return null;
			}

			final TypeDefinition result = super.resolveType(descriptor, mightBePrimitive);

			if (result == null) {
				_failedTypes.add(descriptor);
			}

			return result;
		}

		@Override
		public synchronized TypeDefinition resolve(final TypeReference type) {
			return super.resolve(type);
		}
	}
}
