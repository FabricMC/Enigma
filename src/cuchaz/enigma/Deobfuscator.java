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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.jar.JarFile;

import javassist.bytecode.Descriptor;

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
	public interface ProgressListener
	{
		void init( int totalWork );
		void onProgress( int numDone, String message );
	}
	
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
		for( ClassEntry obfClassEntry : m_jarIndex.getObfClassEntries() )
		{
			// skip inner classes
			if( m_jarIndex.getOuterClass( obfClassEntry.getName() ) != null )
			{
				continue;
			}
			
			// separate the classes
			ClassEntry deobfClassEntry = deobfuscateEntry( obfClassEntry );
			if( !deobfClassEntry.equals( obfClassEntry ) )
			{
				// if the class has a mapping, clearly it's deobfuscated
				deobfClasses.add( deobfClassEntry.getName() );
			}
			else if( !obfClassEntry.getPackageName().equals( "default" ) )
			{
				// also call it deobufscated if it's not in the "default" package
				deobfClasses.add( obfClassEntry.getName() );
			}
			else
			{
				// otherwise, assume it's still obfuscated
				obfClasses.add( obfClassEntry.getName() );
			}
		}
	}
	
	public CompilationUnit getSourceTree( String className )
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
		return builder.getCompilationUnit();
	}
	
	public SourceIndex getSourceIndex( CompilationUnit sourceTree, String source )
	{
		// build the source index
		SourceIndex index = new SourceIndex( source );
		sourceTree.acceptVisitor( new SourceIndexVisitor(), index );
		
		// DEBUG
		//root.acceptVisitor( new TreeDumpVisitor( new File( "tree.txt" ) ), null );

		/* DEBUG
		for( Token token : index.referenceTokens() )
		{
			EntryReference<Entry,Entry> reference = index.getDeobfReference( token );
			System.out.println( token + " -> " + reference + " -> " + index.getReferenceToken( reference ) );
		}
		*/
		
		return index;
	}
	
	public String getSource( CompilationUnit sourceTree )
	{
		// render the AST into source
		StringWriter buf = new StringWriter();
		sourceTree.acceptVisitor( new InsertParenthesesVisitor(), null );
		sourceTree.acceptVisitor( new JavaOutputVisitor( new PlainTextOutput( buf ), m_settings ), null );
		return buf.toString();
	}
	
	public void writeSources( File dirOut, ProgressListener progress )
	throws IOException
	{
		int numClasses = m_jarIndex.getObfClassEntries().size();
		if( progress != null )
		{
			progress.init( numClasses );
		}
		int i = 0;
		
		// DEOBFUSCATE ALL THE THINGS!! @_@
		for( ClassEntry obfClassEntry : m_jarIndex.getObfClassEntries() )
		{
			// skip inner classes
			if( obfClassEntry.isInnerClass() )
			{
				continue;
			}
			
			ClassEntry deobfClassEntry = deobfuscateEntry( new ClassEntry( obfClassEntry ) );
			if( progress != null )
			{
				progress.onProgress( i++, deobfClassEntry.toString() );
			}
			
			// get the source
			String source = getSource( getSourceTree( obfClassEntry.getName() ) );
			
			// write the file
			File file = new File( dirOut, deobfClassEntry.getName().replace( '.', '/' ) + ".java" );
			file.getParentFile().mkdirs();
			try( FileWriter out = new FileWriter( file ) )
			{
				out.write( source );
			}
		}
		
		// done!
		progress.onProgress( numClasses, "Done!" );
	}
	
	public <T extends Entry> T obfuscateEntry( T deobfEntry )
	{
		if( deobfEntry == null )
		{
			return null;
		}
		return getTranslator( TranslationDirection.Obfuscating ).translateEntry( deobfEntry );
	}
	
	public <T extends Entry> T deobfuscateEntry( T obfEntry )
	{
		if( obfEntry == null )
		{
			return null;
		}
		return getTranslator( TranslationDirection.Deobfuscating ).translateEntry( obfEntry );
	}
	
	public <E extends Entry,C extends Entry> EntryReference<E,C> obfuscateReference( EntryReference<E,C> deobfReference )
	{
		if( deobfReference == null )
		{
			return null;
		}
		return new EntryReference<E,C>(
			obfuscateEntry( deobfReference.entry ),
			obfuscateEntry( deobfReference.context )
		);
	}
	
	public <E extends Entry,C extends Entry> EntryReference<E,C> deobfuscateReference( EntryReference<E,C> obfReference )
	{
		if( obfReference == null )
		{
			return null;
		}
		return new EntryReference<E,C>(
			deobfuscateEntry( obfReference.entry ),
			deobfuscateEntry( obfReference.context )
		);
	}
	
	// NOTE: these methods are a bit messy... oh well

	public void rename( Entry obfEntry, String newName )
	{
		if( obfEntry instanceof ClassEntry )
		{
			m_renamer.setClassName( (ClassEntry)obfEntry, Descriptor.toJvmName( newName ) );
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
			ClassEntry obfClassEntry = (ClassEntry)obfEntry;
			if( obfClassEntry.isInnerClass() )
			{
				// both classes must be in the list
				return m_jarIndex.getObfClassEntries().contains( obfClassEntry.getOuterClassEntry() )
					&& m_jarIndex.getObfClassEntries().contains( obfClassEntry.getInnerClassName() );
				// TODO: make sure this works for the inner class!!
			}
			else
			{
				// class must be in the list
				return m_jarIndex.getObfClassEntries().contains( obfEntry );
			}
		}
		else
		{
			return isObfuscatedIdentifier( obfEntry.getClassEntry() );
		}
	}
}
