package cuchaz.enigma.analysis;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class IndexClassWriter extends ClassWriter {
	private final EntryIndex entryIndex;

	public IndexClassWriter(EntryIndex entryIndex, int flags) {
		super(flags);
		this.entryIndex = entryIndex;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		ClassInfo info1 = getClassInfo(type1);
		ClassInfo info2 = getClassInfo(type2);

		if (info1 == null || info2 == null) {
			return "java/lang/Object";
		}

		if (isAssignable(info1, info2)) {
			return type1;
		} else if (isAssignable(info2, info1)) {
			return type2;
		} else if (info1.isInterface() || info2.isInterface()) {
			return "java/lang/Object";
		} else {
			do {
				info1 = info1.getSuperClass();
			} while (info1 != null && !isAssignable(info1, info2));

			return info1 == null ? "java/lang/Object" : info1.getName();
		}
	}

	private boolean isAssignable(ClassInfo left, ClassInfo right) {
		String leftName = left.getName();

		while (right != null) {
			if (right.getName().equals(leftName)) {
				return true;
			}

			right = right.getSuperClass();
		}

		return false;
	}

	@Nullable
	private ClassInfo getClassInfo(String internalName) {
		ClassDefEntry defEntry = entryIndex.getDefinition(new ClassEntry(internalName));

		if (defEntry != null) {
			return new ClassDefEntryInfo(defEntry);
		}

		Class<?> clazz;

		try {
			clazz = Class.forName(internalName.replace('/', '.'), false, getClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}

		return new ReflectionClassInfo(clazz);
	}

	private interface ClassInfo {
		String getName();

		@Nullable
		ClassInfo getSuperClass();

		boolean isInterface();
	}

	private class ClassDefEntryInfo implements ClassInfo {
		private final ClassDefEntry entry;

		private ClassDefEntryInfo(ClassDefEntry entry) {
			this.entry = entry;
		}

		@Override
		public String getName() {
			return entry.getFullName();
		}

		@Override
		@Nullable
		public ClassInfo getSuperClass() {
			ClassEntry superClass = entry.getSuperClass();

			if (superClass == null) {
				return null;
			}

			return getClassInfo(superClass.getFullName());
		}

		@Override
		public boolean isInterface() {
			return entry.getAccess().isInterface();
		}
	}

	private record ReflectionClassInfo(Class<?> clazz) implements ClassInfo {
		@Override
		public String getName() {
			return Type.getInternalName(clazz);
		}

		@Override
		@Nullable
		public ClassInfo getSuperClass() {
			Class<?> superClass = clazz.isInterface() ? Object.class : clazz.getSuperclass();
			return superClass == null ? null : new ReflectionClassInfo(superClass);
		}

		@Override
		public boolean isInterface() {
			return clazz.isInterface();
		}
	}
}
