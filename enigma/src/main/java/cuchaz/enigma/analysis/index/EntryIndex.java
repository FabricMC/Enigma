package cuchaz.enigma.analysis.index;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.api.view.entry.ClassDefEntryView;
import cuchaz.enigma.api.view.entry.ClassEntryView;
import cuchaz.enigma.api.view.entry.EntryView;
import cuchaz.enigma.api.view.index.EntryIndexView;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class EntryIndex implements JarIndexer, EntryIndexView {
	private final ConcurrentMap<ClassEntry, AccessFlags> classes = new ConcurrentHashMap<>();
	private final ConcurrentMap<FieldEntry, AccessFlags> fields = new ConcurrentHashMap<>();
	private final ConcurrentMap<MethodEntry, AccessFlags> methods = new ConcurrentHashMap<>();
	private final ConcurrentMap<ClassEntry, ClassDefEntry> definitions = new ConcurrentHashMap<>();

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		definitions.put(classEntry, classEntry);
		classes.put(classEntry, classEntry.getAccess());
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
		return classes.containsKey(entry);
	}

	public boolean hasMethod(MethodEntry entry) {
		return methods.containsKey(entry);
	}

	public boolean hasField(FieldEntry entry) {
		return fields.containsKey(entry);
	}

	public boolean hasEntry(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			return hasClass(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return hasMethod(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return hasField(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			return hasMethod(localVariableEntry.getParent());
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
	public AccessFlags getClassAccess(ClassEntry entry) {
		return classes.get(entry);
	}

	@Nullable
	public AccessFlags getEntryAccess(Entry<?> entry) {
		if (entry instanceof MethodEntry methodEntry) {
			return getMethodAccess(methodEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			return getFieldAccess(fieldEntry);
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			return getMethodAccess(localVariableEntry.getParent());
		}

		return null;
	}

	public ClassDefEntry getDefinition(ClassEntry entry) {
		return definitions.get(entry);
	}

	@Override
	public Collection<ClassEntry> getClasses() {
		return classes.keySet();
	}

	public Collection<MethodEntry> getMethods() {
		return methods.keySet();
	}

	public Collection<FieldEntry> getFields() {
		return fields.keySet();
	}

	@Override
	public boolean hasEntry(EntryView entry) {
		if (entry instanceof ClassEntry) {
			return classes.containsKey(entry);
		} else if (entry instanceof FieldEntry) {
			return fields.containsKey(entry);
		} else if (entry instanceof MethodEntry) {
			return methods.containsKey(entry);
		} else {
			return false;
		}
	}

	@Override
	public int getAccess(EntryView entry) {
		AccessFlags access;

		if (entry instanceof ClassEntry classEntry) {
			access = getClassAccess(classEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			access = getFieldAccess(fieldEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			access = getMethodAccess(methodEntry);
		} else {
			return 0;
		}

		return access == null ? 0 : access.getFlags();
	}

	@Override
	public ClassDefEntryView getDefinition(ClassEntryView entry) {
		return getDefinition((ClassEntry) entry);
	}
}
