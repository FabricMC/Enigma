package cuchaz.enigma.mapping;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class JavassistUtil {
	
	public static ClassEntry getClassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getName()));
	}
	
	public static ClassEntry getSuperclassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getClassFile().getSuperclass()));
	}
	
	public static MethodEntry getMethodEntry(CtMethod method) {
		return new MethodEntry(
			getClassEntry(method.getDeclaringClass()),
			method.getName(),
			method.getMethodInfo().getDescriptor()
		);
	}
	
	public static ConstructorEntry getConstructorEntry(CtConstructor constructor) {
		return new ConstructorEntry(
			getClassEntry(constructor.getDeclaringClass()),
			constructor.getMethodInfo().getDescriptor()
		);
	}
	
	public static BehaviorEntry getBehaviorEntry(CtBehavior behavior) {
		if (behavior instanceof CtMethod) {
			return getMethodEntry((CtMethod)behavior);
		} else if (behavior instanceof CtConstructor) {
			return getConstructorEntry((CtConstructor)behavior);
		}
		throw new Error("behavior is neither Method nor Constructor!");
	}
	
	public static FieldEntry getFieldEntry(CtField field) {
		return new FieldEntry(
			getClassEntry(field.getDeclaringClass()),
			field.getName()
		);
	}
}
