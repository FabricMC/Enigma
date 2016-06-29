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
package cuchaz.enigma.inputs.constructors;

// none/b
public class Caller {
	
	// a()V
	public void callBaseDefault() {
		// none/a.<init>()V
		System.out.println(new BaseClass());
	}
	
	// b()V
	public void callBaseInt() {
		// none/a.<init>(I)V
		System.out.println(new BaseClass(5));
	}
	
	// c()V
	public void callSubDefault() {
		// none/d.<init>()V
		System.out.println(new SubClass());
	}
	
	// d()V
	public void callSubInt() {
		// none/d.<init>(I)V
		System.out.println(new SubClass(6));
	}
	
	// e()V
	public void callSubIntInt() {
		// none/d.<init>(II)V
		System.out.println(new SubClass(4, 2));
	}
	
	// f()V
	public void callSubSubInt() {
		// none/e.<init>(I)V
		System.out.println(new SubSubClass(3));
	}
	
	// g()V
	public void callDefaultConstructable() {
		// none/c.<init>()V
		System.out.println(new DefaultConstructable());
	}
}
