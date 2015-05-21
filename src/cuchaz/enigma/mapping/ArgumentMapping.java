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
package cuchaz.enigma.mapping;

import java.io.Serializable;

public class ArgumentMapping implements Serializable, Comparable<ArgumentMapping> {
	
	private static final long serialVersionUID = 8610742471440861315L;
	
	private int m_index;
	private String m_name;
	
	// NOTE: this argument order is important for the MethodReader/MethodWriter
	public ArgumentMapping(int index, String name) {
		m_index = index;
		m_name = NameValidator.validateArgumentName(name);
	}
	
	public ArgumentMapping(ArgumentMapping other) {
		m_index = other.m_index;
		m_name = other.m_name;
	}

	public int getIndex() {
		return m_index;
	}
	
	public String getName() {
		return m_name;
	}
	
	public void setName(String val) {
		m_name = NameValidator.validateArgumentName(val);
	}
	
	@Override
	public int compareTo(ArgumentMapping other) {
		return Integer.compare(m_index, other.m_index);
	}
}
