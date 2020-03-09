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

package cuchaz.enigma.analysis;

public class Token implements Comparable<Token> {

	public int start;
	public int end;
	public String text;

	public Token(int start, int end, String text) {
		this.start = start;
		this.end = end;
		this.text = text;
	}

	public int getRenameOffset(String to) {
		int length = this.end - this.start;
		return to.length() - length;
	}

	public void rename(StringBuffer source, String to) {
		int oldEnd = this.end;
		this.text = to;
		this.end = this.start + to.length();

		source.replace(start, oldEnd, to);
	}

	public Token move(int offset) {
		Token token = new Token(this.start + offset, this.end + offset, null);
		token.text = text;
		return token;
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
		return other instanceof Token && equals((Token) other);
	}

	@Override
	public int hashCode() {
		return start * 37 + end;
	}

	public boolean equals(Token other) {
		return start == other.start && end == other.end && text.equals(other.text);
	}

	@Override
	public String toString() {
		return String.format("[%d,%d]", start, end);
	}
}
