/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
*     Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.inputs.constructors;

// a
public class BaseClass {
	// <init>()V
	public BaseClass() {
		System.out.println("Default constructor");
	}

	// <init>(I)V
	public BaseClass(int i) {
		System.out.println("Int constructor " + i);
	}
}
