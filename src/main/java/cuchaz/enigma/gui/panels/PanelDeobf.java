package cuchaz.enigma.gui.panels;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.LangUtils;

import javax.swing.*;
import java.awt.*;

public class PanelDeobf extends JPanel {

	public final ClassSelector deobfClasses;
	private final Gui gui;

	public PanelDeobf(Gui gui) {
		this.gui = gui;

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onPanelRename);

		this.setLayout(new BorderLayout());
		this.add(new JLabel(LangUtils.translate("panel.classes.deobfuscated")), BorderLayout.NORTH);
		this.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);
	}
}
