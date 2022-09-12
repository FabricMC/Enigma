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

package cuchaz.enigma.inputs.inheritanceTree;

// a
public abstract class BaseClass {
	// a
	private String name;

	// <init>(Ljava/lang/String;)V
	protected BaseClass(String name) {
		this.name = name;
	}

	// a()Ljava/lang/String;
	public String getName() {
		return name;
	}

	// a()V
	public abstract void doBaseThings();
}
