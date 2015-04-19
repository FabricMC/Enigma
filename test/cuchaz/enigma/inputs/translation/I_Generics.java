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

import java.util.List;
import java.util.Map;


public class I_Generics {
	
	public class A_Type {
	}
	
	public List<Integer> f1;
	public List<A_Type> f2;
	public Map<A_Type,A_Type> f3;
	
	public class B_Generic<T> {
		public T f4;
		public T m1() {
			return null;
		}
	}
	
	public B_Generic<Integer> f5;
	public B_Generic<A_Type> f6;
}
