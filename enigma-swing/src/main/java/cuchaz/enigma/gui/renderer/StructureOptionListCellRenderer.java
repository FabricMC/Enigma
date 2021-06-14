package cuchaz.enigma.gui.renderer;

import cuchaz.enigma.analysis.StructureTreeOptions;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import java.awt.*;

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
