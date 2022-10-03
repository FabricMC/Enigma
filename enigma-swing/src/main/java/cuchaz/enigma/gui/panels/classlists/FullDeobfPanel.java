package cuchaz.enigma.gui.panels.classlists;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

public class FullDeobfPanel extends ClassPanel {
	public FullDeobfPanel(Gui gui) {
		super(gui);
	}

	@Override
	protected ClassSelector getClassSelector() {
		return new ClassSelector(this, gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
	}

	@Override
	public void retranslateUi() {
		updateCounter();
		this.contextMenu.retranslateUi();
	}

	@Override
	public void updateCounter() {
		this.title.setText(String.format("%s (%s)",
				I18n.translate(gui.isSingleClassTree() ? "info_panel.classes" : "info_panel.classes.fully_deobfuscated"),
				classes.getClassesCount()));
	}
}
