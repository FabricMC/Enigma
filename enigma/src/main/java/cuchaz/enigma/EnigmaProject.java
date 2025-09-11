package cuchaz.enigma;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.DataInvalidationEvent;
import cuchaz.enigma.api.DataInvalidationListener;
import cuchaz.enigma.api.service.DecompilerInputTransformerService;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.api.view.ProjectView;
import cuchaz.enigma.api.view.entry.EntryView;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.classprovider.ObfuscationFixClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.ObfuscatingTranslator;
import cuchaz.enigma.translation.ProposingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.MappingsChecker;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class EnigmaProject implements ProjectView {
	private final Enigma enigma;

	private final List<Path> jarPaths;
	private final ClassProvider classProvider;
	private final JarIndex jarIndex;
	private final byte[] jarChecksum;
	private final Set<String> projectClasses;

	private EntryRemapper mapper;
	private Translator proposingTranslator;
	@Nullable
	private ObfuscatingTranslator inverseTranslator;

	private final List<DataInvalidationListener> dataInvalidationListeners = new ArrayList<>();

	public EnigmaProject(Enigma enigma, List<Path> jarPaths, ClassProvider classProvider, Set<String> projectClasses, JarIndex jarIndex, byte[] jarChecksum) {
		if (jarChecksum.length != 20) {
			throw new IllegalArgumentException();
		}

		this.enigma = enigma;
		this.jarPaths = List.copyOf(jarPaths);
		this.classProvider = classProvider;
		this.jarIndex = jarIndex;
		this.jarChecksum = jarChecksum;
		this.projectClasses = projectClasses;

		setMappings(null);
	}

	public void setMappings(EntryTree<EntryMapping> mappings) {
		if (mappings != null) {
			mapper = EntryRemapper.mapped(jarIndex, mappings);
		} else {
			mapper = EntryRemapper.empty(jarIndex);
		}

		NameProposalService[] nameProposalServices = enigma.getServices().get(NameProposalService.TYPE).toArray(new NameProposalService[0]);
		proposingTranslator = nameProposalServices.length == 0 ? mapper.getDeobfuscator() : new ProposingTranslator(mapper, nameProposalServices);

		if (inverseTranslator != null) {
			inverseTranslator.refreshAll(proposingTranslator);
		}
	}

	public Enigma getEnigma() {
		return enigma;
	}

	public List<Path> getJarPaths() {
		return jarPaths;
	}

	public ClassProvider getClassProvider() {
		return classProvider;
	}

	public JarIndex getJarIndex() {
		return jarIndex;
	}

	public byte[] getJarChecksum() {
		return jarChecksum;
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

		if (inverseTranslator != null) {
			inverseTranslator.refreshAll(proposingTranslator);
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
		if (obfEntry instanceof MethodEntry obfMethodEntry) {
			// HACKHACK: Object methods are not obfuscated identifiers
			String name = obfMethodEntry.getName();
			String sig = obfMethodEntry.getDesc().toString();

			//TODO replace with a map or check if declaring class is java.lang.Object
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
			} else {
				ClassDefEntry parent = jarIndex.getEntryIndex().getDefinition(obfMethodEntry.getParent());

				if (parent != null && parent.getSuperClass() != null && parent.getSuperClass().getFullName().equals("java/lang/Enum")) {
					if (name.equals("values") && sig.equals("()[L" + parent.getFullName() + ";")) {
						return false;
					} else if (name.equals("valueOf") && sig.equals("(Ljava/lang/String;)L" + parent.getFullName() + ";")) {
						return false;
					}
				}
			}
		} else if (obfEntry instanceof LocalVariableEntry && !((LocalVariableEntry) obfEntry).isArgument()) {
			return false;
		}

		return this.jarIndex.getEntryIndex().hasEntry(obfEntry);
	}

	public boolean isRenamable(EntryReference<Entry<?>, Entry<?>> obfReference) {
		return obfReference.isNamed() && isRenamable(obfReference.getNameableEntry());
	}

	public boolean isObfuscated(Entry<?> entry) {
		String name = entry.getName();

		List<ObfuscationTestService> obfuscationTestServices = this.getEnigma().getServices().get(ObfuscationTestService.TYPE);

		if (!obfuscationTestServices.isEmpty()) {
			for (ObfuscationTestService service : obfuscationTestServices) {
				if (service.testDeobfuscated(entry)) {
					return false;
				}
			}
		}

		List<NameProposalService> nameProposalServices = this.getEnigma().getServices().get(NameProposalService.TYPE);

		if (!nameProposalServices.isEmpty()) {
			for (NameProposalService service : nameProposalServices) {
				if (service.proposeName(entry, mapper).isPresent()) {
					return false;
				}
			}
		}

		String mappedName = mapper.deobfuscate(entry).getName();

		if (mappedName != null && !mappedName.isEmpty() && !mappedName.equals(name)) {
			return false;
		}

		return true;
	}

	public JarExport exportRemappedJar(ProgressListener progress) {
		Collection<ClassEntry> classEntries = jarIndex.getEntryIndex().getClasses();
		ClassProvider fixingClassProvider = new ObfuscationFixClassProvider(classProvider, jarIndex);

		AtomicInteger count = new AtomicInteger();
		progress.init(classEntries.size(), I18n.translate("progress.classes.deobfuscating"));

		Map<String, ClassNode> compiled = classEntries.parallelStream().map(entry -> {
			ClassEntry translatedEntry = proposingTranslator.translate(entry);
			progress.step(count.getAndIncrement(), translatedEntry.toString());

			ClassNode node = fixingClassProvider.get(entry.getFullName());

			if (node != null) {
				ClassNode translatedNode = new ClassNode();
				node.accept(new TranslationClassVisitor(proposingTranslator, Enigma.ASM_VERSION, translatedNode));
				return translatedNode;
			}

			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(n -> n.name, Function.identity()));

		return new JarExport(mapper, compiled);
	}

	public static final class JarExport {
		private final EntryRemapper mapper;
		private final Map<String, ClassNode> compiled;

		JarExport(EntryRemapper mapper, Map<String, ClassNode> compiled) {
			this.mapper = mapper;
			this.compiled = compiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(this.compiled.size(), I18n.translate("progress.jar.writing"));

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

		public SourceExport decompile(EnigmaProject project, ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy) {
			List<ClassSource> decompiled = this.decompileStream(project, progress, decompilerService, errorStrategy).toList();
			return new SourceExport(decompiled);
		}

		public Stream<ClassSource> decompileStream(EnigmaProject project, ProgressListener progress, DecompilerService decompilerService, DecompileErrorStrategy errorStrategy) {
			Collection<ClassNode> classes = this.compiled.values().stream()
					.filter(classNode -> classNode.name.indexOf('$') == -1)
					.peek(classNode -> {
						for (DecompilerInputTransformerService transformer : project.enigma.getServices().get(DecompilerInputTransformerService.TYPE)) {
							transformer.transform(classNode);
						}
					})
					.toList();

			progress.init(classes.size(), I18n.translate("progress.classes.decompiling"));

			//create a common instance outside the loop as mappings shouldn't be changing while this is happening
			Decompiler decompiler = decompilerService.create(new ClassProvider() {
				@Override
				public Collection<String> getClassNames() {
					return compiled.keySet();
				}

				@Override
				public ClassNode get(String name) {
					return compiled.get(name);
				}
			}, new SourceSettings(false, false));

			AtomicInteger count = new AtomicInteger();

			return classes.parallelStream().map(translatedNode -> {
				progress.step(count.getAndIncrement(), translatedNode.name);

				String source = null;

				try {
					source = decompileClass(translatedNode, decompiler);
				} catch (Throwable throwable) {
					switch (errorStrategy) {
					case PROPAGATE:
						throw throwable;
					case IGNORE:
						break;
					case TRACE_AS_SOURCE: {
						StringWriter writer = new StringWriter();
						throwable.printStackTrace(new PrintWriter(writer));
						source = writer.toString();
						break;
					}
					}
				}

				if (source == null) {
					return null;
				}

				return new ClassSource(translatedNode.name, source);
			}).filter(Objects::nonNull);
		}

		private String decompileClass(ClassNode translatedNode, Decompiler decompiler) {
			return decompiler.getSource(translatedNode.name, mapper).asString();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends EntryView> T deobfuscate(T entry) {
		return (T) proposingTranslator.extendedTranslate((Translatable) entry).getValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends EntryView> T obfuscate(T entry) {
		if (inverseTranslator == null) {
			throw new IllegalStateException("Must call registerForInverseMappings before calling obfuscate");
		}

		return (T) inverseTranslator.extendedTranslate((Entry<?>) entry).getValue();
	}

	@Override
	public void registerForInverseMappings() {
		if (inverseTranslator == null) {
			inverseTranslator = new ObfuscatingTranslator(jarIndex);
			inverseTranslator.refreshAll(proposingTranslator);
		}
	}

	public void onEntryChange(EntryMapping prevMapping, EntryChange<?> change) {
		if (inverseTranslator == null || change.getDeobfName().isUnchanged()) {
			return;
		}

		String newName = change.getDeobfName().isSet() ? change.getDeobfName().getNewValue() : proposingTranslator.extendedTranslate(change.getTarget()).getValue().getName();

		for (Entry<?> equivalentEntry : mapper.getObfResolver().resolveEquivalentEntries(change.getTarget())) {
			inverseTranslator.refreshName(equivalentEntry, prevMapping.targetName(), newName);
		}
	}

	@Override
	public Collection<String> getProjectClasses() {
		return projectClasses;
	}

	@Override
	@Nullable
	public ClassNode getBytecode(String className) {
		return classProvider.get(className);
	}

	@Override
	public void addDataInvalidationListener(DataInvalidationListener listener) {
		dataInvalidationListeners.add(listener);
	}

	@Override
	public void invalidateData(@Nullable Collection<String> classes, DataInvalidationEvent.InvalidationType type) {
		DataInvalidationEvent event = new DataInvalidationEvent() {
			@Override
			@Nullable
			public Collection<String> getClasses() {
				return classes;
			}

			@Override
			public InvalidationType getType() {
				return type;
			}
		};

		dataInvalidationListeners.forEach(listener -> listener.onDataInvalidated(event));
	}

	public static final class SourceExport {
		public final Collection<ClassSource> decompiled;

		SourceExport(Collection<ClassSource> decompiled) {
			this.decompiled = decompiled;
		}

		public void write(Path path, ProgressListener progress) throws IOException {
			progress.init(decompiled.size(), I18n.translate("progress.sources.writing"));

			int count = 0;

			for (ClassSource source : decompiled) {
				progress.step(count++, source.name);

				Path sourcePath = source.resolvePath(path);
				source.writeTo(sourcePath);
			}
		}
	}

	public static class ClassSource {
		public final String name;
		public final String source;

		ClassSource(String name, String source) {
			this.name = name;
			this.source = source;
		}

		public void writeTo(Path path) throws IOException {
			Files.createDirectories(path.getParent());

			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write(source);
			}
		}

		public Path resolvePath(Path root) {
			return root.resolve(name.replace('.', '/') + ".java");
		}
	}

	public enum DecompileErrorStrategy {
		PROPAGATE,
		TRACE_AS_SOURCE,
		IGNORE
	}
}
