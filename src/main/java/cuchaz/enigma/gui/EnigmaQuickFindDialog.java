package cuchaz.enigma.gui;

import de.sciss.syntaxpane.actions.DocumentSearchData;
import de.sciss.syntaxpane.actions.gui.QuickFindDialog;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EnigmaQuickFindDialog extends QuickFindDialog {
	public EnigmaQuickFindDialog(JTextComponent target) {
		super(target, DocumentSearchData.getFromEditor(target));
	}

	@Override
	public void showFor(JTextComponent target) {
		String selectedText = target.getSelectedText();

		super.showFor(target);

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
