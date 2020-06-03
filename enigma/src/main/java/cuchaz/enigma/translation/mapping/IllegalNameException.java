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

public class IllegalNameException extends RuntimeException {

	private String name;
	private String reason;

	public IllegalNameException(String name, String reason) {
		this.name = name;
		this.reason = reason;
	}

	public String getReason() {
		return this.reason;
	}

	@Override
	public String getMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("Illegal name: ");
		buf.append(this.name);
		if (this.reason != null) {
			buf.append(" because ");
			buf.append(this.reason);
		}
		return buf.toString();
	}
}
