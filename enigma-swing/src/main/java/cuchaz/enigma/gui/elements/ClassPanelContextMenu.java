package cuchaz.enigma.gui.elements;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.panels.classlists.ClassPanel;
import cuchaz.enigma.utils.I18n;

public class ClassPanelContextMenu {
	private final JPopupMenu ui;
	private final JMenuItem renamePackage = new JMenuItem();
	private final JMenuItem renameClass = new JMenuItem();
	private final JMenuItem expandAll = new JMenuItem();
	private final JMenuItem collapseAll = new JMenuItem();

	public ClassPanelContextMenu(ClassPanel panel) {
		this.ui = new JPopupMenu();

		this.ui.add(this.renamePackage);
		this.ui.add(this.renameClass);
		this.ui.addSeparator();
		this.ui.add(this.expandAll);
		this.ui.add(this.collapseAll);

		ClassSelector classes = panel.classes;

		this.renamePackage.addActionListener(a -> {
			TreePath path;

			if (classes.getSelectedClass() != null) {
				// Rename parent package if selected path is a class
				path = classes.getSelectionPath().getParentPath();
			} else {
				// Rename selected path if it's already a package
				path = classes.getSelectionPath();
			}

			classes.getUI().startEditingAtPath(classes, path);
		});
		this.renameClass.addActionListener(a -> classes.getUI().startEditingAtPath(classes, classes.getSelectionPath()));
		this.expandAll.addActionListener(a -> classes.expandAll());
		this.collapseAll.addActionListener(a -> classes.collapseAll());

		this.retranslateUi();
	}

	public void show(ClassSelector classes, int x, int y) {
		// Only enable rename class if selected path is a class
		this.renameClass.setEnabled(classes.getSelectedClass() != null);

		this.ui.show(classes, x, y);
	}

	public void retranslateUi() {
		this.renamePackage.setText(I18n.translate("popup_menu.class_panel.rename_package"));
		this.renameClass.setText(I18n.translate("popup_menu.class_panel.rename_class"));
		this.expandAll.setText(I18n.translate("popup_menu.class_panel.expand_all"));
		this.collapseAll.setText(I18n.translate("popup_menu.class_panel.collapse_all"));
	}
}
