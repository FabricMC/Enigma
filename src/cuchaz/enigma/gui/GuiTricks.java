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

import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

public class GuiTricks {
	
	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}
	
	public static void showToolTipNow(JComponent component) {
		// HACKHACK: trick the tooltip manager into showing the tooltip right now
		ToolTipManager manager = ToolTipManager.sharedInstance();
		int oldDelay = manager.getInitialDelay();
		manager.setInitialDelay(0);
		manager.mouseMoved(new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, 0, false));
		manager.setInitialDelay(oldDelay);
	}
}
