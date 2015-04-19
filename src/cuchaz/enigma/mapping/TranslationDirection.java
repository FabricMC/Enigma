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

public enum TranslationDirection {
	
	Deobfuscating {
		@Override
		public <T> T choose(T deobfChoice, T obfChoice) {
			return deobfChoice;
		}
	},
	Obfuscating {
		@Override
		public <T> T choose(T deobfChoice, T obfChoice) {
			return obfChoice;
		}
	};
	
	public abstract <T> T choose(T deobfChoice, T obfChoice);
}
