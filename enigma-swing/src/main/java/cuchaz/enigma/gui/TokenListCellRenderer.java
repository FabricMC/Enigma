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

import cuchaz.enigma.source.Token;

import javax.swing.*;
import java.awt.*;

public class TokenListCellRenderer implements ListCellRenderer<Token> {

	private GuiController controller;
	private DefaultListCellRenderer defaultRenderer;

	public TokenListCellRenderer(GuiController controller) {
		this.controller = controller;
		this.defaultRenderer = new DefaultListCellRenderer();
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Token> list, Token token, int index, boolean isSelected, boolean hasFocus) {
		JLabel label = (JLabel) this.defaultRenderer.getListCellRendererComponent(list, token, index, isSelected, hasFocus);
		label.setText(this.controller.getReadableToken(token).toString());
		return label;
	}

}
