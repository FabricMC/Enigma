package cuchaz.enigma.gui.panels.classlists;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.ClassPanelContextMenu;
import cuchaz.enigma.gui.util.GuiUtil;

public abstract class ClassPanel extends JPanel {
	public final ClassSelector classes;
	protected final JLabel title = new JLabel();
	protected final ClassPanelContextMenu contextMenu;
	protected final Gui gui;

	public ClassPanel(Gui gui) {
		this.gui = gui;

		this.classes = getClassSelector();
		this.classes.addMouseListener(GuiUtil.onMousePress(this::onPress));
		this.classes.setSelectionListener(gui.getController()::navigateTo);
		this.classes.setRenameSelectionListener(gui::onRenameFromClassTree);
		this.contextMenu = new ClassPanelContextMenu(this);

		this.setLayout(new BorderLayout());
		this.add(this.title, BorderLayout.NORTH);
		this.add(new JScrollPane(this.classes), BorderLayout.CENTER);

		this.retranslateUi();
	}

	protected abstract ClassSelector getClassSelector();

	protected void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			classes.setSelectionRow(classes.getClosestRowForLocation(e.getX(), e.getY()));
			int i = classes.getRowForPath(classes.getSelectionPath());

			if (i != -1) {
				contextMenu.show(classes, e.getX(), e.getY());
			}
		}
	}

	public void reload() {
		classes.reload();
		updateCounter();
	};

	public void retranslateUi() {
		updateCounter();
		contextMenu.retranslateUi();
	}

	public abstract void updateCounter();
}
