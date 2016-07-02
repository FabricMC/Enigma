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

import java.util.Iterator;

import cuchaz.enigma.bytecode.CheckCastIterator.CheckCast;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;
import javassist.bytecode.*;

public class CheckCastIterator implements Iterator<CheckCast> {

    public static class CheckCast {

        public final String className;
        public final MethodEntry prevMethodEntry;

        public CheckCast(String className, MethodEntry prevMethodEntry) {
            this.className = className;
            this.prevMethodEntry = prevMethodEntry;
        }
    }

    private final ConstPool constants;
    private final CodeAttribute attribute;
    private final CodeIterator iter;
    private CheckCast next;

    public CheckCastIterator(CodeAttribute codeAttribute) throws BadBytecode {
        this.constants = codeAttribute.getConstPool();
        this.attribute = codeAttribute;
        this.iter = this.attribute.iterator();

        this.next = getNext();
    }

    @Override
    public boolean hasNext() {
        return this.next != null;
    }

    @Override
    public CheckCast next() {
        CheckCast out = this.next;
        try {
            this.next = getNext();
        } catch (BadBytecode ex) {
            throw new Error(ex);
        }
        return out;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private CheckCast getNext() throws BadBytecode {
        int prevPos = 0;
        while (this.iter.hasNext()) {
            int pos = this.iter.next();
            int opcode = this.iter.byteAt(pos);
            switch (opcode) {
                case Opcode.CHECKCAST:

                    // get the type of this op code (next two bytes are a classinfo index)
                    MethodEntry prevMethodEntry = getMethodEntry(prevPos);
                    if (prevMethodEntry != null) {
                        return new CheckCast(this.constants.getClassInfo(this.iter.s16bitAt(pos + 1)), prevMethodEntry);
                    }
                    break;
            }
            prevPos = pos;
        }
        return null;
    }

    private MethodEntry getMethodEntry(int pos) {
        switch (this.iter.byteAt(pos)) {
            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESTATIC:
            case Opcode.INVOKEDYNAMIC:
            case Opcode.INVOKESPECIAL: {
                int index = this.iter.s16bitAt(pos + 1);
                return new MethodEntry(
                        new ClassEntry(Descriptor.toJvmName(this.constants.getMethodrefClassName(index))),
                        this.constants.getMethodrefName(index),
                        new Signature(this.constants.getMethodrefType(index))
                );
            }

            case Opcode.INVOKEINTERFACE: {
                int index = this.iter.s16bitAt(pos + 1);
                return new MethodEntry(
                        new ClassEntry(Descriptor.toJvmName(this.constants.getInterfaceMethodrefClassName(index))),
                        this.constants.getInterfaceMethodrefName(index),
                        new Signature(this.constants.getInterfaceMethodrefType(index))
                );
            }
        }
        return null;
    }
}
