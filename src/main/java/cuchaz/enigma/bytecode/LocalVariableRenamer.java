/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.bytecode;

import cuchaz.enigma.mapping.*;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.*;

public class LocalVariableRenamer {

	private Translator translator;

	public LocalVariableRenamer(Translator translator) {
		this.translator = translator;
	}

	public void rename(CtClass c) {
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {

			// if there's a local variable table, just rename everything to v1, v2, v3, ... for now
			CodeAttribute codeAttribute = behavior.getMethodInfo().getCodeAttribute();
			if (codeAttribute == null) {
				continue;
			}

			BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
			ConstPool constants = c.getClassFile().getConstPool();

			LocalVariableAttribute table = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
			if (table != null) {
				renameLVT(behaviorEntry, constants, table);
			}

			LocalVariableTypeAttribute typeTable = (LocalVariableTypeAttribute) codeAttribute.getAttribute(LocalVariableAttribute.typeTag);
			if (typeTable != null) {
				renameLVTT(typeTable, table);
			}
		}
	}

	// DEBUG
	@SuppressWarnings("unused")
	private void dumpTable(LocalVariableAttribute table) {
		for (int i = 0; i < table.tableLength(); i++) {
			System.out.println(String.format("\t%d (%d): %s %s",
				i, table.index(i), table.variableName(i), table.descriptor(i)
			));
		}
	}

	private void renameLVT(BehaviorEntry behaviorEntry, ConstPool constants, LocalVariableAttribute table) {

		// skip empty tables
		if (table.tableLength() <= 0) {
			return;
		}

		// where do we start counting variables?
		int starti = 0;
		if (table.variableName(0).equals("this")) {
			// skip the "this" variable
			starti = 1;
		}

		// rename method arguments first
		int numArgs = 0;
		if (behaviorEntry.getSignature() != null) {
			numArgs = behaviorEntry.getSignature().getArgumentTypes().size();

			boolean isNestedClassConstructor = false;

			// If the behavior is a constructor and if it have more than one arg, it's probably from a nested!
			if (behaviorEntry instanceof ConstructorEntry && behaviorEntry.getClassEntry() != null && behaviorEntry.getClassEntry().isInnerClass() && numArgs >= 1) {
				// Get the first arg type
				Type firstArg = behaviorEntry.getSignature().getArgumentTypes().get(0);

				// If the arg is a class and if the class name match the outer class name of the constructor, it's definitely a constructor of a nested class
				if (firstArg.isClass() && firstArg.getClassEntry().equals(behaviorEntry.getClassEntry().getOuterClassEntry())) {
					isNestedClassConstructor = true;
					numArgs--;
				}
			}

			for (int i = starti; i < starti + numArgs && i < table.tableLength(); i++) {
				int argi = i - starti;
				String argName = this.translator.translate(new ArgumentEntry(behaviorEntry, argi, ""));
				if (argName == null) {
					Type argType = behaviorEntry.getSignature().getArgumentTypes().get(isNestedClassConstructor ? argi + 1 : argi);
					// Unfortunately each of these have different name getters, so they have different code paths
					if (argType.isPrimitive()) {
						Type.Primitive argCls = argType.getPrimitive();
						argName = "a" + argCls.name() + (argi + 1);
					} else if (argType.isArray()) {
						// List types would require this whole block again, so just go with aListx
						argName = "aList" + (argi + 1);
					} else if (argType.isClass()) {
						ClassEntry argClsTrans = this.translator.translateEntry(argType.getClassEntry());
						argName = "a" + argClsTrans.getSimpleName().replace("$", "") + (argi + 1);
					} else {
						argName = "a" + (argi + 1);
					}
				}
				renameVariable(table, i, constants.addUtf8Info(argName));
			}
		}

		// then rename the rest of the args, if any
		for (int i = starti + numArgs; i < table.tableLength(); i++) {
			int firstIndex = Math.min(table.index(starti + numArgs), table.index(i));
			renameVariable(table, i, constants.addUtf8Info("v" + (table.index(i) - firstIndex + 1)));
		}
	}

	private void renameLVTT(LocalVariableTypeAttribute typeTable, LocalVariableAttribute table) {
		// rename args to the same names as in the LVT
		for (int i = 0; i < typeTable.tableLength(); i++) {
			renameVariable(typeTable, i, getNameIndex(table, typeTable.index(i)));
		}
	}

	private void renameVariable(LocalVariableAttribute table, int i, int stringId) {
		// based off of LocalVariableAttribute.nameIndex()
		ByteArray.write16bit(stringId, table.get(), i * 10 + 6);
	}

	private int getNameIndex(LocalVariableAttribute table, int index) {
		for (int i = 0; i < table.tableLength(); i++) {
			if (table.index(i) == index) {
				return table.nameIndex(i);
			}
		}
		return 0;
	}
}
