/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.representation.LocalVariableEntry;
import cuchaz.enigma.translation.representation.MethodEntry;

public class LocalVariableMapping implements Comparable<LocalVariableMapping> {

	private int index;
	private String name;

	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public LocalVariableMapping(int index, String name) {
		this.index = index;
		this.name = NameValidator.validateArgumentName(name);
	}

	public LocalVariableMapping(LocalVariableMapping other) {
		this.index = other.index;
		this.name = other.name;
	}

	public int getIndex() {
		return this.index;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String val) {
		this.name = NameValidator.validateArgumentName(val);
	}

	@Deprecated
	public LocalVariableEntry getObfEntry(MethodEntry methodEntry) {
		return new LocalVariableEntry(methodEntry, index, name);
	}

	public LocalVariableEntry getObfEntry(MethodEntry methodEntry, boolean parameter) {
		return new LocalVariableEntry(methodEntry, index, name, parameter);
	}

	@Override
	public int compareTo(LocalVariableMapping other) {
		return Integer.compare(this.index, other.index);
	}
}
