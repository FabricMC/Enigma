package cuchaz.enigma.gui.elements;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import cuchaz.enigma.api.service.GuiService;
import cuchaz.enigma.utils.I18n;

public class CustomMenuItem implements GuiService.MenuItemBuilder {
	private final JMenuItem menuItem;
	private final Supplier<String> translationKey;
	private BooleanSupplier enabledCondition = () -> true;

	public CustomMenuItem(JMenuItem menuItem, Supplier<String> translationKey) {
		this.menuItem = menuItem;
		this.translationKey = translationKey;
		menuItem.setText(translationKey.get());
	}

	@Override
	public GuiService.MenuItemBuilder setAccelerator(KeyStroke accelerator) {
		menuItem.setAccelerator(accelerator);
		return this;
	}

	@Override
	public GuiService.MenuItemBuilder setEnabledWhen(BooleanSupplier condition) {
		this.enabledCondition = condition;
		return this;
	}

	@Override
	public GuiService.MenuItemBuilder setAction(Runnable action) {
		menuItem.addActionListener(e -> action.run());
		return this;
	}

	public void updateUiState() {
		menuItem.setEnabled(enabledCondition.getAsBoolean());
		menuItem.setText(I18n.translate(translationKey.get()));
	}

	public void retranslateUi() {
		menuItem.setText(I18n.translate(translationKey.get()));
	}
}
