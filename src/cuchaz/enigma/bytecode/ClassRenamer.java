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
package cuchaz.enigma.bytecode;

import java.util.Map;
import java.util.Set;

import javassist.ClassMap;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.InnerClassesAttribute;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.SignatureUpdater;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class ClassRenamer
{
	public static void renameClasses( CtClass c, Map<ClassEntry,ClassEntry> map )
	{
		// build the map used by javassist
		ClassMap nameMap = new ClassMap();
		for( Map.Entry<ClassEntry,ClassEntry> entry : map.entrySet() )
		{
			nameMap.put( entry.getKey().getName(), entry.getValue().getName() );
		}
		c.replaceClassName( nameMap );
		
		// translate the names in the InnerClasses attribute
		ConstPool constants = c.getClassFile().getConstPool();
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute( InnerClassesAttribute.tag );
		if( attr != null )
		{
			for( int i=0; i<attr.tableLength(); i++ )
			{
				ClassEntry inClassEntry = new ClassEntry( Descriptor.toJvmName( attr.innerClass( i ) ) );
				ClassEntry outClassEntry = map.get( inClassEntry );
				if( outClassEntry == null )
				{
					continue;
				}
				attr.setInnerClassIndex( i, constants.addClassInfo( outClassEntry.getName() ) );
				if( attr.outerClassIndex( i ) != 0 )
				{
					attr.setOuterClassIndex( i, constants.addClassInfo( outClassEntry.getOuterClassName() ) );
				}
				if( attr.innerNameIndex( i ) != 0 )
				{
					attr.setInnerNameIndex( i, constants.addUtf8Info( outClassEntry.getInnerClassName() ) );
				}
				
				/* DEBUG
				System.out.println( String.format( "\tOBF: %s DEOBF: %s-> ATTR: %s,%s,%s",
					obfClassEntry, deobfClassEntry,
					attr.outerClass( i ),
					attr.innerClass( i ),
					attr.innerName( i )
				) );
				*/
			}
		}
	}
	
	public static Set<ClassEntry> getAllClassEntries( final CtClass c )
	{
		// get the classes that javassist knows about
		final Set<ClassEntry> entries = Sets.newHashSet();
		ClassMap map = new ClassMap( )
		{
			@Override
			public Object get( Object obj )
			{
				if( obj instanceof String )
				{
					String str = (String)obj;
					
					// javassist throws a lot of weird things at this map
					// I either have to implement my on class scanner, or just try to filter out the weirdness
					// I'm opting to filter out the weirdness for now
					
					// skip anything with generic arguments
					if( str.indexOf( '<' ) >= 0 || str.indexOf( '>' ) >= 0 || str.indexOf( ';' ) >= 0 )
					{
						return null;
					}
					
					// convert path/to/class.inner to path/to/class$inner
					str = str.replace( '.', '$' );
					
					// remember everything else
					entries.add( new ClassEntry( str ) );
				}
				return null;
			}
			private static final long serialVersionUID = -202160293602070641L;
		};
		c.replaceClassName( map );
		
		// also check InnerClassesAttribute
		InnerClassesAttribute attr = (InnerClassesAttribute)c.getClassFile().getAttribute( InnerClassesAttribute.tag );
		if( attr != null )
		{
			for( int i=0; i<attr.tableLength(); i++ )
			{
				entries.add( new ClassEntry( Descriptor.toJvmName( attr.innerClass( i ) ) ) );
			}
		}
		
		return entries;
	}
	
	public static void moveAllClassesOutOfDefaultPackage( CtClass c, String newPackageName )
	{
		// rename all classes
		Map<ClassEntry,ClassEntry> map = Maps.newHashMap();
		for( ClassEntry classEntry : ClassRenamer.getAllClassEntries( c ) )
		{
			if( classEntry.isInDefaultPackage() )
			{
				map.put( classEntry, new ClassEntry( newPackageName + "/" + classEntry.getName() ) );
			}
		}
		ClassRenamer.renameClasses( c, map );
		
		// TEMP
		for( ClassEntry classEntry : ClassRenamer.getAllClassEntries( c ) )
		{
			if( classEntry.isInDefaultPackage() )
			{
				throw new Error( "!!! " + classEntry );
			}
		}
		
		// TEMP
		for( CtBehavior behavior : c.getDeclaredBehaviors() )
		{
			if( behavior.getSignature() == null )
			{
				continue;
			}
			
			SignatureUpdater.update( behavior.getSignature(), new ClassNameUpdater( )
			{
				@Override
				public String update( String className )
				{
					ClassEntry classEntry = new ClassEntry( className );
					if( classEntry.isInDefaultPackage() )
					{
						throw new Error( "!!! " + className );
					}
					return className;
				}
			} );
		}
	}
}
