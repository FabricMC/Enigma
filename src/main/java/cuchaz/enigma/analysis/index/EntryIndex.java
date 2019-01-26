package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntryIndex implements JarIndexer {
	private Map<ClassEntry, AccessFlags> classes = new HashMap<>();
	private Map<FieldEntry, AccessFlags> fields = new HashMap<>();
	private Map<MethodEntry, AccessFlags> methods = new HashMap<>();

	@Override
	public void indexClass(ClassDefEntry classEntry) {
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
		return classes.keySet();
	}

	public Collection<MethodEntry> getMethods() {
		return methods.keySet();
	}

	public Collection<FieldEntry> getFields() {
		return fields.keySet();
	}
}
