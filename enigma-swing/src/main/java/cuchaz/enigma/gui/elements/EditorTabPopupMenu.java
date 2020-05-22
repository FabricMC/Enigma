package cuchaz.enigma.gui.elements;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.utils.I18n;

public class EditorTabPopupMenu {

	private final JPopupMenu ui;
	private final JMenuItem close;
	private final JMenuItem closeAll;
	private final JMenuItem closeOthers;
	private final JMenuItem closeLeft;
	private final JMenuItem closeRight;

	private final Gui gui;
	private PanelEditor editor;

	public EditorTabPopupMenu(Gui gui) {
		this.gui = gui;

		this.ui = new JPopupMenu();

		this.close = new JMenuItem(I18n.translate("popup_menu.editor_tab.close"));
		this.close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.CTRL_DOWN_MASK));
		this.close.addActionListener(a -> gui.closeEditor(editor));
		this.ui.add(this.close);

		this.closeAll = new JMenuItem(I18n.translate("popup_menu.editor_tab.close_all"));
		this.closeAll.addActionListener(a -> gui.closeAllEditorTabs());
		this.ui.add(this.closeAll);

		this.closeOthers = new JMenuItem(I18n.translate("popup_menu.editor_tab.close_others"));
		this.closeOthers.addActionListener(a -> gui.closeTabsExcept(editor));
		this.ui.add(this.closeOthers);

		this.closeLeft = new JMenuItem(I18n.translate("popup_menu.editor_tab.close_left"));
		this.closeLeft.addActionListener(a -> gui.closeTabsLeftOf(editor));
		this.ui.add(this.closeLeft);

		this.closeRight = new JMenuItem(I18n.translate("popup_menu.editor_tab.close_right"));
		this.closeRight.addActionListener(a -> gui.closeTabsRightOf(editor));
		this.ui.add(this.closeRight);
	}

	public void show(Component invoker, int x, int y, PanelEditor panelEditor) {
		this.editor = panelEditor;
		ui.show(invoker, x, y);
	}

}
