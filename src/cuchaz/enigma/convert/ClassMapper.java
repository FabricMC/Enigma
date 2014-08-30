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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;

import javassist.CtClass;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Maps;

import cuchaz.enigma.Constants;
import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;

public class ClassMapper
{
	public static void main( String[] args )
	throws IOException, MappingParseException
	{
		// TEMP
		JarFile fromJar = new JarFile( new File( "input/1.8-pre1.jar" ) );
		JarFile toJar = new JarFile( new File( "input/1.8-pre3.jar" ) );
		File inMappingsFile = new File( "../minecraft-mappings/1.8-pre.mappings" );
		File outMappingsFile = new File( "../minecraft-mappings/1.8-pre3.mappings" );
		
		// compute the matching
		ClassMatching matching = ClassMapper.computeMatching( fromJar, toJar );
		
		// use the matching to convert the mappings
		Mappings mappings = new MappingsReader().read( new FileReader( inMappingsFile ) );
		Map<String,Map.Entry<ClassIdentity,List<ClassIdentity>>> conversionMap = matching.getConversionMap();
		Map<String,String> finalConversion = Maps.newHashMap();
		Set<String> unmatchedSourceClasses = Sets.newHashSet();
		for( ClassMapping classMapping : mappings.classes() )
		{
			// is there a match for this class?
			Map.Entry<ClassIdentity,List<ClassIdentity>> entry = conversionMap.get( classMapping.getObfName() );
			ClassIdentity sourceClass = entry.getKey();
			List<ClassIdentity> matches = entry.getValue();
			
			if( matches.isEmpty() )
			{
				// no match! =(
				System.out.println( "No exact match for source class " + classMapping.getObfName() );
				
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
					addFinalConversion( finalConversion, sourceClass, bestMatch.getValue() );
				}
				else
				{
					unmatchedSourceClasses.add( classMapping.getObfName() );
				}
			}
			if( matches.size() == 1 )
			{
				// unique match! We're good to go!
				addFinalConversion( finalConversion, sourceClass, matches.get( 0 ) );
			}
			else if( matches.size() > 1 )
			{
				// too many matches! =(
				unmatchedSourceClasses.add( classMapping.getObfName() );
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
		for( Map.Entry<String,String> entry : finalConversion.entrySet() )
		{
			if( !entry.getKey().equals( entry.getValue() ) )
			{
				System.out.println( String.format( "Class change: %s -> %s", entry.getKey(), entry.getValue() ) );
			}
		}
		
		// do the final conversion
		mappings.renameObfClasses( finalConversion );
		FileWriter writer = new FileWriter( outMappingsFile );
		new MappingsWriter().write( writer, mappings );
		writer.close();
		System.out.println( "Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath() );
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
		for( boolean useRawNames : Arrays.asList( false, true ) )
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
	
	private static void addFinalConversion( Map<String,String> finalConversion, ClassIdentity sourceClass, ClassIdentity destClass )
	{
		// flatten inner classes since these are all obf classes in the none package
		String sourceClassName = sourceClass.getClassEntry().getName();
		if( sourceClass.getClassEntry().isInnerClass() )
		{
			sourceClassName = Constants.NonePackage + "/" + sourceClass.getClassEntry().getInnerClassName();
		}
		
		String destClassName = destClass.getClassEntry().getName();
		if( destClass.getClassEntry().isInnerClass() )
		{
			destClassName = Constants.NonePackage + "/" + destClass.getClassEntry().getInnerClassName();
		}
		
		finalConversion.put( sourceClassName, destClassName );
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
