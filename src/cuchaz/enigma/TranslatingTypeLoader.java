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
import javassist.ClassPool;
import javassist.CtClass;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import cuchaz.enigma.bytecode.ClassTranslator;
import cuchaz.enigma.bytecode.MethodParameterWriter;
import cuchaz.enigma.mapping.Translator;

public class TranslatingTypeLoader implements ITypeLoader
{
	private JarFile m_jar;
	private Translator m_obfuscatingTranslator;
	private Translator m_deobfuscatingTranslator;
	
	public TranslatingTypeLoader( JarFile jar, Translator obfuscatingTranslator, Translator deobfuscatingTranslator )
	{
		m_jar = jar;
		m_obfuscatingTranslator = obfuscatingTranslator;
		m_deobfuscatingTranslator = deobfuscatingTranslator;
	}
	
	@Override
	public boolean tryLoadType( String name, Buffer out )
	{
		// is this a deobufscated class name?
		String obfName = m_obfuscatingTranslator.translateClass( name );
		if( obfName != null )
		{
			// point to the obfuscated class
			name = obfName;
		}
		
		JarEntry entry = m_jar.getJarEntry( name + ".class" );
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
			buf = data.toByteArray();
			
			// translate the class
			ClassPool classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( name, buf ) );
			try
			{
				CtClass c = classPool.get( name );
				new MethodParameterWriter( m_deobfuscatingTranslator ).writeMethodArguments( c );
				new ClassTranslator( m_deobfuscatingTranslator ).translate( c );
				buf = c.toBytecode();
			}
			catch( Exception ex )
			{
				throw new Error( ex );
			}
			
			// pass it along to the decompiler
			out.reset( buf.length );
			System.arraycopy( buf, 0, out.array(), out.position(), buf.length );
			out.position( 0 );
			
			return true;
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
}
