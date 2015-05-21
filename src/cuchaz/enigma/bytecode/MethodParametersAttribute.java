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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

public class MethodParametersAttribute extends AttributeInfo {
	
	private MethodParametersAttribute(ConstPool pool, List<Integer> parameterNameIndices) {
		super(pool, "MethodParameters", writeStruct(parameterNameIndices));
	}
	
	public static void updateClass(MethodInfo info, List<String> names) {
		
		// add the names to the class const pool
		ConstPool constPool = info.getConstPool();
		List<Integer> parameterNameIndices = new ArrayList<Integer>();
		for (String name : names) {
			if (name != null) {
				parameterNameIndices.add(constPool.addUtf8Info(name));
			} else {
				parameterNameIndices.add(0);
			}
		}
		
		// add the attribute to the method
		info.addAttribute(new MethodParametersAttribute(constPool, parameterNameIndices));
	}
	
	private static byte[] writeStruct(List<Integer> parameterNameIndices) {
		// JVM 8 Spec says the struct looks like this:
		// http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.24
		// uint8 num_params
		// for each param:
		// uint16 name_index -> points to UTF8 entry in constant pool, or 0 for no entry
		// uint16 access_flags -> don't care, just set to 0
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		
		// NOTE: java hates unsigned integers, so we have to be careful here
		// the writeShort(), writeByte() methods will read 16,8 low-order bits from the int argument
		// as long as the int argument is in range of the unsigned short/byte type, it will be written as an unsigned short/byte
		// if the int is out of range, the byte stream won't look the way we want and weird things will happen
		final int SIZEOF_UINT8 = 1;
		final int SIZEOF_UINT16 = 2;
		final int MAX_UINT8 = (1 << 8) - 1;
		final int MAX_UINT16 = (1 << 16) - 1;
		
		try {
			assert (parameterNameIndices.size() >= 0 && parameterNameIndices.size() <= MAX_UINT8);
			out.writeByte(parameterNameIndices.size());
			
			for (Integer index : parameterNameIndices) {
				assert (index >= 0 && index <= MAX_UINT16);
				out.writeShort(index);
				
				// just write 0 for the access flags
				out.writeShort(0);
			}
			
			out.close();
			byte[] data = buf.toByteArray();
			assert (data.length == SIZEOF_UINT8 + parameterNameIndices.size() * (SIZEOF_UINT16 + SIZEOF_UINT16));
			return data;
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
}
