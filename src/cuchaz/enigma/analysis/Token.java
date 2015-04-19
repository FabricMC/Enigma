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
package cuchaz.enigma.analysis;

public class Token implements Comparable<Token> {
	
	public int start;
	public int end;
	public String text;
	
	public Token(int start, int end) {
		this(start, end, null);
	}
	
	public Token(int start, int end, String source) {
		this.start = start;
		this.end = end;
		if (source != null) {
			this.text = source.substring(start, end);
		}
	}
	
	public boolean contains(int pos) {
		return pos >= start && pos <= end;
	}
	
	@Override
	public int compareTo(Token other) {
		return start - other.start;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Token) {
			return equals((Token)other);
		}
		return false;
	}
	
	public boolean equals(Token other) {
		return start == other.start && end == other.end;
	}
	
	@Override
	public String toString() {
		return String.format("[%d,%d]", start, end);
	}
}
