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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.jar.JarFile;

import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.SourceIndexVisitor;
import cuchaz.enigma.analysis.TreeDumpVisitor;
import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Renamer;
import cuchaz.enigma.mapping.TranslationDirection;
import cuchaz.enigma.mapping.Translator;

public class Deobfuscator
{
	private File m_file;
	private JarFile m_jar;
	private DecompilerSettings m_settings;
	private JarIndex m_jarIndex;
	private Mappings m_mappings;
	private Renamer m_renamer;
	private TranslatingTypeLoader m_typeLoader;
	
	public Deobfuscator( File file )
	throws IOException
	{
		m_file = file;
		m_jar = new JarFile( m_file );
		
		// build the jar index
		m_jarIndex = new JarIndex();
		m_jarIndex.indexJar( m_jar );
		
		// config the decompiler
		m_settings = DecompilerSettings.javaDefaults();
		// DEBUG
		//m_settings.setShowSyntheticMembers( true );
		
		// init mappings
		setMappings( new Mappings() );
	}
	
	public String getJarName( )
	{
		return m_file.getName();
	}
	
	public JarIndex getJarIndex( )
	{
		return m_jarIndex;
	}
	
	public Mappings getMappings( )
	{
		return m_mappings;
	}
	public void setMappings( Mappings val )
	{
		if( val == null )
		{
			val = new Mappings();
		}
		m_mappings = val;
		m_renamer = new Renamer( m_jarIndex, m_mappings );
		
		// update decompiler options
		m_typeLoader = new TranslatingTypeLoader(
			m_jar,
			m_jarIndex,
			getTranslator( TranslationDirection.Obfuscating ),
			getTranslator( TranslationDirection.Deobfuscating )
		);
		m_settings.setTypeLoader( m_typeLoader );
	}
	
	public Translator getTranslator( TranslationDirection direction )
	{
		return m_mappings.getTranslator( m_jarIndex.getAncestries(), direction );
	}
	
	public void getSeparatedClasses( List<String> obfClasses, List<String> deobfClasses )
	{
		for( String obfClassName : m_jarIndex.getObfClassNames() )
		{
			// skip inner classes
			if( m_jarIndex.getOuterClass( obfClassName ) != null )
			{
				continue;
			}
			
			// separate the classes
			ClassMapping classMapping = m_mappings.getClassByObf( obfClassName );
			if( classMapping != null && !classMapping.getObfName().equals( classMapping.getDeobfName() ) )
			{
				deobfClasses.add( classMapping.getDeobfName() );
			}
			else if( obfClassName.indexOf( '/' ) >= 0 )
			{
				// this class is in a package and therefore is not obfuscated
				deobfClasses.add( obfClassName );
			}
			else
			{
				obfClasses.add( obfClassName );
			}
		}
	}
	
	public SourceIndex getSource( String className )
	{
		// is this class deobfuscated?
		// we need to tell the decompiler the deobfuscated name so it doesn't get freaked out
		// the decompiler only sees the deobfuscated class, so we need to load it by the deobfuscated name
		ClassMapping classMapping = m_mappings.getClassByObf( className );
		if( classMapping != null )
		{
			className = classMapping.getDeobfName();
		}
		
		// decompile it!
		TypeDefinition resolvedType = new MetadataSystem( m_settings.getTypeLoader() ).lookupType( className ).resolve();
		DecompilerContext context = new DecompilerContext();
		context.setCurrentType( resolvedType );
		context.setSettings( m_settings );
		AstBuilder builder = new AstBuilder( context );
		builder.addType( resolvedType );
		builder.runTransformations( null );
		CompilationUnit root = builder.getCompilationUnit();
		
		// render the AST into source
		StringWriter buf = new StringWriter();
		root.acceptVisitor( new InsertParenthesesVisitor(), null );
		// DEBUG
		root.acceptVisitor( new TreeDumpVisitor( new File( "tree.txt" ) ), null );
		root.acceptVisitor( new JavaOutputVisitor( new PlainTextOutput( buf ), m_settings ), null );
		
		// build the source index
		SourceIndex index = new SourceIndex( buf.toString() );
		root.acceptVisitor( new SourceIndexVisitor(), index );
		
		/* DEBUG
		for( Token token : index.referenceTokens() )
		{
			EntryReference<Entry,Entry> reference = index.getDeobfReference( token );
			System.out.println( token + " -> " + reference + " -> " + index.getReferenceToken( reference ) );
		}
		*/
		
		return index;
	}
	
