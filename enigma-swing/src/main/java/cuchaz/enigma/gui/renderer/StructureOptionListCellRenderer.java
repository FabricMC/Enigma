package cuchaz.enigma.gui.renderer;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import cuchaz.enigma.analysis.StructureTreeOptions;
import cuchaz.enigma.utils.I18n;

public class StructureOptionListCellRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof StructureTreeOptions.Option option) {
			this.setText(I18n.translate(option.getTranslationKey()));
		}

		return c;
	}
}
