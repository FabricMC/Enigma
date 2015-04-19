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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Util;

public class AboutDialog {
	
	public static void show(JFrame parent) {
		// init frame
		final JFrame frame = new JFrame(Constants.Name + " - About");
		final Container pane = frame.getContentPane();
		pane.setLayout(new FlowLayout());
		
		// load the content
		try {
			String html = Util.readResourceToString("/about.html");
			html = String.format(html, Constants.Name, Constants.Version);
			JLabel label = new JLabel(html);
			label.setHorizontalAlignment(JLabel.CENTER);
			pane.add(label);
		} catch (IOException ex) {
			throw new Error(ex);
		}
		
		// show the link
		String html = "<html><a href=\"%s\">%s</a></html>";
		html = String.format(html, Constants.Url, Constants.Url);
		JButton link = new JButton(html);
		link.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Util.openUrl(Constants.Url);
			}
		});
		link.setBorderPainted(false);
		link.setOpaque(false);
		link.setBackground(Color.WHITE);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setFocusable(false);
		JPanel linkPanel = new JPanel();
		linkPanel.add(link);
		pane.add(linkPanel);
		
		// show ok button
		JButton okButton = new JButton("Ok");
		pane.add(okButton);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.dispose();
			}
		});
		
		// show the frame
		pane.doLayout();
		frame.setSize(400, 220);
		frame.setResizable(false);
		frame.setLocationRelativeTo(parent);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
