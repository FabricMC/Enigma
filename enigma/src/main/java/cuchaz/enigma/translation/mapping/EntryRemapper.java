package cuchaz.enigma.translation.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;

import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryRemapper {
	private final DeltaTrackingTree<EntryMapping> obfToDeobf;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;

	private final MappingValidator validator;

	private EntryRemapper(JarIndex jarIndex, EntryTree<EntryMapping> obfToDeobf) {
		this.obfToDeobf = new DeltaTrackingTree<>(obfToDeobf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, obfResolver);
		this.jarIndex = jarIndex;

		this.validator = new MappingValidator(obfToDeobf, deobfuscator, jarIndex);
	}

	public static EntryRemapper mapped(JarIndex index, EntryTree<EntryMapping> obfToDeobf) {
		return new EntryRemapper(index, obfToDeobf);
	}

	public static EntryRemapper empty(JarIndex index) {
		return new EntryRemapper(index, new HashEntryTree<>());
	}

	public <E extends Entry<?>> void mapFromObf(ValidationContext vc, E obfuscatedEntry, @Nullable EntryMapping deobfMapping) {
		mapFromObf(vc, obfuscatedEntry, deobfMapping, true);
	}

	public <E extends Entry<?>> void mapFromObf(ValidationContext vc, E obfuscatedEntry, @Nullable EntryMapping deobfMapping, boolean renaming) {
		mapFromObf(vc, obfuscatedEntry, deobfMapping, renaming, false);
	}

	public <E extends Entry<?>> void mapFromObf(ValidationContext vc, E obfuscatedEntry, @Nullable EntryMapping deobfMapping, boolean renaming, boolean validateOnly) {
		if (obfuscatedEntry instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry) obfuscatedEntry;
			ClassEntry classEntry = fieldEntry.getParent();

			mapRecordComponentGetter(vc, classEntry, fieldEntry, deobfMapping);
		}

		Collection<E> resolvedEntries = obfResolver.resolveEntry(obfuscatedEntry, renaming ? ResolutionStrategy.RESOLVE_ROOT : ResolutionStrategy.RESOLVE_CLOSEST);

		if (renaming && deobfMapping != null) {
			for (E resolvedEntry : resolvedEntries) {
				validator.validateRename(vc, resolvedEntry, deobfMapping.getTargetName());
			}
		}

		if (validateOnly || !vc.canProceed()) return;

		for (E resolvedEntry : resolvedEntries) {
			obfToDeobf.insert(resolvedEntry, deobfMapping);
		}
	}

	public void removeByObf(ValidationContext vc, Entry<?> obfuscatedEntry) {
		mapFromObf(vc, obfuscatedEntry, null);
	}

	// A little bit of a hack to also map the getter method for record fields/components.
	private void mapRecordComponentGetter(ValidationContext vc, ClassEntry classEntry, FieldEntry fieldEntry, EntryMapping fieldMapping) {
		if (!jarIndex.getEntryIndex().getClassAccess(classEntry).isRecord() || jarIndex.getEntryIndex().getFieldAccess(fieldEntry).isStatic()) {
			return;
		}

		// Find all the methods in this record class
		List<MethodEntry> classMethods = jarIndex.getEntryIndex().getMethods().stream()
				.filter(entry -> classEntry.equals(entry.getParent()))
				.collect(Collectors.toList());

		MethodEntry methodEntry = null;

		for (MethodEntry method : classMethods) {
			// Find the matching record component getter via matching the names. My understanding is this is safe, failing this it may need to be a bit more intelligent
			if (method.getName().equals(fieldEntry.getName()) && method.getDesc().toString().equals("()" + fieldEntry.getDesc())) {
				methodEntry = method;
				break;
			}
		}

		if (methodEntry == null && fieldMapping != null) {
			vc.raise(Message.UNKNOWN_RECORD_GETTER, fieldMapping.getTargetName());
			return;
		}

		mapFromObf(vc, methodEntry, fieldMapping != null ? new EntryMapping(fieldMapping.getTargetName()) : null);
	}

	@Nullable
	public EntryMapping getDeobfMapping(Entry<?> entry) {
		return obfToDeobf.get(entry);
	}

	public boolean hasDeobfMapping(Entry<?> obfEntry) {
		return obfToDeobf.contains(obfEntry);
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
}
