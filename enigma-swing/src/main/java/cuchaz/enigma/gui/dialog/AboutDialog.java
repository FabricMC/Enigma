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

package cuchaz.enigma.gui.dialog;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;

public class AboutDialog {

	public static void show(JFrame parent) {
		JDialog frame = new JDialog(parent, String.format(I18n.translate("menu.help.about.title"), Enigma.NAME), true);
		Container pane = frame.getContentPane();
		pane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
				.insets(2)
				.weight(1.0, 0.0)
				.anchor(GridBagConstraints.WEST);

		JLabel title = new JLabel(Enigma.NAME);
		title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() * 1.5f));

		JButton okButton = new JButton(I18n.translate("prompt.ok"));
		okButton.addActionListener(e -> frame.dispose());

		pane.add(title, cb.pos(0, 0).build());
		pane.add(new JLabel(I18n.translate("menu.help.about.description")), cb.pos(0, 1).width(2).build());
		pane.add(new JLabel(I18n.translateFormatted("menu.help.about.version", Enigma.VERSION)), cb.pos(0, 2).width(2).build());
		pane.add(GuiUtil.createLink(Enigma.URL, () -> GuiUtil.openUrl(Enigma.URL)), cb.pos(0, 3).build());
		pane.add(okButton, cb.pos(1, 3).anchor(GridBagConstraints.SOUTHEAST).build());

		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(parent);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

}
