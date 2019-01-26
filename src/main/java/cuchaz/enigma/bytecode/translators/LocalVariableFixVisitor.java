package cuchaz.enigma.bytecode.translators;

import com.google.common.base.CharMatcher;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

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
		private boolean hasLvt;

		Method(int api, MethodDefEntry methodEntry, MethodVisitor visitor) {
			super(api, visitor);
			this.methodEntry = methodEntry;
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			hasLvt = true;

			String translatedName = name;

			if (isInvalidName(name)) {
				int argumentIndex = methodEntry.getArgumentIndex(ownerEntry, index);

				if (argumentIndex >= 0) {
					List<TypeDescriptor> arguments = methodEntry.getDesc().getArgumentDescs();
					boolean argument = argumentIndex < arguments.size();
					if (argument) {
						translatedName = "arg" + (argumentIndex + 1);
					} else {
						translatedName = "var" + (argumentIndex + 1);
					}
				}
			}

			super.visitLocalVariable(translatedName, desc, signature, start, end, index);
		}

		private boolean isInvalidName(String name) {
			return !CharMatcher.ascii().matchesAllOf(name);
		}

		@Override
		public void visitEnd() {
			if (!hasLvt) {
				List<TypeDescriptor> arguments = methodEntry.getDesc().getArgumentDescs();
				for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
					super.visitParameter("arg" + (argumentIndex + 1), 0);
				}
			}

			super.visitEnd();
		}
	}
}
