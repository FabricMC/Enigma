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
	private Map<ClassEntry, ClassDefEntry> definitions = new HashMap<>();

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
