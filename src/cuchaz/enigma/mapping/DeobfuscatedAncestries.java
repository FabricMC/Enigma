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

import java.util.Map;

public class DeobfuscatedAncestries extends Ancestries
{
	private static final long serialVersionUID = 8316248774892618324L;
	
	private Ancestries m_ancestries;
	private Map<String,ClassMapping> m_classesByObf;
	private Map<String,ClassMapping> m_classesByDeobf;
	
	protected DeobfuscatedAncestries( Ancestries ancestries, Map<String,ClassMapping> classesByObf, Map<String,ClassMapping> classesByDeobf )
	{
		m_ancestries = ancestries;
		m_classesByObf = classesByObf;
		m_classesByDeobf = classesByDeobf;
	}
	
	@Override
	public String getSuperclassName( String deobfClassName )
	{
		// obfuscate the class name
		ClassMapping classIndex = m_classesByDeobf.get( deobfClassName );
		if( classIndex == null )
		{
			return null;
		}
		String obfClassName = classIndex.getObfName();
		
		// get the superclass
		String obfSuperclassName = m_ancestries.getSuperclassName( obfClassName );
		if( obfSuperclassName == null )
		{
			return null;
		}
		
		// deobfuscate the superclass name
		classIndex = m_classesByObf.get( obfSuperclassName );
		if( classIndex == null )
		{
			return null;
		}
		
		return classIndex.getDeobfName();
	}
}
