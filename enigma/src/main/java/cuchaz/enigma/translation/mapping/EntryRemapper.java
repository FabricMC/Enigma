package cuchaz.enigma.translation.mapping;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryRemapper {
	private final DeltaTrackingTree<EntryMapping> obfToDeobf;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;
	private final ClassProvider classProvider;

	private final MappingValidator validator;

	private EntryRemapper(JarIndex jarIndex, EntryTree<EntryMapping> obfToDeobf, ClassProvider classProvider) {
		this.obfToDeobf = new DeltaTrackingTree<>(obfToDeobf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, obfResolver);
		this.jarIndex = jarIndex;
		this.classProvider = classProvider;

		this.validator = new MappingValidator(obfToDeobf, deobfuscator, jarIndex);
	}

	public static EntryRemapper mapped(JarIndex index, EntryTree<EntryMapping> obfToDeobf, ClassProvider classProvider) {
		return new EntryRemapper(index, obfToDeobf, classProvider);
	}

	public static EntryRemapper empty(JarIndex index, ClassProvider classProvider) {
		return new EntryRemapper(index, new HashEntryTree<>(), classProvider);
	}

	public void validatePutMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping) {
		doPutMapping(vc, obfuscatedEntry, deobfMapping, true);
	}

	public void putMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping) {
		doPutMapping(vc, obfuscatedEntry, deobfMapping, false);
	}

	private void doPutMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping, boolean validateOnly) {
		if (obfuscatedEntry instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry) obfuscatedEntry;
			ClassEntry classEntry = fieldEntry.getParent();

			if (jarIndex.getEntryIndex().getDefinition(classEntry).isRecord()) {
				mapRecordComponentGetter(vc, classEntry, fieldEntry, deobfMapping);
				mapRecordCanonicalConstructor(vc, classEntry, fieldEntry, deobfMapping);
			}
		}

		boolean renaming = !Objects.equals(getDeobfMapping(obfuscatedEntry).targetName(), deobfMapping.targetName());

		Collection<Entry<?>> resolvedEntries = obfResolver.resolveEntry(obfuscatedEntry, renaming ? ResolutionStrategy.RESOLVE_ROOT : ResolutionStrategy.RESOLVE_CLOSEST);

		if (renaming && deobfMapping.targetName() != null) {
			for (Entry<?> resolvedEntry : resolvedEntries) {
				validator.validateRename(vc, resolvedEntry, deobfMapping.targetName());
			}
		}

		if (validateOnly || !vc.canProceed()) {
			return;
		}

		for (Entry<?> resolvedEntry : resolvedEntries) {
			if (deobfMapping.equals(EntryMapping.DEFAULT)) {
				obfToDeobf.insert(resolvedEntry, null);
			} else {
				obfToDeobf.insert(resolvedEntry, deobfMapping);
			}
		}
	}

	// A little bit of a hack to also map the getter method for record fields.
	private void mapRecordComponentGetter(ValidationContext vc, ClassEntry classEntry, FieldEntry fieldEntry, EntryMapping fieldMapping) {
		if (jarIndex.getEntryIndex().getFieldAccess(fieldEntry).isStatic()) {
			return;
		}

		// Find all the methods in this record class
		List<MethodEntry> classMethods = jarIndex.getEntryIndex().getMethods().stream().filter(entry -> classEntry.equals(entry.getParent())).toList();

		MethodEntry methodEntry = null;

		for (MethodEntry method : classMethods) {
			// Find the matching record component getter via matching the names. TODO: Support when the record field and method names do not match
			if (method.getName().equals(fieldEntry.getName()) && method.getDesc().toString().equals("()" + fieldEntry.getDesc())) {
				methodEntry = method;
				break;
			}
		}

		if (methodEntry == null && fieldMapping != null) {
			vc.raise(Message.UNKNOWN_RECORD_GETTER, fieldMapping.targetName());
			return;
		}

		// Also remap the associated method, without the javadoc.
		doPutMapping(vc, methodEntry, new EntryMapping(fieldMapping.targetName()), false);
	}

	// Map a record's canonical constructor params
	private void mapRecordCanonicalConstructor(ValidationContext vc, ClassEntry classEntry, FieldEntry fieldEntry, EntryMapping fieldMapping) {
		final ClassNode classNode = classProvider.get(classEntry.getName());

		final List<FieldNode> recordFields = classNode.fields.stream()
				.filter(fieldNode -> !Modifier.isStatic(fieldNode.access))
				.toList();

		assert !recordFields.isEmpty();

		final String recordDesc = "(%s)V".formatted(
				recordFields.stream()
						.map(entry -> entry.desc.toString())
						.collect(Collectors.joining())
		);

		final MethodEntry canonicalConstructor = new MethodEntry(classEntry, "<init>", new MethodDescriptor(recordDesc));

		if (!jarIndex.getEntryIndex().hasMethod(canonicalConstructor)) {
			// Record does not have a canonicalConstructor
			return;
		}

		// Compute the LVT index
		int lvtIndex = 0;

		for (FieldNode recordField : recordFields) {
			lvtIndex += Type.getType(recordField.desc).getSize();

			if (recordField.name.equals(fieldEntry.getName()) && recordField.desc.equals(fieldEntry.getDesc().toString())) {
				break;
			}
		}

		final String name = fieldMapping.targetName();
		final LocalVariableEntry localVariableEntry = new LocalVariableEntry(canonicalConstructor, lvtIndex, name, true, null);

		doPutMapping(vc, localVariableEntry, new EntryMapping(name), false);
	}

	@Nonnull
	public EntryMapping getDeobfMapping(Entry<?> entry) {
		EntryMapping entryMapping = obfToDeobf.get(entry);
		return entryMapping == null ? EntryMapping.DEFAULT : entryMapping;
	}

	public <T extends Translatable> TranslateResult<T> extendedDeobfuscate(T translatable) {
		return deobfuscator.extendedTranslate(translatable);
	}

	public <T extends Translatable> T deobfuscate(T translatable) {
		return deobfuscator.translate(translatable);
	}

	public Translator getDeobfuscator() {
		return deobfuscator;
	}

	public Stream<Entry<?>> getObfEntries() {
		return obfToDeobf.getAllEntries();
	}

	public Collection<Entry<?>> getObfChildren(Entry<?> obfuscatedEntry) {
		return obfToDeobf.getChildren(obfuscatedEntry);
	}

	public DeltaTrackingTree<EntryMapping> getObfToDeobf() {
		return obfToDeobf;
	}

	public MappingDelta<EntryMapping> takeMappingDelta() {
		return obfToDeobf.takeDelta();
	}

	public boolean isDirty() {
		return obfToDeobf.isDirty();
	}

	public EntryResolver getObfResolver() {
		return obfResolver;
	}

	public MappingValidator getValidator() {
		return validator;
	}
}
