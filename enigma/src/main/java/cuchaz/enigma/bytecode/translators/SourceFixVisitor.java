package cuchaz.enigma.bytecode.translators;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

public class SourceFixVisitor extends ClassVisitor {
	private final JarIndex index;
	private ClassDefEntry ownerEntry;

	public SourceFixVisitor(int api, ClassVisitor visitor, JarIndex index) {
		super(api, visitor);
		this.index = index;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		ownerEntry = ClassDefEntry.parse(access, name, signature, superName, interfaces);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodDefEntry methodEntry = MethodDefEntry.parse(ownerEntry, access, name, descriptor, signature);

		BridgeMethodIndex bridgeIndex = index.getBridgeMethodIndex();

		if (bridgeIndex.isBridgeMethod(methodEntry)) {
			access |= Opcodes.ACC_BRIDGE;
		} else if (bridgeIndex.isSpecializedMethod(methodEntry)) {
			name = bridgeIndex.getBridgeFromSpecialized(methodEntry).getName();
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
