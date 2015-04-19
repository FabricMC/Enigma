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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import com.google.common.collect.Lists;

public class SignatureUpdater {
	
	public interface ClassNameUpdater {
		String update(String className);
	}
	
	public static String update(String signature, ClassNameUpdater updater) {
		try {
			StringBuilder buf = new StringBuilder();
			
			// read the signature character-by-character
			StringReader reader = new StringReader(signature);
			int i = -1;
			while ( (i = reader.read()) != -1) {
				char c = (char)i;
				
				// does this character start a class name?
				if (c == 'L') {
					// update the class name and add it to the buffer
					buf.append('L');
					String className = readClass(reader);
					if (className == null) {
						throw new IllegalArgumentException("Malformed signature: " + signature);
					}
					buf.append(updater.update(className));
					buf.append(';');
				} else {
					// copy the character into the buffer
					buf.append(c);
				}
			}
			
			return buf.toString();
		} catch (IOException ex) {
			// I'm pretty sure a StringReader will never throw one of these
			throw new Error(ex);
		}
	}
	
	private static String readClass(StringReader reader) throws IOException {
		// read all the characters in the buffer until we hit a ';'
		// remember to treat generics correctly
		StringBuilder buf = new StringBuilder();
		int depth = 0;
		int i = -1;
		while ( (i = reader.read()) != -1) {
			char c = (char)i;
			
			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (depth == 0) {
				if (c == ';') {
					return buf.toString();
				} else {
					buf.append(c);
				}
			}
		}
		
		return null;
	}
	
	public static List<String> getClasses(String signature) {
		final List<String> classNames = Lists.newArrayList();
		update(signature, new ClassNameUpdater() {
			@Override
			public String update(String className) {
				classNames.add(className);
				return className;
			}
		});
		return classNames;
	}
}
