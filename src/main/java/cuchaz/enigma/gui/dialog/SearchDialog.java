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

import com.google.common.collect.Lists;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.LangUtils;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchDialog {

	private JTextField searchField;
	private JList<String> classList;
	private JFrame frame;

	private Gui parent;
	private List<ClassEntry> deobfClasses;

	private KeyEventDispatcher keyEventDispatcher;

	public SearchDialog(Gui parent) {
		this.parent = parent;

		deobfClasses = Lists.newArrayList();
		this.parent.getController().addSeparatedClasses(Lists.newArrayList(), deobfClasses);
		deobfClasses.removeIf(ClassEntry::isInnerClass);
	}

	public void show() {
		frame = new JFrame(LangUtils.translate("menu.view.search"));
		frame.setVisible(false);
		JPanel pane = new JPanel();
		pane.setBorder(new EmptyBorder(5, 10, 5, 10));

		addRow(pane, jPanel -> {
			searchField = new JTextField("", 20);

			searchField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent keyEvent) {
					updateList();
				}
			});

			jPanel.add(searchField);
		});

		addRow(pane, jPanel -> {
			classList = new JList<>();
			classList.setLayoutOrientation(JList.VERTICAL);
			classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			classList.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent mouseEvent) {
					if(mouseEvent.getClickCount() >= 2){
						openSelected();
					}
				}
			});
			jPanel.add(classList);
		});


		keyEventDispatcher = keyEvent -> {
			if(!frame.isVisible()){
				return false;
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_DOWN){
				int next = classList.isSelectionEmpty() ? 0 : classList.getSelectedIndex() + 1;
				classList.setSelectedIndex(next);
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_UP){
				int next = classList.isSelectionEmpty() ? classList.getModel().getSize() : classList.getSelectedIndex() - 1;
				classList.setSelectedIndex(next);
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER){
				openSelected();
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE){
				close();
			}
			return false;
		};

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);

		frame.setContentPane(pane);
		frame.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		frame.setSize(360, 500);
		frame.setAlwaysOnTop(true);
		frame.setResizable(false);
		frame.setLocationRelativeTo(parent.getFrame());
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		searchField.requestFocusInWindow();
	}

	private void openSelected(){
		close();
		if(classList.isSelectionEmpty()){
			return;
		}
		deobfClasses.stream()
			.filter(classEntry -> classEntry.getSimpleName().equals(classList.getSelectedValue())).
			findFirst()
			.ifPresent(classEntry -> {
				parent.getController().navigateTo(classEntry);
				parent.getDeobfPanel().deobfClasses.setSelectionClass(classEntry);
			});
	}

	private void close(){
		frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher);
	}

	private void addRow(JPanel pane, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		pane.add(panel, BorderLayout.CENTER);
	}

	//Updates the list of class names
	private void updateList() {
		DefaultListModel<String> listModel = new DefaultListModel<>();

		//Basic search using the Fuzzy libary
		//TODO improve on this, to not just work from string and to keep the ClassEntry
		List<ExtractedResult> results = FuzzySearch.extractTop(searchField.getText(), deobfClasses.stream().map(ClassEntry::getSimpleName).collect(Collectors.toList()), 25);
		results.forEach(extractedResult -> listModel.addElement(extractedResult.getString()));

		classList.setModel(listModel);
	}



}
