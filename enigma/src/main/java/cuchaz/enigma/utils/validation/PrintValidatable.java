package cuchaz.enigma.utils.validation;

import java.util.Arrays;

public class PrintValidatable implements Validatable {

	public static final PrintValidatable INSTANCE = new PrintValidatable();

	@Override
	public void addMessage(ParameterizedMessage message) {
		String text = message.getText();
		String longText = message.getLongText();
		String type = switch (message.message.type) {
			case INFO -> "info";
			case WARNING -> "warning";
			case ERROR -> "error";
		};
		System.out.printf("%s: %s\n", type, text);
		if (!longText.isEmpty()) {
			Arrays.stream(longText.split("\n")).forEach(s -> System.out.printf("  %s\n", s));
		}
	}

	@Override
	public void clearMessages() {
	}

}
