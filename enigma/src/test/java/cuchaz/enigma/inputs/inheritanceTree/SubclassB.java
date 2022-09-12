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

// c extends a
public class SubclassB extends BaseClass {
	// a
	private int numThings;

	// <init>()V
	protected SubclassB() {
		// a.<init>(Ljava/lang/String;)V
		super("B");

		// access to a
		numThings = 4;
	}

	@Override
	// a()V
	public void doBaseThings() {
		// call to a.a()Ljava/lang/String;
		System.out.println("Base things by B! " + getName());
	}

	// b()V
	public void doBThings() {
		// access to a
		System.out.println("" + numThings + " B things!");
	}
}
