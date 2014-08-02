/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;

public class MethodParametersAttribute extends AttributeInfo
{
	public MethodParametersAttribute( ConstPool pool, int attributeNameIndex, List<Integer> parameterNameIndices )
	{
		super( pool, "MethodParameters", writeStruct( attributeNameIndex, parameterNameIndices ) );
	}
	
	private static byte[] writeStruct( int attributeNameIndex, List<Integer> parameterNameIndices )
	{
		// JVM Spec says the struct looks like this:
		// http://cr.openjdk.java.net/~mr/se/8/java-se-8-fr-spec-01/java-se-8-jvms-fr-diffs.pdf
		// uint16 name_index -> points to UTF8 entry in constant pool that says "MethodParameters"
		// uint32 length -> length of this struct, minus 6 bytes (ie, length of num_params and parameter array)
		// uint8 num_params
		// for each param:
		//    uint16 name_index -> points to UTF8 entry in constant pool, or 0 for no entry
		//    uint16 access_flags -> don't care, just set to 0
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( buf );
		
		// NOTE: java hates unsigned integers, so we have to be careful here
		// the writeShort(), writeByte() methods will read 16,8 low-order bits from the int argument
		// as long as the int argument is in range of the unsigned short/byte type, it will be written as an unsigned short/byte
		// if the int is out of range, the byte stream won't look the way we want and weird things will happen
		final int SIZEOF_UINT16 = 2;
		final int MAX_UINT8 = ( 1 << 8 ) - 1;
		final int MAX_UINT16 = ( 1 << 16 ) - 1;
		final long MAX_UINT32 = ( 1 << 32 ) - 1;
		
		try
		{
			assert( attributeNameIndex >= 0 && attributeNameIndex <= MAX_UINT16 );
			out.writeShort( attributeNameIndex );
			
			long length = SIZEOF_UINT16 + parameterNameIndices.size()*( SIZEOF_UINT16 + SIZEOF_UINT16 );
			assert( length >= 0 && length <= MAX_UINT32 );
			out.writeInt( (int)length );
			
			assert( parameterNameIndices.size() >= 0 && parameterNameIndices.size() <= MAX_UINT8 );
			out.writeByte( parameterNameIndices.size() );
			
			for( Integer index : parameterNameIndices )
			{
				assert( index >= 0 && index <= MAX_UINT16 );
				out.writeShort( index );
				
				// just write 0 for the access flags
				out.writeShort( 0 );
			}
			
			out.close();
			return buf.toByteArray();
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
}
