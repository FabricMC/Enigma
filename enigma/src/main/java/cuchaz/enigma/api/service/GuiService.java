package cuchaz.enigma.api.service;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.KeyStroke;

import cuchaz.enigma.api.EnigmaIcon;
import cuchaz.enigma.api.view.GuiView;
import cuchaz.enigma.api.view.entry.EntryView;

public interface GuiService extends EnigmaService {
	EnigmaServiceType<GuiService> TYPE = EnigmaServiceType.create("gui");

	default void onStart(GuiView gui) {
	}

	default void addToEditorContextMenu(GuiView gui, MenuRegistrar registrar) {
	}

	default void addGutterMarkers(GuiView gui, EntryView entry, GutterMarkerAdder gutter) {
	}

	interface MenuRegistrar {
		void addSeparator();

		default MenuItemBuilder add(String translationKey) {
			return add(() -> translationKey);
		}

		MenuItemBuilder add(Supplier<String> translationKey);
	}

	interface MenuItemBuilder {
		MenuItemBuilder setAccelerator(KeyStroke accelerator);
		MenuItemBuilder setEnabledWhen(BooleanSupplier condition);
		MenuItemBuilder setAction(Runnable action);
	}

	interface GutterMarkerAdder {
		GutterMarkerBuilder addMarker(EnigmaIcon icon, GutterMarkerAlignment alignment);
	}

	interface GutterMarkerBuilder {
		GutterMarkerBuilder setClickAction(Runnable action);
		GutterMarkerBuilder setTooltip(String tooltip);
	}

	enum GutterMarkerAlignment {
		LEFT, RIGHT
	}
}
