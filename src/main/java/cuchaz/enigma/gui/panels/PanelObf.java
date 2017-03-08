package cuchaz.enigma.gui.panels;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.mapping.ClassEntry;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class PanelObf extends JPanel {

	public final ClassSelector obfClasses;
	private final Gui gui;

	public PanelObf(Gui gui) {
		this.gui = gui;

		Comparator<ClassEntry> obfClassComparator = (a, b) -> {
			String aname = a.getName();
			String bname = b.getName();
			if (aname.length() != bname.length()) {
				return aname.length() - bname.length();
			}
			return aname.compareTo(bname);
		};

		this.obfClasses = new ClassSelector(gui, obfClassComparator, false);
		this.obfClasses.setSelectionListener(gui::navigateTo);
		this.obfClasses.setRenameSelectionListener(gui::onPanelRename);

		this.setLayout(new BorderLayout());
		this.add(new JLabel("Obfuscated Classes"), BorderLayout.NORTH);
		this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);
	}
}
