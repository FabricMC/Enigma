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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import cuchaz.enigma.bytecode.ClassProtectifier;
import cuchaz.enigma.bytecode.ClassPublifier;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.mapping.entry.*;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.utils.Utils;
import oml.ast.transformers.InvalidIdentifierFix;
import oml.ast.transformers.Java8Generics;
import oml.ast.transformers.ObfuscatedEnumSwitchRewriterTransform;
import oml.ast.transformers.RemoveObjectCasts;
import oml.ast.transformers.VaragsFixer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Deobfuscator {

	private final ReferencedEntryPool entryPool = new ReferencedEntryPool();
	private final ParsedJar parsedJar;
	private final DecompilerSettings settings;
	private final JarIndex jarIndex;
	private final MappingsRenamer renamer;
	private final Map<TranslationDirection, Translator> translatorCache;
	private Mappings mappings;

	public Deobfuscator(ParsedJar jar) {
		this.parsedJar = jar;

		// build the jar index
		this.jarIndex = new JarIndex(entryPool);
		this.jarIndex.indexJar(this.parsedJar, true);

		// config the decompiler
		this.settings = DecompilerSettings.javaDefaults();
		this.settings.setMergeVariables(Utils.getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		this.settings.setForceExplicitImports(Utils.getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		this.settings.setForceExplicitTypeArguments(
				Utils.getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		// DEBUG
		this.settings.setShowDebugLineNumbers(Utils.getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		this.settings.setShowSyntheticMembers(Utils.getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));

		// init defaults
		this.translatorCache = Maps.newTreeMap();
		this.renamer = new MappingsRenamer(this.jarIndex, null, this.entryPool);
		// init mappings
		setMappings(new Mappings());
	}

	public Deobfuscator(JarFile jar) throws IOException {
		this(new ParsedJar(jar));
	}

	public ParsedJar getJar() {
		return this.parsedJar;
	}

	public JarIndex getJarIndex() {
		return this.jarIndex;
	}

	public Mappings getMappings() {
		return this.mappings;
	}

	public void setMappings(Mappings val) {
		setMappings(val, true);
	}

	public void setMappings(Mappings val, boolean warnAboutDrops) {
		if (val == null) {
			val = new Mappings();
		}

		// drop mappings that don't match the jar
		MappingsChecker checker = new MappingsChecker(this.jarIndex);
		checker.dropBrokenMappings(val);
		if (warnAboutDrops) {
			for (Map.Entry<ClassEntry, ClassMapping> mapping : checker.getDroppedClassMappings().entrySet()) {
				System.out.println("WARNING: Couldn't find class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
			}
			for (Map.Entry<ClassEntry, ClassMapping> mapping : checker.getDroppedInnerClassMappings().entrySet()) {
				System.out.println("WARNING: Couldn't find inner class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
			}
			for (Map.Entry<FieldEntry, FieldMapping> mapping : checker.getDroppedFieldMappings().entrySet()) {
				System.out.println("WARNING: Couldn't find field entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
			}
			for (Map.Entry<MethodEntry, MethodMapping> mapping : checker.getDroppedMethodMappings().entrySet()) {
				System.out.println("WARNING: Couldn't find behavior entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
			}
		}

		this.mappings = val;
		this.renamer.setMappings(mappings);
		this.translatorCache.clear();
	}

	public Translator getTranslator(TranslationDirection direction) {
		return this.translatorCache.computeIfAbsent(direction,
				k -> this.mappings.getTranslator(direction, this.jarIndex.getTranslationIndex()));
	}

	public void getSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		for (ClassEntry obfClassEntry : this.jarIndex.getObfClassEntries()) {
			// skip inner classes
			if (obfClassEntry.isInnerClass()) {
				continue;
			}

			// separate the classes
			ClassEntry deobfClassEntry = deobfuscateEntry(obfClassEntry);
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
				getTranslator(TranslationDirection.OBFUSCATING),
				getTranslator(TranslationDirection.DEOBFUSCATING)
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

		// first, assume class name is deobf
		String deobfClassName = className;

		// if it wasn't actually deobf, then we can find a mapping for it and get the deobf name
		ClassMapping classMapping = this.mappings.getClassByObf(className);
		if (classMapping != null && classMapping.getDeobfName() != null) {
			deobfClassName = classMapping.getDeobfName();
		}

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
		return getSourceIndex(sourceTree, source, null);
	}

	public SourceIndex getSourceIndex(CompilationUnit sourceTree, String source, Boolean ignoreBadTokens) {

		// build the source index
		SourceIndex index;
		if (ignoreBadTokens != null) {
			index = new SourceIndex(source, ignoreBadTokens);
		} else {
			index = new SourceIndex(source);
		}
		sourceTree.acceptVisitor(new SourceIndexVisitor(entryPool), index);

		// DEBUG
		// sourceTree.acceptVisitor( new TreeDumpVisitor( new File( "tree.txt" ) ), null );

		// resolve all the classes in the source references
		for (Token token : index.referenceTokens()) {
			EntryReference<Entry, Entry> deobfReference = index.getDeobfReference(token);

			// get the obfuscated entry
			Entry obfEntry = obfuscateEntry(deobfReference.entry);

			// try to resolve the class
			ClassEntry resolvedObfClassEntry = this.jarIndex.getTranslationIndex().resolveEntryOwner(obfEntry);
			if (resolvedObfClassEntry != null && !resolvedObfClassEntry.equals(obfEntry.getOwnerClassEntry())) {
				// change the class of the entry
				obfEntry = obfEntry.updateOwnership(resolvedObfClassEntry);

				// save the new deobfuscated reference
				deobfReference.entry = deobfuscateEntry(obfEntry);
				index.replaceDeobfReference(token, deobfReference);
			}

			// DEBUG
			// System.out.println( token + " -> " + reference + " -> " + index.getReferenceToken( reference ) );
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
		Set<ClassEntry> classEntries = Sets.newHashSet();
		for (ClassEntry obfClassEntry : this.jarIndex.getObfClassEntries()) {
			// skip inner classes
			if (obfClassEntry.isInnerClass()) {
				continue;
			}

			classEntries.add(obfClassEntry);
		}

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
			ClassEntry deobfClassEntry = deobfuscateEntry(new ClassEntry(obfClassEntry));
			if (progress != null) {
				progress.onProgress(count.getAndIncrement(), deobfClassEntry.toString());
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
				System.err.println("Unable to deobfuscate class " + deobfClassEntry + " (" + obfClassEntry + ")");
				t.printStackTrace(System.err);
			}
		});
		stopwatch.stop();
		System.out.println("writeSources Done in : " + stopwatch.toString());
		if (progress != null) {
			progress.onProgress(count.get(), "Done:");
		}
	}

	private void addAllPotentialAncestors(Set<ClassEntry> classEntries, ClassEntry classObfEntry) {
		for (ClassEntry interfaceEntry : jarIndex.getTranslationIndex().getInterfaces(classObfEntry)) {
			if (classEntries.add(interfaceEntry)) {
				addAllPotentialAncestors(classEntries, interfaceEntry);
			}
		}

		ClassEntry superClassEntry = jarIndex.getTranslationIndex().getSuperclass(classObfEntry);
		if (superClassEntry != null && classEntries.add(superClassEntry)) {
			addAllPotentialAncestors(classEntries, superClassEntry);
		}
	}

	public boolean isMethodProvider(ClassEntry classObfEntry, MethodEntry methodEntry) {
		Set<ClassEntry> classEntries = new HashSet<>();
		addAllPotentialAncestors(classEntries, classObfEntry);

		for (ClassEntry parentEntry : classEntries) {
			MethodEntry ancestorMethodEntry = entryPool.getMethod(parentEntry, methodEntry.getName(), methodEntry.getDesc());
			if (jarIndex.containsObfMethod(ancestorMethodEntry)) {
				return false;
			}
		}

		return true;
	}

	public void rebuildMethodNames(ProgressListener progress) {
		final AtomicInteger i = new AtomicInteger();
		Map<ClassMapping, Map<Entry, String>> renameClassMap = new HashMap<>();

		progress.init(getMappings().classes().size() * 3, "Rebuilding method names");

		Lists.newArrayList(getMappings().classes()).parallelStream().forEach(classMapping -> {
			progress.onProgress(i.getAndIncrement(), classMapping.getDeobfName());
			rebuildMethodNames(classMapping, renameClassMap);
		});


		renameClassMap.entrySet().parallelStream().forEach(renameClassMapEntry -> {
			progress.onProgress(i.getAndIncrement(), renameClassMapEntry.getKey().getDeobfName());
			for (Map.Entry<Entry, String> entry : renameClassMapEntry.getValue().entrySet()) {
				Entry obfEntry = entry.getKey();

				removeMapping(obfEntry);
			}
		});

		renameClassMap.entrySet().parallelStream().forEach(renameClassMapEntry -> {
			progress.onProgress(i.getAndIncrement(), renameClassMapEntry.getKey().getDeobfName());

			for (Map.Entry<Entry, String> entry : renameClassMapEntry.getValue().entrySet()) {
				Entry obfEntry = entry.getKey();
				String name = entry.getValue();

				try {
					rename(obfEntry, name);
				} catch (IllegalNameException exception) {
					System.out.println("WARNING: " + exception.getMessage());
				}
			}
		});
	}

	private void rebuildMethodNames(ClassMapping classMapping, Map<ClassMapping, Map<Entry, String>> renameClassMap) {
		Map<Entry, String> renameEntries = new HashMap<>();

		for (MethodMapping methodMapping : Lists.newArrayList(classMapping.methods())) {
			ClassEntry classObfEntry = classMapping.getObfEntry();
			MethodEntry obfEntry = methodMapping.getObfEntry(classObfEntry);

			if (isMethodProvider(classObfEntry, obfEntry)) {
				if (hasDeobfuscatedName(obfEntry)
						&& !(methodMapping.getDeobfName().equals(methodMapping.getObfName()))) {
					renameEntries.put(obfEntry, methodMapping.getDeobfName());
				}

				ArrayList<LocalVariableMapping> arguments = Lists.newArrayList(methodMapping.arguments());
				for (LocalVariableMapping localVariableMapping : arguments) {
					Entry argObfEntry = localVariableMapping.getObfEntry(obfEntry);
					if (hasDeobfuscatedName(argObfEntry)) {
						renameEntries.put(argObfEntry, deobfuscateEntry(argObfEntry).getName());
					}
				}
			}
		}

		classMapping.markDirty();
		renameClassMap.put(classMapping, renameEntries);
		for (ClassMapping innerClass : classMapping.innerClasses()) {
			rebuildMethodNames(innerClass, renameClassMap);
		}
	}


	public void writeJar(File out, ProgressListener progress) {
		transformJar(out, progress, createTypeLoader()::transformInto);
	}

	public void protectifyJar(File out, ProgressListener progress) {
		transformJar(out, progress, (node, writer) -> {
			node.accept(new ClassProtectifier(Opcodes.ASM5, writer));
			return node.name;
		});
	}

	public void publifyJar(File out, ProgressListener progress) {
		transformJar(out, progress, (node, writer) -> {
			node.accept(new ClassPublifier(Opcodes.ASM5, writer));
			return node.name;
		});
	}

	public void transformJar(File out, ProgressListener progress, ClassTransformer transformer) {
		try (JarOutputStream outJar = new JarOutputStream(new FileOutputStream(out))) {
			if (progress != null) {
				progress.init(parsedJar.getClassCount(), "Transforming classes...");
			}

			AtomicInteger i = new AtomicInteger();
			parsedJar.visit(node -> {
				if (progress != null) {
					progress.onProgress(i.getAndIncrement(), node.name);
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
				progress.onProgress(i.get(), "Done!");
			}
		} catch (IOException ex) {
			throw new Error("Unable to write to Jar file!");
		}
	}

	public <T extends Entry> T obfuscateEntry(T deobfEntry) {
		if (deobfEntry == null) {
			return null;
		}
		T translatedEntry = getTranslator(TranslationDirection.OBFUSCATING).getTranslatedEntry(deobfEntry);
		if (translatedEntry == null) {
			return deobfEntry;
		}
		return translatedEntry;
	}

	public <T extends Entry> T deobfuscateEntry(T obfEntry) {
		if (obfEntry == null) {
			return null;
		}
		T translatedEntry = getTranslator(TranslationDirection.DEOBFUSCATING).getTranslatedEntry(obfEntry);
		if (translatedEntry == null) {
			return obfEntry;
		}
		return translatedEntry;
	}

	public <E extends Entry, C extends Entry> EntryReference<E, C> obfuscateReference(EntryReference<E, C> deobfReference) {
		if (deobfReference == null) {
			return null;
		}
		return new EntryReference<>(obfuscateEntry(deobfReference.entry), obfuscateEntry(deobfReference.context), deobfReference);
	}

	public <E extends Entry, C extends Entry> EntryReference<E, C> deobfuscateReference(EntryReference<E, C> obfReference) {
		if (obfReference == null) {
			return null;
		}
		return new EntryReference<>(deobfuscateEntry(obfReference.entry), deobfuscateEntry(obfReference.context), obfReference);
	}

	public boolean isObfuscatedIdentifier(Entry obfEntry) {
		return isObfuscatedIdentifier(obfEntry, false);
	}

	public boolean isObfuscatedIdentifier(Entry obfEntry, boolean hack) {

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

			// FIXME: HACK EVEN MORE HACK!
			if (hack && this.jarIndex.containsObfEntry(obfEntry.getOwnerClassEntry()))
				return true;
		}

		return this.jarIndex.containsObfEntry(obfEntry);
	}

	public boolean isRenameable(EntryReference<Entry, Entry> obfReference, boolean activeHack) {
		return obfReference.isNamed() && isObfuscatedIdentifier(obfReference.getNameableEntry(), activeHack);
	}

	public boolean isRenameable(EntryReference<Entry, Entry> obfReference) {
		return isRenameable(obfReference, false);
	}

	public boolean hasDeobfuscatedName(Entry obfEntry) {
		Translator translator = getTranslator(TranslationDirection.DEOBFUSCATING);
		if (obfEntry instanceof ClassEntry) {
			ClassEntry obfClass = (ClassEntry) obfEntry;
			List<ClassMapping> mappingChain = this.mappings.getClassMappingChain(obfClass);
			ClassMapping classMapping = mappingChain.get(mappingChain.size() - 1);
			return classMapping != null && classMapping.getDeobfName() != null;
		} else if (obfEntry instanceof FieldEntry) {
			return translator.hasFieldMapping((FieldEntry) obfEntry);
		} else if (obfEntry instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry) obfEntry;
			if (methodEntry.isConstructor()) {
				return false;
			}
			return translator.hasMethodMapping(methodEntry);
		} else if (obfEntry instanceof LocalVariableEntry) {
			return translator.hasLocalVariableMapping((LocalVariableEntry) obfEntry);
		} else {
			throw new Error("Unknown entry desc: " + obfEntry.getClass().getName());
		}
	}

	public void rename(Entry obfEntry, String newName) {
		rename(obfEntry, newName, true);
	}

	// NOTE: these methods are a bit messy... oh well

	public void rename(Entry obfEntry, String newName, boolean clearCache) {
		if (obfEntry instanceof ClassEntry) {
			this.renamer.setClassName((ClassEntry) obfEntry, newName);
		} else if (obfEntry instanceof FieldEntry) {
			this.renamer.setFieldName((FieldEntry) obfEntry, newName);
		} else if (obfEntry instanceof MethodEntry) {
			if (((MethodEntry) obfEntry).isConstructor()) {
				throw new IllegalArgumentException("Cannot rename constructors");
			}
			this.renamer.setMethodTreeName((MethodEntry) obfEntry, newName);
		} else if (obfEntry instanceof LocalVariableEntry) {
			this.renamer.setLocalVariableTreeName((LocalVariableEntry) obfEntry, newName);
		} else {
			throw new Error("Unknown entry desc: " + obfEntry.getClass().getName());
		}

		// clear caches
		if (clearCache)
			this.translatorCache.clear();
	}

	public void removeMapping(Entry obfEntry) {
		if (obfEntry instanceof ClassEntry) {
			this.renamer.removeClassMapping((ClassEntry) obfEntry);
		} else if (obfEntry instanceof FieldEntry) {
			this.renamer.removeFieldMapping((FieldEntry) obfEntry);
		} else if (obfEntry instanceof MethodEntry) {
			if (((MethodEntry) obfEntry).isConstructor()) {
				throw new IllegalArgumentException("Cannot rename constructors");
			}
			this.renamer.removeMethodTreeMapping((MethodEntry) obfEntry);
		} else if (obfEntry instanceof LocalVariableEntry) {
			this.renamer.removeLocalVariableMapping((LocalVariableEntry) obfEntry);
		} else {
			throw new Error("Unknown entry desc: " + obfEntry);
		}

		// clear caches
		this.translatorCache.clear();
	}

	public void markAsDeobfuscated(Entry obfEntry) {
		if (obfEntry instanceof ClassEntry) {
			this.renamer.markClassAsDeobfuscated((ClassEntry) obfEntry);
		} else if (obfEntry instanceof FieldEntry) {
			this.renamer.markFieldAsDeobfuscated((FieldEntry) obfEntry);
		} else if (obfEntry instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry) obfEntry;
			if (methodEntry.isConstructor()) {
				throw new IllegalArgumentException("Cannot rename constructors");
			}
			this.renamer.markMethodTreeAsDeobfuscated(methodEntry);
		} else if (obfEntry instanceof LocalVariableEntry) {
			this.renamer.markArgumentAsDeobfuscated((LocalVariableEntry) obfEntry);
		} else {
			throw new Error("Unknown entry desc: " + obfEntry);
		}

		// clear caches
		this.translatorCache.clear();
	}

	public void changeModifier(Entry entry, Mappings.EntryModifier modifierEntry) {
		Entry obfEntry = obfuscateEntry(entry);
		if (obfEntry instanceof ClassEntry)
			this.renamer.setClassModifier((ClassEntry) obfEntry, modifierEntry);
		else if (obfEntry instanceof FieldEntry)
			this.renamer.setFieldModifier((FieldEntry) obfEntry, modifierEntry);
		else if (obfEntry instanceof MethodEntry)
			this.renamer.setMethodModifier((MethodEntry) obfEntry, modifierEntry);
		else
			throw new Error("Unknown entry desc: " + obfEntry);
	}

	public Mappings.EntryModifier getModifier(Entry obfEntry) {
		Entry entry = obfuscateEntry(obfEntry);
		if (entry != null)
			obfEntry = entry;
		if (obfEntry instanceof ClassEntry)
			return this.renamer.getClassModifier((ClassEntry) obfEntry);
		else if (obfEntry instanceof FieldEntry)
			return this.renamer.getFieldModifier((FieldEntry) obfEntry);
		else if (obfEntry instanceof MethodEntry)
			return this.renamer.getMethodModfifier((MethodEntry) obfEntry);
		else
			throw new Error("Unknown entry desc: " + obfEntry);
	}

	public static void runCustomTransforms(AstBuilder builder, DecompilerContext context){
		List<IAstTransform> transformers = Arrays.asList(
				new ObfuscatedEnumSwitchRewriterTransform(context),
				new VaragsFixer(context),
				new RemoveObjectCasts(context),
				new Java8Generics(),
				new InvalidIdentifierFix()
		);
		for (IAstTransform transform : transformers){
			transform.run(builder.getCompilationUnit());
		}
	}

	public interface ProgressListener {
		void init(int totalWork, String title);

		void onProgress(int numDone, String message);
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
