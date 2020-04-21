package cuchaz.enigma.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import cuchaz.enigma.utils.Message;

// For now, just render the translated text.
// TODO: Icons or something later?
public class MessageListCellRenderer extends DefaultListCellRenderer {

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		Message message = (Message) value;
		if (message != null) {
			setText(message.translate());
		}
		return this;
	}

}
