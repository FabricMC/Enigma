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
package cuchaz.enigma.gui;

import java.awt.Color;

public class ObfuscatedHighlightPainter extends BoxHighlightPainter {
	
	public ObfuscatedHighlightPainter() {
		// red ish
		super(new Color(255, 220, 220), new Color(160, 80, 80));
	}
}
