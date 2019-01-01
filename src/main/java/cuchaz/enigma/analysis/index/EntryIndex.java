package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.*;

import javax.annotation.Nullable;
import java.util.*;

public class EntryIndex implements JarIndexer, RemappableIndex {
	private final Set<ClassEntry> classes = new HashSet<>();
	private final Map<MethodEntry, AccessFlags> methods = new HashMap<>();
	private final Map<FieldEntry, AccessFlags> fields = new HashMap<>();

	@Override
	public EntryIndex remapped(Translator translator) {
		EntryIndex remapped = new EntryIndex();

		for (ClassEntry classEntry : classes) {
			remapped.classes.add(translator.translate(classEntry));
		}
		for (Map.Entry<MethodEntry, AccessFlags> entry : methods.entrySet()) {
			remapped.methods.put(translator.translate(entry.getKey()), entry.getValue());
		}
		for (Map.Entry<FieldEntry, AccessFlags> entry : fields.entrySet()) {
			remapped.fields.put(translator.translate(entry.getKey()), entry.getValue());
		}

		return remapped;
	}

	@Override
	public void remapEntry(Entry<?> entry, Entry<?> newEntry) {
		if (entry instanceof ClassEntry) {
			classes.remove(entry);
			classes.add((ClassEntry) newEntry);
		} else if (entry instanceof MethodEntry) {
			AccessFlags access = methods.remove(entry);
			if (access != null) {
				methods.put((MethodEntry) newEntry, access);
			}
		} else if (entry instanceof FieldEntry) {
			AccessFlags access = fields.remove(entry);
			if (access != null) {
				fields.put((FieldEntry) newEntry, access);
			}
		}
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		classes.add(classEntry);
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		methods.put(methodEntry, methodEntry.getAccess());
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		fields.put(fieldEntry, fieldEntry.getAccess());
	}

	public boolean hasClass(ClassEntry entry) {
		return classes.contains(entry);
	}

	public boolean hasMethod(MethodEntry entry) {
		return methods.containsKey(entry);
	}

	public boolean hasField(FieldEntry entry) {
		return fields.containsKey(entry);
	}

	public boolean hasEntry(Entry<?> entry) {
		if (entry instanceof ClassEntry) {
			return hasClass((ClassEntry) entry);
		} else if (entry instanceof MethodEntry) {
			return hasMethod((MethodEntry) entry);
		} else if (entry instanceof FieldEntry) {
			return hasField((FieldEntry) entry);
		} else if (entry instanceof LocalVariableEntry) {
			return hasMethod(((LocalVariableEntry) entry).getParent());
		}

		return false;
	}

	@Nullable
	public AccessFlags getMethodAccess(MethodEntry entry) {
		return methods.get(entry);
	}

	@Nullable
	public AccessFlags getFieldAccess(FieldEntry entry) {
		return fields.get(entry);
	}

	@Nullable
	public AccessFlags getEntryAccess(Entry<?> entry) {
		if (entry instanceof MethodEntry) {
			return getMethodAccess((MethodEntry) entry);
		} else if (entry instanceof FieldEntry) {
			return getFieldAccess((FieldEntry) entry);
		} else if (entry instanceof LocalVariableEntry) {
			return getMethodAccess(((LocalVariableEntry) entry).getParent());
		}

		return null;
	}

	public Collection<ClassEntry> getClasses() {
		return Collections.unmodifiableCollection(classes);
	}

	public Collection<MethodEntry> getMethods() {
		return methods.keySet();
	}

	public Collection<FieldEntry> getFields() {
		return fields.keySet();
	}
}
