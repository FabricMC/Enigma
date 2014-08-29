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
import java.util.jar.JarFile;

import javassist.CtClass;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import cuchaz.enigma.analysis.JarClassIterator;

public class ClassMapper
{
	public static void main( String[] args )
	throws IOException
	{
		// TEMP
		JarFile fromJar = new JarFile( new File( "input/1.8-pre1.jar" ) );
		JarFile toJar = new JarFile( new File( "input/1.8-pre2.jar" ) );
		
		new ClassMapper( fromJar, toJar );
	}
	
	public ClassMapper( JarFile a, JarFile b )
	{
		int numAClasses = JarClassIterator.getClassEntries( a ).size();
		int numBClasses = JarClassIterator.getClassEntries( b ).size();
		
		// TEMP
		System.out.println( "A classes: " + numAClasses );
		System.out.println( "B classes: " + numBClasses );
		
		// compute the a classes
		Multiset<ClassIdentity> aclasses = HashMultiset.create();
		for( CtClass c : JarClassIterator.classes( a ) )
		{
			ClassIdentity aclass = new ClassIdentity( c );
			aclasses.add( aclass );
		}
		
		int numMatches = 0;
		
		// match the b classes to the a classes
		for( CtClass c : JarClassIterator.classes( b ) )
		{
			ClassIdentity bclass = new ClassIdentity( c );
			if( aclasses.contains( bclass ) )
			{
				numMatches++;
			}
			
			// TEMP
			//System.out.println( bclass );
		}
		
		// TEMP
		System.out.println( String.format( "Class matches: %d/%d (missing %d)",
			numMatches, aclasses.size(), aclasses.size() - numMatches
		) );
	}
}
