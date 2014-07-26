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

import cuchaz.enigma.analysis.Analyzer;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.gui.ClassSelectionHandler;
import cuchaz.enigma.gui.Gui;

public class Main
{
	public static void main( String[] args )
	throws Exception
	{
		startGui();
	}
	
	private static void startGui( )
	throws Exception
	{
		final Gui gui = new Gui();
		
		// settings
		final File jarFile = new File( "/home/jeff/.minecraft/versions/1.7.10/1.7.10.jar" );
		gui.setTitle( jarFile.getName() );
		
		// init the deobfuscator
		final Deobfuscator deobfuscator = new Deobfuscator( jarFile );
		gui.setObfClasses( deobfuscator.getObfuscatedClasses() );
		
		// handle events
		gui.setClassSelectionHandler( new ClassSelectionHandler( )
		{
			@Override
			public void classSelected( final ClassFile classFile )
			{
				gui.setSource( "(deobfuscating...)" );
				
				// run the deobfuscator in a separate thread so we don't block the GUI event queue
				new Thread( )
				{
					@Override
					public void run( )
					{
						String source = deobfuscator.getSource( classFile );
						SourceIndex index = Analyzer.analyze( classFile.getName(), source );
						gui.setSource( source, index );
					}
				}.start();
			}
		} );
	}
}
