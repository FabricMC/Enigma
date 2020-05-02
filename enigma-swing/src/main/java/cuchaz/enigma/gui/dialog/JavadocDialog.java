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

import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.util.ScaleUtil;

import javax.swing.*;
import javax.swing.text.html.HTML;

import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;

import com.google.common.base.Strings;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.elements.ValidatableTextArea;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.packet.ChangeDocsC2SPacket;
import cuchaz.enigma.translation.mapping.IllegalNameException;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class JavadocDialog {

	private final JDialog ui;
	private final GuiController controller;
	private final EntryReference<Entry<?>, Entry<?>> entry;

	private final ValidatableTextArea text;

	private final ValidationContext vc = new ValidationContext();

	private JavadocDialog(JFrame parent, GuiController controller, EntryReference<Entry<?>, Entry<?>> entry, String preset) {
		this.ui = new JDialog(parent, I18n.translate("javadocs.edit"));
		this.controller = controller;
		this.entry = entry;
		this.text = new ValidatableTextArea(10, 40);

		// set up dialog
		Container contentPane = ui.getContentPane();
		contentPane.setLayout(new BorderLayout());

		// editor panel
		this.text.setText(preset);
		this.text.setTabSize(2);
		contentPane.add(new JScrollPane(this.text), BorderLayout.CENTER);
		this.text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						if (event.isControlDown()) {
							doSave();
							if (vc.canProceed()) {
								close();
							}
						}
						break;
					case KeyEvent.VK_ESCAPE:
						close();
						break;
					default:
						break;
				}
			}
		});

		// buttons panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("javadocs.instruction"))));
		JButton cancelButton = new JButton(I18n.translate("javadocs.cancel"));
		cancelButton.addActionListener(event -> close());
		buttonsPanel.add(cancelButton);
		JButton saveButton = new JButton(I18n.translate("javadocs.save"));
		saveButton.addActionListener(event -> doSave());
		buttonsPanel.add(saveButton);
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);

		// tags panel
		JMenuBar tagsMenu = new JMenuBar();

		// add javadoc tags
		for (JavadocTag tag : JavadocTag.values()) {
			JButton tagButton = new JButton(tag.getText());
			tagButton.addActionListener(action -> {
				boolean textSelected = text.getSelectedText() != null;
				String tagText = tag.isInline() ? "{" + tag.getText() + " }" : tag.getText() + " ";

				if (textSelected) {
					if (tag.isInline()) {
						tagText = "{" + tag.getText() + " " + text.getSelectedText() + "}";
					} else {
						tagText = tag.getText() + " " + text.getSelectedText();
					}
					text.replaceSelection(tagText);
				} else {
					text.insert(tagText, text.getCaretPosition());
				}

				if (tag.isInline()) {
					text.setCaretPosition(text.getCaretPosition() - 1);
				}
				text.grabFocus();
			});
			tagsMenu.add(tagButton);
		}

		// add html tags
		JComboBox<String> htmlList = new JComboBox<String>();
		htmlList.setPreferredSize(new Dimension());
		for (HTML.Tag htmlTag : HTML.getAllTags()) {
			htmlList.addItem(htmlTag.toString());
		}
		htmlList.addActionListener(action -> {
			String tagText = "<" + htmlList.getSelectedItem().toString() + ">";
			text.insert(tagText, text.getCaretPosition());
			text.grabFocus();
		});
		tagsMenu.add(htmlList);

		contentPane.add(tagsMenu, BorderLayout.NORTH);

		// show the frame
		this.ui.setSize(ScaleUtil.getDimension(600, 400));
		this.ui.setLocationRelativeTo(parent);
		this.ui.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public void doSave() {
		vc.reset();
		validate();
		if (!vc.canProceed()) return;
		save();
	}

	public void close() {
		this.ui.setVisible(false);
	}

	public void validate() {
		vc.setActiveElement(text);
		// TODO validate doc text immediately, not only on save -> get rid of exception
	}

	public void save() {
		vc.setActiveElement(text);
		try {
			controller.changeDocs(entry, text.getText());
			controller.sendPacket(new ChangeDocsC2SPacket(entry.getNameableEntry(), text.getText()));
		} catch (IllegalNameException ex) {
			vc.raise(Message.INVALID_NAME, ex.getReason());
		}
	}

	public static void show(JFrame parent, GuiController controller, EntryReference<Entry<?>, Entry<?>> entry) {
		EntryReference<Entry<?>, Entry<?>> translatedReference = controller.project.getMapper().deobfuscate(entry);
		String text = Strings.nullToEmpty(translatedReference.entry.getJavadocs());

		JavadocDialog dialog = new JavadocDialog(parent, controller, entry, text);
		dialog.ui.doLayout();
		dialog.ui.setVisible(true);
		dialog.text.grabFocus();
	}

	private enum JavadocTag {
		CODE(true),
		LINK(true),
		LINKPLAIN(true),
		RETURN(false),
		SEE(false),
		THROWS(false);

		private boolean inline;

		private JavadocTag(boolean inline) {
			this.inline = inline;
		}

		public String getText() {
			return "@" + this.name().toLowerCase();
		}

		public boolean isInline() {
			return this.inline;
		}
	}

}
