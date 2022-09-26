package cuchaz.enigma.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import de.sciss.syntaxpane.actions.DocumentSearchData;
import de.sciss.syntaxpane.actions.gui.QuickFindDialog;

import cuchaz.enigma.gui.config.keybind.KeyBinds;

public class EnigmaQuickFindDialog extends QuickFindDialog {
	public EnigmaQuickFindDialog(JTextComponent target) {
		super(target, DocumentSearchData.getFromEditor(target));

		JToolBar toolBar = getToolBar();
		JTextField textField = getTextField(toolBar);

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);

				if (KeyBinds.QUICK_FIND_DIALOG_PREVIOUS.matches(e)) {
					JToolBar toolBar = getToolBar();
					getPrevButton(toolBar).doClick();
				} else if (KeyBinds.QUICK_FIND_DIALOG_NEXT.matches(e)) {
					JToolBar toolBar = getToolBar();
					getNextButton(toolBar).doClick();
				}
			}
		});
	}

	@Override
	public void showFor(JTextComponent target) {
		String selectedText = target.getSelectedText();

		try {
			super.showFor(target);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Container view = target.getParent();
		Point loc = new Point(0, view.getHeight() - getSize().height);
		setLocationRelativeTo(view);
		SwingUtilities.convertPointToScreen(loc, view);
		setLocation(loc);

		JToolBar toolBar = getToolBar();
		JTextField textField = getTextField(toolBar);

		if (selectedText != null) {
			textField.setText(selectedText);
		}

		textField.selectAll();
	}

	private JToolBar getToolBar() {
		return components(getContentPane(), JToolBar.class).findFirst().orElse(null);
	}

	private JTextField getTextField(JToolBar toolBar) {
		return components(toolBar, JTextField.class).findFirst().orElse(null);
	}

	private JButton getNextButton(JToolBar toolBar) {
		Stream<JButton> buttons = components(toolBar, JButton.class);
		return buttons.skip(1).findFirst().orElse(null);
	}

	private JButton getPrevButton(JToolBar toolBar) {
		Stream<JButton> buttons = components(toolBar, JButton.class);
		return buttons.findFirst().orElse(null);
	}

	private static Stream<Component> components(Container container) {
		return IntStream.range(0, container.getComponentCount()).mapToObj(container::getComponent);
	}

	private static <T extends Component> Stream<T> components(Container container, Class<T> type) {
		return components(container).filter(type::isInstance).map(type::cast);
	}
}
