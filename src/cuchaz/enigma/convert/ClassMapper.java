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
package cuchaz.enigma.convert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javassist.CtClass;
import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;

public class ClassMapper
{
	public static void main( String[] args )
	throws IOException
	{
		// TEMP
		JarFile fromJar = new JarFile( new File( "input/1.8-pre1.jar" ) );
		JarFile toJar = new JarFile( new File( "input/1.8-pre2.jar" ) );
		
		// compute the matching
		ClassMatching matching = ClassMapper.computeMatching( fromJar, toJar );
		
		// TODO: use the matching to convert the mappings
	}
	
	public static ClassMatching computeMatching( JarFile sourceJar, JarFile destJar )
	{
		// index jars
		System.out.println( "Indexing source jar..." );
		JarIndex sourceIndex = new JarIndex();
		sourceIndex.indexJar( sourceJar );
		System.out.println( "Indexing dest jar..." );
		JarIndex destIndex = new JarIndex();
		destIndex.indexJar( destJar );
		
		System.out.println( "Computing matching..." );
		
		TranslatingTypeLoader sourceLoader = new TranslatingTypeLoader( sourceJar, sourceIndex );
		TranslatingTypeLoader destLoader = new TranslatingTypeLoader( destJar, destIndex );

		ClassMatching matching = null;
		for( boolean useReferences : Arrays.asList( false, true ) )
		{
			int numMatches = 0;
			do
			{
				SidedClassNamer sourceNamer = null;
				SidedClassNamer destNamer = null;
				if( matching != null )
				{
					// build a class namer
					ClassNamer namer = new ClassNamer( matching.getUniqueMatches() );
					sourceNamer = namer.getSourceNamer();
					destNamer = namer.getDestNamer();
					
					// note the number of matches
					numMatches = matching.getUniqueMatches().size();
				}
				
				// get the entries left to match
				Set<ClassEntry> sourceClassEntries = sourceIndex.getObfClassEntries();
				Set<ClassEntry> destClassEntries = destIndex.getObfClassEntries();
				if( matching != null )
				{
					sourceClassEntries.clear();
					destClassEntries.clear();
					for( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : matching.getAmbiguousMatches().entrySet() )
					{
						for( ClassIdentity c : entry.getKey() )
						{
							sourceClassEntries.add( c.getClassEntry() );
							matching.removeSource( c );
						}
						for( ClassIdentity c : entry.getValue() )
						{
							destClassEntries.add( c.getClassEntry() );
							matching.removeDest( c );
						}
					}
					for( ClassIdentity c : matching.getUnmatchedSourceClasses() )
					{
						sourceClassEntries.add( c.getClassEntry() );
						matching.removeSource( c );
					}
					for( ClassIdentity c : matching.getUnmatchedDestClasses() )
					{
						destClassEntries.add( c.getClassEntry() );
						matching.removeDest( c );
					}
				}
				else
				{
					matching = new ClassMatching();
				}
				
				// compute a matching for the classes
				for( ClassEntry classEntry : sourceClassEntries )
				{
					CtClass c = sourceLoader.loadClass( classEntry.getName() );
					ClassIdentity sourceClass = new ClassIdentity( c, sourceNamer, sourceIndex, useReferences );
					matching.addSource( sourceClass );
				}
				for( ClassEntry classEntry : destClassEntries )
				{
					CtClass c = destLoader.loadClass( classEntry.getName() );
					ClassIdentity destClass = new ClassIdentity( c, destNamer, destIndex, useReferences );
					matching.matchDestClass( destClass );
				}
				
				// TEMP
				System.out.println( matching );
			}
			while( matching.getUniqueMatches().size() - numMatches > 0 );
		}
		
		/* DEBUG: show some ambiguous matches
		List<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>> ambiguousMatches = new ArrayList<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>>( matching.getAmbiguousMatches().entrySet() );
		Collections.sort( ambiguousMatches, new Comparator<Map.Entry<List<ClassIdentity>,List<ClassIdentity>>>( )
		{
			@Override
			public int compare( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> a, Map.Entry<List<ClassIdentity>,List<ClassIdentity>> b )
			{
				String aName = a.getKey().get( 0 ).getClassEntry().getName();
				String bName = b.getKey().get( 0 ).getClassEntry().getName();
				return aName.compareTo( bName );
			}
		} );
		for( Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry : ambiguousMatches )
		{
			for( ClassIdentity c : entry.getKey() )
			{
				System.out.print( c.getClassEntry().getName() + " " );
			}
			System.out.println();
		}
		Map.Entry<List<ClassIdentity>,List<ClassIdentity>> entry = ambiguousMatches.get( 7 );
		for( ClassIdentity c : entry.getKey() )
		{
			System.out.println( c );
		}
		for( ClassIdentity c : entry.getKey() )
		{
			System.out.println( decompile( sourceLoader, c.getClassEntry() ) );
		}
		*/
		
		return matching;
	}
	
	/* DEBUG
	private static String decompile( TranslatingTypeLoader loader, ClassEntry classEntry )
	{
		PlainTextOutput output = new PlainTextOutput();
		DecompilerSettings settings = DecompilerSettings.javaDefaults();
		settings.setForceExplicitImports( true );
		settings.setShowSyntheticMembers( true );
		settings.setTypeLoader( loader );
		Decompiler.decompile( classEntry.getName(), output, settings );
		return output.toString();
	}
	*/
}
