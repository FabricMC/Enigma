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

import cuchaz.enigma.Constants;
import cuchaz.enigma.utils.LangUtils;
import cuchaz.enigma.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class AboutDialog {

	public static void show(JFrame parent) {
		// init frame
		final JFrame frame = new JFrame(Constants.NAME + " - " + LangUtils.translate("menu.help.about"));
		final Container pane = frame.getContentPane();
		pane.setLayout(new FlowLayout());

		// load the content
		try {
			String html = Utils.readResourceToString("/about.html");
			html = String.format(html, Constants.NAME, Constants.VERSION);
			JLabel label = new JLabel(html);
			label.setHorizontalAlignment(JLabel.CENTER);
			pane.add(label);
		} catch (IOException ex) {
			throw new Error(ex);
		}

		// show the link
		String html = "<html><a href=\"%s\">%s</a></html>";
		html = String.format(html, Constants.URL, Constants.URL);
		JButton link = new JButton(html);
		link.addActionListener(event -> Utils.openUrl(Constants.URL));
		link.setBorderPainted(false);
		link.setOpaque(false);
		link.setBackground(Color.WHITE);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setFocusable(false);
		JPanel linkPanel = new JPanel();
		linkPanel.add(link);
		pane.add(linkPanel);

		// show ok button
		JButton okButton = new JButton(LangUtils.translate("menu.help.about.ok"));
		pane.add(okButton);
		okButton.addActionListener(arg0 -> frame.dispose());

		// show the frame
		pane.doLayout();
		frame.setSize(400, 220);
		frame.setResizable(false);
		frame.setLocationRelativeTo(parent);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
