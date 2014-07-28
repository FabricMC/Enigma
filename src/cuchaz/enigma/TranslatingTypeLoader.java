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
import cuchaz.enigma.mapping.Translator;

public class TranslatingTypeLoader implements ITypeLoader
{
	private JarFile m_jar;
	private ClassTranslator m_classTranslator;
	private Translator m_obfuscatingTranslator;
	
	public TranslatingTypeLoader( JarFile jar, Translator deobfuscatingTranslator, Translator obfuscatingTranslator )
	{
		m_jar = jar;
		m_classTranslator = new ClassTranslator( deobfuscatingTranslator );
		m_obfuscatingTranslator = obfuscatingTranslator;
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
			byte[] buf = new byte[(int)entry.getSize()];
			InputStream in = m_jar.getInputStream( entry );
			int bytesRead = in.read( buf );
			assert( bytesRead == buf.length );
			
			// translate the class
			ClassPool classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( name, buf ) );
			try
			{
				CtClass c = classPool.get( name );
				m_classTranslator.translate( c );
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
