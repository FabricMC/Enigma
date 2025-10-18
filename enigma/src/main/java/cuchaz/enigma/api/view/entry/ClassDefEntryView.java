package cuchaz.enigma.api.view.entry;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ClassDefEntryView extends ClassEntryView, DefEntryView {
	@Nullable
	ClassEntryView getSuperClass();

	ClassEntryView[] getInterfaces();
}
