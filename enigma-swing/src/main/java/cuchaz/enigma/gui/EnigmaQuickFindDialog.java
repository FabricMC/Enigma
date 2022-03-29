/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.gui;

import java.awt.Container;
import java.awt.Point;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import de.sciss.syntaxpane.actions.DefaultSyntaxAction;
import de.sciss.syntaxpane.actions.DocumentSearchData;
import de.sciss.syntaxpane.actions.gui.EscapeListener;
import de.sciss.syntaxpane.components.Markers;
import de.sciss.syntaxpane.util.SwingUtils;

import javax.swing.text.JTextComponent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reimplementation of {@link de.sciss.syntaxpane.actions.gui.QuickFindDialog} to allow more customization.
 */
public class EnigmaQuickFindDialog extends JDialog implements DocumentListener, ActionListener, EscapeListener {
	private static final int SEARCH_FIELD_MAX_WIDTH = 200;
	private static final int SEARCH_FIELD_MAX_HEIGHT = 24;
	private static final int SEARCH_FIELD_MIN_WIDTH = 60;
	private static final int SEARCH_FIELD_MIN_HEIGHT = 24;
	private static final int PREFERRED_TOOLBAR_WIDTH = 684;

	private final Markers.SimpleMarker marker = new Markers.SimpleMarker(Color.PINK);
	private WeakReference<JTextComponent> target;
	private WeakReference<DocumentSearchData> searchData;
	private int prevCaretPos;

	private JToolBar toolBar;
	private JLabel statusLabel;
	private JLabel label;
	private JTextField searchField;
	private JButton prevButton;
	private JButton nextButton;
	private JCheckBox ignoreCaseCheckBox;
	private JCheckBox regexCheckBox;
	private JCheckBox wrapCheckBox;

	public EnigmaQuickFindDialog(JTextComponent target) {
		this(target, DocumentSearchData.getFromEditor(target));
	}

	public EnigmaQuickFindDialog(JTextComponent target, DocumentSearchData searchData) {
		super(SwingUtilities.getWindowAncestor(target), ModalityType.MODELESS);

		initComponents();
		SwingUtils.addEscapeListener(this);
		this.searchData = new WeakReference<>(searchData);
	}

	private static void setButtonIcon(AbstractButton button, String name) {
		URL url = DefaultSyntaxAction.class.getClassLoader().getResource(DefaultSyntaxAction.SMALL_ICONS_LOC_PREFIX + name);
		if (url != null) {
			button.setIcon(new ImageIcon(url));
		}
	}

