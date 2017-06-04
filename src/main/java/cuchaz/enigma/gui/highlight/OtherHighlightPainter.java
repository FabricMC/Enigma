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

package cuchaz.enigma.gui.highlight;

import cuchaz.enigma.config.Config;

public class OtherHighlightPainter extends BoxHighlightPainter {

	public OtherHighlightPainter() {
		super(null, getColor(Config.INSTANCE.otherColorOutline, Config.INSTANCE.otherOutlineAlpha));
	}
}
