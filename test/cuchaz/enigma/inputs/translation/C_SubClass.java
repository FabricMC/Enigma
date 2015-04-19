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
package cuchaz.enigma.inputs.translation;

public class C_SubClass extends B_BaseClass {
	
	public char f2; // shadows B_BaseClass.f2
	public int f3;
	public int f4;
	
	@Override
	public int m1() {
		return 32;
	}
	
	public int m3() {
		return 7;
	}
}