	public void showFor(JTextComponent target) {
		prevCaretPos = target.getCaretPosition();

		Container view = target.getParent();
		Dimension size = getSize();

		// Set the width of the dialog to the width of the target
		size.width = target.getVisibleRect().width;
		setSize(size);

		// Put the dialog at the bottom of the target
		Point loc = new Point(0, view.getHeight() - size.height);
		setLocationRelativeTo(view);
		SwingUtilities.convertPointToScreen(loc, view);
		setLocation(loc);

		searchField.setFont(target.getFont());
		searchField.getDocument().addDocumentListener(this);

		// Close the dialog when clicking outside it
		WindowFocusListener focusListener = new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				setVisible(false);
				target.getDocument().removeDocumentListener(EnigmaQuickFindDialog.this);
				Markers.removeMarkers(target, marker);
				removeWindowListener(this);
			}
		};
		addWindowFocusListener(focusListener);

		this.target = new WeakReference<>(target);

		DocumentSearchData searchData = this.searchData.get();
		wrapCheckBox.setSelected(searchData.isWrap());

		// Set the search field to the current selection
		String selectedText = target.getSelectedText();
		if (selectedText != null) {
			searchField.setText(selectedText);
		} else {
			Pattern pattern = searchData.getPattern();
			if (pattern != null) {
				searchField.setText(pattern.pattern());
			}
		}
		searchField.selectAll();

		setVisible(true);
	}

	private void initComponents() {
		toolBar = new JToolBar();
		statusLabel = new JLabel();
		label = new JLabel();
		searchField = new JTextField();
		prevButton = new JButton();
		nextButton = new JButton();
		ignoreCaseCheckBox = new JCheckBox();
		regexCheckBox = new JCheckBox();
		wrapCheckBox = new JCheckBox();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBackground(Color.DARK_GRAY);
		setName("QuickFindDialog");
		setResizable(false);
		setUndecorated(true);

		toolBar.setBorder(BorderFactory.createEtchedBorder());
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		toolBar.addSeparator();

		label.setLabelFor(searchField);
		ResourceBundle bundle = ResourceBundle.getBundle("de/sciss/syntaxpane/Bundle");
		label.setText(bundle.getString("QuickFindDialog.jLabel1.text"));
		toolBar.add(label);
		toolBar.addSeparator();

		searchField.setColumns(30);
		searchField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		searchField.setMaximumSize(new Dimension(SEARCH_FIELD_MAX_WIDTH, SEARCH_FIELD_MAX_HEIGHT));
		searchField.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, SEARCH_FIELD_MIN_HEIGHT));
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				if (KeyBinds.QUICK_FIND_DIALOG_PREVIOUS.matches(e)) {
					prevButton.doClick();
				} else if (KeyBinds.QUICK_FIND_DIALOG_NEXT.matches(e)) {
					nextButton.doClick();
				}
			}
		});
		toolBar.add(searchField);
		toolBar.addSeparator();

		setButtonIcon(prevButton, "go-up.png");
		prevButton.setHorizontalTextPosition(SwingConstants.CENTER);
		prevButton.setFocusable(false);
		prevButton.setOpaque(false);
		prevButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		prevButton.addActionListener(this::prevButtonActionPerformed);
		toolBar.add(prevButton);

		setButtonIcon(nextButton, "go-down.png");
		nextButton.setHorizontalTextPosition(SwingConstants.CENTER);
		nextButton.setMargin(new Insets(2, 2, 2, 2));
		nextButton.setFocusable(false);
		nextButton.setOpaque(false);
		nextButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		nextButton.addActionListener(this::nextButtonActionPerformed);
		toolBar.add(nextButton);

		ignoreCaseCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_IGNORE_CASE.getKeyCode());
		ignoreCaseCheckBox.setText(bundle.getString("QuickFindDialog.jChkIgnoreCase.text"));
		ignoreCaseCheckBox.setFocusable(false);
		ignoreCaseCheckBox.setOpaque(false);
		ignoreCaseCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		ignoreCaseCheckBox.addActionListener(this);
		toolBar.add(ignoreCaseCheckBox);

		regexCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_REGEX.getKeyCode());
		regexCheckBox.setText(bundle.getString("QuickFindDialog.jChkRegExp.text"));
		regexCheckBox.setFocusable(false);
		regexCheckBox.setOpaque(false);
		regexCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		regexCheckBox.addActionListener(this);
		toolBar.add(regexCheckBox);

		wrapCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_WRAP.getKeyCode());
		wrapCheckBox.setText(bundle.getString("QuickFindDialog.jChkWrap.text"));
		wrapCheckBox.setFocusable(false);
		wrapCheckBox.setOpaque(false);
		wrapCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		wrapCheckBox.addActionListener(this);
		toolBar.add(wrapCheckBox);

		toolBar.addSeparator();

		statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getStyle() | Font.BOLD, statusLabel.getFont().getSize() - 2));
		statusLabel.setForeground(Color.RED);
		toolBar.add(statusLabel);

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(toolBar, GroupLayout.DEFAULT_SIZE, PREFERRED_TOOLBAR_WIDTH, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));

		pack();
	}

	private void prevButtonActionPerformed(ActionEvent e) {
		if (searchData.get().doFindPrev(target.get())) {
			statusLabel.setText(null);
		} else {
			statusLabel.setText(ResourceBundle.getBundle("de/sciss/syntaxpane/Bundle").getString("QuickFindDialog.NotFound"));
		}
	}

	private void nextButtonActionPerformed(ActionEvent e) {
		if (searchData.get().doFindNext(target.get())) {
			statusLabel.setText(null);
		} else {
			statusLabel.setText(ResourceBundle.getBundle("de/sciss/syntaxpane/Bundle").getString("QuickFindDialog.NotFound"));
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		updateFind();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		updateFind();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		updateFind();
	}

	private void updateFind() {
		JTextComponent target = this.target.get();
		DocumentSearchData searchData = this.searchData.get();
		String searchText = searchField.getText();

		if (searchText == null || searchText.isEmpty()) {
			statusLabel.setText(null);
			return;
		}

		try {
			searchData.setWrap(wrapCheckBox.isSelected());
			searchData.setPattern(searchText, regexCheckBox.isSelected(), ignoreCaseCheckBox.isSelected());
			statusLabel.setText(null);

			// The DocumentSearchData doFindNext will always find from current pos,
			// so we need to relocate to our saved pos before we call doFindNext
			target.setCaretPosition(prevCaretPos);
			if (!searchData.doFindNext(target)) {
				statusLabel.setText(ResourceBundle.getBundle("de/sciss/syntaxpane/Bundle").getString("QuickFindDialog.NotFound"));
			} else {
				statusLabel.setText(null);
			}
		} catch (PatternSyntaxException e) {
			statusLabel.setText(e.getDescription());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			updateFind();
		}
	}

	@Override
	public void escapePressed() {
		setVisible(false);
	}
}
