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

// none/a
public abstract class BaseClass {
	
	// a
	private String m_name;
	
	// <init>(Ljava/lang/String;)V
	protected BaseClass(String name) {
		m_name = name;
	}
	
	// a()Ljava/lang/String;
	public String getName() {
		return m_name;
	}
	
	// a()V
	public abstract void doBaseThings();
}
