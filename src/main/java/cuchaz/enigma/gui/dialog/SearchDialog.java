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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.util.AbstractListCellRenderer;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.search.SearchEntry;
import cuchaz.enigma.utils.search.SearchUtil;

public class SearchDialog {

	private JTextField searchField;
	private JList<SearchEntryImpl> classList;
	private JDialog dialog;

	private final Gui parent;
	private final List<SearchEntryImpl> classes;
	private final SearchUtil<SearchEntryImpl> su;

	public SearchDialog(Gui parent) {
		this.parent = parent;

		this.classes = parent.getController().project.getJarIndex().getEntryIndex().getClasses().parallelStream()
				.map(e -> SearchEntryImpl.from(e, parent.getController()))
				.collect(Collectors.toList());
		this.classes.removeIf(e -> e.obf.isInnerClass());

		su = new SearchUtil<>();
		su.addAll(classes);
	}

	public void show() {
		dialog = new JDialog(parent.getFrame(), I18n.translate("menu.view.search"), true);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(ScaleUtil.createEmptyBorder(4, 4, 4, 4));
		contentPane.setLayout(new BorderLayout(ScaleUtil.scale(4), ScaleUtil.scale(4)));

		searchField = new JTextField();
		searchField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateList();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateList();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateList();
			}

		});
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					int next = classList.isSelectionEmpty() ? 0 : classList.getSelectedIndex() + 1;
					classList.setSelectedIndex(next);
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					int prev = classList.isSelectionEmpty() ? classList.getModel().getSize() : classList.getSelectedIndex() - 1;
					classList.setSelectedIndex(prev);
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					close();
				}
			}
		});
		searchField.addActionListener(e -> openSelected());
		contentPane.add(searchField, BorderLayout.NORTH);

		classList = new JList<>();
		classList.setCellRenderer(new ListCellRendererImpl());
		classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		classList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() >= 2) {
					int idx = classList.locationToIndex(mouseEvent.getPoint());
					SearchEntryImpl entry = classList.getModel().getElementAt(idx);
					openEntry(entry.obf);
				}
			}
		});
		contentPane.add(new JScrollPane(classList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton open = new JButton("Open");
		open.addActionListener(event -> openSelected());
		buttonBar.add(open);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(event -> close());
		buttonBar.add(cancel);
		contentPane.add(buttonBar, BorderLayout.SOUTH);

		dialog.setContentPane(contentPane);

		dialog.setSize(ScaleUtil.getDimension(400, 500));
		dialog.setLocationRelativeTo(parent.getFrame());
		dialog.setVisible(true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		dialog.requestFocus();
		searchField.requestFocus();
	}

	private void openSelected() {
		SearchEntryImpl selectedValue = classList.getSelectedValue();
		if (selectedValue != null) {
			openEntry(selectedValue.obf);
		}
	}

	private void openEntry(ClassEntry e) {
		close();
		parent.getController().navigateTo(e);
		parent.getDeobfPanel().deobfClasses.setSelectionClass(e);
	}

	private void close() {
		dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
	}

	// Updates the list of class names
	private void updateList() {
		DefaultListModel<SearchEntryImpl> listModel = new DefaultListModel<>();

		List<SearchEntryImpl> results = su.search(searchField.getText(), 25);
		results.forEach(listModel::addElement);

		classList.setModel(listModel);
	}

	private static final class SearchEntryImpl implements SearchEntry {

		public final ClassEntry obf;
		public final ClassEntry deobf;

		private SearchEntryImpl(ClassEntry obf, ClassEntry deobf) {
			this.obf = obf;
			this.deobf = deobf;
		}

		@Override
		public List<String> getSearchableNames() {
			if (deobf != null) {
				return Arrays.asList(obf.getSimpleName(), deobf.getSimpleName());
			} else {
				return Collections.singletonList(obf.getSimpleName());
			}
		}

		@Override
		public String toString() {
			return String.format("SearchEntryImpl { obf: %s, deobf: %s }", obf, deobf);
		}

		public static SearchEntryImpl from(ClassEntry e, GuiController controller) {
			ClassEntry deobf = controller.project.getMapper().deobfuscate(e);
			if (deobf == e) deobf = null;
			return new SearchEntryImpl(e, deobf);
		}

	}

	private static final class ListCellRendererImpl extends AbstractListCellRenderer<SearchEntryImpl> {

		private JLabel mainName;
		private JLabel secondaryName;

		public ListCellRendererImpl() {
			this.setLayout(new BorderLayout());

			mainName = new JLabel();
			this.add(mainName, BorderLayout.WEST);

			secondaryName = new JLabel();
			secondaryName.setFont(secondaryName.getFont().deriveFont(Font.ITALIC, 12));
			secondaryName.setForeground(Color.GRAY);
			this.add(secondaryName, BorderLayout.EAST);
		}

		@Override
		public void updateUiForEntry(JList<? extends SearchEntryImpl> list, SearchEntryImpl value, int index, boolean isSelected, boolean cellHasFocus) {
			if (value.deobf == null) {
				mainName.setText(value.obf.getSimpleName());
				mainName.setToolTipText(value.obf.getFullName());
				secondaryName.setText("");
				secondaryName.setToolTipText("");
			} else {
				mainName.setText(value.deobf.getSimpleName());
				mainName.setToolTipText(value.deobf.getFullName());
				secondaryName.setText(value.obf.getSimpleName());
				secondaryName.setToolTipText(value.obf.getFullName());
			}
		}

	}

}
