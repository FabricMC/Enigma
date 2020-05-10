package cuchaz.enigma.gui.dialog;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

public class ChangeDialog {

	public static void show(Gui gui) {
		// init frame
		JFrame frame = new JFrame(I18n.translate("menu.view.change.title"));
		JPanel textPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		frame.setLayout(new BorderLayout());
		frame.add(BorderLayout.NORTH, textPanel);
		frame.add(BorderLayout.SOUTH, buttonPanel);

		// show text
		JLabel text = new JLabel((I18n.translate("menu.view.change.summary")));
		text.setHorizontalAlignment(JLabel.CENTER);
		textPanel.add(text);

		// show ok button
		JButton okButton = new JButton(I18n.translate("menu.view.change.ok"));
		buttonPanel.add(okButton);
		okButton.addActionListener(event -> frame.dispose());
		okButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					frame.dispose();
				}
			}
		});

		// show the frame
		frame.pack();
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setLocationRelativeTo(gui.getFrame());
	}
}
