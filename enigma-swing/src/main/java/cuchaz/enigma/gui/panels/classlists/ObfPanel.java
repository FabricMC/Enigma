package cuchaz.enigma.gui.panels.classlists;

import java.util.Comparator;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;

public class ObfPanel extends ClassPanel {
	public ObfPanel(Gui gui) {
		super(gui);
	}

	@Override
	protected ClassSelector getClassSelector() {
		Comparator<ClassEntry> obfClassComparator = (a, b) -> {
			String aname = a.getFullName();
			String bname = b.getFullName();

			if (aname.length() != bname.length()) {
				return aname.length() - bname.length();
			}

			return aname.compareTo(bname);
		};

		return new ClassSelector(this, gui, obfClassComparator, false);
	}

	@Override
	public void retranslateUi() {
		this.title.setText(String.format("%s (%s)",
				I18n.translate("info_panel.classes.obfuscated"),
				classes.getClassesCount()));
	}

	@Override
	public void updateCounter() {
		retranslateUi();
	}
}
