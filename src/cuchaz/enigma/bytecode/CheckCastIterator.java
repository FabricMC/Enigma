/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.bytecode;

import java.util.Iterator;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;
import cuchaz.enigma.bytecode.CheckCastIterator.CheckCast;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;

public class CheckCastIterator implements Iterator<CheckCast> {
	
	public static class CheckCast {
		
		public String className;
		public MethodEntry prevMethodEntry;
		
		public CheckCast(String className, MethodEntry prevMethodEntry) {
			this.className = className;
			this.prevMethodEntry = prevMethodEntry;
		}
	}
	
	private ConstPool m_constants;
	private CodeAttribute m_attribute;
	private CodeIterator m_iter;
	private CheckCast m_next;
	
	public CheckCastIterator(CodeAttribute codeAttribute) throws BadBytecode {
		m_constants = codeAttribute.getConstPool();
		m_attribute = codeAttribute;
		m_iter = m_attribute.iterator();
		
		m_next = getNext();
	}
	
	@Override
	public boolean hasNext() {
		return m_next != null;
	}
	
	@Override
	public CheckCast next() {
		CheckCast out = m_next;
		try {
			m_next = getNext();
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
		while (m_iter.hasNext()) {
			int pos = m_iter.next();
			int opcode = m_iter.byteAt(pos);
			switch (opcode) {
				case Opcode.CHECKCAST:
					
					// get the type of this op code (next two bytes are a classinfo index)
					MethodEntry prevMethodEntry = getMethodEntry(prevPos);
					if (prevMethodEntry != null) {
						return new CheckCast(m_constants.getClassInfo(m_iter.s16bitAt(pos + 1)), prevMethodEntry);
					}
				break;
			}
			prevPos = pos;
		}
		return null;
	}
	
	private MethodEntry getMethodEntry(int pos) {
		switch (m_iter.byteAt(pos)) {
			case Opcode.INVOKEVIRTUAL:
			case Opcode.INVOKESTATIC:
			case Opcode.INVOKEDYNAMIC:
			case Opcode.INVOKESPECIAL: {
				int index = m_iter.s16bitAt(pos + 1);
				return new MethodEntry(
					new ClassEntry(Descriptor.toJvmName(m_constants.getMethodrefClassName(index))),
					m_constants.getMethodrefName(index),
					new Signature(m_constants.getMethodrefType(index))
				);
			}
			
			case Opcode.INVOKEINTERFACE: {
				int index = m_iter.s16bitAt(pos + 1);
				return new MethodEntry(
					new ClassEntry(Descriptor.toJvmName(m_constants.getInterfaceMethodrefClassName(index))),
					m_constants.getInterfaceMethodrefName(index),
					new Signature(m_constants.getInterfaceMethodrefType(index))
				);
			}
		}
		return null;
	}
	
	public Iterable<CheckCast> casts() {
		return new Iterable<CheckCast>() {
			@Override
			public Iterator<CheckCast> iterator() {
				return CheckCastIterator.this;
			}
		};
	}
}
