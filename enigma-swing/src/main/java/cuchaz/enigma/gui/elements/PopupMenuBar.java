package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.panels.PanelEditor;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class PopupMenuBar extends JPopupMenu {

	public final JMenuItem renameMenu;
	public final JMenuItem editJavadocMenu;
	public final JMenuItem showInheritanceMenu;
	public final JMenuItem showImplementationsMenu;
	public final JMenuItem showCallsMenu;
	public final JMenuItem showCallsSpecificMenu;
	public final JMenuItem openEntryMenu;
	public final JMenuItem openPreviousMenu;
	public final JMenuItem openNextMenu;
	public final JMenuItem toggleMappingMenu;

	public PopupMenuBar(PanelEditor editor, Gui gui) {
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.rename"));
			menu.addActionListener(event -> gui.startRename(editor));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.renameMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.javadoc"));
			menu.addActionListener(event -> gui.startDocChange(editor));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.editJavadocMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.inheritance"));
			menu.addActionListener(event -> gui.showInheritance(editor));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.showInheritanceMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.implementations"));
			menu.addActionListener(event -> gui.showImplementations(editor));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.showImplementationsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.calls"));
			menu.addActionListener(event -> gui.showCalls(editor, true));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.showCallsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.calls.specific"));
			menu.addActionListener(event -> gui.showCalls(editor, false));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.showCallsSpecificMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.declaration"));
			menu.addActionListener(event -> gui.getController().navigateTo(editor.getCursorReference().entry));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.openEntryMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.back"));
			menu.addActionListener(event -> gui.getController().openPreviousReference());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.openPreviousMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.forward"));
			menu.addActionListener(event -> gui.getController().openNextReference());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.openNextMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.mark_deobfuscated"));
			menu.addActionListener(event -> gui.toggleMapping(editor));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.toggleMappingMenu = menu;
		}
		{
			this.add(new JSeparator());
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.zoom.in"));
			menu.addActionListener(event -> editor.offsetEditorZoom(2));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
			this.add(menu);
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.zoom.out"));
			menu.addActionListener(event -> editor.offsetEditorZoom(-2));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
			this.add(menu);
		}
		{
			JMenuItem menu = new JMenuItem(I18n.translate("popup_menu.zoom.reset"));
			menu.addActionListener(event -> editor.resetEditorZoom());
			this.add(menu);
		}
	}
}
