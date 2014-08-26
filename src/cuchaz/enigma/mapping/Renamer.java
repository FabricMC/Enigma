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
package cuchaz.enigma.mapping;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import cuchaz.enigma.analysis.JarIndex;

public class Renamer
{
	private JarIndex m_index;
	private Mappings m_mappings;
	
	public Renamer( JarIndex index, Mappings mappings )
	{
		m_index = index;
		m_mappings = mappings;
	}
	
	public void setClassName( ClassEntry obf, String deobfName )
	{
		deobfName = NameValidator.validateClassName( deobfName );
		ClassMapping classMapping = getOrCreateClassMapping( obf );
		
		if( obf.isInnerClass() )
		{
			classMapping.setInnerClassName( obf.getInnerClassName(), deobfName );
		}
		else
		{
			m_mappings.m_classesByDeobf.remove( classMapping.getDeobfName() );
			classMapping.setDeobfName( deobfName );
			m_mappings.m_classesByDeobf.put( deobfName, classMapping );
		}
		
		updateDeobfMethodSignatures();
	}
	
	public void setFieldName( FieldEntry obf, String deobfName )
	{
		deobfName = NameValidator.validateFieldName( deobfName );
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping( obf.getClassEntry() );
		classMapping.setFieldName( obf.getName(), deobfName );
	}
	
	public void setMethodTreeName( MethodEntry obf, String deobfName )
	{
		for( MethodEntry entry : m_index.getRelatedMethodImplementations( obf ) )
		{
			setMethodName( entry, deobfName );
		}
	}

	public void setMethodName( MethodEntry obf, String deobfName )
	{
		deobfName = NameValidator.validateMethodName( deobfName );
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping( obf.getClassEntry() );
		String deobfSignature = m_mappings.getTranslator( m_index.getAncestries(), TranslationDirection.Deobfuscating ).translateSignature( obf.getSignature() );
		classMapping.setMethodNameAndSignature( obf.getName(), obf.getSignature(), deobfName, deobfSignature );
	}
	
	public void setArgumentName( ArgumentEntry obf, String deobfName )
	{
		deobfName = NameValidator.validateArgumentName( deobfName );
		ClassMapping classMapping = getOrCreateClassMappingOrInnerClassMapping( obf.getClassEntry() );
		classMapping.setArgumentName( obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), deobfName );
	}
	
	public void write( OutputStream out )
	throws IOException
	{
		// TEMP: just use the object output for now. We can find a more efficient storage format later
		GZIPOutputStream gzipout = new GZIPOutputStream( out );
		ObjectOutputStream oout = new ObjectOutputStream( gzipout );
		oout.writeObject( this );
		gzipout.finish();
	}
	
	private ClassMapping getOrCreateClassMapping( ClassEntry obfClassEntry )
	{
		String obfClassName = obfClassEntry.getOuterClassName();
		ClassMapping classMapping = m_mappings.m_classesByObf.get( obfClassName );
		if( classMapping == null )
		{
			classMapping = new ClassMapping( obfClassName, obfClassName );
			m_mappings.m_classesByObf.put( classMapping.getObfName(), classMapping );
			m_mappings.m_classesByDeobf.put( classMapping.getDeobfName(), classMapping );
		}
		return classMapping;
	}
	
	private ClassMapping getOrCreateClassMappingOrInnerClassMapping( ClassEntry obfClassEntry )
	{
		ClassMapping classMapping = getOrCreateClassMapping( obfClassEntry );
		if( obfClassEntry.isInnerClass() )
		{
			classMapping = classMapping.getOrCreateInnerClass( obfClassEntry.getInnerClassName() );
		}
		return classMapping;
	}
	
	private void updateDeobfMethodSignatures( )
	{
		Translator translator = m_mappings.getTranslator( m_index.getAncestries(), TranslationDirection.Deobfuscating );
		for( ClassMapping classMapping : m_mappings.m_classesByObf.values() )
		{
			classMapping.updateDeobfMethodSignatures( translator );
		}
	}
}
