package cuchaz.enigma.source.procyon;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

public class EntryParser {
	public static FieldDefEntry parse(FieldDefinition definition) {
		ClassEntry owner = parse(definition.getDeclaringType());
		TypeDescriptor descriptor = new TypeDescriptor(definition.getErasedSignature());
		Signature signature = Signature.createTypedSignature(definition.getSignature());
		AccessFlags access = new AccessFlags(definition.getModifiers());
		return new FieldDefEntry(owner, definition.getName(), descriptor, signature, access, null);
	}

	public static ClassDefEntry parse(TypeDefinition def) {
		String name = def.getInternalName();
		Signature signature = Signature.createSignature(def.getSignature());
		AccessFlags access = new AccessFlags(def.getModifiers());
		ClassEntry superClass = def.getBaseType() != null ? parse(def.getBaseType()) : null;
		ClassEntry[] interfaces = def.getExplicitInterfaces().stream().map(EntryParser::parse).toArray(ClassEntry[]::new);
		return new ClassDefEntry(name, signature, access, superClass, interfaces);
	}

	public static ClassEntry parse(TypeReference typeReference) {
		return new ClassEntry(typeReference.getInternalName());
	}

	public static MethodDefEntry parse(MethodDefinition definition) {
		ClassEntry classEntry = parse(definition.getDeclaringType());
		MethodDescriptor descriptor = new MethodDescriptor(definition.getErasedSignature());
		Signature signature = Signature.createSignature(definition.getSignature());
		AccessFlags access = new AccessFlags(definition.getModifiers());
		return new MethodDefEntry(classEntry, definition.getName(), descriptor, signature, access, null);
	}

	public static TypeDescriptor parseTypeDescriptor(TypeReference type) {
		return new TypeDescriptor(type.getErasedSignature());
	}
}
