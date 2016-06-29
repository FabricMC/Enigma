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

public class B_AnonymousWithScopeArgs {
	
	public static void foo(final D_Simple arg) {
		System.out.println(new Object() {
			@Override
			public String toString() {
				return arg.toString();
			}
		});
	}
}
