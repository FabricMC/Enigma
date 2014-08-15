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
package cuchaz.enigma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.ClassTranslator;
import cuchaz.enigma.bytecode.InnerClassWriter;
import cuchaz.enigma.bytecode.MethodParameterWriter;
import cuchaz.enigma.mapping.Translator;

public class TranslatingTypeLoader implements ITypeLoader
{
	private JarFile m_jar;
	private JarIndex m_jarIndex;
	private Translator m_obfuscatingTranslator;
	private Translator m_deobfuscatingTranslator;
	
	public TranslatingTypeLoader( JarFile jar, JarIndex jarIndex, Translator obfuscatingTranslator, Translator deobfuscatingTranslator )
	{
		m_jar = jar;
		m_jarIndex = jarIndex;
		m_obfuscatingTranslator = obfuscatingTranslator;
		m_deobfuscatingTranslator = deobfuscatingTranslator;
	}
	
	@Override
	public boolean tryLoadType( String deobfClassName, Buffer out )
	{
		// TEMP
		if( !deobfClassName.startsWith( "java" ) && !deobfClassName.startsWith( "org" ) )
		{
			System.out.println( "Looking for: " + deobfClassName );
		}
		
		// what class file should we actually load?
		String obfClassName = m_obfuscatingTranslator.translateClass( deobfClassName );
		if( obfClassName == null )
		{
			obfClassName = deobfClassName;
		}
		String classFileName = obfClassName;
		
		// is this a properly-referenced inner class?
		boolean isInnerClass = deobfClassName.indexOf( '$' ) >= 0;
		if( isInnerClass )
		{
			// get just the bare inner class name
			String[] parts = deobfClassName.split( "\\$" );
			String deobfClassFileName = parts[parts.length - 1];
			
			// make sure the bare inner class name is obfuscated
			classFileName = m_obfuscatingTranslator.translateClass( deobfClassFileName );
			if( classFileName == null )
			{
				classFileName = deobfClassFileName;
			}
		}
		
		// TEMP
		if( !deobfClassName.startsWith( "java" ) && !deobfClassName.startsWith( "org" ) )
		{
			System.out.println( "\tLooking at class file: " + classFileName );
		}
		
		// get the jar entry
		JarEntry entry = m_jar.getJarEntry( classFileName + ".class" );
		if( entry == null )
		{
			return false;
		}
		
		try
		{
			// read the class file into a buffer
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			byte[] buf = new byte[1024*1024]; // 1 KiB
			InputStream in = m_jar.getInputStream( entry );
			while( true )
			{
				int bytesRead = in.read( buf );
				if( bytesRead <= 0 )
				{
					break;
				}
				data.write( buf, 0, bytesRead );
			}
			data.close();
			in.close();
			buf = data.toByteArray();
			
			// load the javassist handle to the class
			String javaClassFileName = Descriptor.toJavaName( classFileName );
			ClassPool classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( javaClassFileName, buf ) );
			CtClass c = classPool.get( javaClassFileName );
			
			if( isInnerClass )
			{
				// rename the class to what procyon expects
				c.setName( deobfClassName );
			}
			else
			{
				// maybe it's an outer class
				new InnerClassWriter( m_deobfuscatingTranslator, m_jarIndex ).writeInnerClasses( c );
			}
			
			new MethodParameterWriter( m_deobfuscatingTranslator ).writeMethodArguments( c );
			new ClassTranslator( m_deobfuscatingTranslator ).translate( c );
			
			assert( Descriptor.toJvmName( c.getName() ).equals( deobfClassName ) );
			assert( c.getClassFile().getName().equals( deobfClassName ) );
			
			buf = c.toBytecode();
			
			// pass the transformed class along to the decompiler
			out.reset( buf.length );
			System.arraycopy( buf, 0, out.array(), out.position(), buf.length );
			out.position( 0 );
			
			return true;
		}
		catch( IOException | NotFoundException | CannotCompileException ex )
		{
			throw new Error( ex );
		}
	}
	
}
