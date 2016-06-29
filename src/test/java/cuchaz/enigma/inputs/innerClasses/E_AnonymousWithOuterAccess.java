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
package cuchaz.enigma.inputs.innerClasses;

public class E_AnonymousWithOuterAccess {
	
	// reproduction of error case documented at:
	// https://bitbucket.org/cuchaz/enigma/issue/61/stackoverflowerror-when-deobfuscating
	
	public Object makeInner() {
		outerMethod();
		return new Object() {
			@Override
			public String toString() {
				return outerMethod();
			}
		};
	}
	
	private String outerMethod() {
		return "foo";
	}
}