	public Entry obfuscateEntry( Entry deobfEntry )
	{
		if( deobfEntry == null )
		{
			return null;
		}
		return getTranslator( TranslationDirection.Obfuscating ).translateEntry( deobfEntry );
	}
	
	public Entry deobfuscateEntry( Entry obfEntry )
	{
		if( obfEntry == null )
		{
			return null;
		}
		return getTranslator( TranslationDirection.Deobfuscating ).translateEntry( obfEntry );
	}
	
	public EntryReference<Entry,Entry> obfuscateReference( EntryReference<Entry,Entry> deobfReference )
	{
		if( deobfReference == null )
		{
			return null;
		}
		return new EntryReference<Entry,Entry>(
			obfuscateEntry( deobfReference.entry ),
			obfuscateEntry( deobfReference.context ),
			deobfReference.pos
		);
	}
	
	public EntryReference<Entry,Entry> deobfuscateReference( EntryReference<Entry,Entry> obfReference )
	{
		if( obfReference == null )
		{
			return null;
		}
		return new EntryReference<Entry,Entry>(
			deobfuscateEntry( obfReference.entry ),
			deobfuscateEntry( obfReference.context ),
			obfReference.pos
		);
	}
	
	// NOTE: these methods are a bit messy... oh well

	public void rename( Entry obfEntry, String newName )
	{
		if( obfEntry instanceof ClassEntry )
		{
			m_renamer.setClassName( (ClassEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof FieldEntry )
		{
			m_renamer.setFieldName( (FieldEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof MethodEntry )
		{
			m_renamer.setMethodTreeName( (MethodEntry)obfEntry, newName );
		}
		else if( obfEntry instanceof ConstructorEntry )
		{
			m_renamer.setClassName( obfEntry.getClassEntry(), newName );
		}
		else if( obfEntry instanceof ArgumentEntry )
		{
			m_renamer.setArgumentName( (ArgumentEntry)obfEntry, newName );
		}
		else
		{
			throw new Error( "Unknown entry type: " + obfEntry.getClass().getName() );
		}
		
		// clear the type loader cache
		m_typeLoader.clearCache();
	}
	
	public boolean hasMapping( Entry obfEntry )
	{
		Translator translator = getTranslator( TranslationDirection.Deobfuscating );
		if( obfEntry instanceof ClassEntry )
		{
			String deobfName = translator.translate( (ClassEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof FieldEntry )
		{
			String deobfName = translator.translate( (FieldEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof MethodEntry )
		{
			String deobfName = translator.translate( (MethodEntry)obfEntry );
			return deobfName != null && !deobfName.equals( obfEntry.getName() );
		}
		else if( obfEntry instanceof ConstructorEntry )
		{
			String deobfName = translator.translate( obfEntry.getClassEntry() );
			return deobfName != null && !deobfName.equals( obfEntry.getClassName() );
		}
		else if( obfEntry instanceof ArgumentEntry )
		{
			return translator.translate( (ArgumentEntry)obfEntry ) != null;
		}
		else
		{
			throw new Error( "Unknown entry type: " + obfEntry.getClass().getName() );
		}
	}

	public boolean isObfuscatedIdentifier( Entry obfEntry )
	{
		if( obfEntry instanceof ClassEntry )
		{
			if( obfEntry.getName().indexOf( '$' ) >= 0 )
			{
				String[] parts = obfEntry.getName().split( "\\$" );
				assert( parts.length == 2 ); // not supporting recursively-nested classes
				String outerClassName = parts[0];
				String innerClassName = parts[1];
				
				// both classes must be in the list
				return m_jarIndex.getObfClassNames().contains( outerClassName )
					&& m_jarIndex.getObfClassNames().contains( innerClassName );
			}
			else
			{
				// class must be in the list
				return m_jarIndex.getObfClassNames().contains( obfEntry.getName() );
			}
		}
		else
		{
			return isObfuscatedIdentifier( obfEntry.getClassEntry() );
		}
	}
}
