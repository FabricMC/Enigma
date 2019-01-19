package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;

public class EntryIndex implements JarIndexer, RemappableIndex {
	private final EntryTree<AccessFlags> entries = new HashEntryTree<>();

	@Override
	public EntryIndex remapped(Translator translator) {
		EntryIndex remapped = new EntryIndex();
		for (Entry<?> entry : entries.getAllEntries()) {
			remapped.entries.insert(translator.translate(entry), entries.get(entry));
		}

		return remapped;
	}

	@Override
	public void remapEntry(Entry<?> prevEntry, Entry<?> newEntry) {
		EntryTreeNode<AccessFlags> node = entries.findNode(prevEntry);
		if (node == null) {
			return;
		}

		for (EntryTreeNode<AccessFlags> child : node.getNodesRecursively()) {
			Entry<?> entry = child.getEntry();
			AccessFlags access = child.getValue();

			entries.remove(entry);
			entries.insert(entry.replaceAncestor(prevEntry, newEntry), access);
		}
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		entries.insert(classEntry, classEntry.getAccess());
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		entries.insert(methodEntry, methodEntry.getAccess());
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		entries.insert(fieldEntry, fieldEntry.getAccess());
	}

	public boolean hasClass(ClassEntry entry) {
		return entries.contains(entry);
	}

	public boolean hasMethod(MethodEntry entry) {
		return entries.contains(entry);
	}

	public boolean hasField(FieldEntry entry) {
		return entries.contains(entry);
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
		return entries.get(entry);
	}

	@Nullable
	public AccessFlags getFieldAccess(FieldEntry entry) {
		return entries.get(entry);
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
		return entries.getAllEntries().stream()
				.filter(entry -> entry instanceof ClassEntry)
				.map(entry -> (ClassEntry) entry)
				.collect(Collectors.toSet());
	}

	public Collection<MethodEntry> getMethods() {
		return entries.getAllEntries().stream()
				.filter(entry -> entry instanceof MethodEntry)
				.map(entry -> (MethodEntry) entry)
				.collect(Collectors.toSet());
	}

	public Collection<FieldEntry> getFields() {
		return entries.getAllEntries().stream()
				.filter(entry -> entry instanceof FieldEntry)
				.map(entry -> (FieldEntry) entry)
				.collect(Collectors.toSet());
	}
}
