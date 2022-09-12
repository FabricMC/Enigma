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

package cuchaz.enigma.inputs.innerClasses;

@SuppressWarnings("unused")
public class C_ConstructorArgs {
	Inner i;

	public void foo() {
		i = new Inner(5);
	}

	class Inner {
		private int a;

		Inner(int a) {
			this.a = a;
		}
	}
}
