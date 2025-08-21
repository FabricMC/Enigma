package cuchaz.enigma.bytecode.translators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cuchaz.enigma.translation.LocalNameGenerator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

public class LocalVariableFixVisitor extends ClassVisitor {
	private ClassDefEntry ownerEntry;

	public LocalVariableFixVisitor(int api, ClassVisitor visitor) {
		super(api, visitor);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		ownerEntry = ClassDefEntry.parse(access, name, signature, superName, interfaces);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodDefEntry methodEntry = MethodDefEntry.parse(ownerEntry, access, name, descriptor, signature);
		return new Method(api, methodEntry, super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class Method extends MethodVisitor {
		private final MethodDefEntry methodEntry;
		private final Map<Integer, String> parameterNames = new HashMap<>();
		private final Map<Integer, Integer> parameterIndices = new HashMap<>();
		private boolean hasParameterTable;
		private int parameterIndex = 0;

		Method(int api, MethodDefEntry methodEntry, MethodVisitor visitor) {
			super(api, visitor);
			this.methodEntry = methodEntry;

			int lvIndex = methodEntry.getAccess().isStatic() ? 0 : 1;
			List<TypeDescriptor> parameters = methodEntry.getDesc().getArgumentDescs();

			for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
				TypeDescriptor param = parameters.get(parameterIndex);
				parameterIndices.put(lvIndex, parameterIndex);
				lvIndex += param.getSize();
			}
		}

		@Override
		public void visitParameter(String name, int access) {
			hasParameterTable = true;
			super.visitParameter(fixParameterName(parameterIndex, name), fixParameterAccess(parameterIndex, access));
			parameterIndex++;
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			if (index == 0 && !methodEntry.getAccess().isStatic()) {
				name = "this";
			} else if (parameterIndices.containsKey(index)) {
				name = fixParameterName(parameterIndices.get(index), name);
			} else if (isInvalidName(name)) {
				name = LocalNameGenerator.generateLocalVariableName(index, new TypeDescriptor(desc));
			}

			super.visitLocalVariable(name, desc, signature, start, end, index);
		}

		private boolean isInvalidName(String name) {
			return name == null || name.isEmpty() || name.chars().anyMatch(ch -> ch < 0x20);
		}

		@Override
		public void visitEnd() {
			if (!hasParameterTable) {
				List<TypeDescriptor> arguments = methodEntry.getDesc().getArgumentDescs();

				for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
					super.visitParameter(fixParameterName(argumentIndex, null), fixParameterAccess(argumentIndex, 0));
				}
			}

			super.visitEnd();
		}

		private String fixParameterName(int index, String name) {
			if (parameterNames.get(index) != null) {
				return parameterNames.get(index); // to make sure that LVT names are consistent with parameter table names
			}

			if (isInvalidName(name)) {
				List<TypeDescriptor> arguments = methodEntry.getDesc().getArgumentDescs();
				name = LocalNameGenerator.generateArgumentName(index, arguments.get(index), arguments);
			}

			if (index == 0 && ownerEntry.getAccess().isEnum() && methodEntry.getName().equals("<init>")) {
				name = "name";
			}

			if (index == 1 && ownerEntry.getAccess().isEnum() && methodEntry.getName().equals("<init>")) {
				name = "ordinal";
			}

			parameterNames.put(index, name);
			return name;
		}

		private int fixParameterAccess(int index, int access) {
			if (index == 0 && ownerEntry.getAccess().isEnum() && methodEntry.getName().equals("<init>")) {
				access |= Opcodes.ACC_SYNTHETIC;
			}

			if (index == 1 && ownerEntry.getAccess().isEnum() && methodEntry.getName().equals("<init>")) {
				access |= Opcodes.ACC_SYNTHETIC;
			}

			return access;
		}
	}
}
