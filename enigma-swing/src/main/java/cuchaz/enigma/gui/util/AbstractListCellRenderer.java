package cuchaz.enigma.gui.util;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;

public abstract class AbstractListCellRenderer<E> extends JPanel implements ListCellRenderer<E> {
	private static final Border NO_FOCUS_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);

	private Border noFocusBorder;

	public AbstractListCellRenderer() {
		setBorder(getNoFocusBorder());
	}

	protected Border getNoFocusBorder() {
		if (noFocusBorder == null) {
			Border border = UIManager.getLookAndFeel().getDefaults().getBorder("List.List.cellNoFocusBorder");
			noFocusBorder = border != null ? border : NO_FOCUS_BORDER;
		}

		return noFocusBorder;
	}

	protected Border getBorder(boolean isSelected, boolean cellHasFocus) {
		Border b = null;

		if (cellHasFocus) {
			UIDefaults defaults = UIManager.getLookAndFeel().getDefaults();

			if (isSelected) {
				b = defaults.getBorder("List.focusSelectedCellHighlightBorder");
			}

			if (b == null) {
				b = defaults.getBorder("List.focusCellHighlightBorder");
			}
		} else {
			b = getNoFocusBorder();
		}

		return b;
	}

	public abstract void updateUiForEntry(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus);

	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
		updateUiForEntry(list, value, index, isSelected, cellHasFocus);

		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		setEnabled(list.isEnabled());
		setFont(list.getFont());

		setBorder(getBorder(isSelected, cellHasFocus));

		// This isn't the width of the cell, but it's close enough for where it's needed (getComponentAt in getToolTipText)
		setSize(list.getWidth(), getPreferredSize().height);

		return this;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		Component c = getComponentAt(event.getPoint());

		if (c instanceof JComponent) {
			return ((JComponent) c).getToolTipText();
		}

		return getToolTipText();
	}
}
