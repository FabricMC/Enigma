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
package cuchaz.enigma.inputs.inheritanceTree;

// none/c extends none/a
public class SubclassB extends BaseClass {
	
	// a
	private int m_numThings;
	
	// <init>()V
	protected SubclassB() {
		// none/a.<init>(Ljava/lang/String;)V
		super("B");
		
		// access to a
		m_numThings = 4;
	}
	
	@Override
	// a()V
	public void doBaseThings() {
		// call to none/a.a()Ljava/lang/String;
		System.out.println("Base things by B! " + getName());
	}
	
	// b()V
	public void doBThings() {
		// access to a
		System.out.println("" + m_numThings + " B things!");
	}
}
