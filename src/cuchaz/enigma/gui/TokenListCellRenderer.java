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

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import cuchaz.enigma.analysis.Token;

public class TokenListCellRenderer implements ListCellRenderer<Token> {
	
	private GuiController m_controller;
	private DefaultListCellRenderer m_defaultRenderer;
	
	public TokenListCellRenderer(GuiController controller) {
		m_controller = controller;
		m_defaultRenderer = new DefaultListCellRenderer();
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends Token> list, Token token, int index, boolean isSelected, boolean hasFocus) {
		JLabel label = (JLabel)m_defaultRenderer.getListCellRendererComponent(list, token, index, isSelected, hasFocus);
		label.setText(m_controller.getReadableToken(token).toString());
		return label;
	}
}
