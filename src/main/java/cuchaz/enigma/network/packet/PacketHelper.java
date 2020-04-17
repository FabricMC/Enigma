package cuchaz.enigma.network.packet;

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PacketHelper {

	private static final int ENTRY_CLASS = 0, ENTRY_FIELD = 1, ENTRY_METHOD = 2, ENTRY_LOCAL_VAR = 3;

	public static Entry<?> readEntry(DataInput input) throws IOException {
		return readEntry(input, null, true);
	}

	public static Entry<?> readEntry(DataInput input, Entry<?> parent, boolean includeParent) throws IOException {
		int type = input.readUnsignedByte();

		if (includeParent && input.readBoolean()) {
			parent = readEntry(input, null, true);
		}

		String name = input.readUTF();

		String javadocs = null;
		if (input.readBoolean()) {
			javadocs = input.readUTF();
		}

		switch (type) {
		case ENTRY_CLASS: {
			if (parent != null && !(parent instanceof ClassEntry)) {
				throw new IOException("Class requires class parent");
			}
			return new ClassEntry((ClassEntry) parent, name, javadocs);
		}
		case ENTRY_FIELD: {
			if (!(parent instanceof ClassEntry)) {
				throw new IOException("Field requires class parent");
			}
			TypeDescriptor desc = new TypeDescriptor(input.readUTF());
			return new FieldEntry((ClassEntry) parent, name, desc, javadocs);
		}
		case ENTRY_METHOD: {
			if (!(parent instanceof ClassEntry)) {
				throw new IOException("Method requires class parent");
			}
			MethodDescriptor desc = new MethodDescriptor(input.readUTF());
			return new MethodEntry((ClassEntry) parent, name, desc, javadocs);
		}
		case ENTRY_LOCAL_VAR: {
			if (!(parent instanceof MethodEntry)) {
				throw new IOException("Local variable requires method parent");
			}
			int index = input.readUnsignedShort();
			boolean parameter = input.readBoolean();
			return new LocalVariableEntry((MethodEntry) parent, index, name, parameter, javadocs);
		}
		default: throw new IOException("Received unknown entry type " + type);
		}
	}

	public static void writeEntry(DataOutput output, Entry<?> entry) throws IOException {
		writeEntry(output, entry, true);
	}

	public static void writeEntry(DataOutput output, Entry<?> entry, boolean includeParent) throws IOException {
		// type
		if (entry instanceof ClassEntry) {
			output.writeByte(ENTRY_CLASS);
		} else if (entry instanceof FieldEntry) {
			output.writeByte(ENTRY_FIELD);
		} else if (entry instanceof MethodEntry) {
			output.writeByte(ENTRY_METHOD);
		} else if (entry instanceof LocalVariableEntry) {
			output.writeByte(ENTRY_LOCAL_VAR);
		} else {
			throw new IOException("Don't know how to serialize entry of type " + entry.getClass().getSimpleName());
		}

		// parent
		if (includeParent) {
			output.writeBoolean(entry.getParent() != null);
			if (entry.getParent() != null) {
				writeEntry(output, entry.getParent(), true);
			}
		}

		// name
		output.writeUTF(entry.getName());

		// javadocs
		output.writeBoolean(entry.getJavadocs() != null);
		if (entry.getJavadocs() != null) {
			output.writeUTF(entry.getJavadocs());
		}

		// type-specific stuff
		if (entry instanceof FieldEntry) {
			output.writeUTF(((FieldEntry) entry).getDesc().toString());
		} else if (entry instanceof MethodEntry) {
			output.writeUTF(((MethodEntry) entry).getDesc().toString());
		} else if (entry instanceof LocalVariableEntry) {
			LocalVariableEntry localVar = (LocalVariableEntry) entry;
			output.writeShort(localVar.getIndex());
			output.writeBoolean(localVar.isArgument());
		}
	}

}
