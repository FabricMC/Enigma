package cuchaz.enigma.bytecode;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.ByteArray;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.LocalVariableAttribute;


public class LocalVariableRenamer {

	public void rename(CtClass c) {
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			
			// if there's a local variable table, just rename everything to v1, v2, v3, ... for now
			CodeAttribute codeAttribute = behavior.getMethodInfo().getCodeAttribute();
			if (codeAttribute == null) {
				continue;
			}
			LocalVariableAttribute table = (LocalVariableAttribute)codeAttribute.getAttribute(LocalVariableAttribute.tag);
			if (table == null) {
				continue;
			}
			
			ConstPool constants = c.getClassFile().getConstPool();
			for (int i=0; i<table.tableLength(); i++) {
				renameVariable(table, i, constants.addUtf8Info("v" + i));
			}
		}
	}

	private void renameVariable(LocalVariableAttribute table, int i, int stringId) {
		// based off of LocalVariableAttribute.nameIndex()
		ByteArray.write16bit(stringId, table.get(), i*10 + 6);
	}
}
