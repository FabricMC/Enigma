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

package cuchaz.enigma.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GuiTricks {

	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}

	public static void deactivateButton(JButton button) {
		button.setEnabled(false);
		button.setText("");
		for (ActionListener listener : button.getActionListeners()) {
			button.removeActionListener(listener);
		}
	}

	public static void activateButton(JButton button, String text, ActionListener newListener) {
		button.setText(text);
		button.setEnabled(true);
		for (ActionListener listener : button.getActionListeners()) {
			button.removeActionListener(listener);
		}
		button.addActionListener(newListener);
	}
}
