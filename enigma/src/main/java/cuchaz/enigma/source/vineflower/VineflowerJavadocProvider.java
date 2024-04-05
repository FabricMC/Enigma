package cuchaz.enigma.source.vineflower;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.StructRecordComponent;
import org.objectweb.asm.Opcodes;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

class VineflowerJavadocProvider implements IFabricJavadocProvider {
	private final EntryRemapper remapper;

	VineflowerJavadocProvider(EntryRemapper remapper) {
		this.remapper = remapper;
	}

	@Override
	public String getClassDoc(StructClass cls) {
		if (remapper == null) return null;

		List<String> recordComponentDocs = new LinkedList<>();

		if (isRecord(cls)) {
			for (StructRecordComponent component : cls.getRecordComponents()) {
				EntryMapping mapping = remapper.getDeobfMapping(fieldEntryOf(cls, component));
				String javadoc = mapping.javadoc();

				if (javadoc != null) {
					recordComponentDocs.add(String.format("@param %s %s", mapping.targetName(), javadoc));
				}
			}
		}

		EntryMapping mapping = remapper.getDeobfMapping(classEntryOf(cls));
		StringBuilder builder = new StringBuilder();
		String javadoc = mapping.javadoc();

		if (javadoc != null) {
			builder.append(javadoc);
		}

		if (!recordComponentDocs.isEmpty()) {
			if (javadoc != null) {
				builder.append('\n');
			}

			for (String recordComponentDoc : recordComponentDocs) {
				builder.append('\n').append(recordComponentDoc);
			}
		}

		javadoc = builder.toString();

		return javadoc.isBlank() ? null : javadoc.trim();
	}

	@Override
	public String getFieldDoc(StructClass cls, StructField fld) {
		boolean isRecordComponent = isRecord(cls) && !fld.hasModifier(Opcodes.ACC_STATIC);

		if (remapper == null || isRecordComponent) {
			return null;
		}

		EntryMapping mapping = remapper.getDeobfMapping(fieldEntryOf(cls, fld));
		String javadoc = mapping.javadoc();

		return javadoc == null || javadoc.isBlank() ? null : javadoc.trim();
	}

	@Override
	public String getMethodDoc(StructClass cls, StructMethod mth) {
		if (remapper == null) return null;

		MethodEntry entry = methodEntryOf(cls, mth);
		EntryMapping mapping = remapper.getDeobfMapping(entry);
		StringBuilder builder = new StringBuilder();
		String javadoc = mapping.javadoc();

		if (javadoc != null) {
			builder.append(javadoc);
		}

		Collection<Entry<?>> children = remapper.getObfChildren(entry);
		boolean addedLf = false;

		if (children != null && !children.isEmpty()) {
			for (Entry<?> each : children) {
				if (each instanceof LocalVariableEntry) {
					mapping = remapper.getDeobfMapping(each);
					javadoc = mapping.javadoc();

					if (javadoc != null) {
						if (!addedLf) {
							addedLf = true;
							builder.append('\n');
						}

						builder.append(String.format("\n@param %s %s", mapping.targetName(), javadoc));
					}
				}
			}
		}

		javadoc = builder.toString();

		return javadoc == null || javadoc.isBlank() ? null : javadoc.trim();
	}

	private boolean isRecord(StructClass cls) {
		if (cls.superClass == null) return false;

		return cls.superClass.getString().equals("java/lang/Record");
	}

	private ClassEntry classEntryOf(StructClass cls) {
		return ClassEntry.parse(cls.qualifiedName);
	}

	private FieldEntry fieldEntryOf(StructClass cls, StructField fld) {
		return FieldEntry.parse(cls.qualifiedName, fld.getName(), fld.getDescriptor());
	}

	private MethodEntry methodEntryOf(StructClass cls, StructMethod mth) {
		return MethodEntry.parse(cls.qualifiedName, mth.getName(), mth.getDescriptor());
	}
}
