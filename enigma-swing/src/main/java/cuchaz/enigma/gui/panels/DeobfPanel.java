package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

public class DeobfPanel extends JPanel {

	public final ClassSelector deobfClasses;
	private final JLabel title = new JLabel();

	private final Gui gui;

	public DeobfPanel(Gui gui) {
		this.gui = gui;

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		this.setLayout(new BorderLayout());
		this.add(this.title, BorderLayout.NORTH);
		this.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

		this.retranslateUi();
	}

	public void retranslateUi() {
		this.title.setText(I18n.translate("info_panel.classes.deobfuscated"));
	}

}
