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

public class IllegalNameException extends RuntimeException {
	
	private static final long serialVersionUID = -2279910052561114323L;
	
	private String m_name;
	private String m_reason;
	
	public IllegalNameException(String name) {
		this(name, null);
	}
	
	public IllegalNameException(String name, String reason) {
		m_name = name;
		m_reason = reason;
	}
	
	public String getReason() {
		return m_reason;
	}
	
	@Override
	public String getMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("Illegal name: ");
		buf.append(m_name);
		if (m_reason != null) {
			buf.append(" because ");
			buf.append(m_reason);
		}
		return buf.toString();
	}
}
