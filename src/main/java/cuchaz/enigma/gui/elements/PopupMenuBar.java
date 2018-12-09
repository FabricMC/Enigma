package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.Gui;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class PopupMenuBar extends JPopupMenu {

	public final JMenuItem renameMenu;
	public final JMenuItem showInheritanceMenu;
	public final JMenuItem showImplementationsMenu;
	public final JMenuItem showCallsMenu;
	public final JMenuItem showCallsSpecificMenu;
	public final JMenuItem openEntryMenu;
	public final JMenuItem openPreviousMenu;
	public final JMenuItem toggleMappingMenu;

	public PopupMenuBar(Gui gui) {
		{
			JMenuItem menu = new JMenuItem("Rename");
			menu.addActionListener(event -> gui.startRename());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.renameMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Inheritance");
			menu.addActionListener(event -> gui.showInheritance());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.showInheritanceMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Implementations");
			menu.addActionListener(event -> gui.showImplementations());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.showImplementationsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Calls");
			menu.addActionListener(event -> gui.showCalls(true));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.showCallsMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Show Calls (Specific)");
			menu.addActionListener(event -> gui.showCalls(false));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK));
			menu.setEnabled(false);
			this.add(menu);
			this.showCallsSpecificMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Go to Declaration");
			menu.addActionListener(event -> gui.navigateTo(gui.reference.entry));
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.openEntryMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Go to previous");
			menu.addActionListener(event -> gui.getController().openPreviousReference());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.openPreviousMenu = menu;
		}
		{
			JMenuItem menu = new JMenuItem("Mark as deobfuscated");
			menu.addActionListener(event -> gui.toggleMapping());
			menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0));
			menu.setEnabled(false);
			this.add(menu);
			this.toggleMappingMenu = menu;
		}
	}
}
