package cuchaz.enigma.api.view.index;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.api.view.entry.MethodEntryView;

@ApiStatus.NonExtendable
public interface BridgeMethodIndexView {
	@Nullable
	MethodEntryView getBridgeFromSpecialized(MethodEntryView specialized);

	@Nullable
	MethodEntryView getSpecializedFromBridge(MethodEntryView bridge);
}
