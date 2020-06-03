package cuchaz.enigma.gui;

import de.sciss.syntaxpane.actions.DocumentSearchData;
import de.sciss.syntaxpane.actions.gui.QuickFindDialog;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EnigmaQuickFindDialog extends QuickFindDialog {
	public EnigmaQuickFindDialog(JTextComponent target) {
		super(target, DocumentSearchData.getFromEditor(target));

		JToolBar toolBar = getToolBar();
		JTextField textField = getTextField(toolBar);

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					JToolBar toolBar = getToolBar();
					boolean next = !e.isShiftDown();
					JButton button = next ? getNextButton(toolBar) : getPrevButton(toolBar);
					button.doClick();
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
		return IntStream.range(0, container.getComponentCount())
				.mapToObj(container::getComponent);
	}

	private static <T extends Component> Stream<T> components(Container container, Class<T> type) {
		return components(container)
				.filter(type::isInstance)
				.map(type::cast);
	}
}
