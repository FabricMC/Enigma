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

import com.google.common.base.Stopwatch;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.ReferencedEntryPool;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;
import oml.ast.transformers.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
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
	private final ReferencedEntryPool entryPool = new ReferencedEntryPool();
	private final ParsedJar parsedJar;
	private final DecompilerSettings settings;
	private final JarIndex jarIndex;
	private final IndexTreeBuilder indexTreeBuilder;
	private BidirectionalMapper mapper;

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
		// config the decompiler
		this.settings = DecompilerSettings.javaDefaults();
		this.settings.setMergeVariables(Utils.getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		this.settings.setForceExplicitImports(Utils.getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		this.settings.setForceExplicitTypeArguments(
				Utils.getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		// DEBUG
		this.settings.setShowDebugLineNumbers(Utils.getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		this.settings.setShowSyntheticMembers(Utils.getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));

		// init mappings
		mapper = new BidirectionalMapper(jarIndex);
	}

	public Deobfuscator(JarFile jar, Consumer<String> listener) throws IOException {
		this(new ParsedJar(jar), listener);
	}

	public Deobfuscator(ParsedJar jar) throws IOException {
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

	public BidirectionalMapper getMapper() {
		return this.mapper;
	}

	public void setMappings(EntryTree<EntryMapping> mappings) {
		if (mappings != null) {
			Collection<Entry<?>> dropped = dropMappings(mappings);
			mapper = new BidirectionalMapper(jarIndex, mappings);

			DeltaTrackingTree<EntryMapping> deobfToObf = mapper.getDeobfToObf();
			for (Entry<?> entry : dropped) {
				deobfToObf.trackDeletion(entry);
			}
		} else {
			mapper = new BidirectionalMapper(jarIndex);
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
				deobfClasses.add(deobfClassEntry);
			} else if (obfClassEntry.getPackageName() != null) {
				// also call it deobufscated if it's not in the none package
				deobfClasses.add(obfClassEntry);
			} else {
				// otherwise, assume it's still obfuscated
				obfClasses.add(obfClassEntry);
			}
		}
	}

	public TranslatingTypeLoader createTypeLoader() {
		return new TranslatingTypeLoader(
				this.parsedJar,
				this.jarIndex,
				this.entryPool,
				this.mapper.getObfuscator(),
				this.mapper.getDeobfuscator()
		);
	}

	public CompilationUnit getSourceTree(String className) {
		return getSourceTree(className, createTypeLoader());
	}

	public CompilationUnit getSourceTree(String className, ITranslatingTypeLoader loader) {
		return getSourceTree(className, loader, new NoRetryMetadataSystem(loader));
	}

	public CompilationUnit getSourceTree(String className, ITranslatingTypeLoader loader, MetadataSystem metadataSystem) {

		// we don't know if this class name is obfuscated or deobfuscated
		// we need to tell the decompiler the deobfuscated name so it doesn't get freaked out
		// the decompiler only sees classes after deobfuscation, so we need to load it by the deobfuscated name if there is one

		String deobfClassName = mapper.deobfuscate(new ClassEntry(className)).getFullName();

		// set the desc loader
		this.settings.setTypeLoader(loader);

		// see if procyon can find the desc
		TypeReference type = metadataSystem.lookupType(deobfClassName);
		if (type == null) {
			throw new Error(String.format("Unable to find desc: %s (deobf: %s)\nTried class names: %s",
					className, deobfClassName, loader.getClassNamesToTry(deobfClassName)
			));
		}
		TypeDefinition resolvedType = type.resolve();

		// decompile it!
		DecompilerContext context = new DecompilerContext();
		context.setCurrentType(resolvedType);
		context.setSettings(this.settings);
		AstBuilder builder = new AstBuilder(context);
		builder.addType(resolvedType);
		builder.runTransformations(null);
		runCustomTransforms(builder, context);
		return builder.getCompilationUnit();
	}

	public SourceIndex getSourceIndex(CompilationUnit sourceTree, String source) {
		return getSourceIndex(sourceTree, source, true);
	}

	public SourceIndex getSourceIndex(CompilationUnit sourceTree, String source, boolean ignoreBadTokens) {

		// build the source index
		SourceIndex index = new SourceIndex(source, ignoreBadTokens);
		sourceTree.acceptVisitor(new SourceIndexVisitor(entryPool), index);

		// resolve all the classes in the source references
		for (Token token : index.referenceTokens()) {
			EntryReference<Entry<?>, Entry<?>> deobfReference = index.getDeobfReference(token);

			EntryResolver resolver = mapper.getDeobfResolver();
			index.replaceDeobfReference(token, resolver.resolveReference(deobfReference));
		}

		return index;
	}

	public String getSource(CompilationUnit sourceTree) {
		// render the AST into source
		StringWriter buf = new StringWriter();
		sourceTree.acceptVisitor(new InsertParenthesesVisitor(), null);
		sourceTree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(buf), this.settings), null);
		return buf.toString();
	}

	public void writeSources(File dirOut, ProgressListener progress) {
		// get the classes to decompile
		Set<ClassEntry> classEntries = jarIndex.getEntryIndex().getClasses().stream()
				.filter(classEntry -> !classEntry.isInnerClass())
				.collect(Collectors.toSet());

		if (progress != null) {
			progress.init(classEntries.size(), "Decompiling classes...");
		}

		//create a common instance outside the loop as mappings shouldn't be changing while this is happening
		//synchronized to make sure the parallelStream doesn't CME with the cache
		ITranslatingTypeLoader typeLoader = new SynchronizedTypeLoader(createTypeLoader());

		MetadataSystem metadataSystem = new NoRetryMetadataSystem(typeLoader);
		metadataSystem.setEagerMethodLoadingEnabled(true);//ensures methods are loaded on classload and prevents race conditions

		// DEOBFUSCATE ALL THE THINGS!! @_@
		Stopwatch stopwatch = Stopwatch.createStarted();
		AtomicInteger count = new AtomicInteger();
		classEntries.parallelStream().forEach(obfClassEntry -> {
			ClassEntry deobfClassEntry = mapper.deobfuscate(obfClassEntry);
			if (progress != null) {
				progress.step(count.getAndIncrement(), deobfClassEntry.toString());
			}

			try {
				// get the source
				CompilationUnit sourceTree = getSourceTree(obfClassEntry.getName(), typeLoader, metadataSystem);

				// write the file
				File file = new File(dirOut, deobfClassEntry.getName().replace('.', '/') + ".java");
				file.getParentFile().mkdirs();
				try (Writer writer = new BufferedWriter(new FileWriter(file))) {
					sourceTree.acceptVisitor(new InsertParenthesesVisitor(), null);
					sourceTree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(writer), settings), null);
				}
			} catch (Throwable t) {
				// don't crash the whole world here, just log the error and keep going
				// TODO: set up logback via log4j
				System.err.println("Unable to decompile class " + deobfClassEntry + " (" + obfClassEntry + ")");
				t.printStackTrace(System.err);
			}
		});
		stopwatch.stop();
		System.out.println("writeSources Done in : " + stopwatch.toString());
		if (progress != null) {
			progress.step(count.get(), "Done:");
		}
	}

	public void writeJar(File out, ProgressListener progress) {
		transformJar(out, progress, createTypeLoader()::transformInto);
	}

	public void transformJar(File out, ProgressListener progress, ClassTransformer transformer) {
		try (JarOutputStream outJar = new JarOutputStream(new FileOutputStream(out))) {
			if (progress != null) {
				progress.init(parsedJar.getClassCount(), "Transforming classes...");
			}

			AtomicInteger i = new AtomicInteger();
			parsedJar.visitNode(node -> {
				if (progress != null) {
					progress.step(i.getAndIncrement(), node.name);
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

			if (progress != null) {
				progress.step(i.get(), "Done!");
			}
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

	public boolean isObfuscatedIdentifier(Entry<?> obfEntry) {
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
		}

		return this.jarIndex.getEntryIndex().hasEntry(obfEntry);
	}

	public boolean isRenameable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && isObfuscatedIdentifier(obfReference.getNameableEntry());
	}

	public boolean hasDeobfuscatedName(Entry<?> obfEntry) {
		return mapper.hasDeobfMapping(obfEntry);
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

	public static void runCustomTransforms(AstBuilder builder, DecompilerContext context) {
		List<IAstTransform> transformers = Arrays.asList(
				new ObfuscatedEnumSwitchRewriterTransform(context),
				new VarargsFixer(context),
				new RemoveObjectCasts(context),
				new Java8Generics(),
				new InvalidIdentifierFix()
		);
		for (IAstTransform transform : transformers) {
			transform.run(builder.getCompilationUnit());
		}
	}

	public interface ClassTransformer {
		String transform(ClassNode node, ClassWriter writer);
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

		public synchronized TypeDefinition resolve(final TypeReference type) {
			return super.resolve(type);
		}
	}
}
