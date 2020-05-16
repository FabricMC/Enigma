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

import cuchaz.enigma.Enigma;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.util.ScaleUtil;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class CrashDialog {

	private static CrashDialog instance = null;

	private JFrame frame;
	private JTextArea text;

	private CrashDialog(JFrame parent) {
		// init frame
		frame = new JFrame(String.format(I18n.translate("crash.title"), Enigma.NAME));
		final Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout());

		JLabel label = new JLabel(String.format(I18n.translate("crash.summary"), Enigma.NAME));
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(label, BorderLayout.NORTH);

		// report panel
		text = new JTextArea();
		text.setTabSize(2);
		pane.add(new JScrollPane(text), BorderLayout.CENTER);

		// buttons panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
		JButton exportButton = new JButton(I18n.translate("crash.export"));
		exportButton.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(new File("enigma_crash.log"));
			if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					File file = chooser.getSelectedFile();
					FileWriter writer = new FileWriter(file);
					writer.write(instance.text.getText());
					writer.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		buttonsPanel.add(exportButton);
		buttonsPanel.add(Box.createHorizontalGlue());
		buttonsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("crash.exit.warning"))));
		JButton ignoreButton = new JButton(I18n.translate("crash.ignore"));
		ignoreButton.addActionListener(event -> {
			// close (hide) the dialog
			frame.setVisible(false);
		});
		buttonsPanel.add(ignoreButton);
		JButton exitButton = new JButton(I18n.translate("crash.exit"));
		exitButton.addActionListener(event -> {
			// exit enigma
			System.exit(1);
		});
		buttonsPanel.add(exitButton);
		pane.add(buttonsPanel, BorderLayout.SOUTH);

		// show the frame
		frame.setSize(ScaleUtil.getDimension(600, 400));
		frame.setLocationRelativeTo(parent);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public static void init(JFrame parent) {
		instance = new CrashDialog(parent);
	}

	public static void show(Throwable ex) {
		// get the error report
		StringWriter buf = new StringWriter();
		ex.printStackTrace(new PrintWriter(buf));
		String report = buf.toString();

		// show it!
		instance.text.setText(report);
		instance.frame.doLayout();
		instance.frame.setVisible(true);
	}
}
