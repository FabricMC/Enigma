package cuchaz.enigma.mapping;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.expr.ConstructorCall;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

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
			new Signature(method.getMethodInfo().getDescriptor())
		);
	}
	
	public static MethodEntry getMethodEntry(MethodCall call) {
		return new MethodEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			call.getMethodName(),
			new Signature(call.getSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(CtConstructor constructor) {
		return new ConstructorEntry(
			getClassEntry(constructor.getDeclaringClass()),
			new Signature(constructor.getMethodInfo().getDescriptor())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(ConstructorCall call) {
		return new ConstructorEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			new Signature(call.getSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(NewExpr call) {
		return new ConstructorEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			new Signature(call.getSignature())
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
	
	public static FieldEntry getFieldEntry(FieldAccess call) {
		return new FieldEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			call.getFieldName()
		);
	}
}
