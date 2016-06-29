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

import java.util.ArrayList;
import java.util.List;

public class D_AnonymousTesting {
	
	public List<Object> getObjs() {
		List<Object> objs = new ArrayList<Object>();
		objs.add(new Object() {
			@Override
			public String toString() {
				return "Object!";
			}
		});
		return objs;
	}
}
