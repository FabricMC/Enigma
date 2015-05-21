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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import cuchaz.enigma.Constants;

public class CrashDialog {
	
	private static CrashDialog m_instance = null;
	
	private JFrame m_frame;
	private JTextArea m_text;
	
	private CrashDialog(JFrame parent) {
		// init frame
		m_frame = new JFrame(Constants.Name + " - Crash Report");
		final Container pane = m_frame.getContentPane();
		pane.setLayout(new BorderLayout());
		
		JLabel label = new JLabel(Constants.Name + " has crashed! =(");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(label, BorderLayout.NORTH);
		
		// report panel
		m_text = new JTextArea();
		m_text.setTabSize(2);
		pane.add(new JScrollPane(m_text), BorderLayout.CENTER);
		
		// buttons panel
		JPanel buttonsPanel = new JPanel();
		FlowLayout buttonsLayout = new FlowLayout();
		buttonsLayout.setAlignment(FlowLayout.RIGHT);
		buttonsPanel.setLayout(buttonsLayout);
		buttonsPanel.add(GuiTricks.unboldLabel(new JLabel("If you choose exit, you will lose any unsaved work.")));
		JButton ignoreButton = new JButton("Ignore");
		ignoreButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// close (hide) the dialog
				m_frame.setVisible(false);
			}
		});
		buttonsPanel.add(ignoreButton);
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// exit enigma
				System.exit(1);
			}
		});
		buttonsPanel.add(exitButton);
		pane.add(buttonsPanel, BorderLayout.SOUTH);
		
		// show the frame
		m_frame.setSize(600, 400);
		m_frame.setLocationRelativeTo(parent);
		m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
	
	public static void init(JFrame parent) {
		m_instance = new CrashDialog(parent);
	}
	
	public static void show(Throwable ex) {
		// get the error report
		StringWriter buf = new StringWriter();
		ex.printStackTrace(new PrintWriter(buf));
		String report = buf.toString();
		
		// show it!
		m_instance.m_text.setText(report);
		m_instance.m_frame.doLayout();
		m_instance.m_frame.setVisible(true);
	}
}
