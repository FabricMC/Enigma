/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.Color;

public class DeobfuscatedHighlightPainter extends BoxHighlightPainter {
	
	public DeobfuscatedHighlightPainter() {
		// green ish
		super(new Color(220, 255, 220), new Color(80, 160, 80));
	}
}
