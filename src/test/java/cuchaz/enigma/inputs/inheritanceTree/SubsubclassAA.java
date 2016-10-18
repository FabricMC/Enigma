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

// d extends b
public class SubsubclassAA extends SubclassA {
	
	protected SubsubclassAA() {
		// call to b.<init>(Ljava/lang/String;)V
		super("AA");
	}
	
	@Override
	// a()Ljava/lang/String;
	public String getName() {
		// call to b.a()Ljava/lang/String;
		return "subsub" + super.getName();
	}
	
	@Override
	// a()V
	public void doBaseThings() {
		// call to d.a()Ljava/lang/String;
		System.out.println("Base things by " + getName());
	}
}
