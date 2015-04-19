/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

public class ExceptionIgnorer {

	public static boolean shouldIgnore(Throwable t) {
		
		// is this that pesky concurrent access bug in the highlight painter system?
		// (ancient ui code is ancient)
		if (t instanceof ArrayIndexOutOfBoundsException) {
			StackTraceElement[] stackTrace = t.getStackTrace();
			if (stackTrace.length > 1) {
			
				// does this stack frame match javax.swing.text.DefaultHighlighter.paint*() ?
				StackTraceElement frame = stackTrace[1];
				if (frame.getClassName().equals("javax.swing.text.DefaultHighlighter") && frame.getMethodName().startsWith("paint")) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}
