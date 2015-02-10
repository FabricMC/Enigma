package cuchaz.enigma.mapping;

import java.util.List;

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

import com.beust.jcommander.internal.Lists;
import com.strobel.assembler.metadata.MethodDefinition;

import cuchaz.enigma.analysis.JarIndex;

public class EntryFactory {
	
	public static ClassEntry getClassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getName()));
	}
	
	public static ClassEntry getObfClassEntry(JarIndex jarIndex, ClassMapping classMapping) {
		return new ClassEntry(getChainedOuterClassName(jarIndex, classMapping.getObfName()));
	}
	
	private static String getChainedOuterClassName(JarIndex jarIndex, String obfClassName) {
		
		// lookup the chain of outer classes
		List<String> obfOuterClassNames = Lists.newArrayList();
		String checkName = obfClassName;
		while (true) {
			
			// if this class name has a package, then it can't be an inner class
			if (!new ClassEntry(checkName).isInDefaultPackage()) {
				break;
			}
			
			String obfOuterClassName = jarIndex.getOuterClass(checkName);
			if (obfOuterClassName != null) {
				obfOuterClassNames.add(obfOuterClassName);
				checkName = obfOuterClassName;
			} else {
				break;
			}
		}
		
		// build the chained class name
		StringBuilder buf = new StringBuilder();
		for (int i=obfOuterClassNames.size()-1; i>=0; i--) {
			buf.append(obfOuterClassNames.get(i));
			buf.append("$");
		}
		buf.append(obfClassName);
		return buf.toString();
	}
	
	public static ClassEntry getDeobfClassEntry(ClassMapping classMapping) {
		return new ClassEntry(classMapping.getDeobfName());
	}
	
	public static ClassEntry getSuperclassEntry(CtClass c) {
		return new ClassEntry(Descriptor.toJvmName(c.getClassFile().getSuperclass()));
	}
	
	public static FieldEntry getFieldEntry(CtField field) {
		return new FieldEntry(
			getClassEntry(field.getDeclaringClass()),
			field.getName(),
			new Type(field.getFieldInfo().getDescriptor())
		);
	}
	
	public static FieldEntry getFieldEntry(FieldAccess call) {
		return new FieldEntry(
			new ClassEntry(Descriptor.toJvmName(call.getClassName())),
			call.getFieldName(),
			new Type(call.getSignature())
		);
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
	
	public static MethodEntry getMethodEntry(MethodDefinition def) {
		return new MethodEntry(
			new ClassEntry(def.getDeclaringType().getInternalName()),
			def.getName(),
			new Signature(def.getSignature())
		);
	}
	
	public static ConstructorEntry getConstructorEntry(CtConstructor constructor) {
		if (constructor.isClassInitializer()) {
			return new ConstructorEntry(
				getClassEntry(constructor.getDeclaringClass())
			);
		} else {
			return new ConstructorEntry(
				getClassEntry(constructor.getDeclaringClass()),
				new Signature(constructor.getMethodInfo().getDescriptor())
			);
		}
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
	
	public static ConstructorEntry getConstructorEntry(MethodDefinition def) {
		if (def.isTypeInitializer()) {
			return new ConstructorEntry(
				new ClassEntry(def.getDeclaringType().getInternalName())
			);
		} else {
			return new ConstructorEntry(
				new ClassEntry(def.getDeclaringType().getInternalName()),
				new Signature(def.getSignature())
			);
		}
	}
	
	public static BehaviorEntry getBehaviorEntry(CtBehavior behavior) {
		if (behavior instanceof CtMethod) {
			return getMethodEntry((CtMethod)behavior);
		} else if (behavior instanceof CtConstructor) {
			return getConstructorEntry((CtConstructor)behavior);
		}
		throw new Error("behavior is neither Method nor Constructor!");
	}
	
	public static BehaviorEntry getBehaviorEntry(String className, String behaviorName, String behaviorSignature) {
		return getBehaviorEntry(new ClassEntry(className), behaviorName, new Signature(behaviorSignature));
	}
	
	public static BehaviorEntry getBehaviorEntry(ClassEntry classEntry, String behaviorName, Signature behaviorSignature) {
		if (behaviorName.equals("<init>")) {
			return new ConstructorEntry(classEntry, behaviorSignature);
		} else if(behaviorName.equals("<clinit>")) {
			return new ConstructorEntry(classEntry);
		} else {
			return new MethodEntry(classEntry, behaviorName, behaviorSignature);
		}
	}
	
	public static BehaviorEntry getBehaviorEntry(MethodDefinition def) {
		if (def.isConstructor() || def.isTypeInitializer()) {
			return getConstructorEntry(def);
		} else {
			return getMethodEntry(def);
		}
	}
	
	public static BehaviorEntry getObfBehaviorEntry(ClassEntry classEntry, MethodMapping methodMapping) {
		return getBehaviorEntry(classEntry, methodMapping.getObfName(), methodMapping.getObfSignature());
	}
}
