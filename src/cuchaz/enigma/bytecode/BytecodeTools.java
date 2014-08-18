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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CtBehavior;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ExceptionTable;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.Util;
import cuchaz.enigma.bytecode.BytecodeIndexIterator.Index;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;

public class BytecodeTools
{
	public static byte[] writeBytecode( Bytecode bytecode )
	throws IOException
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( buf );
		try
		{
			// write the constant pool
			new ConstPoolEditor( bytecode.getConstPool() ).writePool( out );
			
			// write metadata
			out.writeShort( bytecode.getMaxStack() );
			out.writeShort( bytecode.getMaxLocals() );
			out.writeShort( bytecode.getStackDepth() );
			
			// write the code
			out.writeShort( bytecode.getSize() );
			out.write( bytecode.get() );
			
			// write the exception table
			int numEntries = bytecode.getExceptionTable().size();
			out.writeShort( numEntries );
			for( int i=0; i<numEntries; i++ )
			{
				out.writeShort( bytecode.getExceptionTable().startPc( i ) );
				out.writeShort( bytecode.getExceptionTable().endPc( i ) );
				out.writeShort( bytecode.getExceptionTable().handlerPc( i ) );
				out.writeShort( bytecode.getExceptionTable().catchType( i ) );
			}
			
			out.close();
			return buf.toByteArray();
		}
		catch( Exception ex )
		{
			Util.closeQuietly( out );
			throw new Error( ex );
		}
	}
	
	public static Bytecode readBytecode( byte[] bytes )
	throws IOException
	{
		ByteArrayInputStream buf = new ByteArrayInputStream( bytes );
		DataInputStream in = new DataInputStream( buf );
		try
		{
			// read the constant pool entries and update the class
			ConstPool pool = ConstPoolEditor.readPool( in );
			
			// read metadata
			int maxStack = in.readShort();
			int maxLocals = in.readShort();
			int stackDepth = in.readShort();
			
			Bytecode bytecode = new Bytecode( pool, maxStack, maxLocals );
			bytecode.setStackDepth( stackDepth );
			
			// read the code
			int size = in.readShort();
			byte[] code = new byte[size];
			in.read( code );
			setBytecode( bytecode, code );
			
			// read the exception table
			int numEntries = in.readShort();
			for( int i=0; i<numEntries; i++ )
			{
				bytecode.getExceptionTable().add( in.readShort(), in.readShort(), in.readShort(), in.readShort() );
			}
			
			in.close();
			return bytecode;
		}
		catch( Exception ex )
		{
			Util.closeQuietly( in );
			throw new Error( ex );
		}
	}
	
	public static Bytecode prepareMethodForBytecode( CtBehavior behavior, Bytecode bytecode )
	throws BadBytecode
	{
		// update the destination class const pool
		bytecode = copyBytecodeToConstPool( behavior.getMethodInfo().getConstPool(), bytecode );
		
		// update method locals and stack
		CodeAttribute attribute = behavior.getMethodInfo().getCodeAttribute();
		if( bytecode.getMaxLocals() > attribute.getMaxLocals() )
		{
			attribute.setMaxLocals( bytecode.getMaxLocals() );
		}
		if( bytecode.getMaxStack() > attribute.getMaxStack() )
		{
			attribute.setMaxStack( bytecode.getMaxStack() );
		}
		
		return bytecode;
	}
	
	public static Bytecode copyBytecodeToConstPool( ConstPool dest, Bytecode bytecode )
	throws BadBytecode
	{
		// get the entries this bytecode needs from the const pool
		Set<Integer> indices = Sets.newTreeSet();
		ConstPoolEditor editor = new ConstPoolEditor( bytecode.getConstPool() );
		BytecodeIndexIterator iterator = new BytecodeIndexIterator( bytecode );
		for( Index index : iterator.indices() )
		{
			assert( index.isValid( bytecode ) );
			InfoType.gatherIndexTree( indices, editor, index.getIndex() );
		}
		
		Map<Integer,Integer> indexMap = Maps.newTreeMap();
		
		ConstPool src = bytecode.getConstPool();
		ConstPoolEditor editorSrc = new ConstPoolEditor( src );
		ConstPoolEditor editorDest = new ConstPoolEditor( dest );
		
		// copy entries over in order of level so the index mapping is easier
		for( InfoType type : InfoType.getSortedByLevel() )
		{
			for( int index : indices )
			{
				ConstInfoAccessor entry = editorSrc.getItem( index );
				
				// skip entries that aren't this type
				if( entry.getType() != type )
				{
					continue;
				}
				
				// make sure the source entry is valid before we copy it
				assert( type.subIndicesAreValid( entry, editorSrc ) );
				assert( type.selfIndexIsValid( entry, editorSrc ) );
				
				// make a copy of the entry so we can modify it safely
				ConstInfoAccessor entryCopy = editorSrc.getItem( index ).copy();
				assert( type.subIndicesAreValid( entryCopy, editorSrc ) );
				assert( type.selfIndexIsValid( entryCopy, editorSrc ) );
				
				// remap the indices
				type.remapIndices( indexMap, entryCopy );
				assert( type.subIndicesAreValid( entryCopy, editorDest ) );
				
				// put the copy in the destination pool
				int newIndex = editorDest.addItem( entryCopy.getItem() );
				entryCopy.setIndex( newIndex );
				assert( type.selfIndexIsValid( entryCopy, editorDest ) ) : type + ", self: " + entryCopy + " dest: " + editorDest.getItem( entryCopy.getIndex() );
				
				// make sure the source entry is unchanged
				assert( type.subIndicesAreValid( entry, editorSrc ) );
				assert( type.selfIndexIsValid( entry, editorSrc ) );
				
				// add the index mapping so we can update the bytecode later
				if( indexMap.containsKey( index ) )
				{
					throw new Error( "Entry at index " + index + " already copied!" );
				}
				indexMap.put( index, newIndex );
			}
		}
		
		// make a new bytecode
		Bytecode newBytecode = new Bytecode( dest, bytecode.getMaxStack(), bytecode.getMaxLocals() );
		bytecode.setStackDepth( bytecode.getStackDepth() );
		setBytecode( newBytecode, bytecode.get() );
		setExceptionTable( newBytecode, bytecode.getExceptionTable() );
		
		// apply the mappings to the bytecode
		BytecodeIndexIterator iter = new BytecodeIndexIterator( newBytecode );
		for( Index index : iter.indices() )
		{
			int oldIndex = index.getIndex();
			Integer newIndex = indexMap.get( oldIndex );
			if( newIndex != null )
			{
				// make sure this mapping makes sense
				InfoType typeSrc = editorSrc.getItem( oldIndex ).getType();
				InfoType typeDest = editorDest.getItem( newIndex ).getType();
				assert( typeSrc == typeDest );
				
				// apply the mapping
				index.setIndex( newIndex );
			}
		}
		iter.saveChangesToBytecode();
		
		// make sure all the indices are valid
		iter = new BytecodeIndexIterator( newBytecode );
		for( Index index : iter.indices() )
		{
			assert( index.isValid( newBytecode ) );
		}
		
		return newBytecode;
	}
	
	public static void setBytecode( Bytecode dest, byte[] src )
	{
		if( src.length > dest.getSize() )
		{
			dest.addGap( src.length - dest.getSize() );
		}
		assert( dest.getSize() == src.length );
		for( int i=0; i<src.length; i++ )
		{
			dest.write( i, src[i] );
		}
	}
	
	public static void setExceptionTable( Bytecode dest, ExceptionTable src )
	{
		// clear the dest exception table
		int size = dest.getExceptionTable().size();
		for( int i=size-1; i>=0; i-- )
		{
			dest.getExceptionTable().remove( i );
		}
		
		// copy the exception table
		for( int i=0; i<src.size(); i++ )
		{
			dest.getExceptionTable().add(
				src.startPc( i ),
				src.endPc( i ),
				src.handlerPc( i ),
				src.catchType( i )
			);
		}
	}
	
	public static List<String> getParameterTypes( String signature )
	{
		List<String> types = Lists.newArrayList();
		for( int i=0; i<signature.length(); )
		{
			char c = signature.charAt( i );
			
			// handle parens
			if( c == '(' )
			{
				i++;
				c = signature.charAt( i );
			}
			if( c == ')' )
			{
				break;
			}
			
			// find a type
			String type = null;
			
			int arrayDim = 0;
			while( c == '[' )
			{
				// advance to array type
				arrayDim++;
				i++;
				c = signature.charAt( i );
			}
			
			if( c == 'L' )
			{
				// read class type
				int pos = signature.indexOf( ';', i + 1 );
				String className = signature.substring( i + 1, pos );
				type = "L" + className + ";";
				i = pos + 1;
			}
			else
			{
				// read primitive type
				type = signature.substring( i, i + 1 );
				i++;
			}
			
			// was it an array?
			while( arrayDim-- > 0 )
			{
				type = "[" + type;
			}
			types.add( type );
		}
		return types;
	}
}
