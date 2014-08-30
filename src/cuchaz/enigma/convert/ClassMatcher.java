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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;

import javassist.CtBehavior;
import javassist.CtClass;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.MethodMapping;

public class ClassMatcher
{
	public static void main( String[] args )
	throws IOException, MappingParseException
	{
		// TEMP
		JarFile sourceJar = new JarFile( new File( "input/1.8-pre1.jar" ) );
		JarFile destJar = new JarFile( new File( "input/1.8-pre2.jar" ) );
		File inMappingsFile = new File( "../minecraft-mappings/1.8-pre.mappings" );
		File outMappingsFile = new File( "../minecraft-mappings/1.8-pre2.mappings" );
		
		// do the conversion
		Mappings mappings = new MappingsReader().read( new FileReader( inMappingsFile ) );
		convertMappings( sourceJar, destJar, mappings );
		
		// write out the convert mappings
		FileWriter writer = new FileWriter( outMappingsFile );
		new MappingsWriter().write( writer, mappings );
		writer.close();
		System.out.println( "Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath() );
	}
	
	private static void convertMappings( JarFile sourceJar, JarFile destJar, Mappings mappings )
	{
		// index jars
		System.out.println( "Indexing source jar..." );
		JarIndex sourceIndex = new JarIndex();
		sourceIndex.indexJar( sourceJar, false );
		System.out.println( "Indexing dest jar..." );
		JarIndex destIndex = new JarIndex();
		destIndex.indexJar( destJar, false );
		TranslatingTypeLoader sourceLoader = new TranslatingTypeLoader( sourceJar, sourceIndex );
		TranslatingTypeLoader destLoader = new TranslatingTypeLoader( destJar, destIndex );

		// compute the matching
		ClassMatching matching = ClassMatcher.computeMatching( sourceIndex, sourceLoader, destIndex, destLoader );
		
		// start the class conversion map with the unique and ambiguous matchings
		Map<String,Map.Entry<ClassIdentity,List<ClassIdentity>>> conversionMap = matching.getConversionMap();
		
		// probabilistically match the unmatched source classes
		for( ClassIdentity sourceClass : new ArrayList<ClassIdentity>( matching.getUnmatchedSourceClasses() ) )
		{
			System.out.println( "No exact match for source class " + sourceClass.getClassEntry() );
			
			// find the closest classes
			TreeMap<Integer,ClassIdentity> scoredMatches = Maps.newTreeMap( Collections.reverseOrder() );
			for( ClassIdentity c : matching.getUnmatchedDestClasses() )
			{
				scoredMatches.put( sourceClass.getMatchScore( c ), c );
			}
			Iterator<Map.Entry<Integer,ClassIdentity>> iter = scoredMatches.entrySet().iterator();
			for( int i=0; i<10 && iter.hasNext(); i++ )
			{
				Map.Entry<Integer,ClassIdentity> score = iter.next();
				System.out.println( String.format( "\tScore: %3d   %s", score.getKey(), score.getValue().getClassEntry().getName() ) );
			}
			
			// does the best match have a non-zero score and the same name?
			Map.Entry<Integer,ClassIdentity> bestMatch = scoredMatches.firstEntry();
			if( bestMatch.getKey() > 0 && bestMatch.getValue().getClassEntry().equals( sourceClass.getClassEntry() ) )
			{
				// use it
				System.out.println( "\tAutomatically choosing likely match: " + bestMatch.getValue().getClassEntry().getName() );
				conversionMap.put(
					sourceClass.getClassEntry().getName(),
					new AbstractMap.SimpleEntry<ClassIdentity,List<ClassIdentity>>( sourceClass, Arrays.asList( bestMatch.getValue() ) )
				);
			}
		}
		
		// use the matching to convert the mappings
		BiMap<String,String> classConversion = HashBiMap.create();
		Set<String> unmatchedSourceClasses = Sets.newHashSet();
		for( String className : mappings.getAllObfClassNames() )
		{
			// is there a match for this class?
			Map.Entry<ClassIdentity,List<ClassIdentity>> entry = conversionMap.get( className );
			ClassIdentity sourceClass = entry.getKey();
			List<ClassIdentity> matches = entry.getValue();
			
			if( matches.isEmpty() )
			{
				// no match! =(
				unmatchedSourceClasses.add( className );
			}
			else if( matches.size() == 1 )
			{
				// unique match! We're good to go!
				classConversion.put(
					sourceClass.getClassEntry().getName(),
					matches.get( 0 ).getClassEntry().getName()
				);
			}
			else if( matches.size() > 1 )
			{
				// too many matches! =(
				unmatchedSourceClasses.add( className );
			}
		}
		
		// remove (and warn about) unmatched classes
		if( !unmatchedSourceClasses.isEmpty() )
		{
			System.err.println( "WARNING: there were unmatched classes!" );
			for( String className : unmatchedSourceClasses )
			{
				System.err.println( "\t" + className );
				mappings.removeClassByObfName( className );
			}
			System.err.println( "Mappings for these classes have been removed." );
		}
		
		// show the class name changes
		for( Map.Entry<String,String> entry : classConversion.entrySet() )
		{
			if( !entry.getKey().equals( entry.getValue() ) )
			{
				System.out.println( String.format( "Class change: %s -> %s", entry.getKey(), entry.getValue() ) );
				/* DEBUG
				System.out.println( String.format( "\n%s\n%s",
					new ClassIdentity( sourceLoader.loadClass( entry.getKey() ), null, sourceIndex, false, false ),
					new ClassIdentity( destLoader.loadClass( entry.getValue() ), null, destIndex, false, false )
				) );
				*/
			}
		}
		
		// TEMP: show some classes
		for( String className : Arrays.asList( "none/em", "none/ej", "none/en" ) )
		{
			System.out.println( String.format( "check: %s -> %s", className, classConversion.get( className ) ) );
		}
		
		// convert the classes
		mappings.renameObfClasses( classConversion );
		
		// look for method matches
		System.out.println( "Matching methods..." );
		for( ClassMapping classMapping : mappings.classes() )
		{
			ClassEntry classEntry = new ClassEntry( classMapping.getObfName() );
			for( MethodMapping methodMapping : classMapping.methods() )
			{
				// skip constructors
				if( methodMapping.getObfName().equals( "<init>" ) )
				{
					continue;
				}
				
				MethodEntry methodEntry = new MethodEntry(
					classEntry,
					methodMapping.getObfName(),
					methodMapping.getObfSignature()
				);
				if( !destIndex.isMethodImplemented( methodEntry ) )
				{
					System.err.println( "WARNING: method doesn't match: " + methodEntry );
					
					// show the available methods
					System.err.println( "\tAvailable dest methods:" );
					CtClass c = destLoader.loadClass( classMapping.getObfName() );
					for( CtBehavior behavior : c.getDeclaredBehaviors() )
					{
						MethodEntry declaredMethodEntry = new MethodEntry(
							new ClassEntry( classMapping.getObfName() ),
							behavior.getName(),
							behavior.getSignature()
						);
						System.err.println( "\t\t" + declaredMethodEntry );
					}
					
					System.err.println( "\tAvailable source methods:" );
					c = sourceLoader.loadClass( classConversion.inverse().get( classMapping.getObfName() ) );
					for( CtBehavior behavior : c.getDeclaredBehaviors() )
					{
						MethodEntry declaredMethodEntry = new MethodEntry(
							new ClassEntry( classMapping.getObfName() ),
							behavior.getName(),
							behavior.getSignature()
						);
						System.err.println( "\t\t" + declaredMethodEntry );
					}
				}
			}
		}
	}
	
	public static ClassMatching computeMatching( JarIndex sourceIndex, TranslatingTypeLoader sourceLoader, JarIndex destIndex, TranslatingTypeLoader destLoader )
	{
		System.out.println( "Matching classes..." );
		ClassMatching matching = null;
		for( boolean useRawNames : Arrays.asList( false/*, true*/ ) )
		{
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
						ClassIdentity sourceClass = new ClassIdentity( c, sourceNamer, sourceIndex, useReferences, useRawNames );
						matching.addSource( sourceClass );
					}
					for( ClassEntry classEntry : destClassEntries )
					{
						CtClass c = destLoader.loadClass( classEntry.getName() );
						ClassIdentity destClass = new ClassIdentity( c, destNamer, destIndex, useReferences, useRawNames );
						matching.matchDestClass( destClass );
					}
					
					// TEMP
					System.out.println( matching );
				}
				while( matching.getUniqueMatches().size() - numMatches > 0 );
			}
		}
		
		// DEBUG: check the class matches
		System.out.println( "Checking class matches..." );
		for( Map.Entry<ClassIdentity,ClassIdentity> entry : matching.getUniqueMatches().entrySet() )
		{
			// check source
			ClassIdentity sourceClass = entry.getKey();
			CtClass sourceC = sourceLoader.loadClass( sourceClass.getClassEntry().getName() );
			assert( sourceC != null )
				: "Unable to load source class " + sourceClass.getClassEntry();
			assert( sourceClass.matches( sourceC ) )
				: "Source " + sourceClass + " doesn't match " + new ClassIdentity( sourceC, null, sourceIndex, false, false );
			
			// check dest
			ClassIdentity destClass = entry.getValue();
			CtClass destC = destLoader.loadClass( destClass.getClassEntry().getName() );
			assert( destC != null )
				: "Unable to load dest class " + destClass.getClassEntry();
			assert( destClass.matches( destC ) )
				: "Dest " + destClass + " doesn't match " + new ClassIdentity( destC, null, destIndex, false, false );
		}
		
		// warn about the ambiguous matchings
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
			System.out.println( "Ambiguous matching:" );
			System.out.println( "\tSource: " + getClassNames( entry.getKey() ) );
			System.out.println( "\tDest:   " + getClassNames( entry.getValue() ) );
		}
		
		/* DEBUG
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
	
	private static List<String> getClassNames( Collection<ClassIdentity> classes )
	{
		List<String> out = Lists.newArrayList();
		for( ClassIdentity c : classes )
		{
			out.add( c.getClassEntry().getName() );
		}
		Collections.sort( out );
		return out;
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
