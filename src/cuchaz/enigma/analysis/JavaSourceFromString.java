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
package cuchaz.enigma.analysis;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class JavaSourceFromString extends SimpleJavaFileObject
{
	private final String m_source;
	
	JavaSourceFromString( String name, String source )
	{
		super( URI.create( "string:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
		m_source = source;
	}
	
	public CharSequence getCharContent( boolean ignoreEncodingErrors )
	{
		return m_source;
	}
}
