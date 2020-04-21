package cuchaz.enigma.gui.elements;

import java.awt.event.MouseEvent;

import javax.swing.JTabbedPane;

public class CollapsibleTabbedPane extends JTabbedPane {

	public CollapsibleTabbedPane() {
	}

	public CollapsibleTabbedPane(int tabPlacement) {
		super(tabPlacement);
	}

	public CollapsibleTabbedPane(int tabPlacement, int tabLayoutPolicy) {
		super(tabPlacement, tabLayoutPolicy);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		int id = e.getID();
		if (id == MouseEvent.MOUSE_PRESSED) {
			if (!isEnabled()) return;
			int tabIndex = getUI().tabForCoordinate(this, e.getX(), e.getY());
			if (tabIndex >= 0 && isEnabledAt(tabIndex)) {
				if (tabIndex == getSelectedIndex()) {
					if (isFocusOwner() && isRequestFocusEnabled()) {
						requestFocus();
					} else {
						setSelectedIndex(-1);
					}
					return;
				}
			}
		}
		super.processMouseEvent(e);
	}

}
