package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.Nullable;

public interface ClassDefEntryView extends ClassEntryView, DefEntryView {
	@Nullable
	ClassEntryView getSuperClass();

	ClassEntryView[] getInterfaces();
}
