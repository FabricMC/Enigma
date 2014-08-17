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

import cuchaz.enigma.analysis.BridgeFixer;
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
		// what class file should we actually load?
		String obfClassName = m_obfuscatingTranslator.translateClass( deobfClassName );
		if( obfClassName == null )
		{
			obfClassName = deobfClassName;
		}
		String classFileName = obfClassName;
		
		// is this an inner class?
		if( obfClassName.indexOf( '$' ) >= 0 )
		{
			// the file name is the bare inner class name
			String[] parts = obfClassName.split( "\\$" );
			classFileName = parts[parts.length - 1];
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
			
			// do all kinds of deobfuscating transformations on the class
			new InnerClassWriter( m_deobfuscatingTranslator, m_jarIndex ).write( c );
			new BridgeFixer().fixBridges( c );
			new MethodParameterWriter( m_deobfuscatingTranslator ).writeMethodArguments( c );
			new ClassTranslator( m_deobfuscatingTranslator ).translate( c );
			
			// sanity checking
			assert( Descriptor.toJvmName( c.getName() ).equals( deobfClassName ) )
				: String.format( "%s is not %s", Descriptor.toJvmName( c.getName() ), deobfClassName );
			assert( Descriptor.toJvmName( c.getClassFile().getName() ).equals( deobfClassName ) )
				: String.format( "%s is not %s", Descriptor.toJvmName( c.getClassFile().getName() ), deobfClassName );
			
			// pass the transformed class along to the decompiler
			buf = c.toBytecode();
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
