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

import java.util.Iterator;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;

public class BytecodeIndexIterator implements Iterator<BytecodeIndexIterator.Index>
{
	public static class Index
	{
		private CodeIterator m_iter;
		private int m_pos;
		private boolean m_isWide;
		
		protected Index( CodeIterator iter, int pos, boolean isWide )
		{
			m_iter = iter;
			m_pos = pos;
			m_isWide = isWide;
		}
		
		public int getIndex( )
		{
			if( m_isWide )
			{
				return m_iter.s16bitAt( m_pos );
			}
			else
			{
				return m_iter.byteAt( m_pos );
			}
		}
		
		public void setIndex( int val )
		throws BadBytecode
		{
			if( m_isWide )
			{
				m_iter.write16bit( val, m_pos );
			}
			else
			{
				if( val < 256 )
				{
					// we can write the byte
					m_iter.writeByte( val, m_pos );
				}
				else
				{
					// we need to upgrade this instruction to LDC_W
					assert( m_iter.byteAt( m_pos - 1 ) == Opcode.LDC );
					m_iter.insertGap( m_pos - 1, 1 );
					m_iter.writeByte( Opcode.LDC_W, m_pos - 1 );
					m_iter.write16bit( val, m_pos );
					m_isWide = true;
					
					// move the iterator to the next opcode
					m_iter.move( m_pos + 2 );
				}
			}
			
			// sanity check
			assert( val == getIndex() );
		}
		
		public boolean isValid( Bytecode bytecode )
		{
			return getIndex() >= 0 && getIndex() < bytecode.getConstPool().getSize();
		}
	}
	
	private Bytecode m_bytecode;
	private CodeAttribute m_attribute;
	private CodeIterator m_iter;
	private Index m_next;
	
	public BytecodeIndexIterator( Bytecode bytecode )
	throws BadBytecode
	{
		m_bytecode = bytecode;
		m_attribute = bytecode.toCodeAttribute();
		m_iter = m_attribute.iterator();
		
		m_next = getNext();
	}
	
	@Override
	public boolean hasNext( )
	{
		return m_next != null;
	}

	@Override
	public Index next( )
	{
		Index out = m_next;
		try
		{
			m_next = getNext();
		}
		catch( BadBytecode ex )
		{
			throw new Error( ex );
		}
		return out;
	}

	@Override
	public void remove( )
	{
		throw new UnsupportedOperationException();
	}
	
	private Index getNext( )
	throws BadBytecode
	{
		while( m_iter.hasNext() )
		{
			int pos = m_iter.next();
			int opcode = m_iter.byteAt( pos );
			switch( opcode )
			{
				// for only these opcodes, the next two bytes are a const pool reference
				case Opcode.ANEWARRAY:
				case Opcode.CHECKCAST:
				case Opcode.INSTANCEOF:
				case Opcode.INVOKEDYNAMIC:
				case Opcode.INVOKEINTERFACE:
				case Opcode.INVOKESPECIAL:
				case Opcode.INVOKESTATIC:
				case Opcode.INVOKEVIRTUAL:
				case Opcode.LDC_W:
				case Opcode.LDC2_W:
				case Opcode.MULTIANEWARRAY:
				case Opcode.NEW:
				case Opcode.PUTFIELD:
				case Opcode.PUTSTATIC:
				case Opcode.GETFIELD:
				case Opcode.GETSTATIC:
					return new Index( m_iter, pos + 1, true );
				
				case Opcode.LDC:
					return new Index( m_iter, pos + 1, false );
			}
		}
		
		return null;
	}
	
	public Iterable<Index> indices( )
	{
		return new Iterable<Index>( )
		{
			@Override
			public Iterator<Index> iterator( )
			{
				return BytecodeIndexIterator.this;
			}
		};
	}
	
	public void saveChangesToBytecode( )
	{
		BytecodeTools.setBytecode( m_bytecode, m_attribute.getCode() );
	}
}
