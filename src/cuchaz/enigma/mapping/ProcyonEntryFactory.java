package cuchaz.enigma.mapping;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;


public class ProcyonEntryFactory {
	
	public static FieldEntry getFieldEntry(FieldDefinition def) {
		return new FieldEntry(
			new ClassEntry(def.getDeclaringType().getInternalName()),
			def.getName(),
			new Type(def.getErasedSignature())
		);
	}
	
	public static MethodEntry getMethodEntry(MethodDefinition def) {
		return new MethodEntry(
			new ClassEntry(def.getDeclaringType().getInternalName()),
			def.getName(),
			new Signature(def.getErasedSignature())
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
				new Signature(def.getErasedSignature())
			);
		}
	}
	
	public static BehaviorEntry getBehaviorEntry(MethodDefinition def) {
		if (def.isConstructor() || def.isTypeInitializer()) {
			return getConstructorEntry(def);
		} else {
			return getMethodEntry(def);
		}
	}
}
