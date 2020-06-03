package cuchaz.enigma.utils.validation;

import java.util.Arrays;

public class PrintValidatable implements Validatable {

	public static final PrintValidatable INSTANCE = new PrintValidatable();

	@Override
	public void addMessage(ParameterizedMessage message) {
		String text = message.getText();
		String longText = message.getLongText();
		String type;
		switch (message.message.type) {
			case INFO:
				type = "info";
				break;
			case WARNING:
				type = "warning";
				break;
			case ERROR:
				type = "error";
				break;
			default:
				throw new IllegalStateException("unreachable");
		}
		System.out.printf("%s: %s\n", type, text);
		if (!longText.isEmpty()) {
			Arrays.stream(longText.split("\n")).forEach(s -> System.out.printf("  %s\n", s));
		}
	}

	@Override
	public void clearMessages() {
	}

}
