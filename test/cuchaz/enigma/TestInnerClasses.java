/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.analysis.JarIndex;

public class TestInnerClasses
{
	private JarIndex m_index;
	
	private static final String AnonymousOuter = "none/a";
	private static final String AnonymousInner = "none/b";
	private static final String SimpleOuter = "none/e";
	private static final String SimpleInner = "none/f";
	private static final String ConstructorArgsOuter = "none/c";
	private static final String ConstructorArgsInner = "none/d";
	
	public TestInnerClasses( )
	throws Exception
	{
		m_index = new JarIndex();
		m_index.indexJar( new JarFile( "build/libs/testInnerClasses.obf.jar" ), true );
	}
	
	@Test
	public void simple( )
	{
		assertThat( m_index.getOuterClass( SimpleInner ), is( SimpleOuter ) );
		assertThat( m_index.getInnerClasses( SimpleOuter ), containsInAnyOrder( SimpleInner ) );
		assertThat( m_index.isAnonymousClass( SimpleInner ), is( false ) );
	}
	
	@Test
	public void anonymous( )
	{
		assertThat( m_index.getOuterClass( AnonymousInner ), is( AnonymousOuter ) );
		assertThat( m_index.getInnerClasses( AnonymousOuter ), containsInAnyOrder( AnonymousInner ) );
		assertThat( m_index.isAnonymousClass( AnonymousInner ), is( true ) );
	}

	@Test
	public void constructorArgs( )
	{
		assertThat( m_index.getOuterClass( ConstructorArgsInner ), is( ConstructorArgsOuter ) );
		assertThat( m_index.getInnerClasses( ConstructorArgsOuter ), containsInAnyOrder( ConstructorArgsInner ) );
		assertThat( m_index.isAnonymousClass( ConstructorArgsInner ), is( false ) );
	}
}
